# Project Status — Phase 2 Finalization

## Current Verdict: READY TO FREEZE

Phase 2 now includes the existing private media workflow plus message deletion and Unicode emoji messaging. The acceptance checks below were run locally against MongoDB and real Chromium browser sessions. The GitHub Actions workflow has been updated, but no hosted Actions run was triggered in this session.

## Phase 1 — Preserved

- Cookie authentication, bearer-token API support, registration, login, and logout.
- Durable JWT revocation, strict conversation authorization, and real-time socket messaging.
- User discovery, one-to-one conversations, message history, read receipts, typing, and presence.
- Query-string JWT authentication remains unsupported.

## Phase 2 — Media Messaging

- JPEG, PNG, WebP, MP4, and WebM allowlists with server-side signature validation and configured image/video size limits.
- Multer 2.5.0 is installed in the active dependency tree; multipart requests are bounded and temporary uploads use disk storage outside public directories.
- Private storage uses generated storage keys. Authorized media fetches validate conversation membership and message visibility.
- Full and byte-range media responses use `Cache-Control: private, no-store`; video range requests and custom viewers are retained.
- JPEG/PNG privacy processing and cleanup of failed uploads remain in place.

## Authentication and Revocation Hardening

- Revocation lookup errors now fail closed; authenticated HTTP requests return a service-unavailable response rather than accepting a token.
- Logout verifies the signed token and persists revocation before reporting success or clearing the matching cookie. Storage-write failures return an error and leave the cookie intact.
- Revocation entries use the token's `jti` and expire at the token's expiration. Legacy tokens without a `jti` are keyed by a token digest.
- Socket.IO connections verify revocation, packet checks disconnect on store errors, and logout disconnects only sockets for the revoked `jti`.

## Message Deletion

- `DELETE /api/messages/:messageId` with `{ "scope": "me" }` hides a message from the requesting user's history and list preview without affecting the other participant.
- `{ "scope": "everyone" }` is sender-only and creates a persistent redacted tombstone displayed as “This message was deleted.” No time limit is imposed.
- Deletion broadcasts server-authenticated `message:deleted` updates to the affected user rooms.
- Media hidden for one user cannot be fetched by that user. Global media deletion blocks access for both users and removes the underlying object when no live message references it.
- Deletion cannot erase copies previously downloaded, screenshotted, recorded, or otherwise captured by recipients.

## Emoji Messaging

- The composer uses `emoji-picker-react` 5.1.4 with searchable emoji categories, recent emojis, keyboard navigation, outside-click/Escape dismissal, and a compact mobile layout.
- Selection inserts Unicode at the remembered text selection and leaves sending to the user. Emoji-only and mixed text/emoji messages persist as ordinary Unicode text.
- No reactions or counters were introduced.

## Dependencies and CI

- Active Multer version: 2.5.0. `npm audit --audit-level=moderate` reported no known vulnerabilities.
- Added `emoji-picker-react` 5.1.4. The lockfile was updated with the package manifest.
- CI now uses Node.js 24 and `npm ci`; it typechecks, builds, runs integration and revocation-failure tests, installs Chromium, runs Playwright browser tests, and audits dependencies.

## Verification Results

Commands were run locally; the CI workflow itself was not triggered on GitHub.

| Command / scenario | Result |
|---|---|
| `npm run typecheck` | PASS |
| `npm run build` | PASS; Vite reports a large-chunk advisory and upstream annotation warnings |
| `npm run test:e2e` | PASS — 51 numbered Phase 1/2 integration, authorization, deletion, storage, and emoji checks |
| `npm run test:auth-revocation` | PASS — simulated revocation lookup/write failures fail closed; expired tokens are rejected |
| `npm run test:browser` | PASS — two isolated browser users; image rendered; genuine WebM metadata, playback advancement, seeking, and close/pause checked; emoji search/categories/cursor insertion/emoji-only send; deletion synchronization/persistence; mobile picker |
| `npm audit --audit-level=moderate` | PASS — 0 vulnerabilities |
| `npm ls multer emoji-picker-react --all` | PASS — Multer 2.5.0 and emoji-picker-react 5.1.4 active |
| `git diff --check` | PASS |

The browser video fixture is a 2-second, 2.3 KB WebM. `test-fixtures/generate.mjs` regenerates it from browser-generated JPEG frames when `FFMPEG_PATH` points to an FFmpeg build with MJPEG decoding and libvpx-vp9 encoding.

## Remaining Limitations

- The GitHub Actions workflow has not yet been observed on GitHub; its constituent local commands passed.
- Revocation-store failure behavior is tested with simulated storage failures, not by interrupting the live MongoDB service.
- Local filesystem storage is intended for single-node development/staging; production multi-node deployments need shared object storage.
- There is no end-to-end encryption, screenshot/recording protection, view-once or expiring media, group chat, calling, or Android client. These remain outside Phase 2.

## Phase Decision

**Recommendation: freeze Phase 2** after merging this verified change set. The requested Phase 1 regressions, private media behavior, both deletion scopes, emoji insertion/persistence, real browser playback, and fail-closed revocation paths have been validated locally. Do not treat server-side deletion as guaranteed erasure of recipient copies.
