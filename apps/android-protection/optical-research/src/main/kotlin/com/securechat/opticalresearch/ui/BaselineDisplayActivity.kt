package com.securechat.opticalresearch.ui

import android.app.Activity
import android.graphics.Bitmap
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import com.securechat.opticalresearch.display.DisplayDiagnosticManager
import com.securechat.opticalresearch.model.*
import com.securechat.opticalresearch.renderer.NormalBaselineRenderer
import com.securechat.opticalresearch.renderer.OpticalSurfaceView
import com.securechat.opticalresearch.ui.theme.LabDarkBg
import com.securechat.opticalresearch.ui.theme.LabError
import com.securechat.opticalresearch.ui.theme.LabPrimary
import java.io.File

/**
 * Dedicated research display Activity for Phone A.
 *
 * Implements:
 * - Dedicated SurfaceView with hardware VSYNC rendering.
 * - FLAG_KEEP_SCREEN_ON to prevent display timeout during camera photography.
 * - Fullscreen display with high-contrast corner fiducials.
 * - Session tracking and export preparation.
 * - Strictly static image (no flicker or temporal modulation in Phase 4A).
 */
class BaselineDisplayActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CHART_TYPE = "extra_chart_type"
        const val EXTRA_EXPERIMENT_ID = "extra_experiment_id"
    }

    private lateinit var surfaceView: OpticalSurfaceView
    private lateinit var renderer: NormalBaselineRenderer
    private var chartBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prevent display sleep during experiment
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val chartTypeName = intent.getStringExtra(EXTRA_CHART_TYPE) ?: TestChartType.COMBINED_QUALITY.name
        val chartType = try {
            TestChartType.valueOf(chartTypeName)
        } catch (e: Exception) {
            TestChartType.COMBINED_QUALITY
        }

        val experimentId = intent.getStringExtra(EXTRA_EXPERIMENT_ID) ?: ExperimentSession.generateExperimentId()

        // Generate synthetic reference chart bitmap
        val bmp = TestChartGenerator.generateChart(chartType)
        this.chartBitmap = bmp

        // Save reference chart into session folder
        val repository = SessionRepository(this)
        val sourceDir = repository.getSourceDir(experimentId)
        val refFile = File(sourceDir, "reference.png")
        val sha256 = TestChartGenerator.saveChartToFile(bmp, refFile)

        // Create or initialize session
        val displayManager = DisplayDiagnosticManager(this)
        val phoneAInfo = displayManager.queryCurrentDisplayInfo()

        val existingSession = repository.getSession(experimentId)
        if (existingSession == null) {
            val newSession = ExperimentSession(
                experimentId = experimentId,
                testMode = "baseline",
                sourceImage = SourceImageDetails(
                    name = "reference.png",
                    hashSha256 = sha256,
                    width = bmp.width,
                    height = bmp.height,
                    localUri = refFile.absolutePath
                ),
                phoneA = phoneAInfo
            )
            repository.saveSession(newSession)
        }

        // Initialize renderer and SurfaceView
        renderer = NormalBaselineRenderer(experimentId)
        surfaceView = OpticalSurfaceView(this).apply {
            setRenderer(renderer, bmp)
        }

        setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                // Dedicated hardware SurfaceView
                AndroidView(
                    factory = { surfaceView },
                    modifier = Modifier.fillMaxSize()
                )

                // Top overlay banner
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
                            text = "$experimentId • ${chartType.title}",
                            color = LabPrimary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    // Clear End Experiment Control
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
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        chartBitmap?.recycle()
        chartBitmap = null
    }
}
