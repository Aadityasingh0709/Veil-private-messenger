package com.securechat.opticalresearch.model

/**
 * Immutable record of Phone A (Display device) hardware, OS, and display properties.
 * Information is obtained dynamically via Android Display APIs or marked as unknown/manual.
 */
data class PhoneADeviceInfo(
    val manufacturer: String? = null,
    val model: String? = null,
    val androidVersion: String? = null,
    val apiLevel: Int? = null,
    val displayResolution: String? = null,
    val refreshRateHz: Float? = null,
    val supportedRefreshRates: List<Float>? = null,
    val screenBrightnessPercent: Float? = null,
    val adaptiveBrightness: Boolean? = null,
    val screenOrientation: String? = null
)
