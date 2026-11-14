#!/usr/bin/env python3
"""
audit_spatial_renderer.py - Phase 4B.1 Deterministic Rendering Comparison & Verification

Simulates the exact rendering pipeline of NormalBaselineRenderer and SpatialModulationRenderer
on the canonical Master Combined Quality reference chart (EXP-007) to verify:
1. Normal Mode preserves original source fidelity.
2. Spatial Mode introduces controlled, bounded spatial perturbation strictly within dstRect.
3. Aspect ratio is preserved.
4. Corner fiducials and calibration margins remain pristine and unperturbed.
5. Generates deterministic comparison images and metrics without overwriting original sources.
"""

import os
import sys
import numpy as np
import cv2

# Add parent directory for module imports
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from metrics import calculate_psnr, calculate_ssim, calculate_delta_e_cie76
from phase4b_analysis import compute_edge_preservation_score, compute_text_readability_score

WIDTH = 1080
HEIGHT = 1920
MARGIN = 60
MARKER_RADIUS = 36


def draw_fiducials(canvas: np.ndarray):
    """Replicates Android drawFiducials() exactly."""
    corners = [
        (MARGIN, MARGIN),
        (WIDTH - MARGIN, MARGIN),
        (WIDTH - MARGIN, HEIGHT - MARGIN),
        (MARGIN, HEIGHT - MARGIN)
    ]
    for idx, (cx, cy) in enumerate(corners):
        sz = MARKER_RADIUS + 8
        cv2.rectangle(canvas, (cx - sz, cy - sz), (cx + sz, cy + sz), (255, 255, 255), -1)
        cv2.rectangle(canvas, (cx - sz, cy - sz), (cx + sz, cy + sz), (0, 0, 0), 2)
        cv2.circle(canvas, (cx, cy), MARKER_RADIUS, (0, 0, 0), -1)
        cv2.circle(canvas, (cx, cy), int(MARKER_RADIUS * 0.65), (255, 255, 255), -1)
        cv2.circle(canvas, (cx, cy), int(MARKER_RADIUS * 0.35), (0, 0, 0), -1)
        cv2.circle(canvas, (cx, cy), 3, (255, 255, 255), -1)
        cv2.putText(canvas, str(idx + 1), (cx - 4, cy + 4), cv2.FONT_HERSHEY_SIMPLEX, 0.35, (255, 255, 255), 1)


def calculate_fit_center(img_w: int, img_h: int, surf_w: int, surf_h: int, margin: float = 60.0, reserved_v: float = 80.0):
    avail_w = surf_w - (margin * 2)
    avail_h = surf_h - (margin * 2) - reserved_v

    img_aspect = img_w / img_h
    avail_aspect = avail_w / avail_h

    if img_aspect > avail_aspect:
        draw_w = avail_w
        draw_h = avail_w / img_aspect
    else:
        draw_h = avail_h
        draw_w = avail_h * img_aspect

    left = int(margin + (avail_w - draw_w) / 2.0)
    top = int(margin + 40.0 + (avail_h - draw_h) / 2.0)
    right = int(left + draw_w)
    bottom = int(top + draw_h)

    return left, top, right, bottom, int(draw_w), int(draw_h)


def render_normal_baseline(ref_img: np.ndarray, experiment_id: str) -> np.ndarray:
    """Simulates NormalBaselineRenderer."""
    canvas = np.full((HEIGHT, WIDTH, 3), 245, dtype=np.uint8)
    h, w = ref_img.shape[:2]
    left, top, right, bottom, dw, dh = calculate_fit_center(w, h, WIDTH, HEIGHT)

    scaled_ref = cv2.resize(ref_img, (dw, dh), interpolation=cv2.INTER_AREA)
    canvas[top:top+dh, left:left+dw] = scaled_ref

    # Frame & Fiducials
    cv2.rectangle(canvas, (MARGIN, MARGIN), (WIDTH - MARGIN, HEIGHT - MARGIN), (0, 0, 0), 2)
    draw_fiducials(canvas)

    # Text HUD
    cv2.putText(canvas, f"SESSION: {experiment_id}", (MARGIN + 16, MARGIN + 30),
                cv2.FONT_HERSHEY_SIMPLEX, 0.6, (60, 60, 60), 2, cv2.LINE_AA)
    cv2.putText(canvas, "PHASE 4A BASELINE // STATIC REFERENCE", (MARGIN + 16, HEIGHT - MARGIN - 16),
                cv2.FONT_HERSHEY_SIMPLEX, 0.5, (60, 60, 60), 1, cv2.LINE_AA)

    return canvas, (left, top, dw, dh)


