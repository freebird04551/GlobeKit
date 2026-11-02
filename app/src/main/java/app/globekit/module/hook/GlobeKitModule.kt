package app.globekit.module.hook

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.content.res.Resources
import android.location.Location
import android.os.LocaleList
import android.os.PersistableBundle
import android.telephony.CarrierConfigManager
import android.telephony.ServiceState
import android.telephony.SubscriptionInfo
import android.telephony.TelephonyManager
import android.util.Log
import app.globekit.module.model.EnvironmentProfile
import app.globekit.module.model.ProfileStore
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone

class GlobeKitModule : XposedModule() {
    @Volatile
    private var activeProfile: EnvironmentProfile? = null
    private var installedHooks = 0

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        log(
            Log.INFO,
            TAG,
            "Loaded in ${param.processName} with $frameworkName API $apiVersion",
        )
    }

    override fun onPackageLoaded(param: PackageLoadedParam) {
        if (!param.isFirstPackage || param.packageName in EXCLUDED_PACKAGES) return
        try {
            val remotePreferences = getRemotePreferences(ProfileStore.FILE_NAME)
            activeProfile = ProfileStore.readActive(remotePreferences, param.packageName)

            installTelephonyHooks()
            installCarrierConfigHooks()
            installLocaleHooks()
            installTimeZoneHooks()
            installLocationHooks()
            log(Log.INFO, TAG, "Installed $installedHooks hooks for ${param.packageName}")
        } catch (error: Throwable) {
            log(Log.ERROR, TAG, "Failed to initialize hooks for ${param.packageName}", error)
        }
    }

    private fun installTelephonyHooks() {
        hookConstant(
            TelephonyManager::class.java,
            setOf("getSimCountryIso", "getSimCountryIsoForPhone"),
        ) { it.region.countryIsoLowercase }
        hookConstant(
            TelephonyManager::class.java,
            setOf("getNetworkCountryIso", "getNetworkCountryIsoForPhone"),
        ) { it.region.countryIsoLowercase }
        hookConstant(
            TelephonyManager::class.java,
            setOf(
                "getSimOperatorName",
                "getSimOperatorNameForPhone",
                "getNetworkOperatorName",
                "getNetworkOperatorNameForPhone",
            ),
        ) { it.carrier.displayName }
        hookConstant(
            TelephonyManager::class.java,
            setOf(
                "getSimOperator",
                "getSimOperatorNumeric",
                "getSimOperatorNumericForPhone",
                "getNetworkOperator",
                "getNetworkOperatorForPhone",
            ),
        ) { it.carrier.operatorNumeric }

        hookConstant(SubscriptionInfo::class.java, setOf("getCountryIso")) {
            it.region.countryIsoLowercase
        }
        hookConstant(SubscriptionInfo::class.java, setOf("getCarrierName")) {
            it.carrier.displayName
        }
        hookConstant(SubscriptionInfo::class.java, setOf("getMcc")) { it.carrier.mccInt }
        hookConstant(SubscriptionInfo::class.java, setOf("getMnc")) { it.carrier.mncInt }
        hookConstant(SubscriptionInfo::class.java, setOf("getMccString")) { it.carrier.mcc }
        hookConstant(SubscriptionInfo::class.java, setOf("getMncString")) { it.carrier.mnc }

        hookConstant(
            ServiceState::class.java,
            setOf("getOperatorAlphaLong", "getOperatorAlphaShort"),
        ) { it.carrier.displayName }
        hookConstant(ServiceState::class.java, setOf("getOperatorNumeric")) {
            it.carrier.operatorNumeric
        }
    }

    private fun installCarrierConfigHooks() {
        val names = setOf(
            "getConfig",
            "getConfigForSubId",
            "getConfigForSubIdWithFeature",
            "getConfigSubsetForSubId",
        )
        for (method in CarrierConfigManager::class.java.declaredMethods) {
            if (method.name !in names || !PersistableBundle::class.java.isAssignableFrom(method.returnType)) {
                continue
            }
            install(method) {
                hook(method).intercept { chain ->
                    val result = chain.proceed()
                    val current = activeProfile
                    if (current == null) {
                        result
                    } else {
                        val bundle = if (result is PersistableBundle) {
                            PersistableBundle(result)
                        } else {
                            PersistableBundle()
                        }
                        bundle.putString(KEY_COUNTRY, current.region.countryIsoLowercase)
                        bundle.putBoolean(KEY_NAME_ENABLED, true)
                        bundle.putString(KEY_NAME, current.carrier.displayName)
                        bundle.putBoolean(KEY_FORCE_HOME, true)
                        bundle
                    }
                }
            }
        }
    }

    private fun installLocaleHooks() {
        // Preserve original locale APIs, resource configuration and the full
        // language list when the user asks to follow the system.
        if (activeProfile?.followsSystemLanguage == true) return
        hookConstant(Locale::class.java, setOf("getDefault")) { it.locale }
        hookConstant(LocaleList::class.java, setOf("getDefault", "getAdjustedDefault")) {
            LocaleList(it.locale)
        }
        hookConstant(Configuration::class.java, setOf("getLocales")) {
            LocaleList(it.locale)
        }

        for (method in Resources::class.java.declaredMethods) {
            if (method.name != "getConfiguration" ||
                !Configuration::class.java.isAssignableFrom(method.returnType)
            ) {
                continue
            }
            install(method) {
                hook(method).intercept { chain ->
                    val original = chain.proceed() as Configuration
                    val current = activeProfile
                    if (current == null) {
                        original
                    } else {
                        Configuration(original).apply {
                            setLocales(LocaleList(current.locale))
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("PrivateApi")
    private fun installTimeZoneHooks() {
        hookConstant(TimeZone::class.java, setOf("getDefault")) {
            TimeZone.getTimeZone(it.region.timeZoneId)
        }
        hookConstant(ZoneId::class.java, setOf("systemDefault")) { it.region.zoneId }
        hookConstant(android.icu.util.TimeZone::class.java, setOf("getDefault")) {
            android.icu.util.TimeZone.getTimeZone(it.region.timeZoneId)
        }

        try {
            val systemProperties = Class.forName("android.os.SystemProperties")
            for (method in systemProperties.declaredMethods) {
                if (method.name != "get" || method.parameterCount == 0 ||
                    method.parameterTypes[0] != String::class.java || method.returnType != String::class.java
                ) {
                    continue
                }
                install(method) {
                    hook(method).intercept { chain ->
                        val current = activeProfile
                        if (current != null && chain.getArg(0) == "persist.sys.timezone") {
                            current.region.timeZoneId
                        } else {
                            chain.proceed()
                        }
                    }
                }
            }
        } catch (error: ClassNotFoundException) {
            log(Log.WARN, TAG, "SystemProperties is unavailable", error)
        }
    }

    private fun installLocationHooks() {
        hookConstant(Location::class.java, setOf("getLatitude")) { it.region.latitude }
        hookConstant(Location::class.java, setOf("getLongitude")) { it.region.longitude }
    }

    private fun hookConstant(
        type: Class<*>,
        names: Set<String>,
        value: (EnvironmentProfile) -> Any?,
    ) {
        for (method in type.declaredMethods) {
            if (method.name !in names || Modifier.isAbstract(method.modifiers)) continue
            install(method) {
                hook(method).intercept { chain ->
                    val current = activeProfile
                    if (current == null) chain.proceed() else value(current)
                }
            }
        }
    }

    private fun install(method: Method, installer: () -> Unit) {
        try {
            method.isAccessible = true
            installer()
            installedHooks++
        } catch (error: Throwable) {
            log(Log.WARN, TAG, "Cannot hook $method", error)
        }
    }

    private companion object {
        const val TAG = "GlobeKit"
        const val KEY_COUNTRY = "sim_country_iso_override_string"
        const val KEY_NAME_ENABLED = "carrier_name_override_bool"
        const val KEY_NAME = "carrier_name_string"
        const val KEY_FORCE_HOME = "force_home_network_bool"
        val EXCLUDED_PACKAGES = setOf(
            "app.globekit.module",
            "android",
            "com.android.phone",
            "com.android.systemui",
        )
    }
}
