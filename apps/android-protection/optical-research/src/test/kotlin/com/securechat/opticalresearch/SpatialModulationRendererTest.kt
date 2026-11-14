package com.securechat.opticalresearch

import com.securechat.opticalresearch.model.*
import com.securechat.opticalresearch.renderer.SpatialModulationRenderer
import com.securechat.opticalresearch.renderer.SpatialPatternMath
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit test suite for Phase 4B.1 Spatial Modulation Prototype.
 *
 * Verifies:
 * 1. Parameter changes and validation bounds.
 * 2. Effect enable/disable toggle and baseline restoration.
 * 3. Emergency stop control and recovery.
 * 4. Resource cleanup and safety.
 * 5. Rendering output calculations (aspect ratio fit, pattern math).
 * 6. Experiment manifest recording of actual parameters.
 */
class SpatialModulationRendererTest {

    @Test
    fun testDefaultParametersAndState() {
        val renderer = SpatialModulationRenderer("EXP-TEST-001")
        val params = renderer.getParams()

        assertEquals(SpatialPatternType.HORIZONTAL_LINES, params.patternType)
        assertEquals(8, params.gridPeriodPx)
        assertEquals(0.08f, params.contrastRatio, 0.001f)
        assertEquals(0f, params.orientationDeg, 0.001f)

        assertTrue("Modulation effect should be enabled by default", renderer.isEffectEnabled)
        assertFalse("Emergency stop should be false initially", renderer.emergencyStop)
        assertTrue("Modulation should be active by default", renderer.isModulationActive)
        assertTrue(renderer.modeName.contains("Spatial Modulation"))
    }

    @Test
    fun testParameterChanges() {
        val renderer = SpatialModulationRenderer("EXP-TEST-002")

        val newParams = ModulationParams.Spatial(
            patternType = SpatialPatternType.CHECKERBOARD,
            gridPeriodPx = 16,
            orientationDeg = 45f,
            contrastRatio = 0.12f
        )
        renderer.updateParams(newParams)

        val updated = renderer.getParams()
        assertEquals(SpatialPatternType.CHECKERBOARD, updated.patternType)
        assertEquals(16, updated.gridPeriodPx)
        assertEquals(45f, updated.orientationDeg, 0.001f)
        assertEquals(0.12f, updated.contrastRatio, 0.001f)
        assertNull("Valid parameters should pass validation", updated.validate())
    }

    @Test
    fun testEffectToggleAndRestoration() {
        val renderer = SpatialModulationRenderer("EXP-TEST-003")

        // 1. Initial: active
        assertTrue(renderer.isModulationActive)

        // 2. Disable: falls back to Normal baseline mode
        renderer.isEffectEnabled = false
        assertFalse("Effect disabled should deactivate modulation", renderer.isEffectEnabled)
        assertFalse("isModulationActive should be false when effect is disabled", renderer.isModulationActive)

        // 3. Restoration: re-enabling effect restores spatial modulation
        renderer.isEffectEnabled = true
        assertTrue("Restoration should re-enable effect", renderer.isEffectEnabled)
        assertTrue("Restoration should re-activate modulation", renderer.isModulationActive)
    }

    @Test
    fun testEmergencyStopAndRestoration() {
        val renderer = SpatialModulationRenderer("EXP-TEST-004")

        // 1. Initial state
        assertTrue(renderer.isModulationActive)

        // 2. Trigger emergency stop
        renderer.stopModulation()
        assertTrue("Emergency stop flag must be true", renderer.emergencyStop)
        assertFalse("Modulation must be inactive on emergency stop", renderer.isModulationActive)

        // 3. Restoration after stop
        renderer.restoreModulation()
        assertFalse("Emergency stop should be cleared", renderer.emergencyStop)
        assertTrue("Modulation should be active after restoration", renderer.isModulationActive)
    }

    @Test
    fun testResourceCleanup() {
        val renderer = SpatialModulationRenderer("EXP-TEST-005")

        // Calling release should complete without exception
        renderer.release()

        // Multiple calls to release should be idempotent and safe
        renderer.release()
    }

