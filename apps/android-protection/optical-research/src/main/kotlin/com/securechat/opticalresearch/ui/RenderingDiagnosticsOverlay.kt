package com.securechat.opticalresearch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securechat.opticalresearch.renderer.RenderingDiagnostics
import com.securechat.opticalresearch.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Phase 4B: Live Rendering Diagnostics HUD Overlay.
 *
 * Displays real-time hardware frame timing telemetry collected by [RenderingDiagnostics]:
 * - Actual mean FPS over rolling 30-frame window
 * - Inter-frame interval (mean ± min/max ms)
 * - Estimated dropped frames (intervals >1.5x expected 60Hz 16.67ms)
 * - Total frames accumulated
 *
 * Crucial for research validation: proves whether the display achieved true 60/120Hz
 * or experienced frame drops during camera capture.
 */
@Composable
fun RenderingDiagnosticsOverlay(
    diagnostics: RenderingDiagnostics,
    modifier: Modifier = Modifier
) {
    var snapshot by remember { mutableStateOf(diagnostics.snapshot()) }

    // Poll diagnostics at 1 Hz for UI update
    LaunchedEffect(diagnostics) {
        while (true) {
            snapshot = diagnostics.snapshot()
            delay(1000L)
        }
    }

    Box(
        modifier = modifier
            .background(LabDarkSurface.copy(alpha = 0.88f), RoundedCornerShape(8.dp))
            .border(1.dp, LabBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "HW-TIMING",
                    color = LabPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${String.format("%.1f", snapshot.recentMeanFps)} FPS",
                    color = if (snapshot.recentMeanFps >= 55.0) LabSuccess else LabWarning,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Δt: ${String.format("%.1f", snapshot.meanFrameIntervalMs)}ms",
                    color = LabTextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )

                val dropColor = if (snapshot.estimatedDroppedFrames > 0) LabWarning else LabTextSecondary
                Text(
                    text = "Drops: ${snapshot.estimatedDroppedFrames}",
                    color = dropColor,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = "Frames: ${snapshot.totalFramesRecorded}",
                    color = LabTextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
