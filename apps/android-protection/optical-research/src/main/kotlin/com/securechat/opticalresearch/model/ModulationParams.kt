package com.securechat.opticalresearch.model

/**
 * Sealed class hierarchy encapsulating the experimental parameters for each Phase 4B rendering mode.
 *
 * All modulation is opt-in. Default instances represent the OFF / safe state.
 * Safety limits are enforced as named constants and validated in [validate].
 *
 * Phase 4B does NOT produce adaptive protection; these are research parameters only.
 */
sealed class ModulationParams {

    /** Validates that all parameter values are within documented safety bounds. Returns error message or null. */
    abstract fun validate(): String?

    /** Human-readable description of the active parameters for session logging. */
    abstract fun describe(): String

    // -------------------------------------------------------------------------
    // MODE 0 — Normal baseline (no modulation)
    // -------------------------------------------------------------------------

    /**
     * Presents the reference chart exactly as in Phase 4A — static, unmodified.
     * Used as the control condition for every Phase 4B comparison.
     */
    data object Baseline : ModulationParams() {
        override fun validate(): String? = null
        override fun describe(): String = "mode=baseline"
    }

    // -------------------------------------------------------------------------
    // MODE 1 — Spatial modulation
    // -------------------------------------------------------------------------

    /**
     * Controlled spatial perturbation overlaid on the reference image.
     *
     * @param patternType  Which geometric pattern to overlay (see [SpatialPatternType]).
     * @param gridPeriodPx Size of the repeating unit in pixels.  Range: [2, 64].
     * @param orientationDeg Pattern rotation angle in degrees.  Range: [0, 180).
     * @param contrastRatio  Peak luminance perturbation as a fraction of full range [0, 1].
     *                       Capped at [MAX_CONTRAST_RATIO] for comfort.
     */
    data class Spatial(
        val patternType: SpatialPatternType = SpatialPatternType.HORIZONTAL_LINES,
        val gridPeriodPx: Int = DEFAULT_GRID_PERIOD_PX,
        val orientationDeg: Float = 0f,
        val contrastRatio: Float = DEFAULT_CONTRAST_RATIO
    ) : ModulationParams() {

        companion object {
            const val DEFAULT_GRID_PERIOD_PX = 8
            const val DEFAULT_CONTRAST_RATIO = 0.08f
            const val MAX_CONTRAST_RATIO = 0.15f   // ±15 % luminance perturbation ceiling
            const val MIN_GRID_PERIOD_PX = 2
            const val MAX_GRID_PERIOD_PX = 64
        }

        override fun validate(): String? {
            if (contrastRatio < 0f || contrastRatio > MAX_CONTRAST_RATIO)
                return "contrastRatio $contrastRatio exceeds safety ceiling $MAX_CONTRAST_RATIO"
            if (gridPeriodPx < MIN_GRID_PERIOD_PX || gridPeriodPx > MAX_GRID_PERIOD_PX)
                return "gridPeriodPx $gridPeriodPx out of range [$MIN_GRID_PERIOD_PX, $MAX_GRID_PERIOD_PX]"
            if (orientationDeg < 0f || orientationDeg >= 180f)
                return "orientationDeg $orientationDeg outside [0, 180)"
            return null
        }

        override fun describe(): String =
            "mode=spatial pattern=${patternType.name} period=${gridPeriodPx}px " +
            "orientation=${orientationDeg}deg contrast=${contrastRatio}"
    }

    // -------------------------------------------------------------------------
    // MODE 2 — Temporal modulation
    // -------------------------------------------------------------------------