    @Test
    fun testAspectRatioPreservationFitCenter() {
        // Test wide image on tall screen (letterbox)
        val wideFit = SpatialPatternMath.calculateFitCenterRect(
            imageWidth = 1920,
            imageHeight = 1080,
            surfaceWidth = 1080,
            surfaceHeight = 2400,
            marginPx = 60f,
            reservedVerticalPx = 80f
        )

        val expectedAspect = 1920f / 1080f
        val actualAspect = wideFit.width / wideFit.height
        assertEquals("Image aspect ratio must be preserved exactly", expectedAspect, actualAspect, 0.01f)
        assertTrue("Width should fit within margins", wideFit.width <= 1080 - 120)
        assertTrue("Left margin should be at least marginPx", wideFit.left >= 60f)

        // Test tall image on square screen (pillarbox)
        val tallFit = SpatialPatternMath.calculateFitCenterRect(
            imageWidth = 1080,
            imageHeight = 2400,
            surfaceWidth = 1080,
            surfaceHeight = 1080,
            marginPx = 60f,
            reservedVerticalPx = 80f
        )
        val expectedTallAspect = 1080f / 2400f
        val actualTallAspect = tallFit.width / tallFit.height
        assertEquals("Tall image aspect ratio must be preserved exactly", expectedTallAspect, actualTallAspect, 0.01f)
    }

    @Test
    fun testSpatialPatternMathLuminancePerturbations() {
        val period = 8
        val contrast = 0.10f

        // 1. HORIZONTAL_LINES: y=0..3 should darken (-contrast), y=4..7 should be 0f
        val h0 = SpatialPatternMath.computeLuminancePerturbation(0, 0, 100, 100, SpatialPatternType.HORIZONTAL_LINES, period, contrast)
        val h4 = SpatialPatternMath.computeLuminancePerturbation(0, 4, 100, 100, SpatialPatternType.HORIZONTAL_LINES, period, contrast)
        assertEquals(-contrast, h0, 0.001f)
        assertEquals(0f, h4, 0.001f)

        // 2. VERTICAL_LINES: x=0..3 should darken, x=4..7 should be 0f
        val v0 = SpatialPatternMath.computeLuminancePerturbation(0, 0, 100, 100, SpatialPatternType.VERTICAL_LINES, period, contrast)
        val v4 = SpatialPatternMath.computeLuminancePerturbation(4, 0, 100, 100, SpatialPatternType.VERTICAL_LINES, period, contrast)
        assertEquals(-contrast, v0, 0.001f)
        assertEquals(0f, v4, 0.001f)

        // 3. CHECKERBOARD: alternates between -contrast and +contrast
        val c00 = SpatialPatternMath.computeLuminancePerturbation(0, 0, 100, 100, SpatialPatternType.CHECKERBOARD, period, contrast)
        val c10 = SpatialPatternMath.computeLuminancePerturbation(period, 0, 100, 100, SpatialPatternType.CHECKERBOARD, period, contrast)
        assertEquals(-contrast, c00, 0.001f)
        assertEquals(contrast, c10, 0.001f)

        // 4. SINE_WAVE: smooth sinusoidal modulation bounded within [-contrast, +contrast]
        for (y in 0 until period) {
            val sineVal = SpatialPatternMath.computeLuminancePerturbation(0, y, 100, 100, SpatialPatternType.SINE_WAVE, period, contrast)
            assertTrue("Sine wave value $sineVal must be within [-$contrast, $contrast]", sineVal in -contrast..contrast)
        }

        // 5. RADIAL_GRID: concentric rings
        val rCenter = SpatialPatternMath.computeLuminancePerturbation(50, 50, 100, 100, SpatialPatternType.RADIAL_GRID, period, contrast)
        assertEquals(-contrast, rCenter, 0.001f)
    }

    @Test
    fun testConservativeIntensityLimitsEnforced() {
        // Attempting to exceed 15% contrast ceiling must be clamped in math
        val excessContrast = 0.25f
        val valClamped = SpatialPatternMath.computeLuminancePerturbation(
            0, 0, 100, 100,
            SpatialPatternType.CHECKERBOARD,
            8,
            excessContrast
        )
        assertEquals(
            "Contrast should be clamped to MAX_CONTRAST_RATIO (0.15)",
            -ModulationParams.Spatial.MAX_CONTRAST_RATIO,
            valClamped,
            0.001f
        )
    }

