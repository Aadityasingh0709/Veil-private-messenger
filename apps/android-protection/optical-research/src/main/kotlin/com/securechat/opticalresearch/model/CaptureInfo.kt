package com.securechat.opticalresearch.model

/**
 * Optical acquisition metadata recorded for Phone B (the photographing camera).
 * Unknown or unavailable parameters remain null or "unknown" without fabrication.
 */
data class PhoneBCaptureInfo(
    val manufacturer: String? = null,
    val model: String? = null,
    val cameraResolution: String? = null,
    val exposureTimeSeconds: String? = null,
    val iso: Int? = null,
    val focusMode: String? = null,
    val whiteBalance: String? = null,
    val distanceCm: Float? = null,
    val cameraAngleDegrees: Float? = null,
    val ambientLighting: String? = null,
    val captureMode: String = "auto" // "auto", "manual", or "unknown"
)

data class ExifMetadata(
    val make: String? = null,
    val model: String? = null,
    val focalLength: String? = null,
    val fNumber: String? = null,
    val exposureTime: String? = null,
    val iso: String? = null
)

data class CaptureItem(
    val id: String,
    val filename: String,
    val absolutePath: String,
    val hashSha256: String? = null,
    val repetitionNumber: Int = 1,
    val timestamp: String? = null,
    val exif: ExifMetadata? = null,
    val fileSizeBytes: Long = 0L
)
