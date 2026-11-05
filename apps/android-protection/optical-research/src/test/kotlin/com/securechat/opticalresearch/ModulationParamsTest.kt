package com.securechat.opticalresearch

import com.securechat.opticalresearch.model.ModulationParams
import com.securechat.opticalresearch.model.ModulationSessionExtras
import com.securechat.opticalresearch.model.SpatialPatternType
import org.junit.Assert.*
import org.junit.Test

class ModulationParamsTest {

    @Test
    fun testBaselineValidationAndDescription() {
        val baseline = ModulationParams.Baseline
        assertNull("Baseline validation should always pass", baseline.validate())
        assertTrue("Baseline description should mention static", baseline.describe().contains("static", ignoreCase = true))
        assertEquals("baseline", ModulationSessionExtras.testModeFromParams(baseline))
    }

    @Test
    fun testSpatialValidationHappyPath() {
        val spatial = ModulationParams.Spatial(
            patternType = SpatialPatternType.CHECKERBOARD,
            gridPeriodPx = 16,
            contrastRatio = 0.08f,
            orientationDeg = 45f
        )
        assertNull("Valid spatial parameters should produce null error", spatial.validate())
        assertTrue(spatial.describe().contains("CHECKERBOARD"))
        assertEquals("spatial_modulation", ModulationSessionExtras.testModeFromParams(spatial))
    }

    @Test
    fun testSpatialValidationRejectsOutOfRange() {
        // Grid period too small
        val invalidPeriod = ModulationParams.Spatial(gridPeriodPx = 0)
        assertNotNull(invalidPeriod.validate())

        // Contrast ratio exceeding 15% safety limit
        val invalidContrastHigh = ModulationParams.Spatial(contrastRatio = 0.16f)
        assertNotNull(invalidContrastHigh.validate())
        assertTrue(invalidContrastHigh.validate()!!.contains("contrast", ignoreCase = true))

        // Negative contrast
        val invalidContrastNeg = ModulationParams.Spatial(contrastRatio = -0.01f)
        assertNotNull(invalidContrastNeg.validate())

        // Negative orientation
        val invalidOrientationNeg = ModulationParams.Spatial(orientationDeg = -1f)
        assertNotNull(invalidOrientationNeg.validate())

        // Orientation >= 180
        val invalidOrientationHigh = ModulationParams.Spatial(orientationDeg = 180f)
        assertNotNull(invalidOrientationHigh.validate())
    }

    @Test
    fun testTemporalValidationHappyPath() {
        val temporal = ModulationParams.Temporal(
            modulationFrequencyHz = 2.0,
            dutyCycle = 0.5f,
            amplitudeFraction = 0.08f
        )
        assertNull("Valid temporal parameters should produce null error", temporal.validate())
        assertTrue(temporal.describe().contains("2.0 Hz"))
        assertEquals("temporal_modulation", ModulationSessionExtras.testModeFromParams(temporal))
    }

    @Test
    fun testTemporalValidationEnforcesWcagCeiling() {
        // Exceeding 3.0 Hz WCAG photosensitivity safety ceiling
        val unsafeFreq = ModulationParams.Temporal(modulationFrequencyHz = 3.5)
        val error = unsafeFreq.validate()
        assertNotNull("Frequencies above 3.0 Hz must be rejected", error)
        assertTrue("Error should mention safety ceiling or WCAG", error!!.contains("3.0") || error.contains("WCAG"))

        // Zero or negative frequency
        val zeroFreq = ModulationParams.Temporal(modulationFrequencyHz = 0.0)
        assertNotNull(zeroFreq.validate())

        // Duty cycle out of bounds
        val zeroDuty = ModulationParams.Temporal(dutyCycle = 0f)
        assertNotNull(zeroDuty.validate())
        val fullDuty = ModulationParams.Temporal(dutyCycle = 1.0f)
        assertNotNull(fullDuty.validate())

        // Amplitude exceeding 15%
        val unsafeAmplitude = ModulationParams.Temporal(amplitudeFraction = 0.20f)
        assertNotNull(unsafeAmplitude.validate())
    }

    @Test
    fun testCombinedValidationDelegation() {
        val validCombined = ModulationParams.Combined()
        assertNull(validCombined.validate())
        assertEquals("combined_modulation", ModulationSessionExtras.testModeFromParams(validCombined))

        // Invalid spatial inside combined
        val invalidSpatialInCombined = ModulationParams.Combined(
            spatial = ModulationParams.Spatial(contrastRatio = 0.5f)
        )
        assertNotNull(invalidSpatialInCombined.validate())

        // Invalid temporal inside combined
        val invalidTemporalInCombined = ModulationParams.Combined(
            temporal = ModulationParams.Temporal(modulationFrequencyHz = 10.0)
        )
        assertNotNull(invalidTemporalInCombined.validate())
    }

    @Test
    fun testModulationSessionExtrasNotesRoundtrip() {
        val spatial = ModulationParams.Spatial(
            patternType = SpatialPatternType.DIAGONAL_LINES,
            gridPeriodPx = 12,
            contrastRatio = 0.10f
        )
        val extras = ModulationSessionExtras.from(spatial)

        val initialNotes = "Researcher: Phone A handheld at 30cm."
        val embeddedNotes = ModulationSessionExtras.embedInNotes(initialNotes, extras)

        assertTrue(embeddedNotes.startsWith("Researcher: Phone A handheld at 30cm."))
        assertTrue(embeddedNotes.contains("[[PHASE4B_EXTRAS]]"))

        val extracted = ModulationSessionExtras.extractFromNotes(embeddedNotes)
        assertNotNull(extracted)
        assertEquals("spatial_modulation", extracted!!.testMode)
        assertEquals(SpatialPatternType.DIAGONAL_LINES.name, extracted.spatialPattern)
        assertEquals(12, extracted.gridPeriodPx)
        assertEquals(0.10f, extracted.contrastRatio!!, 0.001f)
    }
}
