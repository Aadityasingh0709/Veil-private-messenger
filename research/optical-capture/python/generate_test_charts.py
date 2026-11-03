#!/usr/bin/env python3
"""
generate_test_charts.py - Deterministic Synthetic Test Chart Generator for Veil Optical Research Lab (Phase 4A)

Generates 7 controlled, reproducible synthetic test charts with known dimensions,
pixel values, and high-contrast corner fiducial markers for camera perspective correction:
1. Color Calibration Chart (24-patch colorimetric reference)
2. Grayscale Gradient Chart (16-step wedge + smooth continuous 0-255 ramp)
3. Checkerboard Pattern (16x16 alternating geometric calibration grid)
4. Fine-Line Patterns (Ronchi rulings at 1, 2, 4, 8 px line pairs + radial star target)
5. High-Contrast Edge Patterns (ISO 12233 slanted edges + knife-edge steps)
6. Text Readability Chart (Typographic legibility cascade from 48pt to 6pt with varying contrast)
7. Combined Image-Quality Chart (Master target integrating all diagnostic zones)
"""

import os
import hashlib
import json
import numpy as np
import cv2

# Standard resolution matching typical smartphone 9:16 display ratio
WIDTH = 1080
HEIGHT = 1920

# 4 Corner Fiducial Marker specification (center coordinates)
MARGIN = 60
MARKER_RADIUS = 36
CORNERS = [
    (MARGIN, MARGIN),                      # Top-Left
    (WIDTH - MARGIN, MARGIN),              # Top-Right
    (WIDTH - MARGIN, HEIGHT - MARGIN),     # Bottom-Right
    (MARGIN, HEIGHT - MARGIN)              # Bottom-Left
]


def draw_corner_fiducials(img: np.ndarray) -> np.ndarray:
    """
    Draws 4 high-contrast concentric circular bullseye fiducials at the designated corner positions.
    Each fiducial consists of:
    - Outer black ring on white square
    - Mid white ring
    - Inner black circle
    - White subpixel crosshair
    This geometry provides high gradient contrast for sub-pixel corner and circle detection.
    """
    for idx, (cx, cy) in enumerate(CORNERS):
        # Draw background white square with 2px black border
        sz = MARKER_RADIUS + 8
        cv2.rectangle(img, (cx - sz, cy - sz), (cx + sz, cy + sz), (255, 255, 255), -1)
        cv2.rectangle(img, (cx - sz, cy - sz), (cx + sz, cy + sz), (0, 0, 0), 2)
        
        # Outer black ring
        cv2.circle(img, (cx, cy), MARKER_RADIUS, (0, 0, 0), -1)
        # Mid white ring
        cv2.circle(img, (cx, cy), int(MARKER_RADIUS * 0.65), (255, 255, 255), -1)
        # Inner black circle
        cv2.circle(img, (cx, cy), int(MARKER_RADIUS * 0.35), (0, 0, 0), -1)
        # Center white dot
        cv2.circle(img, (cx, cy), 3, (255, 255, 255), -1)
        # Small orientation tick (distinguishes top-left, top-right, etc.)
        cv2.putText(img, str(idx + 1), (cx - 4, cy + 4), cv2.FONT_HERSHEY_SIMPLEX, 0.35, (255, 255, 255), 1)
        
    return img


def draw_outer_calibration_frame(img: np.ndarray, title: str) -> np.ndarray:
    """Draws outer bounding calibration box, millimeter-equivalent ticks, and header title."""
    # Outer 2px border connecting the fiducial boundary
    cv2.rectangle(img, (MARGIN, MARGIN), (WIDTH - MARGIN, HEIGHT - MARGIN), (0, 0, 0), 2)
    # Inner 1px border
    cv2.rectangle(img, (MARGIN + 12, MARGIN + 12), (WIDTH - MARGIN - 12, HEIGHT - MARGIN - 12), (80, 80, 80), 1)

    # Header bar
    header_top = MARGIN + 16
    header_bottom = header_top + 48
    cv2.rectangle(img, (MARGIN + 14, header_top), (WIDTH - MARGIN - 14, header_bottom), (240, 240, 240), -1)
    cv2.putText(img, f"VEIL OPTICAL LAB // {title.upper()}", (MARGIN + 24, header_top + 32),
                cv2.FONT_HERSHEY_SIMPLEX, 0.75, (0, 0, 0), 2, cv2.LINE_AA)
    cv2.putText(img, f"{WIDTH}x{HEIGHT} RAW REFERENCE", (WIDTH - MARGIN - 260, header_top + 32),
                cv2.FONT_HERSHEY_SIMPLEX, 0.5, (100, 100, 100), 1, cv2.LINE_AA)
    
    # Footer info
    footer_y = HEIGHT - MARGIN - 20
    cv2.putText(img, "PHASE 4A BASELINE CAPTURE REFERENCE // DO NOT RESCALE", (MARGIN + 24, footer_y),
                cv2.FONT_HERSHEY_SIMPLEX, 0.45, (100, 100, 100), 1, cv2.LINE_AA)
    
    return img


