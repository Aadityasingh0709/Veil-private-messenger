package com.securechat.protection

import android.os.Build

/**
 * Immutable snapshot describing the current capture protection configuration on the Window.
 *
 * NOTE: This status reflects the software flags configured by the SDK. It does not
 * guarantee immunity against physical cameras or all hardware/firmware capture mechanisms.
 *
 * @property isWindowSecureActive True if WindowManager.LayoutParams.FLAG_SECURE is currently active on the window.
 * @property activeSessionsCount The number of active reference-counted protection sessions.
 * @property wasPreExistingSecure True if FLAG_SECURE was already enabled on the window before the SDK was attached.
 * @property isScreenshotDetectionSupported True if the OS supports Activity.registerScreenCaptureCallback (Android 14 / API 34+).
 * @property isScreenshotDetectionRegistered True if a screenshot detection callback is currently registered.
 * @property androidApiLevel The device runtime Android API level (Build.VERSION.SDK_INT).
 * @property platformNotes Contextual notes regarding OS limitations or behavior.
 */
data class ProtectionStatus(
    val isWindowSecureActive: Boolean,
    val activeSessionsCount: Int,
    val wasPreExistingSecure: Boolean,
    val isScreenshotDetectionSupported: Boolean,
    val isScreenshotDetectionRegistered: Boolean,
    val androidApiLevel: Int = Build.VERSION.SDK_INT,
    val platformNotes: String = ""
)
