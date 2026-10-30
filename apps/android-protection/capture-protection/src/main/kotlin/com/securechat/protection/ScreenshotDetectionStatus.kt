package com.securechat.protection

import android.os.Build

/**
 * Status snapshot of the native screenshot detection callback (Android 14+ / API 34+).
 *
 * @property isSupported True if Android API level >= 34 (Build.VERSION_CODES.UPSIDE_DOWN_CAKE).
 * @property isRegistered True if a callback is currently registered with the Activity.
 * @property androidApiLevel Current device runtime API level (Build.VERSION.SDK_INT).
 * @property notes Explanatory platform notes describing capability and constraints.
 */
data class ScreenshotDetectionStatus(
    val isSupported: Boolean,
    val isRegistered: Boolean,
    val androidApiLevel: Int = Build.VERSION.SDK_INT,
    val notes: String = ""
)
