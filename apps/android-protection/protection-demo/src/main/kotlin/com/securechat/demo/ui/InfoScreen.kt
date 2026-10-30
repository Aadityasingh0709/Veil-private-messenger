package com.securechat.demo.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securechat.demo.ui.theme.AmberWarning
import com.securechat.demo.ui.theme.PrimaryBlue
import com.securechat.demo.ui.theme.Slate500

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SDK Info & Platform Limits") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Mandatory Disclaimer Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AmberWarning.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = AmberWarning,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "SECURITY BOUNDARY & HONEST CLAIMS",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = AmberWarning
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "FLAG_SECURE is an operating system flag. It provides strong software-level protection on standard Android devices, but it does NOT guarantee universal capture resistance or immunity against external physical camera photography.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SectionCard(
                title = "1. How FLAG_SECURE Operates",
                content = "• Window-Level Surface Control: FLAG_SECURE is an attribute of the WindowManager.LayoutParams. When set, SurfaceFlinger (Android's display compositor) excludes that window surface from screenshot buffers and mirrors.\n\n" +
                        "• Task Switcher Preview: The system blanks or replaces the app thumbnail in the Recents overview.\n\n" +
                        "• Not a Canvas Filter: FLAG_SECURE cannot be applied selectively to a single image or text component within an unprotected window. The entire window surface is either secure or not."
            )

            Spacer(modifier = Modifier.height(14.dp))

            SectionCard(
                title = "2. Screenshot Detection vs. Prevention",
                content = "• Android 14+ ScreenCaptureCallback: Detects when a hardware screenshot is taken. It does NOT prevent or cancel the screenshot.\n\n" +
                        "• Callback Suppression: When FLAG_SECURE is enabled, Android suppresses the ScreenCaptureCallback because screenshots are blocked at the compositor level.\n\n" +
                        "• ADB Exclusion: Official Android documentation notes that ADB screencap and instrumentation capture do NOT trigger the callback."
            )

            Spacer(modifier = Modifier.height(14.dp))

            SectionCard(
                title = "3. Known Platform & Hardware Limitations",
                content = "• OEM Custom ROMs: Certain vendor-modified Android distributions or custom ROMs have historically exhibited inconsistent FLAG_SECURE enforcement or screen-recording loopholes.\n\n" +
                        "• System Accessibility / Overlays: Rogue apps with system alert window or accessibility permissions may attempt overlay-based inference or tapjacking.\n\n" +
                        "• Root & Kernel Exploit Vectors: Devices with modified bootloaders, Magisk, or kernel-level frame-buffer access can bypass WindowManager policies.\n\n" +
                        "• External Displays & HDCP: FLAG_SECURE disables display mirroring on non-secure external displays. However, high-end hardware capture cards with HDCP stripping (e.g. Elgato) present hardware-level attack vectors outside Android's software boundary."
            )

            Spacer(modifier = Modifier.height(14.dp))

            SectionCard(
                title = "4. The Analog Hole & The Path to Phase 4",
                content = "• The Physical Camera Problem: No OS flag can prevent a user from pointing another smartphone or camera at the screen and taking a photograph.\n\n" +
                        "• Future Optical Anti-Capture: Phase 4 will introduce optical anti-capture research (spatial/temporal modulation) designed to degrade external camera photography while maintaining acceptable human readability.\n\n" +
                        "• Phase 3 Focus: Establishing the baseline native Android SDK, reference counting, and OS window controls."
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun SectionCard(title: String, content: String) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                color = Slate500,
                lineHeight = 18.sp
            )
        }
    }
}
