#!/usr/bin/env python3
"""
report_generator.py - Scientific Laboratory Report and Visualizer Generator

Creates structured experiment outputs:
1. results.json: Full machine-readable experiment dataset
2. results.csv: Tabular metric summary
3. comparison.png: 4-panel visual comparison (Reference | Raw Capture | Aligned | Heatmap)
4. report.md: Formatted Markdown laboratory report
"""

import os
import json
import csv
from typing import Dict, Any, List
import numpy as np
import cv2


def generate_comparison_image(
    reference_bgr: np.ndarray,
    raw_capture_bgr: np.ndarray,
    aligned_bgr: np.ndarray,
    output_path: str
) -> str:
    """
    Generates a 4-panel diagnostic comparison image:
    [Reference Chart] [Raw Camera Capture] [Perspective-Aligned] [Difference Heatmap]
    """
    target_h = 720
    aspect = reference_bgr.shape[1] / reference_bgr.shape[0]
    target_w = int(target_h * aspect)
    
    # 1. Scaled reference
    ref_scaled = cv2.resize(reference_bgr, (target_w, target_h), interpolation=cv2.INTER_AREA)
    
    # 2. Scaled raw capture
    raw_aspect = raw_capture_bgr.shape[1] / raw_capture_bgr.shape[0]
    raw_w = int(target_h * raw_aspect)
    raw_scaled = cv2.resize(raw_capture_bgr, (raw_w, target_h), interpolation=cv2.INTER_AREA)
    # Center crop or pad raw to target_w for uniform layout
    if raw_w > target_w:
        crop_start = (raw_w - target_w) // 2
        raw_scaled = raw_scaled[:, crop_start:crop_start + target_w]
    elif raw_w < target_w:
        pad_left = (target_w - raw_w) // 2
        pad_right = target_w - raw_w - pad_left
        raw_scaled = cv2.copyMakeBorder(raw_scaled, 0, 0, pad_left, pad_right, cv2.BORDER_CONSTANT, value=(40, 40, 40))
        
    # 3. Scaled aligned capture
    aligned_scaled = cv2.resize(aligned_bgr, (target_w, target_h), interpolation=cv2.INTER_AREA)
    
    # 4. Difference Heatmap
    diff_gray = cv2.cvtColor(
        cv2.absdiff(ref_scaled, aligned_scaled), cv2.COLOR_BGR2GRAY
    )
    # Amplify difference for visual clarity (5x gain)
    diff_amplified = cv2.multiply(diff_gray, 5)
    heatmap = cv2.applyColorMap(diff_amplified, cv2.COLORMAP_INFERNO)
    
    # Add title headers to each panel
    header_h = 40
    panels = [
        ("Reference Chart (Phone A)", ref_scaled),
        ("Raw Capture (Phone B)", raw_scaled),
        ("Perspective Aligned", aligned_scaled),
        ("Error Heatmap (5x Gain)", heatmap)
    ]
    
    titled_panels = []
    for title, panel in panels:
        header = np.full((header_h, target_w, 3), 30, dtype=np.uint8)
        cv2.putText(header, title, (16, 26), cv2.FONT_HERSHEY_SIMPLEX, 0.55, (255, 255, 255), 1, cv2.LINE_AA)
        titled_panels.append(np.vstack([header, panel]))
        
    composite = np.hstack(titled_panels)
    
    # Master top banner
    banner_h = 50
    banner = np.full((banner_h, composite.shape[1], 3), 15, dtype=np.uint8)
    cv2.putText(
        banner,
        "VEIL OPTICAL RESEARCH LAB // PHASE 4A BASELINE CAPTURE DIAGNOSTIC",
        (24, 32),
        cv2.FONT_HERSHEY_SIMPLEX,
        0.7,
        (0, 220, 255),
        2,
        cv2.LINE_AA
    )
    final_img = np.vstack([banner, composite])
    
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    cv2.imwrite(output_path, final_img)
    return output_path


