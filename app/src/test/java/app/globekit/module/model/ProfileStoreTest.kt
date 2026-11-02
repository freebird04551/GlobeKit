package app.globekit.module.model

import android.content.SharedPreferences
import java.lang.reflect.Proxy
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class ProfileStoreTest {
    private val preferences = memoryPreferences()
    private val japan = ProfileCatalog.regions.first { it.id == "jp-tokyo" }
    private val usa = ProfileCatalog.regions.first { it.id == "us-new-york" }

    @Test fun legacyDefaultRemainsActiveAndFollowsRegionLanguage() {
        preferences.edit().putBoolean("configured", true)
            .putString("region", "jp-tokyo").putString("carrier", "docomo").commit()
        val active = ProfileStore.readActive(preferences, "example.app")!!
        assertEquals("jp-tokyo", active.region.id)
        assertNull(active.languageTag)
        assertEquals("ja-JP", active.locale.toLanguageTag())
    }

    @Test fun unconfiguredAppsUseTheSystem() {
        assertNull(ProfileStore.readActive(preferences, "example.app"))
    }

    @Test fun appOverrideWinsWithoutChangingOtherApps() {
        ProfileStore.write(preferences, japan, japan.carriers.first())
        ProfileStore.write(preferences, usa, usa.carriers.first(), "zh-CN", "example.a")
        assertEquals("us-new-york", ProfileStore.readActive(preferences, "example.a")!!.region.id)
        assertEquals("zh-CN", ProfileStore.readActive(preferences, "example.a")!!.locale.toLanguageTag())
        assertEquals("jp-tokyo", ProfileStore.readActive(preferences, "example.b")!!.region.id)
    }

    @Test fun clearingAnOverrideRestoresDefaultAndPreservesOtherOverrides() {
        ProfileStore.write(preferences, japan, japan.carriers.first())
        ProfileStore.write(preferences, usa, usa.carriers.first(), "en-US", "example.a")
        ProfileStore.write(preferences, usa, usa.carriers.first(), "fr-FR", "example.b")
        ProfileStore.clear(preferences, "example.a")
        assertFalse(ProfileStore.isConfigured(preferences, "example.a"))
        assertEquals("ja-JP", ProfileStore.readActive(preferences, "example.a")!!.locale.toLanguageTag())
        assertEquals("fr-FR", ProfileStore.readActive(preferences, "example.b")!!.locale.toLanguageTag())
    }

    @Test fun clearingDefaultPreservesAppOverrides() {
        ProfileStore.write(preferences, japan, japan.carriers.first())
        ProfileStore.write(preferences, usa, usa.carriers.first(), null, "example.a")
        ProfileStore.clear(preferences)
        assertNull(ProfileStore.readActive(preferences, "example.b"))
        assertEquals("en-US", ProfileStore.readActive(preferences, "example.a")!!.locale.toLanguageTag())
    }

    @Test fun languageIsIndependentOfRegionAndCanReturnToFollowingRegion() {
        ProfileStore.write(preferences, japan, japan.carriers.first(), "zh-TW")
        assertEquals("zh-TW", ProfileStore.readActive(preferences)!!.locale.toLanguageTag())
        ProfileStore.write(preferences, usa, usa.carriers.first(), "zh-TW")
        assertEquals("zh-TW", ProfileStore.readActive(preferences)!!.locale.toLanguageTag())
        ProfileStore.write(preferences, usa, usa.carriers.first())
        assertEquals("en-US", ProfileStore.readActive(preferences)!!.locale.toLanguageTag())
    }

    @Test fun clearingOverrideWithoutDefaultRestoresSystem() {
        ProfileStore.write(preferences, japan, japan.carriers.first(), null, "example.a")
        ProfileStore.clear(preferences, "example.a")
        assertNull(ProfileStore.readActive(preferences, "example.a"))
    }

    @Test fun invalidStoredLanguageFallsBackToRegion() {
        preferences.edit().putBoolean("configured", true).putString("language", "!!").commit()
        assertEquals("ja-JP", ProfileStore.readActive(preferences)!!.locale.toLanguageTag())
    }

    @Test fun systemLanguageTracksCurrentLocaleInsteadOfTheSelectedRegion() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("de-DE"))
            ProfileStore.write(preferences, japan, japan.carriers.first(), "system")
            val active = ProfileStore.readActive(preferences)!!
            assertEquals("system", active.languageTag)
            assertEquals("de-DE", active.locale.toLanguageTag())
            Locale.setDefault(Locale.forLanguageTag("fr-FR"))
            assertEquals("fr-FR", active.locale.toLanguageTag())
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test fun appCanFollowSystemWhileOtherAppsFollowDefaultLanguage() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("zh-CN"))
            ProfileStore.write(preferences, japan, japan.carriers.first(), "en-US")
            ProfileStore.write(preferences, usa, usa.carriers.first(), "system", "example.a")
            assertEquals("zh-CN", ProfileStore.readActive(preferences, "example.a")!!.locale.toLanguageTag())
            assertEquals("en-US", ProfileStore.readActive(preferences, "example.b")!!.locale.toLanguageTag())
            ProfileStore.clear(preferences, "example.a")
            assertEquals("en-US", ProfileStore.readActive(preferences, "example.a")!!.locale.toLanguageTag())
        } finally {
            Locale.setDefault(original)
        }
    }

    // The Android interface is the only boundary replaced here. ProfileStore and
    // all profile/language resolution run unchanged on the JVM.
    private fun memoryPreferences(): SharedPreferences {
        val data = mutableMapOf<String, Any?>()
        return Proxy.newProxyInstance(javaClass.classLoader, arrayOf(SharedPreferences::class.java)) { _, method, args ->
            when (method.name) {
                "getString", "getBoolean" -> data[args!![0]] ?: args[1]
                "edit" -> {
                    val updates = mutableMapOf<String, Any?>()
                    Proxy.newProxyInstance(javaClass.classLoader, arrayOf(SharedPreferences.Editor::class.java)) { editor, edit, values ->
                        when (edit.name) {
                            "putString", "putBoolean" -> { updates[values!![0] as String] = values[1]; editor }
                            "remove" -> { updates[values!![0] as String] = null; editor }
                            "commit" -> { updates.forEach { (key, value) -> if (value == null) data.remove(key) else data[key] = value }; true }
                            else -> error("Unexpected editor call: ${edit.name}")
                        }
                    }
                }
                else -> error("Unexpected preferences call: ${method.name}")
            }
        } as SharedPreferences
    }
}
