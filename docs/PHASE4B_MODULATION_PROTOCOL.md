# Phase 4B — Spatial and Temporal Modulation Experimental Protocol

## 1. Protocol Objective & Research Rationale

The **Phase 4B Modulation Protocol** establishes the scientific methodology for evaluating whether software-driven display modulation can measurably degrade optical camera captures while preserving human readability and visual safety.

Phase 4B builds directly upon the Phase 4A baseline dataset (7 test charts, 21 calibrated captures across Phone A and Phone B). Where Phase 4A measured normal capture fidelity in a strictly static display state, Phase 4B introduces parameterized **spatial** and **temporal** luminance perturbations:

1. **Mode 1 — Spatial Modulation**: Introduces periodic spatial luminance structures (checkerboard, vertical lines, horizontal lines, diagonal lines, radial rings, sinusoidal waves) designed to interact destructively with camera sensor Bayer color filter arrays (CFAs) and post-processing demosaicing algorithms.
2. **Mode 2 — Temporal Modulation**: Alternates display luminance between two complementary states (Phase A and Phase B) synchronized to the display panel's hardware VSYNC signal via Android's `Choreographer`. This exploits the temporal integration divergence between human visual perception (flicker fusion threshold ~15–60 Hz) and camera electronic rolling shutters (1/60 s to 1/2000 s exposure intervals).
3. **Mode 3 — Combined Modulation**: Evaluates whether simultaneous spatial perturbation and temporal phase cycling produce synergistic capture suppression without compounding human eye strain.

---

## 2. Human Safety & Photobiological Compliance Framework

Photobiological safety is non-negotiable. Display modulation algorithms must prevent visual discomfort, asthenopia, and photosensitive seizure risks.

### Hard Safety Constraints (Enforced in Code):
- **Temporal Frequency Ceiling**: Modulation frequency is strictly capped at **$f_{\text{max}} = 3.0\text{ Hz}$**.
  - *Standard*: WCAG 2.1 Success Criterion 2.3.1 (Three Flashes or Below Threshold) and IEC 61966-2-1. Frequencies in the critical $3\text{–}30\text{ Hz}$ photosensitive hazard window are programmatically rejected by `ModulationParams.Temporal.validate()`.
- **Modulation Contrast Ceiling**: Maximum contrast perturbation is hard-capped at **$15\%$** ($\Delta L / L \le 0.15$). High-contrast flashing is prohibited.
- **Fail-Safe Emergency Stop**: The UI features an omnipresent red **⏹ STOP** button that immediately halts temporal alternation and restores a static baseline display.
- **Hardware Frame Timing Telemetry**: `RenderingDiagnostics` monitors actual frame intervals via Choreographer timestamps, detecting display stalls or dropped frames.

---

## 3. Experimental Parameters & Modulations

### Mode 1: Spatial Modulation Suite
| Parameter | Range / Options | Research Objective |
|---|---|---|
| **Pattern Types** | `HORIZONTAL_LINES`, `VERTICAL_LINES`, `DIAGONAL_LINES`, `CHECKERBOARD`, `RADIAL_GRID`, `SINE_WAVE` | Evaluates CFA moiré sensitivity across orthogonal, diagonal, and curved spatial frequencies |
| **Grid Period** | $2\text{ to }64\text{ pixels}$ | Tunes spatial frequency relative to camera sensor Nyquist frequency |
| **Contrast Ratio** | $0.01\text{ to }0.15$ ($1\%\text{–}15\%$) | Identifies minimum threshold needed to induce capture noise |
| **Orientation** | $0^\circ\text{ to }179^\circ$ | Tests angle-dependent aliasing against camera sensor grid |

### Mode 2: Temporal Modulation Suite
| Parameter | Range / Default | Research Objective |
|---|---|---|
| **Modulation Frequency** | $0.5\text{ to }3.0\text{ Hz}$ (Step $0.1\text{ Hz}$) | Evaluates shutter phase capture vs perceptual averaging |
| **Duty Cycle** | $0.10\text{ to }0.90$ (Default $0.50$) | Tests asymmetric phase exposure durations |
| **Amplitude** | $0.01\text{ to }0.15$ ($1\%\text{–}15\%$) | Controls luminance swing between Phase A (darken) and Phase B (brighten) |
| **VSYNC Sync** | Hardware Choreographer | Eliminates software frame tearing and irregular timing |

### Mode 3: Combined Modulation
- Applies spatial pattern overlay to reference image, then alternates luminance states over time.
- Verifies whether spatial high-frequency moiré patterns coupled with sub-shutter temporal phase shifts disrupt camera auto-exposure and demosaicing more effectively than either mode alone.

---

## 4. Hardware Frame Diagnostics & Telemetry

