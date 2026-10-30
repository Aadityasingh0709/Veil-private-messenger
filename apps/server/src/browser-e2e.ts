import { chromium, type Browser, type Page } from "playwright";
import path from "node:path";
import fs from "node:fs";

const BASE_URL = process.env.CLIENT_URL ?? "http://localhost:5173";
const API_URL = process.env.API_URL ?? "http://localhost:4000";

const FIXTURES_DIR = path.resolve(process.cwd(), "test-fixtures");
const SAMPLE_PNG = path.join(FIXTURES_DIR, "sample.png");
const SAMPLE_WEBM = path.join(FIXTURES_DIR, "sample.webm");

async function wait(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function registerAndLogin(page: Page, name: string, username: string, email: string) {
  await page.goto(`${BASE_URL}/register`);
  await page.fill('input[name="name"]', name);
  await page.fill('input[name="username"]', username);
  await page.fill('input[name="email"]', email);
  await page.fill('input[name="password"]', "Password123!");
  await page.click('button[type="submit"]');

  // Wait for navigation to chat home
  await page.waitForURL(`${BASE_URL}/`, { timeout: 10000 });
  await page.waitForSelector(".sidebar", { timeout: 10000 });
}

async function startConversationWith(page: Page, targetUsername: string) {
  const searchInput = page.locator(".search-input");
  await searchInput.fill(targetUsername);
  // Wait for search result item
  const resultItem = page.locator(".search-result-item", { hasText: targetUsername });
  await resultItem.waitFor({ state: "visible", timeout: 10000 });
  await resultItem.click();
  // Wait for conversation view to load
  await page.locator(".messages-viewport").waitFor({ state: "visible", timeout: 10000 });
}

async function runBrowserTests() {
  console.log("===============================================================");
  console.log("=== PHASE 2 REAL BROWSER (PLAYWRIGHT) VERIFICATION SUITE   ===");
  console.log("===============================================================\n");

  if (!fs.existsSync(SAMPLE_PNG)) {
    throw new Error(`Sample PNG fixture not found at: ${SAMPLE_PNG}`);
  }
  if (!fs.existsSync(SAMPLE_WEBM)) {
    throw new Error(`Sample WebM fixture not found at: ${SAMPLE_WEBM}`);
  }

  const browser: Browser = await chromium.launch({
    headless: true,
    args: ["--no-sandbox", "--disable-setuid-sandbox", "--use-fake-ui-for-media-stream"]
  });

  const timestamp = Date.now();
  const aliceName = `Alice Browser ${timestamp}`;
  const aliceUsername = `alice_b_${timestamp}`;
  const aliceEmail = `alice_b_${timestamp}@test.com`;

  const bobName = `Bob Browser ${timestamp}`;
  const bobUsername = `bob_b_${timestamp}`;
  const bobEmail = `bob_b_${timestamp}@test.com`;

  try {
    // Create two separate browser contexts (separate cookies/sessions)
    const contextA = await browser.newContext();
    const contextB = await browser.newContext();

    const pageA = await contextA.newPage();
    const pageB = await contextB.newPage();

    pageA.on("response", (res) => { if (res.status() >= 400) console.log("[Browser A HTTP Err]", res.url(), res.status()); });
    pageB.on("response", (res) => { if (res.status() >= 400) console.log("[Browser B HTTP Err]", res.url(), res.status()); });

    console.log("1. Registering Alice and Bob in separate browser contexts...");
    await registerAndLogin(pageA, aliceName, aliceUsername, aliceEmail);
    console.log("✓ Alice registered and logged in on Context A");

    await registerAndLogin(pageB, bobName, bobUsername, bobEmail);
    console.log("✓ Bob registered and logged in on Context B");

    console.log("\n2. Setting up one-to-one conversation from Alice to Bob...");
    await startConversationWith(pageA, bobUsername);
    console.log("✓ Alice started conversation with Bob");

    // -----------------------------------------------------------------------
    // BROWSER TEST D: PHASE 1 REGRESSION (REAL-TIME TEXT CHAT SYNCHRONIZATION)
    // -----------------------------------------------------------------------
    console.log("\n3. [Browser Test D] Testing Phase 1 text messaging & real-time sync...");
    const testMessageText = `Hello Bob! Realtime text verification ${timestamp}`;
    const composerInputA = pageA.locator(".composer-input");
    await composerInputA.fill(testMessageText);
    await pageA.locator(".composer-send-btn").click();

    // Verify message appears in Alice's view
    await pageA.locator(".bubble-text", { hasText: testMessageText }).waitFor({ timeout: 5000 });
    console.log("✓ Alice's message rendered in Alice's chat view");

    // Bob opens conversation with Alice if not already open
    await startConversationWith(pageB, aliceUsername);

    // Verify Bob receives Alice's message in real time
    await pageB.locator(".bubble-text", { hasText: testMessageText }).waitFor({ timeout: 8000 });
    console.log("✓ Bob received Alice's message in real-time via Socket.IO");

    // Bob replies
    const replyText = `Hi Alice, received your text! ${timestamp}`;
    await pageB.locator(".composer-input").fill(replyText);
    await pageB.locator(".composer-send-btn").click();

    // Alice receives Bob's reply
    await pageA.locator(".bubble-text", { hasText: replyText }).waitFor({ timeout: 8000 });
    console.log("✓ Alice received Bob's reply in real-time");

    console.log("\n4. [Browser Test E] Testing emoji selection and message deletion...");
    const composerInput = pageA.locator(".composer-input");
    await composerInput.fill("Keep this text");
    await composerInput.evaluate((input: HTMLInputElement) => input.setSelectionRange(5, 5));
    await pageA.locator(".btn-emoji").click();
    const emojiSearch = pageA.getByRole("textbox", { name: "Search emojis" });
    await emojiSearch.fill("grinning face");
    const emojiResult = pageA.locator('.EmojiPickerReact button[aria-label="grinning face"]').first();
    await emojiResult.waitFor({ state: "visible", timeout: 5000 });
    await emojiResult.click();
    const insertedText = await composerInput.inputValue();
    const insertedCaret = await composerInput.evaluate((input: HTMLInputElement) => input.selectionStart);
    if (insertedText !== "Keep 😀this text" || insertedCaret !== 7) {
      throw new Error(`Emoji insertion did not preserve the unsent text or cursor position (got "${insertedText}")`);
    }
    await pageA.locator(".btn-emoji").click();
    await pageA.getByRole("textbox", { name: "Search emojis" }).fill("");
    await pageA.locator('.EmojiPickerReact button[aria-label="Flags"]').click();
    await pageA.locator('.EmojiPickerReact button[aria-label*="flag"]').first().waitFor({ state: "visible", timeout: 5000 });
    await pageA.locator(".btn-emoji").click();
    await pageA.locator(".composer-send-btn").click();
    await pageB.locator(".bubble-text", { hasText: "Keep 😀this text" }).waitFor({ timeout: 8000 });
    await composerInput.fill("😀");
    await pageA.locator(".composer-send-btn").click();
    await pageB.locator(".bubble-text", { hasText: /^😀$/ }).waitFor({ timeout: 8000 });
    console.log("✓ Emoji search, category navigation, text-preserving insertion, send and realtime delivery work");

    const replyRowA = pageA.locator(".message-row").filter({ hasText: replyText });
    await replyRowA.getByRole("button", { name: "Message actions" }).click();
    await pageA.getByRole("menuitem", { name: "Delete for me" }).click();
    await replyRowA.waitFor({ state: "detached", timeout: 5000 });
    await pageB.locator(".bubble-text", { hasText: replyText }).waitFor({ state: "visible", timeout: 3000 });
    console.log("✓ Delete for me removes the message only from the requesting browser");

    const originalRowA = pageA.locator(".message-row").filter({ hasText: testMessageText });
    pageA.once("dialog", (dialog) => dialog.accept());
    await originalRowA.getByRole("button", { name: "Message actions" }).click();
    await pageA.getByRole("menuitem", { name: "Delete for everyone" }).click();
    await pageA.locator(".bubble-deleted", { hasText: "This message was deleted." }).waitFor({ state: "visible", timeout: 5000 });
    await pageB.locator(".bubble-deleted", { hasText: "This message was deleted." }).waitFor({ state: "visible", timeout: 5000 });
    await pageB.reload();
    await pageB.waitForSelector(".sidebar", { timeout: 10000 });
    await startConversationWith(pageB, aliceUsername);
    await pageB.locator(".bubble-deleted").waitFor({ state: "visible", timeout: 10000 });
    console.log("✓ Delete for everyone synchronizes to both browsers and persists after refresh");

    // -----------------------------------------------------------------------
    // BROWSER TEST A: IMAGE WORKFLOW (UPLOAD, RENDER, CUSTOM VIEWER, PERSIST)
    // -----------------------------------------------------------------------
    console.log("\n5. [Browser Test A] Testing Image upload & custom viewer workflow...");
    // Add caption first, then select file
    const imageCaption = "Photo attachment test";
    await composerInputA.fill(imageCaption);

    const fileChooserPromiseA = pageA.waitForEvent("filechooser");
    await pageA.locator(".btn-attach").click();
    const fileChooserA = await fileChooserPromiseA;
    await fileChooserA.setFiles(SAMPLE_PNG);

    // Verify message appears in Alice's view with image preview
    const imageBubbleA = pageA.locator(".bubble-media-wrapper").first();
    await imageBubbleA.waitFor({ state: "visible", timeout: 15000 });
    console.log("✓ Image preview bubble rendered in sender's view");

    // Bob receives image message in real time
    const imageBubbleB = pageB.locator(".bubble-media-wrapper").first();
    await imageBubbleB.waitFor({ state: "visible", timeout: 15000 });
    console.log("✓ Image preview bubble received in recipient's view");

    // Open image in Custom Media Viewer
    await imageBubbleB.click();
    const viewerModal = pageB.locator(".media-viewer-backdrop");
    await viewerModal.waitFor({ state: "visible", timeout: 5000 });
    console.log("✓ Media viewer modal opened on click");

    // Verify custom viewer renders image
    const viewerImg = pageB.locator(".media-viewer-image");
    await viewerImg.waitFor({ state: "visible", timeout: 8000 });
    const isImgNatural = await viewerImg.evaluate((el: HTMLImageElement) => el.complete && el.naturalWidth > 0);
    if (!isImgNatural) {
      throw new Error("Image did not load successfully in viewer (naturalWidth is 0)");
    }
    console.log("✓ Real image loaded and rendered in custom viewer");

    // Close viewer via close button
    await pageB.locator(".media-viewer-close-btn").click();
    await viewerModal.waitFor({ state: "hidden", timeout: 3000 });
    console.log("✓ Custom viewer closed cleanly");

    // Refresh page and confirm message persists
    await pageB.reload();
    await pageB.waitForSelector(".sidebar", { timeout: 10000 });
    await startConversationWith(pageB, aliceUsername);
    await pageB.locator(".bubble-media-wrapper").first().waitFor({ state: "visible", timeout: 10000 });
    console.log("✓ Image message persisted after page refresh");

    // -----------------------------------------------------------------------
    // BROWSER TEST B: VIDEO WORKFLOW (UPLOAD, PLAYBACK, SEEK, CLOSE, PERSIST)
    // -----------------------------------------------------------------------
    console.log("\n6. [Browser Test B] Testing Video upload & playback workflow...");
    const fileChooserVideoPromise = pageA.waitForEvent("filechooser");
    await pageA.locator(".btn-attach").click();
    const fileChooserVideo = await fileChooserVideoPromise;
    await fileChooserVideo.setFiles(SAMPLE_WEBM);

    // Verify video bubble appears on Bob's side
    const videoBubbleB = pageB.locator(".bubble-video-wrapper").first();
    await videoBubbleB.waitFor({ state: "visible", timeout: 25000 });
    console.log("✓ Video message received by Bob in real-time");

    // Click to open video in custom media viewer
    await videoBubbleB.click();
    await viewerModal.waitFor({ state: "visible", timeout: 5000 });

    const viewerVideo = pageB.locator(".media-viewer-video");
    await viewerVideo.waitFor({ state: "visible", timeout: 10000 });

    // Verify video metadata loaded
    const hasMetadata = await viewerVideo.evaluate((el: HTMLVideoElement) => {
      return new Promise<boolean>((resolve) => {
        if (el.readyState >= 1) return resolve(true);
        el.onloadedmetadata = () => resolve(true);
        setTimeout(() => resolve(el.readyState >= 1), 5000);
      });
    });
    console.log("✓ Video metadata loaded successfully (readyState >= 1):", hasMetadata);
    if (!hasMetadata) throw new Error("Video metadata did not load");

    // Verify video playback advances
    await viewerVideo.evaluate((el: HTMLVideoElement) => {
      el.muted = true;
      return el.play();
    });
    await pageB.waitForFunction(() => {
      const video = document.querySelector<HTMLVideoElement>(".media-viewer-video");
      return Boolean(video && video.currentTime > 0.1 && !video.paused);
    }, undefined, { timeout: 5000 });
    const currentTime = await viewerVideo.evaluate((el: HTMLVideoElement) => el.currentTime);
    console.log("✓ Video playback started, currentTime:", currentTime.toFixed(2), "s");
    if (currentTime <= 0.1) {
      throw new Error("Video playback did not advance in the browser");
    }

    // Verify video seeking
    await viewerVideo.evaluate((el: HTMLVideoElement) => {
      el.currentTime = 1.0;
    });
    await pageB.waitForFunction(() => {
      const video = document.querySelector<HTMLVideoElement>(".media-viewer-video");
      return Boolean(video && !video.seeking && Math.abs(video.currentTime - 1.0) < 0.2);
    }, undefined, { timeout: 5000 });
    const seekTime = await viewerVideo.evaluate((el: HTMLVideoElement) => el.currentTime);
    console.log("✓ Video seeking verified, new time:", seekTime.toFixed(2), "s");
    if (Math.abs(seekTime - 1.0) > 0.2) throw new Error(`Video seek landed at ${seekTime}, expected near 1.0`);

    // Close viewer via Escape key
    await viewerVideo.evaluate((el: HTMLVideoElement) => {
      (window as Window & { closingVideo?: HTMLVideoElement }).closingVideo = el;
    });
    await pageB.keyboard.press("Escape");
    await viewerModal.waitFor({ state: "hidden", timeout: 3000 });
    const pauseOnClose = await pageB.evaluate(() =>
      (window as Window & { closingVideo?: HTMLVideoElement }).closingVideo?.paused ?? false
    );
    if (!pauseOnClose) throw new Error("Closing the viewer did not pause video playback");
    console.log("✓ Video viewer closed via Escape key and video playback halted");

    // Refresh and confirm video message persists
    await pageB.reload();
    await pageB.waitForSelector(".sidebar", { timeout: 10000 });
    await startConversationWith(pageB, aliceUsername);
    await pageB.locator(".bubble-video-wrapper").first().waitFor({ state: "visible", timeout: 10000 });
    console.log("✓ Video message persisted after page refresh");

    // -----------------------------------------------------------------------
    // BROWSER TEST C: ERRORS AND EDGE CASES
    // -----------------------------------------------------------------------
    console.log("\n7. [Browser Test C] Testing UI errors, validations & edge cases...");

    // 1. Unsupported format (attempting to upload a .txt file)
    const dummyTxtPath = path.join(FIXTURES_DIR, "invalid.txt");
    fs.writeFileSync(dummyTxtPath, "Plain text content not allowed");

    const fileChooserInvalidPromise = pageA.waitForEvent("filechooser");
    await pageA.locator(".btn-attach").click();
    const fileChooserInvalid = await fileChooserInvalidPromise;
    await fileChooserInvalid.setFiles(dummyTxtPath);

    // Client-side validation should display error banner
    const errorBanner = pageA.locator(".error-banner");
    await errorBanner.waitFor({ state: "visible", timeout: 5000 });
    const errorText = await errorBanner.textContent();
    console.log("✓ Unsupported format (.txt) rejected with client feedback:", errorText?.trim());
    await pageA.locator(".error-banner-close").click();

    // 2. Mobile-width viewport test
    await pageA.setViewportSize({ width: 375, height: 667 });
    await wait(300);
    const isComposerVisibleMobile = await pageA.locator(".composer-container").isVisible();
    console.log("✓ Mobile viewport (375x667) rendered cleanly, composer visible:", isComposerVisibleMobile);
    await pageA.locator(".btn-emoji").click();
    const pickerBox = await pageA.locator(".emoji-picker-popover").boundingBox();
    if (!pickerBox || pickerBox.x < 0 || pickerBox.x + pickerBox.width > 375) {
      throw new Error("Emoji picker does not fit within the mobile viewport");
    }
    await pageA.locator(".btn-emoji").click();
    if (await pageA.locator(".emoji-picker-popover").count()) {
      throw new Error("Emoji picker close action did not dismiss the picker");
    }
    console.log("✓ Emoji picker remains within the mobile viewport and its close button works");

    // Clean up temporary files
    try {
      fs.unlinkSync(dummyTxtPath);
    } catch {}

    console.log("\n===============================================================");
    console.log("🎉 ALL REAL BROWSER PLAYWRIGHT TESTS PASSED (A, B, C, D) 100%!");
    console.log("===============================================================");
  } finally {
    await browser.close();
  }
}

runBrowserTests().catch((err) => {
  console.error("\n❌ Browser verification test failed:", err);
  process.exit(1);
});
