# Project Vision — Privacy-Focused Messenger

## 1. Overview and Long-Term Objective

This project is a privacy-focused communication platform designed to enable standard messaging while sensitive visual content receives defense-in-depth protection against unauthorized digital and physical capture.

The platform is designed from the ground up to evolve from a web prototype into a hardened native mobile architecture supporting high-security communication workflows.

---

## 2. Master Multi-Phase Roadmap

1. **Phase 1: Core Chat Application (Completed)**
   - Account registration, login, logout, and persistent session authentication.
   - User discovery and one-to-one conversation creation.
   - Real-time text messaging with Socket.IO.
   - Persistent message history in MongoDB.
   - Typing indicators, presence, and strict conversation isolation.

2. **Phase 2: Media Messaging and Custom Viewer (Completed)**
   - Media attachments (JPEG, PNG, WebP images; MP4, WebM videos).
   - Server-side format allowlists, magic-byte inspection, and size limits.
   - Private filesystem storage outside public directories with server-generated keys.
   - Authorized media delivery with safe headers (`Cache-Control: private, no-store`).
   - HTTP byte-range request streaming for video scrubbing/seeking.
   - In-app custom media viewer for responsive desktop and mobile media consumption.
   - Message deletion (for me / for everyone) and Unicode emoji messaging.

3. **Phase 3: Digital Capture Protection and Native Android SDK (Current Phase)**
   - Reusable native Android capture-protection library (`capture-protection`, `.aar`).
   - Operating system-supported secure window functionality (`FLAG_SECURE`).
   - Testing of system screen recording degradation.
   - External / non-secure display protection.
   - Android 14+ screenshot detection callback integration (`registerScreenCaptureCallback`).
   - Interactive capture testing lab and verification demo app (`protection-demo`, `.apk`).

4. **Phase 4: Optical Anti-Capture Research Prototype**
   - Software-based optical anti-capture rendering attempting to degrade photographs taken with an external physical camera while preserving acceptable human readability.
   - Spatial and temporal pattern modulation.

5. **Phase 5: Advanced Native Android Protection Engine**
   - High-performance native rendering integration (OpenGL ES / Vulkan / NDK).
   - Real-time display refresh synchronization.

6. **Phase 6: Protected Media Integration into the Native Messenger**
   - Porting full messaging, authentication, and media workflows into the native Android client.
   - Direct integration of capture protection into media bubbles and viewer.

7. **Phase 7: View-Once and Advanced Privacy Features**
   - View-once and expiring media attachments.
   - Ephemeral message lifecycles with server-side purging.

8. **Phase 8: Adaptive Protection**
   - Dynamic parameter tuning based on display brightness, ambient light, and device characteristics.

9. **Phase 9: ML-Based Optimization and Benchmarking**
   - Machine-learning assisted parameter search against simulated optical capture models.

---

## 3. Important Research & Platform Limitations Disclaimer

- **Operating System Scope**: `FLAG_SECURE` provides OS-level prevention against software screenshots and system screen recording on standard Android devices. It cannot prevent an adversary from pointing a physical camera at the display.
- **Optical Anti-Capture Status**: Software-based optical anti-capture is the central research objective of Phase 4 and is not yet implemented. We do not claim protection capabilities that have not been implemented and physically verified.
- **No Absolute Immunity**: No combination of software flags or optical patterns can guarantee 100% resistance against all possible capture equipment or compromised device kernels.
