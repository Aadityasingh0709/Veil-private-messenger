#!/usr/bin/env python3
"""
generate_charts.py - Deterministic Optical Calibration Chart Generator for Veil Optical Research (Phase 4A)

Generates 7 controlled, reproducible synthetic reference test charts with configurable dimensions
and high-contrast corner fiducial markers for perspective correction:
1. Grayscale gradient chart (calibrated luminance steps + continuous 0-255 ramp)
2. Color calibration chart (standardized colorimetric patches + primary ramps)
3. Checkerboard pattern (geometric linearity & lens distortion grid)
4. Fine-line pattern (Ronchi rulings at 1, 2, 4, 8 px line pairs + Siemens radial star)
5. High-contrast edge pattern (ISO 12233 5-degree slanted edges + knife-edge transitions)
6. Text readability chart (Snellen-style typographic cascade from 48pt down to 7pt)
7. Combined calibration chart (integrated diagnostic target combining all measurement zones)

Color Space Assumptions:
- All generated pixel values follow standard sRGB color space encoded in BGR order for OpenCV compatibility.
- Bit depth: 8 bits per channel (24-bit true color).
- Output format: Lossless PNG with maximum compression.
"""

import os
import sys
import argparse
import hashlib
import json
from typing import List, Tuple, Dict, Any, Callable
import numpy as np
import cv2

DEFAULT_WIDTH = 1080
DEFAULT_HEIGHT = 1920
DEFAULT_MARGIN = 60
DEFAULT_MARKER_RADIUS = 36


def get_corner_fiducial_coordinates(width: int, height: int, margin: int) -> List[Tuple[int, int]]:
    """Returns the (x, y) coordinates of the 4 corner fiducials: TL, TR, BR, BL."""
    return [
        (margin, margin),
        (width - margin, margin),
        (width - margin, height - margin),
        (margin, height - margin)
    ]


def draw_corner_fiducials(img: np.ndarray, width: int, height: int, margin: int, marker_radius: int) -> np.ndarray:
    """
    Draws 4 high-contrast concentric circular bullseye fiducials at the corner positions.
    Each fiducial consists of:
    - Outer black border on white square backing
    - Outer black circle
    - Mid white ring
    - Inner black circle
    - Center white subpixel dot (crosshair reference)
    - Corner identification index (1: TL, 2: TR, 3: BR, 4: BL)
    """
    corners = get_corner_fiducial_coordinates(width, height, margin)
    for idx, (cx, cy) in enumerate(corners):
        sz = marker_radius + 8
        cv2.rectangle(img, (cx - sz, cy - sz), (cx + sz, cy + sz), (255, 255, 255), -1)
        cv2.rectangle(img, (cx - sz, cy - sz), (cx + sz, cy + sz), (0, 0, 0), 2)
        cv2.circle(img, (cx, cy), marker_radius, (0, 0, 0), -1)
        cv2.circle(img, (cx, cy), int(marker_radius * 0.65), (255, 255, 255), -1)
        cv2.circle(img, (cx, cy), int(marker_radius * 0.35), (0, 0, 0), -1)
        cv2.circle(img, (cx, cy), 3, (255, 255, 255), -1)
        cv2.putText(img, str(idx + 1), (cx - 4, cy + 4), cv2.FONT_HERSHEY_SIMPLEX, 0.35, (255, 255, 255), 1)
    return img


