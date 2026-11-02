package app.globekit.module

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.PopupMenu
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import app.globekit.module.model.CarrierProfile
import app.globekit.module.model.EnvironmentProfile
import app.globekit.module.model.LanguageCatalog
import app.globekit.module.model.LanguageOption
import app.globekit.module.model.ProfileCatalog
import app.globekit.module.model.ProfileStore
import app.globekit.module.model.RegionProfile
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

class MainActivity : Activity(), XposedServiceHelper.OnServiceListener {
    private lateinit var targetSpinner: Spinner
    private lateinit var languageSpinner: Spinner
    private lateinit var regionSpinner: Spinner
    private lateinit var carrierSpinner: Spinner
    private lateinit var statusDot: TextView
    private lateinit var statusText: TextView
    private lateinit var scopeText: TextView
    private lateinit var simCountryValue: TextView
    private lateinit var networkCountryValue: TextView
    private lateinit var carrierValue: TextView
    private lateinit var mccMncValue: TextView
    private lateinit var languageValue: TextView
    private lateinit var timezoneValue: TextView
    private lateinit var offsetValue: TextView
    private lateinit var gpsValue: TextView
    private lateinit var saveButton: Button
    private lateinit var resetButton: Button
    private var preferences: SharedPreferences? = null
    private var scopeCount = 0
    private var restoringSelection = false
    private var carrierRegionId: String? = null
    private var xposedService: XposedService? = null
    private var appearanceSignature = ""
    private var updates: UpdateController? = null
    private var hasLoadedConfiguration = false
    private var loadedTargetPackage: String? = null
    private var pendingDraft: ConfigDraft? = null
    private val selectedPackageName: String?
        get() = (targetSpinner.selectedItem as? ConfigTarget)?.packageName

    private class ConfigDraft(val packageName: String?, val profile: EnvironmentProfile)

    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(AppAppearance.wrap(newBase))

    override fun onCreate(savedInstanceState: Bundle?) {
        AppAppearance.apply(this)
        super.onCreate(savedInstanceState)
        appearanceSignature = AppAppearance.signature(this)
        if (savedInstanceState?.getBoolean("draft.present") == true) {
            val region = ProfileCatalog.findRegion(savedInstanceState.getString("draft.region"))
            val carrier = ProfileCatalog.findCarrier(region, savedInstanceState.getString("draft.carrier"))
            pendingDraft = ConfigDraft(savedInstanceState.getString("draft.package"),
                EnvironmentProfile(region, carrier, savedInstanceState.getString("draft.language")))
        }
        try {
            setContentView(R.layout.activity_main)
            bindViews()
            PageUi.configureWindow(this, findViewById(R.id.root))
            configureSelectors()
            findViewById<View>(R.id.overflow_button).setOnClickListener(::showOverflowMenu)
            findViewById<View>(R.id.update_notice).setOnClickListener { AppLinks.open(this, AppLinks.RELEASES) }
            findViewById<View>(R.id.root).post(::connectXposedService)
        } catch (error: Throwable) {
            Log.e(TAG, "Unable to create the configuration screen", error)
            showStartupFailure(error)
        }
    }

    private fun bindViews() {
        targetSpinner = findViewById(R.id.target_spinner)
        languageSpinner = findViewById(R.id.language_spinner)
        regionSpinner = findViewById(R.id.region_spinner)
        carrierSpinner = findViewById(R.id.carrier_spinner)
        statusDot = findViewById(R.id.status_dot)
        statusText = findViewById(R.id.status_text)
        scopeText = findViewById(R.id.scope_text)
        simCountryValue = findViewById(R.id.sim_country_value)
        networkCountryValue = findViewById(R.id.network_country_value)
        carrierValue = findViewById(R.id.carrier_value)
        mccMncValue = findViewById(R.id.mcc_mnc_value)
        languageValue = findViewById(R.id.language_value)
        timezoneValue = findViewById(R.id.timezone_value)
        offsetValue = findViewById(R.id.offset_value)
        gpsValue = findViewById(R.id.gps_value)
        saveButton = findViewById(R.id.save_button)
        resetButton = findViewById(R.id.reset_button)
    }

    private fun configureSelectors() {
        targetSpinner.adapter = ConfigTargetAdapter(this, listOf(ConfigTarget.default(this)))
        targetSpinner.onSelectionChanged {
            if (!hasLoadedConfiguration || selectedPackageName != loadedTargetPackage) loadSelectedConfiguration()
        }
        languageSpinner.setItems(LanguageCatalog.options)
        languageSpinner.onSelectionChanged {
            if (!restoringSelection) updatePreview()
        }
        regionSpinner.setItems(ProfileCatalog.regions)
        regionSpinner.onSelectionChanged {
            if (restoringSelection) return@onSelectionChanged
            val region = regionSpinner.selectedItem as? RegionProfile ?: return@onSelectionChanged
            if (carrierRegionId != region.id) updateCarrierSpinner(region, null) else updatePreview()
        }
        carrierSpinner.onSelectionChanged(::updatePreview)
        saveButton.isEnabled = false
        saveButton.setOnClickListener { saveSelection() }
        resetButton.isEnabled = false
        resetButton.setOnClickListener { resetConfiguration() }
        updateCarrierSpinner(ProfileCatalog.defaultRegion, null)
    }

