package com.securechat.opticalresearch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securechat.opticalresearch.model.ExperimentSession
import com.securechat.opticalresearch.model.PhoneADeviceInfo
import com.securechat.opticalresearch.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    deviceInfo: PhoneADeviceInfo,
    experiments: List<ExperimentSession>,
    onNavigateNewExperiment: () -> Unit,
    onNavigateCalibration: () -> Unit,
    onNavigateGallery: () -> Unit,
    onNavigateImport: (String?) -> Unit,
    onNavigateResults: (String) -> Unit,
    onExportLatest: () -> Unit,
    onNavigateModulation: (String) -> Unit = {}
) {
    val latestExp = experiments.firstOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Veil Optical Research Lab",
                            style = MaterialTheme.typography.titleLarge,
                            color = LabTextPrimary
                        )
                        Text(
                            text = "PHASE 4A/4B // OPTICAL BASELINE & MODULATION EXPERIMENTS",
                            style = MaterialTheme.typography.labelSmall,
                            color = LabPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LabDarkSurface
                )
            )
        },
        containerColor = LabDarkBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Stage Info Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = LabDarkCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = LabPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Two-Phone Baseline Protocol",
                                fontWeight = FontWeight.Bold,
                                color = LabTextPrimary
                            )
                            Text(
                                text = "Measure reference display characteristics, acquire Phone B captures, and evaluate PSNR/SSIM offline via Python.",
                                fontSize = 12.sp,
                                color = LabTextSecondary
                            )
                        }
                    }
                }
            }

            // 2. Live Display & Hardware Telemetry Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, LabBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = LabDarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Phone A Telemetry (Display Device)",
                                fontWeight = FontWeight.SemiBold,
                                color = LabTextPrimary
                            )
                            Badge(containerColor = LabPrimary.copy(alpha = 0.2f)) {
                                Text(
                                    text = "LIVE SENSOR",
                                    color = LabPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        TelemetryRow("Hardware Model", "${deviceInfo.manufacturer} ${deviceInfo.model}")
                        TelemetryRow("Android OS", "Android ${deviceInfo.androidVersion} (API ${deviceInfo.apiLevel})")
                        TelemetryRow("Display Resolution", deviceInfo.displayResolution ?: "Unknown")
                        TelemetryRow(
                            "Active Refresh Rate",
                            "${deviceInfo.refreshRateHz ?: 60.0f} Hz",
                            highlightColor = LabSuccess
                        )
                        TelemetryRow(
                            "Supported Refresh Modes",
                            deviceInfo.supportedRefreshRates?.joinToString(", ") { "${it}Hz" } ?: "60Hz"
                        )
                        TelemetryRow(
                            "Panel Brightness",
                            if (deviceInfo.screenBrightnessPercent != null) "${deviceInfo.screenBrightnessPercent.toInt()}% (Adaptive: ${deviceInfo.adaptiveBrightness ?: false})" else "System Protected"
                        )
                    }
                }
            }

            // 3. Quick Action Buttons Grid
            item {
                Text(
                    text = "Laboratory Controls",
                    fontWeight = FontWeight.SemiBold,
                    color = LabTextPrimary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ActionButton(
                            modifier = Modifier.weight(1f),
                            title = "New Experiment",
                            subtitle = "Create capture session",
                            icon = Icons.Default.AddCircle,
                            tint = LabPrimary,
                            onClick = onNavigateNewExperiment
                        )
                        ActionButton(
                            modifier = Modifier.weight(1f),
                            title = "Device Calibration",
                            subtitle = "Observe display APIs",
                            icon = Icons.Default.Tune,
                            tint = LabSecondary,
                            onClick = onNavigateCalibration
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ActionButton(
                            modifier = Modifier.weight(1f),
                            title = "Display Test Chart",
                            subtitle = "7 synthetic targets",
                            icon = Icons.Default.AspectRatio,
                            tint = LabSuccess,
                            onClick = onNavigateGallery
                        )
                        ActionButton(
                            modifier = Modifier.weight(1f),
                            title = "Import Capture",
                            subtitle = "From Phone B camera",
                            icon = Icons.Default.UploadFile,
                            tint = LabWarning,
                            onClick = { onNavigateImport(latestExp?.experimentId) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ActionButton(
                            modifier = Modifier.weight(1f),
                            title = "Export Package",
                            subtitle = "ZIP for Python tool",
                            icon = Icons.Default.Archive,
                            tint = Color(0xFFE040FB),
                            onClick = onExportLatest
                        )
                        ActionButton(
                            modifier = Modifier.weight(1f),
                            title = "Session Results",
                            subtitle = "${experiments.size} sessions recorded",
                            icon = Icons.Default.History,
                            tint = Color(0xFF64B5F6),
                            onClick = {
                                if (latestExp != null) onNavigateResults(latestExp.experimentId)
                                else onNavigateNewExperiment()
                            }
                        )
                    }
                }
            }

            // Phase 4B: Modulation Experiments Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Phase 4B — Modulation Experiments",
                        fontWeight = FontWeight.SemiBold,
                        color = LabTextPrimary
                    )
                    Badge(containerColor = LabPrimary.copy(alpha = 0.2f)) {
                        Text(
                            text = "PHASE 4B",
                            color = LabPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ActionButton(
                            modifier = Modifier.weight(1f),
                            title = "Spatial Mod",
                            subtitle = "Mode 1: 6 patterns",
                            icon = Icons.Default.GridOn,
                            tint = LabPrimary,
                            onClick = { onNavigateModulation("spatial") }
                        )
                        ActionButton(
                            modifier = Modifier.weight(1f),
                            title = "Temporal Mod",
                            subtitle = "Mode 2: VSYNC ≤3Hz",
                            icon = Icons.Default.Speed,
                            tint = LabWarning,
                            onClick = { onNavigateModulation("temporal") }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ActionButton(
                            modifier = Modifier.weight(1f),
                            title = "Combined Mod",
                            subtitle = "Mode 3: Spatial + Temporal",
                            icon = Icons.Default.Layers,
                            tint = LabSuccess,
                            onClick = { onNavigateModulation("combined") }
                        )
                        ActionButton(
                            modifier = Modifier.weight(1f),
                            title = "Static Baseline",
                            subtitle = "Phase 4A control",
                            icon = Icons.Default.AspectRatio,
                            tint = LabSecondary,
                            onClick = { onNavigateModulation("baseline") }
                        )
                    }
                }
            }

            // 4. Recent Experiment Summary Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, LabBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = LabDarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Latest Research Session",
                            fontWeight = FontWeight.SemiBold,
                            color = LabTextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (latestExp != null) {
                            Text(
                                text = latestExp.experimentId,
                                style = MaterialTheme.typography.labelSmall,
                                color = LabPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Asset: ${latestExp.sourceImage.name} • ${latestExp.captures.size} captures imported",
                                fontSize = 13.sp,
                                color = LabTextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { onNavigateResults(latestExp.experimentId) },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Inspect Session")
                            }
                        } else {
                            Text(
                                text = "No experiments logged yet. Tap 'New Experiment' to create your baseline session.",
                                fontSize = 13.sp,
                                color = LabTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TelemetryRow(
    label: String,
    value: String,
    highlightColor: Color = LabTextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = LabTextSecondary)
        Text(
            text = value,
            fontSize = 13.sp,
            color = highlightColor,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ActionButton(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .border(1.dp, LabBorder, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = LabDarkCard),
        onClick = onClick,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth()
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = LabTextPrimary)
            Text(text = subtitle, fontSize = 11.sp, color = LabTextSecondary)
        }
    }
}