    /**
     * Phase-alternating frame presentation synchronized with the hardware VSYNC signal.
     *
     * "Temporal modulation" here means alternating between two spatial phases of the reference
     * image (or two contrast states) on successive VSYNC frames. This exploits the difference
     * between human temporal integration (~1/15 s persistence of vision) and camera shutter
     * exposure (typically ≤ 1/60 s), which may capture one phase without temporal averaging.
     *
     * @param modulationFrequencyHz  Rate at which the phase alternates.
     *                               Minimum 0.5 Hz; maximum [MAX_MODULATION_HZ] Hz (conservative safety limit).
     * @param phaseOffsetFraction    Initial phase offset [0, 1) for the alternation cycle.
     * @param dutyCycle              Fraction of each cycle the "Phase A" frame is shown [0.1, 0.9].
     * @param amplitudeFraction      Relative difference between Phase A and Phase B luminance states [0, MAX_AMPLITUDE].
     */
    data class Temporal(
        val modulationFrequencyHz: Float = DEFAULT_FREQ_HZ,
        val phaseOffsetFraction: Float = 0f,
        val dutyCycle: Float = 0.5f,
        val amplitudeFraction: Float = DEFAULT_AMPLITUDE
    ) : ModulationParams() {

        companion object {
            const val DEFAULT_FREQ_HZ = 2.0f
            const val DEFAULT_AMPLITUDE = 0.08f
            /**
             * Hard safety ceiling for temporal modulation frequency.
             *
             * WCAG 2.1 / IEC 61966 photosensitivity guideline: flashing at 3 Hz or below is
             * generally considered safe (no more than 3 flashes per second). We use 3 Hz as
             * a strict ceiling to stay within documented comfort limits.
             * This ceiling is conservative; we do not claim it eliminates all discomfort risk.
             */
            const val MAX_MODULATION_HZ = 3.0f
            const val MAX_AMPLITUDE = 0.15f
        }

        override fun validate(): String? {
            if (modulationFrequencyHz <= 0f || modulationFrequencyHz > MAX_MODULATION_HZ)
                return "modulationFrequencyHz $modulationFrequencyHz outside (0, $MAX_MODULATION_HZ]"
            if (dutyCycle < 0.1f || dutyCycle > 0.9f)
                return "dutyCycle $dutyCycle outside [0.1, 0.9]"
            if (amplitudeFraction < 0f || amplitudeFraction > MAX_AMPLITUDE)
                return "amplitudeFraction $amplitudeFraction exceeds safety ceiling $MAX_AMPLITUDE"
            if (phaseOffsetFraction < 0f || phaseOffsetFraction >= 1f)
                return "phaseOffsetFraction $phaseOffsetFraction outside [0, 1)"
            return null
        }

        override fun describe(): String =
            "mode=temporal freq=${modulationFrequencyHz}Hz duty=${dutyCycle} " +
            "amplitude=${amplitudeFraction} phase=${phaseOffsetFraction}"
    }

    // -------------------------------------------------------------------------
    // MODE 3 — Combined spatial-temporal modulation
    // -------------------------------------------------------------------------

    /**
     * Combines spatial and temporal perturbations simultaneously.
     * The spatial pattern is alternated between two phases on each temporal beat,
     * allowing investigation of whether combined techniques outperform either alone.
     *
     * Parameters are individually validated to the same safety ceilings as the
     * standalone spatial and temporal modes.
     */
    data class Combined(
        val spatial: Spatial = Spatial(),
        val temporal: Temporal = Temporal()
    ) : ModulationParams() {
        override fun validate(): String? =
            spatial.validate() ?: temporal.validate()

        override fun describe(): String =
            "mode=combined [${spatial.describe()}] [${temporal.describe()}]"
    }
}

/**
 * Geometric pattern options for [ModulationParams.Spatial].
 * All patterns tile deterministically and produce no random or seed-dependent output.
 */
enum class SpatialPatternType(val displayLabel: String) {
    HORIZONTAL_LINES("Horizontal Lines"),
    VERTICAL_LINES("Vertical Lines"),
    DIAGONAL_LINES("Diagonal Lines"),
    CHECKERBOARD("Checkerboard Grid"),
    RADIAL_GRID("Radial Grid"),
    SINE_WAVE("Sine-Wave Luminance")
}
