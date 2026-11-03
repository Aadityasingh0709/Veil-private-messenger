package com.securechat.opticalresearch

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class CaptureImportValidationTest {

    private fun detectExtensionFromMagicBytes(header: ByteArray): String? {
        if (header.size < 4) return null
        if (header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte() && header[2] == 0xFF.toByte()) {
            return "jpg"
        }
        if (header[0] == 0x89.toByte() && header[1] == 0x50.toByte() && header[2] == 0x4E.toByte() && header[3] == 0x47.toByte()) {
            return "png"
        }
        if (header.size >= 12 &&
            header[0] == 0x52.toByte() && header[1] == 0x49.toByte() && header[2] == 0x46.toByte() && header[3] == 0x46.toByte() &&
            header[8] == 0x57.toByte() && header[9] == 0x45.toByte() && header[10] == 0x42.toByte() && header[11] == 0x50.toByte()
        ) {
            return "webp"
        }
        return null
    }

    @Test
    fun testValidJpegSignature() {
        val jpegHeader = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
        assertEquals("jpg", detectExtensionFromMagicBytes(jpegHeader))
    }

    @Test
    fun testValidPngSignature() {
        val pngHeader = byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(), 0x0D.toByte(), 0x0A.toByte())
        assertEquals("png", detectExtensionFromMagicBytes(pngHeader))
    }

    @Test
    fun testValidWebpSignature() {
        val webpHeader = byteArrayOf(
            'R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte(),
            0, 0, 0, 0,
            'W'.code.toByte(), 'E'.code.toByte(), 'B'.code.toByte(), 'P'.code.toByte()
        )
        assertEquals("webp", detectExtensionFromMagicBytes(webpHeader))
    }

    @Test
    fun testRejectUnsupportedHeader() {
        val pdfHeader = byteArrayOf('%'.code.toByte(), 'P'.code.toByte(), 'D'.code.toByte(), 'F'.code.toByte())
        assertNull(detectExtensionFromMagicBytes(pdfHeader))

        val randomBytes = byteArrayOf(0x00, 0x01, 0x02, 0x03)
        assertNull(detectExtensionFromMagicBytes(randomBytes))
    }

    @Test
    fun testSha256Checksum() {
        val data = "VEIL OPTICAL RESEARCH LAB".toByteArray(Charsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(data).joinToString("") { "%02x".format(it) }
        assertEquals(64, hash.length)
        assertTrue(hash.all { it in '0'..'9' || it in 'a'..'f' })
    }
}
