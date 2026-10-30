package com.securechat.protection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectionStatusTest {

    @Test
    fun `protection status correctly represents properties`() {
        val status = ProtectionStatus(
            isWindowSecureActive = true,
            activeSessionsCount = 2,
            wasPreExistingSecure = false,
            isScreenshotDetectionSupported = true,
            isScreenshotDetectionRegistered = true,
            androidApiLevel = 34,
            platformNotes = "Test note"
        )

        assertTrue(status.isWindowSecureActive)
        assertEquals(2, status.activeSessionsCount)
        assertFalse(status.wasPreExistingSecure)
        assertTrue(status.isScreenshotDetectionSupported)
        assertTrue(status.isScreenshotDetectionRegistered)
        assertEquals(34, status.androidApiLevel)
        assertEquals("Test note", status.platformNotes)
    }

    @Test
    fun `default config has preservePreExistingFlags and autoRestore enabled`() {
        val config = ProtectionConfig()
        assertTrue(config.preservePreExistingFlags)
        assertTrue(config.autoRestoreOnRelease)
    }
}