def generate_color_calibration_chart() -> np.ndarray:
    """1. Color Calibration Chart: 24 standardized colorimetric patches with neutral ramp and primaries."""
    img = np.full((HEIGHT, WIDTH, 3), 250, dtype=np.uint8)
    img = draw_outer_calibration_frame(img, "Color Calibration Chart")
    
    # 24 standard color patches (BGR order for OpenCV)
    # 6 columns x 4 rows
    colors_bgr = [
        # Row 1: Natural skin and foliage
        (56, 81, 115),   (133, 150, 194), (166, 122, 90),  (64, 108, 90),   (175, 128, 130), (169, 193, 98),
        # Row 2: Primaries and secondaries
        (44, 125, 214),  (165, 91, 70),   (75, 61, 179),   (106, 59, 91),   (58, 187, 157),  (43, 161, 229),
        # Row 3: Saturated chromatic hues
        (147, 60, 9),    (72, 149, 70),   (36, 43, 175),   (34, 199, 238),  (137, 76, 187),  (171, 134, 0),
        # Row 4: Neutral grayscale ramp (white to black)
        (245, 245, 245), (200, 200, 200), (160, 160, 160), (120, 120, 120), (80, 80, 80),   (30, 30, 30)
    ]
    
    patch_w = 130
    patch_h = 180
    start_x = (WIDTH - (6 * patch_w + 5 * 20)) // 2
    start_y = 200
    
    for row in range(4):
        for col in range(6):
            idx = row * 6 + col
            color = colors_bgr[idx]
            x1 = start_x + col * (patch_w + 20)
            y1 = start_y + row * (patch_h + 24)
            x2 = x1 + patch_w
            y2 = y1 + patch_h
            
            # Patch border and fill
            cv2.rectangle(img, (x1 - 2, y1 - 2), (x2 + 2, y2 + 2), (60, 60, 60), 1)
            cv2.rectangle(img, (x1, y1), (x2, y2), color, -1)
            
            # Label
            label = f"#{idx+1:02d}"
            cv2.putText(img, label, (x1 + 6, y2 - 8), cv2.FONT_HERSHEY_SIMPLEX, 0.4, (255, 255, 255) if color[0] < 128 else (0, 0, 0), 1)

    # Add continuous primary RGB gradients at the bottom
    grad_y = start_y + 4 * (patch_h + 24) + 40
    grad_h = 45
    grad_w = WIDTH - 2 * (MARGIN + 40)
    grad_x = MARGIN + 40
    
    for i in range(grad_w):
        val = int(i * 255 / grad_w)
        # Red ramp
        img[grad_y:grad_y+grad_h, grad_x + i] = [0, 0, val]
        # Green ramp
        img[grad_y+60:grad_y+60+grad_h, grad_x + i] = [0, val, 0]
        # Blue ramp
        img[grad_y+120:grad_y+120+grad_h, grad_x + i] = [val, 0, 0]

    return draw_corner_fiducials(img)


