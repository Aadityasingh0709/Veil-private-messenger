package com.securechat.opticalresearch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.securechat.opticalresearch.display.DisplayDiagnosticManager
import com.securechat.opticalresearch.model.SessionRepository
import com.securechat.opticalresearch.ui.ResearchNavHost
import com.securechat.opticalresearch.ui.theme.VeilOpticalResearchTheme

class MainActivity : ComponentActivity() {

    private lateinit var displayManager: DisplayDiagnosticManager
    private lateinit var repository: SessionRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        displayManager = DisplayDiagnosticManager(this)
        repository = SessionRepository(this)

        setContent {
            VeilOpticalResearchTheme {
                ResearchNavHost(
                    displayManager = displayManager,
                    repository = repository
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        displayManager.startListening()
    }

    override fun onPause() {
        super.onPause()
        displayManager.stopListening()
    }
}
