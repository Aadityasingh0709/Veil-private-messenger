<p align="center">
  <img src="https://img.shields.io/badge/TypeScript-3178C6?logo=typescript&logoColor=white" alt="TypeScript" />
  <img src="https://img.shields.io/badge/React-61DAFB?logo=react&logoColor=black" alt="React" />
  <img src="https://img.shields.io/badge/Express-000000?logo=express&logoColor=white" alt="Express" />
  <img src="https://img.shields.io/badge/Socket.IO-010101?logo=socketdotio&logoColor=white" alt="Socket.IO" />
  <img src="https://img.shields.io/badge/MongoDB-47A248?logo=mongodb&logoColor=white" alt="MongoDB" />
  <img src="https://img.shields.io/badge/Vite-646CFF?logo=vite&logoColor=white" alt="Vite" />
  <img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="MIT License" />
</p>

# 🔒 Private Messenger

> A privacy-focused, real-time one-to-one messaging platform with secure media sharing, built from the ground up with security as a first-class citizen.

Private Messenger is designed for sensitive, direct communication — combining real-time text chat with hardened media upload, validation, and streaming. The platform prioritizes user privacy through EXIF metadata stripping, magic-byte file validation, private storage isolation, and content-security hardened media delivery.

---

## ✨ Features

### Core Messaging
- **Real-time text messaging** — instant delivery via Socket.IO with typing indicators and read receipts
- **Persistent conversations** — full message history stored in MongoDB with chronological ordering
- **Message deletion** — hide messages for yourself or let the sender tombstone a message for both participants, synchronized in real time
- **Emoji picker** — searchable Unicode emoji categories, recent emojis, and cursor-aware insertion into ordinary messages
- **User presence** — live online/offline status with last-seen timestamps
- **User discovery** — search by name or username without exposing email addresses

### Secure Media Sharing *(Phase 2)*
- **Image support** — JPEG, PNG, WebP (up to 10 MB)
- **Video support** — MP4, WebM (up to 100 MB)
- **Magic-byte validation** — real file signature inspection prevents MIME/extension spoofing
- **EXIF stripping** — location-bearing metadata automatically removed from images on upload
- **Private storage** — files stored outside public directories with server-generated keys
- **Byte-range streaming** — HTTP `206 Partial Content` for video seeking and progressive buffering
- **Custom media viewer** — in-app fullscreen modal with image scaling, video playback controls, and keyboard navigation

### Authentication & Security
- **Session-based auth** — JWT tokens in HTTP-only cookies with durable revocation, fail-closed revocation checks, and Argon2 password hashing
- **Rate limiting** — configurable request throttling via `express-rate-limit`
- **Conversation isolation** — strict participant verification on every message and media request
- **Private media** — authorization on every fetch, `Cache-Control: private, no-store`, strict content headers, bounded disk uploads, and signature checks

---

## 🏗️ Architecture

