package app.globekit.module.model

import java.util.Locale

class LanguageOption(val tag: String?, val label: String) {
    override fun toString(): String =
        if (tag == null || tag == EnvironmentProfile.SYSTEM_LANGUAGE) label else "$label（$tag）"
}

object LanguageCatalog {
    val options: List<LanguageOption> = listOf(
        LanguageOption(null, "跟随所选地区"),
        LanguageOption(EnvironmentProfile.SYSTEM_LANGUAGE, "跟随系统语言"),
        // 中文与英语
        LanguageOption("zh-CN", "简体中文"),
        LanguageOption("zh-TW", "繁体中文（台湾）"),
        LanguageOption("zh-HK", "繁体中文（香港）"),
        LanguageOption("en-US", "英语"),
        // 东亚与东南亚语言
        LanguageOption("ja-JP", "日语"),
        LanguageOption("ko-KR", "韩语"),
        LanguageOption("vi-VN", "越南语"),
        LanguageOption("th-TH", "泰语"),
        LanguageOption("id-ID", "印度尼西亚语"),
        LanguageOption("ms-MY", "马来语"),
        // 欧洲语言，同语种及相关语言相邻
        LanguageOption("fr-FR", "法语"),
        LanguageOption("es-ES", "西班牙语"),
        LanguageOption("pt-BR", "葡萄牙语（巴西）"),
        LanguageOption("pt-PT", "葡萄牙语（葡萄牙）"),
        LanguageOption("it-IT", "意大利语"),
        LanguageOption("de-DE", "德语"),
        LanguageOption("nl-NL", "荷兰语"),
        LanguageOption("ru-RU", "俄语"),
        LanguageOption("uk-UA", "乌克兰语"),
        LanguageOption("pl-PL", "波兰语"),
        LanguageOption("cs-CZ", "捷克语"),
        LanguageOption("ro-RO", "罗马尼亚语"),
        LanguageOption("hu-HU", "匈牙利语"),
        LanguageOption("sv-SE", "瑞典语"),
        LanguageOption("nb-NO", "挪威语"),
        LanguageOption("da-DK", "丹麦语"),
        LanguageOption("fi-FI", "芬兰语"),
        LanguageOption("el-GR", "希腊语"),
        // 西亚与南亚语言
        LanguageOption("tr-TR", "土耳其语"),
        LanguageOption("ar-SA", "阿拉伯语"),
        LanguageOption("he-IL", "希伯来语"),
        LanguageOption("fa-IR", "波斯语"),
        LanguageOption("hi-IN", "印地语"),
        LanguageOption("bn-BD", "孟加拉语"),
        LanguageOption("ta-IN", "泰米尔语"),
        LanguageOption("ur-PK", "乌尔都语"),
        // 其他语言
        LanguageOption("sw-KE", "斯瓦希里语"),
    )

    // Keep an existing regional tag when restoring a saved configuration, while
    // presenting only one concise choice for that language in the UI.
    fun optionsFor(savedTag: String?): List<LanguageOption> {
        if (savedTag == null || options.any { it.tag == savedTag }) return options
        val locale = Locale.forLanguageTag(savedTag)
        val index = options.indexOfFirst {
            it.tag != null && it.tag != EnvironmentProfile.SYSTEM_LANGUAGE &&
                Locale.forLanguageTag(it.tag).language == locale.language
        }
        if (index < 0) return options + LanguageOption(savedTag, locale.getDisplayLanguage(Locale.SIMPLIFIED_CHINESE))
        return options.mapIndexed { i, option ->
            if (i == index) LanguageOption(savedTag, option.label) else option
        }
    }
}
