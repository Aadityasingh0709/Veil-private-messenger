package com.securechat.opticalresearch

import com.securechat.opticalresearch.model.*
import org.junit.Assert.*
import org.junit.Test

class ManifestSerializationTest {

    @Test
    fun testCompleteManifestSerialization() {
        val expId = "EXP-20261103-120000-TEST01"
        val session = ExperimentSession(
            experimentId = expId,
            timestamp = "2026-11-03T12:00:00Z",
            testMode = "baseline",
            sourceImage = SourceImageDetails(
                name = "chart_combined_quality.png",
                hashSha256 = "a".repeat(64),
                width = 1080,
                height = 1920
            ),
            phoneA = PhoneADeviceInfo(
                manufacturer = "Xiaomi",
                model = "2312DRAABI",
                androidVersion = "15",
                apiLevel = 35,
                displayResolution = "1080x2400",
                refreshRateHz = 120.0f,
                supportedRefreshRates = listOf(60.0f, 120.0f),
                screenBrightnessPercent = 85.0f,
                adaptiveBrightness = false,
                screenOrientation = "PORTRAIT"
            ),
            phoneB = PhoneBCaptureInfo(
                manufacturer = "Google",
                model = "Pixel 8",
                cameraResolution = "12MP",
                exposureTimeSeconds = "1/120",
                iso = 100,
                focusMode = "FIXED",
                whiteBalance = "5500K",
                distanceCm = 40.0f,
                cameraAngleDegrees = 90.0f,
                ambientLighting = "Darkened lab",
                captureMode = "manual"
            ),
            captures = listOf(
                CaptureItem(
                    id = "capture_001.jpg",
                    filename = "capture_001.jpg",
                    absolutePath = "/tmp/capture_001.jpg",
                    hashSha256 = "b".repeat(64),
                    repetitionNumber = 1,
                    timestamp = "2026-11-03T12:01:00Z",
                    exif = ExifMetadata(make = "Google", model = "Pixel 8", iso = "100")
                )
            ),
            notes = "Test notes"
        )

        val manifest = ExperimentManifest.fromSession(session)
        val jsonStr = manifest.toJson()

        assertNotNull(jsonStr)
        assertTrue(jsonStr.contains("\"schemaVersion\": \"1.0.0\""))
        assertTrue(jsonStr.contains("\"experimentId\": \"$expId\""))
        assertTrue(jsonStr.contains("\"testMode\": \"baseline\""))
        assertTrue(jsonStr.contains("chart_combined_quality.png"))

        val parsed = ExperimentManifest.fromJson(jsonStr)
        assertEquals("1.0.0", parsed.schemaVersion)
        assertEquals(expId, parsed.experimentId)
        assertEquals("baseline", parsed.testMode)
        assertEquals(1, parsed.captures.size)
        assertEquals("capture_001.jpg", parsed.captures[0].filename)
        assertEquals("Pixel 8", parsed.phoneB.model)
    }

    @Test
    fun testMissingMetadataHandling() {
        val expId = "EXP-20261103-120000-EMPTY"
        val session = ExperimentSession(
            experimentId = expId,
            sourceImage = SourceImageDetails(
                name = "reference.png",
                hashSha256 = "c".repeat(64),
                width = 1080,
                height = 1920
            ),
            phoneA = PhoneADeviceInfo(), // all nulls
            phoneB = PhoneBCaptureInfo(), // all nulls / defaults
            captures = emptyList()
        )

        val manifest = ExperimentManifest.fromSession(session)
        val jsonStr = manifest.toJson()
        assertNotNull(jsonStr)

        val parsed = ExperimentManifest.fromJson(jsonStr)
        assertEquals("1.0.0", parsed.schemaVersion)
        assertEquals(expId, parsed.experimentId)
        assertNull(parsed.phoneA.manufacturer)
        assertNull(parsed.phoneB.distanceCm)
        assertTrue(parsed.captures.isEmpty())
    }
}