def generate_grayscale_gradient_chart() -> np.ndarray:
    """2. Grayscale Gradient Chart: 16-step calibrated luminance wedge + smooth 0-255 ramp."""
    img = np.full((HEIGHT, WIDTH, 3), 255, dtype=np.uint8)
    img = draw_outer_calibration_frame(img, "Grayscale Gradient Chart")
    
    content_x = MARGIN + 40
    content_w = WIDTH - 2 * content_x
    
    # 1. Continuous smooth gradient (0 to 255)
    y_start = 180
    grad_h = 160
    for i in range(content_w):
        val = int(i * 255 / content_w)
        img[y_start:y_start+grad_h, content_x + i] = [val, val, val]
    cv2.rectangle(img, (content_x, y_start), (content_x + content_w, y_start + grad_h), (0, 0, 0), 2)
    cv2.putText(img, "Continuous 8-bit Gradient (0 - 255)", (content_x, y_start - 10),
                cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 0), 1)
    
    # 2. 16-step linear gray wedge
    y_start2 = 400
    step_h = 160
    num_steps = 16
    step_w = content_w // num_steps
    
    for step in range(num_steps):
        val = int(step * 255 / (num_steps - 1))
        x1 = content_x + step * step_w
        x2 = content_x + (step + 1) * step_w if step < num_steps - 1 else content_x + content_w
        img[y_start2:y_start2+step_h, x1:x2] = [val, val, val]
        cv2.rectangle(img, (x1, y_start2), (x2, y_start2+step_h), (120, 120, 120), 1)
        # Value label
        text_color = (255, 255, 255) if val < 128 else (0, 0, 0)
        cv2.putText(img, f"{val}", (x1 + 4, y_start2 + step_h // 2),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.35, text_color, 1)

    cv2.putText(img, "16-Step Calibrated Wedge", (content_x, y_start2 - 10),
                cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 0), 1)

    # 3. Low-contrast detection patches (critical for sensor dynamic range analysis)
    # Near black patches: 0, 2, 4, 8, 12, 16, 24, 32
    # Near white patches: 224, 232, 240, 244, 248, 252, 254, 255
    y_start3 = 630
    cv2.putText(img, "Shadow & Highlight Detail Resolving Patches", (content_x, y_start3 - 10),
                cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 0), 1)
    
    shadow_vals = [0, 2, 4, 8, 12, 16, 24, 32]
    highlight_vals = [224, 232, 240, 244, 248, 252, 254, 255]
    pw = content_w // 8
    
    # Shadow row
    for idx, v in enumerate(shadow_vals):
        x1 = content_x + idx * pw
        cv2.rectangle(img, (x1, y_start3), (x1 + pw, y_start3 + 100), (v, v, v), -1)
        cv2.rectangle(img, (x1, y_start3), (x1 + pw, y_start3 + 100), (80, 80, 80), 1)
        cv2.putText(img, f"v={v}", (x1 + 6, y_start3 + 60), cv2.FONT_HERSHEY_SIMPLEX, 0.4, (200, 200, 200), 1)
        
    # Highlight row
    y_start4 = 760
    for idx, v in enumerate(highlight_vals):
        x1 = content_x + idx * pw
        cv2.rectangle(img, (x1, y_start4), (x1 + pw, y_start4 + 100), (v, v, v), -1)
        cv2.rectangle(img, (x1, y_start4), (x1 + pw, y_start4 + 100), (80, 80, 80), 1)
        cv2.putText(img, f"v={v}", (x1 + 6, y_start4 + 60), cv2.FONT_HERSHEY_SIMPLEX, 0.4, (40, 40, 40), 1)

    # 4. Vertical continuous gradient column in lower portion
    y_start5 = 920
    grad_col_h = 750
    for row in range(grad_col_h):
        val = int(row * 255 / grad_col_h)
        img[y_start5 + row, content_x:content_x+content_w] = [val, val, val]
    cv2.rectangle(img, (content_x, y_start5), (content_x + content_w, y_start5 + grad_col_h), (0, 0, 0), 2)
    cv2.putText(img, "Vertical Luminance Ramp (Top 0% -> Bottom 100%)", (content_x, y_start5 - 10),
                cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 0), 1)

    return draw_corner_fiducials(img)


def generate_checkerboard_chart() -> np.ndarray:
    """3. Checkerboard Pattern: 16x16 alternating geometric calibration grid."""
    img = np.full((HEIGHT, WIDTH, 3), 255, dtype=np.uint8)
    img = draw_outer_calibration_frame(img, "Checkerboard Calibration Grid")
    
    # 16 x 24 grid centered in available space
    cols = 16
    rows = 24
    sq_size = 54
    grid_w = cols * sq_size
    grid_h = rows * sq_size
    
    start_x = (WIDTH - grid_w) // 2
    start_y = (HEIGHT - grid_h) // 2 + 20
    
    for r in range(rows):
        for c in range(cols):
            x = start_x + c * sq_size
            y = start_y + r * sq_size
            is_black = (r + c) % 2 == 1
            color = (0, 0, 0) if is_black else (255, 255, 255)
            cv2.rectangle(img, (x, y), (x + sq_size, y + sq_size), color, -1)
            
    # Border around grid
    cv2.rectangle(img, (start_x, start_y), (start_x + grid_w, start_y + grid_h), (0, 0, 0), 3)
    cv2.putText(img, f"16x24 Grid // Square: {sq_size}x{sq_size} px // Geometric Distortion Target",
                (start_x, start_y - 12), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 0), 1)

    return draw_corner_fiducials(img)