```
private-messenger/
├── apps/
│   ├── server/          # Express + Socket.IO backend API
│   │   └── src/
│   │       ├── index.ts          # Server entry point
│   │       ├── routes.ts         # REST API routes & media streaming
│   │       ├── socket.ts         # Socket.IO event handlers
│   │       ├── models.ts         # Mongoose schemas (User, Conversation, Message, MediaAsset)
│   │       ├── auth.ts            # JWT validation, revocation, and auth middleware
│   │       ├── storage.ts        # Storage adapter for media persistence
│   │       ├── serializers.ts    # DTO serializers for API responses
│   │       ├── seed.ts           # Demo data seeder
│   │       ├── e2e-test.ts       # API integration, authorization, deletion, and emoji tests
│   │       └── browser-e2e.ts    # Playwright image, video, emoji, and deletion tests
│   └── web/             # React + Vite frontend SPA
│       └── src/
│           ├── App.tsx           # Main app shell with routing
│           ├── api.ts            # HTTP client wrapper
│           ├── styles.css        # Global styles (Inter font, dark theme)
│           └── components/
│               ├── AuthForm.tsx        # Login / Registration
│               ├── ChatList.tsx        # Conversation sidebar
│               ├── MessageBubble.tsx   # Text & media message rendering
│               ├── MessageComposer.tsx # Text, emoji picker & media upload
│               ├── MediaViewer.tsx     # Fullscreen media viewer modal
│               ├── UserSearch.tsx      # User discovery search
│               ├── Navigation.tsx      # Sidebar navigation
│               ├── ProfileModal.tsx    # User profile dialog
│               ├── Avatar.tsx          # User avatar with status indicator
│               └── States.tsx          # Loading, empty, error states
├── packages/
│   └── shared/          # Shared validation schemas & type contracts
│       └── src/index.ts          # Zod schemas, MIME allowlists, DTO types
├── docs/
│   ├── PROJECT_VISION.md         # Multi-phase roadmap & research notes
│   └── PROJECT_STATUS.md         # Phase completion audit & test results
└── package.json         # Workspace root (npm workspaces)
```

The project uses an **npm workspaces** monorepo with three packages:

| Package | Scope | Description |
|---------|-------|-------------|
| `@secure-chat/server` | `apps/server` | Express REST API + Socket.IO real-time server |
| `@secure-chat/web` | `apps/web` | React SPA built with Vite |
| `@secure-chat/shared` | `packages/shared` | Zod validation schemas, MIME allowlists, and shared TypeScript types |

---

## 🚀 Getting Started

### Prerequisites

