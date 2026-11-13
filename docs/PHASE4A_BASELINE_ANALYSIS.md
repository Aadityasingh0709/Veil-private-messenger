# Veil Phase 4A — Optical Capture Baseline Analysis

**Dataset:** 7 experiments, 21 camera captures.  **Status:** Baseline analysis complete with provenance caveats.

## Executive summary

All seven latest experiment submissions were processed. The 21 selected capture files are globally unique, their SHA-256 values match their own ZIP manifests, and all 21 screen quadrilaterals were successfully detected and perspective-aligned to the corresponding 1080 × 1920 reference canvas.
Across the seven submitted ZIPs, the capture-session IDs and reference images are cyclically offset relative to the logical experiment labels. This may be an export/session-packaging issue or a file-selection sequence issue; the application source has not been audited as part of this image analysis. References for EXP-001 through EXP-006 were recovered from the next archive; EXP-007 was recovered from EXP-001 (wraparound), using the visible chart titles and capture overlays.
EXP-007 has an additional metadata caveat: its captured overlay ID (`EXP-20261105-225211-89D5F2`) was not present in any submitted manifest. Its device metadata is therefore retained from the EXP-007 archive as a fallback and is not independently confirmed.

## Dataset integrity and methods

- Selected the latest re-submitted archive for each logical experiment. The older EXP-001 archive with a duplicate capture and older EXP-003 mistaken submission were superseded.
- All 21 selected capture files have distinct hashes globally and match their own archive manifest hashes.
- Corrected reference mapping uses the adjacent archive references because the exported references are cyclically offset by one session.
- Perspective alignment used four inner calibration-marker centers and a homography to reference marker coordinates `(50,50)`, `(1030,50)`, `(1030,1870)`, `(50,1870)`.
- Metrics were calculated on a chart-specific active-content ROI, excluding most page margins and the outer registration frame.
- RGB SSIM/PSNR are raw image-file comparisons. Grayscale SSIM/PSNR are also reported; affine-normalized grayscale metrics additionally remove a global mean/contrast difference to help isolate structure from exposure differences.
- Colour ΔE00 is an exploratory 24-patch comparison for EXP-001 and EXP-007; no colorimeter or calibrated display/camera profile was used.

## Experiment summary

| Experiment | Chart | Refresh | Brightness | RGB SSIM | Gray SSIM | Gray SSIM normalized | RGB PSNR dB | ΔE00 |
|---|---|---:|---:|---:|---:|---:|---:|---:|
| EXP-001 | Colour Calibration | 120 Hz | 17.3% | 0.695 | 0.764 | 0.744 | 14.47 | 7.91 |
| EXP-002 | Grayscale Gradient | 120 Hz | 31.8% | 0.883 | 0.883 | 0.885 | 15.68 | n/a |
| EXP-003 | Checkerboard Grid | 120 Hz | 31.8% | 0.433 | 0.434 | 0.697 | 10.17 | n/a |
| EXP-004 | Fine-Line & Resolution Target | 120 Hz | 31.8% | 0.472 | 0.473 | 0.553 | 8.50 | n/a |
| EXP-005 | High-Contrast Edge & MTF Target | 120 Hz | 31.8% | 0.853 | 0.855 | 0.916 | 12.53 | n/a |
| EXP-006 | Typographic Legibility Check | 120 Hz | 31.8% | 0.744 | 0.744 | 0.752 | 11.78 | n/a |
| EXP-007 | Master Combined Quality Target | 120 Hz | 31.8% | 0.647 | 0.705 | 0.705 | 13.21 | 7.81 |

## Interpretation

- The grayscale chart has the highest raw/normalized grayscale SSIM (about 0.883/0.885), indicating strong structural agreement after alignment.
- The checkerboard and fine-line charts have lower similarity (about 0.434 and 0.473 grayscale SSIM). These high-frequency patterns are sensitive to sampling phase, moiré, camera/display scaling and resampling; this is an expected optical-capture sensitivity, not proof of anti-capture protection.
- The high-contrast edge chart has high grayscale SSIM after global luminance normalization (about 0.916). A validated numeric MTF50 result is not reported because the simple slanted-edge estimate was not stable across all three captures.
- EXP-001 and EXP-007 have exploratory mean ΔE00 patch differences of about 7.91 and 7.81 respectively. These values reflect the complete display-camera-image pipeline and are not calibrated display colour accuracy.
- The captures are baseline captures with no spatial or temporal modulation enabled. They cannot demonstrate Veil anti-capture effectiveness.

## Experimental limitations

- Phone A brightness differed: EXP-001 metadata 17.25%; EXP-002 through EXP-007 metadata 31.76%. EXP-001 should not be directly compared as if capture conditions were identical.
- Refresh rate was recorded as 120 Hz in the matched metadata for all seven experiments. Phone B was a Google Pixel 8, 40 cm away, 90° angle, 5 lux recorded ambient lighting, automatic mode; recorded exposure 1/120 s and ISO 100. Capture EXIF blocks are empty, so these camera settings cannot be independently checked from the JPEGs.
- EXP-007 metadata session identity is not reconciled; treat its brightness/refresh metadata as provisional.
- Perspective correction uses detected fiducial marker centers. Small residual alignment errors, camera ISP processing, JPEG compression, display scaling, screen subpixels and reflections influence pixel metrics.
- PSNR and SSIM are similarity metrics, not direct measures of human readability, recoverability or privacy. Laplacian variance is not a calibrated MTF metric.

## Recommended next step — Phase 4B

1. Preserve this corrected baseline as the control dataset.
2. Lock Phone A brightness, refresh mode and screen orientation for each comparison pair.
3. Lock Phone B focus, exposure, ISO, white balance and lens where the camera app permits; record actual values per capture.
4. Capture the same charts at the same distance, angle and lighting for each modulation condition.
5. Test spatial-only, temporal-only and combined modulation separately, beginning with conservative settings and explicit visual-comfort checks.
6. Compare each modulated result against this baseline using the same alignment and ROI pipeline, plus a human readability/comfort assessment.

## Provenance mapping

| Logical experiment | Capture ZIP | Reference ZIP | Metadata ZIP |
|---|---|---|---|
| EXP-001 — Colour Calibration | EXP-20261105-150344-9A9FFB.zip | EXP-20261105-223604-9A4B87.zip | EXP-20261105-223604-9A4B87.zip |
| EXP-002 — Grayscale Gradient | EXP-20261105-223604-9A4B87.zip | EXP-20261105-223824-6822A6.zip | EXP-20261105-223824-6822A6.zip |
| EXP-003 — Checkerboard Grid | EXP-20261105-223824-6822A6.zip | EXP-20261105-224046-D7621B.zip | EXP-20261105-224046-D7621B.zip |
| EXP-004 — Fine-Line & Resolution Target | EXP-20261105-224046-D7621B.zip | EXP-20261105-224302-6CC577.zip | EXP-20261105-224302-6CC577.zip |
| EXP-005 — High-Contrast Edge & MTF Target | EXP-20261105-224302-6CC577.zip | EXP-20261105-224540-8E3394.zip | EXP-20261105-224540-8E3394.zip |
| EXP-006 — Typographic Legibility Check | EXP-20261105-224540-8E3394.zip | EXP-20261105-224952-E857DA.zip | EXP-20261105-224952-E857DA.zip |
| EXP-007 — Master Combined Quality Target | EXP-20261105-224952-E857DA.zip | EXP-20261105-150344-9A9FFB.zip | EXP-20261105-224952-E857DA.zip |

Original uploaded ZIPs were not modified. The corrected canonical dataset is a derived analysis package.