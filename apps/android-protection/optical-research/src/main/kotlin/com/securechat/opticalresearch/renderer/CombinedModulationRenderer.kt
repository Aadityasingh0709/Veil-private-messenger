package com.securechat.opticalresearch.renderer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.RectF
import com.securechat.opticalresearch.model.ModulationParams
import com.securechat.opticalresearch.model.SpatialPatternType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Phase 4B — Mode 3: Combined Spatial and Temporal Modulation Renderer.
 *
 * Applies both a spatial luminance perturbation pattern AND a hardware-synchronized
 * temporal phase alternation simultaneously to the reference image.
 *
 * Rationale:
 * Spatial modulation tests sensitivity to high-frequency periodic patterns and moiré
 * interference with camera sensor color-filter arrays (CFAs). Temporal modulation tests
 * camera shutter phase capture vs human flicker fusion.
 * The combined mode evaluates whether simultaneous spatial-temporal modulation produces
 * synergistic degradation in camera captures while remaining tolerable to a human viewer.
 *
 * Safety:
 * - Hard temporal ceiling: [ModulationParams.Temporal.MAX_MODULATION_HZ] = 3.0 Hz.
 * - Hard spatial contrast ceiling: [ModulationParams.Spatial.MAX_CONTRAST_RATIO] = 15%.
 * - Hard temporal amplitude ceiling: [ModulationParams.Temporal.MAX_AMPLITUDE] = 15%.
 * - [emergencyStop] immediately halts temporal alternation and spatial overlay.
 * - [diagnostics] tracks Choreographer frame intervals for post-hoc validation.
 *
 * Implements [OpticalRenderer]. Designed for hosting in [OpticalSurfaceView].
 */
