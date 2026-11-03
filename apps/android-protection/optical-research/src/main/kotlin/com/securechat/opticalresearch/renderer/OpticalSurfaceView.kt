package com.securechat.opticalresearch.renderer

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.Choreographer
import android.view.SurfaceHolder
import android.view.SurfaceView

/**
 * Dedicated research rendering view using Android SurfaceView and Choreographer.
 *
 * Synchronizes display rendering with the display panel's hardware VSYNC signals.
 * Hosts an OpticalRenderer (NormalBaselineRenderer in Phase 4A).
 */
class OpticalSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback, Choreographer.FrameCallback {

    private var renderer: OpticalRenderer? = null
    private var referenceBitmap: Bitmap? = null

    private var isSurfaceAvailable = false
    private var isRenderingActive = false

    init {
        holder.addCallback(this)
    }

    fun setRenderer(renderer: OpticalRenderer, bitmap: Bitmap) {
        this.renderer = renderer
        this.referenceBitmap = bitmap
        if (isSurfaceAvailable && width > 0 && height > 0) {
            renderer.initialize(bitmap, width, height)
            requestRender()
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        isSurfaceAvailable = true
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        referenceBitmap?.let { bmp ->
            renderer?.initialize(bmp, width, height)
            requestRender()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isSurfaceAvailable = false
        stopRendering()
        renderer?.release()
    }

    fun startRendering() {
        if (!isRenderingActive) {
            isRenderingActive = true
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun stopRendering() {
        isRenderingActive = false
        Choreographer.getInstance().removeFrameCallback(this)
    }

    fun requestRender() {
        if (isSurfaceAvailable) {
            renderFrame(System.nanoTime())
        }
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!isRenderingActive) return

        renderFrame(frameTimeNanos)

        // For future dynamic modulation, continue posting VSYNC frames:
        if (renderer?.isModulationActive == true) {
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private fun renderFrame(frameTimeNanos: Long) {
        if (!isSurfaceAvailable) return
        val canvas = holder.lockCanvas() ?: return
        try {
            renderer?.render(canvas, frameTimeNanos)
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }
}
