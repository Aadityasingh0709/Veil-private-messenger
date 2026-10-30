package com.securechat.protection

/**
 * Configuration options for window capture protection.
 *
 * @property preservePreExistingFlags If true, the controller notes whether FLAG_SECURE was already
 *   present on the Window prior to any SDK session. Releasing all sessions will not clear a pre-existing flag.
 * @property autoRestoreOnRelease When true, clearing the last active session automatically restores
 *   the window flags to their baseline state.
 */
data class ProtectionConfig(
    val preservePreExistingFlags: Boolean = true,
    val autoRestoreOnRelease: Boolean = true
)
