# Contributing to Veil (Private Messenger)

Thank you for your interest in contributing to **Veil**! This project follows a disciplined multi-phase engineering and scientific research strategy.

---

## 🗺️ Current Project Status: Phase 4B (Spatial & Temporal Display Modulation)

The platform is actively executing **Phase 4B** of its 9-phase roadmap:

| Phase | Milestone | Status | Description |
|---|---|---|---|
| **Phase 1** | Core Web Chat | ✅ **Completed** | Full auth, Argon2id, JWT revocation, MongoDB persistence, Socket.IO duplex text chat |
| **Phase 2** | Secure Media Messaging | ✅ **Completed** | Magic-byte validation, EXIF stripping, chunked HTTP 206 video streaming, tombstoning, emoji picker |
| **Phase 3** | Android Capture Protection SDK | ✅ **Completed** | Reusable `capture-protection` AAR, reference-counted `FLAG_SECURE`, Android 14+ `ScreenCaptureCallback` |
| **Phase 4A** | Optical Baseline System | ✅ **Completed** | Dedicated Android research APK (`:optical-research`), 7 synthetic targets, Python alignment & SSIM/PSNR baseline |
| **Phase 4B** | Display Modulation Research | ✅ **Current** | 6 spatial patterns, Choreographer VSYNC temporal flicker, safety controls, comparative analysis suite |
| **Phase 5+** | Native Engine & Beyond | 🔲 Planned | Low-level C++/Vulkan engine, native client integration, ephemeral media, and ML capture optimization |

---

## 🛠️ Multi-Tier Development Setup

Veil is organized as a monorepo containing Web, Server, Android, and Python components:

### 1. Web & Backend Stack (Node.js & TypeScript)
1. **Clone the repository:**
   ```bash
   git clone https://github.com/Aadityasingh0709/Veil-private-messenger.git
   cd Veil-private-messenger
   ```
2. **Install dependencies:**
   ```bash
   npm install
   ```
3. **Configure Environment:**
   Copy `apps/server/.env.example` to `apps/server/.env`:
   ```bash
   cp apps/server/.env.example apps/server/.env
   ```
   Ensure MongoDB 7.0+ is running locally on `mongodb://127.0.0.1:27017/secure_chat`.
4. **Seed Demo Accounts (Optional):**
   ```bash
   npm run seed
   ```
5. **Start Development Stack:**
   ```bash
   npm run dev
   ```

---

### 2. Android Protection SDK & Research Modules (Kotlin & Gradle)
1. **Prerequisites**: JDK 17 (Temurin recommended) and Android SDK (API 34+).
2. **Navigate to the Android workspace**:
   ```bash
   cd apps/android-protection
   ```
3. **Run Unit Tests**:
   ```bash
   ./gradlew :capture-protection:test :optical-research:test
   ```
4. **Assemble Artifacts**:
   ```bash
   ./gradlew :capture-protection:assembleRelease
   ./gradlew :protection-demo:assembleDebug
   ./gradlew :optical-research:assembleDebug
   ```

---

### 3. Python Computer Vision Pipeline
1. **Navigate to Python research root**:
   ```bash
   cd research/optical-capture/python
   ```
2. **Create and activate virtual environment**:
   ```bash
   python -m venv .venv
   source .venv/bin/activate  # On Windows: .venv\Scripts\activate
   ```
3. **Install dependencies & run tests**:
   ```bash
   pip install -r requirements.txt
   pytest tests/ -v
   ```

---

## 🛡️ Critical Safety & Compliance Guidelines

When contributing to optical research or modulation modules (`apps/android-protection/optical-research/` or `research/optical-capture/python/`):
- **Photobiological Safety**: Hard ceiling of **$f_{\text{max}} = 3.0\text{ Hz}$** must be maintained for temporal alternation in accordance with **WCAG 2.1 Success Criterion 2.3.1**. Frequencies in the $3\text{–}30\text{ Hz}$ range are strictly prohibited.
- **Contrast Ceiling**: Modulation contrast must remain bounded ($\Delta L / L \le 0.15$).
- **Fail-Safe Emergency Controls**: All modulation renderers must maintain an immediate, non-blocking Emergency Stop hook.

---

## 🧪 Verification Before Submitting a PR

Before opening a pull request, run the verification checks relevant to your changes:

```bash
# 1. Typecheck and build all web/server packages
npm run typecheck
npm run build

# 2. Run backend integration tests (requires MongoDB)
npm run test:e2e
npm run test:auth-revocation

# 3. Run Android unit tests
cd apps/android-protection && ./gradlew test

# 4. Run Python CV tests
cd research/optical-capture/python && pytest tests/ -v
```

---

## 📝 Pull Request Guidelines

- **Branch naming**: `feat/<feature-name>`, `fix/<bug-name>`, `docs/<topic>`, `research/<experiment>`.
- **Commit messages**: Follow [Conventional Commits](https://www.conventionalcommits.org/) (`feat: ...`, `fix: ...`, `docs: ...`, `test: ...`, `refactor: ...`).
- **Granular commits**: Keep commits focused and logically separated.
- **Privacy & Security**: Never commit actual `.env` secrets, live API keys, or raw personal media captures.
