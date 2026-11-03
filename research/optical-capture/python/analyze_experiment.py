#!/usr/bin/env python3
"""
analyze_experiment.py - Automated Optical Capture Analysis CLI Tool

Accepts an exported experiment package (.zip or directory) from the Veil Optical Research Lab,
validates manifest integrity, aligns captured photos with perspective correction,
calculates baseline PSNR/SSIM metrics, and generates structured reports.

Usage:
  python analyze_experiment.py --input <path_to_zip_or_dir> [--output <output_dir>] [--manual-corners x1,y1,x2,y2,x3,y3,x4,y4]
"""

import os
import sys
import argparse
import json
import zipfile
import tempfile
import hashlib
from typing import Dict, Any, List, Optional, Tuple
import numpy as np
import cv2
import jsonschema

from image_alignment import align_capture_to_reference, AlignmentResult
from metrics import evaluate_capture_quality, ImageMetrics
from report_generator import generate_comparison_image, generate_markdown_report, generate_csv_summary


def compute_sha256(filepath: str) -> str:
    """Computes SHA-256 hash of a file."""
    h = hashlib.sha256()
    with open(filepath, "rb") as f:
        while chunk := f.read(65536):
            h.update(chunk)
    return h.hexdigest()


def load_and_validate_manifest(manifest_path: str, schema_path: str) -> Dict[str, Any]:
    """Loads manifest.json and validates against manifest.schema.json."""
    if not os.path.exists(manifest_path):
        raise FileNotFoundError(f"Missing required manifest file: {manifest_path}")
        
    with open(manifest_path, "r", encoding="utf-8") as f:
        try:
            manifest = json.load(f)
        except json.JSONDecodeError as e:
            raise ValueError(f"Corrupt manifest.json file: {e}")
            
    if os.path.exists(schema_path):
        with open(schema_path, "r", encoding="utf-8") as sf:
            schema = json.load(sf)
        try:
            jsonschema.validate(instance=manifest, schema=schema)
        except jsonschema.ValidationError as ve:
            print(f"[WARNING] Manifest schema validation warning: {ve.message}")
            
    return manifest


def parse_corners_arg(corners_str: Optional[str]) -> Optional[List[Tuple[float, float]]]:
    """Parses 'x1,y1,x2,y2,x3,y3,x4,y4' string into a list of 4 coordinate tuples."""
    if not corners_str:
        return None
    parts = [float(p.strip()) for p in corners_str.split(",")]
    if len(parts) != 8:
        raise ValueError("Manual corners must contain exactly 8 numbers (x1,y1,x2,y2,x3,y3,x4,y4)")
    return [(parts[0], parts[1]), (parts[2], parts[3]), (parts[4], parts[5]), (parts[6], parts[7])]


