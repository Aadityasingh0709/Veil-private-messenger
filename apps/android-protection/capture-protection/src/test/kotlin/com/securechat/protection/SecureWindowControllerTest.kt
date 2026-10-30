package com.securechat.protection

import android.view.Window
import android.view.WindowManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class SecureWindowControllerTest {

    private lateinit var mockWindow: Window
    private lateinit var layoutParams: WindowManager.LayoutParams

    @Before
    fun setUp() {
        mockWindow = mock(Window::class.java)
        layoutParams = WindowManager.LayoutParams()
        `when`(mockWindow.attributes).thenReturn(layoutParams)
    }

    @Test
    fun `acquireProtection adds FLAG_SECURE and creates active session`() {
        val controller = SecureWindowController(mockWindow)

        val session = controller.acquireProtection("test_tag")

        assertTrue(session.isActive)
        assertEquals("test_tag", session.tag)
        assertEquals(1, controller.getStatus().activeSessionsCount)
        verify(mockWindow).addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    @Test
    fun `releasing session clears FLAG_SECURE when no other sessions exist`() {
        val controller = SecureWindowController(mockWindow)

        val session = controller.acquireProtection("media")
        session.release()

        assertFalse(session.isActive)
        assertEquals(0, controller.getStatus().activeSessionsCount)
        verify(mockWindow).clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    @Test
    fun `multiple overlapping sessions keep protection active until all are released`() {
        val controller = SecureWindowController(mockWindow)

        val session1 = controller.acquireProtection("session1")
        val session2 = controller.acquireProtection("session2")

        assertEquals(2, controller.getStatus().activeSessionsCount)

        // Release first session
        session1.release()
        assertFalse(session1.isActive)
        assertTrue(session2.isActive)
        assertEquals(1, controller.getStatus().activeSessionsCount)

        // Clear flag should NOT have been called yet because session2 is active
        verify(mockWindow, times(0)).clearFlags(WindowManager.LayoutParams.FLAG_SECURE)

        // Release second session
        session2.release()
        assertFalse(session2.isActive)
        assertEquals(0, controller.getStatus().activeSessionsCount)
        verify(mockWindow, times(1)).clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    @Test
    fun `pre-existing FLAG_SECURE is preserved after all sessions release`() {
        // Window already had FLAG_SECURE set prior to SDK initialization
        layoutParams.flags = WindowManager.LayoutParams.FLAG_SECURE

        val controller = SecureWindowController(mockWindow)
        assertTrue(controller.getStatus().wasPreExistingSecure)

        val session = controller.acquireProtection("banner")
        session.release()

        // Because pre-existing flags must be preserved, clearFlags should NOT be called
        verify(mockWindow, times(0)).clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    @Test
    fun `idempotent release calls do not decrement active count multiple times`() {
        val controller = SecureWindowController(mockWindow)

        val session = controller.acquireProtection("session")
        assertEquals(1, controller.getStatus().activeSessionsCount)

        session.release()
        session.release()
        session.close() // AutoCloseable close()

        assertEquals(0, controller.getStatus().activeSessionsCount)
    }

    @Test
    fun `releaseAll clears all active sessions and restores baseline`() {
        val controller = SecureWindowController(mockWindow)

        controller.acquireProtection("s1")
        controller.acquireProtection("s2")
        controller.acquireProtection("s3")
        assertEquals(3, controller.getStatus().activeSessionsCount)

        controller.releaseAll()
        assertEquals(0, controller.getStatus().activeSessionsCount)
        verify(mockWindow, times(1)).clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    @Test
    fun `use block releases session automatically`() {
        val controller = SecureWindowController(mockWindow)

        controller.acquireProtection("scoped").use { session ->
            assertTrue(session.isActive)
            assertEquals(1, controller.getStatus().activeSessionsCount)
        }

        assertEquals(0, controller.getStatus().activeSessionsCount)
        verify(mockWindow).clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    @Test
    fun `getInstance returns canonical controller instance for same window`() {
        val controller1 = SecureWindowController.getInstance(mockWindow)
        val controller2 = SecureWindowController.getInstance(mockWindow)

        assertEquals(controller1, controller2)
    }

    @Test
    fun `detectionController updates getStatus isScreenshotDetectionRegistered accurately`() {
        val controller = SecureWindowController(mockWindow)
        assertFalse(controller.getStatus().isScreenshotDetectionRegistered)

        val mockDetectionController = mock(ScreenshotDetectionController::class.java)
        `when`(mockDetectionController.isRegistered()).thenReturn(true)

        controller.detectionController = mockDetectionController
        assertTrue(controller.getStatus().isScreenshotDetectionRegistered)
        assertTrue(controller.getStatus().platformNotes.contains("screenshot detection callback"))
    }

    @Test
    fun `autoRestoreOnRelease false leaves flags unchanged upon release`() {
        val config = ProtectionConfig(autoRestoreOnRelease = false)
        val controller = SecureWindowController(mockWindow, config)

        val session = controller.acquireProtection("custom_config")
        verify(mockWindow).addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        session.release()
        // clearFlags should not be called because autoRestoreOnRelease is false
        verify(mockWindow, times(0)).clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    @Test
    fun `preservePreExistingFlags false clears FLAG_SECURE even if pre-existing`() {
        layoutParams.flags = WindowManager.LayoutParams.FLAG_SECURE
        val config = ProtectionConfig(preservePreExistingFlags = false)
        val controller = SecureWindowController(mockWindow, config)

        val session = controller.acquireProtection("banner")
        session.release()

        // Because preservePreExistingFlags is false, clearFlags is executed
        verify(mockWindow, times(1)).clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    @Test
    fun `status callback receives updates on acquire and release`() {
        val controller = SecureWindowController(mockWindow)
        var receivedCount = 0
        controller.setOnStatusChangedListener {
            receivedCount++
        }

        val session = controller.acquireProtection("notified")
        assertEquals(1, receivedCount)

        session.release()
        assertEquals(2, receivedCount)
    }
}
