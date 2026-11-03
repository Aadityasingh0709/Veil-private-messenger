package com.securechat.opticalresearch.model

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Local file-based repository for persisting ExperimentSession records in app internal storage.
 * Completely offline; does not require or communicate with any external backend.
 */
class SessionRepository(private val context: Context) {

    private val experimentsDir: File
        get() = File(context.filesDir, "experiments").apply { if (!exists()) mkdirs() }

    fun getSessionDir(experimentId: String): File {
        return File(experimentsDir, experimentId).apply { if (!exists()) mkdirs() }
    }

    fun getSourceDir(experimentId: String): File {
        return File(getSessionDir(experimentId), "source").apply { if (!exists()) mkdirs() }
    }

    fun getCapturesDir(experimentId: String): File {
        return File(getSessionDir(experimentId), "captures").apply { if (!exists()) mkdirs() }
    }

    fun saveSession(session: ExperimentSession) {
        val sessionDir = getSessionDir(session.experimentId)
        val manifestFile = File(sessionDir, "manifest.json")
        val manifest = ExperimentManifest.fromSession(session)
        manifestFile.writeText(manifest.toJson(), Charsets.UTF_8)

        // Write human-readable notes.txt
        val notesFile = File(sessionDir, "notes.txt")
        val notesContent = buildString {
            appendLine("VEIL OPTICAL RESEARCH LAB — EXPERIMENT NOTES")
            appendLine("ID: ${session.experimentId}")
            appendLine("Date: ${session.timestamp}")
            appendLine("Mode: ${session.testMode}")
            appendLine("Source Asset: ${session.sourceImage.name}")
            appendLine("Phone A: ${session.phoneA.manufacturer} ${session.phoneA.model} (${session.phoneA.displayResolution}, ${session.phoneA.refreshRateHz}Hz)")
            appendLine("Phone B: ${session.phoneB.manufacturer} ${session.phoneB.model} (${session.phoneB.distanceCm}cm, angle ${session.phoneB.cameraAngleDegrees}°)")
            appendLine("Captures: ${session.captures.size}")
            appendLine("---")
            appendLine(session.notes.ifEmpty { "(No qualitative notes recorded)" })
        }
        notesFile.writeText(notesContent, Charsets.UTF_8)
    }

    fun getSession(experimentId: String): ExperimentSession? {
        val sessionDir = File(experimentsDir, experimentId)
        val manifestFile = File(sessionDir, "manifest.json")
        if (!manifestFile.exists()) return null

        return try {
            val manifest = ExperimentManifest.fromJson(manifestFile.readText(Charsets.UTF_8))
            val captures = manifest.captures.map { c ->
                val capFile = File(getCapturesDir(experimentId), c.filename)
                CaptureItem(
                    id = c.filename,
                    filename = c.filename,
                    absolutePath = capFile.absolutePath,
                    hashSha256 = c.hashSha256,
                    repetitionNumber = c.repetitionNumber,
                    timestamp = c.timestamp,
                    exif = c.exif,
                    fileSizeBytes = if (capFile.exists()) capFile.length() else 0L
                )
            }

            ExperimentSession(
                experimentId = manifest.experimentId,
                timestamp = manifest.timestamp,
                testMode = manifest.testMode,
                sourceImage = SourceImageDetails(
                    name = manifest.sourceImage.name,
                    hashSha256 = manifest.sourceImage.hashSha256,
                    width = manifest.sourceImage.width,
                    height = manifest.sourceImage.height
                ),
                phoneA = manifest.phoneA,
                phoneB = manifest.phoneB,
                captures = captures,
                notes = manifest.notes
            )
        } catch (e: Exception) {
            null
        }
    }

    fun listSessions(): List<ExperimentSession> {
        val dirs = experimentsDir.listFiles { f -> f.isDirectory } ?: return emptyList()
        return dirs.mapNotNull { d -> getSession(d.name) }
            .sortedByDescending { it.timestamp }
    }

    fun deleteSession(experimentId: String): Boolean {
        val sessionDir = File(experimentsDir, experimentId)
        return if (sessionDir.exists()) sessionDir.deleteRecursively() else false
    }

    companion object {
        fun calculateSha256(file: File): String {
            if (!file.exists()) return ""
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
}
