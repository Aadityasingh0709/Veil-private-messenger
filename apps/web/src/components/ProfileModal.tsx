import type { AccountUser } from "@secure-chat/shared";
import { Avatar } from "./Avatar";

interface ProfileModalProps {
  user: AccountUser;
  isOpen: boolean;
  onClose: () => void;
  onOpenFeatures?: () => void;
  onLogout: () => void;
}

export function ProfileModal({ user, isOpen, onClose, onOpenFeatures, onLogout }: ProfileModalProps) {
  if (!isOpen) return null;

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-card" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3>Your Profile</h3>
          <button type="button" className="btn-close" onClick={onClose} aria-label="Close">
            ×
          </button>
        </div>

        <div className="profile-body">
          <div className="profile-identity">
            <Avatar user={user} size="lg" showStatus />
            <div className="profile-names">
              <h2>{user.name}</h2>
              <p className="muted">@{user.username}</p>
            </div>
          </div>

          <div className="profile-details-list">
            <div className="detail-item">
              <span className="detail-label">Email</span>
              <span className="detail-value">{user.email}</span>
            </div>
            <div className="detail-item">
              <span className="detail-label">Account Status</span>
              <span className="detail-value">
                <span className="badge-online">Active / Online</span>
              </span>
            </div>
            <div className="detail-item">
              <span className="detail-label">Current Phase</span>
              <span className="detail-value">Phase 4B (Display Modulation & Anti-Capture Research)</span>
            </div>
          </div>

          <div className="profile-notice">
            <strong>About Phase 4B:</strong>
            <p className="muted small">
              Veil combines real-time messaging, secure media streaming, native Android
              window compositor capture defense (FLAG_SECURE), and pioneering optical
              anti-capture display modulation research.
            </p>
            {onOpenFeatures && (
              <button
                type="button"
                className="btn-secondary"
                style={{ marginTop: "10px", width: "100%", fontSize: "12px", padding: "6px 10px" }}
                onClick={() => {
                  onClose();
                  onOpenFeatures();
                }}
              >
                🛡️ Open Capabilities & Security Hub →
              </button>
            )}
          </div>
        </div>

        <div className="modal-actions">
          <button type="button" className="btn-secondary" onClick={onClose}>
            Close
          </button>
          <button type="button" className="btn-danger" onClick={onLogout}>
            Sign Out
          </button>
        </div>
      </div>
    </div>
  );
}
