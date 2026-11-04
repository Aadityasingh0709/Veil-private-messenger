# Phase 4A — Optical Research Lab Baseline Capture Protocol

## 1. Protocol Objective & Purpose

The **Phase 4A Baseline Capture Protocol** specifies a controlled, repeatable experimental procedure for acquiring and evaluating baseline optical photographs of smartphone displays.

The primary objective is to quantify normal optical capture fidelity under known physical, electrical, and optical variables before introducing active display modulation:
- **Phone A** displays standardized synthetic reference test charts with four outer corner calibration fiducials in a strictly static state.
- **Phone B** photographs Phone A's screen using locked manual camera settings from a fixed distance and angle.
- The original photographs are imported into the session, perspective-corrected via subpixel homography, and evaluated to establish baseline benchmarks for **PSNR**, **SSIM**, **RMSE**, and **Delta E ($\Delta E$)**.

This baseline serves as the quantitative control against which future Phase 4B spatial and temporal modulation prototypes will be evaluated.

---

## 2. Equipment & Laboratory Apparatus

| Item | Technical Specification | Research Function |
|---|---|---|
| **Phone A (Display Device)** | Modern Android smartphone (e.g. Xiaomi 2312DRAABI, Google Pixel, Samsung Galaxy) running Android 10+ (API 29+) | Hosts the `optical-research` application; renders static reference charts |
| **Phone B (Camera Sensor)** | Secondary smartphone or digital camera with Pro / Manual camera mode | Captures optical photographs of Phone A's display surface |
| **Mounting Apparatus** | Sturdy desktop tripod or articulated arm with smartphone clamp | Eliminates handshake vibration and maintains constant distance/angle |
| **Ambient Illumination** | Controlled lab environment ($<10\text{ lux}$ darkened room or fixed $300\text{ lux}$ diffuse illumination) | Eliminates specular glare and external room light reflections on Phone A's glass screen |
| **Measurement Tooling** | Metric tape measure or optical calibration rail | Verifies lens-to-screen distance ($d \pm 0.5\text{ cm}$) |

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

### Geometric Constraints:
1. **Perpendicular Baseline ($\theta = 90^\circ$)**:
   The camera optical axis must be centered and perpendicular to Phone A's display to minimize trapezoidal skew.
2. **Standard Distance ($d = 40.0\text{ cm}$)**:
   Position Phone B so Phone A's display (including the outer calibration frame and all 4 corner fiducials) fills approximately $80\text{–}90\%$ of the camera viewfinder with a clean safety margin.
3. **Off-Axis Variation ($\theta = 75^\circ$)**:
   To measure angular luminance falloff and color shift, tilt Phone B by a calibrated $15^\circ$ angle off-perpendicular and record the angle in session metadata.

---

## 4. Controlled Variables & Device Settings

### Phone A Configuration:
1. **Application**: Launch `Veil Optical Lab` (`com.securechat.optical.research.debug`).
2. **Screen Brightness**: Set to a fixed manual level ($80\text{–}100\%$). Disable adaptive brightness in Android system settings.
3. **Display Refresh Rate**: Record active refresh rate from the dashboard (e.g. 60Hz or 120Hz).
4. **Orientation**: Locked to Portrait ($0^\circ$).
5. **Wake Lock**: The application automatically acquires `FLAG_KEEP_SCREEN_ON` during active display.

### Phone B Configuration:
1. **Camera Mode**: Pro / Manual Mode (preferred) or Standard Auto.
2. **Focus**: Tap-to-focus on the center target of Phone A and lock focus (AF-L).
3. **ISO Sensitivity**: Set to lowest native ISO (e.g. ISO 50 or 100) to minimize sensor thermal noise.
4. **Shutter Speed**: Lock shutter speed (e.g. $1/120\text{ s}$ or $1/250\text{ s}$) and record in session metadata.
5. **White Balance**: Fixed daylight ($5500\text{ K}$) or neutral gray card calibrated.
6. **Flash & Processing**: Flash strictly disabled. Disable computational HDR, beauty smoothing, night mode, and digital zoom.

---

## 5. Step-by-Step Baseline Acquisition Procedure

### Step 1: Session Initialization & Target Selection
1. On Phone A, open **Veil Optical Lab** and navigate to **New Experiment** (or **Synthetic Test Chart Gallery**).
2. Select one of the 7 standardized reference charts:
   - `chart_combined_quality.png` (Comprehensive master target)
   - `chart_color_calibration.png` (Colorimetric fidelity)
   - `chart_grayscale_gradient.png` (Dynamic range and luminance gamma curve)
   - `chart_checkerboard.png` (Lens distortion and geometric warp)
   - `chart_fine_lines.png` (Modulation Transfer Function / resolving power)
   - `chart_high_contrast_edges.png` (Knife-edge step response)
   - `chart_text_readability.png` (Snellen typographic legibility scale)
3. Tap **Display on Phone A**. The screen displays the fullscreen reference chart with 4 corner bullseye fiducials and the active `EXP-YYYYMMDD-HHMMSS-XXXX` session identifier.

