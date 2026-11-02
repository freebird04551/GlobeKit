package app.globekit.module.model

import android.content.SharedPreferences

object ProfileStore {
    const val FILE_NAME = "environment"
    private const val KEY_REGION = "region"
    private const val KEY_CARRIER = "carrier"
    private const val KEY_CONFIGURED = "configured"
    private const val KEY_LANGUAGE = "language"

    private fun key(name: String, packageName: String?): String =
        if (packageName == null) name else "app.$packageName.$name"

    fun read(preferences: SharedPreferences, packageName: String? = null): EnvironmentProfile {
        val region = ProfileCatalog.findRegion(
            preferences.getString(key(KEY_REGION, packageName), ProfileCatalog.defaultRegion.id),
        )
        val carrier = ProfileCatalog.findCarrier(
            region,
            preferences.getString(key(KEY_CARRIER, packageName), region.carriers.first().id),
        )
        return EnvironmentProfile(region, carrier, preferences.getString(key(KEY_LANGUAGE, packageName), null))
    }

    fun readActive(preferences: SharedPreferences, packageName: String? = null): EnvironmentProfile? =
        when {
            packageName != null && isConfigured(preferences, packageName) -> read(preferences, packageName)
            isConfigured(preferences) -> read(preferences)
            else -> null
        }

    fun isConfigured(preferences: SharedPreferences, packageName: String? = null): Boolean =
        preferences.getBoolean(key(KEY_CONFIGURED, packageName), false)

    fun write(
        preferences: SharedPreferences,
        region: RegionProfile,
        carrier: CarrierProfile,
        languageTag: String? = null,
        packageName: String? = null,
    ): Boolean = preferences.edit()
        .putBoolean(key(KEY_CONFIGURED, packageName), true)
        .putString(key(KEY_REGION, packageName), region.id)
        .putString(key(KEY_CARRIER, packageName), carrier.id)
        .putString(key(KEY_LANGUAGE, packageName), EnvironmentProfile(region, carrier, languageTag).languageTag)
        .commit()

    fun clear(preferences: SharedPreferences, packageName: String? = null): Boolean = preferences.edit()
        .remove(key(KEY_CONFIGURED, packageName))
        .remove(key(KEY_REGION, packageName))
        .remove(key(KEY_CARRIER, packageName))
        .remove(key(KEY_LANGUAGE, packageName))
        .commit()
}
