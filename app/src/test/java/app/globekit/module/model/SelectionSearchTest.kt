package app.globekit.module.model

import org.junit.Assert.*
import org.junit.Test

class SelectionSearchTest {
    @Test fun searchesApplicationNameAndPackageWithoutChangingOrder() {
        val apps = listOf(
            listOf("默认配置", ""),
            listOf("TikTok", "com.ss.android.ugc.trill"),
            listOf("LINE", "jp.naver.line.android"),
        ).map { fields -> object : SearchableOption { override val searchFields = fields } }
        assertEquals(listOf(1), SelectionSearch.matchingIndices(apps, "tiktok"))
        assertEquals(listOf(1), SelectionSearch.matchingIndices(apps, "UGC.TRILL"))
        assertEquals(listOf(2), SelectionSearch.matchingIndices(apps, "LINE jp.naver"))
        assertEquals(listOf(1, 2), SelectionSearch.matchingIndices(apps, "android"))
        assertEquals(listOf(0), SelectionSearch.matchingIndices(apps, "默认"))
        assertEquals(emptyList<Int>(), SelectionSearch.matchingIndices(apps, "不存在"))
        assertEquals(listOf(0, 1, 2), SelectionSearch.matchingIndices(apps, ""))
    }

    private val regions = listOf("jp-tokyo", "us-new-york", "us-los-angeles", "ru-moscow")
        .map { ProfileCatalog.findRegion(it) }

    @Test fun searchesRegionCityAndEnglishNames() {
        assertEquals(listOf(0), SelectionSearch.matchingIndices(regions, "东京"))
        assertEquals(listOf(0), SelectionSearch.matchingIndices(regions, "Japan"))
        assertEquals(listOf(2), SelectionSearch.matchingIndices(regions, "美国 los"))
    }

    @Test fun countryCodeMatchesExactlyInsteadOfUnrelatedSubstrings() {
        assertEquals(listOf(1, 2), SelectionSearch.matchingIndices(regions, "us"))
    }

    @Test fun languageCodesAcceptCaseWidthAndUnderscoreVariations() {
        val languages = LanguageCatalog.optionsFor("en-AU")
        val hongKong = languages.indexOfFirst { it.tag == "zh-HK" }
        assertEquals(listOf(hongKong), SelectionSearch.matchingIndices(languages, "ＺＨ＿ＨＫ"))
        assertEquals(listOf(hongKong), SelectionSearch.matchingIndices(languages, "繁体 香港"))
        val english = languages.indexOfFirst { it.tag == "en-AU" }
        assertEquals(listOf(english), SelectionSearch.matchingIndices(languages, "enau"))
    }

    @Test fun clearingQueryRestoresOriginalOrderAndEmptyResultsStayEmpty() {
        assertEquals(listOf(0, 1, 2, 3), SelectionSearch.matchingIndices(regions, "  "))
        assertEquals(emptyList<Int>(), SelectionSearch.matchingIndices(regions, "不存在的地区"))
        assertEquals(listOf("jp-tokyo", "us-new-york", "us-los-angeles", "ru-moscow"), regions.map { it.id })
    }
}
