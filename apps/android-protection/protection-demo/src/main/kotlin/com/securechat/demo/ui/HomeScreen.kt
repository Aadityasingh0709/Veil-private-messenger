package com.securechat.demo.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securechat.demo.ui.theme.EmeraldGreen
import com.securechat.demo.ui.theme.PrimaryBlue
import com.securechat.demo.ui.theme.Slate500
import com.securechat.protection.ScreenshotDetectionController

@Composable
fun HomeScreen(
    onNavigateNormalViewer: () -> Unit,
    onNavigateProtectedViewer: () -> Unit,
    onNavigateScreenshotDetection: () -> Unit,
    onNavigateTestingLab: () -> Unit,
    onNavigateInfo: () -> Unit
) {
    val scrollState = rememberScrollState()
    val isDetectionSupported = ScreenshotDetectionController.isSupported()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        // App Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Private Messenger",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Phase 3 · Native Protection SDK Demo",
                    style = MaterialTheme.typography.labelMedium,
                    color = Slate500
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Device & SDK Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SYSTEM & SDK ENVIRONMENT",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
                Spacer(modifier = Modifier.height(10.dp))

                InfoRow(label = "SDK Module", value = "capture-protection v1.0.0 (AAR)")
                InfoRow(label = "Android Version", value = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                InfoRow(label = "Device Hardware", value = "${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
                InfoRow(
                    label = "FLAG_SECURE",
                    value = "Available (OS Window Surface)",
                    valueColor = EmeraldGreen
                )
                InfoRow(
                    label = "Screenshot Detection",
                    value = if (isDetectionSupported) "Supported (API >= 34)" else "Unsupported (Requires API >= 34)",
                    valueColor = if (isDetectionSupported) EmeraldGreen else Slate500
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "VERIFICATION SUITE",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Slate500
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Navigation Mode 1: Normal Viewer (Baseline)
        NavModeCard(
            title = "1. Normal Viewer (Baseline)",
            subtitle = "Displays sensitive content without FLAG_SECURE. Screenshots and recordings capture normally for comparison.",
            icon = Icons.Default.LockOpen,
            badgeColor = Slate500,
            badgeText = "UNPROTECTED",
            onClick = onNavigateNormalViewer
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Navigation Mode 2: Protected Viewer (SDK Applied)
        NavModeCard(
            title = "2. Protected Media Viewer",
            subtitle = "Activates FLAG_SECURE via SecureWindowController. Blocks screenshots, blackouts screen recordings, and censors task preview.",
            icon = Icons.Default.Lock,
            badgeColor = EmeraldGreen,
            badgeText = "PROTECTED",
            onClick = onNavigateProtectedViewer
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Navigation Mode 3: Screenshot Detection Demo (API 34+)
        NavModeCard(
            title = "3. Screenshot Detection Demo",
            subtitle = "Tests Android 14+ registerScreenCaptureCallback. Distinct mode: observes screenshots taken while window is not protected.",
            icon = Icons.Default.Notifications,
            badgeColor = if (isDetectionSupported) PrimaryBlue else Slate500,
            badgeText = if (isDetectionSupported) "API 34+" else "INACTIVE",
            onClick = onNavigateScreenshotDetection
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Navigation Mode 4: Capture Testing Lab
        NavModeCard(
            title = "4. Capture Testing Lab",
            subtitle = "Interactive QA checklist to test and log PASS/FAIL results for screenshots, video capture, recent-apps, and HDMI displays.",
            icon = Icons.Default.Science,
            badgeColor = PrimaryBlue,
            badgeText = "LAB",
            onClick = onNavigateTestingLab
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Navigation Mode 5: SDK Info & Limitations
        NavModeCard(
            title = "5. SDK Architecture & Limitations",
            subtitle = "Technical documentation of FLAG_SECURE mechanics, SurfaceFlinger enforcement, and why physical camera capture requires Phase 4.",
            icon = Icons.Default.Info,
            badgeColor = Slate500,
            badgeText = "DOCS",
            onClick = onNavigateInfo
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun InfoRow(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Slate500)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (valueColor != Color.Unspecified) valueColor else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun NavModeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeColor: Color,
    badgeText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(badgeColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = badgeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Slate500,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
