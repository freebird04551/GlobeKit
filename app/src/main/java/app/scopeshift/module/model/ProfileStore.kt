package app.scopeshift.module.model

import android.content.SharedPreferences

object ProfileStore {
    const val FILE_NAME = "environment"
    private const val KEY_REGION = "region"
    private const val KEY_CARRIER = "carrier"
    private const val KEY_CONFIGURED = "configured"

    fun read(preferences: SharedPreferences): EnvironmentProfile {
        val region = ProfileCatalog.findRegion(
            preferences.getString(KEY_REGION, ProfileCatalog.defaultRegion.id),
        )
        val carrier = ProfileCatalog.findCarrier(
            region,
            preferences.getString(KEY_CARRIER, region.carriers.first().id),
        )
        return EnvironmentProfile(region, carrier)
    }

    fun readActive(preferences: SharedPreferences): EnvironmentProfile? =
        if (isConfigured(preferences)) read(preferences) else null

    fun isConfigured(preferences: SharedPreferences): Boolean =
        preferences.getBoolean(KEY_CONFIGURED, false)

    fun write(
        preferences: SharedPreferences,
        region: RegionProfile,
        carrier: CarrierProfile,
    ): Boolean = preferences.edit()
        .putBoolean(KEY_CONFIGURED, true)
        .putString(KEY_REGION, region.id)
        .putString(KEY_CARRIER, carrier.id)
        .commit()

    fun clear(preferences: SharedPreferences): Boolean = preferences.edit()
        .remove(KEY_CONFIGURED)
        .remove(KEY_REGION)
        .remove(KEY_CARRIER)
        .commit()
}
