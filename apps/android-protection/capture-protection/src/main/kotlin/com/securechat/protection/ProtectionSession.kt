package com.securechat.protection

/**
 * Handle representing an active protection claim on a Window.
 *
 * Implements [AutoCloseable] so that callers can use standard Kotlin `use { ... }` blocks.
 */
interface ProtectionSession : AutoCloseable {
    /**
     * Unique identifier for this session claim.
     */
    val sessionId: String

    /**
     * Optional caller tag identifying which component requested protection (e.g. "media_viewer").
     */
    val tag: String

    /**
     * True if this session is currently active and holding a reference on the controller.
     */
    val isActive: Boolean

    /**
     * Timestamp in milliseconds when the session was acquired.
     */
    val acquiredAtMillis: Long

    /**
     * Releases this protection claim. Idempotent: safe to call multiple times.
     */
    fun release()

    /**
     * Closes the session (alias to [release] for AutoCloseable support).
     */
    override fun close() = release()
}
