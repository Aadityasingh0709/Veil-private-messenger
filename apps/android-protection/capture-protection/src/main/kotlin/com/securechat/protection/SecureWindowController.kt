package com.securechat.protection

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.Window
import android.view.WindowManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Controller responsible for managing [WindowManager.LayoutParams.FLAG_SECURE] on an Android [Window].
 *
 * ### Architectural Scope
 * `FLAG_SECURE` is an OS-level window control enforced by SurfaceFlinger and the Window Manager.
 * It instructs the system to:
 * 1. Black out or reject hardware-button and system screenshots for the window.
 * 2. Censor the window's content in the system recent-apps (task switcher) preview.
 * 3. Black out or disable rendering on non-secure external displays (e.g. Miracast, HDMI without HDCP).
 *
 * **CRITICAL DISTINCTION**:
 * `FLAG_SECURE` applies to the entire [Window]. It is not a per-element or per-image canvas filter.
 * When enabled, all content displayed within that Activity's window surface is protected.
 *
 * ### Features
 * - **Reference Counting**: Allows multiple components (e.g. media viewer and sensitive banner)
 *   to hold independent [ProtectionSession] handles. Protection remains active until all sessions release.
 * - **Pre-existing State Preservation**: If `FLAG_SECURE` was already present before the SDK
 *   was attached, the controller will NOT clear the flag when SDK sessions are released.
 * - **Thread Safety**: Window layout parameter mutations are automatically dispatched to the
 *   main thread if called from a background thread.
 * - **Lifecycle Aware**: Can be attached to a [LifecycleOwner] to guarantee automated release on destroy.
 * - **Weak Referencing**: Avoids Activity / Window memory leaks.
 */
class SecureWindowController(
    window: Window,
    private val config: ProtectionConfig = ProtectionConfig()
) {

    /**
     * Secondary constructor for convenience with an [Activity].
     */
    constructor(activity: Activity, config: ProtectionConfig = ProtectionConfig()) : this(activity.window, config)

    private val windowRef: WeakReference<Window> = WeakReference(window)
    private val mainHandler = Handler(Looper.getMainLooper())

    // Tracks whether FLAG_SECURE was already enabled before the SDK performed any modification.
    private val wasPreExistingSecure: Boolean = isFlagSecurePresent(window)

    // Set of currently active session IDs to tags
    private val activeSessions = ConcurrentHashMap<String, String>()

    // Status listener callback
    private var onStatusChangedListener: ((ProtectionStatus) -> Unit)? = null

    /**
     * Acquires a new reference-counted protection session on this window.
     *
     * Enables [WindowManager.LayoutParams.FLAG_SECURE] on the window if it is not already active.
     *
     * @param tag Optional descriptive name of the calling component (e.g. "image_viewer", "chat_bubble").
     * @return An active [ProtectionSession] handle.
     */
    fun acquireProtection(tag: String = "default"): ProtectionSession {
        val sessionId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        activeSessions[sessionId] = tag
        applySecureFlagToWindow(secure = true)
        notifyStatusChanged()

        return object : ProtectionSession {
            override val sessionId: String = sessionId
            override val tag: String = tag
            override val acquiredAtMillis: Long = now
            private val released = AtomicBoolean(false)

            override val isActive: Boolean
                get() = !released.get() && activeSessions.containsKey(sessionId)

            override fun release() {
                if (released.compareAndSet(false, true)) {
                    releaseInternal(sessionId)
                }
            }
        }
    }

    /**
     * Releases a specific protection session.
     */
    fun releaseProtection(session: ProtectionSession) {
        session.release()
    }

    /**
     * Releases all currently held protection sessions and restores baseline window flags.
     */
    fun releaseAll() {
        if (activeSessions.isEmpty()) return
        activeSessions.clear()
        restoreBaselineState()
        notifyStatusChanged()
    }

    /**
     * Checks if FLAG_SECURE is currently active on the managed window.
     */
    fun isProtectionActive(): Boolean {
        val window = windowRef.get() ?: return false
        return isFlagSecurePresent(window)
    }

    /**
     * Returns an immutable snapshot of current protection status.
     */
    fun getStatus(): ProtectionStatus {
        val window = windowRef.get()
        val isSecure = window?.let { isFlagSecurePresent(it) } ?: false
        return ProtectionStatus(
            isWindowSecureActive = isSecure,
            activeSessionsCount = activeSessions.size,
            wasPreExistingSecure = wasPreExistingSecure,
            isScreenshotDetectionSupported = ScreenshotDetectionController.isSupported(),
            isScreenshotDetectionRegistered = false,
            platformNotes = getPlatformNotes(isSecure)
        )
    }

    /**
     * Attaches this controller to a [LifecycleOwner] to automatically release sessions
     * when the Activity is destroyed.
     */
    fun attachLifecycle(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                releaseAll()
                owner.lifecycle.removeObserver(this)
            }
        })
    }

    /**
     * Sets a listener to be notified whenever protection status changes.
     */
    fun setOnStatusChangedListener(listener: ((ProtectionStatus) -> Unit)?) {
        this.onStatusChangedListener = listener
    }

    private fun releaseInternal(sessionId: String) {
        activeSessions.remove(sessionId)
        if (activeSessions.isEmpty()) {
            restoreBaselineState()
        }
        notifyStatusChanged()
    }

    private fun restoreBaselineState() {
        if (!config.autoRestoreOnRelease) return

        if (wasPreExistingSecure && config.preservePreExistingFlags) {
            // Keep FLAG_SECURE because it was active prior to SDK involvement
            applySecureFlagToWindow(secure = true)
        } else {
            // Safely clear FLAG_SECURE
            applySecureFlagToWindow(secure = false)
        }
    }

    private fun applySecureFlagToWindow(secure: Boolean) {
        runOnMainThread {
            val window = windowRef.get() ?: return@runOnMainThread
            if (secure) {
                window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    private fun isFlagSecurePresent(window: Window): Boolean {
        val flags = window.attributes.flags
        return (flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
    }

    private fun runOnMainThread(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }

    private fun notifyStatusChanged() {
        val listener = onStatusChangedListener ?: return
        val status = getStatus()
        runOnMainThread {
            listener.invoke(status)
        }
    }

    private fun getPlatformNotes(isSecure: Boolean): String {
        return if (isSecure) {
            "FLAG_SECURE active on window. System screenshot captures & task previews are blocked. Note: Screenshot detection callback is suppressed by Android when FLAG_SECURE is active."
        } else {
            "Window is in standard capture-allowed state. Standard screenshots and screen recordings permitted."
        }
    }
}