class CombinedModulationRenderer(
    private val experimentId: String,
    private var params: ModulationParams.Combined = ModulationParams.Combined(),
    val diagnostics: RenderingDiagnostics = RenderingDiagnostics()
) : OpticalRenderer {

    override val modeName: String = "Combined Modulation (Phase 4B)"
    override val isModulationActive: Boolean get() = !emergencyStop

    /** Emergency stop flag: halts modulation immediately, falling back to static baseline. */
    @Volatile var emergencyStop: Boolean = false

    private var bitmap: Bitmap? = null
    private var patternBitmap: Bitmap? = null
    private var surfaceWidth: Int = 0
    private var surfaceHeight: Int = 0

    private val srcRect = Rect()
    private val dstRect = RectF()

    private val marginPx = 60f
    private val markerRadiusPx = 36f

    // Pre-allocated paints
    private val bgPaint = Paint().apply { color = Color.rgb(245, 245, 245); style = Paint.Style.FILL }
    private val framePaint = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 3f; isAntiAlias = true }
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val patternPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val temporalOverlayPaint = Paint().apply { style = Paint.Style.FILL }
    private val markerBlack = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL; isAntiAlias = true }
    private val markerWhite = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL; isAntiAlias = true }

    // Amber/yellow HUD text for combined mode
    private val textPaint = Paint().apply {
        color = Color.rgb(255, 205, 60)
        textSize = 24f
        isAntiAlias = true
        typeface = android.graphics.Typeface.MONOSPACE
    }
    private val stopTextPaint = Paint().apply {
        color = Color.rgb(255, 80, 80)
        textSize = 30f
        isAntiAlias = true
        typeface = android.graphics.Typeface.MONOSPACE
    }

    /** Update full combined parameters. */
    fun updateParams(newParams: ModulationParams.Combined) {
        val spatialChanged = newParams.spatial != params.spatial
        params = newParams
        if (spatialChanged && surfaceWidth > 0 && surfaceHeight > 0) {
            buildPatternBitmap(surfaceWidth, surfaceHeight)
        }
    }

    /** Update spatial sub-parameters. */
    fun updateSpatialParams(spatial: ModulationParams.Spatial) {
        updateParams(params.copy(spatial = spatial))
    }

    /** Update temporal sub-parameters. */
    fun updateTemporalParams(temporal: ModulationParams.Temporal) {
        updateParams(params.copy(temporal = temporal))
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
            drawWidth = availWidth
            drawHeight = availWidth / imgAspect
        } else {
            drawHeight = availHeight
            drawWidth = availHeight * imgAspect
        }

        val left = marginPx + (availWidth - drawWidth) / 2f
        val top = marginPx + 40f + (availHeight - drawHeight) / 2f
        dstRect.set(left, top, left + drawWidth, top + drawHeight)

        buildPatternBitmap(width, height)
    }

    private fun buildPatternBitmap(w: Int, h: Int) {
        patternBitmap?.recycle()
        val spatial = params.spatial
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val period = spatial.gridPeriodPx
        val contrast = spatial.contrastRatio.coerceIn(0f, ModulationParams.Spatial.MAX_CONTRAST_RATIO)
        val darkenAlpha = (contrast * 255).toInt()
        val darkenColor = Color.argb(darkenAlpha, 0, 0, 0)
        val brightenColor = Color.argb(darkenAlpha, 255, 255, 255)

        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        when (spatial.patternType) {
            SpatialPatternType.HORIZONTAL_LINES -> {
                p.color = darkenColor
                var y = 0
                while (y < h) {
                    canvas.drawRect(0f, y.toFloat(), w.toFloat(), (y + period / 2).toFloat(), p)
                    y += period
                }
            }

            SpatialPatternType.VERTICAL_LINES -> {
                p.color = darkenColor
                var x = 0
                while (x < w) {
                    canvas.drawRect(x.toFloat(), 0f, (x + period / 2).toFloat(), h.toFloat(), p)
                    x += period
                }
            }

            SpatialPatternType.DIAGONAL_LINES -> {
                p.color = darkenColor
                p.strokeWidth = (period / 2).toFloat()
                p.style = Paint.Style.STROKE
                val diag = w + h
                var d = -diag
                while (d < diag) {
                    canvas.drawLine(d.toFloat(), 0f, (d + h).toFloat(), h.toFloat(), p)
                    d += period
                }
            }

            SpatialPatternType.CHECKERBOARD -> {
                val darkPaint = Paint().apply { color = darkenColor; style = Paint.Style.FILL }
                val brightPaint = Paint().apply { color = brightenColor; style = Paint.Style.FILL }
                var row = 0
                while (row * period < h) {
                    var col = 0
                    while (col * period < w) {
                        val paint = if ((row + col) % 2 == 0) darkPaint else brightPaint
                        canvas.drawRect(
                            (col * period).toFloat(), (row * period).toFloat(),
                            ((col + 1) * period).toFloat(), ((row + 1) * period).toFloat(),
                            paint
                        )
                        col++
                    }
                    row++
                }
            }

            SpatialPatternType.RADIAL_GRID -> {
                p.color = darkenColor
                p.style = Paint.Style.STROKE
                p.strokeWidth = (period / 2).toFloat()
                val cx = w / 2f
                val cy = h / 2f
                val maxR = kotlin.math.sqrt((cx * cx + cy * cy).toDouble()).toFloat()
                var r = period.toFloat()
                while (r < maxR) {
                    canvas.drawCircle(cx, cy, r, p)
                    r += period
                }
            }

            SpatialPatternType.SINE_WAVE -> {
                for (row in 0 until h) {
                    val sinVal = sin(2.0 * PI * row / period).toFloat()
                    val alpha = (abs(sinVal) * darkenAlpha).toInt()
                    val lineColor = if (sinVal >= 0f) Color.argb(alpha, 0, 0, 0)
                    else Color.argb(alpha, 255, 255, 255)
                    p.color = lineColor
                    canvas.drawLine(0f, row.toFloat(), w.toFloat(), row.toFloat(), p)
                }
            }
        }

        val finalBmp = if (spatial.orientationDeg != 0f) {
            val matrix = Matrix()
            matrix.postRotate(spatial.orientationDeg, w / 2f, h / 2f)
            val rotated = Bitmap.createBitmap(bmp, 0, 0, w, h, matrix, true)
            bmp.recycle()
            rotated
        } else bmp

        patternBitmap = finalBmp
    }

    override fun render(canvas: Canvas, frameTimeNanos: Long) {
        diagnostics.recordFrame(frameTimeNanos)

        canvas.drawRect(0f, 0f, surfaceWidth.toFloat(), surfaceHeight.toFloat(), bgPaint)

        // 1. Reference bitmap
        bitmap?.let { canvas.drawBitmap(it, srcRect, dstRect, bitmapPaint) }

        if (!emergencyStop) {
            // 2. Spatial modulation overlay
            canvas.save()
            canvas.clipRect(dstRect)
            patternBitmap?.let { canvas.drawBitmap(it, 0f, 0f, patternPaint) }

            // 3. Temporal modulation overlay
            val temporal = params.temporal
            val periodNs = (1_000_000_000.0 / temporal.modulationFrequencyHz).toLong()
            val phaseOffsetNs = (temporal.phaseOffsetFraction * periodNs).toLong()
            val positionInCycle = ((frameTimeNanos + phaseOffsetNs) % periodNs).toFloat() / periodNs
            val isPhaseA = positionInCycle < temporal.dutyCycle

            val amplitude = temporal.amplitudeFraction.coerceIn(0f, ModulationParams.Temporal.MAX_AMPLITUDE)
            val overlayAlpha = (amplitude * 255).toInt()
            val overlayColor = if (isPhaseA)
                Color.argb(overlayAlpha, 0, 0, 0)
            else
                Color.argb(overlayAlpha, 255, 255, 255)

            temporalOverlayPaint.color = overlayColor
            canvas.drawRect(dstRect, temporalOverlayPaint)
            canvas.restore()

            // HUD Status
            val phaseLabel = if (isPhaseA) "A" else "B"
            val fps = diagnostics.recentMeanFps()
            val spatLabel = "${params.spatial.patternType.displayLabel.take(6)} ${params.spatial.gridPeriodPx}px"
            val tempLabel = "${temporal.modulationFrequencyHz}Hz-$phaseLabel"
            canvas.drawText(
                "COMBINED • $spatLabel • $tempLabel • FPS:${fps.toInt()}",
                marginPx + 16f, marginPx + 30f, textPaint
            )
        } else {
            // Emergency stop active: static baseline with warning
            canvas.drawText("⏹ MODULATION STOPPED", marginPx + 16f, marginPx + 36f, stopTextPaint)
        }

        // Calibration frame
        canvas.drawRect(marginPx, marginPx, surfaceWidth - marginPx, surfaceHeight - marginPx, framePaint)

        // Fiducials
        drawFiducials(canvas)

        // Session metadata footer
        canvas.drawText(
            "SESSION: $experimentId",
            marginPx + 16f, surfaceHeight - marginPx - 16f, textPaint
        )
    }

    private fun drawFiducials(canvas: Canvas) {
        val corners = listOf(
            Pair(marginPx, marginPx),
            Pair(surfaceWidth - marginPx, marginPx),
            Pair(marginPx, surfaceHeight - marginPx),
            Pair(surfaceWidth - marginPx, surfaceHeight - marginPx)
        )
        for ((cx, cy) in corners) {
            canvas.drawCircle(cx, cy, markerRadiusPx, markerBlack)
            canvas.drawCircle(cx, cy, markerRadiusPx * 0.6f, markerWhite)
            canvas.drawCircle(cx, cy, markerRadiusPx * 0.2f, markerBlack)
        }
    }

    override fun release() {
        patternBitmap?.recycle()
        patternBitmap = null
        bitmap = null
    }
}
