package com.securechat.opticalresearch.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securechat.opticalresearch.importer.CaptureImporter
import com.securechat.opticalresearch.model.*
import com.securechat.opticalresearch.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureImportScreen(
    session: ExperimentSession,
    onSaveSession: (ExperimentSession) -> Unit,
    onNavigateResults: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { SessionRepository(context) }
    val importer = remember { CaptureImporter(context) }

    var currentSession by remember { mutableStateOf(session) }

    // Phone B capture telemetry state
    var phoneBModel by remember { mutableStateOf(session.phoneB.model ?: "Google Pixel 8") }
    var distanceCm by remember { mutableStateOf(session.phoneB.distanceCm?.toString() ?: "40") }
    var cameraAngle by remember { mutableStateOf(session.phoneB.cameraAngleDegrees?.toString() ?: "90") }
    var lighting by remember { mutableStateOf(session.phoneB.ambientLighting ?: "Darkened Room (5 lux)") }
    var captureMode by remember { mutableStateOf(session.phoneB.captureMode) }
    var iso by remember { mutableStateOf(session.phoneB.iso?.toString() ?: "100") }
    var exposureTime by remember { mutableStateOf(session.phoneB.exposureTimeSeconds ?: "1/120") }
    var sessionNotes by remember { mutableStateOf(session.notes) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val nextRep = currentSession.captures.size + 1
            when (val res = importer.importCapture(uri, currentSession.experimentId, nextRep, repository)) {
                is CaptureImporter.ImportResult.Success -> {
                    val updatedCaptures = currentSession.captures + res.captureItem
                    currentSession = currentSession.copy(captures = updatedCaptures)
                    onSaveSession(currentSession)
                    Toast.makeText(context, "Imported ${res.captureItem.filename} (Repetition #$nextRep)", Toast.LENGTH_SHORT).show()
                }
                is CaptureImporter.ImportResult.Error -> {
                    Toast.makeText(context, "Import Rejected: ${res.reason}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Camera Capture", color = LabTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = LabTextPrimary
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
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = LabDarkCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Associate Phone B Photograph",
                            style = MaterialTheme.typography.titleMedium,
                            color = LabPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Session: ${currentSession.experimentId}\nAsset: ${currentSession.sourceImage.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = LabTextSecondary
                        )
                    }
                }
            }

            // Capture conditions form
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, LabBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = LabDarkSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Phone B Acquisition Conditions",
                            fontWeight = FontWeight.Bold,
                            color = LabTextPrimary
                        )

                        OutlinedTextField(
                            value = phoneBModel,
                            onValueChange = { phoneBModel = it },
                            label = { Text("Phone B Device Model") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = LabTextPrimary,
                                unfocusedTextColor = LabTextPrimary
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = distanceCm,
                                onValueChange = { distanceCm = it },
                                label = { Text("Distance (cm)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = LabTextPrimary,
                                    unfocusedTextColor = LabTextPrimary
                                )
                            )
                            OutlinedTextField(
                                value = cameraAngle,
                                onValueChange = { cameraAngle = it },
                                label = { Text("Angle (deg)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = LabTextPrimary,
                                    unfocusedTextColor = LabTextPrimary
                                )
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = iso,
                                onValueChange = { iso = it },
                                label = { Text("ISO") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = LabTextPrimary,
                                    unfocusedTextColor = LabTextPrimary
                                )
                            )
                            OutlinedTextField(
                                value = exposureTime,
                                onValueChange = { exposureTime = it },
                                label = { Text("Shutter Speed") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = LabTextPrimary,
                                    unfocusedTextColor = LabTextPrimary
                                )
                            )
                        }

                        OutlinedTextField(
                            value = lighting,
                            onValueChange = { lighting = it },
                            label = { Text("Ambient Lighting") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = LabTextPrimary,
                                unfocusedTextColor = LabTextPrimary
                            )
                        )

                        OutlinedTextField(
                            value = sessionNotes,
                            onValueChange = { sessionNotes = it },
                            label = { Text("Qualitative Notes & Observations") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = LabTextPrimary,
                                unfocusedTextColor = LabTextPrimary
                            )
                        )
                    }
                }
            }

            // File picker action
            item {
                Button(
                    onClick = {
                        // Persist telemetry changes first
                        val updated = currentSession.copy(
                            phoneB = PhoneBCaptureInfo(
                                model = phoneBModel,
                                distanceCm = distanceCm.toFloatOrNull(),
                                cameraAngleDegrees = cameraAngle.toFloatOrNull(),
                                iso = iso.toIntOrNull(),
                                exposureTimeSeconds = exposureTime,
                                ambientLighting = lighting,
                                captureMode = captureMode
                            ),
                            notes = sessionNotes
                        )
                        currentSession = updated
                        onSaveSession(updated)
                        filePickerLauncher.launch(arrayOf("image/jpeg", "image/png", "image/webp"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = LabPrimary)
                ) {
                    Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, tint = LabDarkBg)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Import Original Photograph (Repetition #${currentSession.captures.size + 1})",
                        color = LabDarkBg,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // List of imported captures
            item {
                Text(
                    text = "Imported Captures (${currentSession.captures.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = LabTextPrimary
                )
            }

            items(currentSession.captures) { cap ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, LabBorder, RoundedCornerShape(10.dp)),
                    colors = CardDefaults.cardColors(containerColor = LabDarkSurface)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = LabSuccess,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Repetition #${cap.repetitionNumber}: ${cap.filename}",
                                fontWeight = FontWeight.Bold,
                                color = LabTextPrimary
                            )
                            Text(
                                text = "Size: ${cap.fileSizeBytes / 1024} KB • SHA-256: ${cap.hashSha256?.take(12)}...",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = LabTextSecondary
                            )
                            if (cap.exif?.make != null || cap.exif?.model != null) {
                                Text(
                                    text = "Camera: ${cap.exif.make} ${cap.exif.model} (ISO ${cap.exif.iso}, ${cap.exif.exposureTime}s)",
                                    fontSize = 11.sp,
                                    color = LabPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Complete Session & View Results
            if (currentSession.captures.isNotEmpty()) {
                item {
                    Button(
                        onClick = {
                            val updated = currentSession.copy(
                                phoneB = PhoneBCaptureInfo(
                                    model = phoneBModel,
                                    distanceCm = distanceCm.toFloatOrNull(),
                                    cameraAngleDegrees = cameraAngle.toFloatOrNull(),
                                    iso = iso.toIntOrNull(),
                                    exposureTimeSeconds = exposureTime,
                                    ambientLighting = lighting,
                                    captureMode = captureMode
                                ),
                                notes = sessionNotes
                            )
                            onSaveSession(updated)
                            onNavigateResults(updated.experimentId)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = LabSuccess)
                    ) {
                        Text("View Session Details & Export", color = LabDarkBg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
