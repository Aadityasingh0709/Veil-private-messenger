package com.securechat.opticalresearch.renderer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF

/**
 * Core rendering interface for the Veil Optical Research Lab.
 *
 * Defines the contract for presenting visual content onto the research surface.
 * In Phase 4A, this displays a static baseline reference chart without temporal or spatial modulation.
 * In future phases, extension points will implement experimental modulation techniques.
 */
interface OpticalRenderer {
    val modeName: String
    val isModulationActive: Boolean

    /**
     * Initializes the renderer with the reference bitmap and surface dimensions.
     */
    fun initialize(referenceBitmap: Bitmap, width: Int, height: Int)

    /**
     * Called on each frame presentation (VSYNC-aligned when using Choreographer).
     * @param canvas The target Canvas to render onto
     * @param frameTimeNanos Monotonic frame timestamp in nanoseconds
     */
    fun render(canvas: Canvas, frameTimeNanos: Long)

    /**
     * Releases any active hardware resources or buffers.
     */
    fun release()
}
