package app.globekit.module.model

import org.junit.Assert.*
import org.junit.Test

class LanguageCatalogTest {
    @Test fun restoringLegacyRegionalLanguagePreservesTagWithoutDuplicateChoices() {
        val options = LanguageCatalog.optionsFor("en-AU")
        val selected = options.single { it.tag == "en-AU" }
        assertEquals("en-AU", EnvironmentProfile(ProfileCatalog.defaultRegion,
            ProfileCatalog.defaultRegion.carriers.first(), selected.tag).locale.toLanguageTag())
        assertEquals(1, options.count { it.tag?.startsWith("en-") == true })
    }

    @Test fun chineseVariantsAndSystemFollowingRemainIndependentlySelectable() {
        val tags = LanguageCatalog.optionsFor("zh-HK").map { it.tag }
        assertTrue(tags.containsAll(listOf(null, "system", "zh-CN", "zh-TW", "zh-HK")))
    }
}
