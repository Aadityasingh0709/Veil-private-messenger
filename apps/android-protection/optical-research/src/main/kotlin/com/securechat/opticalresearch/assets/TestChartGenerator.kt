package com.securechat.opticalresearch.assets

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlin.math.cos
import kotlin.math.sin

/**
 * Deterministic synthetic test chart generator in Kotlin.
 *
 * Generates the 7 standardized reference charts directly in-memory as Android Bitmaps
 * matching the Python generator (1080x1920) with corner fiducial markers.
 */
object TestChartGenerator {

    const val WIDTH = 1080
    const val HEIGHT = 1920
    const val MARGIN = 60f
    const val MARKER_RADIUS = 36f

    fun generateChart(type: TestChartType): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        when (type) {
            TestChartType.COLOR_CALIBRATION -> drawColorCalibrationChart(canvas)
            TestChartType.GRAYSCALE_GRADIENT -> drawGrayscaleGradientChart(canvas)
            TestChartType.CHECKERBOARD -> drawCheckerboardChart(canvas)
            TestChartType.FINE_LINES -> drawFineLinesChart(canvas)
            TestChartType.HIGH_CONTRAST_EDGES -> drawHighContrastEdgesChart(canvas)
            TestChartType.TEXT_READABILITY -> drawTextReadabilityChart(canvas)
            TestChartType.COMBINED_QUALITY -> drawCombinedQualityChart(canvas)
        }

