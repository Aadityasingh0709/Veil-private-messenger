# Phase 3 — Native Android Capture Protection SDK & Verification Demo

## 1. Executive Summary & Objective

Phase 3 introduces the first native Android component for Private Messenger:
1. **`capture-protection`**: A reusable, standalone Android Library (`.aar`) written in Kotlin providing reference-counted window-level capture protection and Android 14+ screenshot detection.
2. **`protection-demo`**: An installable Jetpack Compose demonstration application (`.apk`) providing a physical testing lab to verify screenshot blocking, screen recording degradation, task switcher censoring, and screenshot detection.

> [!IMPORTANT]
> **Preservation of Existing Stack**: The existing web messaging client (React, Vite), backend API (Node.js, Express, TypeScript, MongoDB), and CI pipeline remain 100% intact and functional. Phase 3 operates as an independent Android module under `apps/android-protection/`.

---

## 2. Architecture & Module Design

The Android project is structured as an independent multi-module Gradle project with Kotlin DSL:

```
apps/android-protection/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradlew / gradlew.bat
├── gradle/wrapper/
│   ├── gradle-wrapper.jar
│   └── gradle-wrapper.properties
├── capture-protection/             <-- Reusable Android Library Module (:capture-protection)
│   ├── build.gradle.kts
│   ├── consumer-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   └── kotlin/com/securechat/protection/
│       │       ├── ProtectionConfig.kt
│       │       ├── ProtectionStatus.kt
│       │       ├── ProtectionSession.kt
│       │       ├── SecureWindowController.kt
│       │       └── ScreenshotDetectionController.kt
│       └── test/kotlin/com/securechat/protection/
│           ├── SecureWindowControllerTest.kt
│           ├── ProtectionStatusTest.kt
│           └── ScreenshotDetectionControllerTest.kt
└── protection-demo/                <-- Installable Demonstration Application (:protection-demo)
    ├── build.gradle.kts
    └── src/
        └── main/
            ├── AndroidManifest.xml
            ├── res/
            └── kotlin/com/securechat/demo/
                ├── MainActivity.kt
                ├── ProtectedViewerActivity.kt
                └── ui/
                    ├── HomeScreen.kt
                    ├── NormalViewerScreen.kt
                    ├── ProtectedViewerScreen.kt
                    ├── ScreenshotDetectionScreen.kt
                    ├── TestingLabScreen.kt
                    ├── InfoScreen.kt
                    └── theme/
```

### Module Dependency Flow
```
protection-demo (Android App)
       │
       ▼
capture-protection (Android Library / AAR)
       │
       ▼
Android Window Security APIs (SurfaceFlinger / WindowManager)
```

---

## 3. SDK Public API Specification

The `capture-protection` SDK provides an isolated, minimal API free of UI or navigation dependencies:

### A. `SecureWindowController`
Manages `WindowManager.LayoutParams.FLAG_SECURE` on an Activity or Window:

```kotlin
class SecureWindowController(
    window: Window,
    config: ProtectionConfig = ProtectionConfig()
) {
    constructor(activity: Activity, config: ProtectionConfig = ProtectionConfig())

    companion object {
        fun getInstance(window: Window, config: ProtectionConfig = ProtectionConfig()): SecureWindowController
        fun getInstance(activity: Activity, config: ProtectionConfig = ProtectionConfig()): SecureWindowController
    }

    var detectionController: ScreenshotDetectionController?

    fun acquireProtection(tag: String = "default"): ProtectionSession
    fun releaseProtection(session: ProtectionSession)
    fun releaseAll()
    fun isProtectionActive(): Boolean
    fun getStatus(): ProtectionStatus
    fun attachLifecycle(lifecycleOwner: LifecycleOwner)
    fun setOnStatusChangedListener(listener: ((ProtectionStatus) -> Unit)?)
}
```

#### Key Capabilities:
- **Multi-Controller Coordination**: Uses `getInstance()` with a synchronized `WeakHashMap` to guarantee a single canonical controller per Window, preventing reference count collisions between disparate components.
- **Reference Counting**: Multiple callers (e.g. image viewer, sensitive text bubble) acquire independent sessions. `FLAG_SECURE` remains active on the window as long as `activeSessionsCount > 0`.
- **Pre-existing State Preservation**: If `FLAG_SECURE` was already present before the SDK was attached, the controller will NOT clear the flag when all SDK sessions are released (unless configured otherwise).
- **Screenshot Detection Integration**: Associated `detectionController` allows `getStatus()` to accurately reflect live detection registration and explain OS suppression states without returning fabricated data.
- **Main-Thread Dispatch**: Mutations to window layout parameters are safely marshaled to `Looper.getMainLooper()`.
- **Lifecycle Awareness**: Binds to `LifecycleOwner` to guarantee that all sessions release when an Activity is destroyed.
- **Memory Safety**: Uses `WeakReference<Window>` and `WeakReference<Activity>` to prevent Activity context leaks.

