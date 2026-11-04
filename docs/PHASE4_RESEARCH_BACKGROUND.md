# Phase 4 — Optical Anti-Capture Research Background & Theoretical Foundations

## 1. Executive Summary & The "Analog Hole"

Modern operating systems provide robust software-level boundaries against visual exfiltration. As implemented and verified in **Veil Phase 3**, `WindowManager.LayoutParams.FLAG_SECURE` instructs the Android OS window compositor (SurfaceFlinger) to blank display buffers, neutralizing software screenshots, screen recording APIs, and task-switcher previews.

However, all software compositor defenses terminate at the physical display surface. This boundary is universally known in information security as the **Analog Hole**:

$$\text{Visual Information} \xrightarrow{\text{SurfaceFlinger}} \text{Photons Emitted from OLED/LCD} \xrightarrow{\text{Free Space}} \text{External Camera Lens / Retina}$$

Any physical optical sensor (a secondary smartphone, digital camera, or covert optical recorder) positioned in front of the screen can photograph displayed secrets without triggering operating-system alerts or kernel protections.

The central research objective of **Veil Phase 4** is:
> *"Can we render visual content on a smartphone display such that it remains reasonably readable and visually comfortable to a human observer, while photographs of the displayed content taken by external cameras become significantly degraded, corrupted, or unreadable?"*

> [!IMPORTANT]
> **Scientific Integrity & Scope Notice**:
> Phase 4 does **not** claim that optical capture resistance has already been achieved. The project is at **Phase 4A (Baseline Experimentation)**. Our objective is to construct an objective, reproducible two-phone laboratory apparatus and establish baseline photographic quality before introducing experimental modulation algorithms in Phase 4B. Published academic findings cited below represent prior external research in controlled optical/projection setups, not verified production capabilities of Veil.

---

## 2. Fundamental Disparity: Human Perception vs. Camera Sensor Sampling

The theoretical basis of optical anti-capture relies on exploiting the physical, physiological, and temporal disparities between **Human Visual Perception (HVS)** and **Electronic CMOS Sensor Sampling**.

```
+-----------------------------------------------------------------------------------+
|                                 DISPLAY SURFACE                                   |
|                     Time-Varying Luminous Flux L(x, y, t)                         |
+-----------------------------------------------------------------------------------+
                       /                                     \
                      /                                       \
                     v                                         v
   +------------------------------------+    +------------------------------------+
   |       HUMAN VISUAL SYSTEM (HVS)    |    |       CMOS ELECTRONIC CAMERA       |
   |                                    |    |                                    |
   | • Temporal Integration (Bloch's Law)|   | • Discrete Exposure Shutter (t_exp)|
   | • Critical Flicker Fusion (~60 Hz) |    | • Electronic Rolling Shutter (ERS) |
   | • Talbot-Plateau Luminance Sum     |    | • Line-by-Line Temporal Skew       |
   | • Spatial Contrast Sensitivity     |    | • Spatial Aliasing & Moiré         |
   |                                    |    |                                    |
   | Perceived: Time-Averaged Integral  |    | Captured: Instantaneous Slice      |
   +------------------------------------+    +------------------------------------+
```

### A. Human Visual Perception (The Continuous Integrator)

1. **Bloch's Law of Temporal Summation**:
   For optical stimuli of duration shorter than the critical integration time $t_c \approx 30\text{–}100\text{ ms}$, the human visual system behaves as an analog temporal integrator. The threshold of visual perception $S$ depends on the product of luminance intensity $I$ and exposure time $t$:
   $$S \propto \int_{0}^{t_c} I(t) \, dt = I \times t$$
   High-frequency temporal fluctuations within this window are not perceived as individual events, but merge into a smooth visual continuum.

2. **Talbot-Plateau Law & Critical Flicker Fusion (CFF)**:
   When periodic optical pulses oscillate at a repetition frequency $f$ exceeding the Critical Flicker Fusion Threshold ($f > f_{\text{CFF}} \approx 50\text{–}60\text{ Hz}$ under standard photopic illumination), human consciousness resolves only the time-averaged luminance:
   $$L_{\text{perceived}} = \frac{1}{T} \int_{0}^{T} L(t) \, dt$$
   Oscillating complementary visual patterns above $f_{\text{CFF}}$ fuse into a single perceived image.

3. **Campbell-Robson Spatial Contrast Sensitivity Function (CSF)**:
   The human eye's spatial resolving power is band-limited, peaking between 3 and 5 cycles per degree (cpd) and attenuating steeply beyond 30 cpd. High-frequency spatial perturbation patterns positioned in the spatial roll-off zone remain imperceptible or visually negligible to human observers while still disrupting sensor sampling.

### B. Electronic CMOS Sensor Sampling (The Discrete Slicer)

1. **Discrete Exposure Integration Window ($t_{\text{exp}}$)**:
   Unlike the human visual system's continuous biochemical integration, a CMOS image sensor accumulates photons over an isolated exposure window dictated by shutter speed:
   $$E_{\text{cam}}(x, y) = \int_{t_0}^{t_0 + t_{\text{exp}}} L(x, y, t) \, dt$$
   In smartphone photography, exposure times typically range from $t_{\text{exp}} = 1/60\text{ s}$ to $1/2000\text{ s}$ ($0.5\text{–}16.6\text{ ms}$). When $t_{\text{exp}} < T_{\text{display}}$ (the period of display modulation), the camera integrates only an asymmetric temporal slice of the cycle, recording high-contrast imbalances that the human eye averages away.

