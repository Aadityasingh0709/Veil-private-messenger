<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Android%20SDK-3DDC84?logo=android&logoColor=white" alt="Android SDK" />
  <img src="https://img.shields.io/badge/TypeScript-3178C6?logo=typescript&logoColor=white" alt="TypeScript" />
  <img src="https://img.shields.io/badge/React-61DAFB?logo=react&logoColor=black" alt="React" />
  <img src="https://img.shields.io/badge/Express-000000?logo=express&logoColor=white" alt="Express" />
  <img src="https://img.shields.io/badge/Socket.IO-010101?logo=socketdotio&logoColor=white" alt="Socket.IO" />
  <img src="https://img.shields.io/badge/MongoDB-47A248?logo=mongodb&logoColor=white" alt="MongoDB" />
  <img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="MIT License" />
</p>

# 🔒 Private Messenger

> A privacy-focused communication platform combining full-featured real-time messaging with native mobile digital capture protection and optical anti-capture research.

Private Messenger is designed from the ground up for high-security, sensitive communication workflows across web and native Android platforms. The platform protects visual content across both software layers (using OS compositor controls and screenshot detection) and hardware boundaries (preparing the foundation for optical anti-capture rendering against physical cameras).

---

## ✨ Features by Phase

### 📱 Native Android Capture Protection *(Phase 3 — Current)*
- **Standalone SDK (`capture-protection`)** — Reusable, decoupled Kotlin library (`.aar`) with zero UI or navigation dependencies.
- **Reference-Counted Window Protection** — `SecureWindowController` enforces `WindowManager.LayoutParams.FLAG_SECURE` at the OS compositor level.
- **Pre-Existing Flag Preservation** — Respects baseline window flags so SDK session cleanups never strip pre-existing security states.
- **Thread & Lifecycle Safety** — Thread-safe session registry, automatic dispatch to the main looper, and automated cleanup on Activity destroy via `DefaultLifecycleObserver`.
- **Android 14+ Screenshot Detection** — `ScreenshotDetectionController` integrates native `Activity.ScreenCaptureCallback` with safe fallback on older Android versions.
- **Interactive Verification Demo App (`protection-demo`)** — 100% Jetpack Compose and Material 3 application:
  - **Protection Dashboard**: Live hardware diagnostics, API level, and SDK status monitor.
  - **Normal Viewer**: Baseline unprotected document for screenshot comparison.
  - **Protected Viewer**: Dedicated Activity demonstrating isolated window protection, blocking hardware screenshots, blanking video recordings, and censoring task-switcher previews.
  - **Screenshot Detection Demo**: Live event monitor tracking Android 14+ screenshot capture callbacks.
  - **Capture Testing Lab**: Interactive QA checklist to test and record `PASS`/`FAIL` outcomes for 6 physical verification checkpoints and export markdown logs to clipboard.
  - **SDK Information & Platform Limits**: Educational guide explaining SurfaceFlinger mechanics, OEM variations, and why software flags cannot block physical camera lenses.

### 🖼️ Secure Media Sharing *(Phase 2)*
- **Image Support** — JPEG, PNG, WebP (up to 10 MB) with EXIF/GPS metadata stripping on upload.
- **Video Support** — MP4, WebM (up to 100 MB) with HTTP `206 Partial Content` byte-range streaming for seeking and progressive buffering.
- **Magic-Byte Signature Inspection** — Real file signature validation prevents MIME and extension spoofing.
- **Private Storage Isolation** — Media stored outside public web roots with server-generated opaque keys.
- **Custom Media Viewer** — In-app fullscreen modal with image zoom/scale, video playback controls, and keyboard navigation.
- **Message Deletion & Tombstoning** — Real-time soft-delete for myself or sender-only tombstone ("This message was deleted.") for both participants.
- **Unicode Emoji Picker** — Built-in categorized picker with recent emojis, keyboard shortcuts, and cursor-aware insertion.

### 💬 Core Messaging *(Phase 1)*
- **Real-Time Messaging** — Instant duplex delivery via Socket.IO with typing indicators and read receipts.
- **Persistent History** — Chronological conversation storage in MongoDB with user isolation.
- **Presence Tracking** — Real-time online/offline indicators and last-seen timestamps.
- **User Discovery** — Search users by name or username without exposing email addresses.
- **Robust Authentication** — Argon2id password hashing, HTTP-only session cookies, and durable, fail-closed JWT revocation.

---

## 🏗️ Architecture

