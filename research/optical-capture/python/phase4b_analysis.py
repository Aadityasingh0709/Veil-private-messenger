#!/usr/bin/env python3
"""
phase4b_analysis.py - Phase 4B Spatial and Temporal Modulation Analysis Tools

Provides specialized comparative evaluation functions for Phase 4B experiments:
- Edge preservation score (Sobel gradient cross-correlation)
- Text readability score (Normalized Laplacian variance ratio)
- Baseline vs. Modulated metric differential calculation (ΔPSNR, ΔSSIM, ΔReadability)
- Provenance checks for known Phase 4A baseline caveats (EXP-001 brightness, EXP-007 ID)
"""

from dataclasses import dataclass, asdict
from typing import Dict, Any, Optional, Tuple, List
import numpy as np
import cv2


@dataclass
class ModulationEvaluation:
    delta_psnr_db: float
    delta_ssim: float
    baseline_psnr_db: float
    modulated_psnr_db: float
    baseline_ssim: float
    modulated_ssim: float
    edge_preservation_score: float
    text_readability_score: float
    anti_capture_degradation_index: float
    lpips_status: str
    provenance_caveats: List[str]

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


def compute_edge_preservation_score(
    aligned_capture: np.ndarray,
    reference: np.ndarray,
    roi: Optional[Tuple[int, int, int, int]] = None
) -> float:
    """
    Measures edge preservation between reference test chart and captured photograph.

    Uses Sobel gradient operators to extract gradient magnitude maps.
    Calculates normalized cross-correlation between reference and captured gradients.
    Score ranges from 0.0 (total edge disruption/loss) to 1.0 (perfect edge preservation).
    """
    # Crop ROI if provided: (x, y, w, h)
    if roi is not None:
        x, y, w, h = roi
        aligned_capture = aligned_capture[y : y + h, x : x + w]
        reference = reference[y : y + h, x : x + w]

    # Convert to grayscale float
    if aligned_capture.ndim == 3:
        cap_gray = cv2.cvtColor(aligned_capture, cv2.COLOR_BGR2GRAY)
    else:
        cap_gray = aligned_capture.copy()

    if reference.ndim == 3:
        ref_gray = cv2.cvtColor(reference, cv2.COLOR_BGR2GRAY)
    else:
        ref_gray = reference.copy()

    # Sobel gradient calculation
    gx_ref = cv2.Sobel(ref_gray.astype(np.float64), cv2.CV_64F, 1, 0, ksize=3)
    gy_ref = cv2.Sobel(ref_gray.astype(np.float64), cv2.CV_64F, 0, 1, ksize=3)
    mag_ref = np.sqrt(gx_ref**2 + gy_ref**2)

    gx_cap = cv2.Sobel(cap_gray.astype(np.float64), cv2.CV_64F, 1, 0, ksize=3)
    gy_cap = cv2.Sobel(cap_gray.astype(np.float64), cv2.CV_64F, 0, 1, ksize=3)
    mag_cap = np.sqrt(gx_cap**2 + gy_cap**2)

    denom = np.sqrt(np.sum(mag_ref**2)) * np.sqrt(np.sum(mag_cap**2))
    if denom == 0:
        return 0.0

    score = float(np.sum(mag_ref * mag_cap) / denom)
    return float(np.clip(score, 0.0, 1.0))


def compute_text_readability_score(
    aligned_capture: np.ndarray,
    reference: np.ndarray,
    roi: Optional[Tuple[int, int, int, int]] = None
) -> float:
    """
    Evaluates high-frequency contrast and text legibility retention.

    Computes ratio of Laplacian high-frequency variance between captured photograph
    and reference chart. Values near 1.0 indicate intact edge sharpness.
    """
    if roi is not None:
        x, y, w, h = roi
        aligned_capture = aligned_capture[y : y + h, x : x + w]
        reference = reference[y : y + h, x : x + w]

    if aligned_capture.ndim == 3:
        cap_gray = cv2.cvtColor(aligned_capture, cv2.COLOR_BGR2GRAY)
    else:
        cap_gray = aligned_capture.copy()

    if reference.ndim == 3:
        ref_gray = cv2.cvtColor(reference, cv2.COLOR_BGR2GRAY)
    else:
        ref_gray = reference.copy()

    lap_ref = cv2.Laplacian(ref_gray.astype(np.float64), cv2.CV_64F)
    lap_cap = cv2.Laplacian(cap_gray.astype(np.float64), cv2.CV_64F)

    var_ref = float(np.var(lap_ref))
    var_cap = float(np.var(lap_cap))

    if var_ref == 0:
        return 1.0 if var_cap == 0 else 0.0

    ratio = var_cap / var_ref
    # Scale to [0.0, 1.0] ceiling
    return float(np.clip(ratio, 0.0, 1.0))


def compare_modulation_vs_baseline(
    modulated_metrics: Dict[str, Any],
    baseline_metrics: Dict[str, Any],
    edge_preservation: float,
    text_readability: float,
    baseline_experiment_id: str = ""
) -> ModulationEvaluation:
    """
    Computes comparative metrics between a Phase 4B modulated capture and Phase 4A baseline.

    Delta calculations:
    - delta_psnr_db = modulated_psnr - baseline_psnr (negative indicates degradation)
    - delta_ssim = modulated_ssim - baseline_ssim (negative indicates structural degradation)
    - anti_capture_degradation_index = composite score of capture suppression [0.0 to 1.0]
    """
    b_psnr = float(baseline_metrics.get("psnr_db", 0.0))
    m_psnr = float(modulated_metrics.get("psnr_db", 0.0))
    d_psnr = m_psnr - b_psnr

    b_ssim = float(baseline_metrics.get("ssim", 0.0))
    m_ssim = float(modulated_metrics.get("ssim", 0.0))
    d_ssim = m_ssim - b_ssim

    # Anti-capture degradation index: higher means greater camera degradation
    # Weighted combination of SSIM reduction and edge disruption
    ssim_drop = max(0.0, -d_ssim)
    edge_loss = max(0.0, 1.0 - edge_preservation)
    degradation_index = float(np.clip(0.6 * ssim_drop + 0.4 * edge_loss, 0.0, 1.0))

    # Check known baseline caveats
    provenance_caveats = []
    if "EXP-001" in baseline_experiment_id or "EXP-20261103-181514-04F8A3" in baseline_experiment_id:
        provenance_caveats.append(
            "BASELINE_CAVEAT: EXP-001 had Phone A brightness at 17.3% (vs standard 31.8%). "
            "Absolute PSNR/SSIM deltas against EXP-001 reflect brightness asymmetry."
        )
    if "EXP-007" in baseline_experiment_id or "EXP-20261103-190343-FE93CD" in baseline_experiment_id:
        provenance_caveats.append(
            "BASELINE_CAVEAT: EXP-007 has session ID mismatch across archive manifest and summary CSV."
        )

    return ModulationEvaluation(
        delta_psnr_db=round(d_psnr, 3),
        delta_ssim=round(d_ssim, 4),
        baseline_psnr_db=round(b_psnr, 3),
        modulated_psnr_db=round(m_psnr, 3),
        baseline_ssim=round(b_ssim, 4),
        modulated_ssim=round(m_ssim, 4),
        edge_preservation_score=round(edge_preservation, 4),
        text_readability_score=round(text_readability, 4),
        anti_capture_degradation_index=round(degradation_index, 4),
        lpips_status="NOT_INSTALLED_TORCH_UNAVAILABLE",
        provenance_caveats=provenance_caveats
    )
