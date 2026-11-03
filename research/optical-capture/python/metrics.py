#!/usr/bin/env python3
"""
metrics.py - Objective Image-Quality and Capture-Similarity Metrics

Calculates standard photographic capture metrics:
- PSNR (Peak Signal-to-Noise Ratio) [dB]
- SSIM (Structural Similarity Index) [-1 to 1]
- RMSE (Root Mean Squared Error) [0 to 255]
- MAE (Mean Absolute Error) [0 to 255]
- Delta E (CIE76 color difference in CIELAB color space)

IMPORTANT SCIENTIFIC INTERPRETATION NOTE:
PSNR and SSIM measure mathematical signal similarity between the reference image
and the camera photograph. They do NOT directly indicate whether human visual perception
can extract sensitive text or reconstruct the message, nor do they represent an absolute
measure of privacy.
"""

from dataclasses import dataclass, asdict
from typing import Dict, Any, Optional, Tuple
import numpy as np
import cv2
from skimage.metrics import structural_similarity as ssim_fn

METRICS_ENGINE_VERSION = "1.0.0"


@dataclass
class ImageMetrics:
    psnr_db: float
    ssim: float
    rmse: float
    mae: float
    delta_e_cie76: float
    roi_psnr_db: float
    roi_ssim: float
    data_range: float
    engine_version: str
    interpretation_note: str

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


def calculate_psnr(ref: np.ndarray, test: np.ndarray) -> float:
    """
    Computes Peak Signal-to-Noise Ratio in dB.
    Handles exact identity by returning 100.0 dB as a nominal practical ceiling.
    """
    mse = np.mean((ref.astype(np.float64) - test.astype(np.float64)) ** 2)
    if mse == 0:
        return 100.0  # Mathematically infinite; capped for numerical serialization
    max_pixel = 255.0
    return float(20.0 * np.log10(max_pixel / np.sqrt(mse)))


def calculate_ssim(ref: np.ndarray, test: np.ndarray) -> float:
    """
    Computes multichannel Structural Similarity Index (SSIM) over full dynamic range [0, 255].
    """
    val = ssim_fn(ref, test, channel_axis=2, data_range=255.0)
    return float(val)


def calculate_delta_e_cie76(ref_bgr: np.ndarray, test_bgr: np.ndarray) -> float:
    """
    Calculates average CIE76 color difference (Delta E) across all pixels in CIELAB color space.
    Delta E < 1.0 is imperceptible to human eye.
    Delta E > 5.0 is readily apparent.
    """
    ref_lab = cv2.cvtColor(ref_bgr, cv2.COLOR_BGR2LAB).astype(np.float64)
    test_lab = cv2.cvtColor(test_bgr, cv2.COLOR_BGR2LAB).astype(np.float64)
    delta_e = np.sqrt(np.sum((ref_lab - test_lab) ** 2, axis=2))
    return float(np.mean(delta_e))


def extract_inner_roi(img: np.ndarray, margin_px: int = 120) -> np.ndarray:
    """Extracts the inner content region, excluding the calibration margin and fiducials."""
    h, w = img.shape[:2]
    return img[margin_px:h - margin_px, margin_px:w - margin_px]


def evaluate_capture_quality(
    reference_bgr: np.ndarray,
    aligned_bgr: np.ndarray,
    margin_px: int = 120
) -> ImageMetrics:
    """
    Evaluates comprehensive baseline similarity metrics between reference test chart
    and perspective-aligned camera capture.
    """
    if reference_bgr.shape != aligned_bgr.shape:
        raise ValueError(
            f"Image dimensions must match for metrics computation. "
            f"Reference: {reference_bgr.shape}, Aligned: {aligned_bgr.shape}"
        )

    # Full frame metrics
    psnr_val = calculate_psnr(reference_bgr, aligned_bgr)
    ssim_val = calculate_ssim(reference_bgr, aligned_bgr)
    
    diff = reference_bgr.astype(np.float64) - aligned_bgr.astype(np.float64)
    rmse_val = float(np.sqrt(np.mean(diff ** 2)))
    mae_val = float(np.mean(np.abs(diff)))
    delta_e = calculate_delta_e_cie76(reference_bgr, aligned_bgr)
    
    # Inner ROI metrics (content area excluding calibration fiducials and borders)
    ref_roi = extract_inner_roi(reference_bgr, margin_px)
    aligned_roi = extract_inner_roi(aligned_bgr, margin_px)
    roi_psnr = calculate_psnr(ref_roi, aligned_roi)
    roi_ssim = calculate_ssim(ref_roi, aligned_roi)
    
    note = (
        "SSIM and PSNR quantify pixel-level signal fidelity against the digital reference. "
        "They do not measure human cognitive legibility, reading comfort, or absolute privacy."
    )
    
    return ImageMetrics(
        psnr_db=round(psnr_val, 3),
        ssim=round(ssim_val, 4),
        rmse=round(rmse_val, 3),
        mae=round(mae_val, 3),
        delta_e_cie76=round(delta_e, 3),
        roi_psnr_db=round(roi_psnr, 3),
        roi_ssim=round(roi_ssim, 4),
        data_range=255.0,
        engine_version=METRICS_ENGINE_VERSION,
        interpretation_note=note
    )
