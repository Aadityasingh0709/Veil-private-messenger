package com.securechat.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.securechat.demo.ui.ProtectedViewerScreen
import com.securechat.demo.ui.theme.SecureChatTheme
import com.securechat.protection.ProtectionSession
import com.securechat.protection.ProtectionStatus
import com.securechat.protection.SecureWindowController

/**
 * Dedicated Activity demonstrating isolated FLAG_SECURE window protection.
 *
 * Activation occurs in [onCreate] prior to composing content to guarantee
 * that the window surface is protected before any visual pixels render.
 */
class ProtectedViewerActivity : ComponentActivity() {

    private lateinit var windowController: SecureWindowController
    private var protectionSession: ProtectionSession? = null
    private var currentStatus by mutableStateOf<ProtectionStatus?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Initialize controller for this Activity's Window
        windowController = SecureWindowController(this)
        windowController.attachLifecycle(this)

        // 2. Acquire protection BEFORE content is visible
        protectionSession = windowController.acquireProtection("protected_viewer_activity")
        currentStatus = windowController.getStatus()

        // 3. Render Compose UI
        setContent {
            SecureChatTheme {
                ProtectedViewerScreen(
                    status = currentStatus ?: windowController.getStatus(),
                    onNavigateBack = { finish() }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // ProtectionSession is safely closed & window state restored
        protectionSession?.release()
        protectionSession = null
    }
}