```
private-messenger/
├── apps/
│   ├── android-protection/          # Native Android Protection Project (Phase 3)
│   │   ├── capture-protection/      # Reusable Android Library Module (Release AAR)
│   │   │   └── src/main/kotlin/com/securechat/protection/
│   │   │       ├── SecureWindowController.kt
│   │   │       ├── ProtectionSession.kt
│   │   │       ├── ProtectionConfig.kt
│   │   │       ├── ProtectionStatus.kt
│   │   │       └── ScreenshotDetectionController.kt
│   │   ├── protection-demo/         # Jetpack Compose Demo Application (Debug APK)
│   │   │   └── src/main/kotlin/com/securechat/demo/
│   │   │       ├── MainActivity.kt
│   │   │       ├── ProtectedViewerActivity.kt
│   │   │       └── ui/ (HomeScreen, ProtectedViewerScreen, TestingLabScreen, etc.)
│   │   ├── build.gradle.kts
│   │   ├── settings.gradle.kts
│   │   ├── gradle.properties
│   │   └── gradlew / gradlew.bat
│   ├── server/                      # Express + Socket.IO Backend API
│   │   └── src/
│   │       ├── index.ts             # Server entry point
│   │       ├── routes.ts            # REST routes & streaming endpoints
│   │       ├── socket.ts            # Real-time event handlers
│   │       ├── models.ts            # Mongoose schemas
│   │       ├── auth.ts              # JWT authentication & revocation
│   │       └── storage.ts           # Media filesystem adapter
│   └── web/                         # React + Vite Web Client SPA
│       └── src/
│           ├── App.tsx              # Application shell with isolated scrolling
│           ├── api.ts               # HTTP client wrapper
│           ├── styles.css           # Vanilla CSS design system
│           └── components/          # Reusable UI components
├── packages/
│   └── shared/                      # Shared TypeScript contracts & schemas
├── docs/
│   ├── PROJECT_VISION.md            # Master 9-phase roadmap & research vision
│   ├── PROJECT_STATUS.md            # Comprehensive phase audit & status
│   └── PHASE3_ANDROID_PROTECTION.md # Android SDK technical specification & API guide
├── .github/workflows/
│   └── ci.yml                       # Dual CI pipeline (Node.js + Android SDK)
└── package.json                     # Monorepo root workspace
```

---

## 🚀 Getting Started

### 1. Web Application & Backend API

#### Prerequisites
- **Node.js**: v20+ (v24 LTS recommended)
- **MongoDB**: v6+ (running locally at `mongodb://127.0.0.1:27017`)

#### Setup & Launch
```bash
# Clone the repository
git clone https://github.com/Aadityasingh0709/private-messenger.git
cd private-messenger

# Install exact locked monorepo dependencies
npm ci

# Configure environment variables
cp .env.example .env

# Start backend and web frontend in development mode
npm run dev
```

- **Backend API**: `http://localhost:4000`
- **Web Client**: `http://localhost:5173`

*(Optional) Seed sample conversation data*:
```bash
npm run seed
```

---

### 2. Native Android Capture Protection SDK & Demo

#### Prerequisites
- **Java Development Kit**: JDK 17 (e.g. OpenJDK 17 / Amazon Corretto 17)
- **Android SDK**: `compileSdk = 34`, `build-tools = 34.0.0`
- **Android Device / Emulator**: Running Android 8.0 (API 26) or higher (Android 14+ recommended for screenshot detection callback).

#### Build Commands
Navigate to `apps/android-protection/`:

```bash
cd apps/android-protection

# Run SDK unit tests
./gradlew :capture-protection:test

# Build the reusable release Android Archive (AAR)
./gradlew :capture-protection:assembleRelease

# Build the installable demonstration APK
./gradlew :protection-demo:assembleDebug
```

*Windows equivalents*:
```cmd
gradlew.bat :capture-protection:test
gradlew.bat :capture-protection:assembleRelease
gradlew.bat :protection-demo:assembleDebug
```

#### Build Output Artifacts
- **Release AAR**: `apps/android-protection/capture-protection/build/outputs/aar/capture-protection-release.aar`
- **Debug APK**: `apps/android-protection/protection-demo/build/outputs/apk/debug/protection-demo-debug.apk`

---

## 📲 Physical Device Verification Protocol

To verify capture protection on a physical Android device:

```bash
# Install the debug demo APK via ADB
adb install -r apps/android-protection/protection-demo/build/outputs/apk/debug/protection-demo-debug.apk
```

| Step | Screen | Test Action | Expected Result |
|---|---|---|---|
| **1** | **Normal Viewer** | Press `Power + Volume Down` | Screenshot captures the sensitive document normally (baseline). |
| **2** | **Protected Viewer** | Press `Power + Volume Down` | System displays *"Can't take screenshot due to security policy"* or writes solid black file. |
| **3** | **Protected Viewer** | Start system screen recording | Recorded video displays solid black rectangle over the protected window. |
| **4** | **Protected Viewer** | Open Android Recents / App Switcher | App thumbnail card is blanked out in the task overview. |
| **5** | **Screenshot Detection** | Press `Power + Volume Down` | Callback fires, counter increments, and in-app toast notification appears (Android 14+ only; shows unsupported notice on < API 34). |
| **6** | **Capture Testing Lab** | Complete manual checklist | Record `PASS`/`FAIL` verdicts, add observation notes, and tap *"Copy Markdown QA Report"* to export verification logs. |

