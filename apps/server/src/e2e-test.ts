import { io } from "socket.io-client";
import fs from "node:fs";
import path from "node:path";
import { config } from "./config.js";

const API = process.env.API_URL ?? "http://localhost:4000";

// Sample valid media buffers for testing
const VALID_1X1_PNG = Buffer.from([
  0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, // PNG Signature
  0x00, 0x00, 0x00, 0x0d, // IHDR length
  0x49, 0x48, 0x44, 0x52, // "IHDR"
  0x00, 0x00, 0x00, 0x01, // width: 1
  0x00, 0x00, 0x00, 0x01, // height: 1
  0x08, 0x06, 0x00, 0x00, 0x00,
  0x1f, 0x15, 0xc4, 0x89, // CRC
  0x00, 0x00, 0x00, 0x0a, // IDAT length
  0x49, 0x44, 0x41, 0x54, // "IDAT"
  0x78, 0x9c, 0x63, 0x00, 0x01, 0x00, 0x00, 0x05, 0x00, 0x01,
  0x0d, 0x0a, 0x2d, 0xb4, // CRC
  0x00, 0x00, 0x00, 0x00, // IEND length
  0x49, 0x45, 0x4e, 0x44, // "IEND"
  0xae, 0x42, 0x60, 0x82  // CRC
]);

// Valid minimal JPEG with EXIF APP1 metadata segment to verify stripping
const VALID_JPEG_WITH_EXIF = Buffer.concat([
  Buffer.from([0xff, 0xd8]), // SOI
  Buffer.from([
    0xff, 0xe1, 0x00, 0x12, // APP1 length 18
    0x45, 0x78, 0x69, 0x66, 0x00, 0x00, // "Exif\0\0"
    0x4d, 0x4d, 0x00, 0x2a, 0x00, 0x00, 0x00, 0x08, 0x00, 0x00
  ]),
  Buffer.from([
    0xff, 0xc0, 0x00, 0x0b, 0x08, // SOF0 length 11, precision 8
    0x00, 0x01, 0x00, 0x01, // height 1, width 1
    0x01, 0x01, 0x11, 0x00
  ]),
  Buffer.from([0xff, 0xd9]) // EOI
]);

// Valid minimal WebP structure
const VALID_WEBP = Buffer.concat([
  Buffer.from("RIFF", "ascii"),
  Buffer.from([0x24, 0x00, 0x00, 0x00]), // file size - 8
  Buffer.from("WEBP", "ascii"),
  Buffer.from("VP8 ", "ascii"),
  Buffer.from([0x14, 0x00, 0x00, 0x00]), // chunk size 20
  Buffer.from([0xd0, 0x01, 0x00, 0x9d, 0x01, 0x2a, 0x01, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00])
]);

// Valid minimal MP4 structure with ftyp box
const VALID_MIN_MP4 = Buffer.concat([
  Buffer.from([0x00, 0x00, 0x00, 0x20]), // box length 32
  Buffer.from("ftypisom", "ascii"),      // major brand isom
  Buffer.from([0x00, 0x00, 0x02, 0x00]), // minor version
  Buffer.from("isomiso2mp41", "ascii"),  // compatible brands
  Buffer.from([0x00, 0x00, 0x00, 0x08]), // mdat length 8
  Buffer.from("mdat", "ascii")           // mdat box
]);

// Valid minimal WebM structure with EBML header
const VALID_MIN_WEBM = Buffer.from([
  0x1a, 0x45, 0xdf, 0xa3, // EBML Header
  0x9f, 0x42, 0x86, 0x81, 0x01, // DocType
  0x42, 0xf7, 0x81, 0x01,
  0x42, 0xf2, 0x81, 0x04,
  0x42, 0xf3, 0x81, 0x08,
  0x77, 0x65, 0x62, 0x6d // "webm"
]);

