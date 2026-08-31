package app.scopeshift.module

import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import app.scopeshift.module.model.CarrierProfile
import app.scopeshift.module.model.EnvironmentProfile
import app.scopeshift.module.model.ProfileCatalog
import app.scopeshift.module.model.ProfileStore
import app.scopeshift.module.model.RegionProfile
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

class MainActivity : Activity(), XposedServiceHelper.OnServiceListener {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)
            bindViews()
            configureSystemBars()
            configureSelectors()
            findViewById<View>(R.id.root).post(::connectXposedService)
        } catch (error: Throwable) {
            Log.e(TAG, "Unable to create the configuration screen", error)
            showStartupFailure(error)
        }
    }

    private fun bindViews() {
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

    private fun configureSystemBars() {
        window.insetsController?.setSystemBarsAppearance(
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
        )
        findViewById<View>(R.id.root).setOnApplyWindowInsetsListener { view, insets ->
            val systemBars = insets.getInsets(WindowInsets.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun configureSelectors() {
        regionSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            ProfileCatalog.regions,
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        regionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateCarrierSpinner(ProfileCatalog.regions[position], null)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        carrierSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updatePreview()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        saveButton.isEnabled = false
        saveButton.setOnClickListener { saveSelection() }
        resetButton.isEnabled = false
        resetButton.setOnClickListener { resetConfiguration() }
        updateCarrierSpinner(ProfileCatalog.defaultRegion, null)
    }

    private fun connectXposedService() {
        try {
            XposedServiceHelper.registerListener(this)
        } catch (error: Throwable) {
            Log.e(TAG, "Unable to register the Xposed service listener", error)
            showServiceError(getString(R.string.service_unavailable))
        }
    }

    override fun onServiceBind(service: XposedService) {
        runOnUiThread { bindServiceState(service) }
    }

    override fun onServiceDied(service: XposedService) {
        runOnUiThread { bindServiceState(null) }
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
            val apiVersion = service.apiVersion
            if (apiVersion < MINIMUM_XPOSED_API) {
                showServiceError(getString(R.string.api_too_old, apiVersion))
                return
            }
            val remotePreferences = service.getRemotePreferences(ProfileStore.FILE_NAME)
            val stored = ProfileStore.read(remotePreferences)
            val configured = ProfileStore.isConfigured(remotePreferences)
            scopeCount = service.scope.size
            preferences = remotePreferences
            statusDot.setTextColor(getColor(R.color.accent))
            statusText.text = getString(R.string.connected, service.frameworkName, apiVersion)
            restoreSelection(stored)
            updateConfigSummary(stored, configured)
            saveButton.isEnabled = true
            resetButton.isEnabled = configured
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
        restoringSelection = false
        updatePreview()
    }

    private fun updateCarrierSpinner(region: RegionProfile, preferredCarrierId: String?) {
        val selectedId = preferredCarrierId ?: (carrierSpinner.selectedItem as? CarrierProfile)?.id
        carrierSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            region.carriers,
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
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
        languageValue.text = region.localeTag
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
            if (ProfileStore.write(remotePreferences, region, carrier)) {
                updateConfigSummary(EnvironmentProfile(region, carrier), true)
                resetButton.isEnabled = true
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
            if (ProfileStore.clear(remotePreferences)) {
                val defaultProfile = ProfileStore.read(remotePreferences)
                restoreSelection(defaultProfile)
                updateConfigSummary(defaultProfile, false)
                resetButton.isEnabled = false
                Toast.makeText(this, R.string.reset_success, Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, R.string.reset_failed, Toast.LENGTH_SHORT).show()
            }
        } catch (error: Throwable) {
            Log.e(TAG, "Unable to clear the remote preferences", error)
            showServiceError(getString(R.string.service_unavailable))
            Toast.makeText(this, R.string.reset_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateConfigSummary(profile: EnvironmentProfile, configured: Boolean) {
        val configuration = if (configured) {
            getString(R.string.configured, profile.region.displayName)
        } else {
            getString(R.string.unconfigured)
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
        const val TAG = "ScopeShift"
        const val MINIMUM_XPOSED_API = 101
    }
}
