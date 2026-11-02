package app.globekit.module

import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import java.lang.ref.WeakReference

/** The service helper must be registered once per process, not once per Activity. */
object XposedConnection : XposedServiceHelper.OnServiceListener {
    @Volatile private var service: XposedService? = null
    @Volatile private var observer: WeakReference<XposedServiceHelper.OnServiceListener>? = null
    private var registered = false

    fun bind(listener: XposedServiceHelper.OnServiceListener) {
        observer = WeakReference(listener)
        if (!registered) {
            XposedServiceHelper.registerListener(this)
            registered = true
        }
        service?.let(listener::onServiceBind)
    }

    fun unbind(listener: XposedServiceHelper.OnServiceListener) {
        if (observer?.get() === listener) observer = null
    }

    override fun onServiceBind(service: XposedService) {
        this.service = service
        observer?.get()?.onServiceBind(service)
    }

    override fun onServiceDied(service: XposedService) {
        if (this.service === service) {
            this.service = null
            observer?.get()?.onServiceDied(service)
        }
    }
}
