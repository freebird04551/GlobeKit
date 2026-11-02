package app.globekit.module

import android.app.Activity
import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import app.globekit.module.model.ReleaseVersion
import app.globekit.module.model.UpdateAttempts

/** Private UI state; never shared with hooked applications. */
object UpdateState {
    private val attempts = UpdateAttempts()
    fun preferences(context: Context) = context.getSharedPreferences("updates", Context.MODE_PRIVATE)

    fun automatic(context: Context) = preferences(context).getBoolean("automatic", true)

    fun setAutomatic(context: Context, enabled: Boolean) {
        preferences(context).edit().putBoolean("automatic", enabled).apply()
        if (enabled) attempts.reset()
    }

    fun shouldCheck(context: Context): Boolean = attempts.shouldCheck(
        automatic(context), GlobeKitApplication.session,
    )

    fun attempted(): Long = attempts.started(GlobeKitApplication.session)

    fun cancelAttempt(token: Long) = attempts.cancel(token)

    fun remember(context: Context, latest: String?) {
        preferences(context).edit().putString("latest", latest).apply()
    }

    fun available(activity: Activity): String? = preferences(activity).getString("latest", null)
        ?.takeIf { ReleaseVersion.isNewer(it, AppLinks.version(activity)) == true }

    fun versionDescription(activity: Activity): String =
        activity.getString(R.string.current_version) + " " + AppLinks.versionLabel(activity) + " " +
            (available(activity)?.let { activity.getString(R.string.version_update_hint, it) }
                ?: activity.getString(R.string.version_check_hint))

    fun versionLabel(activity: Activity): CharSequence {
        val version = AppLinks.versionLabel(activity)
        if (available(activity) == null) return version
        return SpannableString("$version  ●").apply {
            setSpan(ForegroundColorSpan(activity.getColor(R.color.update_badge)), length - 1, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}