def process_experiment(
    input_path: str,
    output_dir: Optional[str] = None,
    manual_corners: Optional[List[Tuple[float, float]]] = None
) -> Dict[str, Any]:
    """Processes an exported experiment package and generates all analysis deliverables."""
    temp_extract_dir = None
    
    # 1. Unpack ZIP if input is an archive
    if os.path.isfile(input_path) and input_path.lower().endswith(".zip"):
        temp_extract_dir = tempfile.mkdtemp(prefix="veil_optical_exp_")
        print(f"[INFO] Extracting experiment ZIP: {input_path} -> {temp_extract_dir}")
        with zipfile.ZipFile(input_path, "r") as zf:
            zf.extractall(temp_extract_dir)
        exp_root = temp_extract_dir
    elif os.path.isdir(input_path):
        exp_root = input_path
    else:
        raise FileNotFoundError(f"Input path does not exist or is not a ZIP/directory: {input_path}")
        
    # Check if files are nested inside a subfolder (e.g. experiment_001/manifest.json)
    manifest_path = os.path.join(exp_root, "manifest.json")
    if not os.path.exists(manifest_path):
        subdirs = [d for d in os.listdir(exp_root) if os.path.isdir(os.path.join(exp_root, d))]
        for sub in subdirs:
            candidate = os.path.join(exp_root, sub, "manifest.json")
            if os.path.exists(candidate):
                exp_root = os.path.join(exp_root, sub)
                manifest_path = candidate
                break

    # 2. Schema validation
    base_dir = os.path.dirname(os.path.abspath(__file__))
    schema_path = os.path.join(base_dir, "schemas", "manifest.schema.json")
    manifest = load_and_validate_manifest(manifest_path, schema_path)
    exp_id = manifest.get("experimentId", "UNKNOWN_EXP")
    print(f"[INFO] Loaded valid experiment manifest for: {exp_id}")
    
    # 3. Locate reference image
    source_info = manifest.get("sourceImage", {})
    source_name = source_info.get("name", "reference.png")
    
    # Check candidate locations for reference image
    ref_candidates = [
        os.path.join(exp_root, "source", source_name),
        os.path.join(exp_root, "source", "reference.png"),
        os.path.join(exp_root, source_name),
        os.path.join(base_dir, "..", "datasets", "reference_charts", source_name)
    ]
    ref_path = None
    for cand in ref_candidates:
        if os.path.exists(cand):
            ref_path = cand
            break
            
    if not ref_path:
        raise FileNotFoundError(f"Reference image '{source_name}' could not be located in experiment.")
        
    ref_img = cv2.imread(ref_path)
    if ref_img is None:
        raise ValueError(f"Failed to decode reference image at: {ref_path}")
        
    ref_h, ref_w = ref_img.shape[:2]
    print(f"[INFO] Loaded reference image: {ref_path} ({ref_w}x{ref_h} px)")
    
    # Standard reference fiducial markers (margin 60px)
    margin = 60
    ref_corners = [
        (float(margin), float(margin)),
        (float(ref_w - margin), float(margin)),
        (float(ref_w - margin), float(ref_h - margin)),
        (float(margin), float(ref_h - margin))
    ]
    
    # 4. Prepare output destination
    if not output_dir:
        output_dir = os.path.join(base_dir, "..", "reports", exp_id)
    os.makedirs(output_dir, exist_ok=True)
    
    # 5. Process captures
    captures_meta = manifest.get("captures", [])
    if not captures_meta:
        raise ValueError(f"Experiment {exp_id} contains no capture entries in manifest.")
        
    captures_dir = os.path.join(exp_root, "captures")
    processed_captures = []
    
    psnr_list = []
    ssim_list = []
    rmse_list = []
    delta_e_list = []
    
    first_aligned = None
    first_raw = None
    
    for cap in captures_meta:
        fname = cap.get("filename")
        rep_num = cap.get("repetitionNumber", 1)
        cap_file = os.path.join(captures_dir, fname) if os.path.exists(os.path.join(captures_dir, fname)) else os.path.join(exp_root, fname)
        
        if not os.path.exists(cap_file):
            print(f"[WARNING] Capture file not found: {cap_file}. Skipping.")
            continue
            
        cap_img = cv2.imread(cap_file)
        if cap_img is None:
            print(f"[WARNING] Could not decode capture file: {cap_file}. Skipping.")
            continue
            
        print(f"[INFO] Aligning capture #{rep_num}: {fname} ({cap_img.shape[1]}x{cap_img.shape[0]} px)")
        align_result: AlignmentResult = align_capture_to_reference(
            captured_img=cap_img,
            ref_width=ref_w,
            ref_height=ref_h,
            ref_corners=ref_corners,
            manual_corners=manual_corners
        )
        
        if not align_result.success or align_result.aligned_image is None:
            print(f"[ERROR] Failed to align capture #{rep_num}: {align_result.warnings}")
            continue
            
        # Save aligned capture
        aligned_fname = f"aligned_{fname}"
        aligned_out_path = os.path.join(output_dir, aligned_fname)
        cv2.imwrite(aligned_out_path, align_result.aligned_image)
        
        # Calculate similarity metrics
        metrics: ImageMetrics = evaluate_capture_quality(ref_img, align_result.aligned_image)
        print(f"  -> Metrics: PSNR = {metrics.psnr_db:.2f} dB, SSIM = {metrics.ssim:.4f}, Delta E = {metrics.delta_e_cie76:.2f}")
        
        psnr_list.append(metrics.psnr_db)
        ssim_list.append(metrics.ssim)
        rmse_list.append(metrics.rmse)
        delta_e_list.append(metrics.delta_e_cie76)
        
        if first_aligned is None:
            first_aligned = align_result.aligned_image
            first_raw = cap_img
            
        processed_captures.append({
            "repetition": rep_num,
            "filename": fname,
            "aligned_filename": aligned_fname,
            "alignment": {
                "method": align_result.method,
                "rotation_degrees": round(align_result.rotation_degrees, 2),
                "scale_factor": round(align_result.scale_factor, 4),
                "detected_corners": align_result.detected_corners,
                "transform_matrix": align_result.transform_matrix,
                "warnings": align_result.warnings
            },
            "metrics": metrics.to_dict()
        })
        
    if not processed_captures:
        raise RuntimeError("No captures could be aligned or evaluated successfully.")
        
    # 6. Aggregate statistics
    summary = {
        "capture_count": len(processed_captures),
        "psnr_mean": float(np.mean(psnr_list)),
        "psnr_std": float(np.std(psnr_list)) if len(psnr_list) > 1 else 0.0,
        "psnr_min": float(np.min(psnr_list)),
        "psnr_max": float(np.max(psnr_list)),
        "ssim_mean": float(np.mean(ssim_list)),
        "ssim_std": float(np.std(ssim_list)) if len(ssim_list) > 1 else 0.0,
        "ssim_min": float(np.min(ssim_list)),
        "ssim_max": float(np.max(ssim_list)),
        "rmse_mean": float(np.mean(rmse_list)),
        "rmse_std": float(np.std(rmse_list)) if len(rmse_list) > 1 else 0.0,
        "rmse_min": float(np.min(rmse_list)),
        "rmse_max": float(np.max(rmse_list)),
        "delta_e_mean": float(np.mean(delta_e_list)),
        "delta_e_std": float(np.std(delta_e_list)) if len(delta_e_list) > 1 else 0.0,
        "delta_e_min": float(np.min(delta_e_list)),
        "delta_e_max": float(np.max(delta_e_list))
    }
    
    complete_results = {
        "experimentId": exp_id,
        "timestamp": manifest.get("timestamp"),
        "testMode": manifest.get("testMode"),
        "sourceImage": source_info,
        "phoneA": manifest.get("phoneA"),
        "phoneB": manifest.get("phoneB"),
        "notes": manifest.get("notes"),
        "summary": summary,
        "captures": processed_captures
    }
    
    # 7. Generate Output Files
    results_json_path = os.path.join(output_dir, "results.json")
    with open(results_json_path, "w", encoding="utf-8") as f:
        json.dump(complete_results, f, indent=2)
    print(f"[INFO] Saved JSON results to: {results_json_path}")
    
    results_csv_path = os.path.join(output_dir, "results.csv")
    generate_csv_summary(complete_results, results_csv_path)
    print(f"[INFO] Saved CSV summary to: {results_csv_path}")
    
    comparison_png_path = os.path.join(output_dir, "comparison.png")
    if first_aligned is not None and first_raw is not None:
        generate_comparison_image(ref_img, first_raw, first_aligned, comparison_png_path)
        print(f"[INFO] Saved visual comparison diagnostic to: {comparison_png_path}")
        
    report_md_path = os.path.join(output_dir, "report.md")
    generate_markdown_report(manifest, complete_results, report_md_path)
    print(f"[INFO] Saved Markdown laboratory report to: {report_md_path}")
    
    # Cleanup temp dir if extracted
    if temp_extract_dir and os.path.exists(temp_extract_dir):
        import shutil
        shutil.rmtree(temp_extract_dir, ignore_errors=True)
        
    print(f"\n[SUCCESS] Optical Analysis Complete for {exp_id}!")
    print(f"  Mean PSNR: {summary['psnr_mean']:.2f} dB (+/- {summary['psnr_std']:.2f})")
    print(f"  Mean SSIM: {summary['ssim_mean']:.4f} (+/- {summary['ssim_std']:.4f})")
    return complete_results


def main():
    parser = argparse.ArgumentParser(description="Veil Optical Research Lab - Phase 4A Baseline Capture Analysis")
    parser.add_argument("--input", "-i", required=True, help="Path to exported experiment package (.zip or directory)")
    parser.add_argument("--output", "-o", default=None, help="Output directory for reports and results")
    parser.add_argument("--manual-corners", default=None, help="Optional manual 4-corner coordinates: x1,y1,x2,y2,x3,y3,x4,y4")
    
    args = parser.parse_args()
    corners = parse_corners_arg(args.manual_corners)
    
    try:
        process_experiment(input_path=args.input, output_dir=args.output, manual_corners=corners)
    except Exception as e:
        print(f"[FATAL ERROR] {e}", file=sys.stderr)
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    main()
