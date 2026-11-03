# Phase 4A — Optical Research Lab Baseline Capture Protocol

## 1. Protocol Objective

The **Phase 4A Baseline Capture Protocol** defines a rigorous, repeatable physical experimental procedure for measuring baseline optical capture fidelity across two smartphones.

This protocol establishes the control benchmark:
- **Phone A** displays standardized synthetic test charts with four corner calibration fiducials in a strictly static, un-modulated state.
- **Phone B** photographs Phone A's screen under controlled physical, optical, and environmental conditions.
- The original photographs are imported into the session and analyzed via the Python analysis pipeline to compute baseline **PSNR**, **SSIM**, **RMSE**, and **Delta E ($\Delta E$)**.

This baseline serves as the quantitative control against which future Phase 4B spatial and temporal modulation prototypes will be compared.

---

## 2. Equipment & Laboratory Requirements

| Item | Specification | Research Role |
|---|---|---|
| **Phone A (Display Device)** | Modern Android smartphone (e.g. Xiaomi 2312DRAABI, Pixel, Galaxy) running Android 10+ (API 29+) | Hosts the `optical-research` application; renders static reference charts |
| **Phone B (Camera Device)** | Secondary smartphone or digital camera with manual capture controls (Pro/Manual mode preferred) | Captures photographs of Phone A's screen |
| **Mounting Apparatus** | Tripod or articulated desktop mount with clamp | Ensures zero handshake and strictly fixed distance/angle between captures |
| **Ambient Illumination** | Controlled room lighting (preferably darkened lab environment, $<10\text{ lux}$, or fixed diffuse illumination) | Eliminates stray ambient reflections and glare on Phone A's glass screen |
| **Measurement Tooling** | Physical metric ruler / tape measure | Verifies exact lens-to-screen distance ($d \pm 0.5\text{ cm}$) |

---

## 3. Apparatus Setup & Geometry

```
                    +------------------------------------+
                    |        Phone B (Camera Sensor)     |
                    +------------------------------------+
                                      |
                                      | [Optical Axis]
                                      v
                                  <=======> Distance: d = 40.0 cm (±0.5 cm)
                                      |     Angle: θ = 90° (Perpendicular)
                                      v
                    +------------------------------------+
                    |        Phone A (Display Surface)   |
                    |     [Veil Optical Research Lab]    |
                    +------------------------------------+
```

### Alignment Constraints:
1. **Perpendicular Baseline ($\theta = 90^\circ$)**:
   The camera optical axis must align perpendicular to the center of Phone A's display surface to minimize trapezoidal perspective distortion.
2. **Fixed Distance ($d = 40\text{ cm}$ standard)**:
   Position Phone B so Phone A's entire display panel (including the outer border and all 4 corner bullseye fiducials) is framed within the camera viewfinder with an approximately 5–10% safety margin.
3. **Off-Axis Variation ($\theta = 75^\circ$ or $15^\circ$ tilt)**:
   For secondary test runs evaluating angular viewing falloff, tilt Phone B by a calibrated $15^\circ$ angle and record this explicitly in Phone B metadata.

---

## 4. Controlled Variables & Device Settings

### Phone A Configuration:
1. **Application**: Launch `Veil Optical Lab` (`com.securechat.optical.research.debug`).
2. **Brightness**: Set to a fixed level ($80\text{–}100\%$). Disable adaptive brightness / auto-brightness in Android system settings.
3. **Display Refresh Rate**: Note current display refresh rate in the Research Dashboard (e.g. 60Hz or 120Hz).
4. **Orientation**: Lock to Portrait ($0^\circ$).
5. **Wake Lock**: The application automatically acquires `FLAG_KEEP_SCREEN_ON` during active display.

### Phone B Configuration:
1. **Camera Mode**: Pro / Manual Mode (preferred) or Standard Auto.
2. **Focus**: Tap-to-focus on the center of Phone A's screen and lock focus (AF-L).
3. **ISO Sensitivity**: Set to lowest native ISO (e.g. ISO 50 or 100) to minimize sensor thermal noise.
4. **Shutter Speed**: Record the shutter speed (e.g. $1/125\text{ s}$, $1/250\text{ s}$, or $1/500\text{ s}$).
5. **White Balance**: Fixed daylight ($5500\text{ K}$) or auto.
6. **Flash**: Strictly disabled.
7. **Post-Processing**: Disable HDR, beauty filters, scene optimizers, and digital zoom.

---

## 5. Step-by-Step Execution Procedure