    @Test
    fun testManifestRecordingOfSpatialParameters() {
        val spatial = ModulationParams.Spatial(
            patternType = SpatialPatternType.DIAGONAL_LINES,
            gridPeriodPx = 12,
            contrastRatio = 0.10f,
            orientationDeg = 45f
        )

        // Active spatial modulation session extras
        val extrasEnabled = ModulationSessionExtras.from(spatial, isEffectEnabled = true)
        assertEquals("spatial_modulation", extrasEnabled.testMode)
        assertEquals("spatial", extrasEnabled.modulationType)
        assertEquals("DIAGONAL_LINES", extrasEnabled.spatialPattern)
        assertEquals(12, extrasEnabled.gridPeriodPx)
        assertEquals(0.10f, extrasEnabled.contrastRatio!!, 0.001f)
        assertEquals(45f, extrasEnabled.orientationDeg!!, 0.001f)
        assertTrue(extrasEnabled.isEffectEnabled!!)

        // Disabled spatial modulation session extras (Normal display mode)
        val extrasDisabled = ModulationSessionExtras.from(spatial, isEffectEnabled = false)
        assertEquals("baseline", extrasDisabled.testMode)
        assertEquals("none", extrasDisabled.modulationType)
        assertFalse(extrasDisabled.isEffectEnabled!!)

        // Manifest serialization test
        val session = ExperimentSession(
            experimentId = "EXP-20261113-120000-SPAT01",
            testMode = extrasEnabled.testMode,
            sourceImage = SourceImageDetails(
                name = "chart_combined_quality.png",
                hashSha256 = "d".repeat(64),
                width = 1080,
                height = 1920
            ),
            phoneA = PhoneADeviceInfo(manufacturer = "Xiaomi", model = "2312DRAABI"),
            notes = ModulationSessionExtras.embedInNotes("Phase 4B.1 experiment", extrasEnabled)
        )

        val manifest = ExperimentManifest.fromSession(session)
        val json = manifest.toJson()

        assertNotNull(json)
        assertTrue(json.contains("\"testMode\": \"spatial_modulation\""))
        assertTrue(json.contains("\"modulationType\": \"spatial\""))
        assertTrue(json.contains("\"spatialPattern\": \"DIAGONAL_LINES\""))
        assertTrue(json.contains("\"gridPeriodPx\": 12"))
        assertTrue(json.contains("\"contrastRatio\": 0.1"))

        // Roundtrip parsing
        val parsed = ExperimentManifest.fromJson(json)
        assertEquals("spatial_modulation", parsed.testMode)
        val modParams = parsed.modulationParams
        assertNotNull(modParams)
        assertEquals("spatial", modParams!!.modulationType)
        assertEquals("DIAGONAL_LINES", modParams.spatialPattern)
        assertEquals(12, modParams.gridPeriodPx)
    }

    @Test
    fun testInvalidSpatialParameterRejection() {
        val invalidContrast = ModulationParams.Spatial(contrastRatio = 0.20f)
        assertNotNull("Contrast > 0.15 must be rejected", invalidContrast.validate())

        val negativeContrast = ModulationParams.Spatial(contrastRatio = -0.05f)
        assertNotNull("Negative contrast must be rejected", negativeContrast.validate())

        val smallPeriod = ModulationParams.Spatial(gridPeriodPx = 1)
        assertNotNull("Period < 2px must be rejected", smallPeriod.validate())

        val largePeriod = ModulationParams.Spatial(gridPeriodPx = 128)
        assertNotNull("Period > 64px must be rejected", largePeriod.validate())

        val invalidOrientation = ModulationParams.Spatial(orientationDeg = 185f)
        assertNotNull("Orientation >= 180 deg must be rejected", invalidOrientation.validate())
    }

    @Test
    fun testPatternDeterminism() {
        val pattern = SpatialPatternType.CHECKERBOARD
        val period = 16
        val contrast = 0.10f

        // Same coordinates and parameters must always yield identical perturbation
        for (i in 0 until 50) {
            val v1 = SpatialPatternMath.computeLuminancePerturbation(25, 42, 1080, 1920, pattern, period, contrast)
            val v2 = SpatialPatternMath.computeLuminancePerturbation(25, 42, 1080, 1920, pattern, period, contrast)
            assertEquals("Pattern math must be 100% deterministic", v1, v2, 0.0f)
        }
    }
}