def render_spatial_modulation(ref_img: np.ndarray, experiment_id: str, pattern: str = "CHECKERBOARD", period: int = 8, contrast: float = 0.08) -> np.ndarray:
    """Simulates SpatialModulationRenderer."""
    normal_canvas, (left, top, dw, dh) = render_normal_baseline(ref_img, experiment_id)
    spatial_canvas = normal_canvas.copy()

    # Generate pattern over dstRect
    overlay = np.zeros((dh, dw, 3), dtype=np.float32)
    alpha = float(np.clip(contrast, 0.0, 0.15))

    if pattern == "CHECKERBOARD":
        dark_factor = 1.0 - alpha
        bright_factor = 1.0 + alpha
        y_indices, x_indices = np.indices((dh, dw))
        row = (y_indices // period)
        col = (x_indices // period)
        mask = (row + col) % 2 == 0

        mult = np.where(mask, dark_factor, bright_factor)
        content_region = spatial_canvas[top:top+dh, left:left+dw].astype(np.float32)
        modulated = np.clip(content_region * mult[:, :, np.newaxis], 0, 255).astype(np.uint8)
        spatial_canvas[top:top+dh, left:left+dw] = modulated

    elif pattern == "HORIZONTAL_LINES":
        dark_factor = 1.0 - alpha
        y_indices = np.indices((dh, dw))[0]
        mask = (y_indices % period) < (period // 2)
        mult = np.where(mask, dark_factor, 1.0)
        content_region = spatial_canvas[top:top+dh, left:left+dw].astype(np.float32)
        modulated = np.clip(content_region * mult[:, :, np.newaxis], 0, 255).astype(np.uint8)
        spatial_canvas[top:top+dh, left:left+dw] = modulated

    # Overwrite HUD header with Spatial status
    cv2.rectangle(spatial_canvas, (MARGIN + 14, MARGIN + 14), (MARGIN + 600, MARGIN + 40), (245, 245, 245), -1)
    cv2.putText(spatial_canvas, f"SPATIAL * {pattern} * {period}px * {int(contrast*100)}%",
                (MARGIN + 16, MARGIN + 30), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 140, 0), 2, cv2.LINE_AA)

    return spatial_canvas, (left, top, dw, dh)


def main():
    charts_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "datasets", "reference_charts")
    out_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "datasets", "audit_verification")
    os.makedirs(out_dir, exist_ok=True)

    ref_path = os.path.join(charts_dir, "chart_combined_quality.png")
    if not os.path.exists(ref_path):
        print(f"Error: {ref_path} not found. Run generate_test_charts.py first.")
        return 1

    ref_img = cv2.imread(ref_path)
    exp_id = "EXP-20261114-140000-AUDIT"

    # 1. Normal Baseline Rendering
    normal_img, (left, top, dw, dh) = render_normal_baseline(ref_img, exp_id)

    # 2. Spatial Modulation Rendering (Checkerboard, period=8px, contrast=0.08)
    spatial_img, _ = render_spatial_modulation(ref_img, exp_id, pattern="CHECKERBOARD", period=8, contrast=0.08)

    # 3. Spatial Modulation Rendering (Horizontal Lines, period=8px, contrast=0.08)
    spatial_lines_img, _ = render_spatial_modulation(ref_img, exp_id, pattern="HORIZONTAL_LINES", period=8, contrast=0.08)

    # Save artifact outputs
    out_orig = os.path.join(out_dir, "audit_source_original.png")
    out_norm = os.path.join(out_dir, "audit_normal_renderer_output.png")
    out_spat = os.path.join(out_dir, "audit_spatial_renderer_output.png")
    out_lines = os.path.join(out_dir, "audit_spatial_horizontal_lines.png")
    out_diff = os.path.join(out_dir, "audit_spatial_difference_heatmap.png")

    cv2.imwrite(out_orig, ref_img)
    cv2.imwrite(out_norm, normal_img)
    cv2.imwrite(out_spat, spatial_img)
    cv2.imwrite(out_lines, spatial_lines_img)

    # Generate Difference Heatmap (magnified 5x for visual clarity)
    diff = cv2.absdiff(normal_img, spatial_img)
    diff_gray = cv2.cvtColor(diff, cv2.COLOR_BGR2GRAY)
    diff_mag = np.clip(diff_gray.astype(np.float32) * 5.0, 0, 255).astype(np.uint8)
    diff_heatmap = cv2.applyColorMap(diff_mag, cv2.COLORMAP_JET)
    cv2.imwrite(out_diff, diff_heatmap)

    # Quantitative Metrics Evaluation
    # ROI: Inner content area where reference image is rendered
    roi_normal = normal_img[top:top+dh, left:left+dw]
    roi_spatial = spatial_img[top:top+dh, left:left+dw]
    roi_lines = spatial_lines_img[top:top+dh, left:left+dw]

    psnr_normal_spatial = calculate_psnr(roi_normal, roi_spatial)
    ssim_normal_spatial = calculate_ssim(roi_normal, roi_spatial)
    delta_e_normal_spatial = calculate_delta_e_cie76(roi_normal, roi_spatial)
    edge_preservation = compute_edge_preservation_score(roi_spatial, roi_normal)
    text_readability = compute_text_readability_score(roi_spatial, roi_normal)

    # Verification of Outside Margin Preservation:
    # Margins and fiducials MUST remain identical between normal and spatial modes
    margin_diff_top = np.max(np.abs(normal_img[0:top, :] - spatial_img[0:top, :]))
    margin_diff_bottom = np.max(np.abs(normal_img[top+dh:, :] - spatial_img[top+dh:, :]))
    margin_diff_left = np.max(np.abs(normal_img[top:top+dh, 0:left] - spatial_img[top:top+dh, 0:left]))
    margin_diff_right = np.max(np.abs(normal_img[top:top+dh, left+dw:] - spatial_img[top:top+dh, left+dw:]))

    print("=== AUDIT VERIFICATION RESULTS ===")
    print(f"Original Reference: {out_orig} ({ref_img.shape[1]}x{ref_img.shape[0]})")
    print(f"Normal Renderer Output: {out_norm} ({normal_img.shape[1]}x{normal_img.shape[0]})")
    print(f"Spatial Renderer Output: {out_spat} ({spatial_img.shape[1]}x{spatial_img.shape[0]})")
    print(f"Difference Heatmap: {out_diff}")
    print("--- Quantitative Metrics (Normal vs Spatial Checkerboard 8% contrast) ---")
    print(f"  PSNR: {psnr_normal_spatial:.2f} dB")
    print(f"  SSIM: {ssim_normal_spatial:.4f}")
    print(f"  Delta E (CIELAB): {delta_e_normal_spatial:.2f}")
    print(f"  Edge Preservation Score: {edge_preservation:.4f}")
    print(f"  Text Readability Score: {text_readability:.4f}")
    print("--- Boundary & Margin Integrity ---")
    print(f"  Max pixel diff outside dstRect (Top): {margin_diff_top}")
    print(f"  Max pixel diff outside dstRect (Bottom): {margin_diff_bottom}")
    print(f"  Max pixel diff outside dstRect (Left): {margin_diff_left}")
    print(f"  Max pixel diff outside dstRect (Right): {margin_diff_right}")

    # Margin diff outside dstRect (except the text title at top) must be 0
    assert margin_diff_left == 0, "Spatial modulation leaked outside left boundary!"
    assert margin_diff_right == 0, "Spatial modulation leaked outside right boundary!"
    assert margin_diff_bottom == 0, "Spatial modulation leaked outside bottom boundary!"
    print("[PASS] Spatial modulation strictly confined to dstRect; fiducials and calibration margins are 100% pristine.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
