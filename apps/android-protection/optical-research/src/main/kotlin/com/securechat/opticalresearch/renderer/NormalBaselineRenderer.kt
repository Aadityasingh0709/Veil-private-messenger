package com.securechat.opticalresearch.renderer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF

/**
 * Phase 4A Baseline Renderer.
 *
 * Presents a controlled, static reference test chart onto the research surface:
 * - Preserves native image aspect ratio (Fit Center).
 * - Renders a high-contrast calibration frame.
 * - Draws 4 detectable corner fiducials (concentric bullseyes) in the margins.
 * - Embeds session identifier overlay.
 * - STRICTLY STATIC: Zero animation, zero flicker, zero temporal or spatial modulation.
 */
class NormalBaselineRenderer(
    private val experimentId: String
) : OpticalRenderer {

    override val modeName: String = "Normal Baseline (Phase 4A)"
    override val isModulationActive: Boolean = false

    private var bitmap: Bitmap? = null
    private var surfaceWidth: Int = 0
    private var surfaceHeight: Int = 0

    private val srcRect = Rect()
    private val dstRect = RectF()

    private val marginPx: Float = 60.0f
    private val markerRadiusPx: Float = 36.0f

    // Pre-allocated paint objects
    private val bgPaint = Paint().apply {
        color = Color.rgb(245, 245, 245)
        style = Paint.Style.FILL
    }

    private val framePaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 3.0f
        isAntiAlias = true
    }

    private val markerBlackPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val markerWhitePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.DKGRAY
        textSize = 28.0f
        isAntiAlias = true
        typeface = android.graphics.Typeface.MONOSPACE
    }

    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)

    override fun initialize(referenceBitmap: Bitmap, width: Int, height: Int) {
        this.bitmap = referenceBitmap
        this.surfaceWidth = width
        this.surfaceHeight = height

        srcRect.set(0, 0, referenceBitmap.width, referenceBitmap.height)

        // Calculate aspect ratio fit center within margins
        val availWidth = width - (marginPx * 2)
        val availHeight = height - (marginPx * 2) - 80 // Reserve 80px for session header/footer

        val imgAspect = referenceBitmap.width.toFloat() / referenceBitmap.height.toFloat()
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

        val left = marginPx + (availWidth - drawWidth) / 2.0f
        val top = marginPx + 40.0f + (availHeight - drawHeight) / 2.0f
        dstRect.set(left, top, left + drawWidth, top + drawHeight)
    }

    override fun render(canvas: Canvas, frameTimeNanos: Long) {
        // Clear background
        canvas.drawRect(0f, 0f, surfaceWidth.toFloat(), surfaceHeight.toFloat(), bgPaint)

        // 1. Draw Reference Test Image
        bitmap?.let { bmp ->
            canvas.drawBitmap(bmp, srcRect, dstRect, bitmapPaint)
        }

        // 2. Draw outer calibration frame connecting markers
        canvas.drawRect(
            marginPx,
            marginPx,
            surfaceWidth - marginPx,
            surfaceHeight - marginPx,
            framePaint
        )

        // 3. Draw 4 Corner Bullseye Fiducials
        val corners = listOf(
            Pair(marginPx, marginPx),
            Pair(surfaceWidth - marginPx, marginPx),
            Pair(surfaceWidth - marginPx, surfaceHeight - marginPx),
            Pair(marginPx, surfaceHeight - marginPx)
        )

        for ((idx, corner) in corners.withIndex()) {
            val cx = corner.first
            val cy = corner.second

            // Outer white square backing
            val sz = markerRadiusPx + 8f
            canvas.drawRect(cx - sz, cy - sz, cx + sz, cy + sz, markerWhitePaint)
            canvas.drawRect(cx - sz, cy - sz, cx + sz, cy + sz, framePaint)

            // Outer black circle
            canvas.drawCircle(cx, cy, markerRadiusPx, markerBlackPaint)
            // Mid white ring
            canvas.drawCircle(cx, cy, markerRadiusPx * 0.65f, markerWhitePaint)
            // Inner black circle
            canvas.drawCircle(cx, cy, markerRadiusPx * 0.35f, markerBlackPaint)
            // Center white dot
            canvas.drawCircle(cx, cy, 3.0f, markerWhitePaint)

            // Number tag
            canvas.drawText("${idx + 1}", cx - 6f, cy + 8f, markerWhitePaint.apply { textSize = 16f })
        }

        // 4. Session ID Header & Instructions
        canvas.drawText("SESSION: $experimentId", marginPx + 16f, marginPx + 30f, textPaint)
        canvas.drawText("PHASE 4A BASELINE // STATIC REFERENCE", marginPx + 16f, surfaceHeight - marginPx - 16f, textPaint)
    }

    override fun release() {
        bitmap = null
    }
}