def generate_fine_lines_chart() -> np.ndarray:
    """4. Fine-Line Patterns: Ronchi rulings at 1, 2, 4, 8 px line pairs + radial star target."""
    img = np.full((HEIGHT, WIDTH, 3), 255, dtype=np.uint8)
    img = draw_outer_calibration_frame(img, "Fine-Line & Resolution Target")
    
    content_x = MARGIN + 40
    content_w = WIDTH - 2 * content_x
    
    # Frequencies to test: 1px, 2px, 4px, 8px line-pairs
    frequencies = [(1, "1 px Line Pair (Nyquist limit)"),
                   (2, "2 px Line Pair (Half-Nyquist)"),
                   (4, "4 px Line Pair"),
                   (8, "8 px Line Pair")]
    
    y = 180
    for period, desc in frequencies:
        cv2.putText(img, desc, (content_x, y - 8), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 0), 1)
        pattern_h = 100
        # Horizontal lines
        half_w = (content_w - 20) // 2
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
        
        y += pattern_h + 40
        
    # Radial Siemens star target in bottom half
    star_cy = y + 260
    star_cx = WIDTH // 2
    star_radius = 240
    cv2.putText(img, "Siemens Star Target (All-angle Resolution / Moire Detector)",
                (content_x, y + 10), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 0), 1)
    
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
    cv2.circle(img, (star_cx, star_cy), 8, (255, 255, 255), -1)

    return draw_corner_fiducials(img)