### Step 1: Session Creation & Chart Selection
1. On Phone A, open **Veil Optical Lab** and navigate to **New Experiment** (or **Synthetic Test Chart Gallery**).
2. Select one of the 7 synthetic test charts:
   - `chart_combined_quality.png` (Recommended for comprehensive baseline)
   - `chart_color_calibration.png` (Colorimetric fidelity)
   - `chart_grayscale_gradient.png` (Dynamic range and luminance curve)
   - `chart_checkerboard.png` (Lens distortion and geometric warp)
   - `chart_fine_lines.png` (MTF and optical resolving power)
   - `chart_high_contrast_edges.png` (Knife-edge step response)
   - `chart_text_readability.png` (Snellen typographic legibility scale)
3. Tap **Display on Phone A**. The screen transitions to the fullscreen `BaselineDisplayActivity`, showing the reference chart with 4 outer corner fiducials and the generated `EXP-YYYYMMDD-HHMMSS-XXXX` session identifier.

### Step 2: Physical Photographic Acquisition (Phone B)
1. Mount Phone B on the tripod at $d = 40\text{ cm}$.
2. Verify all 4 corner fiducials are visible in the viewfinder.
3. Trigger the camera shutter using a 2-second timer or remote shutter to eliminate physical touch vibration.
4. Repeat the capture **at least 3 times** under identical conditions (`capture_001.jpg`, `capture_002.jpg`, `capture_003.jpg`) to measure experimental repeatability.

### Step 3: Capture Import & Association
1. Transfer the raw photographs from Phone B to Phone A (via USB, Quick Share, or local network).
   - *CRITICAL*: Do not send via compression services (e.g. WhatsApp or email compression). Retain original camera JPEG/PNG/WebP files.
2. On Phone A, exit the fullscreen display and open **Import Camera Capture**.
3. Fill in Phone B capture telemetry (Device model, distance, angle, ISO, shutter speed, lighting).
4. Tap **Import Original Photograph**. Use the Android system document picker to select `capture_001.jpg`.
5. The importer validates magic-byte headers, copies raw bytes into the session directory, computes the SHA-256 hash, and extracts EXIF metadata.
6. Repeat for repetitions 2 and 3.

### Step 4: Experiment Package Export
1. Tap **View Session Details & Export** or **Export Package**.
2. Tap **Export Experiment ZIP Package**.
3. Save the resulting `EXP-YYYYMMDD-HHMMSS-XXXX.zip` to Phone A storage or share directly to your analysis workstation via USB / ADB:
   ```bash
   adb pull /sdcard/Android/data/com.securechat.optical.research.debug/files/exports/<EXP_ID>.zip .
   ```

---

## 6. Offline Python Image Analysis

On your analysis computer:
```bash
# Activate virtual environment
source research/optical-capture/python/.venv/bin/activate  # Or on Windows: .\research\optical-capture\python\.venv\Scripts\activate

# Run automated analysis CLI
python research/optical-capture/python/analyze_experiment.py --input <EXP_ID>.zip
```

The pipeline automatically:
1. Validates `manifest.json` against `schemas/manifest.schema.json`.
2. Verifies SHA-256 integrity of source and capture files.
3. Automatically detects the 4 corner bullseye fiducials.
4. Computes the perspective homography transformation matrix:
   $$H = \begin{bmatrix} h_{11} & h_{12} & h_{13} \\ h_{21} & h_{22} & h_{23} \\ h_{31} & h_{32} & h_{33} \end{bmatrix}$$
5. Warps the captured photograph back to the exact reference coordinate space ($1080 \times 1920$).
6. Calculates objective baseline metrics:
   - **PSNR (dB)**: Full-frame and inner ROI peak signal-to-noise ratio.
   - **SSIM**: Multichannel structural similarity index.
   - **RMSE & MAE**: Luminance residual error.
   - **Delta E ($\Delta E$)**: CIE76 color difference in CIELAB space.
7. Generates `results.json`, `results.csv`, `comparison.png`, and publication-quality `report.md`.

---

## 7. Metric Interpretation & Scientific Boundaries

1. **PSNR & SSIM Are Similarity Metrics, Not Privacy Metrics**:
   A high baseline PSNR ($>25\text{ dB}$) and SSIM ($>0.80$) confirms accurate alignment and camera resolving power under normal conditions. In future Phase 4B tests, a reduction in PSNR/SSIM measures mathematical signal deviation; it does not prove human illegibility. Corroborating human readability trials are mandatory.
2. **Moire Artifacts in Baseline**:
   Interference between Phone A's subpixel pitch and Phone B's Bayer sensor grid can introduce slight baseline moiré ripples even on static images. The baseline protocol isolates this camera-sensor artifact from display-induced degradation.
