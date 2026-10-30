package com.securechat.demo

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.securechat.demo.ui.HomeScreen
import com.securechat.demo.ui.InfoScreen
import com.securechat.demo.ui.NormalViewerScreen
import com.securechat.demo.ui.ScreenshotDetectionScreen
import com.securechat.demo.ui.TestingLabScreen
import com.securechat.demo.ui.theme.SecureChatTheme
import com.securechat.protection.ScreenshotDetectionController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var screenshotDetectionController: ScreenshotDetectionController
    private var detectionCount by mutableIntStateOf(0)
    private var lastDetectionTime by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ScreenshotDetectionController with main-thread callback
        screenshotDetectionController = ScreenshotDetectionController(this) {
            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
            detectionCount++
            lastDetectionTime = timestamp

            Toast.makeText(
                this,
                "📸 Screenshot Detected by Android 14+ ScreenCaptureCallback at $timestamp",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Attach lifecycle observer for automated registration on start / stop
        screenshotDetectionController.attachLifecycle(this)

        setContent {
            SecureChatTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            onNavigateNormalViewer = { navController.navigate("normal_viewer") },
                            onNavigateProtectedViewer = {
                                // Launch dedicated ProtectedViewerActivity with isolated FLAG_SECURE window
                                startActivity(Intent(this@MainActivity, ProtectedViewerActivity::class.java))
                            },
                            onNavigateScreenshotDetection = { navController.navigate("screenshot_detection") },
                            onNavigateTestingLab = { navController.navigate("testing_lab") },
                            onNavigateInfo = { navController.navigate("info") }
                        )
                    }

                    composable("normal_viewer") {
                        NormalViewerScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateProtectedViewer = {
                                startActivity(Intent(this@MainActivity, ProtectedViewerActivity::class.java))
                            }
                        )
                    }

                    composable("screenshot_detection") {
                        ScreenshotDetectionScreen(
                            detectionEventsCount = detectionCount,
                            lastDetectionTimestamp = lastDetectionTime,
                            onResetEvents = {
                                detectionCount = 0
                                lastDetectionTime = null
                            },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable("testing_lab") {
                        TestingLabScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable("info") {
                        InfoScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
