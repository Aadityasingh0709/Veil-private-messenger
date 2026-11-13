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
 * Phase 4B.1 — Spatial Modulation Prototype Renderer.
 *
 * Presents the reference test chart with an opt-in, controlled, static spatial luminance
 * perturbation pattern overlaid using alpha compositing:
 * - Deterministic, parameter-driven pattern generation (6 geometric patterns).
 * - Aspect-ratio-preserving fit center calculation (delegated to [SpatialPatternMath]).
 * - User toggle: switch between Normal baseline mode and Spatial Modulation mode.
 * - Strict conservative intensity bounds (contrast capped at [ModulationParams.Spatial.MAX_CONTRAST_RATIO] = 15%).
 * - Zero temporal flicker, zero aggressive brightness shifts, zero ML dependencies.
 * - Emergency stop control halts all modulation immediately.
 * - Preserves modularity so temporal and combined modulation can be built alongside.
 * - Purely experimental: makes no claims of camera degradation prior to physical experimentation.
 */
class SpatialModulationRenderer(
    private val experimentId: String,
    private var params: ModulationParams.Spatial = ModulationParams.Spatial()
) : OpticalRenderer {

    override val modeName: String = "Spatial Modulation (Phase 4B.1 Prototype)"

    /**
     * True only when the effect is user-enabled and emergency stop has not been triggered.
     * When false, the renderer presents the reference image in unmodified Normal baseline mode.
     */
    override val isModulationActive: Boolean
        get() = isEffectEnabled && !emergencyStop

    /**
     * User control to enable or disable the spatial modulation overlay.
     * Toggling this allows immediate comparison between Normal baseline and Spatial mode.
     */
    @Volatile
    var isEffectEnabled: Boolean = true

    /**
     * Fail-safe emergency stop control.
     * When set, immediately halts modulation and reverts display to static baseline.
     */
    @Volatile
    var emergencyStop: Boolean = false

    private var bitmap: Bitmap? = null
    private var surfaceWidth: Int = 0
    private var surfaceHeight: Int = 0

    private val srcRect = Rect()
    private val dstRect = RectF()

    private val marginPx = 60f
    private val markerRadiusPx = 36f

    // Pre-allocated paints (initialized lazily to avoid Android framework calls in JVM tests)
    private val bgPaint by lazy { Paint().apply { color = 0xFFF5F5F5.toInt(); style = Paint.Style.FILL } }
    private val framePaint by lazy { Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 3f; isAntiAlias = true } }
    private val bitmapPaint by lazy { Paint(Paint.FILTER_BITMAP_FLAG) }
    private val markerBlack by lazy { Paint().apply { color = Color.BLACK; style = Paint.Style.FILL; isAntiAlias = true } }
    private val markerWhite by lazy { Paint().apply { color = Color.WHITE; style = Paint.Style.FILL; isAntiAlias = true } }
    private val textPaint by lazy {
        Paint().apply {
            color = 0xFF64DC64.toInt()
            textSize = 26f
            isAntiAlias = true
            typeface = android.graphics.Typeface.MONOSPACE
        }
    }
    private val stopTextPaint by lazy {
        Paint().apply {
            color = 0xFFFF5050.toInt()
            textSize = 28f
            isAntiAlias = true
            typeface = android.graphics.Typeface.MONOSPACE
        }
    }
    private val disabledTextPaint by lazy {
        Paint().apply {
            color = 0xFFA0A0A0.toInt()
            textSize = 26f
            isAntiAlias = true
            typeface = android.graphics.Typeface.MONOSPACE
        }
    }

    // Off-screen pattern bitmap (recycled on param or dimension change)
    private var patternBitmap: Bitmap? = null
    private val patternPaint by lazy {
        Paint().apply {
            isAntiAlias = false
        }
    }

    /** Returns the currently active spatial parameters. */
    fun getParams(): ModulationParams.Spatial = params

    /**
     * Updates spatial parameters mid-session.
     * Triggers pattern bitmap regeneration if surface dimensions are initialized.
     */
    fun updateParams(newParams: ModulationParams.Spatial) {
        params = newParams
        patternBitmap?.recycle()
        patternBitmap = null
        if (surfaceWidth > 0 && surfaceHeight > 0) {
            buildPatternBitmap(surfaceWidth, surfaceHeight)
        }
    }


    /** Triggers immediate stop, halting modulation overlay. */
    fun stopModulation() {
        emergencyStop = true
        isEffectEnabled = false
    }

    /** Restores modulation after an emergency stop or disable action. */
    fun restoreModulation() {
        emergencyStop = false
        isEffectEnabled = true
    }

    override fun initialize(referenceBitmap: Bitmap, width: Int, height: Int) {
        this.bitmap = referenceBitmap
        this.surfaceWidth = width
        this.surfaceHeight = height

        srcRect.set(0, 0, referenceBitmap.width, referenceBitmap.height)

        // Compute aspect-ratio-preserving destination rectangle
        val fit = SpatialPatternMath.calculateFitCenterRect(
            imageWidth = referenceBitmap.width,
            imageHeight = referenceBitmap.height,
            surfaceWidth = width,
            surfaceHeight = height,
            marginPx = marginPx,
            reservedVerticalPx = 80f
        )
        dstRect.set(fit.left, fit.top, fit.right, fit.bottom)

        // Pre-render pattern bitmap
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

        // Apply rotation matrix if orientationDeg != 0
        val finalBmp = if (params.orientationDeg != 0f) {
            val matrix = Matrix()
            matrix.postRotate(params.orientationDeg, w / 2f, h / 2f)
            val rotated = Bitmap.createBitmap(bmp, 0, 0, w, h, matrix, true)
            bmp.recycle()
            rotated
        } else {
            bmp
        }

        patternBitmap = finalBmp
    }

    override fun render(canvas: Canvas, frameTimeNanos: Long) {
        // Clear background with neutral laboratory gray
        canvas.drawRect(0f, 0f, surfaceWidth.toFloat(), surfaceHeight.toFloat(), bgPaint)

        // 1. Draw underlying reference image preserving native aspect ratio
        bitmap?.let { canvas.drawBitmap(it, srcRect, dstRect, bitmapPaint) }

        // 2. Controlled spatial perturbation overlay (drawn ONLY when modulation is active)
        if (isModulationActive) {
            canvas.save()
            canvas.clipRect(dstRect)
            patternBitmap?.let { canvas.drawBitmap(it, 0f, 0f, patternPaint) }
            canvas.restore()
        }

        // 3. Calibration frame connecting corner fiducials
        canvas.drawRect(marginPx, marginPx, surfaceWidth - marginPx, surfaceHeight - marginPx, framePaint)

        // 4. Corner fiducials for homography perspective correction
        drawFiducials(canvas)

        // 5. HUD Status & Parameters text
        when {
            emergencyStop -> {
                canvas.drawText("⏹ MODULATION STOPPED", marginPx + 16f, marginPx + 30f, stopTextPaint)
            }
            !isEffectEnabled -> {
                canvas.drawText("NORMAL MODE • SPATIAL MODULATION OFF", marginPx + 16f, marginPx + 30f, disabledTextPaint)
            }
            else -> {
                canvas.drawText(
                    "SPATIAL • ${params.patternType.displayLabel} • ${params.gridPeriodPx}px • ${(params.contrastRatio * 100).toInt()}%",
                    marginPx + 16f, marginPx + 30f, textPaint
                )
            }
        }

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
        for (c in corners) {
            val cx = c.first
            val cy = c.second
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
        surfaceWidth = 0
        surfaceHeight = 0
    }
}
