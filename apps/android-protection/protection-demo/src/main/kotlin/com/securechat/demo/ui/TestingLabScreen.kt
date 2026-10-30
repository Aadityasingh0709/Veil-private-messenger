package com.securechat.demo.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securechat.demo.ui.theme.EmeraldGreen
import com.securechat.demo.ui.theme.PrimaryBlue
import com.securechat.demo.ui.theme.RedCrimson
import com.securechat.demo.ui.theme.Slate200
import com.securechat.demo.ui.theme.Slate500
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TestVerdict {
    NOT_TESTED,
    PASS,
    FAIL
}

data class TestCase(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val passCriteria: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestingLabScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    val testCases = remember {
        listOf(
            TestCase(
                id = "TC-01",
                category = "Screenshot Prevention",
                title = "Protected Viewer Screenshot Block",
                description = "Press hardware Power + Volume Down while in Protected Viewer.",
                passCriteria = "Toast 'Can't take screenshot due to security policy' or black image file."
            ),
            TestCase(
                id = "TC-02",
                category = "Screenshot Baseline",
                title = "Normal Viewer Screenshot Capture",
                description = "Press hardware Power + Volume Down while in Normal Viewer.",
                passCriteria = "Screenshot clearly contains the sample confidential directive."
            ),
            TestCase(
                id = "TC-03",
                category = "Screen Recording",
                title = "Protected Viewer Video Recording Block",
                description = "Record screen using system recorder while navigating through Protected Viewer.",
                passCriteria = "Output video replaces protected window surface with solid black rectangle."
            ),
            TestCase(
                id = "TC-04",
                category = "Task Switcher",
                title = "Recent-Apps Preview Censor",
                description = "Open Android app switcher / recents carousel while Protected Viewer was active.",
                passCriteria = "App preview thumbnail is blacked out or hidden in the task switcher."
            ),
            TestCase(
                id = "TC-05",
                category = "Screenshot Detection",
                title = "Android 14+ Detection Callback",
                description = "Take screenshot on Screenshot Detection Demo screen (Android 14+ only).",
                passCriteria = "Counter increments and detection toast triggers (or correctly shows Unsupported on < API 34)."
            ),
            TestCase(
                id = "TC-06",
                category = "External Display",
                title = "Non-Secure Display Projection",
                description = "Connect phone to Miracast / non-HDCP external display if available.",
                passCriteria = "Protected window is suppressed on the non-secure display or Marked as NOT_TESTED if no display available."
            )
        )
    }

    val verdicts = remember { mutableStateMapOf<String, TestVerdict>().apply {
        testCases.forEach { put(it.id, TestVerdict.NOT_TESTED) }
    }}

    val notes = remember { mutableStateMapOf<String, String>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Capture Testing Lab") },
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
            // Header summary
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "PHYSICAL DEVICE QA VERIFICATION",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})\n" +
                                "OS: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
                                "Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = Slate500,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Instructions: Perform each physical test on your device and record the observed outcome below. Export the resulting verification log to Markdown when finished.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Test Case Cards
            testCases.forEach { tc ->
                val verdict = verdicts[tc.id] ?: TestVerdict.NOT_TESTED
                val note = notes[tc.id] ?: ""

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${tc.id}: ${tc.title}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tc.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = tc.description, style = MaterialTheme.typography.bodySmall, color = Slate500)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Pass Criteria: ${tc.passCriteria}",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = Slate500
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Verdict Selector
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = verdict == TestVerdict.PASS,
                                onClick = { verdicts[tc.id] = TestVerdict.PASS },
                                label = { Text("PASS", fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldGreen.copy(alpha = 0.2f),
                                    selectedLabelColor = EmeraldGreen
                                )
                            )
                            FilterChip(
                                selected = verdict == TestVerdict.FAIL,
                                onClick = { verdicts[tc.id] = TestVerdict.FAIL },
                                label = { Text("FAIL", fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RedCrimson.copy(alpha = 0.2f),
                                    selectedLabelColor = RedCrimson
                                )
                            )
                            FilterChip(
                                selected = verdict == TestVerdict.NOT_TESTED,
                                onClick = { verdicts[tc.id] = TestVerdict.NOT_TESTED },
                                label = { Text("NOT TESTED") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Slate200,
                                    selectedLabelColor = Slate500
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = note,
                            onValueChange = { notes[tc.id] = it },
                            placeholder = { Text("Add optional observation note…", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Export Markdown Report Button
            Button(
                onClick = {
                    val report = buildString {
                        appendLine("### Physical Device Verification Results")
                        appendLine("- **Device**: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
                        appendLine("- **Android OS**: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                        appendLine("- **Date**: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
                        appendLine()
                        appendLine("| ID | Test Case | Category | Verdict | Notes |")
                        appendLine("|---|---|---|---|---|")
                        testCases.forEach { tc ->
                            val v = verdicts[tc.id] ?: TestVerdict.NOT_TESTED
                            val n = notes[tc.id]?.ifBlank { "N/A" } ?: "N/A"
                            appendLine("| ${tc.id} | ${tc.title} | ${tc.category} | **$v** | $n |")
                        }
                    }

                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("QA Verification Report", report))
                    Toast.makeText(context, "Verification report copied to clipboard!", Toast.LENGTH_LONG).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy Markdown QA Report to Clipboard", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
