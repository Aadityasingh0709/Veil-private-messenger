package com.securechat.opticalresearch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securechat.opticalresearch.model.ModulationParams
import com.securechat.opticalresearch.model.SpatialPatternType
import com.securechat.opticalresearch.ui.theme.*

/**
 * Phase 4B: Live Experimental Modulation Control Panel.
 *
 * Provides researcher controls for real-time modulation parameter adjustment:
 * - Mode selection: Baseline, Spatial, Temporal, Combined
 * - Spatial controls: pattern geometry, grid period px, contrast ratio (capped at 15%), orientation
 * - Temporal controls: frequency (0.5–3.0 Hz, capped at 3.0 Hz WCAG ceiling), duty cycle, amplitude (capped at 15%)
 * - Prominent emergency STOP control
 * - Real-time validation warning display
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModulationControlPanel(
    params: ModulationParams,
    onParamsChange: (ModulationParams) -> Unit,
    onEmergencyStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val validationError = params.validate()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        colors = CardDefaults.cardColors(containerColor = LabDarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with Emergency Stop Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MODULATION CONTROLS",
                        color = LabPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Phase 4B Parameter Suite",
                        color = LabTextSecondary,
                        fontSize = 11.sp
                    )
                }

                Button(
                    onClick = onEmergencyStop,
                    colors = ButtonDefaults.buttonColors(containerColor = LabError),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "⏹ STOP",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Validation Warning if any
            if (validationError != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LabError.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = "Warning", tint = LabError)
                    Text(
                        text = validationError,
                        color = LabError,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Mode Selector
            Text(
                text = "TEST MODE",
                color = LabTextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val modes = listOf("Static" to 0, "Spatial" to 1, "Temporal" to 2, "Combined" to 3)
                val currentModeIndex = when (params) {
                    is ModulationParams.Baseline -> 0
                    is ModulationParams.Spatial -> 1
                    is ModulationParams.Temporal -> 2
                    is ModulationParams.Combined -> 3
                }

                modes.forEach { (label, index) ->
                    val isSelected = currentModeIndex == index
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (!isSelected) {
                                val newParams: ModulationParams = when (index) {
                                    0 -> ModulationParams.Baseline
                                    1 -> ModulationParams.Spatial()
                                    2 -> ModulationParams.Temporal()
                                    3 -> ModulationParams.Combined()
                                    else -> ModulationParams.Baseline
                                }
                                onParamsChange(newParams)
                            }
                        },
                        label = { Text(label, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LabPrimary,
                            selectedLabelColor = Color.Black,
                            containerColor = LabDarkCard,
                            labelColor = LabTextPrimary
                        )
                    )
                }
            }

            Divider(color = LabBorder, thickness = 1.dp)

            // Mode-specific controls
            when (params) {
                is ModulationParams.Baseline -> {
                    Text(
                        text = "Static baseline rendering active. No spatial or temporal perturbation applied.",
                        color = LabTextSecondary,
                        fontSize = 12.sp
                    )
                }

                is ModulationParams.Spatial -> {
                    SpatialControlsSection(
                        spatial = params,
                        onChange = { onParamsChange(it) }
                    )
                }

                is ModulationParams.Temporal -> {
                    TemporalControlsSection(
                        temporal = params,
                        onChange = { onParamsChange(it) }
                    )
                }

                is ModulationParams.Combined -> {
                    Text(
                        text = "SPATIAL MODULATION",
                        color = LabWarning,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    SpatialControlsSection(
                        spatial = params.spatial,
                        onChange = { onParamsChange(params.copy(spatial = it)) }
                    )

                    Divider(color = LabBorder, thickness = 1.dp)

                    Text(
                        text = "TEMPORAL MODULATION",
                        color = LabWarning,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    TemporalControlsSection(
                        temporal = params.temporal,
                        onChange = { onParamsChange(params.copy(temporal = it)) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpatialControlsSection(
    spatial: ModulationParams.Spatial,
    onChange: (ModulationParams.Spatial) -> Unit
) {
    var expandedPatternDropdown by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Pattern Type Dropdown
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Pattern:", color = LabTextPrimary, fontSize = 12.sp)
            ExposedDropdownMenuBox(
                expanded = expandedPatternDropdown,
                onExpandedChange = { expandedPatternDropdown = !expandedPatternDropdown }
            ) {
                OutlinedTextField(
                    value = spatial.patternType.displayLabel,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.menuAnchor().width(180.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = LabTextPrimary
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LabPrimary,
                        unfocusedBorderColor = LabBorder
                    )
                )
                ExposedDropdownMenu(
                    expanded = expandedPatternDropdown,
                    onDismissRequest = { expandedPatternDropdown = false }
                ) {
                    SpatialPatternType.values().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.displayLabel, fontSize = 12.sp) },
                            onClick = {
                                onChange(spatial.copy(patternType = type))
                                expandedPatternDropdown = false
                            }
                        )
                    }
                }
            }
        }

        // Grid Period Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Period:", color = LabTextPrimary, fontSize = 12.sp)
            Text("${spatial.gridPeriodPx} px", color = LabPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
        Slider(
            value = spatial.gridPeriodPx.toFloat(),
            onValueChange = { onChange(spatial.copy(gridPeriodPx = it.toInt())) },
            valueRange = 2f..64f,
            steps = 30,
            colors = SliderDefaults.colors(thumbColor = LabPrimary, activeTrackColor = LabPrimary)
        )

        // Contrast Ratio Slider (max 15%)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Contrast (max 15%):", color = LabTextPrimary, fontSize = 12.sp)
            Text(
                "${(spatial.contrastRatio * 100).toInt()}%",
                color = LabPrimary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Slider(
            value = spatial.contrastRatio,
            onValueChange = { onChange(spatial.copy(contrastRatio = it)) },
            valueRange = 0.01f..ModulationParams.Spatial.MAX_CONTRAST_RATIO,
            steps = 13,
            colors = SliderDefaults.colors(thumbColor = LabPrimary, activeTrackColor = LabPrimary)
        )

        // Orientation Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Orientation:", color = LabTextPrimary, fontSize = 12.sp)
            Text("${spatial.orientationDeg.toInt()}°", color = LabPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
        Slider(
            value = spatial.orientationDeg,
            onValueChange = { onChange(spatial.copy(orientationDeg = it)) },
            valueRange = 0f..179f,
            steps = 11,
            colors = SliderDefaults.colors(thumbColor = LabPrimary, activeTrackColor = LabPrimary)
        )
    }
}

@Composable
private fun TemporalControlsSection(
    temporal: ModulationParams.Temporal,
    onChange: (ModulationParams.Temporal) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Frequency Slider (0.5 to 3.0 Hz WCAG ceiling)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Frequency (max 3 Hz):", color = LabTextPrimary, fontSize = 12.sp)
            Text(
                String.format("%.1f Hz", temporal.modulationFrequencyHz),
                color = LabPrimary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Slider(
            value = temporal.modulationFrequencyHz,
            onValueChange = { onChange(temporal.copy(modulationFrequencyHz = (it * 10).toInt() / 10.0f)) },
            valueRange = 0.5f..ModulationParams.Temporal.MAX_MODULATION_HZ,
            steps = 24,
            colors = SliderDefaults.colors(thumbColor = LabPrimary, activeTrackColor = LabPrimary)
        )

        // Duty Cycle Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Duty Cycle:", color = LabTextPrimary, fontSize = 12.sp)
            Text(
                "${(temporal.dutyCycle * 100).toInt()}%",
                color = LabPrimary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Slider(
            value = temporal.dutyCycle,
            onValueChange = { onChange(temporal.copy(dutyCycle = it)) },
            valueRange = 0.1f..0.9f,
            steps = 15,
            colors = SliderDefaults.colors(thumbColor = LabPrimary, activeTrackColor = LabPrimary)
        )

        // Amplitude Slider (max 15%)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Amplitude (max 15%):", color = LabTextPrimary, fontSize = 12.sp)
            Text(
                "${(temporal.amplitudeFraction * 100).toInt()}%",
                color = LabPrimary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Slider(
            value = temporal.amplitudeFraction,
            onValueChange = { onChange(temporal.copy(amplitudeFraction = it)) },
            valueRange = 0.01f..ModulationParams.Temporal.MAX_AMPLITUDE,
            steps = 13,
            colors = SliderDefaults.colors(thumbColor = LabPrimary, activeTrackColor = LabPrimary)
        )
    }
}