2. **Electronic Rolling Shutter (ERS) Dynamics**:
   Consumer smartphone sensors expose row-by-row rather than simultaneously exposing all pixels (global shutter). Line $y$ begins and ends its exposure at:
   $$t_{\text{start}}(y) = t_0 + y \cdot \Delta t_{\text{line}}$$
   where $\Delta t_{\text{line}}$ is the sensor line readout time (typically $10\text{–}30\,\mu\text{s}$). Under time-varying display illumination of frequency $f_{\text{mod}}$, the recorded image exhibits prominent horizontal banding ripples with spatial band spacing:
   $$W_{\text{band}} = \frac{1}{f_{\text{mod}} \cdot \Delta t_{\text{line}}} \quad [\text{pixels}]$$

3. **Spatial Aliasing, Bayer CFA, and Moiré Interference**:
   Smartphone CMOS sensors employ a Bayer Color Filter Array (CFA) or Quad-Bayer mosaic with discrete pixel pitches ($0.7\text{–}1.4\,\mu\text{m}$). When the sensor samples a display panel's discrete subpixel grid, spatial beat interference creates visible moiré patterns:
   $$f_{\text{moiré}} = |f_{\text{display\_grid}} - f_{\text{sensor\_sampling}}|$$
   Demosaicing algorithms (e.g. Malvar-He-Cutler, bilinear interpolation) further amplify these high-frequency spatial artifacts into chromatic color blotches.

---

## 3. Review of Established Research Literature

Veil's research roadmap is grounded in published peer-reviewed investigations into psychovisual display modulation:

### 1. Temporal Psychovisual Modulation (TPVM)
- **Reference**: Zhai, G., & Wu, X. (2014). *Defeating Camcorder Piracy by Temporal Psychovisual Modulation*. IEEE/OSA Journal of Display Technology, 10(9), 754–761. [Optica JDT Link](https://opg.optica.org/jdt/abstract.cfm?uri=jdt-10-9-754)
- **Mechanism**: Explored decomposing an image $I_0$ into complementary sub-frame pairs displayed at high refresh rates ($>120\text{ Hz}$):
  $$I_1(x, y) = I_0(x, y) + \Delta I(x, y), \quad I_2(x, y) = I_0(x, y) - \Delta I(x, y)$$
  The human eye perceives the arithmetic average:
  $$\frac{I_1 + I_2}{2} = I_0$$
  An unsynchronized camera recording with $t_{\text{exp}} \le T/2$ captures only $I_1$ or $I_2$, recording corrupted content obscured by high-contrast noise pattern $\pm \Delta I$.

### 2. Modulated Light Perception Differences
- **Reference**: Wang, C., et al. (2025). *Investigation into preventing piracy based on the temporal perception difference between devices and humans using modulated projection light*. Applied Acoustics / ScienceDirect, 228, 110320. [ScienceDirect Link](https://www.sciencedirect.com/science/article/pii/S0141938225000320)
- **Mechanism**: Analyzed the mathematical boundaries where modulated waveforms degrade the Signal-to-Noise Ratio (SNR) of CMOS recording sensors in projection environments while remaining imperceptible to human viewers.

---

## 4. Engineering Challenges: Projection vs. Smartphone Displays

Applying projection-based anti-piracy techniques to smartphone displays introduces major engineering constraints:

| Parameter | Theatrical Projection Setup | Consumer Smartphone Display | Veil Research Impact |
|---|---|---|---|
| **Luminous Power** | Very high lumens ($>10,000\text{ lm}$); high dynamic modulation range | Peak luminance $500\text{–}1500\text{ nits}$; battery and thermal limits | Modulation depth $\Delta I$ must be conservative to preserve battery and panel life |
| **Refresh Rate Control** | Dedicated custom hardware, DLP mirrors ($144\text{–}240\text{ Hz}$ locked) | Android OS compositor (SurfaceFlinger); LTPO variable refresh ($1\text{–}120\text{ Hz}$) | Cannot assume fixed frame timing; must handle OS frame drops and dynamic throttling |
| **Pixel Architecture** | Continuous analog optical projection, optical convergence | Physical discrete subpixel matrix (e.g. Diamond PenTile OLED, RGB stripe LCD) | Subpixel color fringing and chromatic moiré at fine spatial frequencies |
| **PWM Backlight Dimming** | Constant or DC lamp / laser source | High-frequency OLED PWM dimming ($240\text{ Hz}\text{–}3840\text{ Hz}$) | Panel PWM frequency convolves with temporal modulation frames |
| **Viewing Geometry** | Fixed seating angle ($<30^\circ$) | Dynamic handheld orientation, variable angles ($0\text{–}60^\circ$) | Off-axis luminance falloff and color shift alter perceived balance |

---

## 5. Photobiological Safety Standards (IEEE 1789 Compliance)

Any future temporal modulation in Phase 4B must adhere strictly to established human photobiological safety standards:

1. **Epileptic Seizure Avoidance (IEEE 1789-2015)**:
   Under **IEEE Standard 1789-2015** (*IEEE Recommended Practices for Modulating Current in High-Brightness LEDs for Mitigating Health Risks to Viewers*), full-field stroboscopic modulation between **3 Hz and 65 Hz** presents an elevated risk of photosensitive epileptic seizures.
   - **Phase 4A uses strictly static images**. Zero temporal modulation is enabled.
   - Later Phase 4B experiments must operate strictly above safe flicker thresholds ($>100\text{ Hz}$) or utilize micro-amplitude spatial modulation below perceptual flicker thresholds.

2. **Mandatory Safety Fail-Safes for Phase 4B**:
   - Any experimental modulation engine must default to **OFF**.
   - Requires explicit affirmative opt-in confirmation with clear warnings.
   - Must feature a persistent, high-visibility emergency stop control.
   - Modulation depth must be bounded by conservative software ceilings.
   - Immediate session termination upon tester report of visual fatigue or discomfort.