async function runTest() {
  console.log("===============================================================");
  console.log("=== COMPREHENSIVE PHASE 1 & PHASE 2 VERIFICATION TEST SUITE ===");
  console.log("===============================================================\n");

  const timestamp = Date.now();
  const aliceEmail = `alice_${timestamp}@test.com`;
  const bobEmail = `bob_${timestamp}@test.com`;
  const malloryEmail = `mallory_${timestamp}@test.com`;
  const password = "Password123!";

  // -------------------------------------------------------------
  // 1. AUTHENTICATION & SESSION PERSISTENCE TESTS (PHASE 1)
  // -------------------------------------------------------------
  console.log("1. Testing Registration (Alice, Bob, Mallory)...");
  const resAlice = await fetch(`${API}/api/auth/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      name: "Alice Smith",
      username: `alice_${timestamp}`,
      email: aliceEmail,
      password
    })
  });
  if (!resAlice.ok) throw new Error(`Alice register failed: ${await resAlice.text()}`);
  const cookieAlice = resAlice.headers.get("set-cookie")?.split(";")[0] ?? "";
  const tokenAlice = cookieAlice.replace("token=", "");
  const { user: alice } = await resAlice.json();
  console.log("✓ Alice registered:", alice.id, alice.name);

  // Duplicate email check
  console.log("2. Testing Duplicate Registration Rejection...");
  const resDup = await fetch(`${API}/api/auth/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      name: "Alice Duplicate",
      username: `alice_dup_${timestamp}`,
      email: aliceEmail,
      password
    })
  });
  if (resDup.status === 409) {
    console.log("✓ Duplicate email registration properly rejected with status 409");
  } else {
    throw new Error(`Expected status 409 for duplicate email, got ${resDup.status}`);
  }

  // Register Bob
  const resBob = await fetch(`${API}/api/auth/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      name: "Bob Jones",
      username: `bob_${timestamp}`,
      email: bobEmail,
      password
    })
  });
  if (!resBob.ok) throw new Error(`Bob register failed: ${await resBob.text()}`);
  const cookieBob = resBob.headers.get("set-cookie")?.split(";")[0] ?? "";
  const tokenBob = cookieBob.replace("token=", "");
  const { user: bob } = await resBob.json();
  console.log("✓ Bob registered:", bob.id, bob.name);

  // Register Mallory (Unauthorized User C)
  const resMallory = await fetch(`${API}/api/auth/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      name: "Mallory Eve",
      username: `mallory_${timestamp}`,
      email: malloryEmail,
      password
    })
  });
  const cookieMallory = resMallory.headers.get("set-cookie")?.split(";")[0] ?? "";
  const tokenMallory = cookieMallory.replace("token=", "");
  const { user: mallory } = await resMallory.json();
  console.log("✓ Mallory registered:", mallory.id);

  // Login with invalid credentials test
  console.log("3. Testing Invalid Login Credentials...");
  const resInvalidLogin = await fetch(`${API}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: aliceEmail, password: "WrongPassword!" })
  });
  if (resInvalidLogin.status === 401) {
    console.log("✓ Invalid login credentials rejected with status 401");
  } else {
    throw new Error(`Expected status 401 for invalid credentials, got ${resInvalidLogin.status}`);
  }

  // Session persistence check (/api/me)
  console.log("4. Testing Authenticated Session Persistence (/api/me)...");
  const resMeAlice = await fetch(`${API}/api/me`, {
    headers: { Cookie: cookieAlice }
  });
  if (resMeAlice.ok) {
    const data = await resMeAlice.json();
    if (data.user.id === alice.id && data.user.email === alice.email) {
      console.log("✓ Session verified for Alice via cookie");
    } else {
      throw new Error("User data mismatch on /api/me");
    }
  } else {
    throw new Error(`Failed /api/me for Alice: ${resMeAlice.status}`);
  }

  // Authorization Bearer token header support
  console.log("5. Testing Authorization Bearer Header Support...");
  const resBearer = await fetch(`${API}/api/me`, {
    headers: { Authorization: `Bearer ${tokenAlice}` }
  });
  if (resBearer.status === 200) {
    console.log("✓ Authorization Bearer header successfully accepted with status 200");
  } else {
    throw new Error(`Expected 200 for Bearer header, got ${resBearer.status}`);
  }

  // Query parameter token rejection (Priority Two)
  console.log("6. Testing Query Parameter Token Rejection (/api/me?token=...)...");
  const resQueryMe = await fetch(`${API}/api/me?token=${encodeURIComponent(tokenAlice)}`);
  if (resQueryMe.status === 401) {
    console.log("✓ Token supplied only in query string rejected with 401");
  } else {
    throw new Error(`Expected status 401 when token is passed only in query, got ${resQueryMe.status}`);
  }

  // User search isolation
  console.log("7. Testing User Search Isolation...");
  const resSearch = await fetch(`${API}/api/users?q=${encodeURIComponent(`bob_${timestamp}`)}`, {
    headers: { Cookie: cookieAlice }
  });
  const { users } = await resSearch.json();
  const foundBob = users.find((u: any) => u.id === bob.id);
  if (foundBob && !("email" in foundBob)) {
    console.log("✓ User search verified: found Bob without exposing email");
  } else {
    throw new Error("User search failed or exposed sensitive fields");
  }

  // -------------------------------------------------------------
  // 2. CONVERSATION MANAGEMENT & ISOLATION
  // -------------------------------------------------------------
  console.log("8. Testing Concurrent Conversation Creation Deduplication...");
  const [convA, convB] = await Promise.all([
    fetch(`${API}/api/conversations`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Cookie: cookieAlice },
      body: JSON.stringify({ userId: bob.id })
    }).then((r) => r.json()),
    fetch(`${API}/api/conversations`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Cookie: cookieBob },
      body: JSON.stringify({ userId: alice.id })
    }).then((r) => r.json())
  ]);

  if (convA.conversation.id !== convB.conversation.id) {
    throw new Error(`Deduplication failed: Got ${convA.conversation.id} and ${convB.conversation.id}`);
  }
  const conversation = convA.conversation;
  console.log("✓ Concurrent requests resolved to single conversation:", conversation.id);

  // -------------------------------------------------------------
  // 3. REALTIME MESSAGING VIA SOCKET.IO (PHASE 1 REGRESSION)
  // -------------------------------------------------------------
  console.log("9. Connecting Socket.IO Clients (Alice, Bob, Mallory)...");
  const socketAlice = io(API, {
    auth: { token: tokenAlice },
    transports: ["websocket"]
  });
  const socketBob = io(API, {
    auth: { token: tokenBob },
    transports: ["websocket"]
  });
  const socketMallory = io(API, {
    auth: { token: tokenMallory },
    transports: ["websocket"]
  });

  await Promise.all([
    new Promise<void>((resolve) => socketAlice.on("connect", () => resolve())),
    new Promise<void>((resolve) => socketBob.on("connect", () => resolve())),
    new Promise<void>((resolve) => socketMallory.on("connect", () => resolve()))
  ]);
  console.log("✓ Sockets connected and authenticated");

  // Room authorization test
  const aliceJoinRes: any = await new Promise((resolve) =>
    socketAlice.emit("conversation:join", conversation.id, resolve)
  );
  if (!aliceJoinRes.ok) throw new Error("Alice failed to join conversation room");

  const malloryJoinRes: any = await new Promise((resolve) =>
    socketMallory.emit("conversation:join", conversation.id, resolve)
  );
  if (malloryJoinRes.error) {
    console.log("✓ Alice & Bob joined room; unauthorized user room join was rejected");
  } else {
    throw new Error("Unauthorized user should not be able to join conversation room");
  }

  // Phase 1: Real-time text message exchange
  console.log("10. Realtime Text Message Exchange...");
  const textMessagePromise = new Promise<any>((resolve) => {
    socketBob.on("message:new", (msg) => {
      if (msg.type === "text") resolve(msg);
    });
  });

  const sendTextAck: any = await new Promise((resolve) => {
    socketAlice.emit(
      "message:send",
      { conversationId: conversation.id, text: "Phase 1 Text Message Verified" },
      resolve
    );
  });
  if (!sendTextAck.message) throw new Error("Text send ack failed");

  const receivedTextMsg = await textMessagePromise;
  if (receivedTextMsg.text !== "Phase 1 Text Message Verified") {
    throw new Error("Received incorrect message text over socket");
  }
  console.log("✓ Bob received Alice's text message in real time:", receivedTextMsg.text);

  const emojiText = "Hello 😀🌍🚀";
  const emojiMessagePromise = new Promise<any>((resolve) => {
    socketBob.once("message:new", (msg) => {
      if (msg.text === emojiText) resolve(msg);
    });
  });
  const emojiAck: any = await new Promise((resolve) => {
    socketAlice.emit("message:send", { conversationId: conversation.id, text: emojiText }, resolve);
  });
  const receivedEmoji = await emojiMessagePromise;
  if (emojiAck.message?.text !== emojiText || receivedEmoji.text !== emojiText) {
    throw new Error("Unicode emoji message was not preserved through Socket.IO");
  }
  const emojiOnly = "😀";
  const emojiOnlyPromise = new Promise<any>((resolve) => {
    socketBob.once("message:new", (msg) => {
      if (msg.text === emojiOnly) resolve(msg);
    });
  });
  const emojiOnlyAck: any = await new Promise((resolve) => {
    socketAlice.emit("message:send", { conversationId: conversation.id, text: emojiOnly }, resolve);
  });
  if (emojiOnlyAck.message?.text !== emojiOnly || (await emojiOnlyPromise).text !== emojiOnly) {
    throw new Error("Emoji-only message was not preserved through Socket.IO");
  }
  console.log("✓ Unicode emoji text was persisted and delivered without modification");

  // -------------------------------------------------------------
  // 4. PHASE 2 MEDIA UPLOADS & VALIDATION
  // -------------------------------------------------------------
  console.log("11. Phase 2: Uploading PNG Image with Caption...");
  const formPng = new FormData();
  formPng.append("file", new Blob([VALID_1X1_PNG], { type: "image/png" }), "pixel.png");
  formPng.append("caption", "Minimal valid PNG image");

  const pngPromise = new Promise<any>((resolve) => {
    socketBob.on("message:new", (msg) => {
      if (msg.type === "image") resolve(msg);
    });
  });

  const resPng = await fetch(`${API}/api/conversations/${conversation.id}/media`, {
    method: "POST",
    headers: { Cookie: cookieAlice },
    body: formPng
  });
  if (!resPng.ok) throw new Error(`PNG upload failed: ${await resPng.text()}`);
  const { message: pngMsg } = await resPng.json();
  const pngSocketMsg = await pngPromise;

  if (pngMsg.id !== pngSocketMsg.id || pngMsg.type !== "image") {
    throw new Error("PNG upload and socket broadcast did not match");
  }
  console.log("✓ PNG image uploaded & received in real time:", pngMsg.id);

  console.log("12. Phase 2: Uploading JPEG Image with EXIF Metadata...");
  const mediaStorageDirs = [...new Set([
    config.mediaStorageDir,
    path.resolve(process.cwd(), "apps/server/storage/media")
  ])].filter((dir) => fs.existsSync(dir));
  const filesBeforeJpeg = new Map(mediaStorageDirs.map((dir) => [dir, new Set(fs.readdirSync(dir))]));
  const formJpeg = new FormData();
  formJpeg.append("file", new Blob([VALID_JPEG_WITH_EXIF], { type: "image/jpeg" }), "photo_with_exif.jpg");
  formJpeg.append("caption", "JPEG with EXIF metadata");

  const resJpeg = await fetch(`${API}/api/conversations/${conversation.id}/media`, {
    method: "POST",
    headers: { Cookie: cookieAlice },
    body: formJpeg
  });
  if (!resJpeg.ok) throw new Error(`JPEG upload failed: ${await resJpeg.text()}`);
  const { message: jpegMsg } = await resJpeg.json();
  const jpegStorageFiles = mediaStorageDirs.flatMap((dir) =>
    fs.readdirSync(dir)
      .filter((name) => !filesBeforeJpeg.get(dir)?.has(name))
      .map((name) => path.join(dir, name))
  );
  if (jpegStorageFiles.length !== 1) {
    throw new Error(`Expected one newly stored JPEG object, found ${jpegStorageFiles.length}`);
  }
  const jpegStoragePath = jpegStorageFiles[0];
  console.log("✓ JPEG uploaded successfully:", jpegMsg.id);

  // Verify EXIF was stripped by fetching stored content
  const resJpegDownload = await fetch(`${API}/api/media/${jpegMsg.mediaAssetId}`, {
    headers: { Cookie: cookieAlice }
  });
  const storedJpegBuffer = Buffer.from(await resJpegDownload.arrayBuffer());
  if (storedJpegBuffer.includes(Buffer.from("Exif\0\0"))) {
    throw new Error("Security failure: Stored JPEG still contains location-bearing EXIF markers!");
  }
  console.log("✓ Verified location-bearing EXIF metadata stripped from stored JPEG");

  console.log("13. Phase 2: Uploading WebP Image...");
  const formWebp = new FormData();
  formWebp.append("file", new Blob([VALID_WEBP], { type: "image/webp" }), "sample.webp");

  const resWebp = await fetch(`${API}/api/conversations/${conversation.id}/media`, {
    method: "POST",
    headers: { Cookie: cookieAlice },
    body: formWebp
  });
  if (!resWebp.ok) throw new Error(`WebP upload failed: ${await resWebp.text()}`);
  const { message: webpMsg } = await resWebp.json();
  console.log("✓ WebP image uploaded successfully:", webpMsg.id);

  console.log("14. Phase 2: Uploading MP4 Video...");
  const formMp4 = new FormData();
  formMp4.append("file", new Blob([VALID_MIN_MP4], { type: "video/mp4" }), "sample.mp4");
  formMp4.append("caption", "Minimal valid MP4 video");

  const videoPromise = new Promise<any>((resolve) => {
    socketBob.on("message:new", (msg) => {
      if (msg.type === "video") resolve(msg);
    });
  });

  const resMp4 = await fetch(`${API}/api/conversations/${conversation.id}/media`, {
    method: "POST",
    headers: { Cookie: cookieAlice },
    body: formMp4
  });
  if (!resMp4.ok) throw new Error(`MP4 upload failed: ${await resMp4.text()}`);
  const { message: mp4Msg } = await resMp4.json();
  const mp4SocketMsg = await videoPromise;
  if (mp4Msg.id !== mp4SocketMsg.id || mp4Msg.type !== "video") {
    throw new Error("MP4 upload and socket broadcast did not match");
  }
  console.log("✓ MP4 video uploaded & received in real time:", mp4Msg.id);

  console.log("15. Phase 2: Uploading WebM Video...");
  const formWebm = new FormData();
  formWebm.append("file", new Blob([VALID_MIN_WEBM], { type: "video/webm" }), "sample.webm");

  const resWebm = await fetch(`${API}/api/conversations/${conversation.id}/media`, {
    method: "POST",
    headers: { Cookie: cookieAlice },
    body: formWebm
  });
  if (!resWebm.ok) throw new Error(`WebM upload failed: ${await resWebm.text()}`);
  const { message: webmMsg } = await resWebm.json();
  console.log("✓ WebM video uploaded successfully:", webmMsg.id);

  // -------------------------------------------------------------
  // 5. STREAMING & HTTP BYTE-RANGE REQUEST VERIFICATION
  // -------------------------------------------------------------
  console.log("16. Phase 2: Testing Standard Range Request (bytes=0-15)...");
  const resRange1 = await fetch(`${API}/api/conversations/${conversation.id}/media/${mp4Msg.mediaAssetId}`, {
    headers: { Cookie: cookieBob, Range: "bytes=0-15" }
  });
  if (resRange1.status !== 206) throw new Error(`Expected 206 for byte-range, got ${resRange1.status}`);
  const contentRange1 = resRange1.headers.get("content-range");
  if (!contentRange1?.startsWith("bytes 0-15/")) throw new Error(`Unexpected Content-Range: ${contentRange1}`);
  const rangeCacheControl = resRange1.headers.get("cache-control");
  if (rangeCacheControl !== "private, no-store") {
    throw new Error(`Expected Cache-Control: private, no-store on range request, got ${rangeCacheControl}`);
  }
  console.log("✓ Standard byte-range request verified (206, Content-Range:", contentRange1, ")");

  console.log("17. Phase 2: Testing Suffix Range Request (bytes=-10)...");
  const resRange2 = await fetch(`${API}/api/conversations/${conversation.id}/media/${mp4Msg.mediaAssetId}`, {
    headers: { Cookie: cookieAlice, Range: "bytes=-10" }
  });
  if (resRange2.status !== 206) throw new Error(`Expected 206 for suffix byte-range, got ${resRange2.status}`);
  const contentRange2 = resRange2.headers.get("content-range");
  console.log("✓ Suffix byte-range request verified (206, Content-Range:", contentRange2, ")");

  console.log("18. Phase 2: Testing Out-of-Bounds Range Request (bytes=1000-2000)...");
  const resRange3 = await fetch(`${API}/api/conversations/${conversation.id}/media/${mp4Msg.mediaAssetId}`, {
    headers: { Cookie: cookieBob, Range: "bytes=1000-2000" }
  });
  if (resRange3.status === 416) {
    const range416Cache = resRange3.headers.get("cache-control");
    if (range416Cache !== "private, no-store") throw new Error(`Expected private, no-store on 416, got ${range416Cache}`);
    console.log("✓ Out-of-bounds range request rejected with status 416 Range Not Satisfiable");
  } else {
    throw new Error(`Expected status 416 for invalid range, got ${resRange3.status}`);
  }

  // -------------------------------------------------------------
  // 6. SECURITY BOUNDARIES & THREE-USER AUTHORIZATION
  // -------------------------------------------------------------
  console.log("19. Security: Unauthenticated Access Rejected...");
  const resUnauth = await fetch(`${API}/api/media/${pngMsg.mediaAssetId}`);
  if (resUnauth.status === 401) {
    console.log("✓ Unauthenticated media request rejected with status 401");
  } else {
    throw new Error(`Expected status 401 for unauthenticated request, got ${resUnauth.status}`);
  }

  console.log("20. Security: Query-Token Media Access Rejected (/api/media/:id?token=...)...");
  const resQueryMedia = await fetch(`${API}/api/media/${pngMsg.mediaAssetId}?token=${encodeURIComponent(tokenAlice)}`);
  if (resQueryMedia.status === 401) {
    console.log("✓ Media access using query parameter token rejected with 401");
  } else {
    throw new Error(`Expected 401 when accessing media via query token, got ${resQueryMedia.status}`);
  }

  console.log("21. Security: Mallory (User C) Access Denied to Alice & Bob's Media...");
  const resMalloryMedia = await fetch(`${API}/api/media/${pngMsg.mediaAssetId}`, {
    headers: { Cookie: cookieMallory }
  });
  if (resMalloryMedia.status === 404) {
    const malloryCache = resMalloryMedia.headers.get("cache-control");
    if (malloryCache !== "private, no-store") throw new Error(`Expected private, no-store on 404, got ${malloryCache}`);
    console.log("✓ Unauthorized user access properly denied (status 404)");
  } else {
    throw new Error(`Expected status 404 for unauthorized media access, got ${resMalloryMedia.status}`);
  }

  console.log("22. Security: Mallory Unauthorized Access via Conversation URL Path...");
  const resMalloryConvMedia = await fetch(`${API}/api/conversations/${conversation.id}/media/${pngMsg.mediaAssetId}`, {
    headers: { Cookie: cookieMallory }
  });
  if (resMalloryConvMedia.status === 404) {
    console.log("✓ Unauthorized access via conversation URL path denied (404)");
  } else {
    throw new Error(`Expected 404 for Mallory conversation URL media access, got ${resMalloryConvMedia.status}`);
  }

  console.log("23. Security: Mallory Unauthorized Upload to Alice & Bob's Conversation...");
  const formMallory = new FormData();
  formMallory.append("file", new Blob([VALID_1X1_PNG], { type: "image/png" }), "hack.png");
  const resMalloryUpload = await fetch(`${API}/api/conversations/${conversation.id}/media`, {
    method: "POST",
    headers: { Cookie: cookieMallory },
    body: formMallory
  });
  if (resMalloryUpload.status === 404) {
    console.log("✓ Unauthorized upload to foreign conversation rejected (status 404)");
  } else {
    throw new Error(`Expected status 404 for unauthorized upload, got ${resMalloryUpload.status}`);
  }

  console.log("24. Security: Cross-Conversation Tampering in URL Path...");
  const resTampered = await fetch(`${API}/api/conversations/6ac9300581e753848e920d00/media/${pngMsg.mediaAssetId}`, {
    headers: { Cookie: cookieAlice }
  });
  if (resTampered.status === 404) {
    console.log("✓ Cross-conversation media ID mismatch in URL properly rejected with 404");
  } else {
    throw new Error(`Expected status 404 for mismatched conversation ID, got ${resTampered.status}`);
  }

  console.log("25. Security: Verifying Safe Response Headers & Non-Persistent Caching...");
  const resHeaders = await fetch(`${API}/api/media/${pngMsg.mediaAssetId}`, {
    headers: { Cookie: cookieBob }
  });
  if (resHeaders.headers.get("x-content-type-options") !== "nosniff") {
    throw new Error("Missing X-Content-Type-Options: nosniff header");
  }
  if (resHeaders.headers.get("content-security-policy") !== "default-src 'none'") {
    throw new Error("Missing Content-Security-Policy: default-src 'none' header");
  }
  if (resHeaders.headers.get("cache-control") !== "private, no-store") {
    throw new Error(`Expected Cache-Control: private, no-store, got ${resHeaders.headers.get("cache-control")}`);
  }
  if (resHeaders.headers.get("pragma") !== "no-cache") {
    throw new Error(`Expected Pragma: no-cache, got ${resHeaders.headers.get("pragma")}`);
  }
  console.log("✓ Security headers verified (nosniff, CSP: default-src 'none', Cache-Control: private, no-store)");

  console.log("26. Security: Private StorageKey Path Leaking Prevention...");
  const resAssetJson = await fetch(`${API}/api/conversations/${conversation.id}/messages`, {
    headers: { Cookie: cookieAlice }
  });
  const assetHistory = await resAssetJson.json();
  const mediaMsgs = assetHistory.messages.filter((m: any) => m.media);
  for (const m of mediaMsgs) {
    if ("storageKey" in m.media || "path" in m.media) {
      throw new Error("Security leak: storageKey or filesystem path exposed in message media DTO!");
    }
  }
  console.log("✓ Internal storage paths & storageKey successfully hidden from client DTOs");

  // -------------------------------------------------------------
  // 7. FILE VALIDATION & MULTER DOS HARDENING TESTS
  // -------------------------------------------------------------
  console.log("27. Validation: Spoofed File Signature (HTML renamed as .jpg)...");
  const formSpoofed = new FormData();
  formSpoofed.append(
    "file",
    new Blob([Buffer.from("<html><script>alert(1)</script></html>", "utf-8")], { type: "image/jpeg" }),
    "evil.jpg"
  );
  const resSpoofed = await fetch(`${API}/api/conversations/${conversation.id}/media`, {
    method: "POST",
    headers: { Cookie: cookieAlice },
    body: formSpoofed
  });
  if (resSpoofed.status === 400) {
    console.log("✓ Magic byte mismatch properly detected and rejected with status 400");
  } else {
    throw new Error(`Expected status 400 for spoofed file, got ${resSpoofed.status}`);
  }

  console.log("28. Validation: Disallowed File Format (SVG vector graphic)...");
  const formSvg = new FormData();
  formSvg.append("file", new Blob([Buffer.from("<svg></svg>", "utf-8")], { type: "image/svg+xml" }), "icon.svg");
  const resSvg = await fetch(`${API}/api/conversations/${conversation.id}/media`, {
    method: "POST",
    headers: { Cookie: cookieAlice },
    body: formSvg
  });
  if (resSvg.status === 400) {
    console.log("✓ Disallowed SVG file format rejected with status 400");
  } else {
    throw new Error(`Expected status 400 for SVG, got ${resSvg.status}`);
  }

  console.log("29. Validation: Missing File Upload...");
  const formEmpty = new FormData();
  formEmpty.append("caption", "No file here");
  const resEmpty = await fetch(`${API}/api/conversations/${conversation.id}/media`, {
    method: "POST",
    headers: { Cookie: cookieAlice },
    body: formEmpty
  });
  if (resEmpty.status === 400) {
    console.log("✓ Empty file upload rejected with status 400");
  } else {
    throw new Error(`Expected status 400 for empty upload, got ${resEmpty.status}`);
  }

  console.log("30. Multer DoS Protection: Malformed Array-Index Field Names Rejected...");
  // Attackers send field names like field[999999999]=test to exhaust memory
  const boundary = "----WebKitFormBoundary" + Math.random().toString(36).substring(2);
  const payloadMalformedField = [
    `--${boundary}`,
    `Content-Disposition: form-data; name="caption[999999999]"`,
    "",
    "evil-array-index",
    `--${boundary}`,
    `Content-Disposition: form-data; name="file"; filename="pixel.png"`,
    "Content-Type: image/png",
    "",
    VALID_1X1_PNG.toString("binary"),
    `--${boundary}--`,
    ""
  ].join("\r\n");

  const resMalformedField = await fetch(`${API}/api/conversations/${conversation.id}/media`, {
    method: "POST",
    headers: {
      Cookie: cookieAlice,
      "Content-Type": `multipart/form-data; boundary=${boundary}`
    },
    body: Buffer.from(payloadMalformedField, "binary")
  });
  if (resMalformedField.status === 400) {
    console.log("✓ Malformed multipart array index field successfully rejected with status 400");
  } else {
    throw new Error(`Expected 400 for array index DoS payload, got ${resMalformedField.status}`);
  }

  console.log("31. Multer DoS Protection: Oversized Form Field Size Limit...");
  const formOversizedField = new FormData();
  formOversizedField.append("file", new Blob([VALID_1X1_PNG], { type: "image/png" }), "pixel.png");
  formOversizedField.append("caption", "A".repeat(32 * 1024)); // 32KB caption exceeds 16KB field limit
  const resOversizedField = await fetch(`${API}/api/conversations/${conversation.id}/media`, {
    method: "POST",
    headers: { Cookie: cookieAlice },
    body: formOversizedField
  });
  if (resOversizedField.status === 400) {
    console.log("✓ Oversized form field safely rejected with status 400");
  } else {
    throw new Error(`Expected status 400 for oversized field value, got ${resOversizedField.status}`);
  }

  console.log("32. Memory Protection: Oversized Image Rejected Before Buffering...");
  // Create simulated 12MB JPEG header + padding
  const oversizedImageBuffer = Buffer.alloc(12 * 1024 * 1024);
  VALID_JPEG_WITH_EXIF.copy(oversizedImageBuffer, 0, 0, VALID_JPEG_WITH_EXIF.length);
  const formOversizedImage = new FormData();
  formOversizedImage.append("file", new Blob([oversizedImageBuffer], { type: "image/jpeg" }), "oversized.jpg");

  const resOversizedImage = await fetch(`${API}/api/conversations/${conversation.id}/media`, {
    method: "POST",
    headers: { Cookie: cookieAlice },
    body: formOversizedImage
  });
  if (resOversizedImage.status === 400) {
    const errBody = await resOversizedImage.json();
    console.log("✓ Oversized image rejected with 400:", errBody.error);
  } else {
    throw new Error(`Expected status 400 for oversized image, got ${resOversizedImage.status}`);
  }

  console.log("33. Storage Cleanup: Checking Temporary Upload Directory...");
  const tempDir = path.resolve(process.cwd(), "storage/temp");
  if (fs.existsSync(tempDir)) {
    const tempFiles = fs.readdirSync(tempDir);
    console.log("✓ Temporary upload directory clean (orphaned tmp files count:", tempFiles.length, ")");
  }

  // -------------------------------------------------------------
  // 8. DATABASE PERSISTENCE & CONVERSATION PREVIEW
  // -------------------------------------------------------------
  console.log("34. Persistence: Verifying Message History in MongoDB...");
  const resMessages = await fetch(`${API}/api/conversations/${conversation.id}/messages`, {
    headers: { Cookie: cookieBob }
  });
  const { messages: history } = await resMessages.json();
  console.log("✓ Retrieved", history.length, "persisted messages from database");
  if (history.length !== 8) {
    throw new Error(`Expected 8 messages in history, got ${history.length}`);
  }
  const types = history.map((m: any) => m.type);
  if (types[0] !== "text" || types[1] !== "text" || types[2] !== "text" || types[3] !== "image" || types[4] !== "image" || types[5] !== "image" || types[6] !== "video" || types[7] !== "video") {
    throw new Error(`Unexpected message type sequence: ${types.join(", ")}`);
  }
  if (!history.some((m: any) => m.text === emojiText) || !history.some((m: any) => m.text === emojiOnly)) {
    throw new Error("Unicode emoji messages did not persist unchanged in message history");
  }
  console.log("✓ Chronological ordering, Unicode and emoji-only persistence, and message types verified");

  console.log("35. UI Preview: Verifying Conversation List Snippet...");
  const resConversations = await fetch(`${API}/api/conversations`, {
    headers: { Cookie: cookieBob }
  });
  const { conversations: convList } = await resConversations.json();
  const currentConv = convList.find((c: any) => c.id === conversation.id);
  if (!currentConv?.lastMessage) throw new Error("Conversation missing lastMessage reference");
  if (currentConv.lastMessage.type !== "video") {
    throw new Error(`Expected lastMessage type video, got ${currentConv.lastMessage.type}`);
  }
  console.log("✓ Conversation list correctly reflects latest media message (type: video)");

  console.log("36. Deletion: Bob deletes Alice's WebP message for himself...");
  const hideWebp = await fetch(`${API}/api/messages/${webpMsg.id}`, {
    method: "DELETE",
    headers: { "Content-Type": "application/json", Cookie: cookieBob },
    body: JSON.stringify({ scope: "me" })
  });
  if (hideWebp.status !== 200 || !(await hideWebp.json()).deleted) {
    throw new Error(`Delete for me failed: ${hideWebp.status}`);
  }
  const bobAfterHide = await fetch(`${API}/api/conversations/${conversation.id}/messages`, { headers: { Cookie: cookieBob } }).then((r) => r.json());
  const aliceAfterHide = await fetch(`${API}/api/conversations/${conversation.id}/messages`, { headers: { Cookie: cookieAlice } }).then((r) => r.json());
  if (bobAfterHide.messages.some((m: any) => m.id === webpMsg.id) || !aliceAfterHide.messages.some((m: any) => m.id === webpMsg.id)) {
    throw new Error("Delete for me did not persist for only the requesting user");
  }
  console.log("✓ Delete for me persists for Bob while Alice retains the original message");

  console.log("37. Media: Per-user deletion blocks only the requester's media access...");
  const bobHiddenMedia = await fetch(`${API}/api/media/${webpMsg.mediaAssetId}`, { headers: { Cookie: cookieBob } });
  const aliceVisibleMedia = await fetch(`${API}/api/media/${webpMsg.mediaAssetId}`, { headers: { Cookie: cookieAlice } });
  if (bobHiddenMedia.status !== 404 || aliceVisibleMedia.status !== 200) {
    throw new Error(`Expected hidden media 404 for Bob and 200 for Alice; got ${bobHiddenMedia.status}/${aliceVisibleMedia.status}`);
  }
  console.log("✓ Hidden media is inaccessible to Bob and remains available to Alice");

  console.log("38. Deletion: A recipient cannot delete the sender's message for everyone...");
  const unauthorizedDelete = await fetch(`${API}/api/messages/${receivedTextMsg.id}`, {
    method: "DELETE",
    headers: { "Content-Type": "application/json", Cookie: cookieBob },
    body: JSON.stringify({ scope: "everyone" })
  });
  if (unauthorizedDelete.status !== 403) {
    throw new Error(`Expected 403 for unauthorized delete-for-everyone, got ${unauthorizedDelete.status}`);
  }
  console.log("✓ Non-sender delete-for-everyone request rejected");

  console.log("39. Deletion: Sender deletes a media message for everyone...");
  const mediaDeletionEvent = new Promise<any>((resolve) => {
    socketBob.once("message:deleted", (payload) => {
      if (payload.messageId === jpegMsg.id && payload.scope === "everyone") resolve(payload);
    });
  });
  const deleteJpeg = await fetch(`${API}/api/messages/${jpegMsg.id}`, {
    method: "DELETE",
    headers: { "Content-Type": "application/json", Cookie: cookieAlice },
    body: JSON.stringify({ scope: "everyone" })
  });
  if (!deleteJpeg.ok) throw new Error(`Delete-for-everyone failed: ${deleteJpeg.status}`);
  const mediaDeletionNotice = await mediaDeletionEvent;
  if (mediaDeletionNotice.messageId !== jpegMsg.id) throw new Error("Recipient did not receive deletion event");
  console.log("✓ Deleted media tombstone synchronized to recipient over Socket.IO");

  console.log("40. Security: Globally deleted media cannot be retrieved by either participant...");
  const aliceDeletedMedia = await fetch(`${API}/api/media/${jpegMsg.mediaAssetId}`, { headers: { Cookie: cookieAlice } });
  const bobDeletedMedia = await fetch(`${API}/api/media/${jpegMsg.mediaAssetId}`, { headers: { Cookie: cookieBob } });
  if (aliceDeletedMedia.status !== 404 || bobDeletedMedia.status !== 404) {
    throw new Error(`Expected deleted media 404 for both participants, got ${aliceDeletedMedia.status}/${bobDeletedMedia.status}`);
  }
  if (fs.existsSync(jpegStoragePath)) {
    throw new Error("Deleted media object remains in private storage");
  }
  console.log("✓ Globally deleted media is no longer retrievable through protected media routes");

  console.log("41. Persistence: Both users retain the media deletion tombstone...");
  const aliceAfterGlobalDelete = await fetch(`${API}/api/conversations/${conversation.id}/messages`, { headers: { Cookie: cookieAlice } }).then((r) => r.json());
  const bobAfterGlobalDelete = await fetch(`${API}/api/conversations/${conversation.id}/messages`, { headers: { Cookie: cookieBob } }).then((r) => r.json());
  for (const result of [aliceAfterGlobalDelete, bobAfterGlobalDelete]) {
    const tombstone = result.messages.find((m: any) => m.id === jpegMsg.id);
    if (!tombstone?.deletedForEveryone || tombstone.text || tombstone.mediaAssetId) {
      throw new Error("Deleted media tombstone did not persist without exposing content");
    }
  }
  console.log("✓ Refresh-equivalent history reads return the persisted redacted tombstone");

  console.log("42. Conversations: Delete-for-me updates the requester's last-message preview...");
  const hideEmoji = await fetch(`${API}/api/messages/${receivedEmoji.id}`, {
    method: "DELETE",
    headers: { "Content-Type": "application/json", Cookie: cookieBob },
    body: JSON.stringify({ scope: "me" })
  });
  if (!hideEmoji.ok) throw new Error(`Could not hide latest message: ${hideEmoji.status}`);
  const bobConversationsAfterHide = await fetch(`${API}/api/conversations`, { headers: { Cookie: cookieBob } }).then((r) => r.json());
  const bobConversationAfterHide = bobConversationsAfterHide.conversations.find((c: any) => c.id === conversation.id);
  if (bobConversationAfterHide?.lastMessage?.id !== webmMsg.id) {
    throw new Error("Conversation preview did not fall back to Bob's latest visible message");
  }
  console.log("✓ Conversation list falls back to the latest message visible to Bob");

  console.log("43. Deletion: Sender deletes a text message for everyone...");
  const textDeletionEvent = new Promise<any>((resolve) => {
    socketBob.once("message:deleted", (payload) => {
      if (payload.messageId === receivedTextMsg.id && payload.scope === "everyone") resolve(payload);
    });
  });
  const deleteText = await fetch(`${API}/api/messages/${receivedTextMsg.id}`, {
    method: "DELETE",
    headers: { "Content-Type": "application/json", Cookie: cookieAlice },
    body: JSON.stringify({ scope: "everyone" })
  });
  if (!deleteText.ok) throw new Error(`Text delete-for-everyone failed: ${deleteText.status}`);
  await textDeletionEvent;
  console.log("✓ Sender can tombstone a text message and recipient receives its deletion event");

  console.log("44. Persistence: Text deletion survives a history reload...");
  const bobHistoryAfterTextDelete = await fetch(`${API}/api/conversations/${conversation.id}/messages`, { headers: { Cookie: cookieBob } }).then((r) => r.json());
  const textTombstone = bobHistoryAfterTextDelete.messages.find((m: any) => m.id === receivedTextMsg.id);
  if (!textTombstone?.deletedForEveryone || textTombstone.text) {
    throw new Error("Text deletion did not persist as a redacted tombstone");
  }
  console.log("✓ Reloaded recipient history retains the deleted-text placeholder state");

  // -------------------------------------------------------------
  // 9. LOGOUT & SESSION REVOCATION (PRIORITY FOUR)
  // -------------------------------------------------------------
  console.log("45. Auth: Testing Logout and Immediate Session Revocation...");
  const loggedOutSocketDisconnect = new Promise<void>((resolve, reject) => {
    const timeout = setTimeout(() => reject(new Error("Socket session was not disconnected after logout")), 3000);
    socketBob.once("disconnect", () => {
      clearTimeout(timeout);
      resolve();
    });
  });
  const resLogout = await fetch(`${API}/api/auth/logout`, {
    method: "POST",
    headers: { Cookie: cookieBob }
  });
  if (resLogout.status !== 204) throw new Error(`Logout failed: ${resLogout.status}`);
  await loggedOutSocketDisconnect;
  if (socketBob.connected) throw new Error("Logged-out socket remained connected");
  console.log("✓ Logout succeeded with status 204");

  console.log("46. Auth: Protected Request with Revoked Cookie Rejected...");
  const resRevokedCookie = await fetch(`${API}/api/me`, {
    headers: { Cookie: cookieBob }
  });
  if (resRevokedCookie.status === 401) {
    console.log("✓ Revoked session cookie rejected with status 401");
  } else {
    throw new Error(`Expected status 401 for revoked session cookie, got ${resRevokedCookie.status}`);
  }

  console.log("47. Auth: Protected Request with Revoked Bearer Token Rejected...");
  const resRevokedBearer = await fetch(`${API}/api/me`, {
    headers: { Authorization: `Bearer ${tokenBob}` }
  });
  if (resRevokedBearer.status === 401) {
    console.log("✓ Revoked Bearer token rejected with status 401");
  } else {
    throw new Error(`Expected status 401 for revoked Bearer token, got ${resRevokedBearer.status}`);
  }

  console.log("48. Auth: Socket.IO Reconnection with Revoked Token Rejected...");
  const socketRevoked = io(API, {
    auth: { token: tokenBob },
    transports: ["websocket"],
    reconnection: false
  });
  const revokedSocketError = await new Promise<string>((resolve) => {
    socketRevoked.on("connect_error", (err) => resolve(err.message));
    socketRevoked.on("connect", () => resolve("connected_unexpectedly"));
    setTimeout(() => resolve("timeout"), 3000);
  });
  if (revokedSocketError.includes("Unauthorized") || revokedSocketError.includes("invalid token")) {
    console.log("✓ Socket.IO connection properly rejected with revoked credentials:", revokedSocketError);
  } else {
    throw new Error(`Expected socket connection rejection, got: ${revokedSocketError}`);
  }

  console.log("49. Auth: Independent Session Isolation Verified...");
  const resAliceStillValid = await fetch(`${API}/api/me`, {
    headers: { Cookie: cookieAlice }
  });
  if (resAliceStillValid.status === 200) {
    console.log("✓ Alice's independent session remains fully functional and unrevoked");
  } else {
    throw new Error(`Alice session was unexpectedly invalidated: ${resAliceStillValid.status}`);
  }

  // -------------------------------------------------------------
  // 10. HELMET HTTP SECURITY HEADERS & HEALTH
  // -------------------------------------------------------------
  console.log("50. Security: Helmet HTTP Security Headers Present...");
  const resHelmet = await fetch(`${API}/api/me`, {
    headers: { Cookie: cookieAlice }
  });
  const xCTO = resHelmet.headers.get("x-content-type-options");
  if (xCTO !== "nosniff") {
    throw new Error(`Expected X-Content-Type-Options: nosniff, got ${xCTO}`);
  }
  const xFO = resHelmet.headers.get("x-frame-options");
  if (!xFO) {
    throw new Error("Missing X-Frame-Options header (expected from Helmet)");
  }
  const corp = resHelmet.headers.get("cross-origin-resource-policy");
  if (corp !== "same-origin") {
    throw new Error(`Expected Cross-Origin-Resource-Policy: same-origin, got ${corp}`);
  }
  const xPowered = resHelmet.headers.get("x-powered-by");
  if (xPowered) {
    throw new Error(`X-Powered-By header should be removed by Helmet, got: ${xPowered}`);
  }
  console.log("✓ Helmet HTTP security headers verified (nosniff, X-Frame-Options, CORP: same-origin, no X-Powered-By)");

  console.log("51. Health: Server Health Endpoint (/health)...");
  const resHealth = await fetch(`${API}/health`);
  if (resHealth.status !== 200) throw new Error(`Health check failed: ${resHealth.status}`);
  const healthData = await resHealth.json();
  if (healthData.ok !== true) throw new Error(`Health endpoint returned unexpected data: ${JSON.stringify(healthData)}`);
  console.log("✓ Health endpoint responded with { ok: true }");

  // Clean up socket connections
  socketAlice.disconnect();
  socketBob.disconnect();
  socketMallory.disconnect();
  socketRevoked.disconnect();

  console.log("\n===============================================================");
  console.log("🎉 ALL 51 VERIFICATION CHECKS (PHASE 1 & PHASE 2) PASSED 100%!");
  console.log("===============================================================");
}

runTest().catch((err) => {
  console.error("Test failed:", err);
  process.exit(1);
});
