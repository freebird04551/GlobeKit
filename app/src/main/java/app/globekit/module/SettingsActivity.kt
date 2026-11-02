package app.globekit.module

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ImageSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class SettingsActivity : Activity() {
    private var updates: UpdateController? = null
    private var versionValue: TextView? = null
    private val updateListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> refreshVersion() }
    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(AppAppearance.wrap(newBase))

    override fun onCreate(savedInstanceState: Bundle?) {
        AppAppearance.apply(this)
        super.onCreate(savedInstanceState)
        val root = ScrollView(this).apply { isFillViewport = true; clipToPadding = false }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(20), dp(24), dp(32))
        }
        root.addView(content)
        setContentView(root)
        PageUi.configureWindow(this, root)
        PageUi.addHeader(this, content, R.string.menu_settings)

        val appearance = card(content, R.string.appearance)
        val group = RadioGroup(this)
        val labels = listOf(R.string.mode_light, R.string.mode_dark, R.string.mode_system)
        AppAppearance.modes.forEachIndexed { index, mode ->
            group.addView(RadioButton(this).apply {
                id = View.generateViewId(); tag = mode
                setText(labels[index]); textSize = 18f; minHeight = dp(48)
                setTextColor(getColor(R.color.text_primary))
                isChecked = mode == AppAppearance.mode(this@SettingsActivity)
            })
        }
        group.setOnCheckedChangeListener { radioGroup, checkedId ->
            val mode = radioGroup.findViewById<RadioButton>(checkedId).tag as String
            if (mode != AppAppearance.mode(this)) {
                if (AppAppearance.setMode(this, mode)) recreate() else failed()
            }
        }
        appearance.addView(group)
        val names = resources.getStringArray(R.array.theme_colors)
        val paletteIndex = AppAppearance.palettes.indexOf(AppAppearance.palette(this))
        val colorValue = TextUtils.concat(colorLabel(names[paletteIndex], AppAppearance.accent(this)), "  ›")
        val colorRow = row(appearance, getString(R.string.theme_color), colorValue)
        colorRow.setOnClickListener {
            val choices = object : ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice, names) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                    (super.getView(position, convertView, parent) as CheckedTextView).apply {
                        setPaddingRelative(dp(24), paddingTop, dp(24), paddingBottom)
                        val color = AppAppearance.paletteAccent(this@SettingsActivity, AppAppearance.palettes[position])
                        text = colorLabel(names[position], color)
                    }
            }
            AlertDialog.Builder(this).setTitle(R.string.theme_color)
                .setSingleChoiceItems(choices, AppAppearance.palettes.indexOf(AppAppearance.palette(this))) { dialog, which ->
                    dialog.dismiss()
                    if (AppAppearance.setPalette(this, AppAppearance.palettes[which])) recreate() else failed()
                }.setNegativeButton(android.R.string.cancel, null).show()
        }

        val privacy = card(content, R.string.privacy_title)
        val toggle = Switch(this).apply {
            setText(R.string.hide_launcher); textSize = 18f
            setTextColor(getColor(R.color.text_primary)); minHeight = dp(52)
            isChecked = isLauncherHidden()
        }
        privacy.addView(toggle, LinearLayout.LayoutParams(-1, -2))
        privacy.addView(label(getString(R.string.hide_launcher_hint), 13f, true).apply {
            setPadding(0, dp(4), 0, dp(4))
        })
        var updatingToggle = false
        fun syncToggle() { updatingToggle = true; toggle.isChecked = isLauncherHidden(); updatingToggle = false }
        fun changeVisibility(hidden: Boolean) {
            try {
                packageManager.setComponentEnabledSetting(
                    launcher(),
                    if (hidden) PackageManager.COMPONENT_ENABLED_STATE_DISABLED else PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP,
                )
            } catch (_: RuntimeException) { failed() }
            syncToggle()
        }
        toggle.setOnCheckedChangeListener { _, checked ->
            if (!updatingToggle) {
                if (checked) {
                    AlertDialog.Builder(this).setTitle(R.string.hide_launcher)
                        .setMessage(R.string.hide_launcher_confirm)
                        .setPositiveButton(R.string.hide_action) { _, _ -> changeVisibility(true) }
                        .setNegativeButton(android.R.string.cancel, null)
                        .setOnDismissListener { syncToggle() }.show()
                } else changeVisibility(false)
            }
        }

        val updateCard = card(content, R.string.menu_updates)
        updateCard.addView(Switch(this).apply {
            setText(R.string.automatic_updates); textSize = 18f; minHeight = dp(52)
            setTextColor(getColor(R.color.text_primary))
            isChecked = UpdateState.automatic(this@SettingsActivity)
            setOnCheckedChangeListener { _, checked -> UpdateState.setAutomatic(this@SettingsActivity, checked) }
        }, LinearLayout.LayoutParams(-1, -2))
        updateCard.addView(label(getString(R.string.automatic_updates_hint), 13f, true))

        val about = card(content, R.string.menu_about)
        val versionRow = row(about, getString(R.string.current_version), AppLinks.versionLabel(this))
        versionValue = versionRow.getChildAt(1) as TextView
        versionRow.setOnClickListener {
            updateController().check()
        }
        refreshVersion()
        row(about, getString(R.string.github_download), "GitHub  ›").setOnClickListener { AppLinks.open(this, AppLinks.RELEASES) }
        row(about, getString(R.string.project_page), getString(R.string.app_name) + "  ›").setOnClickListener { AppLinks.open(this, AppLinks.PROJECT) }
    }

    override fun onStart() {
        super.onStart()
        UpdateState.preferences(this).registerOnSharedPreferenceChangeListener(updateListener)
        refreshVersion()
    }

    private fun updateController() = updates ?: UpdateController(this, ::refreshVersion).also { updates = it }

    override fun onResume() {
        super.onResume()
        updateController().check(automatic = true)
    }

    override fun onStop() {
        UpdateState.preferences(this).unregisterOnSharedPreferenceChangeListener(updateListener)
        super.onStop()
    }

    override fun onDestroy() {
        updates?.close()
        super.onDestroy()
    }

    private fun refreshVersion() {
        versionValue?.text = UpdateState.versionLabel(this)
        (versionValue?.parent as? View)?.contentDescription = UpdateState.versionDescription(this)
    }

    private fun launcher() = ComponentName(packageName, "$packageName.Launcher")
    private fun isLauncherHidden(): Boolean = packageManager.getComponentEnabledSetting(launcher()) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    private fun failed() = Toast.makeText(this, R.string.settings_failed, Toast.LENGTH_SHORT).show()
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun colorDot(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        setBounds(0, 0, dp(20), dp(20))
    }

    private fun colorLabel(name: String, color: Int): CharSequence =
        SpannableStringBuilder().append(name).append("  \uFFFC").apply {
            setSpan(ImageSpan(colorDot(color), ImageSpan.ALIGN_CENTER), length - 1, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

    private fun label(text: CharSequence, size: Float, secondary: Boolean = false) = TextView(this).apply {
        this.text = text; textSize = size
        setTextColor(getColor(if (secondary) R.color.text_secondary else R.color.text_primary))
    }

    private fun card(parent: LinearLayout, title: Int): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(14))
            setBackgroundResource(R.drawable.bg_card)
        }
        parent.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(18) })
        card.addView(label(getString(title), 16f).apply {
            setTextColor(AppAppearance.accent(this@SettingsActivity)); setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, dp(10))
        })
        return card
    }

    private fun row(parent: LinearLayout, title: String, value: CharSequence): LinearLayout {
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL; minimumHeight = dp(56)
            setPadding(0, dp(8), 0, dp(8))
        }
        row.addView(label(title, 17f), LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(label(value, 14f, true).apply { gravity = Gravity.END }, LinearLayout.LayoutParams(-2, -2))
        parent.addView(row, LinearLayout.LayoutParams(-1, -2))
        return row
    }
}
