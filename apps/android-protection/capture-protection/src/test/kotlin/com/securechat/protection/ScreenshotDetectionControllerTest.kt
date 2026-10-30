package com.securechat.protection

import android.app.Activity
import org.junit.Assert.assertFalse
import org.junit.Test
import org.mockito.Mockito.mock

class ScreenshotDetectionControllerTest {

    @Test
    fun `isRegistered is initially false`() {
        val mockActivity = mock(Activity::class.java)
        val controller = ScreenshotDetectionController(mockActivity) {}
        assertFalse(controller.isRegistered())
    }

    @Test
    fun `unregister is safe when not registered`() {
        val mockActivity = mock(Activity::class.java)
        val controller = ScreenshotDetectionController(mockActivity) {}
        controller.unregister()
        assertFalse(controller.isRegistered())
    }
}