def generate_markdown_report(
    manifest: Dict[str, Any],
    analysis_results: Dict[str, Any],
    output_path: str
) -> str:
    """Generates an extensive, publication-ready Markdown research report."""
    exp_id = manifest.get("experimentId", "UNKNOWN_EXP")
    timestamp = manifest.get("timestamp", "N/A")
    mode = manifest.get("testMode", "baseline")
    
    phone_a = manifest.get("phoneA", {})
    phone_b = manifest.get("phoneB", {})
    source_img = manifest.get("sourceImage", {})
    
    summary = analysis_results.get("summary", {})
    captures = analysis_results.get("captures", [])
    
    md_lines = [
        f"# Veil Optical Research Lab — Experiment Report: `{exp_id}`",
        "",
        f"**Date/Time**: `{timestamp}`  ",
        f"**Experiment Mode**: `{mode.upper()}`  ",
        f"**Source Test Asset**: `{source_img.get('name', 'N/A')}` ({source_img.get('width', 0)}x{source_img.get('height', 0)} px)  ",
        f"**Asset Hash (SHA-256)**: `{source_img.get('hash_sha256', 'N/A')[:16]}...`",
        "",
        "---",
        "",
        "## 1. Executive Summary & Aggregate Findings",
        "",
        "This laboratory report documents an optical capture session conducted under the **Veil Phase 4A Baseline Capture Protocol**.",
        "The baseline protocol establishes normal photographic capture fidelity without active display modulation.",
        "",
        "| Metric | Mean Value | Std Dev | Min | Max | Interpretation |",
        "|---|---|---|---|---|---|",
        f"| **PSNR (dB)** | `{summary.get('psnr_mean', 0.0):.2f}` | `{summary.get('psnr_std', 0.0):.2f}` | `{summary.get('psnr_min', 0.0):.2f}` | `{summary.get('psnr_max', 0.0):.2f}` | Higher = closer photographic fidelity |",
        f"| **SSIM** | `{summary.get('ssim_mean', 0.0):.4f}` | `{summary.get('ssim_std', 0.0):.4f}` | `{summary.get('ssim_min', 0.0):.4f}` | `{summary.get('ssim_max', 0.0):.4f}` | Structural similarity index [0 to 1] |",
        f"| **RMSE** | `{summary.get('rmse_mean', 0.0):.2f}` | `{summary.get('rmse_std', 0.0):.2f}` | `{summary.get('rmse_min', 0.0):.2f}` | `{summary.get('rmse_max', 0.0):.2f}` | Root-mean-squared luminance error |",
        f"| **Delta E (CIE76)** | `{summary.get('delta_e_mean', 0.0):.2f}` | `{summary.get('delta_e_std', 0.0):.2f}` | `{summary.get('delta_e_min', 0.0):.2f}` | `{summary.get('delta_e_max', 0.0):.2f}` | Color deviation in LAB space |",
        "",
        "> [!NOTE]",
        "> **Scientific Metric Notice**: PSNR and SSIM measure signal difference relative to the digital master. They do not constitute a direct metric of human legibility or privacy degradation. Low similarity in Phase 4B will require corroborating human readability tests.",
        "",
        "---",
        "",
        "## 2. Experimental Apparatus & Runtime Environment",
        "",
        "### Phone A (Display Device)",
        f"- **Device**: `{phone_a.get('manufacturer', 'Unknown')} {phone_a.get('model', 'Unknown')}`",
        f"- **Operating System**: Android {phone_a.get('androidVersion', 'Unknown')} (API {phone_a.get('apiLevel', 'N/A')})",
        f"- **Native Resolution**: `{phone_a.get('displayResolution', 'Unknown')}`",
        f"- **Active Refresh Rate**: `{phone_a.get('refreshRateHz', 'N/A')} Hz`",
        f"- **Supported Display Modes**: `{phone_a.get('supportedRefreshRates', [])}`",
        f"- **Screen Brightness**: `{phone_a.get('screenBrightnessPercent', 'N/A')}%` (Adaptive: `{phone_a.get('adaptiveBrightness', 'N/A')}`)",
        f"- **Orientation**: `{phone_a.get('screenOrientation', 'PORTRAIT')}`",
        "",
        "### Phone B (Camera Capture Device)",
        f"- **Device**: `{phone_b.get('manufacturer', 'Unknown')} {phone_b.get('model', 'Unknown')}`",
        f"- **Camera Resolution**: `{phone_b.get('cameraResolution', 'Unknown')}`",
        f"- **Capture Mode**: `{phone_b.get('captureMode', 'Unknown')}`",
        f"- **Distance from Phone A**: `{phone_b.get('distanceCm', 'N/A')} cm`",
        f"- **Camera Angle**: `{phone_b.get('cameraAngleDegrees', 'N/A')}°`",
        f"- **Ambient Illumination**: `{phone_b.get('ambientLighting', 'Ambient Room Lighting')}`",
        f"- **Exposure Time**: `{phone_b.get('exposureTimeSeconds', 'Auto / Unknown')}`",
        f"- **ISO Sensitivity**: `{phone_b.get('iso', 'Auto / Unknown')}`",
        f"- **White Balance**: `{phone_b.get('whiteBalance', 'Auto / Unknown')}`",
        "",
        "---",
        "",
        "## 3. Repetition Breakdown & Alignment Diagnostics",
        "",
        "| Rep # | Capture File | Alignment Method | Rotation | Scale Factor | PSNR (dB) | SSIM | Delta E |",
        "|---|---|---|---|---|---|---|---|"
    ]
    
    for cap in captures:
        num = cap.get("repetition", 1)
        fname = cap.get("filename", "N/A")
        align = cap.get("alignment", {})
        m = cap.get("metrics", {})
        md_lines.append(
            f"| `{num}` | `{fname}` | `{align.get('method', 'N/A')}` | `{align.get('rotation_degrees', 0.0):.2f}°` | `{align.get('scale_factor', 1.0):.3f}x` | `{m.get('psnr_db', 0.0):.2f}` | `{m.get('ssim', 0.0):.4f}` | `{m.get('delta_e_cie76', 0.0):.2f}` |"
        )
        
    md_lines.extend([
        "",
        "---",
        "",
        "## 4. Visual Comparison Artifact",
        "",
        "![Comparison Diagnostic](comparison.png)",
        "",
        "---",
        "",
        "## 5. Notes & Observations",
        "",
        f"> {manifest.get('notes', 'No qualitative session notes recorded.')}",
        "",
        "## 6. Phase 4A Baseline Status & Readiness for Phase 4B",
        "- **Baseline Established**: This session provides a verified reference dataset for normal camera photography.",
        "- **Next Step**: When Phase 4B modulation (spatial frequency grids or temporal VSYNC modulation) is introduced, the resulting captures will be processed through this exact pipeline to calculate relative degradation $(\\Delta PSNR, \\Delta SSIM)$ against this baseline."
    ])
    
    content = "\n".join(md_lines)
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    with open(output_path, "w", encoding="utf-8") as f:
        f.write(content)
    return output_path


def generate_csv_summary(analysis_results: Dict[str, Any], output_path: str) -> str:
    """Writes a flat CSV file containing all capture records for easy aggregation."""
    captures = analysis_results.get("captures", [])
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    
    fields = [
        "experiment_id", "repetition", "filename", "alignment_method",
        "rotation_deg", "scale_factor", "psnr_db", "ssim", "rmse",
        "mae", "delta_e_cie76", "roi_psnr_db", "roi_ssim"
    ]
    
    with open(output_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(fields)
        for c in captures:
            align = c.get("alignment", {})
            m = c.get("metrics", {})
            writer.writerow([
                analysis_results.get("experimentId", ""),
                c.get("repetition", 1),
                c.get("filename", ""),
                align.get("method", ""),
                round(align.get("rotation_degrees", 0.0), 2),
                round(align.get("scale_factor", 1.0), 3),
                m.get("psnr_db", ""),
                m.get("ssim", ""),
                m.get("rmse", ""),
                m.get("mae", ""),
                m.get("delta_e_cie76", ""),
                m.get("roi_psnr_db", ""),
                m.get("roi_ssim", "")
            ])
    return output_path
