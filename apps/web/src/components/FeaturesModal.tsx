import { useState } from "react";

interface FeaturesModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSelectUserToChat?: () => void;
}

const TEST_CHARTS = [
  { id: "color", name: "Colour Calibration", desc: "24 standard Macbeth-style color patches for spectrophotometric fidelity." },
  { id: "grayscale", name: "Grayscale Gradient", desc: "10-step step wedge & linear gradient verifying tonal response and dynamic range." },
  { id: "checkerboard", name: "Checkerboard Grid", desc: "Alternating pixel blocks evaluating camera sensor demosaicing & Nyquist limits." },
  { id: "fine_lines", name: "Fine-Line & Resolution", desc: "1px, 2px, 4px line patterns assessing MTF resolution and spatial aliasing." },
  { id: "high_contrast", name: "High-Contrast Edge", desc: "Slanted razor edges for Edge Spread Function (ESF) & MTF50 calculation." },
  { id: "text", name: "Typographic Legibility", desc: "Micro-text legibility ladders testing optical OCR and human character recognition." },
  { id: "combined", name: "Master Combined Quality", desc: "Consolidated multi-feature target featuring fiducials and combined metrics." }
];

export function FeaturesModal({ isOpen, onClose, onSelectUserToChat }: FeaturesModalProps) {
  const [activeTab, setActiveTab] = useState<"overview" | "chat" | "media" | "android" | "optical">("overview");

  if (!isOpen) return null;

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-card features-modal-card" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div className="features-modal-header-titles">
            <div className="features-badge-row">
              <span className="badge-online">Veil v0.4.2</span>
              <span className="badge-phase">Phase 4B Active</span>
            </div>
            <h3>Veil Capabilities & Security Hub</h3>
          </div>
          <button type="button" className="btn-close" onClick={onClose} aria-label="Close">
            ×
          </button>
        </div>

        {/* Tab navigation */}
        <div className="features-tabs">
          <button
            type="button"
            className={`features-tab-btn ${activeTab === "overview" ? "active" : ""}`}
            onClick={() => setActiveTab("overview")}
          >
            🌟 Overview
          </button>
          <button
            type="button"
            className={`features-tab-btn ${activeTab === "chat" ? "active" : ""}`}
            onClick={() => setActiveTab("chat")}
          >
            💬 Real-Time Chat
          </button>
          <button
            type="button"
            className={`features-tab-btn ${activeTab === "media" ? "active" : ""}`}
            onClick={() => setActiveTab("media")}
          >
            🖼️ Media & Viewer
          </button>
          <button
            type="button"
            className={`features-tab-btn ${activeTab === "android" ? "active" : ""}`}
            onClick={() => setActiveTab("android")}
          >
            📱 Android SDK
          </button>
          <button
            type="button"
            className={`features-tab-btn ${activeTab === "optical" ? "active" : ""}`}
            onClick={() => setActiveTab("optical")}
          >
            🔬 Optical Research
          </button>
        </div>

        {/* Tab Body */}
        <div className="features-modal-body">
          {activeTab === "overview" && (
            <div className="features-section">
              <div className="features-hero-box">
                <h4>🛡️ Unified Defense Against Digital & Physical Capture</h4>
                <p className="muted small">
                  Veil is engineered to protect sensitive visual exchanges across both software
                  layers (OS compositor protection and screenshot detection) and physical boundaries
                  (display modulation against external camera capture).
                </p>
              </div>

              <div className="security-telemetry-grid">
                <div className="telemetry-card">
                  <div className="telemetry-header">
                    <span className="telemetry-icon">🔒</span>
                    <strong>Argon2id Auth</strong>
                  </div>
                  <p className="small muted">Memory-hard password hashing with HTTP-only cookies.</p>
                  <span className="status-pill pill-active">Active</span>
                </div>

                <div className="telemetry-card">
                  <div className="telemetry-header">
                    <span className="telemetry-icon">⚡</span>
                    <strong>Socket.IO Duplex</strong>
                  </div>
                  <p className="small muted">Sub-second messaging, typing indicators & presence.</p>
                  <span className="status-pill pill-active">Active</span>
                </div>

                <div className="telemetry-card">
                  <div className="telemetry-header">
                    <span className="telemetry-icon">🎬</span>
                    <strong>HTTP 206 Streaming</strong>
                  </div>
                  <p className="small muted">Chunked byte-range video streaming & magic-byte check.</p>
                  <span className="status-pill pill-active">Active</span>
                </div>

                <div className="telemetry-card">
                  <div className="telemetry-header">
                    <span className="telemetry-icon">🛡️</span>
                    <strong>FLAG_SECURE SDK</strong>
                  </div>
                  <p className="small muted">Android compositor screenshot and recording block.</p>
                  <span className="status-pill pill-active">Ready (.aar)</span>
                </div>
              </div>

              <div className="features-roadmap-summary">
                <h5>Active Multi-Phase Milestones</h5>
                <ul className="features-list small muted">
                  <li><strong>Phase 1:</strong> Core Web Chat, JWT Revocation, MongoDB persistence (Completed).</li>
                  <li><strong>Phase 2:</strong> Media Streaming, EXIF Stripping, Custom Viewer, Message Deletion (Completed).</li>
                  <li><strong>Phase 3:</strong> Native Android Capture Protection SDK & Demo Application (Completed & Verified).</li>
                  <li><strong>Phase 4A:</strong> Optical Research Baseline Capture Protocol & 7 Test Charts (Completed).</li>
                  <li><strong>Phase 4B:</strong> Spatial/Temporal Display Modulation Lab & Python CV Suite (Current & Audited).</li>
                </ul>
              </div>
            </div>
          )}

          {activeTab === "chat" && (
            <div className="features-section">
              <h4>💬 Phase 1: Real-Time Messaging & Auth</h4>
              <p className="small muted">
                High-performance real-time messaging platform with conversation isolation, typing
                indicators, and fail-closed authentication.
              </p>

              <div className="feature-cards-stack">
                <div className="feature-card">
                  <strong>Instant Duplex Delivery</strong>
                  <p className="small muted">Powered by Socket.IO with automatic reconnection and heartbeat monitoring.</p>
                </div>
                <div className="feature-card">
                  <strong>Fail-Closed JWT Revocation</strong>
                  <p className="small muted">Token revocation list verified on every route and socket handshake. Defaults to deny if store fails.</p>
                </div>
                <div className="feature-card">
                  <strong>User Discovery & Isolation</strong>
                  <p className="small muted">Search users by name or handle. Strictly prevents data leaks across private conversations.</p>
                </div>
              </div>

              {onSelectUserToChat && (
                <button
                  type="button"
                  className="btn-primary"
                  style={{ marginTop: "12px", width: "100%" }}
                  onClick={() => {
                    onClose();
                    onSelectUserToChat();
                  }}
                >
                  Start or Select a Chat →
                </button>
              )}
            </div>
          )}

          {activeTab === "media" && (
            <div className="features-section">
              <h4>🖼️ Phase 2: Secure Media Delivery & Viewer</h4>
              <p className="small muted">
                Comprehensive visual media pipeline with strict server-side validation and progressive playback.
              </p>

              <div className="feature-cards-stack">
                <div className="feature-card">
                  <strong>Magic-Byte Verification</strong>
                  <p className="small muted">Validates real binary header signatures for JPEG, PNG, WebP, MP4, WebM to prevent spoofing.</p>
                </div>
                <div className="feature-card">
                  <strong>HTTP 206 Byte-Range Streaming</strong>
                  <p className="small muted">Enables seamless video seeking and buffering without downloading entire media files.</p>
                </div>
                <div className="feature-card">
                  <strong>EXIF & Metadata Stripping</strong>
                  <p className="small muted">Automatically strips GPS coordinates, camera serials, and device metadata upon upload.</p>
                </div>
                <div className="feature-card">
                  <strong>Message Deletion & Tombstoning</strong>
                  <p className="small muted">Soft-delete for yourself or broadcast universal tombstones ("This message was deleted.") for both participants.</p>
                </div>
              </div>
            </div>
          )}

          {activeTab === "android" && (
            <div className="features-section">
              <h4>📱 Phase 3: Android Native Protection SDK</h4>
              <p className="small muted">
                Reusable Android library (<code>capture-protection-release.aar</code>) with zero UI dependencies, designed for high-security applications.
              </p>

              <div className="feature-cards-stack">
                <div className="feature-card">
                  <strong>Reference-Counted SecureWindowController</strong>
                  <p className="small muted">Enforces <code>WindowManager.LayoutParams.FLAG_SECURE</code> at the Android OS compositor level.</p>
                </div>
                <div className="feature-card">
                  <strong>Pre-Existing Flag Preservation</strong>
                  <p className="small muted">Tracks whether <code>FLAG_SECURE</code> was already present, ensuring cleanups never degrade baseline window security.</p>
                </div>
                <div className="feature-card">
                  <strong>Android 14+ ScreenCaptureCallback</strong>
                  <p className="small muted">Integrates native OS screenshot detection callback with graceful fallback on older Android versions.</p>
                </div>
                <div className="feature-card">
                  <strong>Physical Device QA Verification</strong>
                  <p className="small muted">Verified on hardware (Xiaomi 2312DRAABI running Android 15 API 35) across all 6 security checkpoints.</p>
                </div>
              </div>

              <div className="sdk-code-snippet">
                <code>
                  {`val controller = SecureWindowController(activity)\ncontroller.acquireProtection("media_viewer")\ncontroller.attachLifecycle(this)`}
                </code>
              </div>
            </div>
          )}

          {activeTab === "optical" && (
            <div className="features-section">
              <h4>🔬 Phase 4A & 4B: Optical Anti-Capture Research</h4>
              <p className="small muted">
                Active display modulation algorithms combating unauthorized physical smartphone photography ("The Analog Hole").
              </p>

              <div className="feature-cards-stack">
                <div className="feature-card">
                  <strong>6 Spatial Modulation Patterns</strong>
                  <p className="small muted">Horizontal lines, vertical lines, diagonal lines, checkerboard, radial grid, and sine waves designed to alias against camera Bayer CFAs.</p>
                </div>
                <div className="feature-card">
                  <strong>Choreographer Hardware VSYNC Alternation</strong>
                  <p className="small muted">Synchronized phase alternation exploiting the gap between human flicker fusion (~15-60 Hz) and camera electronic rolling shutters.</p>
                </div>
                <div className="feature-card">
                  <strong>WCAG 2.1 Photobiological Compliance</strong>
                  <p className="small muted">Hard-coded safety ceiling of f ≤ 3.0 Hz and ≤ 15% contrast to prevent asthenopia and seizure hazards, backed by an omnipresent Emergency Stop.</p>
                </div>
              </div>

              <h5 style={{ marginTop: "14px", marginBottom: "8px" }}>7 Standardized Reference Test Charts</h5>
              <div className="test-charts-mini-list">
                {TEST_CHARTS.map((c) => (
                  <div key={c.id} className="test-chart-mini-item">
                    <strong>{c.name}</strong>: <span className="small muted">{c.desc}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        <div className="modal-actions">
          <button type="button" className="btn-secondary" onClick={onClose}>
            Close
          </button>
        </div>
      </div>
    </div>
  );
}

