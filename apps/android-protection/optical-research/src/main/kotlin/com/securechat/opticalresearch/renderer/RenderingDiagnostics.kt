package com.securechat.opticalresearch.renderer

import java.util.concurrent.atomic.AtomicLong

/**
 * Records actual Choreographer frame presentation timestamps to diagnose
 * rendering timing accuracy in Phase 4B temporal modulation experiments.
 *
 * Problem: Android cannot guarantee exact frame delivery timing, especially under system load.
 * When temporal modulation claims "2 Hz alternation", the actual timing may deviate.
 * This class records up to [maxHistory] inter-frame intervals so the Python analysis pipeline
 * can include real timing in the experiment metadata rather than the requested frequency.
 *
 * Usage:
 * - Call [recordFrame] from every Choreographer.doFrame() callback.
 * - Call [snapshot] before exporting the session to capture a summary.
 * - [recentMeanFps] provides a live display FPS estimate.
 */
class RenderingDiagnostics(private val maxHistory: Int = 300) {

    private val frameTimestampsNs = ArrayDeque<Long>(maxHistory + 1)
    private val frameIntervalNs = ArrayDeque<Long>(maxHistory)

    private val totalFrames = AtomicLong(0L)
    private val droppedFrameEstimates = AtomicLong(0L)

    /** Expected inter-frame interval at 60 Hz in nanoseconds (reference baseline). */
    private val expectedIntervalNs60Hz = 16_666_667L

    /** Record a new frame timestamp. Thread-safe via synchronized. */
    @Synchronized
    fun recordFrame(frameTimeNanos: Long) {
        val prev = frameTimestampsNs.lastOrNull()
        frameTimestampsNs.addLast(frameTimeNanos)
        if (frameTimestampsNs.size > maxHistory + 1) frameTimestampsNs.removeFirst()

        if (prev != null) {
            val intervalNs = frameTimeNanos - prev
            frameIntervalNs.addLast(intervalNs)
            if (frameIntervalNs.size > maxHistory) frameIntervalNs.removeFirst()

            // Estimate dropped frames: if interval > 1.5× expected 60 Hz interval
            if (intervalNs > expectedIntervalNs60Hz * 1.5) {
                droppedFrameEstimates.incrementAndGet()
            }
        }

        totalFrames.incrementAndGet()
    }

    /** Returns mean FPS over the most recent [windowSize] frame intervals. */
    @Synchronized
    fun recentMeanFps(windowSize: Int = 30): Double {
        val recent = frameIntervalNs.takeLast(windowSize.coerceAtMost(frameIntervalNs.size))
        if (recent.isEmpty()) return 0.0
        val meanIntervalNs = recent.average()
        return if (meanIntervalNs > 0) 1_000_000_000.0 / meanIntervalNs else 0.0
    }

    /** Returns mean, min, max inter-frame interval in milliseconds over recorded history. */
    @Synchronized
    fun intervalStatisticsMs(): IntervalStats {
        if (frameIntervalNs.isEmpty()) return IntervalStats(0.0, 0.0, 0.0, 0)
        val ms = frameIntervalNs.map { it / 1_000_000.0 }
        return IntervalStats(
            meanMs = ms.average(),
            minMs = ms.min(),
            maxMs = ms.max(),
            sampleCount = ms.size
        )
    }

    /** Returns a snapshot summary for embedding into the session export. */
    @Synchronized
    fun snapshot(): DiagnosticsSnapshot {
        val stats = intervalStatisticsMs()
        return DiagnosticsSnapshot(
            totalFramesRecorded = totalFrames.get(),
            estimatedDroppedFrames = droppedFrameEstimates.get(),
            meanFrameIntervalMs = stats.meanMs,
            minFrameIntervalMs = stats.minMs,
            maxFrameIntervalMs = stats.maxMs,
            recentMeanFps = recentMeanFps()
        )
    }

    /** Resets all recorded history. */
    @Synchronized
    fun reset() {
        frameTimestampsNs.clear()
        frameIntervalNs.clear()
        totalFrames.set(0L)
        droppedFrameEstimates.set(0L)
    }

    data class IntervalStats(
        val meanMs: Double,
        val minMs: Double,
        val maxMs: Double,
        val sampleCount: Int
    )

    data class DiagnosticsSnapshot(
        val totalFramesRecorded: Long,
        val estimatedDroppedFrames: Long,
        val meanFrameIntervalMs: Double,
        val minFrameIntervalMs: Double,
        val maxFrameIntervalMs: Double,
        val recentMeanFps: Double
    )
}
