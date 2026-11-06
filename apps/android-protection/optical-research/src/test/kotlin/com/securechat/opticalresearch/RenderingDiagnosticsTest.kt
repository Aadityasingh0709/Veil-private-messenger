package com.securechat.opticalresearch

import com.securechat.opticalresearch.renderer.RenderingDiagnostics
import org.junit.Assert.*
import org.junit.Test

class RenderingDiagnosticsTest {

    @Test
    fun testRecordFrameAndMeanFpsAt60Hz() {
        val diag = RenderingDiagnostics()
        var timeNs = 1_000_000_000L
        val stepNs = 16_666_666L // ~60 FPS (16.67 ms)

        // Record 35 frames
        for (i in 0 until 35) {
            diag.recordFrame(timeNs)
            timeNs += stepNs
        }

        val fps = diag.recentMeanFps()
        assertTrue("FPS should be close to 60.0 (got $fps)", fps in 59.0..61.0)
    }

    @Test
    fun testDroppedFrameDetection() {
        val diag = RenderingDiagnostics()
        var timeNs = 1_000_000_000L

        // Frame 1: normal
        diag.recordFrame(timeNs)

        // Frame 2: normal 16.6ms
        timeNs += 16_666_666L
        diag.recordFrame(timeNs)

        // Frame 3: stall / dropped frame (45ms interval > 25ms threshold)
        timeNs += 45_000_000L
        diag.recordFrame(timeNs)

        // Frame 4: normal
        timeNs += 16_666_666L
        diag.recordFrame(timeNs)

        val snapshot = diag.snapshot()
        assertEquals(4L, snapshot.totalFramesRecorded)
        assertEquals("Should detect 1 dropped frame", 1L, snapshot.estimatedDroppedFrames)
    }

    @Test
    fun testIntervalStatisticsCalculations() {
        val diag = RenderingDiagnostics()
        var timeNs = 1_000_000_000L

        diag.recordFrame(timeNs)

        timeNs += 10_000_000L // +10ms
        diag.recordFrame(timeNs)

        timeNs += 20_000_000L // +20ms
        diag.recordFrame(timeNs)

        timeNs += 30_000_000L // +30ms
        diag.recordFrame(timeNs)

        val stats = diag.intervalStatisticsMs()
        assertEquals(10.0, stats.minMs, 0.01)
        assertEquals(30.0, stats.maxMs, 0.01)
        assertEquals(20.0, stats.meanMs, 0.01)
    }

    @Test
    fun testCircularBufferMaxCapacity() {
        val diag = RenderingDiagnostics(maxHistory = 100)
        var timeNs = 1_000_000_000L

        // Record 150 frames
        for (i in 0 until 150) {
            diag.recordFrame(timeNs)
            timeNs += 16_666_666L
        }

        val snapshot = diag.snapshot()
        assertEquals(150L, snapshot.totalFramesRecorded)
    }

    @Test
    fun testResetClearsStateCompletely() {
        val diag = RenderingDiagnostics()
        diag.recordFrame(1_000_000_000L)
        diag.recordFrame(1_016_666_666L)

        val before = diag.snapshot()
        assertEquals(2L, before.totalFramesRecorded)

        diag.reset()

        val after = diag.snapshot()
        assertEquals(0L, after.totalFramesRecorded)
        assertEquals(0L, after.estimatedDroppedFrames)
        assertEquals(0.0, after.recentMeanFps, 0.001)
        assertEquals(0.0, after.meanFrameIntervalMs, 0.001)
    }
}
