#!/usr/bin/env python3
"""
image_alignment.py - Perspective Distortion Correction and Homography Alignment

Detects 4-corner calibration fiducials in photographs of mobile screens taken by Phone B,
computes the perspective homography transformation, and registers the captured image
to the exact coordinate space of the reference test chart.
Supports automatic fiducial detection with manual coordinate fallback.
"""

from dataclasses import dataclass, field
from typing import List, Tuple, Optional, Dict, Any
import numpy as np
import cv2


@dataclass
class AlignmentResult:
    success: bool
    aligned_image: Optional[np.ndarray]
    transform_matrix: Optional[List[List[float]]]
    detected_corners: Optional[List[Tuple[float, float]]]
    reference_corners: List[Tuple[float, float]]
    method: str
    rotation_degrees: float = 0.0
    scale_factor: float = 1.0
    warnings: List[str] = field(default_factory=list)


def order_quad_points(pts: np.ndarray) -> np.ndarray:
    """
    Orders 4 points in clockwise order starting from Top-Left:
    [top-left, top-right, bottom-right, bottom-left].
    """
    rect = np.zeros((4, 2), dtype=np.float32)
    s = pts.sum(axis=1)
    rect[0] = pts[np.argmin(s)]      # Top-left has smallest sum
    rect[2] = pts[np.argmax(s)]      # Bottom-right has largest sum
    
    diff = np.diff(pts, axis=1)
    rect[1] = pts[np.argmin(diff)]   # Top-right has smallest difference
    rect[3] = pts[np.argmax(diff)]   # Bottom-left has largest difference
    return rect


