package app.globekit.module

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object AppLinks {
    private const val REPOSITORY = "freebird04551/GlobeKit"
    const val PROJECT = "https://github.com/$REPOSITORY"
    const val RELEASES = "$PROJECT/releases"
    const val LATEST_API = "https://api.github.com/repos/$REPOSITORY/releases/latest"

    fun version(activity: Activity): String =
        activity.packageManager.getPackageInfo(activity.packageName, 0).versionName ?: "unknown"

    fun versionLabel(activity: Activity): String {
        val info = activity.packageManager.getPackageInfo(activity.packageName, 0)
        return activity.getString(R.string.version_details, info.versionName ?: "unknown", info.longVersionCode)
    }

    fun open(activity: Activity, url: String) {
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(activity, R.string.no_browser, Toast.LENGTH_SHORT).show()
        }
    }
}