def generate_high_contrast_edges_chart() -> np.ndarray:
    """5. High-Contrast Edge Patterns: ISO 12233 slanted edges (5-degree tilt) for MTF/SFR analysis."""
    img = np.full((HEIGHT, WIDTH, 3), 255, dtype=np.uint8)
    img = draw_outer_calibration_frame(img, "High-Contrast Edge & MTF Target")
    
    content_x = MARGIN + 40
    content_w = WIDTH - 2 * content_x
    
    # Standard 5-degree slanted edge block (ISO 12233 compliance)
    cv2.putText(img, "ISO 12233 Slanted Edge Targets (5-deg tilt)", (content_x, 180),
                cv2.FONT_HERSHEY_SIMPLEX, 0.55, (0, 0, 0), 1)
    
    # 4 distinct slanted edge blocks across center and quadrants
    positions = [
        ("Center Vertical Slanted Edge", WIDTH // 2, 400),
        ("Center Horizontal Slanted Edge", WIDTH // 2, 750),
        ("Quadrant Edge 1", WIDTH // 4 + 40, 1150),
        ("Quadrant Edge 2", 3 * WIDTH // 4 - 40, 1150)
    ]
    
    block_size = 220
    angle_rad = np.radians(5.0)  # 5 degrees
    
    for title, cx, cy in positions:
        cv2.putText(img, title, (cx - block_size // 2, cy - block_size // 2 - 12),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.45, (60, 60, 60), 1)
        
        # Draw background gray box
        x1, y1 = cx - block_size // 2, cy - block_size // 2
        x2, y2 = cx + block_size // 2, cy + block_size // 2
        cv2.rectangle(img, (x1, y1), (x2, y2), (240, 240, 240), -1)
        cv2.rectangle(img, (x1, y1), (x2, y2), (0, 0, 0), 2)
        
        # Black slanted half
        if "Horizontal" in title:
            # Horizontal tilted line
            pts = np.array([
                [x1, cy],
                [x2, cy - int(block_size * np.tan(angle_rad))],
                [x2, y2],
                [x1, y2]
            ], dtype=np.int32)
        else:
            # Vertical tilted line
            pts = np.array([
                [cx, y1],
                [cx - int(block_size * np.tan(angle_rad)), y2],
                [x2, y2],
                [x2, y1]
            ], dtype=np.int32)
            
        cv2.fillPoly(img, [pts], (0, 0, 0))

    # Stepped knife-edge array at bottom
    cv2.putText(img, "Knife-Edge Transition Array (Step Response / Halo Detection)",
                (content_x, 1420), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 0), 1)
    for i in range(5):
        y_step = 1450 + i * 65
        cv2.rectangle(img, (content_x, y_step), (content_x + content_w // 2, y_step + 45), (0, 0, 0), -1)
        cv2.rectangle(img, (content_x + content_w // 2, y_step), (content_x + content_w, y_step + 45), (255, 255, 255), -1)
        cv2.rectangle(img, (content_x, y_step), (content_x + content_w, y_step + 45), (100, 100, 100), 1)

    return draw_corner_fiducials(img)


def generate_text_readability_chart() -> np.ndarray:
    """6. Text Readability Chart: Cascading typographic scale from 48pt to 6pt with varying contrast."""
    img = np.full((HEIGHT, WIDTH, 3), 255, dtype=np.uint8)
    img = draw_outer_calibration_frame(img, "Typographic Legibility Chart")
    
    content_x = MARGIN + 40
    content_w = WIDTH - 2 * content_x
    
    cv2.putText(img, "Human Readability Cascade (Snellen-style typographic degradation scale)",
                (content_x, 180), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 0), 1)
    
    # Scale of font sizes and text samples
    typography_scale = [
        (1.5, 3, "VEIL SECURITY 48PT", "THE QUICK BROWN FOX JUMPS OVER 12345"),
        (1.2, 2, "CONFIDENTIAL INTEL 36PT", "PACK MY BOX WITH FIVE DOZEN JUGS 67890"),
        (0.95, 2, "PRIVACY MESSAGING 24PT", "SPHINX OF BLACK QUARTZ JUDGE MY VOW"),
        (0.75, 2, "AUTHENTICATION SESSION 18PT", "Jackdaws love my big sphinx of quartz."),
        (0.60, 1, "PROTECTION KERNEL 14PT", "How razorback-jumping frogs can level six piqued jockeys."),
        (0.48, 1, "CRYPTOGRAPHIC TOKEN 11PT", "Standard cryptographic operations rely on mathematically sound primitives."),
        (0.40, 1, "OPTICAL DEFENSE 9PT", "Human persistence of vision integrates high-frequency temporal modulations at 60Hz."),
        (0.32, 1, "SYSTEM TELEMETRY 7PT", "Display pixel matrix: subpixel pentile / RGB stripe arrangement under rolling shutter capture.")
    ]
    
    y = 230
    for scale, thick, heading, body in typography_scale:
        cv2.putText(img, heading, (content_x, y), cv2.FONT_HERSHEY_SIMPLEX, scale, (0, 0, 0), thick, cv2.LINE_AA)
        y += int(38 * scale) + 12
        cv2.putText(img, body, (content_x, y), cv2.FONT_HERSHEY_SIMPLEX, scale * 0.75, (40, 40, 40), 1, cv2.LINE_AA)
        y += int(30 * scale) + 16
        cv2.line(img, (content_x, y), (content_x + content_w, y), (220, 220, 220), 1)
        y += 18
        
    # Contrast ladder (Text rendered under decreasing contrast: 100%, 75%, 50%, 25%, 15%, 5%)
    cv2.putText(img, "Contrast Sensitivity Ladder (18pt at decreasing contrast ratios)",
                (content_x, y + 10), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 0), 1)
    y += 40
    
    contrast_steps = [(1.0, 0), (0.75, 64), (0.50, 128), (0.25, 192), (0.15, 217), (0.05, 242)]
    for ratio, gray in contrast_steps:
        cv2.putText(img, f"Contrast {int(ratio*100)}%: SENSITIVE DATA TOKEN",
                    (content_x, y), cv2.FONT_HERSHEY_SIMPLEX, 0.65, (gray, gray, gray), 2, cv2.LINE_AA)
        y += 42
        
    # Inverted text banner (White text on black background)
    y += 20
    cv2.rectangle(img, (content_x, y), (content_x + content_w, y + 120), (0, 0, 0), -1)
    cv2.putText(img, "NEGATIVE POLARITY (INVERTED CONTRAST)",
                (content_x + 20, y + 45), cv2.FONT_HERSHEY_SIMPLEX, 0.8, (255, 255, 255), 2, cv2.LINE_AA)
    cv2.putText(img, "Testing contrast reversal under camera sensor tone-mapping algorithms.",
                (content_x + 20, y + 85), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (200, 200, 200), 1, cv2.LINE_AA)

    return draw_corner_fiducials(img)


def generate_combined_quality_chart() -> np.ndarray:
    """7. Combined Image-Quality Chart: Master target integrating color, grayscale, edges, resolution, and text."""
    img = np.full((HEIGHT, WIDTH, 3), 255, dtype=np.uint8)
    img = draw_outer_calibration_frame(img, "Master Combined Quality Target")
    
    content_x = MARGIN + 40
    content_w = WIDTH - 2 * content_x
    
    # 1. Top Section: 12 Color Patches
    y_top = 180
    cols = 6
    patch_w = (content_w - (cols - 1) * 12) // cols
    patch_h = 70
    key_colors_bgr = [
        (56, 81, 115),   (133, 150, 194), (166, 122, 90),  (64, 108, 90),   (175, 128, 130), (169, 193, 98),
        (44, 125, 214),  (165, 91, 70),   (75, 61, 179),   (106, 59, 91),   (58, 187, 157),  (43, 161, 229)
    ]
    for idx, c in enumerate(key_colors_bgr):
        row = idx // cols
        col = idx % cols
        x = content_x + col * (patch_w + 12)
        y = y_top + row * (patch_h + 10)
        cv2.rectangle(img, (x, y), (x + patch_w, y + patch_h), c, -1)
        cv2.rectangle(img, (x, y), (x + patch_w, y + patch_h), (80, 80, 80), 1)

    # 2. Middle Section: 16-step Gray Ramp + Gradient
    y_mid = y_top + 2 * (patch_h + 10) + 20
    for s in range(16):
        val = int(s * 255 / 15)
        sw = content_w // 16
        cv2.rectangle(img, (content_x + s * sw, y_mid), (content_x + (s + 1) * sw, y_mid + 45), (val, val, val), -1)
        cv2.rectangle(img, (content_x + s * sw, y_mid), (content_x + (s + 1) * sw, y_mid + 45), (100, 100, 100), 1)
        
    y_grad = y_mid + 55
    for i in range(content_w):
        val = int(i * 255 / content_w)
        img[y_grad:y_grad + 35, content_x + i] = [val, val, val]
    cv2.rectangle(img, (content_x, y_grad), (content_x + content_w, y_grad + 35), (0, 0, 0), 1)
    
    # 3. Center Section: Circular Center Target + Slanted Edges
    center_y = y_grad + 240
    center_x = WIDTH // 2
    # Concentric rings
    for r in [180, 140, 100, 60, 20]:
        cv2.circle(img, (center_x, center_y), r, (0, 0, 0), 2)
    cv2.line(img, (center_x - 190, center_y), (center_x + 190, center_y), (0, 0, 0), 1)
    cv2.line(img, (center_x, center_y - 190), (center_x, center_y + 190), (0, 0, 0), 1)
    
    # Slanted edges flanking center
    cv2.rectangle(img, (content_x + 20, center_y - 80), (content_x + 180, center_y + 80), (240, 240, 240), -1)
    cv2.rectangle(img, (content_x + 20, center_y - 80), (content_x + 180, center_y + 80), (0, 0, 0), 2)
    pts1 = np.array([[content_x + 100, center_y - 80], [content_x + 85, center_y + 80],
                     [content_x + 180, center_y + 80], [content_x + 180, center_y - 80]], dtype=np.int32)
    cv2.fillPoly(img, [pts1], (0, 0, 0))

    cv2.rectangle(img, (WIDTH - content_x - 180, center_y - 80), (WIDTH - content_x - 20, center_y + 80), (240, 240, 240), -1)
    cv2.rectangle(img, (WIDTH - content_x - 180, center_y - 80), (WIDTH - content_x - 20, center_y + 80), (0, 0, 0), 2)
    pts2 = np.array([[WIDTH - content_x - 100, center_y - 80], [WIDTH - content_x - 115, center_y + 80],
                     [WIDTH - content_x - 20, center_y + 80], [WIDTH - content_x - 20, center_y - 80]], dtype=np.int32)
    cv2.fillPoly(img, [pts2], (0, 0, 0))
    
    # 4. Multi-frequency line pairs
    y_lines = center_y + 220
    for period in [2, 4, 8]:
        for c in range(content_w):
            val = 0 if ((c // period) % 2 == 0) else 255
            img[y_lines:y_lines + 30, content_x + c] = [val, val, val]
        cv2.rectangle(img, (content_x, y_lines), (content_x + content_w, y_lines + 30), (0, 0, 0), 1)
        cv2.putText(img, f"{period}px LP", (content_x - 36, y_lines + 20), cv2.FONT_HERSHEY_SIMPLEX, 0.35, (0, 0, 0), 1)
        y_lines += 38
        
    # 5. Bottom Section: Text Cascade & Inverted Band
    y_text = y_lines + 30
    cv2.putText(img, "VEIL OPTICAL RESEARCH // MASTER CALIBRATION",
                (content_x, y_text), cv2.FONT_HERSHEY_SIMPLEX, 0.75, (0, 0, 0), 2, cv2.LINE_AA)
    cv2.putText(img, "The quick brown fox jumps over the lazy dog. 1234567890",
                (content_x, y_text + 35), cv2.FONT_HERSHEY_SIMPLEX, 0.55, (40, 40, 40), 1, cv2.LINE_AA)
    cv2.putText(img, "Human persistence of vision vs camera rolling shutter sampling.",
                (content_x, y_text + 65), cv2.FONT_HERSHEY_SIMPLEX, 0.45, (80, 80, 80), 1, cv2.LINE_AA)
                
    # Inverted band
    cv2.rectangle(img, (content_x, y_text + 90), (content_x + content_w, y_text + 150), (0, 0, 0), -1)
    cv2.putText(img, "INVERTED TEST REGION: ZERO FLICKER BASELINE",
                (content_x + 20, y_text + 130), cv2.FONT_HERSHEY_SIMPLEX, 0.65, (255, 255, 255), 2, cv2.LINE_AA)

    return draw_corner_fiducials(img)


def main():
    base_dir = os.path.dirname(os.path.abspath(__file__))
    output_dir = os.path.abspath(os.path.join(base_dir, "..", "datasets", "reference_charts"))
    os.makedirs(output_dir, exist_ok=True)
    
    charts = [
        ("chart_color_calibration.png", "Color Calibration Chart", generate_color_calibration_chart),
        ("chart_grayscale_gradient.png", "Grayscale Gradient Chart", generate_grayscale_gradient_chart),
        ("chart_checkerboard.png", "Checkerboard Grid", generate_checkerboard_chart),
        ("chart_fine_lines.png", "Fine-Line & Resolution Target", generate_fine_lines_chart),
        ("chart_high_contrast_edges.png", "High-Contrast Edge & MTF Target", generate_high_contrast_edges_chart),
        ("chart_text_readability.png", "Typographic Legibility Chart", generate_text_readability_chart),
        ("chart_combined_quality.png", "Master Combined Quality Target", generate_combined_quality_chart)
    ]
    
    manifest = {
        "version": "1.0.0",
        "description": "Deterministic Synthetic Reference Charts for Phase 4A Baseline Capture",
        "standard_resolution": {"width": WIDTH, "height": HEIGHT},
        "fiducial_markers": {
            "type": "concentric_bullseye",
            "margin_px": MARGIN,
            "marker_radius_px": MARKER_RADIUS,
            "corner_coordinates": {
                "top_left": list(CORNERS[0]),
                "top_right": list(CORNERS[1]),
                "bottom_right": list(CORNERS[2]),
                "bottom_left": list(CORNERS[3])
            }
        },
        "charts": []
    }
    
    print(f"Generating 7 synthetic test charts into: {output_dir}")
    for filename, title, generator_fn in charts:
        img = generator_fn()
        filepath = os.path.join(output_dir, filename)
        cv2.imwrite(filepath, img, [cv2.IMWRITE_PNG_COMPRESSION, 9])
        
        # Calculate SHA-256
        with open(filepath, "rb") as f:
            file_hash = hashlib.sha256(f.read()).hexdigest()
            
        file_size = os.path.getsize(filepath)
        print(f"  -> Generated {filename} ({file_size} bytes, SHA-256: {file_hash[:12]}...)")
        
        manifest["charts"].append({
            "filename": filename,
            "title": title,
            "width": WIDTH,
            "height": HEIGHT,
            "sha256": file_hash,
            "bytes": file_size
        })
        
    manifest_path = os.path.join(output_dir, "manifest.json")
    with open(manifest_path, "w", encoding="utf-8") as f:
        json.dump(manifest, f, indent=2)
    print(f"Saved charts metadata manifest to: {manifest_path}")


if __name__ == "__main__":
    main()