---

## 📦 Integrating the SDK into Another Android App

1. Copy `capture-protection-release.aar` into your application's `libs/` folder.
2. Add dependencies to your `build.gradle.kts`:
   ```kotlin
   dependencies {
       implementation(files("libs/capture-protection-release.aar"))
       implementation("androidx.core:core-ktx:1.13.1")
       implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
   }
   ```
3. Use the SDK in your Activity:
   ```kotlin
   class SecureViewerActivity : ComponentActivity() {
       private val windowController by lazy { SecureWindowController(this) }

       override fun onCreate(savedInstanceState: Bundle?) {
           super.onCreate(savedInstanceState)
           
           // Acquire protection BEFORE setting visual content
           windowController.acquireProtection("media_viewer")
           windowController.attachLifecycle(this)

           setContent {
               // Render protected media
           }
       }
   }
   ```

---

## 🧪 Automated Testing & Verification Suite

```bash
# 1. Typecheck all web and server packages
npm run typecheck

# 2. Build production web and server bundles
npm run build

# 3. Run backend end-to-end integration tests (requires MongoDB)
npm run test:e2e

# 4. Verify fail-closed JWT token revocation
npm run test:auth-revocation

# 5. Run real-browser Playwright media and emoji tests
npm run test:browser

# 6. Audit web dependencies
npm audit --audit-level=moderate

# 7. Run Android SDK unit tests (JUnit + Mockito)
cd apps/android-protection && ./gradlew :capture-protection:test
```

---

## 🛡️ Security Model & Platform Boundaries

| Security Layer | Enforced Protection | Technical Boundaries |
|---|---|---|
| **Window Surface Security** | `FLAG_SECURE` blocks software screenshots, screen recording, and task-switcher previews. | Applies at window level; cannot selectively protect an individual element inside an unprotected window. |
| **Screenshot Detection** | Native Android 14 `ScreenCaptureCallback` notifies application of captures. | Detection only; does not prevent screenshots. Suppressed by Android when `FLAG_SECURE` is active. Does not detect ADB captures. |
| **Transport & Auth** | Argon2id, HTTP-only secure cookies, and persistent fail-closed JWT revocation. | Server-side deletion cannot un-see or erase copies previously saved by recipients. |
| **Media Delivery** | Opaque keys, magic-byte inspection, EXIF stripping, and `Cache-Control: private, no-store`. | Filesystem storage is single-host; multi-node deployments require cloud object stores. |
| **The Analog Hole** | **Phase 4 Research Goal (Optical Anti-Capture)** | **No OS flag can stop a physical camera from photographing the screen.** Phase 4 will introduce spatial/temporal pattern modulation to degrade external camera captures. |

---

## 🗺️ Master Multi-Phase Roadmap

| Phase | Title | Status | Scope |
|---|---|---|---|
| **Phase 1** | Core Messaging | ✅ **Completed** | Full auth, durable revocation, MongoDB persistence, Socket.IO text messaging |
| **Phase 2** | Media Messaging & Viewer | ✅ **Completed** | Private disk storage, range streaming, custom viewer, soft deletion, emoji picker |
| **Phase 3** | Android Capture Protection SDK | ✅ **Completed** | Reusable `capture-protection` AAR, Compose `protection-demo` APK, physical QA lab |
| **Phase 4** | Optical Anti-Capture Prototype | 🔬 Planned | Software-based spatial/temporal modulation against external camera photography |
| **Phase 5** | Native Android Protection Engine | 🔲 Planned | OpenGL ES / Vulkan / NDK real-time rendering engine |
| **Phase 6** | Native Protected Messenger | 🔲 Planned | Integration of messaging client with native protection SDK |
| **Phase 7** | View-Once & Ephemeral Media | 🔲 Planned | Expiring media attachments with server-side auto-purge |
| **Phase 8** | Adaptive Protection | 🔲 Planned | Dynamic parameter tuning based on ambient light and display properties |
| **Phase 9** | ML Optimization & Benchmarking | 🔲 Planned | Machine learning assisted anti-capture parameter optimization |

> Detailed technical specifications are available in [PROJECT_VISION.md](docs/PROJECT_VISION.md) and [PHASE3_ANDROID_PROTECTION.md](docs/PHASE3_ANDROID_PROTECTION.md).

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

Copyright © 2026 [Aaditya Singh](https://github.com/Aadityasingh0709)