| Requirement | Version |
|-------------|---------|
| [Node.js](https://nodejs.org/) | 24 LTS |
| [MongoDB](https://www.mongodb.com/try/download/community) | 6+ (running locally) |

### Installation

```bash
# Clone the repository
git clone https://github.com/Aadityasingh0709/private-messenger.git
cd private-messenger

# Install the exact locked workspace dependencies
npm ci
```

### Configuration

Copy the example environment file and adjust as needed:

```bash
cp .env.example .env
```

```env
# Database
MONGODB_URI=mongodb://127.0.0.1:27017/secure_chat

# Authentication
JWT_SECRET=replace-with-a-long-random-secret-at-least-32-characters

# CORS
CLIENT_ORIGIN=http://localhost:5173

# Server
PORT=4000

# Media upload limits
MAX_IMAGE_SIZE_BYTES=10485760      # 10 MB
MAX_VIDEO_SIZE_BYTES=104857600     # 100 MB
MEDIA_STORAGE_DIR=storage/media
```

### Running

```bash
# Start both server and client in development mode
npm run dev
```

This launches:
- **Backend API** → `http://localhost:4000`
- **Web Client** → `http://localhost:5173`

> **Tip:** Open `http://localhost:5173` in two separate browser profiles (or one incognito window) to test real-time two-way messaging.

### Seed Demo Data *(Optional)*

```bash
npm run seed
```

---

## 🧪 Verification

```bash
# Typecheck all packages
npm run typecheck

# Production build
npm run build

# End-to-end integration tests (requires running MongoDB)
npm run test:e2e

# Fail-closed JWT revocation failure checks
npm run test:auth-revocation

# Real-browser image/video playback, emoji, and deletion tests (requires MongoDB and both dev servers)
npm run test:browser

# Dependency security audit
npm audit --audit-level=moderate
```

The integration suite covers authentication, messaging, media authorization/upload/streaming, deletion, and Unicode persistence. The Playwright suite opens two independent browser sessions to exercise media rendering and playback plus the message composer. See [PROJECT_STATUS.md](docs/PROJECT_STATUS.md) for executed results and remaining verification gaps.

---

## 📡 API Reference

### REST Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/auth/register` | Create a new account |
| `POST` | `/api/auth/login` | Authenticate and receive session cookie |
| `POST` | `/api/auth/logout` | Invalidate session |
| `GET` | `/api/me` | Get authenticated user profile |
| `GET` | `/api/users?q=...` | Search users by name/username (emails hidden) |
| `GET` | `/api/conversations` | List conversations with last-message preview |
| `POST` | `/api/conversations` | Create or retrieve a conversation |
| `GET` | `/api/conversations/:id/messages` | Fetch message history (text & media) |
| `DELETE` | `/api/messages/:messageId` | Hide with `{ "scope": "me" }` or sender-only tombstone with `{ "scope": "everyone" }` |
| `POST` | `/api/conversations/:id/media` | Upload media attachment (multipart) |
| `GET` | `/api/media/:mediaId` | Retrieve media file (auth required) |
| `GET` | `/api/conversations/:id/media/:mediaId` | Retrieve media with conversation-scoped auth |

### Socket.IO Events

| Direction | Event | Payload |
|-----------|-------|---------|
| Client → Server | `conversation:join` | `conversationId` |
| Client → Server | `message:send` | `{ conversationId, text }` |
| Client → Server | `messages:read` | `conversationId` |
| Client → Server | `typing` | `{ conversationId, isTyping }` |
| Server → Client | `message:deleted` | Authenticated deletion notification; emitted to the affected user(s) |
| Server → Client | `message:new` | `MessageDto` (text or media) |
| Server → Client | `messages:read` | `{ conversationId }` |
| Server → Client | `typing:update` | `{ conversationId, userId, isTyping }` |
| Server → Client | `presence:update` | `{ userId, online, lastSeen? }` |

---

## 🛡️ Security Model

| Layer | Protection |
|-------|------------|
| **Authentication** | Argon2id password hashing, HTTP-only cookies, persisted JWT revocation; revocation-store errors fail closed |
| **Authorization** | Conversation participant verification on every request |
| **File Validation** | Magic-byte signature inspection rejects spoofed files (HTML-as-JPEG, SVGs, executables) |
| **Metadata Privacy** | EXIF/GPS tags stripped from images on upload |
| **Storage Isolation** | Media stored outside web root with server-generated opaque keys |
| **Response Hardening** | `CSP: default-src 'none'`, `X-Content-Type-Options: nosniff`, sensitive-media `Cache-Control: private, no-store` |
| **Upload Safety** | Multer 2.5.0, bounded temporary disk storage, multipart limits and per-IP upload rate limits |
| **Deletion** | Soft-delete tombstones persist for both participants; previously downloaded or captured copies cannot be erased by the server |
| **Rate Limiting** | Configurable per-IP request throttling |
| **Input Validation** | Zod schemas enforce strict type/size/format constraints on all inputs |

---

## 🗺️ Roadmap

| Phase | Status | Focus |
|-------|--------|-------|
| **Phase 1** | ✅ Complete | Core auth, real-time text messaging, presence, conversation isolation |
| **Phase 2** | 🧪 Final verification | Secure media, message deletion, emoji insertion, and browser playback validation |
| **Phase 3** | 🔲 Planned | View-once media, message expiration timers, server-side asset purging |
| **Phase 4** | 🔲 Planned | Optical anti-capture rendering, screenshot/recording protections |
| **Phase 5** | 🔲 Planned | Native Android client (Kotlin + Jetpack Compose) with protection SDK |

> See [PROJECT_VISION.md](docs/PROJECT_VISION.md) for the full roadmap and research notes.

---

## 🤝 Contributing

Contributions are welcome! Please read our [Contributing Guide](CONTRIBUTING.md) and [Code of Conduct](CODE_OF_CONDUCT.md) before submitting a pull request.

```bash
# Quick verification before submitting a PR
npm run typecheck && npm run build && npm run test:e2e && npm run test:browser && npm audit
```

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

Copyright © 2026 [Aaditya Singh](https://github.com/Aadityasingh0709)
