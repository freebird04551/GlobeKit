package app.globekit.module.model

import java.util.Locale

class EnvironmentProfile(
    val region: RegionProfile,
    val carrier: CarrierProfile,
    languageTag: String? = null,
) {
    val languageTag: String? = languageTag?.let { tag ->
        if (tag == SYSTEM_LANGUAGE) return@let SYSTEM_LANGUAGE
        try {
            Locale.Builder().setLanguageTag(tag).build()
                .takeIf { it.language.isNotEmpty() && it.language != "und" }
                ?.toLanguageTag()
        } catch (_: java.util.IllformedLocaleException) {
            null
        }
    }
    val followsSystemLanguage: Boolean = this.languageTag == SYSTEM_LANGUAGE

    val locale: Locale
        get() = if (followsSystemLanguage) Locale.getDefault()
        else this.languageTag?.let(Locale::forLanguageTag) ?: region.locale

    companion object {
        const val SYSTEM_LANGUAGE = "system"
    }
}