        drawOuterCalibrationFrame(canvas, type.title)
        drawCornerFiducials(canvas)
        return bitmap
    }

    private fun drawOuterCalibrationFrame(canvas: Canvas, title: String) {
        val strokePaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }

        canvas.drawRect(MARGIN, MARGIN, WIDTH - MARGIN, HEIGHT - MARGIN, strokePaint)

        val headerTop = MARGIN + 16f
        val headerBottom = headerTop + 48f
        val headerBg = Paint().apply {
            color = Color.rgb(240, 240, 240)
            style = Paint.Style.FILL
        }
        canvas.drawRect(MARGIN + 14f, headerTop, WIDTH - MARGIN - 14f, headerBottom, headerBg)

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            isAntiAlias = true
            typeface = Typeface.MONOSPACE
        }
        canvas.drawText("VEIL OPTICAL LAB // ${title.uppercase()}", MARGIN + 24f, headerTop + 32f, textPaint)
        canvas.drawText("${WIDTH}x$HEIGHT REFERENCE", WIDTH - MARGIN - 260f, headerTop + 32f, textPaint.apply { textSize = 18f })
    }

    private fun drawCornerFiducials(canvas: Canvas) {
        val corners = listOf(
            Pair(MARGIN, MARGIN),
            Pair(WIDTH - MARGIN, MARGIN),
            Pair(WIDTH - MARGIN, HEIGHT - MARGIN),
            Pair(MARGIN, HEIGHT - MARGIN)
        )

        val whitePaint = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL; isAntiAlias = true }
        val blackPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL; isAntiAlias = true }
        val strokePaint = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 2f; isAntiAlias = true }

        for ((idx, corner) in corners.withIndex()) {
            val cx = corner.first
            val cy = corner.second
            val sz = MARKER_RADIUS + 8f

            canvas.drawRect(cx - sz, cy - sz, cx + sz, cy + sz, whitePaint)
            canvas.drawRect(cx - sz, cy - sz, cx + sz, cy + sz, strokePaint)

            canvas.drawCircle(cx, cy, MARKER_RADIUS, blackPaint)
            canvas.drawCircle(cx, cy, MARKER_RADIUS * 0.65f, whitePaint)
            canvas.drawCircle(cx, cy, MARKER_RADIUS * 0.35f, blackPaint)
            canvas.drawCircle(cx, cy, 3f, whitePaint)

            val tagPaint = Paint().apply { color = Color.WHITE; textSize = 16f; isAntiAlias = true; typeface = Typeface.DEFAULT_BOLD }
            canvas.drawText("${idx + 1}", cx - 5f, cy + 6f, tagPaint)
        }
    }

    private fun drawColorCalibrationChart(canvas: Canvas) {
        canvas.drawColor(Color.rgb(250, 250, 250))

        val colorsRgb = intArrayOf(
            Color.rgb(115, 81, 56), Color.rgb(194, 150, 133), Color.rgb(90, 122, 166), Color.rgb(90, 108, 64), Color.rgb(130, 128, 175), Color.rgb(98, 193, 169),
            Color.rgb(214, 125, 44), Color.rgb(70, 91, 165), Color.rgb(179, 61, 75), Color.rgb(91, 59, 106), Color.rgb(157, 187, 58), Color.rgb(229, 161, 43),
            Color.rgb(9, 60, 147), Color.rgb(70, 149, 72), Color.rgb(175, 43, 36), Color.rgb(238, 199, 34), Color.rgb(187, 76, 137), Color.rgb(0, 134, 171),
            Color.rgb(245, 245, 245), Color.rgb(200, 200, 200), Color.rgb(160, 160, 160), Color.rgb(120, 120, 120), Color.rgb(80, 80, 80), Color.rgb(30, 30, 30)
        )

        val patchW = 130f
        val patchH = 180f
        val startX = (WIDTH - (6 * patchW + 5 * 20f)) / 2f
        val startY = 200f

        val patchPaint = Paint().apply { style = Paint.Style.FILL }
        val borderPaint = Paint().apply { color = Color.DKGRAY; style = Paint.Style.STROKE; strokeWidth = 2f }

        for (row in 0 until 4) {
            for (col in 0 until 6) {
                val idx = row * 6 + col
                patchPaint.color = colorsRgb[idx]
                val x1 = startX + col * (patchW + 20f)
                val y1 = startY + row * (patchH + 24f)
                canvas.drawRect(x1, y1, x1 + patchW, y1 + patchH, patchPaint)
                canvas.drawRect(x1, y1, x1 + patchW, y1 + patchH, borderPaint)
            }
        }

        // Bottom RGB ramps
        val gradY = startY + 4 * (patchH + 24f) + 40f
        val gradH = 45f
        val gradW = WIDTH - 2 * (MARGIN + 40f)
        val gradX = MARGIN + 40f
        val linePaint = Paint()

        for (i in 0 until gradW.toInt()) {
            val v = (i * 255f / gradW).toInt()
            linePaint.color = Color.rgb(v, 0, 0)
            canvas.drawRect(gradX + i, gradY, gradX + i + 1, gradY + gradH, linePaint)
            linePaint.color = Color.rgb(0, v, 0)
            canvas.drawRect(gradX + i, gradY + 60f, gradX + i + 1, gradY + 60f + gradH, linePaint)
            linePaint.color = Color.rgb(0, 0, v)
            canvas.drawRect(gradX + i, gradY + 120f, gradX + i + 1, gradY + 120f + gradH, linePaint)
        }
    }

    private fun drawGrayscaleGradientChart(canvas: Canvas) {
        canvas.drawColor(Color.WHITE)
        val contentX = MARGIN + 40f
        val contentW = WIDTH - 2 * contentX

        // Continuous gradient
        val y1 = 180f
        val gradH = 160f
        val linePaint = Paint()
        for (i in 0 until contentW.toInt()) {
            val v = (i * 255f / contentW).toInt()
            linePaint.color = Color.rgb(v, v, v)
            canvas.drawRect(contentX + i, y1, contentX + i + 1, y1 + gradH, linePaint)
        }

        // 16-step wedge
        val y2 = 400f
        val stepH = 160f
        val stepW = contentW / 16f
        val fillPaint = Paint().apply { style = Paint.Style.FILL }
        val borderPaint = Paint().apply { color = Color.GRAY; style = Paint.Style.STROKE; strokeWidth = 1f }

        for (step in 0 until 16) {
            val v = (step * 255f / 15f).toInt()
            fillPaint.color = Color.rgb(v, v, v)
            val sx = contentX + step * stepW
            canvas.drawRect(sx, y2, sx + stepW, y2 + stepH, fillPaint)
            canvas.drawRect(sx, y2, sx + stepW, y2 + stepH, borderPaint)
        }

        // Vertical ramp
        val y3 = 920f
        val colH = 750f
        for (row in 0 until colH.toInt()) {
            val v = (row * 255f / colH).toInt()
            linePaint.color = Color.rgb(v, v, v)
            canvas.drawRect(contentX, y3 + row, contentX + contentW, y3 + row + 1, linePaint)
        }
    }

    private fun drawCheckerboardChart(canvas: Canvas) {
        canvas.drawColor(Color.WHITE)
        val cols = 16
        val rows = 24
        val sqSize = 54f
        val gridW = cols * sqSize
        val gridH = rows * sqSize
        val startX = (WIDTH - gridW) / 2f
        val startY = (HEIGHT - gridH) / 2f + 20f

        val blackPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if ((r + c) % 2 == 1) {
                    val x = startX + c * sqSize
                    val y = startY + r * sqSize
                    canvas.drawRect(x, y, x + sqSize, y + sqSize, blackPaint)
                }
            }
        }
    }

    private fun drawFineLinesChart(canvas: Canvas) {
        canvas.drawColor(Color.WHITE)
        val contentX = MARGIN + 40f
        val contentW = WIDTH - 2 * contentX

        var y = 180f
        val periods = listOf(1, 2, 4, 8)
        val paint = Paint().apply { color = Color.BLACK }

        for (p in periods) {
            val halfW = (contentW - 20f) / 2f
            for (r in 0 until 100) {
                if ((r / p) % 2 == 0) {
                    canvas.drawRect(contentX, y + r, contentX + halfW, y + r + 1, paint)
                }
            }
            val rightX = contentX + halfW + 20f
            for (c in 0 until halfW.toInt()) {
                if ((c / p) % 2 == 0) {
                    canvas.drawRect(rightX + c, y, rightX + c + 1, y + 100f, paint)
                }
            }
            y += 140f
        }

        // Siemens star
        val starCx = WIDTH / 2f
        val starCy = y + 260f
        val radius = 240f
        val spokes = 36
        val angleStep = (2 * Math.PI / spokes).toFloat()

        val spokePaint = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL; isAntiAlias = true }
        for (i in 0 until spokes step 2) {
            val a1 = i * angleStep
            val a2 = (i + 1) * angleStep
            val path = Path().apply {
                moveTo(starCx, starCy)
                lineTo((starCx + radius * cos(a1)).toFloat(), (starCy + radius * sin(a1)).toFloat())
                lineTo((starCx + radius * cos(a2)).toFloat(), (starCy + radius * sin(a2)).toFloat())
                close()
            }
            canvas.drawPath(path, spokePaint)
        }
    }

    private fun drawHighContrastEdgesChart(canvas: Canvas) {
        canvas.drawColor(Color.WHITE)
        val cx = WIDTH / 2f
        val cy = 400f
        val size = 240f

        val grayPaint = Paint().apply { color = Color.rgb(240, 240, 240); style = Paint.Style.FILL }
        val borderPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 3f }
        val blackPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL; isAntiAlias = true }

        // Slanted edge 5 deg
        canvas.drawRect(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2, grayPaint)
        canvas.drawRect(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2, borderPaint)

        val tan5 = Math.tan(Math.toRadians(5.0)).toFloat()
        val path = Path().apply {
            moveTo(cx, cy - size / 2)
            lineTo(cx - size * tan5, cy + size / 2)
            lineTo(cx + size / 2, cy + size / 2)
            lineTo(cx + size / 2, cy - size / 2)
            close()
        }
        canvas.drawPath(path, blackPaint)
    }

    private fun drawTextReadabilityChart(canvas: Canvas) {
        canvas.drawColor(Color.WHITE)
        val contentX = MARGIN + 40f
        var y = 230f

        val scales = listOf(
            Pair(48f, "VEIL SECURITY 48PT"),
            Pair(36f, "CONFIDENTIAL INTEL 36PT"),
            Pair(24f, "PRIVACY MESSAGING 24PT"),
            Pair(18f, "AUTHENTICATION SESSION 18PT"),
            Pair(14f, "PROTECTION KERNEL 14PT"),
            Pair(10f, "CRYPTOGRAPHIC TOKEN 10PT"),
            Pair(8f, "OPTICAL DEFENSE 8PT")
        )

        val textPaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            typeface = Typeface.SERIF
        }

        for ((size, text) in scales) {
            textPaint.textSize = size
            canvas.drawText(text, contentX, y, textPaint)
            y += size + 24f
            textPaint.textSize = size * 0.75f
            canvas.drawText("The quick brown fox jumps over the lazy dog 1234567890", contentX, y, textPaint)
            y += size + 30f
        }
    }

    private fun drawCombinedQualityChart(canvas: Canvas) {
        drawColorCalibrationChart(canvas)
    }

    fun saveChartToFile(bitmap: Bitmap, targetFile: File): String {
        targetFile.parentFile?.mkdirs()
        FileOutputStream(targetFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        return calculateSha256(bitmap)
    }

    fun calculateSha256(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val bytes = stream.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
