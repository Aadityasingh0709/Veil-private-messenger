#!/usr/bin/env python3
"""
test_chart_generator.py - Unit Tests for Optical Calibration Chart Generator (Commit 2)
"""

import sys
import os
import tempfile
import hashlib
import numpy as np
import cv2
import pytest

# Ensure parent directory is in sys.path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from generate_charts import (
    generate_all_charts,
    CHARTS_REGISTRY,
    get_corner_fiducial_coordinates,
    DEFAULT_WIDTH,
    DEFAULT_HEIGHT,
    DEFAULT_MARGIN
)


def test_chart_registry_contains_all_seven_charts():
    """Registry must contain all 7 required diagnostic charts."""
    required = [
        "chart_grayscale_gradient.png",
        "chart_color_calibration.png",
        "chart_checkerboard.png",
        "chart_fine_lines.png",
        "chart_high_contrast_edges.png",
        "chart_text_readability.png",
        "chart_combined_quality.png"
    ]
    for r in required:
        assert r in CHARTS_REGISTRY, f"Missing required chart: {r}"


def test_generate_all_charts_file_existence_and_dimensions():
    """Verifies that generate_all_charts creates all files with exact requested dimensions."""
    with tempfile.TemporaryDirectory() as temp_dir:
        w, h = 800, 1200
        manifest = generate_all_charts(output_dir=temp_dir, width=w, height=h, margin=50)

        assert manifest["resolution"]["width"] == w
        assert manifest["resolution"]["height"] == h
        assert len(manifest["charts"]) == 7

        for chart_meta in manifest["charts"]:
            fname = chart_meta["filename"]
            fpath = os.path.join(temp_dir, fname)
            assert os.path.exists(fpath), f"File {fname} was not created"
            assert os.path.getsize(fpath) > 0, f"File {fname} is empty"

            # Read back image with OpenCV to verify dimensions and 3 channels
            img = cv2.imread(fpath)
            assert img is not None, f"Failed to decode image {fname}"
            assert img.shape == (h, w, 3), f"Expected shape ({h}, {w}, 3) but got {img.shape} for {fname}"


def test_deterministic_generation():
    """Generating charts twice with identical parameters must yield byte-for-byte identical SHA-256 hashes."""
    with tempfile.TemporaryDirectory() as dir1, tempfile.TemporaryDirectory() as dir2:
        m1 = generate_all_charts(output_dir=dir1, width=600, height=800, margin=40)
        m2 = generate_all_charts(output_dir=dir2, width=600, height=800, margin=40)

        for c1, c2 in zip(m1["charts"], m2["charts"]):
            assert c1["filename"] == c2["filename"]
            assert c1["sha256"] == c2["sha256"], f"Non-deterministic hash for {c1['filename']}"
            assert c1["bytes"] == c2["bytes"]

            # Double-check raw file bytes
            b1 = open(os.path.join(dir1, c1["filename"]), "rb").read()
            b2 = open(os.path.join(dir2, c2["filename"]), "rb").read()
            assert b1 == b2, f"Byte mismatch in {c1['filename']}"


def test_corner_fiducial_coordinates():
    """Fiducial coordinates must conform to (margin, margin), (w-margin, margin), etc."""
    w, h, m = 1080, 1920, 60
    coords = get_corner_fiducial_coordinates(w, h, m)
    assert len(coords) == 4
    assert coords[0] == (60, 60)
    assert coords[1] == (1020, 60)
    assert coords[2] == (1020, 1860)
    assert coords[3] == (60, 1860)
