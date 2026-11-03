package com.securechat.opticalresearch.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class SourceImageDetails(
    val name: String,
    val hashSha256: String,
    val width: Int,
    val height: Int,
    val localUri: String? = null
)

/**
 * Core domain entity representing an active or completed research session in Phase 4A.
 */
data class ExperimentSession(
    val experimentId: String = generateExperimentId(),
    val timestamp: String = getCurrentIsoTimestamp(),
    val testMode: String = "baseline", // strictly "baseline" in Phase 4A
    val sourceImage: SourceImageDetails,
    val phoneA: PhoneADeviceInfo,
    val phoneB: PhoneBCaptureInfo = PhoneBCaptureInfo(),
    val captures: List<CaptureItem> = emptyList(),
    val notes: String = ""
) {
    companion object {
        fun generateExperimentId(): String {
            val dateStr = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
            val suffix = UUID.randomUUID().toString().substring(0, 6).uppercase(Locale.US)
            return "EXP-$dateStr-$suffix"
        }

        fun getCurrentIsoTimestamp(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            return sdf.format(Date())
        }
    }
}
