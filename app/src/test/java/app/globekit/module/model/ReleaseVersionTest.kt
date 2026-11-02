package app.globekit.module.model

import org.junit.Assert.*
import org.junit.Test

class ReleaseVersionTest {
    @Test fun comparesNumericComponentsInsteadOfText() {
        assertEquals(true, ReleaseVersion.isNewer("v1.0.10", "1.0.9"))
        assertEquals(false, ReleaseVersion.isNewer("1.0.9", "1.0.10"))
    }
    @Test fun equalVersionsAndMissingZeroComponentsAreNotUpdates() {
        assertEquals(false, ReleaseVersion.isNewer("v1.0.4", "1.0.4"))
        assertEquals(false, ReleaseVersion.isNewer("1.0", "1.0.0"))
    }
    @Test fun comparesMajorAndMinorVersions() {
        assertEquals(true, ReleaseVersion.isNewer("2.0", "1.99.99"))
        assertEquals(true, ReleaseVersion.isNewer("1.1", "1.0.99"))
    }
    @Test fun unrecognizedVersionsRequireManualReview() {
        assertNull(ReleaseVersion.isNewer("release-unknown", "1.0.4"))
        assertNull(ReleaseVersion.isNewer("1.1-beta", "1.0.4"))
        assertNull(ReleaseVersion.isNewer("999999999999999999999999", "1.0.4"))
    }
}
