# Phase 4 — Optical Anti-Capture Research Background & Theoretical Foundations

## 1. Executive Summary & The "Analog Hole"

Modern operating systems provide robust software-level boundaries against visual exfiltration. As implemented and verified in **Veil Phase 3**, `WindowManager.LayoutParams.FLAG_SECURE` instructs the Android OS window compositor (SurfaceFlinger) to blank display buffers, neutralizing software screenshots, screen recording APIs, and task-switcher previews.

However, all software compositor defenses terminate at the physical display surface. This boundary is universally known in information security as the **Analog Hole**:

$$\text{Visual Information} \xrightarrow{\text{SurfaceFlinger}} \text{Photons Emitted from OLED/LCD} \xrightarrow{\text{Free Space}} \text{External Camera Lens / Retina}$$

Any physical optical sensor (a secondary smartphone, digital camera, or covert optical bug) positioned in front of the screen can photograph displayed secrets without triggering operating-system alerts or kernel protections.

The central research objective of **Veil Phase 4** is:
> *"Can we render visual content on a smartphone display such that it remains reasonably readable and visually comfortable to a human observer, while photographs of the displayed content taken by external cameras become significantly degraded, corrupted, or unreadable?"*

Phase 4 does not claim that this objective has already been achieved. Rather, it investigates the mathematical, physical, and psychovisual limits of software-driven display rendering against optical capture.

---

## 2. Fundamental Disparity: Human Perception vs. Camera Sensor Sampling

The theoretical basis of optical anti-capture relies on exploiting the physical and physiological disparities between **Human Visual Perception (HVS)** and **Electronic CMOS Sensor Sampling**.

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

### A. Human Visual Perception (The Integrator)

1. **Bloch's Law & Temporal Integration**:
   For brief optical stimuli below the critical duration $t_c \approx 30\text{–}100\text{ ms}$, the human visual system behaves as an analog temporal integrator. The perceived visual sensation $S$ is proportional to the total luminous energy:
   $$S \propto \int_{0}^{t_c} I(t) \, dt = I \times t$$
   High-frequency temporal fluctuations are not resolved as distinct flashes, but rather are integrated across the temporal receptive fields of retinal photoreceptors.

2. **Talbot-Plateau Law & Critical Flicker Fusion (CFF)**:
   When a periodic light stimulus oscillates at a frequency $f$ exceeding the Critical Flicker Fusion Threshold ($f > f_{\text{CFF}} \approx 50\text{–}60\text{ Hz}$ under photopic conditions), human consciousness perceives a continuous, steady luminance equal to the time-averaged mean:
   $$L_{\text{perceived}} = \frac{1}{T} \int_{0}^{T} L(t) \, dt$$

3. **Spatial Contrast Sensitivity Function (CSF)**:
   The human eye's spatial resolving power is band-limited (peaking around 3–5 cycles per degree and dropping sharply at high spatial frequencies). High-frequency spatial noise patterns can be designed to fall into perceptual masking thresholds.

### B. Electronic CMOS Sensor Sampling (The Slicer)

1. **Discrete Exposure Integration ($t_{\text{exp}}$)**:
   Unlike the human visual system's continuous rolling integration, a camera captures an image over an isolated, discrete shutter exposure duration:
   $$E_{\text{cam}}(x, y) = \int_{t_0}^{t_0 + t_{\text{exp}}} L(x, y, t) \, dt$$
   In well-lit indoor environments or high-speed modes, camera shutter speeds are typically between $t_{\text{exp}} = 1/120\text{ s}$ and $1/2000\text{ s}$ ($0.5\text{–}8.3\text{ ms}$) — an order of magnitude shorter than human visual persistence ($t_c \approx 50\text{–}100\text{ ms}$).

2. **Electronic Rolling Shutter (ERS)**:
   Nearly all smartphone CMOS sensors employ an Electronic Rolling Shutter. Rows of pixels are reset and read out sequentially from top to bottom. Row $y$ is exposed at time:
   $$t_{\text{row}}(y) = t_0 + y \cdot \Delta t_{\text{line}}$$
   where $\Delta t_{\text{line}}$ is the sensor line-readout period (typically $10\text{–}30\,\mu\text{s}$). If the display luminance or pattern fluctuates periodically in time, the camera records horizontal banding artifacts, intensity ripples, and partial frame states across the image height.

3. **Spatial Aliasing and Moiré**:
   When a camera's Bayer color filter array (CFA) samples the display's discrete pixel grid, interference between the two two-dimensional spatial frequencies produces pronounced optical moiré beat frequencies:
   $$f_{\text{moiré}} = |f_{\text{display}} - f_{\text{camera\_sampling}}|$$