### B. `ProtectionSession`
```kotlin
interface ProtectionSession : AutoCloseable {
    val sessionId: String
    val tag: String
    val isActive: Boolean
    val acquiredAtMillis: Long
    fun release()
    override fun close() = release()
}
```
Supports Kotlin's `use` scope:
```kotlin
controller.acquireProtection("media_viewer").use { session ->
    // Sensitive visual content rendered here while FLAG_SECURE is active
} // Session automatically released here
```

### C. `ScreenshotDetectionController` & `ScreenshotDetectionStatus`
Manages Android 14's native `Activity.registerScreenCaptureCallback`:

```kotlin
class ScreenshotDetectionController(
    activity: Activity,
    onScreenshotCaptured: () -> Unit
) {
    companion object {
        fun isSupported(): Boolean // Returns true if API >= 34
    }

    fun register(): Boolean
    fun unregister()
    fun isRegistered(): Boolean
    fun getStatus(): ScreenshotDetectionStatus
    fun attachLifecycle(lifecycleOwner: LifecycleOwner)
}

data class ScreenshotDetectionStatus(
    val isSupported: Boolean,
    val isRegistered: Boolean,
    val androidApiLevel: Int = Build.VERSION.SDK_INT,
    val notes: String = ""
)
```

---

## 4. Technical Distinction: Prevention vs. Detection

A crucial security distinction must be understood:

| Attribute | `FLAG_SECURE` Window Protection | Android 14 `ScreenCaptureCallback` |
|---|---|---|
| **Mechanism** | OS Compositor (SurfaceFlinger) blocks screen capture | Activity callback notifies the app of capture |
| **Prevents Screenshots?** | **YES** (image is blacked out or rejected) | **NO** (capture already occurred) |
| **Screen Recording?** | **YES** (window surface recorded as solid black) | **NO** |
| **Recent-Apps Preview?** | **YES** (task switcher thumbnail is censored) | **NO** |
| **Callback Behavior** | Suppresses screenshot detection callbacks | Fires only when screenshots succeed |
| **ADB Capture** | Blocked | **Not detected** (documented Android limitation) |
| **Permission** | None required | `<uses-permission android:name="android.permission.DETECT_SCREEN_CAPTURE" />` |
| **API Support** | Android 8.0+ (API 26+) | Android 14+ (API 34+) |

> [!WARNING]
> When `FLAG_SECURE` is active, Android suppresses the screenshot detection callback. Therefore, the demonstration application provides **separate testing modes** for Protected Viewer and Screenshot Detection.

---

## 5. Security Boundaries & Platform Limitations

The SDK explicitly documents the boundaries of operating system security:

1. **Window-Level Scope**: `FLAG_SECURE` applies to the entire Window surface. It is not a per-element canvas shader or image filter.
2. **OEM Custom ROMs**: Certain custom Android ROMs or vendor modifications have historically exhibited inconsistent `FLAG_SECURE` enforcement or recorder loopholes.
3. **Accessibility & Overlay Vectors**: Malicious background services with system alert window or accessibility permissions may attempt overlay-based inference.
4. **Root & Kernel Compromise**: Rooted devices or kernel-level frame-buffer dumpers bypass WindowManager policies.
5. **HDMI / External Displays**: `FLAG_SECURE` blocks display on non-secure external displays, but hardware capture cards equipped with HDCP stripping present attack vectors outside Android's software boundary.
6. **The Analog Hole (Physical Cameras)**: **No software API or operating system flag can prevent an adversary from pointing a second phone or camera at the screen.** Addressing this vulnerability is the objective of **Phase 4 (Software-Based Optical Anti-Capture Research)**.

---

## 6. How to Build & Integrate

### A. Build Commands

From the `apps/android-protection/` directory:

```bash
# Build the release Android Archive (AAR)
./gradlew :capture-protection:assembleRelease

# Build the debug installable APK
./gradlew :protection-demo:assembleDebug

# Run unit tests
./gradlew :capture-protection:test
```

