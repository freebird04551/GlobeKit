package app.globekit.module

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import app.globekit.module.model.LanguageOption
import app.globekit.module.model.RegionProfile
import app.globekit.module.model.SelectionSearch

/** Keeps the normal Spinner selection API while adding a searchable picker. */
class SearchableSpinner @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.spinnerStyle,
) : Spinner(context, attrs, defStyleAttr) {
    private var picker: AlertDialog? = null

    // Spinner's superclass opens the unfiltered popup. Dispatch the click event
    // here so accessibility actions use the searchable picker as well.
    @SuppressLint("ClickableViewAccessibility")
    override fun performClick(): Boolean {
        if (picker?.isShowing == true) return true
        val source = adapter ?: return false
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED)
        val items = (0 until source.count).map(source::getItem)
        val regionPicker = items.firstOrNull() is RegionProfile
        val targetPicker = items.firstOrNull() is ConfigTarget
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), 0)
            isFocusableInTouchMode = true
        }
        val searchRow = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        val search = EditText(context).apply {
            setSingleLine(true)
            hint = context.getString(when {
                targetPicker -> R.string.search_apps
                regionPicker -> R.string.search_regions
                else -> R.string.search_languages
            })
            contentDescription = hint
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            imeOptions = EditorInfo.IME_ACTION_DONE
            textSize = 16f
            setTextColor(context.getColor(R.color.text_primary))
        }
        val clear = Button(context, null, android.R.attr.borderlessButtonStyle).apply {
            setText(R.string.search_clear)
            setTextColor(AppAppearance.accent(context))
            isEnabled = false
            setOnClickListener { search.text.clear() }
        }
        searchRow.addView(search, LinearLayout.LayoutParams(0, dp(56), 1f))
        searchRow.addView(clear, LinearLayout.LayoutParams(dp(64), dp(48)))
        content.addView(searchRow)
        val empty = TextView(context).apply {
            setText(R.string.search_empty); gravity = Gravity.CENTER
            setTextColor(context.getColor(R.color.text_secondary))
            visibility = View.GONE
        }
        val results = if (targetPicker) null else
            ArrayAdapter<String>(context, android.R.layout.simple_list_item_single_choice)
        val targetResults = if (targetPicker) ConfigTargetAdapter(context, emptyList()) else null
        val list = ListView(context).apply {
            choiceMode = ListView.CHOICE_MODE_SINGLE
            adapter = targetResults ?: results
        }
        val listHeight = dp(minOf(320, resources.configuration.screenHeightDp / 2))
        content.addView(list, LinearLayout.LayoutParams(-1, listHeight))
        content.addView(empty, LinearLayout.LayoutParams(-1, listHeight))
        list.emptyView = empty

        var visible = items.indices.toList()
        fun filter(query: String) {
            visible = SelectionSearch.matchingIndices(items, query)
            if (targetResults != null) {
                targetResults.replaceItems(visible.map { items[it] as ConfigTarget })
            } else {
                results?.replaceItems(visible.map { items[it].toString() })
            }
            list.clearChoices()
            val checked = visible.indexOf(selectedItemPosition)
            if (checked >= 0) list.setItemChecked(checked, true)
            clear.isEnabled = query.isNotEmpty()
            if (query.isBlank() && checked >= 0) list.setSelection(checked) else list.setSelection(0)
        }
        filter("")
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { filter(s?.toString().orEmpty()) }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        val dialog = AlertDialog.Builder(context)
            .setTitle(when {
                targetPicker -> R.string.config_target
                regionPicker -> R.string.choose_region
                else -> R.string.choose_language
            })
            .setView(content)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        picker = dialog
        list.setOnItemClickListener { _, _, position, _ ->
            val chosen = items[visible[position]]
            val current = adapter
            val index = current?.let { currentAdapter ->
                (0 until currentAdapter.count).firstOrNull { sameOption(chosen, currentAdapter.getItem(it)) }
            }
            if (index == null) Toast.makeText(context, R.string.search_list_changed, Toast.LENGTH_SHORT).show()
            else setSelection(index)
            dialog.dismiss()
        }
        dialog.setOnDismissListener { picker = null }
        content.requestFocus()
        // Floating dialogs still use this window mode for keyboard resizing.
        @Suppress("DEPRECATION")
        val keyboardMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        dialog.window?.setSoftInputMode(keyboardMode)
        dialog.show()
        return true
    }

    private fun sameOption(left: Any?, right: Any?): Boolean = when {
        left is RegionProfile && right is RegionProfile -> left.id == right.id
        left is LanguageOption && right is LanguageOption -> left.tag == right.tag
        left is ConfigTarget && right is ConfigTarget -> left.packageName == right.packageName
        else -> left == right
    }

    override fun onDetachedFromWindow() {
        picker?.dismiss()
        super.onDetachedFromWindow()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
