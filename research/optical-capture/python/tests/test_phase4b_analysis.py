#!/usr/bin/env python3
"""
test_phase4b_analysis.py - Unit Tests for Phase 4B Comparative Analysis Tools and Schema
"""

import sys
import os
import json
import numpy as np
import cv2
import pytest
import jsonschema

# Ensure parent directory is in sys.path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from phase4b_analysis import (
    compute_edge_preservation_score,
    compute_text_readability_score,
    compare_modulation_vs_baseline,
    ModulationEvaluation
)


def test_edge_preservation_identical():
    """Identical high-contrast edge targets must yield an edge score close to 1.0."""
    img = np.zeros((200, 200, 3), dtype=np.uint8)
    cv2.circle(img, (100, 100), 50, (255, 255, 255), -1)
    score = compute_edge_preservation_score(img, img)
    assert pytest.approx(score, abs=0.01) == 1.0


def test_edge_preservation_blurred():
    """Blurred captures must show reduced edge preservation compared to reference."""
    ref = np.zeros((200, 200, 3), dtype=np.uint8)
    cv2.rectangle(ref, (40, 40), (160, 160), (255, 255, 255), -1)

    blurred = cv2.GaussianBlur(ref, (15, 15), 5.0)
    score = compute_edge_preservation_score(blurred, ref)
    assert 0.0 < score < 0.90


def test_text_readability_identical():
    """Identical images must yield a readability score of 1.0."""
    img = np.full((200, 200, 3), 255, dtype=np.uint8)
    cv2.putText(img, "VEIL LAB", (30, 100), cv2.FONT_HERSHEY_SIMPLEX, 1.0, (0, 0, 0), 2)
    score = compute_text_readability_score(img, img)
    assert pytest.approx(score, abs=0.01) == 1.0


def test_text_readability_blurred():
    """Blurred text must reduce the high-frequency Laplacian variance ratio."""
    ref = np.full((200, 200, 3), 255, dtype=np.uint8)
    cv2.putText(ref, "TOP SECRET", (20, 110), cv2.FONT_HERSHEY_SIMPLEX, 0.9, (0, 0, 0), 2)

    blurred = cv2.GaussianBlur(ref, (21, 21), 7.0)
    score = compute_text_readability_score(blurred, ref)
    assert score < 0.50


def test_compare_modulation_vs_baseline_deltas():
    """Calculates correct differentials and degradation index."""
    baseline_metrics = {"psnr_db": 28.5, "ssim": 0.850}
    modulated_metrics = {"psnr_db": 24.0, "ssim": 0.680}

    eval_result = compare_modulation_vs_baseline(
        modulated_metrics=modulated_metrics,
        baseline_metrics=baseline_metrics,
        edge_preservation=0.72,
        text_readability=0.65,
        baseline_experiment_id="EXP-20261103-120000-NORMAL"
    )

    # ΔPSNR = 24.0 - 28.5 = -4.5 dB (degradation)
    assert eval_result.delta_psnr_db == -4.5
    # ΔSSIM = 0.680 - 0.850 = -0.170
    assert eval_result.delta_ssim == -0.170
    # Positive anti-capture degradation score
    assert eval_result.anti_capture_degradation_index > 0.0
    assert eval_result.lpips_status == "NOT_INSTALLED_TORCH_UNAVAILABLE"


def test_baseline_caveat_detection():
    """Flags known Phase 4A baseline caveats when EXP-001 or EXP-007 are referenced."""
    eval_exp001 = compare_modulation_vs_baseline(
        {"psnr_db": 25.0, "ssim": 0.70},
        {"psnr_db": 27.0, "ssim": 0.80},
        0.8, 0.8,
        baseline_experiment_id="EXP-001"
    )
    assert len(eval_exp001.provenance_caveats) >= 1
    assert "17.3%" in eval_exp001.provenance_caveats[0]

    eval_exp007 = compare_modulation_vs_baseline(
        {"psnr_db": 25.0, "ssim": 0.70},
        {"psnr_db": 27.0, "ssim": 0.80},
        0.8, 0.8,
        baseline_experiment_id="EXP-20261103-190343-FE93CD"
    )
    assert len(eval_exp007.provenance_caveats) >= 1
    assert "EXP-007" in eval_exp007.provenance_caveats[0]


def test_phase4b_schema_validation():
    """Validates complete Phase 4B manifest with modulationParams and renderingDiagnostics."""
    schema_path = os.path.abspath(
        os.path.join(os.path.dirname(__file__), "..", "schemas", "phase4b_manifest.schema.json")
    )
    with open(schema_path, "r", encoding="utf-8") as f:
        schema = json.load(f)

    manifest_data = {
        "schemaVersion": "1.1.0",
        "experimentId": "EXP-20261106-001500-MOD001",
        "timestamp": "2026-11-06T00:15:00Z",
        "testMode": "spatial_modulation",
        "sourceImage": {
            "name": "chart_combined_quality.png",
            "hash_sha256": "a" * 64,
            "width": 1080,
            "height": 1920
        },
        "phoneA": {
            "manufacturer": "Xiaomi",
            "model": "2312DRAABI",
            "androidVersion": "15",
            "apiLevel": 35,
            "displayResolution": "1080x2400",
            "refreshRateHz": 120.0
        },
        "phoneB": {
            "captureMode": "auto"
        },
        "modulationParams": {
            "modulationType": "spatial",
            "spatialPattern": "CHECKERBOARD",
            "gridPeriodPx": 16,
            "contrastRatio": 0.08,
            "orientationDeg": 45.0
        },
        "renderingDiagnostics": {
            "totalFramesRecorded": 600,
            "meanIntervalMs": 16.67,
            "recentMeanFps": 59.9,
            "estimatedDroppedFrames": 0,
            "minIntervalMs": 16.2,
            "maxIntervalMs": 17.1
        },
        "captures": [
            {
                "filename": "capture_01.png",
                "repetitionNumber": 1
            }
        ],
        "notes": "Validation test session"
    }

    # Should validate without raising an exception
    jsonschema.validate(instance=manifest_data, schema=schema)
