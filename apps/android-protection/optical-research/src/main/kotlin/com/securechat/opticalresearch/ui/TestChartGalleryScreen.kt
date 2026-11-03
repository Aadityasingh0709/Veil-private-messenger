package com.securechat.opticalresearch.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securechat.opticalresearch.assets.TestChartType
import com.securechat.opticalresearch.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestChartGalleryScreen(
    onSelectChart: (TestChartType) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Synthetic Test Chart Gallery", color = LabTextPrimary) },
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Deterministic Reference Charts (1080x1920)",
                    style = MaterialTheme.typography.titleMedium,
                    color = LabPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select a synthetic calibration chart to display on Phone A with corner fiducial markers. Does not require user personal media.",
                    fontSize = 12.sp,
                    color = LabTextSecondary
                )
            }

            items(TestChartType.values()) { chart ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, LabBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = LabDarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = chart.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = LabTextPrimary
                            )
                            Badge(containerColor = LabBorder) {
                                Text(
                                    text = "1080x1920",
                                    fontSize = 10.sp,
                                    color = LabTextSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = chart.description,
                            fontSize = 13.sp,
                            color = LabTextSecondary,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { onSelectChart(chart) },
                            colors = ButtonDefaults.buttonColors(containerColor = LabPrimary),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = LabDarkBg,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Display on Phone A", color = LabDarkBg, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
