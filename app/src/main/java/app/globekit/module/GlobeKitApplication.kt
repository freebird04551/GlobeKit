package app.globekit.module

import android.app.Activity
import android.app.Application
import android.os.Bundle
import app.globekit.module.model.ForegroundSessions

class GlobeKitApplication : Application() {
    companion object {
        private val visits = ForegroundSessions()
        val session: Long get() = visits.session
    }

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) = visits.started()
            override fun onActivityStopped(activity: Activity) = visits.stopped(activity.isChangingConfigurations)
            override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }
}
