package app.globekit.module.model

class UpdateAttempts {
    private var checkedSession = -1L
    private var token = 0L

    fun shouldCheck(enabled: Boolean, session: Long): Boolean = enabled && checkedSession != session
    fun reset() { checkedSession = -1 }

    fun started(session: Long): Long {
        checkedSession = session
        return ++token
    }

    fun cancel(requestToken: Long) {
        if (token == requestToken) reset()
    }
}

/** Internal navigation and configuration changes stay in the same foreground visit. */
class ForegroundSessions {
    var session = 0L
        private set
    private var startedActivities = 0
    private var changingConfiguration = false

    fun started() {
        if (startedActivities == 0 && !changingConfiguration) session++
        changingConfiguration = false
        startedActivities++
    }

    fun stopped(configurationChange: Boolean) {
        startedActivities--
        if (startedActivities == 0) changingConfiguration = configurationChange
    }
}
