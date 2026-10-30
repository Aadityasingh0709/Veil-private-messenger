package com.securechat.protection

import android.app.Activity
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages the screenshot detection callback introduced in Android 14 (API 34).
 *
 * ### Platform Context & Key Distinction
 * 1. **Detection is NOT Prevention**:
 *    This API notifies the application that a screenshot was taken. It **cannot** prevent or cancel
 *    the capture.
 * 2. **Suppressed under FLAG_SECURE**:
 *    When [android.view.WindowManager.LayoutParams.FLAG_SECURE] is active on an Activity window,
 *    Android blocks screenshot generation entirely, and consequently the OS **does not** trigger
 *    the [Activity.ScreenCaptureCallback] for that window.
 * 3. **Non-Detection of ADB / Instrumentation**:
 *    Android 14's documented detection mechanism does not detect screenshots captured via ADB (`screencap`)
 *    or instrumentation tests. Manual physical hardware-button testing is required to verify the callback.
 * 4. **Required Manifest Permission**:
 *    The consuming application must have `<uses-permission android:name="android.permission.DETECT_SCREEN_CAPTURE" />`.
 */
class ScreenshotDetectionController(
    activity: Activity,
    private val onScreenshotCaptured: () -> Unit
) {
    private val activityRef: WeakReference<Activity> = WeakReference(activity)
    private val isRegistered = AtomicBoolean(false)

    // Store the callback handle as Any? to safely prevent class verification issues on API < 34
    private var screenCaptureCallback: Any? = null

    companion object {
        /**
         * Returns true if the device runtime environment supports the native Screenshot Detection API.
         */
        fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE // API 34
    }

    /**
     * Registers the screen capture callback if running on Android 14 (API 34) or above.
     *
     * Safe to call on older versions: returns false with no exception.
     * Idempotent: duplicate calls while already registered are ignored.
     *
     * @return True if successfully registered, false if unsupported or already registered.
     */
    fun register(): Boolean {
        if (!isSupported()) return false
        if (!isRegistered.compareAndSet(false, true)) return false

        val activity = activityRef.get() ?: run {
            isRegistered.set(false)
            return false
        }

        try {
            registerApi34(activity)
            return true
        } catch (e: Throwable) {
            isRegistered.set(false)
            return false
        }
    }

    /**
     * Unregisters the screen capture callback.
     *
     * Safe to call multiple times or when not registered.
     */
    fun unregister() {
        if (!isSupported()) return
        if (!isRegistered.compareAndSet(true, false)) return

        val activity = activityRef.get() ?: return
        val callback = screenCaptureCallback ?: return

        try {
            unregisterApi34(activity, callback)
        } catch (_: Throwable) {
            // Ignored on cleanup
        } finally {
            screenCaptureCallback = null
        }
    }

    /**
     * Checks if the callback is currently active and registered with the OS.
     */
    fun isRegistered(): Boolean = isRegistered.get()

    /**
     * Returns an immutable status snapshot of the screenshot detection subsystem.
     */
    fun getStatus(): ScreenshotDetectionStatus {
        val supported = isSupported()
        val registered = isRegistered.get()
        val notes = when {
            !supported -> "Screenshot detection requires Android 14 (API 34) or higher. Current device API level is ${Build.VERSION.SDK_INT}."
            registered -> "Activity.ScreenCaptureCallback registered. Supported hardware-button screenshots will trigger notifications. Note: Android suppresses this callback if FLAG_SECURE is active or during ADB screencap."
            else -> "Screenshot detection supported (API >= 34) but callback is not currently registered."
        }
        return ScreenshotDetectionStatus(
            isSupported = supported,
            isRegistered = registered,
            androidApiLevel = Build.VERSION.SDK_INT,
            notes = notes
        )
    }

    /**
     * Binds registration to the Activity lifecycle:
     * - Registers on [Lifecycle.Event.ON_START]
     * - Unregisters on [Lifecycle.Event.ON_STOP]
     */
    fun attachLifecycle(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                register()
            }

            override fun onStop(owner: LifecycleOwner) {
                unregister()
            }

            override fun onDestroy(owner: LifecycleOwner) {
                unregister()
                owner.lifecycle.removeObserver(this)
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun registerApi34(activity: Activity) {
        val callback = Activity.ScreenCaptureCallback {
            onScreenshotCaptured.invoke()
        }
        val executor = ContextCompat.getMainExecutor(activity)
        activity.registerScreenCaptureCallback(executor, callback)
        screenCaptureCallback = callback
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun unregisterApi34(activity: Activity, callback: Any) {
        if (callback is Activity.ScreenCaptureCallback) {
            activity.unregisterScreenCaptureCallback(callback)
        }
    }
}
