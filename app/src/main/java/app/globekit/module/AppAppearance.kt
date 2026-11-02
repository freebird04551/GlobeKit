package app.globekit.module

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.util.TypedValue
import android.view.ContextThemeWrapper

object AppAppearance {
    private const val FILE = "appearance"
    val modes = listOf("light", "dark", "system")
    val palettes = listOf("indigo", "blue", "green", "orange", "rose")

    fun mode(context: Context): String = preferences(context).getString("mode", "system")
        ?.takeIf { it in modes } ?: "system"
    fun palette(context: Context): String = preferences(context).getString("palette", "indigo")
        ?.takeIf { it in palettes } ?: "indigo"
    fun signature(context: Context): String = "${mode(context)}:${palette(context)}"
    fun setMode(context: Context, value: String): Boolean =
        value in modes && preferences(context).edit().putString("mode", value).commit()
    fun setPalette(context: Context, value: String): Boolean =
        value in palettes && preferences(context).edit().putString("palette", value).commit()
    private fun preferences(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun wrap(context: Context): Context {
        val config = Configuration(context.resources.configuration)
        val night = when (mode(context)) {
            "light" -> Configuration.UI_MODE_NIGHT_NO
            "dark" -> Configuration.UI_MODE_NIGHT_YES
            else -> config.uiMode and Configuration.UI_MODE_NIGHT_MASK
        }
        config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
        return context.createConfigurationContext(config)
    }

    fun isDark(context: Context): Boolean =
        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    fun apply(activity: Activity) {
        activity.setTheme(if (isDark(activity)) R.style.Theme_GlobeKit_Dark else R.style.Theme_GlobeKit)
        activity.theme.applyStyle(paletteStyle(palette(activity)), true)
    }

    private fun paletteStyle(palette: String): Int = when (palette) {
        "blue" -> R.style.Palette_Blue
        "green" -> R.style.Palette_Green
        "orange" -> R.style.Palette_Orange
        "rose" -> R.style.Palette_Rose
        else -> R.style.Palette_Indigo
    }

    fun paletteAccent(context: Context, palette: String): Int =
        accent(ContextThemeWrapper(context, paletteStyle(palette)))

    fun accent(context: Context): Int = TypedValue().also {
        context.theme.resolveAttribute(android.R.attr.colorAccent, it, true)
    }.data
}
