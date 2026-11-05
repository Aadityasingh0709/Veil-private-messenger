package com.securechat.opticalresearch.renderer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.securechat.opticalresearch.model.ModulationParams
import com.securechat.opticalresearch.renderer.RenderingDiagnostics

/**
 * Phase 4B — Mode 2: Temporal Modulation Renderer.
 *
 * Presents the reference image alternating between two luminance states (Phase A / Phase B)
 * in synchrony with the display's hardware VSYNC signal via Choreographer.
 *
 * Rationale:
 * Human temporal integration persists for ~1/15 s (flicker fusion ~15 Hz).
 * A camera shutter at 1/60–1/120 s captures only one phase of the alternation cycle,
 * potentially recording a dimmed or brightened version rather than the averaged appearance.
 * This renderer investigates whether that asymmetry produces measurable metric differences.
 *
 * Safety:
 * - Maximum modulation frequency hard-capped at [ModulationParams.Temporal.MAX_MODULATION_HZ] = 3 Hz.
 * - No high-contrast flashing; amplitude ceiling is 15% luminance perturbation.
 * - Emergency stop flag [emergencyStop] halts all modulation and falls back to static display.
 * - [diagnostics] records actual inter-frame intervals so post-analysis can detect dropped frames.
 *
 * Implements [OpticalRenderer]. Designed to be hosted by [OpticalSurfaceView].
 */
class TemporalModulationRenderer(
    private val experimentId: String,
    private var params: ModulationParams.Temporal = ModulationParams.Temporal(),
    val diagnostics: RenderingDiagnostics = RenderingDiagnostics()
) : OpticalRenderer {

    override val modeName: String = "Temporal Modulation (Phase 4B)"
    override val isModulationActive: Boolean get() = !emergencyStop

    /** Set to true to immediately halt all modulation and render static baseline. */
    @Volatile var emergencyStop: Boolean = false

    private var bitmap: Bitmap? = null
    private var surfaceWidth: Int = 0
    private var surfaceHeight: Int = 0

    private val srcRect = Rect()
    private val dstRect = RectF()

    private val marginPx = 60f
    private val markerRadiusPx = 36f

    // Paints
    private val bgPaint = Paint().apply { color = Color.rgb(245, 245, 245); style = Paint.Style.FILL }
    private val framePaint = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 3f; isAntiAlias = true }
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val overlayPaint = Paint().apply { style = Paint.Style.FILL }
    private val markerBlack = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL; isAntiAlias = true }
    private val markerWhite = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL; isAntiAlias = true }
    private val textPaint = Paint().apply {
        color = Color.rgb(100, 180, 255)
        textSize = 26f; isAntiAlias = true; typeface = android.graphics.Typeface.MONOSPACE
    }
    private val stopTextPaint = Paint().apply {
        color = Color.rgb(255, 80, 80)
        textSize = 30f; isAntiAlias = true; typeface = android.graphics.Typeface.MONOSPACE
    }

    /** Update parameters mid-session (safe to call from any thread). */
    fun updateParams(newParams: ModulationParams.Temporal) {
        params = newParams
    }

    override fun initialize(referenceBitmap: Bitmap, width: Int, height: Int) {
        this.bitmap = referenceBitmap
        this.surfaceWidth = width
        this.surfaceHeight = height

        srcRect.set(0, 0, referenceBitmap.width, referenceBitmap.height)

        val availWidth = width - marginPx * 2
        val availHeight = height - marginPx * 2 - 80f
        val imgAspect = referenceBitmap.width.toFloat() / referenceBitmap.height
        val availAspect = availWidth / availHeight

        val drawWidth: Float
        val drawHeight: Float
        if (imgAspect > availAspect) {
            drawWidth = availWidth; drawHeight = availWidth / imgAspect
        } else {
            drawHeight = availHeight; drawWidth = availHeight * imgAspect
        }

        val left = marginPx + (availWidth - drawWidth) / 2f
        val top = marginPx + 40f + (availHeight - drawHeight) / 2f
        dstRect.set(left, top, left + drawWidth, top + drawHeight)
    }

    override fun render(canvas: Canvas, frameTimeNanos: Long) {
        diagnostics.recordFrame(frameTimeNanos)

        canvas.drawRect(0f, 0f, surfaceWidth.toFloat(), surfaceHeight.toFloat(), bgPaint)

        // Render reference image
        bitmap?.let { canvas.drawBitmap(it, srcRect, dstRect, bitmapPaint) }

        if (!emergencyStop) {
            // Determine current phase: Phase A or Phase B
            val periodNs = (1_000_000_000.0 / params.modulationFrequencyHz).toLong()
            val phaseOffsetNs = (params.phaseOffsetFraction * periodNs).toLong()
            val positionInCycle = ((frameTimeNanos + phaseOffsetNs) % periodNs).toFloat() / periodNs
            val isPhaseA = positionInCycle < params.dutyCycle

            // Compute overlay alpha for the active phase
            val amplitude = params.amplitudeFraction.coerceIn(0f, ModulationParams.Temporal.MAX_AMPLITUDE)
            val overlayAlpha = (amplitude * 255).toInt()

            // Phase A: darken; Phase B: brighten (or vice-versa)
            val overlayColor = if (isPhaseA)
                Color.argb(overlayAlpha, 0, 0, 0)        // darken
            else
                Color.argb(overlayAlpha, 255, 255, 255)  // brighten

            overlayPaint.color = overlayColor
            canvas.save()
            canvas.clipRect(dstRect)
            canvas.drawRect(dstRect, overlayPaint)
            canvas.restore()

            // Status overlay
            val phaseLabel = if (isPhaseA) "PHASE-A" else "PHASE-B"
            val fps = diagnostics.recentMeanFps()
            canvas.drawText(
                "TEMPORAL • ${params.modulationFrequencyHz}Hz • ${(amplitude * 100).toInt()}% • $phaseLabel • FPS:${fps.toInt()}",
                marginPx + 16f, marginPx + 30f, textPaint
            )
        } else {
            // Emergency stop: render static + warning
            canvas.drawText("⏹ MODULATION STOPPED", marginPx + 16f, marginPx + 36f, stopTextPaint)
        }

        // Calibration frame and fiducials
        canvas.drawRect(marginPx, marginPx, surfaceWidth - marginPx, surfaceHeight - marginPx, framePaint)
        drawFiducials(canvas)
        canvas.drawText("SESSION: $experimentId", marginPx + 16f, surfaceHeight - marginPx - 16f, textPaint)
    }

    private fun drawFiducials(canvas: Canvas) {
        val corners = listOf(
            marginPx to marginPx,
            surfaceWidth - marginPx to marginPx,
            surfaceWidth - marginPx to surfaceHeight - marginPx,
            marginPx to surfaceHeight - marginPx
        )
        for ((_, c) in corners.withIndex()) {
            val cx = c.first; val cy = c.second
            val sz = markerRadiusPx + 8f
            canvas.drawRect(cx - sz, cy - sz, cx + sz, cy + sz, markerWhite)
            canvas.drawRect(cx - sz, cy - sz, cx + sz, cy + sz, framePaint)
            canvas.drawCircle(cx, cy, markerRadiusPx, markerBlack)
            canvas.drawCircle(cx, cy, markerRadiusPx * 0.65f, markerWhite)
            canvas.drawCircle(cx, cy, markerRadiusPx * 0.35f, markerBlack)
            canvas.drawCircle(cx, cy, 3f, markerWhite)
        }
    }

    override fun release() {
        bitmap = null
    }
}