    private fun connectXposedService() {
        if (isFinishing || isDestroyed) return
        try {
            XposedConnection.bind(this)
        } catch (error: Throwable) {
            Log.e(TAG, "Unable to register the Xposed service listener", error)
            showServiceError(getString(R.string.service_unavailable))
        }
    }

    override fun onServiceBind(service: XposedService) {
        runOnUiThread {
            xposedService = service
            bindServiceState(service)
        }
    }

    override fun onServiceDied(service: XposedService) {
        runOnUiThread {
            xposedService = null
            bindServiceState(null)
        }
    }

    override fun onResume() {
        super.onResume()
        if (appearanceSignature != AppAppearance.signature(this)) {
            recreate()
            return
        }
        xposedService?.let { bindServiceState(it) }
        refreshUpdateNotice()
        updateController().check(automatic = true)
    }

    private fun updateController(): UpdateController = updates ?:
        UpdateController(this, ::refreshUpdateNotice).also { updates = it }

    private fun refreshUpdateNotice() {
        val notice = findViewById<TextView>(R.id.update_notice) ?: return
        val latest = UpdateState.available(this).takeIf { UpdateState.automatic(this) }
        notice.visibility = if (latest == null) View.GONE else View.VISIBLE
        if (latest != null) notice.text = getString(R.string.update_banner, latest)
    }

