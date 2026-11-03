package com.securechat.opticalresearch.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.securechat.opticalresearch.assets.TestChartType
import com.securechat.opticalresearch.display.DisplayDiagnosticManager
import com.securechat.opticalresearch.export.ExperimentExporter
import com.securechat.opticalresearch.model.ExperimentSession
import com.securechat.opticalresearch.model.SessionRepository

@Composable
fun ResearchNavHost(
    displayManager: DisplayDiagnosticManager,
    repository: SessionRepository
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val exporter = remember { ExperimentExporter(context) }

    val deviceInfo by displayManager.deviceInfo.collectAsState()
    var experiments by remember { mutableStateOf(repository.listSessions()) }

    fun refreshExperiments() {
        experiments = repository.listSessions()
    }

    NavHost(
        navController = navController,
        startDestination = "dashboard"
    ) {
        composable("dashboard") {
            DashboardScreen(
                deviceInfo = deviceInfo,
                experiments = experiments,
                onNavigateNewExperiment = {
                    navController.navigate("gallery")
                },
                onNavigateCalibration = {
                    navController.navigate("calibration")
                },
                onNavigateGallery = {
                    navController.navigate("gallery")
                },
                onNavigateImport = { expId ->
                    val targetId = expId ?: ExperimentSession.generateExperimentId()
                    navController.navigate("import/$targetId")
                },
                onNavigateResults = { expId ->
                    navController.navigate("results/$expId")
                },
                onExportLatest = {
                    val latest = experiments.firstOrNull()
                    if (latest != null) {
                        try {
                            val res = exporter.exportExperimentPackage(latest, repository)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/zip"
                                putExtra(Intent.EXTRA_STREAM, res.contentUri)
                                putExtra(Intent.EXTRA_SUBJECT, "Veil Optical Experiment: ${latest.experimentId}")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Export Package"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "No experiments logged yet", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        composable("calibration") {
            CalibrationScreen(
                deviceInfo = deviceInfo,
                onRefreshTelemetry = { displayManager.refresh() },
                onBack = { navController.popBackStack() }
            )
        }

        composable("gallery") {
            TestChartGalleryScreen(
                onSelectChart = { chartType ->
                    val expId = ExperimentSession.generateExperimentId()
                    val intent = Intent(context, BaselineDisplayActivity::class.java).apply {
                        putExtra(BaselineDisplayActivity.EXTRA_CHART_TYPE, chartType.name)
                        putExtra(BaselineDisplayActivity.EXTRA_EXPERIMENT_ID, expId)
                    }
                    context.startActivity(intent)
                    refreshExperiments()
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "import/{experimentId}",
            arguments = listOf(navArgument("experimentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val expId = backStackEntry.arguments?.getString("experimentId") ?: ""
            val session = repository.getSession(expId) ?: ExperimentSession(
                experimentId = expId,
                sourceImage = com.securechat.opticalresearch.model.SourceImageDetails(
                    name = "chart_combined_quality.png",
                    hashSha256 = "0".repeat(64),
                    width = 1080,
                    height = 1920
                ),
                phoneA = deviceInfo
            )

            CaptureImportScreen(
                session = session,
                onSaveSession = { updated ->
                    repository.saveSession(updated)
                    refreshExperiments()
                },
                onNavigateResults = { id ->
                    navController.navigate("results/$id") {
                        popUpTo("dashboard")
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "results/{experimentId}",
            arguments = listOf(navArgument("experimentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val expId = backStackEntry.arguments?.getString("experimentId") ?: ""
            val session = repository.getSession(expId)
            if (session != null) {
                ResultsScreen(
                    session = session,
                    onBack = { navController.popBackStack() }
                )
            } else {
                Toast.makeText(context, "Session not found", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            }
        }
    }
}
