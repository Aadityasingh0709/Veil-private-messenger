#!/usr/bin/env python3
"""
test_pipeline.py - End-to-End Pipeline Tests for Optical Capture Analysis
"""

import sys
import os
import json
import zipfile
import tempfile
import numpy as np
import cv2
import pytest

# Ensure parent directory is in sys.path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from analyze_experiment import process_experiment, load_and_validate_manifest
from test_alignment import create_synthetic_target_with_fiducials


def create_mock_experiment_package(temp_dir: str, num_captures: int = 2) -> str:
    """Helper creating a complete mock experiment directory with synthetic captures."""
    exp_id = "EXP-20261103-120000-TEST01"
    exp_dir = os.path.join(temp_dir, exp_id)
    os.makedirs(os.path.join(exp_dir, "source"), exist_ok=True)
    os.makedirs(os.path.join(exp_dir, "captures"), exist_ok=True)
    
    # Generate reference chart
    w, h = 600, 800
    margin = 60
    ref_img, corners = create_synthetic_target_with_fiducials(w, h, margin)
    ref_path = os.path.join(exp_dir, "source", "reference.png")
    cv2.imwrite(ref_path, ref_img)
    
    # Generate mock captures (simulating slight photographic degradation / noise)
    captures_meta = []
    for i in range(1, num_captures + 1):
        # Slightly alter capture to simulate real optical acquisition
        noisy = cv2.GaussianBlur(ref_img, (3, 3), 0.5)
        cap_name = f"capture_{i:03d}.jpg"
        cap_path = os.path.join(exp_dir, "captures", cap_name)
        cv2.imwrite(cap_path, noisy, [cv2.IMWRITE_JPEG_QUALITY, 92])
        
        captures_meta.append({
            "filename": cap_name,
            "hash_sha256": "0" * 64,
            "repetitionNumber": i,
            "timestamp": "2026-11-03T12:00:00Z",
            "exif": {
                "make": "Google",
                "model": "Pixel 8",
                "focalLength": "6.9mm",
                "fNumber": "1.7",
                "exposureTime": "1/120",
                "iso": 100
            }
        })
        
    manifest = {
        "schemaVersion": "1.0.0",
        "experimentId": exp_id,
        "timestamp": "2026-11-03T12:00:00Z",
        "testMode": "baseline",
        "sourceImage": {
            "name": "reference.png",
            "hash_sha256": "0" * 64,
            "width": w,
            "height": h
        },
        "phoneA": {
            "manufacturer": "Xiaomi",
            "model": "2312DRAABI",
            "androidVersion": "15",
            "apiLevel": 35,
            "displayResolution": f"{w}x{h}",
            "refreshRateHz": 120.0,
            "supportedRefreshRates": [60.0, 120.0],
            "screenBrightnessPercent": 85.0,
            "adaptiveBrightness": False,
            "screenOrientation": "PORTRAIT"
        },
        "phoneB": {
            "manufacturer": "Google",
            "model": "Pixel 8",
            "cameraResolution": "12MP",
            "exposureTimeSeconds": 0.0083,
            "iso": 100,
            "focusMode": "FIXED",
            "whiteBalance": "5500K",
            "distanceCm": 40.0,
            "cameraAngleDegrees": 90.0,
            "ambientLighting": "Darkened lab (5 lux)",
            "captureMode": "manual"
        },
        "captures": captures_meta,
        "notes": "Automated pipeline validation mock session."
    }
    
    with open(os.path.join(exp_dir, "manifest.json"), "w", encoding="utf-8") as f:
        json.dump(manifest, f, indent=2)
        
    with open(os.path.join(exp_dir, "notes.txt"), "w", encoding="utf-8") as f:
        f.write("Test notes for baseline session.\n")
        
    return exp_dir


def test_manifest_schema_validation():
    """Manifest conforming to schema should validate without errors."""
    with tempfile.TemporaryDirectory() as temp_dir:
        exp_dir = create_mock_experiment_package(temp_dir)
        manifest_path = os.path.join(exp_dir, "manifest.json")
        schema_path = os.path.abspath(
            os.path.join(os.path.dirname(__file__), "..", "schemas", "manifest.schema.json")
        )
        manifest = load_and_validate_manifest(manifest_path, schema_path)
        assert manifest["schemaVersion"] == "1.0.0"
        assert manifest["experimentId"] == "EXP-20261103-120000-TEST01"


def test_end_to_end_directory_processing():
    """End-to-end processing of experiment directory must generate all required deliverables."""
    with tempfile.TemporaryDirectory() as temp_dir:
        exp_dir = create_mock_experiment_package(temp_dir)
        output_dir = os.path.join(temp_dir, "analysis_output")
        
        results = process_experiment(input_path=exp_dir, output_dir=output_dir)
        
        # Verify returned structure
        assert results["experimentId"] == "EXP-20261103-120000-TEST01"
        assert results["summary"]["capture_count"] == 2
        assert results["summary"]["psnr_mean"] > 20.0
        assert results["summary"]["ssim_mean"] > 0.8
        
        # Verify generated files on disk
        assert os.path.exists(os.path.join(output_dir, "results.json"))
        assert os.path.exists(os.path.join(output_dir, "results.csv"))
        assert os.path.exists(os.path.join(output_dir, "comparison.png"))
        assert os.path.exists(os.path.join(output_dir, "report.md"))
        assert os.path.exists(os.path.join(output_dir, "aligned_capture_001.jpg"))
        assert os.path.exists(os.path.join(output_dir, "aligned_capture_002.jpg"))


def test_end_to_end_zip_archive_processing():
    """End-to-end processing of a zipped experiment archive must succeed cleanly."""
    with tempfile.TemporaryDirectory() as temp_dir:
        exp_dir = create_mock_experiment_package(temp_dir)
        zip_path = os.path.join(temp_dir, "experiment_package.zip")
        
        # Create ZIP
        with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED) as zf:
            for root, _, files in os.walk(exp_dir):
                for f in files:
                    full_p = os.path.join(root, f)
                    rel_p = os.path.relpath(full_p, exp_dir)
                    zf.write(full_p, rel_p)
                    
        output_dir = os.path.join(temp_dir, "zip_analysis_output")
        results = process_experiment(input_path=zip_path, output_dir=output_dir)
        
        assert results["experimentId"] == "EXP-20261103-120000-TEST01"
        assert os.path.exists(os.path.join(output_dir, "report.md"))
        assert os.path.exists(os.path.join(output_dir, "results.json"))