---

## 3. Review of Established Research Literature

Veil's optical research builds upon established academic literature in psychovisual modulation and anti-piracy display optics:

### 1. Temporal Psychovisual Modulation (TPVM)
- **Reference**: Zhai, G., & Wu, X. (2014). *Defeating Camcorder Piracy by Temporal Psychovisual Modulation*. IEEE/OSA Journal of Display Technology, 10(9), 754–761. [Optica JDT Abstract](https://opg.optica.org/jdt/abstract.cfm?uri=jdt-10-9-754)
- **Principle**: Zhai and Wu proposed presenting pairs of complementary frames alternating at high refresh rates ($>120\text{ Hz}$). If an image $I_0$ is decomposed into two complementary sub-frames:
  $$I_1 = I_0 + \Delta I, \quad I_2 = I_0 - \Delta I$$
  The human eye integrates them to:
  $$\frac{I_1 + I_2}{2} = I_0$$
  However, an unsynchronized camcorder with exposure $t_{\text{exp}} \le \frac{T}{2}$ samples only $I_1$ or $I_2$, capturing severe high-contrast flicker or deliberate spatial corruption patterns ($\pm \Delta I$).

### 2. Modulated Light Perception Differences
- **Reference**: Wang, C., et al. (2025). *Investigation into preventing piracy based on the temporal perception difference between devices and humans using modulated projection light*. Applied Acoustics / ScienceDirect, 228, 110320. [ScienceDirect Link](https://www.sciencedirect.com/science/article/pii/S0141938225000320)
- **Principle**: Explores the mathematical boundaries where modulated illumination waveforms produce imperceptible perceptual differences for human audiences in cinema environments while degrading the signal-to-noise ratio (SNR) of CMOS recording sensors.

---

## 4. Engineering Challenges: Projection vs. Smartphone Displays

While prior academic research demonstrated anti-piracy success in **theatrical projection environments**, porting these concepts to **consumer smartphone displays** introduces severe physical constraints:

| Parameter | Theatrical Projection Setup | Consumer Smartphone Display | Veil Research Impact |
|---|---|---|---|
| **Luminous Power** | Very high lumens (thousands of lumens); high modulation dynamic range | Limited peak luminance ($500\text{–}1500\text{ nits}$); battery constrained | Modulation contrast is strictly bounded by display peak output |
| **Refresh Rate Control** | Dedicated custom hardware, DLP mirrors ($144\text{–}240\text{ Hz}$ locked) | OS-controlled Android compositor; LTPO dynamic variable refresh ($1\text{–}120\text{ Hz}$) | Cannot assume fixed frame timing; must account for OS frame drops and dynamic throttling |
| **Pixel Architecture** | Continuous optical projection, optical convergence | Discrete physical subpixels (e.g. Diamond PenTile OLED or RGB stripe LCD) | Subpixel color fringing and chromatic moiré at fine spatial frequencies |
| **PWM Backlight Dimming** | Constant or DC lamp / laser source | High-frequency PWM dimming ($240\text{ Hz}\text{–}3840\text{ Hz}$) on OLED | Backlight pulse modulation convolves with temporal frame modulation |
| **Viewing Geometry** | Fixed audience angle ($<30^\circ$) | Dynamic hand movement, arbitrary tilt angles ($0\text{–}60^\circ$) | Angular luminance falloff and color shift alter perceived balance |

---

## 5. Human Safety & Visual Comfort Standards

Any future exploration of temporal modulation in Phase 4B must adhere to strict photobiological safety standards:

1. **Epileptic Seizure Avoidance (IEEE 1789)**:
   Under **IEEE Standard 1789-2015** (*IEEE Recommended Practices for Modulating Current in High-Brightness LEDs for Mitigating Health Risks to Viewers*), full-field stroboscopic modulation between **3 Hz and 65 Hz** presents an elevated risk of photosensitive seizures.
   - **Phase 4A uses strictly static images**. Zero temporal modulation is enabled.
   - Any future Phase 4B experiments must operate strictly above safe flicker frequencies ($>100\text{ Hz}$) or utilize micro-amplitude modulation below perceptual detection thresholds.

2. **Fail-Safe Mechanism**:
   Any experimental modulation engine must default to **OFF**, require explicit affirmative opt-in, feature a persistent red emergency stop button, and enforce strict hardware modulation ceilings.