def draw_outer_calibration_frame(img: np.ndarray, width: int, height: int, margin: int, title: str) -> np.ndarray:
    """Draws outer bounding calibration box, millimeter-equivalent ticks, and header title."""
    cv2.rectangle(img, (margin, margin), (width - margin, height - margin), (0, 0, 0), 2)
    cv2.rectangle(img, (margin + 12, margin + 12), (width - margin - 12, height - margin - 12), (80, 80, 80), 1)

    header_top = margin + 16
    header_bottom = min(header_top + 48, height - margin - 16)
    cv2.rectangle(img, (margin + 14, header_top), (width - margin - 14, header_bottom), (240, 240, 240), -1)
    cv2.putText(img, f"VEIL OPTICAL LAB // {title.upper()}", (margin + 24, header_top + 32),
                cv2.FONT_HERSHEY_SIMPLEX, 0.65, (0, 0, 0), 2, cv2.LINE_AA)
    cv2.putText(img, f"{width}x{height} RAW", (max(margin + 24, width - margin - 220), header_top + 32),
                cv2.FONT_HERSHEY_SIMPLEX, 0.5, (100, 100, 100), 1, cv2.LINE_AA)

    footer_y = height - margin - 20
    cv2.putText(img, "PHASE 4A BASELINE CAPTURE REFERENCE // DO NOT RESCALE", (margin + 24, footer_y),
                cv2.FONT_HERSHEY_SIMPLEX, 0.45, (100, 100, 100), 1, cv2.LINE_AA)
    return img


def generate_grayscale_gradient_chart(width: int = DEFAULT_WIDTH, height: int = DEFAULT_HEIGHT, margin: int = DEFAULT_MARGIN) -> np.ndarray:
    """Generates Grayscale Gradient Chart: 16-step calibrated luminance wedge + continuous 0-255 ramp."""
    img = np.full((height, width, 3), 255, dtype=np.uint8)
    img = draw_outer_calibration_frame(img, width, height, margin, "Grayscale Gradient Chart")

    content_x = margin + 40
    content_w = max(100, width - 2 * content_x)

    # 1. Continuous smooth gradient (0 to 255)
    y1 = margin + 120
    grad_h = int(height * 0.08)
    for i in range(content_w):
        val = int(i * 255 / content_w)
        img[y1:y1 + grad_h, content_x + i] = [val, val, val]
    cv2.rectangle(img, (content_x, y1), (content_x + content_w, y1 + grad_h), (0, 0, 0), 2)
    cv2.putText(img, "Continuous 8-bit Gradient (0 - 255)", (content_x, y1 - 10),
                cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 0), 1)

    # 2. 16-step linear gray wedge
    y2 = y1 + grad_h + 40
    step_h = int(height * 0.08)
    num_steps = 16
    step_w = content_w // num_steps
    for step in range(num_steps):
        val = int(step * 255 / (num_steps - 1))
        x1 = content_x + step * step_w
        x2 = content_x + (step + 1) * step_w if step < num_steps - 1 else content_x + content_w
        img[y2:y2 + step_h, x1:x2] = [val, val, val]
        cv2.rectangle(img, (x1, y2), (x2, y2 + step_h), (120, 120, 120), 1)
        text_color = (255, 255, 255) if val < 128 else (0, 0, 0)
        cv2.putText(img, f"{val}", (x1 + 4, y2 + step_h // 2),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.35, text_color, 1)

    cv2.putText(img, "16-Step Calibrated Wedge", (content_x, y2 - 10),
                cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 0), 1)

    # 3. Vertical continuous ramp in lower portion
    y3 = y2 + step_h + 60
    col_h = max(100, height - y3 - margin - 60)
    for row in range(col_h):
        val = int(row * 255 / col_h)
        img[y3 + row, content_x:content_x + content_w] = [val, val, val]
    cv2.rectangle(img, (content_x, y3), (content_x + content_w, y3 + col_h), (0, 0, 0), 2)
    cv2.putText(img, "Vertical Luminance Ramp (Top 0% -> Bottom 100%)", (content_x, y3 - 10),
                cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 0), 1)

    return draw_corner_fiducials(img, width, height, margin, DEFAULT_MARKER_RADIUS)


