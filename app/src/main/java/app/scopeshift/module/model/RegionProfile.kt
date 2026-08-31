package app.scopeshift.module.model

import java.time.Instant
import java.time.ZoneId
import java.util.Locale

class RegionProfile(
    val id: String,
    val displayName: String,
    countryIso: String,
    val localeTag: String,
    val timeZoneId: String,
    val latitude: Double,
    val longitude: Double,
    val carriers: List<CarrierProfile>,
) {
    val countryIso: String = countryIso.uppercase(Locale.ROOT)
    val countryIsoLowercase: String = this.countryIso.lowercase(Locale.ROOT)
    val locale: Locale = Locale.forLanguageTag(localeTag)
    val zoneId: ZoneId = ZoneId.of(timeZoneId)

    val currentOffset: String
        get() {
            val totalMinutes = zoneId.rules.getOffset(Instant.now()).totalSeconds / 60
            val sign = if (totalMinutes >= 0) '+' else '-'
            val absolute = kotlin.math.abs(totalMinutes)
            return String.format(Locale.ROOT, "UTC%c%02d:%02d", sign, absolute / 60, absolute % 60)
        }

    init {
        require(carriers.isNotEmpty()) { "A region must contain at least one carrier" }
    }

    override fun toString(): String = displayName
}
