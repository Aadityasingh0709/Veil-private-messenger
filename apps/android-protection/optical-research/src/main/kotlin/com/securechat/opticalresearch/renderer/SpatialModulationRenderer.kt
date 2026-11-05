package com.securechat.opticalresearch.renderer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import com.securechat.opticalresearch.model.ModulationParams
import com.securechat.opticalresearch.model.SpatialPatternType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Phase 4B — Mode 1: Spatial Modulation Renderer.
 *
 * Renders the reference image with a controlled, deterministic spatial luminance
 * perturbation overlaid using alpha compositing. The perturbation pattern is fully
 * parameterized via [ModulationParams.Spatial].
 *
 * Design decisions:
 * - Pattern is rendered to a recycled off-screen [Bitmap] once per configuration change,
 *   then composited at render time using [PorterDuff.Mode.SRC_OVER] with per-pixel alpha.
 * - No random seed; patterns are purely deterministic given width, height, and params.
 * - Modulation is additive/subtractive luminance perturbation, not color shift.
 * - Spatial patterns do NOT flash or change over time — that is [TemporalModulationRenderer]'s domain.
 * - Safety: contrast is capped at [ModulationParams.Spatial.MAX_CONTRAST_RATIO].
 *
 * Implements [SpatialModulationRenderer] from OpticalRenderer.kt.
 */
class SpatialModulationRenderer(
    private val experimentId: String,
    private var params: ModulationParams.Spatial = ModulationParams.Spatial()
) : OpticalRenderer {

    override val modeName: String = "Spatial Modulation (Phase 4B)"
    override val isModulationActive: Boolean = true

    private var bitmap: Bitmap? = null
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
    private val markerBlack = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL; isAntiAlias = true }
    private val markerWhite = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL; isAntiAlias = true }
    private val textPaint = Paint().apply {
        color = Color.rgb(100, 220, 100)
        textSize = 26f; isAntiAlias = true; typeface = android.graphics.Typeface.MONOSPACE
    }

    // Off-screen pattern bitmap (recycled on param change)
    private var patternBitmap: Bitmap? = null
    private var patternCanvas: Canvas? = null
    private val patternPaint = Paint().apply {
        isAntiAlias = false
        // The pattern is composed using SRC_OVER at reduced alpha; alpha is baked into pixels.
    }

    /** Update parameters mid-session. Triggers pattern regeneration on next initialize. */
    fun updateParams(newParams: ModulationParams.Spatial) {
        params = newParams
        // Pattern will be regenerated in renderFrame by checking dimensions
        patternBitmap?.recycle()
        patternBitmap = null
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

        // Build pattern bitmap
        buildPatternBitmap(width, height)
    }

    private fun buildPatternBitmap(w: Int, h: Int) {
        patternBitmap?.recycle()
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val period = params.gridPeriodPx
        val contrast = params.contrastRatio.coerceIn(0f, ModulationParams.Spatial.MAX_CONTRAST_RATIO)
        val darkenAlpha = (contrast * 255).toInt()
        val darkenColor = Color.argb(darkenAlpha, 0, 0, 0)
        val brightenColor = Color.argb(darkenAlpha, 255, 255, 255)

        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        when (params.patternType) {
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
                // Concentric circle rings spaced by period
                p.color = darkenColor
                p.style = Paint.Style.STROKE
                p.strokeWidth = (period / 2).toFloat()
                val cx = w / 2f; val cy = h / 2f
                val maxR = kotlin.math.sqrt((cx * cx + cy * cy).toDouble()).toFloat()
                var r = period.toFloat()
                while (r < maxR) {
                    canvas.drawCircle(cx, cy, r, p)
                    r += period
                }
            }

            SpatialPatternType.SINE_WAVE -> {
                // Horizontal sine-wave luminance modulation: each row gets a brightness shift
                // proportional to sin(2π * row / period)
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

        // If orientation != 0, rotate the pattern bitmap
        val finalBmp = if (params.orientationDeg != 0f) {
            val matrix = Matrix()
            matrix.postRotate(params.orientationDeg, w / 2f, h / 2f)
            val rotated = Bitmap.createBitmap(bmp, 0, 0, w, h, matrix, true)
            bmp.recycle()
            rotated
        } else bmp

        patternBitmap = finalBmp
        patternCanvas = Canvas(finalBmp)
    }

    override fun render(canvas: Canvas, frameTimeNanos: Long) {
        canvas.drawRect(0f, 0f, surfaceWidth.toFloat(), surfaceHeight.toFloat(), bgPaint)

        // 1. Reference bitmap
        bitmap?.let { canvas.drawBitmap(it, srcRect, dstRect, bitmapPaint) }

        // 2. Spatial pattern overlay (clipped to image destination rect)
        canvas.save()
        canvas.clipRect(dstRect)
        patternBitmap?.let { canvas.drawBitmap(it, 0f, 0f, patternPaint) }
        canvas.restore()

        // 3. Calibration frame
        canvas.drawRect(marginPx, marginPx, surfaceWidth - marginPx, surfaceHeight - marginPx, framePaint)

        // 4. Corner fiducials
        drawFiducials(canvas)

        // 5. Mode/params overlay
        canvas.drawText(
            "SPATIAL • ${params.patternType.displayLabel} • ${params.gridPeriodPx}px • ${(params.contrastRatio * 100).toInt()}%",
            marginPx + 16f, marginPx + 30f, textPaint
        )
        canvas.drawText(
            "SESSION: $experimentId",
            marginPx + 16f, surfaceHeight - marginPx - 16f, textPaint
        )
    }

    private fun drawFiducials(canvas: Canvas) {
        val corners = listOf(
            marginPx to marginPx,
            surfaceWidth - marginPx to marginPx,
            surfaceWidth - marginPx to surfaceHeight - marginPx,
            marginPx to surfaceHeight - marginPx
        )
        for ((idx, c) in corners.withIndex()) {
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
        patternBitmap?.recycle()
        patternBitmap = null
    }
}
