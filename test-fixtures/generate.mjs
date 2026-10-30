import { spawnSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { chromium } from "playwright";

const fixturesDir = path.resolve("test-fixtures");
const outputPath = path.join(fixturesDir, "sample.webm");
const ffmpeg = process.env.FFMPEG_PATH;

if (!ffmpeg) {
  throw new Error("Set FFMPEG_PATH to an FFmpeg build with MJPEG decoding and libvpx-vp9 encoding enabled.");
}

if (!fs.existsSync(fixturesDir)) fs.mkdirSync(fixturesDir, { recursive: true });

const browser = await chromium.launch({ headless: true });
let jpegFrames;
try {
  const page = await browser.newPage({ viewport: { width: 320, height: 240 } });
  await page.setContent(
    '<html><body style="margin:0"><div style="width:320px;height:240px;background:linear-gradient(90deg,#2563eb,#f97316)"></div></body></html>'
  );
  const frame = Buffer.from(await page.locator("body").screenshot({ type: "jpeg", quality: 80 }));
  jpegFrames = Buffer.concat(Array.from({ length: 60 }, () => frame));
} finally {
  await browser.close();
}

const result = spawnSync(ffmpeg, [
  "-y",
  "-f", "image2pipe",
  "-framerate", "30",
  "-vcodec", "mjpeg",
  "-i", "pipe:0",
  "-t", "2",
  "-c:v", "libvpx-vp9",
  "-pix_fmt", "yuv420p",
  "-f", "webm",
  outputPath
], { input: jpegFrames, encoding: "utf8", maxBuffer: 1024 * 1024 });

if (result.error || result.status !== 0) {
  throw new Error(`FFmpeg failed to generate the WebM fixture: ${result.error?.message ?? result.stderr}`);
}

console.log(`Generated playable WebM fixture: ${outputPath} (${fs.statSync(outputPath).size} bytes)`);