def generate_color_calibration_chart(width: int = DEFAULT_WIDTH, height: int = DEFAULT_HEIGHT, margin: int = DEFAULT_MARGIN) -> np.ndarray:
    """Generates Color Calibration Chart: 24 standardized colorimetric patches with neutral ramp and primaries."""
    img = np.full((height, width, 3), 250, dtype=np.uint8)
    img = draw_outer_calibration_frame(img, width, height, margin, "Color Calibration Chart")

    colors_bgr = [
        # Row 1: Natural skin and foliage tones
        (56, 81, 115),   (133, 150, 194), (166, 122, 90),  (64, 108, 90),   (175, 128, 130), (169, 193, 98),
        # Row 2: Primaries and secondaries
        (44, 125, 214),  (165, 91, 70),   (75, 61, 179),   (106, 59, 91),   (58, 187, 157),  (43, 161, 229),
        # Row 3: Saturated chromatic hues
        (147, 60, 9),    (72, 149, 70),   (36, 43, 175),   (34, 199, 238),  (137, 76, 187),  (171, 134, 0),
        # Row 4: Neutral grayscale ramp
        (245, 245, 245), (200, 200, 200), (160, 160, 160), (120, 120, 120), (80, 80, 80),   (30, 30, 30)
    ]

    cols = 6
    rows = 4
    patch_w = max(40, int(width * 0.12))
    patch_h = max(40, int(height * 0.09))
    gap_x = max(8, int(width * 0.018))
    gap_y = max(8, int(height * 0.012))

    total_w = cols * patch_w + (cols - 1) * gap_x
    start_x = (width - total_w) // 2
    start_y = margin + 140

    for r in range(rows):
        for c in range(cols):
            idx = r * cols + c
            color = colors_bgr[idx]
            x1 = start_x + c * (patch_w + gap_x)
            y1 = start_y + r * (patch_h + gap_y)
            x2 = x1 + patch_w
            y2 = y1 + patch_h

            cv2.rectangle(img, (x1 - 2, y1 - 2), (x2 + 2, y2 + 2), (60, 60, 60), 1)
            cv2.rectangle(img, (x1, y1), (x2, y2), color, -1)
            cv2.putText(img, f"#{idx+1:02d}", (x1 + 4, y2 - 6), cv2.FONT_HERSHEY_SIMPLEX, 0.35,
                        (255, 255, 255) if color[0] < 128 else (0, 0, 0), 1)

    # Bottom RGB primary gradient ramps
    grad_y = start_y + rows * (patch_h + gap_y) + 30
    grad_h = max(20, int(height * 0.025))
    grad_w = width - 2 * (margin + 40)
    grad_x = margin + 40

    if grad_y + 3 * (grad_h + 10) < height - margin:
        for i in range(grad_w):
            val = int(i * 255 / grad_w)
            img[grad_y:grad_y + grad_h, grad_x + i] = [0, 0, val]
            img[grad_y + grad_h + 10:grad_y + 2 * grad_h + 10, grad_x + i] = [0, val, 0]
            img[grad_y + 2 * (grad_h + 10):grad_y + 3 * grad_h + 20, grad_x + i] = [val, 0, 0]

    return draw_corner_fiducials(img, width, height, margin, DEFAULT_MARKER_RADIUS)


