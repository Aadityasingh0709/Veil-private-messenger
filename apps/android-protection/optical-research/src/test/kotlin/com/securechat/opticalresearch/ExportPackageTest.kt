package com.securechat.opticalresearch

import com.securechat.opticalresearch.model.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class ExportPackageTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testZipArchiveStructure() {
        val expId = "EXP-20261103-120000-TESTZIP"
        val rootDir = tempFolder.newFolder(expId)
        val sourceDir = File(rootDir, "source").apply { mkdirs() }
        val capturesDir = File(rootDir, "captures").apply { mkdirs() }

        // Create sample manifest.json
        val manifest = ExperimentManifest(
            experimentId = expId,
            timestamp = "2026-11-03T12:00:00Z",
            testMode = "baseline",
            sourceImage = SourceImageManifest("reference.png", "0".repeat(64), 1080, 1920),
            phoneA = PhoneADeviceInfo(manufacturer = "Xiaomi", model = "2312DRAABI"),
            phoneB = PhoneBCaptureInfo(model = "Pixel 8"),
            captures = listOf(
                CaptureManifestItem("capture_001.jpg", "1".repeat(64), 1, "2026-11-03T12:01:00Z", null)
            ),
            notes = "Zip packaging test"
        )
        File(rootDir, "manifest.json").writeText(manifest.toJson())
        File(rootDir, "notes.txt").writeText("Notes for $expId")
        File(sourceDir, "reference.png").writeBytes(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47))
        File(capturesDir, "capture_001.jpg").writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()))

        // Package into ZIP
        val zipFile = File(tempFolder.root, "$expId.zip")
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            zipDirectory(rootDir, rootDir, zos)
        }

        assertTrue("Generated ZIP archive must exist", zipFile.exists())
        assertTrue("ZIP archive must not be empty", zipFile.length() > 0)

        // Verify ZIP contents
        val entries = mutableListOf<String>()
        ZipFile(zipFile).use { zf ->
            val it = zf.entries()
            while (it.hasMoreElements()) {
                entries.add(it.nextElement().name)
            }
        }

        assertTrue("Must contain manifest.json", entries.any { it.endsWith("manifest.json") })
        assertTrue("Must contain notes.txt", entries.any { it.endsWith("notes.txt") })
        assertTrue("Must contain reference.png in source/", entries.any { it.contains("source") && it.endsWith("reference.png") })
        assertTrue("Must contain capture_001.jpg in captures/", entries.any { it.contains("captures") && it.endsWith("capture_001.jpg") })
    }

    private fun zipDirectory(rootDir: File, currentDir: File, zos: ZipOutputStream) {
        val files = currentDir.listFiles() ?: return
        val buffer = ByteArray(4096)
        for (file in files) {
            if (file.isDirectory) {
                zipDirectory(rootDir, file, zos)
            } else {
                val relPath = file.relativeTo(rootDir).path.replace('\\', '/')
                zos.putNextEntry(ZipEntry(relPath))
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
