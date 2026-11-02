package app.globekit.module

import android.app.Activity
import android.app.AlertDialog
import app.globekit.module.model.ReleaseVersion
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.Future
import org.json.JSONObject

class UpdateController(private val activity: Activity, private val onChanged: () -> Unit = {}) {
    private val executor = Executors.newSingleThreadExecutor()
    private var task: Future<*>? = null
    private var progress: AlertDialog? = null
    private var resultDialog: AlertDialog? = null
    private var attemptToken = 0L
    private var running = false
    private var closed = false

    fun check(automatic: Boolean = false) {
        if (closed || activity.isFinishing || activity.isDestroyed) return
        if (automatic && !UpdateState.shouldCheck(activity)) return
        if (!automatic && progress?.isShowing != true) {
            progress = AlertDialog.Builder(activity)
                .setTitle(R.string.menu_updates)
                .setMessage(R.string.checking_updates)
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
        // A manual request can show progress for an already running automatic check.
        if (running) return
        running = true
        attemptToken = UpdateState.attempted()
        task = executor.submit {
            val result = runCatching { latestVersion() }
            activity.runOnUiThread {
                if (closed || activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                running = false
                // Keep known update information when the network request fails.
                if (result.isSuccess) UpdateState.remember(activity, result.getOrNull())
                onChanged()
                val showResult = progress?.isShowing == true
                progress?.dismiss()
                progress = null
                if (!showResult) return@runOnUiThread
                val current = AppLinks.version(activity)
                val latest = result.getOrNull()
                val message = when {
                    result.isFailure -> activity.getString(R.string.update_failed)
                    latest == null -> activity.getString(R.string.update_none)
                    else -> when (ReleaseVersion.isNewer(latest, current)) {
                        true -> activity.getString(R.string.update_available, latest, current)
                        false -> activity.getString(R.string.update_current, current)
                        null -> activity.getString(R.string.update_unknown, latest)
                    }
                }
                resultDialog?.dismiss()
                resultDialog = AlertDialog.Builder(activity)
                    .setTitle(R.string.menu_updates)
                    .setMessage(message)
                    .setPositiveButton(R.string.open_releases) { _, _ -> AppLinks.open(activity, AppLinks.RELEASES) }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        }
    }

    private fun latestVersion(): String? {
        val connection = URL(AppLinks.LATEST_API).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "GlobeKit")
            if (connection.responseCode == HttpURLConnection.HTTP_NOT_FOUND) return null
            if (connection.responseCode != HttpURLConnection.HTTP_OK) throw IOException("HTTP ${connection.responseCode}")
            val json = connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
            if (json.optBoolean("draft") || json.optBoolean("prerelease")) return null
            return json.getString("tag_name").takeIf { it.isNotBlank() }
        } finally {
            connection.disconnect()
        }
    }

    fun close() {
        closed = true
        progress?.dismiss()
        resultDialog?.dismiss()
        if (running) UpdateState.cancelAttempt(attemptToken)
        task?.cancel(true)
        executor.shutdownNow()
    }
}
