package app.globekit.module

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class AboutActivity : Activity() {
    private lateinit var version: Button
    private lateinit var updates: UpdateController
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> refreshVersion() }

    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(AppAppearance.wrap(newBase))

    override fun onCreate(savedInstanceState: Bundle?) {
        AppAppearance.apply(this)
        super.onCreate(savedInstanceState)
        updates = UpdateController(this, ::refreshVersion)
        val root = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(20), dp(24), dp(32))
        }
        root.addView(content)
        setContentView(root)
        PageUi.configureWindow(this, root)
        PageUi.addHeader(this, content, R.string.menu_about)
        content.addView(TextView(this).apply {
            setText(R.string.about_description)
            textSize = 17f; setTextColor(getColor(R.color.text_primary))
            setPadding(0, dp(24), 0, dp(24))
        })
        version = Button(this, null, android.R.attr.borderlessButtonStyle).apply {
            isAllCaps = false; textSize = 17f; minHeight = dp(80)
            setTextColor(getColor(R.color.text_primary))
            setOnClickListener { updates.check() }
        }
        content.addView(version, LinearLayout.LayoutParams(-1, -2))
        content.addView(TextView(this).apply {
            setText(R.string.version_check_hint); textSize = 13f; gravity = Gravity.CENTER
            setTextColor(getColor(R.color.text_secondary))
        })
        listOf(R.string.github_download to AppLinks.RELEASES, R.string.project_page to AppLinks.PROJECT).forEach { (title, url) ->
            content.addView(Button(this, null, android.R.attr.borderlessButtonStyle).apply {
                isAllCaps = false
                setText(title); minHeight = dp(56); setTextColor(AppAppearance.accent(this@AboutActivity))
                setOnClickListener { AppLinks.open(this@AboutActivity, url) }
            }, LinearLayout.LayoutParams(-1, -2))
        }
        refreshVersion()
    }

    private fun refreshVersion() {
        version.text = android.text.TextUtils.concat(getString(R.string.current_version), "\n", UpdateState.versionLabel(this))
        version.contentDescription = UpdateState.versionDescription(this)
    }

    override fun onStart() {
        super.onStart()
        UpdateState.preferences(this).registerOnSharedPreferenceChangeListener(listener)
        refreshVersion()
    }

    override fun onResume() {
        super.onResume()
        updates.check(automatic = true)
    }

    override fun onStop() {
        UpdateState.preferences(this).unregisterOnSharedPreferenceChangeListener(listener)
        super.onStop()
    }

    override fun onDestroy() {
        updates.close()
        super.onDestroy()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
