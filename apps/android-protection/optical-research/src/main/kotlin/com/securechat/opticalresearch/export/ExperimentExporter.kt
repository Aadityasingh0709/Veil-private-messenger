package com.securechat.opticalresearch.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.securechat.opticalresearch.model.ExperimentSession
import com.securechat.opticalresearch.model.SessionRepository
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Packages an experiment session into a standardized ZIP archive ready for transfer to the Python pipeline.
 *
 * Archive layout:
 * experiment_<id>.zip
 * ├── manifest.json
 * ├── source/
 * │   └── reference.png
 * ├── captures/
 * │   ├── capture_001.jpg
 * │   └── capture_002.jpg
 * └── notes.txt
 */
class ExperimentExporter(private val context: Context) {

    data class ExportResult(
        val zipFile: File,
        val contentUri: Uri,
        val fileSizeBytes: Long
    )

    fun exportExperimentPackage(
        session: ExperimentSession,
        repository: SessionRepository
    ): ExportResult {
        // Ensure manifest and notes are flushed to disk
        repository.saveSession(session)

        val sessionDir = repository.getSessionDir(session.experimentId)
        val exportsDir = File(context.filesDir, "exports").apply { if (!exists()) mkdirs() }
        val zipFile = File(exportsDir, "${session.experimentId}.zip")

        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            zipDirectoryContents(sessionDir, sessionDir, zos)
        }

        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            zipFile
        )

        return ExportResult(
            zipFile = zipFile,
            contentUri = contentUri,
            fileSizeBytes = zipFile.length()
        )
    }

    private fun zipDirectoryContents(rootDir: File, currentDir: File, zos: ZipOutputStream) {
        val files = currentDir.listFiles() ?: return
        val buffer = ByteArray(8192)

        for (file in files) {
            if (file.isDirectory) {
                zipDirectoryContents(rootDir, file, zos)
            } else {
                val relativePath = file.relativeTo(rootDir).path.replace('\\', '/')
                val entry = ZipEntry(relativePath)
                zos.putNextEntry(entry)

                FileInputStream(file).use { fis ->
                    var count: Int
                    while (fis.read(buffer).also { count = it } != -1) {
                        zos.write(buffer, 0, count)
                    }
                }
                zos.closeEntry()
            }
        }
    }
}
