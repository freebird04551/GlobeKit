package app.globekit.module

import android.app.Activity
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

internal object PageUi {
    fun configureWindow(activity: Activity, root: View) {
        val flags = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
            WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
        activity.window.insetsController?.setSystemBarsAppearance(if (AppAppearance.isDark(activity)) 0 else flags, flags)
        root.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    fun addHeader(activity: Activity, parent: LinearLayout, title: Int) {
        fun dp(value: Int) = (value * activity.resources.displayMetrics.density).toInt()
        val header = LinearLayout(activity).apply { gravity = Gravity.CENTER_VERTICAL }
        header.addView(Button(activity, null, android.R.attr.borderlessButtonStyle).apply {
            setText(R.string.back)
            setTextColor(AppAppearance.accent(activity))
            isAllCaps = false
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 0)
            setOnClickListener { activity.finish() }
        }, LinearLayout.LayoutParams(dp(96), dp(52)))
        header.addView(TextView(activity).apply {
            setText(title)
            textSize = 24f
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            setTextColor(activity.getColor(R.color.text_primary))
        }, LinearLayout.LayoutParams(0, dp(52), 1f))
        header.addView(View(activity), LinearLayout.LayoutParams(dp(96), 1))
        parent.addView(header)
    }
}
