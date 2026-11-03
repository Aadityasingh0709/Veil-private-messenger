#!/usr/bin/env python3
"""
test_metrics.py - Unit Tests for Objective Image Quality Metrics (PSNR, SSIM, Delta E)
"""

import sys
import os
import numpy as np
import pytest

# Ensure parent directory is in sys.path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from metrics import (
    calculate_psnr,
    calculate_ssim,
    calculate_delta_e_cie76,
    evaluate_capture_quality,
    ImageMetrics
)


def test_psnr_identical_images():
    """Identical images must yield the capped ceiling of 100.0 dB."""
    img = np.random.randint(0, 256, (200, 200, 3), dtype=np.uint8)
    psnr = calculate_psnr(img, img)
    assert psnr >= 99.9


def test_ssim_identical_images():
    """Identical images must yield an SSIM of exactly 1.0."""
    img = np.random.randint(0, 256, (200, 200, 3), dtype=np.uint8)
    ssim = calculate_ssim(img, img)
    assert pytest.approx(ssim, rel=1e-4) == 1.0


def test_delta_e_identical_images():
    """Identical images must yield a Delta E of 0.0."""
    img = np.random.randint(0, 256, (200, 200, 3), dtype=np.uint8)
    delta_e = calculate_delta_e_cie76(img, img)
    assert pytest.approx(delta_e, abs=1e-4) == 0.0


def test_psnr_noisy_image():
    """Adding Gaussian noise should measurably lower PSNR into expected range."""
    ref = np.full((300, 300, 3), 128, dtype=np.uint8)
    # Add noise with std=10
    noise = np.random.normal(0, 10, ref.shape)
    noisy = np.clip(ref.astype(np.float64) + noise, 0, 255).astype(np.uint8)
    
    psnr = calculate_psnr(ref, noisy)
    # Theoretical PSNR for std=10 is 20*log10(255/10) ~ 28.1 dB
    assert 25.0 < psnr < 31.0


def test_ssim_degraded_image():
    """Severe degradation should lower SSIM well below 1.0."""
    ref = np.zeros((200, 200, 3), dtype=np.uint8)
    ref[50:150, 50:150] = 255
    
    # Inverted contrast
    inverted = 255 - ref
    ssim = calculate_ssim(ref, inverted)
    assert ssim < 0.2


def test_evaluate_capture_quality_structure():
    """evaluate_capture_quality must return complete ImageMetrics dataclass."""
    ref = np.random.randint(50, 200, (400, 400, 3), dtype=np.uint8)
    test = ref.copy()
    test[100:200, 100:200] = 0
    
    results: ImageMetrics = evaluate_capture_quality(ref, test, margin_px=50)
    assert isinstance(results, ImageMetrics)
    assert results.psnr_db > 0
    assert 0 <= results.ssim <= 1.0
    assert results.rmse > 0
    assert results.mae > 0
    assert results.delta_e_cie76 > 0
    assert results.engine_version == "1.0.0"
    assert "scientific" in results.interpretation_note.lower() or "not" in results.interpretation_note.lower()


def test_dimension_mismatch_raises_error():
    """Evaluating images with mismatched dimensions must raise a ValueError."""
    ref = np.zeros((100, 100, 3), dtype=np.uint8)
    test = np.zeros((120, 100, 3), dtype=np.uint8)
    with pytest.raises(ValueError):
        evaluate_capture_quality(ref, test)
