package com.securechat.opticalresearch.renderer

import com.securechat.opticalresearch.model.ModulationParams
import com.securechat.opticalresearch.model.SpatialPatternType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure mathematical functions for Phase 4B.1 Spatial Modulation patterns and geometry.
 *
 * Fully decoupled from Android Canvas/Bitmap to allow deterministic unit testing of:
 * - Geometric pattern perturbation calculations
 * - Aspect ratio fit center calculations
 * - Safe contrast boundary checks
 */
object SpatialPatternMath {

    /**
     * Represents the calculated destination rectangle for aspect-ratio-preserving fit center.
     */
    data class FitRect(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        val width: Float,
        val height: Float
    )

    /**
     * Computes destination rectangle preserving image aspect ratio within available surface dimensions.
     */
    fun calculateFitCenterRect(
        imageWidth: Int,
        imageHeight: Int,
        surfaceWidth: Int,
        surfaceHeight: Int,
        marginPx: Float = 60f,
        reservedVerticalPx: Float = 80f
    ): FitRect {
        require(imageWidth > 0 && imageHeight > 0) { "Image dimensions must be positive" }
        require(surfaceWidth > 0 && surfaceHeight > 0) { "Surface dimensions must be positive" }

        val availWidth = surfaceWidth - marginPx * 2f
        val availHeight = surfaceHeight - marginPx * 2f - reservedVerticalPx

        val imgAspect = imageWidth.toFloat() / imageHeight.toFloat()
        val availAspect = availWidth / availHeight

        val drawWidth: Float
        val drawHeight: Float
        if (imgAspect > availAspect) {
            drawWidth = availWidth
            drawHeight = availWidth / imgAspect
        } else {
            drawHeight = availHeight
            drawWidth = availHeight * imgAspect
        }

        val left = marginPx + (availWidth - drawWidth) / 2f
        val top = marginPx + 40f + (availHeight - drawHeight) / 2f
        return FitRect(
            left = left,
            top = top,
            right = left + drawWidth,
            bottom = top + drawHeight,
            width = drawWidth,
            height = drawHeight
        )
    }

    /**
     * Computes the normalized luminance perturbation at coordinate (x, y) for a given pattern.
     *
     * Returns a float in [-contrastRatio, +contrastRatio].
     * Negative values indicate darkening; positive values indicate brightening.
     */
    fun computeLuminancePerturbation(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        patternType: SpatialPatternType,
        gridPeriodPx: Int,
        contrastRatio: Float
    ): Float {
        val safeContrast = contrastRatio.coerceIn(0f, ModulationParams.Spatial.MAX_CONTRAST_RATIO)
        val period = gridPeriodPx.coerceIn(
            ModulationParams.Spatial.MIN_GRID_PERIOD_PX,
            ModulationParams.Spatial.MAX_GRID_PERIOD_PX
        )

        return when (patternType) {
            SpatialPatternType.HORIZONTAL_LINES -> {
                val cyclePos = (y % period + period) % period
                if (cyclePos < period / 2) -safeContrast else 0f
            }

            SpatialPatternType.VERTICAL_LINES -> {
                val cyclePos = (x % period + period) % period
                if (cyclePos < period / 2) -safeContrast else 0f
            }

            SpatialPatternType.DIAGONAL_LINES -> {
                val d = ((x + y) % period + period) % period
                if (d < period / 2) -safeContrast else 0f
            }

            SpatialPatternType.CHECKERBOARD -> {
                val col = x / period
                val row = y / period
                if ((row + col) % 2 == 0) -safeContrast else safeContrast
            }

            SpatialPatternType.RADIAL_GRID -> {
                val cx = width / 2.0
                val cy = height / 2.0
                val dx = x - cx
                val dy = y - cy
                val dist = sqrt(dx * dx + dy * dy)
                val ring = (dist.toInt() % period + period) % period
                if (ring < period / 2) -safeContrast else 0f
            }

            SpatialPatternType.SINE_WAVE -> {
                val sinVal = sin(2.0 * PI * y / period).toFloat()
                sinVal * safeContrast
            }
        }
    }
}
