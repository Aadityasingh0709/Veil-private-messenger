package com.securechat.opticalresearch.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.securechat.opticalresearch.assets.TestChartGenerator
import com.securechat.opticalresearch.assets.TestChartType
import com.securechat.opticalresearch.data.SessionRepository
import com.securechat.opticalresearch.display.DisplayDiagnosticManager
import com.securechat.opticalresearch.model.*
import com.securechat.opticalresearch.renderer.*
import com.securechat.opticalresearch.ui.theme.LabError
import com.securechat.opticalresearch.ui.theme.LabPrimary
import java.io.File

/**
 * Dedicated display Activity for Phase 4B Modulation Experiments.
 *
 * Supports Mode 1 (Spatial), Mode 2 (Temporal), Mode 3 (Combined), and Static Baseline.
 * - Hardware VSYNC synchronized frame loop via Choreographer
 * - Real-time diagnostics HUD overlay
 * - Slide-up parameter adjustment control panel
 * - Emergency stop control
 * - Serializes modulation parameters and diagnostics snapshot into session on exit
 */
class ModulationDisplayActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CHART_TYPE = "extra_chart_type"
        const val EXTRA_EXPERIMENT_ID = "extra_experiment_id"
        const val EXTRA_MODULATION_MODE = "extra_modulation_mode" // "spatial", "temporal", "combined", "baseline"
    }

    private lateinit var surfaceView: OpticalSurfaceView
    private lateinit var activeRenderer: OpticalRenderer
    private var chartBitmap: Bitmap? = null
    private lateinit var repository: SessionRepository
    private lateinit var currentExperimentId: String

    private val diagnostics = RenderingDiagnostics()
    private var currentParams: ModulationParams = ModulationParams.Spatial()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        repository = SessionRepository(this)

        val chartTypeName = intent.getStringExtra(EXTRA_CHART_TYPE) ?: TestChartType.COMBINED_QUALITY.name
        val chartType = try {
            TestChartType.valueOf(chartTypeName)
        } catch (_: Exception) {
            TestChartType.COMBINED_QUALITY
        }

        currentExperimentId = intent.getStringExtra(EXTRA_EXPERIMENT_ID) ?: ExperimentSession.generateExperimentId()
        val modeStr = intent.getStringExtra(EXTRA_MODULATION_MODE) ?: "spatial"

        currentParams = when (modeStr.lowercase()) {
            "temporal" -> ModulationParams.Temporal()
            "combined" -> ModulationParams.Combined()
            "baseline" -> ModulationParams.Baseline
            else -> ModulationParams.Spatial()
        }

        // Generate synthetic reference chart bitmap
        val bmp = TestChartGenerator.generateChart(chartType)
        this.chartBitmap = bmp

        val sourceDir = repository.getSourceDir(currentExperimentId)
        val refFile = File(sourceDir, "reference.png")
        val sha256 = TestChartGenerator.saveChartToFile(bmp, refFile)

        // Initialize session in repository
        val displayManager = DisplayDiagnosticManager(this)
        val phoneAInfo = displayManager.queryCurrentDisplayInfo()
        val initialTestMode = ModulationSessionExtras.testModeFromParams(currentParams)

        val existingSession = repository.getSession(currentExperimentId)
        if (existingSession == null) {
            val extras = ModulationSessionExtras.from(currentParams)
            val newSession = ExperimentSession(
                experimentId = currentExperimentId,
                testMode = initialTestMode,
                sourceImage = SourceImageDetails(
                    name = "reference.png",
                    hashSha256 = sha256,
                    width = bmp.width,
                    height = bmp.height,
                    localUri = refFile.absolutePath
                ),
                phoneA = phoneAInfo,
                notes = ModulationSessionExtras.embedInNotes("", extras)
            )
            repository.saveSession(newSession)
        }

        // Create initial renderer
        activeRenderer = createRenderer(currentParams, bmp)
        surfaceView = OpticalSurfaceView(this).apply {
            setRenderer(activeRenderer, bmp)
        }

        setContent {
            var paramsState by remember { mutableStateOf(currentParams) }
            var isPanelOpen by remember { mutableStateOf(false) }

            Box(modifier = Modifier.fillMaxSize()) {
                // Hardware surface view
                AndroidView(
                    factory = { surfaceView },
                    modifier = Modifier.fillMaxSize()
                )

                // Top controls bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$currentExperimentId • ${chartType.title}",
                            color = LabPrimary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Toggle Control Panel
                        IconButton(
                            onClick = { isPanelOpen = !isPanelOpen },
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.75f), CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Modulation Controls",
                                tint = LabPrimary
                            )
                        }

                        // Close / Exit
                        IconButton(
                            onClick = { finish() },
                            modifier = Modifier
                                .background(LabError, CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "End Experiment",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Diagnostics HUD overlay (top right)
                RenderingDiagnosticsOverlay(
                    diagnostics = diagnostics,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 68.dp, end = 16.dp)
                )

                // Slide-up parameter adjustment panel
                AnimatedVisibility(
                    visible = isPanelOpen,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    ModulationControlPanel(
                        params = paramsState,
                        onParamsChange = { newParams ->
                            paramsState = newParams
                            currentParams = newParams
                            applyNewParams(newParams, bmp)
                        },
                        onEmergencyStop = {
                            triggerEmergencyStop()
                        }
                    )
                }
            }
        }
    }

    private fun createRenderer(params: ModulationParams, bmp: Bitmap): OpticalRenderer {
        return when (params) {
            is ModulationParams.Spatial -> SpatialModulationRenderer(currentExperimentId, params)
            is ModulationParams.Temporal -> TemporalModulationRenderer(currentExperimentId, params, diagnostics)
            is ModulationParams.Combined -> CombinedModulationRenderer(currentExperimentId, params, diagnostics)
            is ModulationParams.Baseline -> NormalBaselineRenderer(currentExperimentId)
        }
    }

    private fun applyNewParams(newParams: ModulationParams, bmp: Bitmap) {
        val needsRecreate = when (activeRenderer) {
            is SpatialModulationRenderer -> newParams !is ModulationParams.Spatial
            is TemporalModulationRenderer -> newParams !is ModulationParams.Temporal
            is CombinedModulationRenderer -> newParams !is ModulationParams.Combined
            is NormalBaselineRenderer -> newParams !is ModulationParams.Baseline
            else -> true
        }

        if (needsRecreate) {
            activeRenderer.cleanup()
            activeRenderer = createRenderer(newParams, bmp)
            surfaceView.setRenderer(activeRenderer, bmp)
        } else {
            when (val r = activeRenderer) {
                is SpatialModulationRenderer -> (newParams as? ModulationParams.Spatial)?.let { r.updateParams(it) }
                is TemporalModulationRenderer -> (newParams as? ModulationParams.Temporal)?.let { r.updateParams(it) }
                is CombinedModulationRenderer -> (newParams as? ModulationParams.Combined)?.let { r.updateParams(it) }
                else -> Unit
            }
        }
    }

    private fun triggerEmergencyStop() {
        when (val r = activeRenderer) {
            is TemporalModulationRenderer -> r.emergencyStop = true
            is CombinedModulationRenderer -> r.emergencyStop = true
            else -> Unit
        }
    }

    private fun persistSessionState() {
        val existingSession = repository.getSession(currentExperimentId) ?: return
        val testMode = ModulationSessionExtras.testModeFromParams(currentParams)
        val extras = ModulationSessionExtras.from(currentParams, diagnostics.snapshot())
        val updatedNotes = ModulationSessionExtras.embedInNotes(existingSession.notes, extras)
        val updatedSession = existingSession.copy(
            testMode = testMode,
            notes = updatedNotes
        )
        repository.saveSession(updatedSession)
    }

    override fun onDestroy() {
        super.onDestroy()
        persistSessionState()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        activeRenderer.cleanup()
        chartBitmap?.recycle()
        chartBitmap = null
    }
}
