package com.securechat.opticalresearch.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securechat.opticalresearch.model.PhoneADeviceInfo
import com.securechat.opticalresearch.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationScreen(
    deviceInfo: PhoneADeviceInfo,
    onRefreshTelemetry: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device & Display Calibration", color = LabTextPrimary) },
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
                    IconButton(onClick = onRefreshTelemetry) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
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
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = LabDarkCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = LabWarning,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Dynamic Panel Refresh Rate Notice",
                                style = MaterialTheme.typography.titleMedium,
                                color = LabTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Modern Android displays utilize LTPO / adaptive variable refresh rates (e.g. 1Hz–120Hz). A requested frame rate is not guaranteed to be delivered by the panel under static conditions.",
                                fontSize = 12.sp,
                                color = LabTextSecondary
                            )
                        }
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
                        Text(
                            text = "Phone A Display Diagnostics",
                            style = MaterialTheme.typography.titleMedium,
                            color = LabPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        TelemetryRow("Manufacturer", deviceInfo.manufacturer ?: "Unknown")
                        TelemetryRow("Hardware Model", deviceInfo.model ?: "Unknown")
                        TelemetryRow("Android Version", "Android ${deviceInfo.androidVersion}")
                        TelemetryRow("API Level", "API ${deviceInfo.apiLevel}")
                        TelemetryRow("Native Pixel Resolution", deviceInfo.displayResolution ?: "Unknown")
                        TelemetryRow(
                            "Current Refresh Rate",
                            "${deviceInfo.refreshRateHz ?: 60.0f} Hz",
                            highlightColor = LabSuccess
                        )
                        TelemetryRow(
                            "Supported Refresh Rates",
                            deviceInfo.supportedRefreshRates?.joinToString(", ") { "${it}Hz" } ?: "60Hz"
                        )
                        TelemetryRow("Screen Orientation", deviceInfo.screenOrientation ?: "PORTRAIT")
                        TelemetryRow(
                            "System Brightness",
                            if (deviceInfo.screenBrightnessPercent != null) "${deviceInfo.screenBrightnessPercent.toInt()}%" else "Not readable via API"
                        )
                        TelemetryRow(
                            "Adaptive Brightness",
                            deviceInfo.adaptiveBrightness?.toString() ?: "Not readable via API"
                        )
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
                        Text(
                            text = "Phase 4A Experimental Calibration Protocol",
                            style = MaterialTheme.typography.titleMedium,
                            color = LabTextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "1. Set Phone A brightness to a fixed level (e.g. 80-100%) and disable adaptive brightness.\n" +
                                    "2. Ensure display refresh rate is locked or noted for accurate rolling-shutter correlation.\n" +
                                    "3. Keep ambient room lighting constant (preferably darkened or fixed lux).\n" +
                                    "4. Maintain a steady Phone B tripod distance (30-50 cm) perpendicular to Phone A.",
                            fontSize = 13.sp,
                            color = LabTextSecondary,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}