    private fun showOverflowMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            menu.add(0, 1, 0, R.string.menu_settings)
            menu.add(0, 2, 1, R.string.menu_updates)
            menu.add(0, 3, 2, R.string.menu_about)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                    2 -> updateController().check()
                    3 -> startActivity(Intent(this@MainActivity, AboutActivity::class.java))
                }
                true
            }
            show()
        }
    }

    override fun onDestroy() {
        XposedConnection.unbind(this)
        updates?.close()
        super.onDestroy()
    }

    private fun currentDraft(): ConfigDraft? {
        if (!hasLoadedConfiguration) return null
        val region = regionSpinner.selectedItem as? RegionProfile ?: return null
        val carrier = carrierSpinner.selectedItem as? CarrierProfile ?: return null
        return ConfigDraft(selectedPackageName, EnvironmentProfile(region, carrier, selectedLanguageTag()))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        (currentDraft() ?: pendingDraft)?.let { draft ->
            outState.putBoolean("draft.present", true)
            outState.putString("draft.package", draft.packageName)
            outState.putString("draft.region", draft.profile.region.id)
            outState.putString("draft.carrier", draft.profile.carrier.id)
            outState.putString("draft.language", draft.profile.languageTag)
        }
        super.onSaveInstanceState(outState)
    }

    private fun bindServiceState(service: XposedService?) {
        if (service == null) {
            showUnavailableState(
                getString(R.string.module_inactive),
                R.string.module_inactive_hint,
            )
            return
        }

        try {
            val draft = pendingDraft ?: currentDraft()
            val apiVersion = service.apiVersion
            if (apiVersion < MINIMUM_XPOSED_API) {
                showServiceError(getString(R.string.api_too_old, apiVersion))
                return
            }
            val remotePreferences = service.getRemotePreferences(ProfileStore.FILE_NAME)
            val packages = service.scope.distinct().sorted()
            scopeCount = packages.size
            preferences = remotePreferences
            statusDot.setTextColor(AppAppearance.accent(this))
            statusText.text = getString(R.string.connected, service.frameworkName, apiVersion)
            val previousPackage = if (draft != null) draft.packageName else selectedPackageName
            val targets = listOf(ConfigTarget.default(this)) +
                packages.map { ConfigTarget.application(this, it) }
            targetSpinner.adapter = ConfigTargetAdapter(this, targets)
            targetSpinner.setSelection(targets.indexOfFirst { it.packageName == previousPackage }.coerceAtLeast(0), false)
            loadSelectedConfiguration()
            if (preferences != null && draft != null && selectedPackageName == draft.packageName) {
                restoreSelection(draft.profile)
            }
            pendingDraft = null
            saveButton.isEnabled = preferences != null
        } catch (error: Throwable) {
            Log.e(TAG, "Unable to initialize the Xposed service", error)
            showServiceError(getString(R.string.service_unavailable))
        }
    }

    private fun showServiceError(message: String) =
        showUnavailableState(message, R.string.service_error_hint)

    private fun showUnavailableState(message: String, hint: Int) {
        preferences = null
        scopeCount = 0
        statusDot.setTextColor(getColor(R.color.error))
        statusText.text = message
        scopeText.setText(hint)
        saveButton.isEnabled = false
        resetButton.isEnabled = false
    }

    private fun restoreSelection(profile: EnvironmentProfile) {
        restoringSelection = true
        regionSpinner.setSelection(ProfileCatalog.regions.indexOf(profile.region).coerceAtLeast(0), false)
        updateCarrierSpinner(profile.region, profile.carrier.id)
        val languageOptions = LanguageCatalog.optionsFor(profile.languageTag)
        languageSpinner.setItems(languageOptions)
        languageSpinner.setSelection(
            languageOptions.indexOfFirst { it.tag == profile.languageTag }.coerceAtLeast(0), false,
        )
        restoringSelection = false
        updatePreview()
    }

    private fun updateCarrierSpinner(region: RegionProfile, preferredCarrierId: String?) {
        carrierRegionId = region.id
        val selectedId = preferredCarrierId ?: (carrierSpinner.selectedItem as? CarrierProfile)?.id
        carrierSpinner.setItems(region.carriers)
        val selected = ProfileCatalog.findCarrier(region, selectedId)
        carrierSpinner.setSelection(region.carriers.indexOf(selected), false)
        if (!restoringSelection) updatePreview()
    }

    private fun updatePreview() {
        val region = regionSpinner.selectedItem as? RegionProfile ?: return
        val carrier = carrierSpinner.selectedItem as? CarrierProfile ?: return
        simCountryValue.text = region.countryIso
        networkCountryValue.text = region.countryIso
        carrierValue.text = carrier.displayName
        mccMncValue.text = carrier.operatorNumeric
        languageValue.text = EnvironmentProfile(region, carrier, selectedLanguageTag()).locale.toLanguageTag()
        timezoneValue.text = region.timeZoneId
        offsetValue.text = region.currentOffset
        gpsValue.text = getString(R.string.gps_coordinates, region.latitude, region.longitude)
    }

    private fun saveSelection() {
        val remotePreferences = preferences
        val region = regionSpinner.selectedItem as? RegionProfile
        val carrier = carrierSpinner.selectedItem as? CarrierProfile
        if (remotePreferences == null || region == null || carrier == null) {
            Toast.makeText(this, R.string.save_unavailable, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            if (ProfileStore.write(remotePreferences, region, carrier, selectedLanguageTag(), selectedPackageName)) {
                loadSelectedConfiguration()
                Toast.makeText(this, R.string.save_success, Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show()
            }
        } catch (error: Throwable) {
            Log.e(TAG, "Unable to save the remote preferences", error)
            showServiceError(getString(R.string.service_unavailable))
            Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetConfiguration() {
        val remotePreferences = preferences
        if (remotePreferences == null) {
            Toast.makeText(this, R.string.reset_unavailable, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val packageName = selectedPackageName
            if (ProfileStore.clear(remotePreferences, packageName)) {
                loadSelectedConfiguration()
                Toast.makeText(this, if (packageName == null) R.string.reset_success else R.string.override_reset_success, Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, R.string.reset_failed, Toast.LENGTH_SHORT).show()
            }
        } catch (error: Throwable) {
            Log.e(TAG, "Unable to clear the remote preferences", error)
            showServiceError(getString(R.string.service_unavailable))
            Toast.makeText(this, R.string.reset_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun selectedLanguageTag(): String? =
        (languageSpinner.selectedItem as? LanguageOption)?.tag

    private fun loadSelectedConfiguration() {
        val remotePreferences = preferences ?: return
        try {
            val packageName = selectedPackageName
            val active = ProfileStore.readActive(remotePreferences, packageName)
            restoreSelection(active ?: ProfileStore.read(remotePreferences))
            val configured = ProfileStore.isConfigured(remotePreferences, packageName)
            resetButton.setText(if (packageName == null) R.string.reset else R.string.remove_override)
            resetButton.isEnabled = configured
            updateConfigSummary(active, configured)
            loadedTargetPackage = packageName
            hasLoadedConfiguration = true
        } catch (error: Throwable) {
            Log.e(TAG, "Unable to load the selected configuration", error)
            showServiceError(getString(R.string.service_unavailable))
        }
    }

    private fun updateConfigSummary(profile: EnvironmentProfile?, configured: Boolean) {
        val configuration = when {
            selectedPackageName != null && configured -> getString(R.string.override_configured, profile!!.region.displayName)
            selectedPackageName != null && profile != null -> getString(R.string.inherited_default, profile.region.displayName)
            selectedPackageName != null -> getString(R.string.inherited_system)
            profile != null -> getString(R.string.configured, profile.region.displayName)
            else -> getString(R.string.unconfigured)
        }
        val targetState = if (scopeCount == 0) {
            getString(R.string.no_scope)
        } else {
            getString(R.string.scope_count, scopeCount)
        }
        scopeText.text = getString(R.string.configuration_and_scope, configuration, targetState)
    }

    private fun showStartupFailure(error: Throwable) {
        val padding = (24 * resources.displayMetrics.density).toInt()
        setContentView(TextView(this).apply {
            gravity = Gravity.CENTER
            setPadding(padding, padding, padding, padding)
            setTextColor(getColor(R.color.text_primary))
            textSize = 16f
            text = getString(R.string.startup_failed, error.javaClass.simpleName)
        })
    }

    private companion object {
        const val TAG = "GlobeKit"
        const val MINIMUM_XPOSED_API = 101
    }
}