def find_fiducial_centers_in_quadrants(gray: np.ndarray) -> Optional[np.ndarray]:
    """
    Locates the 4 bullseye fiducial marker centers by partitioning the image into 4 quadrants
    and finding concentric circular contours or strong Harris/Shi-Tomasi corners.
    """
    h, w = gray.shape
    quadrants = [
        ("TL", (0, 0, w // 2, h // 2)),
        ("TR", (w // 2, 0, w, h // 2)),
        ("BR", (w // 2, h // 2, w, h)),
        ("BL", (0, h // 2, w // 2, h))
    ]
    
    found_centers = []
    
    # Preprocessing: adaptive thresholding to isolate marker shapes
    blurred = cv2.GaussianBlur(gray, (5, 5), 0)
    thresh = cv2.adaptiveThreshold(
        blurred, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY_INV, 25, 7
    )
    
    for quad_name, (qx1, qy1, qx2, qy2) in quadrants:
        quad_thresh = thresh[qy1:qy2, qx1:qx2]
        quad_gray = gray[qy1:qy2, qx1:qx2]
        
        # Method A: Concentric circular contour hierarchy search
        contours, hierarchy = cv2.findContours(quad_thresh, cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)
        best_candidate = None
        min_dist_to_corner = float('inf')
        
        # Target corner point for this quadrant
        target_corner = {
            "TL": (0, 0),
            "TR": (qx2 - qx1, 0),
            "BR": (qx2 - qx1, qy2 - qy1),
            "BL": (0, qy2 - qy1)
        }[quad_name]
        
        if hierarchy is not None and len(contours) > 0:
            for idx, c in enumerate(contours):
                area = cv2.contourArea(c)
                # Check for reasonable fiducial size relative to image dimensions
                if area < 80 or area > (w * h * 0.05):
                    continue
                perimeter = cv2.arcLength(c, True)
                if perimeter == 0:
                    continue
                circularity = 4 * np.pi * area / (perimeter * perimeter)
                # If circular enough
                if circularity > 0.65:
                    M = cv2.moments(c)
                    if M["m00"] != 0:
                        cx = int(M["m10"] / M["m00"])
                        cy = int(M["m01"] / M["m00"])
                        # Check distance from the outer corner (fiducials are near the margins)
                        dist = np.hypot(cx - target_corner[0], cy - target_corner[1])
                        if dist < min_dist_to_corner:
                            min_dist_to_corner = dist
                            best_candidate = (qx1 + cx, qy1 + cy)
                            
        # Method B Fallback: Hough Circles in this quadrant
        if best_candidate is None:
            circles = cv2.HoughCircles(
                quad_gray, cv2.HOUGH_GRADIENT, dp=1.2, minDist=30,
                param1=100, param2=30, minRadius=10, maxRadius=100
            )
            if circles is not None and len(circles) > 0:
                circles = np.round(circles[0, :]).astype("int")
                for (cx, cy, r) in circles:
                    dist = np.hypot(cx - target_corner[0], cy - target_corner[1])
                    if dist < min_dist_to_corner:
                        min_dist_to_corner = dist
                        best_candidate = (qx1 + cx, qy1 + cy)
                        
        if best_candidate is not None:
            found_centers.append(best_candidate)
            
    if len(found_centers) == 4:
        return np.array(found_centers, dtype=np.float32)
    return None


def align_capture_to_reference(
    captured_img: np.ndarray,
    ref_width: int,
    ref_height: int,
    ref_corners: List[Tuple[float, float]],
    manual_corners: Optional[List[Tuple[float, float]]] = None
) -> AlignmentResult:
    """
    Performs perspective registration of captured_img to (ref_width, ref_height).
    
    Parameters:
        captured_img: BGR image from Phone B camera
        ref_width: Width of synthetic reference chart (e.g. 1080)
        ref_height: Height of synthetic reference chart (e.g. 1920)
        ref_corners: Reference chart corner fiducial coordinates [(TL), (TR), (BR), (BL)]
        manual_corners: Optional user-specified corner points [(x,y)*4] if auto-detection fails
    """
    warnings = []
    gray = cv2.cvtColor(captured_img, cv2.COLOR_BGR2GRAY)
    
    src_points = None
    method = "unknown"
    
    # 1. Manual coordinates take precedence if explicitly supplied
    if manual_corners is not None and len(manual_corners) == 4:
        src_points = order_quad_points(np.array(manual_corners, dtype=np.float32))
        method = "manual_coordinates"
    else:
        # 2. Attempt automatic fiducial detection
        detected = find_fiducial_centers_in_quadrants(gray)
        if detected is not None and len(detected) == 4:
            src_points = order_quad_points(detected)
            method = "automatic_fiducials"
        else:
            # 3. Fallback: Find largest high-contrast rectangular screen contour
            blurred = cv2.GaussianBlur(gray, (7, 7), 0)
            edged = cv2.Canny(blurred, 30, 150)
            kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (5, 5))
            dilated = cv2.dilate(edged, kernel, iterations=2)
            contours, _ = cv2.findContours(dilated, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
            contours = sorted(contours, key=cv2.contourArea, reverse=True)
            
            for c in contours[:5]:
                peri = cv2.arcLength(c, True)
                approx = cv2.approxPolyDP(c, 0.02 * peri, True)
                if len(approx) == 4 and cv2.contourArea(approx) > (ref_width * ref_height * 0.1):
                    src_points = order_quad_points(approx.reshape(4, 2).astype(np.float32))
                    method = "screen_contour_fallback"
                    warnings.append("Automatic fiducial detection could not locate all 4 markers; fell back to outer screen contour.")
                    break
                    
    if src_points is None:
        return AlignmentResult(
            success=False,
            aligned_image=None,
            transform_matrix=None,
            detected_corners=None,
            reference_corners=ref_corners,
            method="failed",
            warnings=["Could not detect 4 calibration fiducials or screen boundary. Please supply --manual-corners."]
        )
        
    dst_points = np.array(ref_corners, dtype=np.float32)
    
    # Compute perspective homography matrix
    H = cv2.getPerspectiveTransform(src_points, dst_points)
    
    # Warp perspective to exact reference dimensions
    aligned = cv2.warpPerspective(
        captured_img, H, (ref_width, ref_height),
        flags=cv2.INTER_LANCZOS4,
        borderMode=cv2.BORDER_CONSTANT,
        borderValue=(0, 0, 0)
    )
    
    # Calculate geometric rotation and scaling estimates
    # Vector TL -> TR
    v_src = src_points[1] - src_points[0]
    angle_src = np.degrees(np.arctan2(v_src[1], v_src[0]))
    v_dst = dst_points[1] - dst_points[0]
    angle_dst = np.degrees(np.arctan2(v_dst[1], v_dst[0]))
    rotation = float(angle_dst - angle_src)
    
    len_src = np.linalg.norm(v_src)
    len_dst = np.linalg.norm(v_dst)
    scale = float(len_dst / len_src) if len_src > 0 else 1.0
    
    return AlignmentResult(
        success=True,
        aligned_image=aligned,
        transform_matrix=H.tolist(),
        detected_corners=[(float(p[0]), float(p[1])) for p in src_points],
        reference_corners=ref_corners,
        method=method,
        rotation_degrees=rotation,
        scale_factor=scale,
        warnings=warnings
    )
