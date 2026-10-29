import fs from "node:fs";
import path from "node:path";
import { execFileSync } from "node:child_process";

const dir = path.resolve("test-fixtures");
if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });

// 1. Generate real valid PNG (32x32)
const pngHex = "89504e470d0a1a0a0000000d4948445200000020000000200802000000fc18edaf00000057494441589c63601805a360148c8251300a4641281805a360148c8251300a4641281805a360148c8251300a4641281805a360148c8251300a4641281805a360148c8251300a4641281805a360148c8251300a464128000099ab18410000000049454e44ae426082";
const pngPath = path.join(dir, "sample.png");
fs.writeFileSync(pngPath, Buffer.from(pngHex, "hex"));
console.log("PNG written:", fs.statSync(pngPath).size, "bytes");

// 2. Generate real valid WebM video using playwright ffmpeg
const ffmpeg = "C:\\Users\\HP\\AppData\\Local\\ms-playwright\\ffmpeg-1013\\ffmpeg-win64.exe";
const webmPath = path.join(dir, "sample.webm");

try {
  execFileSync(ffmpeg, [
    "-y",
    "-loop", "1",
    "-i", pngPath,
    "-c:v", "libvpx_vp9",
    "-t", "2",
    "-pix_fmt", "yuv420p",
    webmPath
  ]);
  console.log("WebM video generated:", fs.statSync(webmPath).size, "bytes");
} catch (e) {
  console.error("FFmpeg error:", e);
}