Because mobile operating systems do not guarantee fixed-interval rendering under load, every Phase 4B capture must record actual hardware frame timing via `RenderingDiagnostics`:

- **Rolling FPS**: Mean FPS over a 30-frame window.
- **Inter-Frame Interval ($\Delta t$)**: Mean, minimum, and maximum interval in milliseconds.
- **Dropped Frame Estimation**: Counts intervals $>1.5\times$ the expected frame interval (e.g., $>25.0\text{ ms}$ on a $60\text{ Hz}$ display or $>12.5\text{ ms}$ on a $120\text{ Hz}$ display).
- **Session Export**: Telemetry is serialized as a `DiagnosticsSnapshot` embedded in the experiment manifest.

---

## 5. Quantitative Evaluation Metrics

Comparative analysis between baseline captures and modulated captures is performed using the `compare_modulation.py` tool:

$$\Delta\text{PSNR} = \text{PSNR}_{\text{modulated}} - \text{PSNR}_{\text{baseline}}\quad\text{(dB)}$$
$$\Delta\text{SSIM} = \text{SSIM}_{\text{modulated}} - \text{SSIM}_{\text{baseline}}$$

### Additional Phase 4B Metrics:
1. **Edge Preservation Score ($S_{\text{edge}} \in [0, 1]$)**: Normalized cross-correlation of Sobel gradient magnitudes between reference chart and aligned capture:
   $$S_{\text{edge}} = \frac{\sum |\nabla I_{\text{ref}}| \cdot |\nabla I_{\text{cap}}|}{\sqrt{\sum |\nabla I_{\text{ref}}|^2} \sqrt{\sum |\nabla I_{\text{cap}}|^2}}$$
2. **Text Readability Score ($S_{\text{read}} \in [0, 1]$)**: Normalized ratio of Laplacian high-frequency variance:
   $$S_{\text{read}} = \min\left(1.0, \frac{\text{Var}(\Delta I_{\text{cap}})}{\text{Var}(\Delta I_{\text{ref}})}\right)$$
3. **Anti-Capture Degradation Index ($I_{\text{anti}} \in [0, 1]$)**:
   $$I_{\text{anti}} = 0.6 \cdot \max(0, -\Delta\text{SSIM}) + 0.4 \cdot \max(0, 1.0 - S_{\text{edge}})$$
4. **LPIPS Note**: Learned Perceptual Image Patch Similarity requires PyTorch, which is omitted from the lightweight analysis runtime. The tool explicitly flags `lpips_status = "NOT_INSTALLED_TORCH_UNAVAILABLE"`.

---

## 6. Baseline Provenance & Known Dataset Caveats

When conducting comparisons against the Phase 4A baseline dataset, researchers must account for the following documented anomalies:

1. **EXP-001 (Combined Quality Chart) Brightness Discrepancy**:
   - Phone A panel brightness was recorded at **$17.3\%$**, compared to **$31.8\%$** across all other experiments (`EXP-002` through `EXP-007`).
   - *Impact*: Absolute PSNR and SSIM deltas against EXP-001 reflect a baseline brightness asymmetry. Modulated experiments must be compared against a baseline collected at matched brightness.
2. **EXP-007 Identifier Mismatch**:
   - Archive manifest references `EXP-20261103-190343-FE93CD` while the summary CSV labels it as `EXP-007`. The analysis tool automatically reconciles this identity.
3. **Cycle Archive Offset**:
   - Early Phase 4A exports exhibited a cyclic ZIP offset; verify all files using SHA-256 digests prior to ingestion.

---

## 7. Execution Step-by-Step

1. **Phone A (Display Device)**:
   - Open Veil Optical Lab $\to$ Phase 4B Modulation Experiments.
   - Select mode: **Spatial**, **Temporal**, or **Combined**.
   - Select reference test chart (e.g. `Combined Quality Chart`).
   - Adjust parameters using the live control panel.
   - Verify that the **HW-TIMING** overlay confirms nominal frame rate ($60.0\text{ Hz}$ or $120.0\text{ Hz}$) with 0 drops.
2. **Phone B (Camera Device)**:
   - Mount Phone B on tripod at $40.0\text{ cm}$ perpendicular.
   - Lock ISO ($100$), shutter speed ($1/120\text{ s}$), and manual focus.
   - Take 3 repetitions of the modulated display.
3. **Offline Comparative Evaluation**:
   ```bash
   python compare_modulation.py \
     --baseline path/to/baseline_session.zip \
     --modulated path/to/modulated_session.zip \
     --output ./comparison_results/
   ```
4. **Inspect Output**:
   - `comparison_report.md`: Markdown summary table with findings and caveats.
   - `comparison_metrics.csv`: Machine-readable differentials.
   - `comparison_montage.png`: Side-by-side visual montage.