### Step 2: Photographic Capture (Phone B)
1. Mount Phone B on the tripod at $d = 40.0\text{ cm}$ perpendicular to Phone A.
2. Verify all 4 corner bullseye fiducials are visible in the viewfinder.
3. Use a 2-second shutter delay or remote trigger to eliminate physical touch vibration.
4. Execute **at least 3 captures** under identical conditions (`capture_001.jpg`, `capture_002.jpg`, `capture_003.jpg`) to measure experimental variance.

### Step 3: Raw Import & Telemetry Association
1. Transfer original photographs from Phone B to Phone A via USB or direct local transfer (avoid messaging or email compression services).
2. In **Veil Optical Lab**, tap **Import Camera Capture**.
3. Record Phone B conditions (Model, distance, angle, ISO, shutter speed, lighting).
4. Tap **Import Original Photograph** to import `capture_001.jpg`. The importer validates file magic bytes, copies raw bytes, computes SHA-256 hash, and extracts EXIF telemetry.
5. Repeat for repetitions 2 and 3.

### Step 4: Export Archive
1. Tap **Export Experiment ZIP Package**.
2. Save `EXP-YYYYMMDD-HHMMSS-XXXX.zip` locally or pull directly via ADB:
   ```bash
   adb pull /sdcard/Android/data/com.securechat.optical.research.debug/files/exports/<EXP_ID>.zip .
   ```

---

## 6. Mathematical Formulations & Image Analysis Pipeline

The offline Python analysis pipeline ([`research/optical-capture/python/analyze_experiment.py`](file:///c:/Users/HP/OneDrive/Desktop/sec_app/research/optical-capture/python/analyze_experiment.py)) processes the exported archive:

### 1. Subpixel Perspective Homography Alignment
The 4 corner bullseye fiducials are detected to compute the $3 \times 3$ perspective transformation matrix $H$:
$$\begin{bmatrix} x_{\text{ref}} \\ y_{\text{ref}} \\ 1 \end{bmatrix} \sim H \begin{bmatrix} x_{\text{cam}} \\ y_{\text{cam}} \\ 1 \end{bmatrix}$$
The captured photograph is warped using Lanczos-4 interpolation (`cv2.warpPerspective`) to the exact coordinate space ($1080 \times 1920$) of the digital reference chart.

### 2. Peak Signal-to-Noise Ratio (PSNR)
Evaluates pixel-level mean squared error across all 3 color channels ($M \times N$ pixels):
$$\text{MSE} = \frac{1}{3MN} \sum_{c=1}^3 \sum_{x=1}^M \sum_{y=1}^N \left[ I_{\text{ref}}(x, y, c) - I_{\text{aligned}}(x, y, c) \right]^2$$
$$\text{PSNR} = 20 \log_{10} \left( \frac{255.0}{\sqrt{\text{MSE}}} \right) \quad [\text{dB}]$$
*(For identical images where $\text{MSE} = 0$, the pipeline caps PSNR at a nominal ceiling of $100.0\text{ dB}$).*

### 3. Structural Similarity Index (SSIM)
Evaluates perceptual structural luminance ($l$), contrast ($c$), and structure ($s$) using the standard Wang et al. formulation:
$$\text{SSIM}(x, y) = \frac{(2\mu_x \mu_y + C_1)(2\sigma_{xy} + C_2)}{(\mu_x^2 + \mu_y^2 + C_1)(\sigma_x^2 + \sigma_y^2 + C_2)}$$
where $C_1 = (K_1 L)^2$, $C_2 = (K_2 L)^2$, $L = 255.0$, $K_1 = 0.01$, and $K_2 = 0.03$.

### 4. Perceptual Color Difference ($\Delta E_{\text{CIE76}}$)
Images are converted to CIELAB color space $(L^*, a^*, b^*)$:
$$\Delta E_{76} = \frac{1}{MN} \sum_{x, y} \sqrt{ (L_{\text{ref}}^* - L_{\text{cam}}^*)^2 + (a_{\text{ref}}^* - a_{\text{cam}}^*)^2 + (b_{\text{ref}}^* - b_{\text{cam}}^*)^2 }$$

### 5. Region of Interest (ROI) Masking
In addition to full-frame metrics, metrics are calculated on an inner Region of Interest (excluding the outer 120px calibration frame and fiducial markers) to evaluate pure content fidelity.

---

## 7. Metric Interpretation & Scientific Boundaries

1. **Similarity vs. Readability / Privacy**:
   - High baseline PSNR ($>25\text{ dB}$) and SSIM ($>0.80$) confirm successful alignment and optical resolving power under normal conditions.
   - In future Phase 4B tests, a reduction in PSNR or SSIM measures mathematical signal deviation; it does **not** prove human illegibility or absolute privacy. Subjective human readability trials are mandatory.
2. **Baseline Moiré & Subpixel Sampling**:
   - Smartphone OLED displays (e.g. Diamond PenTile) possess non-stripe subpixel arrangements. Optical sampling by Phone B's Bayer array produces minor baseline moiré ripples even on static images. The baseline protocol isolates this camera-sensor artifact from display-induced degradation.
