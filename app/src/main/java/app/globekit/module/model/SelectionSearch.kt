package app.globekit.module.model

import java.text.Normalizer
import java.util.Locale

interface SearchableOption {
    val searchFields: List<String>
}

object SelectionSearch {
    fun matchingIndices(items: List<Any?>, query: String): List<Int> {
        val normalized = normalize(query).trim()
        if (normalized.isEmpty()) return items.indices.toList()
        // An exact country/language code takes priority over name substrings
        // (e.g. "US" should not return Russia).
        val exact = items.indices.filter { index ->
            when (val item = items[index]) {
                is RegionProfile -> normalize(item.countryIso) == normalized
                is LanguageOption -> item.tag != null && compact(item.tag) == compact(normalized)
                else -> false
            }
        }
        if (exact.isNotEmpty()) return exact
        val words = normalized.split(Regex("\\s+"))
        return items.indices.filter { index ->
            val fields = fields(items[index]).map(::normalize)
            words.all { word -> fields.any { it.contains(word) || compact(it).contains(compact(word)) } }
        }
    }

    private fun fields(item: Any?): List<String> = when (item) {
        is SearchableOption -> item.searchFields
        is RegionProfile -> listOf(item.displayName, item.id, item.countryIso,
            item.locale.getDisplayCountry(Locale.ENGLISH))
        is LanguageOption -> listOf(item.label, item.tag.orEmpty(),
            if (item.tag != null && item.tag != EnvironmentProfile.SYSTEM_LANGUAGE)
                Locale.forLanguageTag(item.tag).getDisplayLanguage(Locale.ENGLISH) else "")
        else -> listOf(item.toString())
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFKC).lowercase(Locale.ROOT).replace('_', '-')
    private fun compact(value: String): String = normalize(value).replace("-", "")
}