**Windows equivalents**:
```cmd
gradlew.bat :capture-protection:assembleRelease
gradlew.bat :protection-demo:assembleDebug
gradlew.bat :capture-protection:test
```

### B. Output Artifact Locations

- **Release AAR**: `apps/android-protection/capture-protection/build/outputs/aar/capture-protection-release.aar`
- **Debug APK**: `apps/android-protection/protection-demo/build/outputs/apk/debug/protection-demo-debug.apk`

### C. Integrating the Library into Another Android Project

1. Copy `capture-protection-release.aar` into your application's `libs/` folder.
2. In your `build.gradle.kts`:
   ```kotlin
   dependencies {
       implementation(files("libs/capture-protection-release.aar"))
       implementation("androidx.core:core-ktx:1.13.1")
       implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
   }
   ```
3. Usage in an Activity:
   ```kotlin
   class SecureMediaViewerActivity : ComponentActivity() {
       private val windowController by lazy { SecureWindowController(this) }

       override fun onCreate(savedInstanceState: Bundle?) {
           super.onCreate(savedInstanceState)
           // Acquire protection BEFORE setting content
           windowController.acquireProtection("media_viewer")
           windowController.attachLifecycle(this)

           setContent {
               // Render sensitive media
           }
       }
   }
   ```

---

## 7. Physical Device Verification Procedure

To verify on an actual Android device:

1. **Install the Debug APK**:
   ```bash
   adb install -r apps/android-protection/protection-demo/build/outputs/apk/debug/protection-demo-debug.apk
   ```
2. **Test 1: Normal Viewer (Baseline)**:
   - Open Normal Viewer.
   - Press `Power + Volume Down`.
   - **Expected**: Screenshot contains the confidential record.
3. **Test 2: Protected Viewer (Screenshot Block)**:
   - Open Protected Media Viewer.
   - Press `Power + Volume Down`.
   - **Expected**: System rejects screenshot with *"Can't take screenshot due to security policy"* or saves a completely black image.
4. **Test 3: Screen Recording**:
   - Start Android quick-settings screen recording.
   - Navigate into Protected Media Viewer.
   - Stop recording and play the video.
   - **Expected**: Protected viewer appears as a solid black rectangle.
5. **Test 4: Task Switcher (Recent Apps)**:
   - While in Protected Viewer, open the Android task switcher carousel.
   - **Expected**: The preview thumbnail is censored/blanked.
6. **Test 5: Screenshot Detection (Android 14+)**:
   - Open Screenshot Detection Demo.
   - Press `Power + Volume Down`.
   - **Expected**: Counter increments and toast notification fires. (If Android < 14, shows graceful unsupported message).
7. **Test 6: Capture Testing Lab**:
   - Open Capture Testing Lab.
   - Record `PASS`/`FAIL` for each checkpoint.
   - Tap *"Copy Markdown QA Report to Clipboard"* to export the log.

### Physical Device Verification Results (Xiaomi 2312DRAABI)

Hardware verification was performed and passed on physical device:
- **Device**: Xiaomi 2312DRAABI (gold)
- **Android OS**: Android 15 (API 35)
- **Date**: 2026-11-01 22:43:53

| ID | Test Case | Category | Verdict | Result & Behavior |
|---|---|---|---|---|
| TC-01 | Protected Viewer Screenshot Block | Screenshot Prevention | **PASS** | Captured black image / policy warning |
| TC-02 | Normal Viewer Screenshot Capture | Screenshot Baseline | **PASS** | Screenshot captured with visible content |
| TC-03 | Protected Viewer Video Recording Block | Screen Recording | **PASS** | Recording displays solid black rectangle |
| TC-04 | Recent-Apps Preview Censor | Task Switcher | **PASS** | Carousel preview censored / blank |
| TC-05 | Android 14+ Detection Callback | Screenshot Detection | **PASS** | Native ScreenCaptureCallback fired |
| TC-06 | Non-Secure Display Projection | External Display | **PASS** | Non-secure projection blocked / blacked out |

---

## 8. Preserved Web Stack Status

- `apps/web`: React + Vite client remains active and unchanged.
- `apps/server`: Express + Socket.IO + MongoDB backend remains intact.
- Monorepo builds (`npm run typecheck`, `npm run build`, `npm run test:e2e`) all continue to pass.
