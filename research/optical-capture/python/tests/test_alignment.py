#!/usr/bin/env python3
"""
test_alignment.py - Unit Tests for Homography Alignment and Perspective Correction
"""

import sys
import os
import numpy as np
import cv2
import pytest

# Ensure parent directory is in sys.path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from image_alignment import (
    align_capture_to_reference,
    order_quad_points,
    AlignmentResult
)
from metrics import calculate_psnr, calculate_ssim


def create_synthetic_target_with_fiducials(w=1080, h=1920, margin=60):
    """Creates a deterministic synthetic test image with concentric bullseye fiducials."""
    img = np.full((h, w, 3), 255, dtype=np.uint8)
    # Inner rectangle
    cv2.rectangle(img, (margin + 40, margin + 40), (w - margin - 40, h - margin - 40), (180, 180, 180), -1)
    cv2.putText(img, "SYNTHETIC TEST TARGET", (margin + 80, h // 2), cv2.FONT_HERSHEY_SIMPLEX, 1.2, (0, 0, 0), 3)
    
    # 4 Bullseye fiducials
    corners = [(margin, margin), (w - margin, margin), (w - margin, h - margin), (margin, h - margin)]
    for cx, cy in corners:
        cv2.circle(img, (cx, cy), 36, (0, 0, 0), -1)
        cv2.circle(img, (cx, cy), 24, (255, 255, 255), -1)
        cv2.circle(img, (cx, cy), 12, (0, 0, 0), -1)
        cv2.circle(img, (cx, cy), 3, (255, 255, 255), -1)
    return img, corners


def test_order_quad_points():
    """Verify order_quad_points orders points clockwise: TL, TR, BR, BL."""
    unordered = np.array([
        [1000.0, 1800.0],  # BR
        [100.0, 100.0],    # TL
        [100.0, 1800.0],   # BL
        [1000.0, 100.0]    # TR
    ], dtype=np.float32)
    
    ordered = order_quad_points(unordered)
    assert np.allclose(ordered[0], [100.0, 100.0])   # TL
    assert np.allclose(ordered[1], [1000.0, 100.0])  # TR
    assert np.allclose(ordered[2], [1000.0, 1800.0]) # BR
    assert np.allclose(ordered[3], [100.0, 1800.0])  # BL


def test_align_with_manual_corners():
    """Applying a known perspective homography and inverting with manual coordinates must recover reference."""
    ref_w, ref_h = 600, 800
    ref_img, ref_corners = create_synthetic_target_with_fiducials(ref_w, ref_h, margin=50)
    
    # Apply synthetic perspective tilt
    src_quad = np.array([
        [0.0, 0.0],
        [float(ref_w), 0.0],
        [float(ref_w), float(ref_h)],
        [0.0, float(ref_h)]
    ], dtype=np.float32)
    
    dst_quad = np.array([
        [30.0, 20.0],
        [float(ref_w) - 40.0, 35.0],
        [float(ref_w) - 20.0, float(ref_h) - 30.0],
        [45.0, float(ref_h) - 15.0]
    ], dtype=np.float32)
    
    H_distort = cv2.getPerspectiveTransform(src_quad, dst_quad)
    distorted = cv2.warpPerspective(ref_img, H_distort, (ref_w, ref_h))
    
    # Transform reference corner points under distortion to simulate manual user picks
    warped_corners = []
    for cx, cy in ref_corners:
        pt = np.array([cx, cy, 1.0], dtype=np.float64)
        wpt = H_distort @ pt
        warped_corners.append((float(wpt[0] / wpt[2]), float(wpt[1] / wpt[2])))
        
    result: AlignmentResult = align_capture_to_reference(
        captured_img=distorted,
        ref_width=ref_w,
        ref_height=ref_h,
        ref_corners=ref_corners,
        manual_corners=warped_corners
    )
    
    assert result.success is True
    assert result.aligned_image is not None
    assert result.aligned_image.shape == (ref_h, ref_w, 3)
    
    # Calculate recovery fidelity
    psnr = calculate_psnr(ref_img, result.aligned_image)
    ssim = calculate_ssim(ref_img, result.aligned_image)
    assert psnr > 25.0
    assert ssim > 0.85


def test_align_failure_on_blank_image():
    """A completely blank white image without corners or markers must report failure cleanly."""
    blank = np.full((500, 500, 3), 255, dtype=np.uint8)
    ref_corners = [(50.0, 50.0), (450.0, 50.0), (450.0, 450.0), (50.0, 450.0)]
    
    result = align_capture_to_reference(
        captured_img=blank,
        ref_width=500,
        ref_height=500,
        ref_corners=ref_corners,
        manual_corners=None
    )
    
    assert result.success is False
    assert result.aligned_image is None
    assert len(result.warnings) > 0
