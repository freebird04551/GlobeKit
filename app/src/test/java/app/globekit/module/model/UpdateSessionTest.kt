package app.globekit.module.model

import org.junit.Assert.*
import org.junit.Test

class UpdateSessionTest {
    @Test fun disabledAutomaticChecksNeverRun() {
        assertFalse(UpdateAttempts().shouldCheck(false, 1))
    }

    @Test fun checksOncePerForegroundVisitWithoutTimeLimit() {
        val attempts = UpdateAttempts()
        assertTrue(attempts.shouldCheck(true, 1))
        attempts.started(1)
        assertFalse(attempts.shouldCheck(true, 1))
        assertTrue(attempts.shouldCheck(true, 2))
    }

    @Test fun internalNavigationAndRotationDoNotCountAsReopening() {
        val visits = ForegroundSessions()
        visits.started() // Home
        visits.started() // Settings starts before home stops
        visits.stopped(false)
        assertEquals(1L, visits.session)
        visits.stopped(true) // Theme recreation
        visits.started()
        assertEquals(1L, visits.session)
        visits.stopped(false) // App leaves foreground
        visits.started()
        assertEquals(2L, visits.session)
    }

    @Test fun cancellingAnOlderRequestCannotUndoANewerCheck() {
        val attempts = UpdateAttempts()
        val old = attempts.started(1)
        val latest = attempts.started(1)
        attempts.cancel(old)
        assertFalse(attempts.shouldCheck(true, 1))
        attempts.cancel(latest)
        assertTrue(attempts.shouldCheck(true, 1))
    }
}