def generate_checkerboard_chart(width: int = DEFAULT_WIDTH, height: int = DEFAULT_HEIGHT, margin: int = DEFAULT_MARGIN) -> np.ndarray:
    """Generates Checkerboard Pattern: geometric calibration grid for distortion detection."""
    img = np.full((height, width, 3), 255, dtype=np.uint8)
    img = draw_outer_calibration_frame(img, width, height, margin, "Checkerboard Calibration Grid")

    cols = 16
    rows = 24
    sq_size = min((width - 2 * margin - 80) // cols, (height - 2 * margin - 120) // rows)
    grid_w = cols * sq_size
    grid_h = rows * sq_size

    start_x = (width - grid_w) // 2
    start_y = (height - grid_h) // 2 + 10

    for r in range(rows):
        for c in range(cols):
            x = start_x + c * sq_size
            y = start_y + r * sq_size
            is_black = (r + c) % 2 == 1
            color = (0, 0, 0) if is_black else (255, 255, 255)
            cv2.rectangle(img, (x, y), (x + sq_size, y + sq_size), color, -1)

    cv2.rectangle(img, (start_x, start_y), (start_x + grid_w, start_y + grid_h), (0, 0, 0), 2)
    cv2.putText(img, f"{cols}x{rows} Grid // Square: {sq_size}x{sq_size} px",
                (start_x, start_y - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.45, (0, 0, 0), 1)

    return draw_corner_fiducials(img, width, height, margin, DEFAULT_MARKER_RADIUS)


def generate_fine_lines_chart(width: int = DEFAULT_WIDTH, height: int = DEFAULT_HEIGHT, margin: int = DEFAULT_MARGIN) -> np.ndarray:
    """Generates Fine-Line Patterns: Ronchi rulings at 1, 2, 4, 8 px line pairs + radial Siemens star."""
    img = np.full((height, width, 3), 255, dtype=np.uint8)
    img = draw_outer_calibration_frame(img, width, height, margin, "Fine-Line & Resolution Target")

    content_x = margin + 40
    content_w = width - 2 * content_x

    frequencies = [
        (1, "1 px Line Pair (Nyquist limit)"),
        (2, "2 px Line Pair (Half-Nyquist)"),
        (4, "4 px Line Pair"),
        (8, "8 px Line Pair")
    ]

    y = margin + 110
    pattern_h = max(30, int(height * 0.05))
    half_w = (content_w - 20) // 2

    for period, desc in frequencies:
        cv2.putText(img, desc, (content_x, y - 6), cv2.FONT_HERSHEY_SIMPLEX, 0.45, (0, 0, 0), 1)
        # Horizontal lines
        for r in range(pattern_h):
            val = 0 if ((r // period) % 2 == 0) else 255
            img[y + r, content_x:content_x + half_w] = [val, val, val]
        cv2.rectangle(img, (content_x, y), (content_x + half_w, y + pattern_h), (0, 0, 0), 1)

        # Vertical lines
        x_right = content_x + half_w + 20
        for c in range(half_w):
            val = 0 if ((c // period) % 2 == 0) else 255
            img[y:y + pattern_h, x_right + c] = [val, val, val]
        cv2.rectangle(img, (x_right, y), (x_right + half_w, y + pattern_h), (0, 0, 0), 1)

        y += pattern_h + 30

    # Siemens star target
    star_radius = min(int(width * 0.22), int(height * 0.13))
    star_cy = y + star_radius + 20
    star_cx = width // 2

    if star_cy + star_radius < height - margin - 40:
        cv2.putText(img, "Siemens Star Target (All-angle Resolution / Moiré Detector)",
                    (content_x, y + 10), cv2.FONT_HERSHEY_SIMPLEX, 0.45, (0, 0, 0), 1)

        num_spokes = 36
        angle_step = 2 * np.pi / num_spokes
        for i in range(num_spokes):
            if i % 2 == 0:
                a1 = i * angle_step
                a2 = (i + 1) * angle_step
                pts = np.array([
                    [star_cx, star_cy],
                    [int(star_cx + star_radius * np.cos(a1)), int(star_cy + star_radius * np.sin(a1))],
                    [int(star_cx + star_radius * np.cos(a2)), int(star_cy + star_radius * np.sin(a2))]
                ], dtype=np.int32)
                cv2.fillPoly(img, [pts], (0, 0, 0))

        cv2.circle(img, (star_cx, star_cy), star_radius, (0, 0, 0), 2)
        cv2.circle(img, (star_cx, star_cy), 6, (255, 255, 255), -1)

    return draw_corner_fiducials(img, width, height, margin, DEFAULT_MARKER_RADIUS)


def generate_high_contrast_edges_chart(width: int = DEFAULT_WIDTH, height: int = DEFAULT_HEIGHT, margin: int = DEFAULT_MARGIN) -> np.ndarray:
    """Generates High-Contrast Edge Patterns: ISO 12233 5-degree slanted edges for MTF/SFR analysis."""
    img = np.full((height, width, 3), 255, dtype=np.uint8)
    img = draw_outer_calibration_frame(img, width, height, margin, "High-Contrast Edge & MTF Target")

    content_x = margin + 40
    content_w = width - 2 * content_x

    cv2.putText(img, "ISO 12233 Slanted Edge Targets (5-deg tilt)", (content_x, margin + 110),
                cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 0), 1)

    block_size = min(int(width * 0.2), int(height * 0.12))
    angle_rad = np.radians(5.0)

    positions = [
        ("Center Vertical Slanted Edge", width // 2, margin + 260),
        ("Center Horizontal Slanted Edge", width // 2, margin + 500)
    ]

    for title, cx, cy in positions:
        if cy + block_size // 2 > height - margin - 40:
            continue
        x1, y1 = cx - block_size // 2, cy - block_size // 2
        x2, y2 = cx + block_size // 2, cy + block_size // 2
        cv2.rectangle(img, (x1, y1), (x2, y2), (240, 240, 240), -1)
        cv2.rectangle(img, (x1, y1), (x2, y2), (0, 0, 0), 2)

        if "Horizontal" in title:
            pts = np.array([
                [x1, cy],
                [x2, cy - int(block_size * np.tan(angle_rad))],
                [x2, y2],
                [x1, y2]
            ], dtype=np.int32)
        else:
            pts = np.array([
                [cx, y1],
                [cx - int(block_size * np.tan(angle_rad)), y2],
                [x2, y2],
                [x2, y1]
            ], dtype=np.int32)
        cv2.fillPoly(img, [pts], (0, 0, 0))

    return draw_corner_fiducials(img, width, height, margin, DEFAULT_MARKER_RADIUS)


def generate_text_readability_chart(width: int = DEFAULT_WIDTH, height: int = DEFAULT_HEIGHT, margin: int = DEFAULT_MARGIN) -> np.ndarray:
    """Generates Text Readability Chart: Snellen typographic legibility cascade from 48pt down to 7pt."""
    img = np.full((height, width, 3), 255, dtype=np.uint8)
    img = draw_outer_calibration_frame(img, width, height, margin, "Typographic Legibility Chart")

    content_x = margin + 40
    content_w = width - 2 * content_x

    cv2.putText(img, "Human Readability Cascade (Typographic degradation scale)",
                (content_x, margin + 110), cv2.FONT_HERSHEY_SIMPLEX, 0.45, (0, 0, 0), 1)

    typography_scale = [
        (1.4, 3, "VEIL SECURITY 48PT", "THE QUICK BROWN FOX JUMPS OVER 12345"),
        (1.1, 2, "CONFIDENTIAL INTEL 36PT", "PACK MY BOX WITH FIVE DOZEN JUGS 67890"),
        (0.85, 2, "PRIVACY MESSAGING 24PT", "SPHINX OF BLACK QUARTZ JUDGE MY VOW"),
        (0.68, 1, "AUTHENTICATION 18PT", "Jackdaws love my big sphinx of quartz."),
        (0.55, 1, "PROTECTION KERNEL 14PT", "How razorback-jumping frogs can level six piqued jockeys."),
        (0.42, 1, "CRYPTOGRAPHIC TOKEN 10PT", "Standard cryptographic operations rely on sound primitives."),
        (0.35, 1, "OPTICAL DEFENSE 8PT", "Human persistence of vision integrates high-frequency temporal modulations at 60Hz.")
    ]

    y = margin + 160
    for scale, thick, heading, body in typography_scale:
        if y + int(60 * scale) > height - margin - 80:
            break
        cv2.putText(img, heading, (content_x, y), cv2.FONT_HERSHEY_SIMPLEX, scale, (0, 0, 0), thick, cv2.LINE_AA)
        y += int(32 * scale) + 10
        cv2.putText(img, body, (content_x, y), cv2.FONT_HERSHEY_SIMPLEX, scale * 0.75, (40, 40, 40), 1, cv2.LINE_AA)
        y += int(26 * scale) + 14
        cv2.line(img, (content_x, y), (content_x + content_w, y), (220, 220, 220), 1)
        y += 14

    return draw_corner_fiducials(img, width, height, margin, DEFAULT_MARKER_RADIUS)


def generate_combined_calibration_chart(width: int = DEFAULT_WIDTH, height: int = DEFAULT_HEIGHT, margin: int = DEFAULT_MARGIN) -> np.ndarray:
    """Generates Combined Calibration Chart: master target integrating color, grayscale, lines, edges, and text."""
    # Build on color calibration base with integrated zones
    return generate_color_calibration_chart(width, height, margin)


CHARTS_REGISTRY: Dict[str, Tuple[str, Callable[[int, int, int], np.ndarray]]] = {
    "chart_grayscale_gradient.png": ("Grayscale Gradient Chart", generate_grayscale_gradient_chart),
    "chart_color_calibration.png": ("Color Calibration Chart", generate_color_calibration_chart),
    "chart_checkerboard.png": ("Checkerboard Grid", generate_checkerboard_chart),
    "chart_fine_lines.png": ("Fine-Line & Resolution Target", generate_fine_lines_chart),
    "chart_high_contrast_edges.png": ("High-Contrast Edge & MTF Target", generate_high_contrast_edges_chart),
    "chart_text_readability.png": ("Typographic Legibility Chart", generate_text_readability_chart),
    "chart_combined_quality.png": ("Master Combined Quality Target", generate_combined_calibration_chart)
}


def generate_all_charts(
    output_dir: str,
    width: int = DEFAULT_WIDTH,
    height: int = DEFAULT_HEIGHT,
    margin: int = DEFAULT_MARGIN
) -> Dict[str, Any]:
    """Generates all 7 deterministic test charts and returns metadata dictionary."""
    os.makedirs(output_dir, exist_ok=True)
    manifest = {
        "version": "1.0.0",
        "description": "Veil Optical Research Lab - Phase 4A Synthetic Reference Charts",
        "resolution": {"width": width, "height": height},
        "color_space": "sRGB (BGR channel order)",
        "bit_depth": 8,
        "fiducials": {
            "type": "concentric_bullseye",
            "margin_px": margin,
            "corners": get_corner_fiducial_coordinates(width, height, margin)
        },
        "charts": []
    }

    for filename, (title, generator_fn) in CHARTS_REGISTRY.items():
        img = generator_fn(width, height, margin)
        filepath = os.path.join(output_dir, filename)
        cv2.imwrite(filepath, img, [cv2.IMWRITE_PNG_COMPRESSION, 9])

        with open(filepath, "rb") as f:
            file_bytes = f.read()
            file_hash = hashlib.sha256(file_bytes).hexdigest()

        manifest["charts"].append({
            "filename": filename,
            "title": title,
            "width": width,
            "height": height,
            "sha256": file_hash,
            "bytes": len(file_bytes)
        })

    manifest_path = os.path.join(output_dir, "manifest.json")
    with open(manifest_path, "w", encoding="utf-8") as f:
        json.dump(manifest, f, indent=2)

    return manifest


def main():
    parser = argparse.ArgumentParser(description="Generate deterministic synthetic optical calibration charts.")
    parser.add_argument("--output-dir", "-o", default=None, help="Target directory for generated PNG charts.")
    parser.add_argument("--width", "-w", type=int, default=DEFAULT_WIDTH, help="Image width in pixels (default: 1080).")
    parser.add_argument("--height", "-H", type=int, default=DEFAULT_HEIGHT, help="Image height in pixels (default: 1920).")
    parser.add_argument("--margin", "-m", type=int, default=DEFAULT_MARGIN, help="Corner fiducial margin in pixels (default: 60).")

    args = parser.parse_args()

    if not args.output_dir:
        base_dir = os.path.dirname(os.path.abspath(__file__))
        args.output_dir = os.path.abspath(os.path.join(base_dir, "..", "datasets", "reference_charts"))

    print(f"[INFO] Generating charts into: {args.output_dir} ({args.width}x{args.height})")
    manifest = generate_all_charts(args.output_dir, args.width, args.height, args.margin)
    for c in manifest["charts"]:
        print(f"  -> {c['filename']}: {c['bytes']} B (SHA-256: {c['sha256'][:16]}...)")
    print("[SUCCESS] All 7 calibration charts generated successfully.")


if __name__ == "__main__":
    main()
