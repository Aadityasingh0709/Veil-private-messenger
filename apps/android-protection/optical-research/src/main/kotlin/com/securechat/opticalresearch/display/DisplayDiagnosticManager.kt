package com.securechat.opticalresearch.display

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.provider.Settings
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import com.securechat.opticalresearch.model.PhoneADeviceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Diagnostic service that queries and monitors Android Display characteristics in real-time.
 *
 * Implements DisplayListener to detect dynamic refresh rate changes (e.g. adaptive 60Hz/120Hz switching),
 * supported display modes, orientation, and brightness levels.
 *
 * Conforms to: https://developer.android.com/media/optimize/performance/frame-rate
 */
class DisplayDiagnosticManager(private val context: Context) : DisplayManager.DisplayListener {

    private val displayManager: DisplayManager =
        context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

    private val _deviceInfo = MutableStateFlow(queryCurrentDisplayInfo())
    val deviceInfo: StateFlow<PhoneADeviceInfo> = _deviceInfo.asStateFlow()

    private var isListening = false

    fun startListening() {
        if (!isListening) {
            displayManager.registerDisplayListener(this, null)
            isListening = true
            refresh()
        }
    }

    fun stopListening() {
        if (isListening) {
            displayManager.unregisterDisplayListener(this)
            isListening = false
        }
    }

    fun refresh() {
        _deviceInfo.value = queryCurrentDisplayInfo()
    }

    override fun onDisplayAdded(displayId: Int) {
        if (displayId == Display.DEFAULT_DISPLAY) refresh()
    }

    override fun onDisplayRemoved(displayId: Int) {
        if (displayId == Display.DEFAULT_DISPLAY) refresh()
    }

    override fun onDisplayChanged(displayId: Int) {
        if (displayId == Display.DEFAULT_DISPLAY) refresh()
    }

    fun queryCurrentDisplayInfo(): PhoneADeviceInfo {
        val defaultDisplay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                context.display ?: displayManager.getDisplay(Display.DEFAULT_DISPLAY)
            } catch (e: Exception) {
                displayManager.getDisplay(Display.DEFAULT_DISPLAY)
            }
        } else {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay
                ?: displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        }

        val mode = defaultDisplay?.mode
        val width = mode?.physicalWidth ?: defaultDisplay?.width ?: 1080
        val height = mode?.physicalHeight ?: defaultDisplay?.height ?: 1920
        val refreshRate = defaultDisplay?.refreshRate ?: 60.0f

        val supportedRefreshRates = defaultDisplay?.supportedModes
            ?.map { it.refreshRate }
            ?.distinct()
            ?.sorted()
            ?: listOf(refreshRate)

        val orientation = when (defaultDisplay?.rotation) {
            Surface.ROTATION_0 -> "PORTRAIT (0°)"
            Surface.ROTATION_90 -> "LANDSCAPE (90°)"
            Surface.ROTATION_180 -> "REVERSE PORTRAIT (180°)"
            Surface.ROTATION_270 -> "REVERSE LANDSCAPE (270°)"
            else -> "UNKNOWN"
        }

        val brightnessPercent = try {
            val raw = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS
            )
            (raw / 255.0f) * 100.0f
        } catch (e: Exception) {
            null
        }

        val adaptiveBrightness = try {
            val modeSetting = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE
            )
            modeSetting == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        } catch (e: Exception) {
            null
        }

        return PhoneADeviceInfo(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            displayResolution = "${width}x${height}",
            refreshRateHz = refreshRate,
            supportedRefreshRates = supportedRefreshRates,
            screenBrightnessPercent = brightnessPercent,
            adaptiveBrightness = adaptiveBrightness,
            screenOrientation = orientation
        )
    }
}
