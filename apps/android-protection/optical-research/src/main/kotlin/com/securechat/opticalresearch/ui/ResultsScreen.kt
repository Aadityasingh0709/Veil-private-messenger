package com.securechat.opticalresearch.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securechat.opticalresearch.export.ExperimentExporter
import com.securechat.opticalresearch.model.ExperimentSession
import com.securechat.opticalresearch.model.SessionRepository
import com.securechat.opticalresearch.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    session: ExperimentSession,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { SessionRepository(context) }
    val exporter = remember { ExperimentExporter(context) }

    fun handleExport() {
        try {
            val result = exporter.exportExperimentPackage(session, repository)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, result.contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Veil Optical Experiment: ${session.experimentId}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Export Experiment ZIP"))
            Toast.makeText(context, "Exported ${result.zipFile.name} (${result.fileSizeBytes / 1024} KB)", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Experiment Summary", color = LabTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = LabTextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { handleExport() }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export Package",
                            tint = LabPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LabDarkSurface)
            )
        },
        containerColor = LabDarkBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = LabDarkCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = session.experimentId,
                            style = MaterialTheme.typography.titleMedium,
                            color = LabPrimary
                        )
                        Text(
                            text = "Created: ${session.timestamp} • Mode: ${session.testMode.uppercase()}",
                            fontSize = 12.sp,
                            color = LabTextSecondary
                        )
                    }
                }
            }

            // Export action
            item {
                Button(
                    onClick = { handleExport() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = LabPrimary)
                ) {
                    Icon(imageVector = Icons.Default.Archive, contentDescription = null, tint = LabDarkBg)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Experiment ZIP Package", color = LabDarkBg, fontWeight = FontWeight.Bold)
                }
            }

            // Telemetry Cards
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, LabBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = LabDarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Source Reference Asset", fontWeight = FontWeight.Bold, color = LabTextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        TelemetryRow("Filename", session.sourceImage.name)
                        TelemetryRow("Dimensions", "${session.sourceImage.width}x${session.sourceImage.height} px")
                        TelemetryRow("SHA-256", "${session.sourceImage.hashSha256.take(16)}...")
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, LabBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = LabDarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Phone A Display Conditions", fontWeight = FontWeight.Bold, color = LabTextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        TelemetryRow("Device", "${session.phoneA.manufacturer} ${session.phoneA.model}")
                        TelemetryRow("OS", "Android ${session.phoneA.androidVersion} (API ${session.phoneA.apiLevel})")
                        TelemetryRow("Resolution", session.phoneA.displayResolution ?: "Unknown")
                        TelemetryRow("Refresh Rate", "${session.phoneA.refreshRateHz ?: 60.0f} Hz", highlightColor = LabSuccess)
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, LabBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = LabDarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Phone B Capture Conditions", fontWeight = FontWeight.Bold, color = LabTextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        TelemetryRow("Camera Device", session.phoneB.model ?: "Unknown")
                        TelemetryRow("Distance", "${session.phoneB.distanceCm ?: "N/A"} cm")
                        TelemetryRow("Angle", "${session.phoneB.cameraAngleDegrees ?: "N/A"}°")
                        TelemetryRow("ISO / Exposure", "ISO ${session.phoneB.iso ?: "Auto"} • ${session.phoneB.exposureTimeSeconds ?: "Auto"}")
                        TelemetryRow("Lighting", session.phoneB.ambientLighting ?: "Ambient")
                    }
                }
            }

            // Analysis status notice
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, LabBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = LabDarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Analysis Pipeline Status",
                            fontWeight = FontWeight.Bold,
                            color = LabWarning
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Scientific integrity requirement: The Android client does NOT fabricate synthetic similarity scores. " +
                                    "Transfer the exported ZIP archive to your analysis workstation and execute:\n\n" +
                                    "python analyze_experiment.py --input ${session.experimentId}.zip\n\n" +
                                    "This calculates objective PSNR, SSIM, and Delta-E with subpixel corner homography alignment.",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = LabTextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Captures
            item {
                Text(
                    text = "Associated Original Captures (${session.captures.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = LabTextPrimary
                )
            }

            items(session.captures) { cap ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, LabBorder, RoundedCornerShape(10.dp)),
                    colors = CardDefaults.cardColors(containerColor = LabDarkCard)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Repetition #${cap.repetitionNumber}: ${cap.filename}",
                            fontWeight = FontWeight.Bold,
                            color = LabTextPrimary
                        )
                        Text(
                            text = "Size: ${cap.fileSizeBytes / 1024} KB • SHA-256: ${cap.hashSha256?.take(16)}...",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = LabTextSecondary
                        )
                    }
                }
            }
        }
    }
}
