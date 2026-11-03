package com.securechat.opticalresearch.importer

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.securechat.opticalresearch.model.CaptureItem
import com.securechat.opticalresearch.model.ExifMetadata
import com.securechat.opticalresearch.model.SessionRepository
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Validates and imports camera captures taken by Phone B via system file picker.
 *
 * CRITICAL REQUIREMENTS:
 * - Validates file signature (Magic bytes for JPEG, PNG, WebP).
 * - Copies raw bytes without re-encoding, rescaling, or compression.
 * - Extracts EXIF telemetry if present without failing if missing.
 * - Associates with experiment session.
 */
class CaptureImporter(private val context: Context) {

    sealed class ImportResult {
        data class Success(val captureItem: CaptureItem) : ImportResult()
        data class Error(val reason: String) : ImportResult()
    }

    fun importCapture(
        uri: Uri,
        experimentId: String,
        repetitionNumber: Int,
        repository: SessionRepository
    ): ImportResult {
        val resolver = context.contentResolver

        // 1. Validate magic bytes
        val magicBytes = ByteArray(12)
        try {
            resolver.openInputStream(uri)?.use { stream ->
                stream.read(magicBytes)
            } ?: return ImportResult.Error("Cannot open input stream for URI: $uri")
        } catch (e: Exception) {
            return ImportResult.Error("Failed to read header bytes: ${e.message}")
        }

        val extension = detectExtensionFromMagicBytes(magicBytes)
            ?: return ImportResult.Error("Unsupported image format. Only JPEG, PNG, and WebP camera captures are supported.")

        // 2. Prepare target capture file
        val capturesDir = repository.getCapturesDir(experimentId)
        val filename = "capture_%03d.%s".format(repetitionNumber, extension)
        val targetFile = File(capturesDir, filename)

        // 3. Byte-for-byte copy (Zero alteration, zero recompression)
        val digest = MessageDigest.getInstance("SHA-256")
        try {
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        digest.update(buffer, 0, read)
                        output.write(buffer, 0, read)
                    }
                }
            } ?: return ImportResult.Error("Failed to copy capture data")
        } catch (e: Exception) {
            return ImportResult.Error("I/O error during capture copy: ${e.message}")
        }

        val sha256 = digest.digest().joinToString("") { "%02x".format(it) }

        // 4. Extract EXIF metadata if present
        val exif = extractExif(targetFile)

        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.format(Date())

        val item = CaptureItem(
            id = filename,
            filename = filename,
            absolutePath = targetFile.absolutePath,
            hashSha256 = sha256,
            repetitionNumber = repetitionNumber,
            timestamp = timestamp,
            exif = exif,
            fileSizeBytes = targetFile.length()
        )

        return ImportResult.Success(item)
    }

    private fun detectExtensionFromMagicBytes(header: ByteArray): String? {
        if (header.size < 4) return null

        // JPEG: FF D8 FF
        if (header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte() && header[2] == 0xFF.toByte()) {
            return "jpg"
        }

        // PNG: 89 50 4E 47
        if (header[0] == 0x89.toByte() && header[1] == 0x50.toByte() && header[2] == 0x4E.toByte() && header[3] == 0x47.toByte()) {
            return "png"
        }

        // WebP: RIFF ... WEBP
        if (header.size >= 12 &&
            header[0] == 0x52.toByte() && header[1] == 0x49.toByte() && header[2] == 0x46.toByte() && header[3] == 0x46.toByte() &&
            header[8] == 0x57.toByte() && header[9] == 0x45.toByte() && header[10] == 0x42.toByte() && header[11] == 0x50.toByte()
        ) {
            return "webp"
        }

        return null
    }

    private fun extractExif(file: File): ExifMetadata? {
        return try {
            val ex = ExifInterface(file.absolutePath)
            ExifMetadata(
                make = ex.getAttribute(ExifInterface.TAG_MAKE),
                model = ex.getAttribute(ExifInterface.TAG_MODEL),
                focalLength = ex.getAttribute(ExifInterface.TAG_FOCAL_LENGTH),
                fNumber = ex.getAttribute(ExifInterface.TAG_F_NUMBER),
                exposureTime = ex.getAttribute(ExifInterface.TAG_EXPOSURE_TIME),
                iso = ex.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
                    ?: ex.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)
            )
        } catch (e: Exception) {
            null
        }
    }
}
