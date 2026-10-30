import { useState } from "react";
import type { MessageDto } from "@secure-chat/shared";
import { api } from "../api";

interface MessageBubbleProps {
  message: MessageDto;
  isMine: boolean;
  onOpenMedia?: (url: string, type: "image" | "video", title?: string) => void;
  onDelete: (messageId: string, scope: "me" | "everyone") => Promise<void>;
}

function formatMessageTime(dateStr: string): string {
  const d = new Date(dateStr);
  return d.toLocaleTimeString([], { hour: "numeric", minute: "2-digit" });
}

function formatFileSize(bytes?: number): string {
  if (!bytes) return "";
  if (bytes < 1024 * 1024) {
    return `${Math.round(bytes / 1024)} KB`;
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function MessageBubble({ message, isMine, onOpenMedia, onDelete }: MessageBubbleProps) {
  const time = formatMessageTime(message.createdAt);
  const mediaId = message.mediaAssetId || message.media?.id;
  const mediaUrl = mediaId ? api.getMediaUrl(mediaId, message.conversationId) : "";
  const [imageLoaded, setImageLoaded] = useState(false);
  const [mediaError, setMediaError] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);

  const isImage = message.type === "image" && Boolean(mediaUrl);
  const isVideo = message.type === "video" && Boolean(mediaUrl);

  const deleteMessage = async (scope: "me" | "everyone") => {
    if (scope === "everyone" && !window.confirm("Delete this message for everyone? Recipients may already have saved a copy.")) {
      return;
    }
    setMenuOpen(false);
    await onDelete(message.id, scope);
  };

  const handleMediaClick = () => {
    if (!mediaUrl || mediaError) return;
    if (isImage) {
      onOpenMedia?.(mediaUrl, "image", message.media?.originalName || "Photo");
    } else if (isVideo) {
      onOpenMedia?.(mediaUrl, "video", message.media?.originalName || "Video");
    }
  };

  return (
    <div className={`message-row ${isMine ? "row-mine" : "row-theirs"}`}>
      <div className={`message-bubble ${isMine ? "bubble-mine" : "bubble-theirs"} ${isImage || isVideo ? "bubble-media" : ""}`}>
        {!message.deletedForEveryone && (
          <div className="message-actions">
            <button
              type="button"
              className="message-actions-trigger"
              aria-label="Message actions"
              aria-haspopup="menu"
              aria-expanded={menuOpen}
              onClick={() => setMenuOpen((open) => !open)}
            >
              ⋯
            </button>
            {menuOpen && (
              <div className="message-actions-menu" role="menu" aria-label="Message actions">
                <button type="button" role="menuitem" onClick={() => void deleteMessage("me")}>Delete for me</button>
                {isMine && (
                  <button type="button" role="menuitem" onClick={() => void deleteMessage("everyone")}>Delete for everyone</button>
                )}
              </div>
            )}
          </div>
        )}
        {message.deletedForEveryone ? (
          <div className="bubble-deleted" aria-label="Message deleted">This message was deleted.</div>
        ) : (
          <>
        {isImage && (
          <div className="bubble-media-wrapper" onClick={handleMediaClick} role="button" tabIndex={0} onKeyDown={(e) => e.key === "Enter" && handleMediaClick()}>
            {!imageLoaded && !mediaError && (
              <div className="media-placeholder">
                <span className="spinner small" />
              </div>
            )}
            {mediaError ? (
              <div className="media-load-error">
                <span>⚠️ Image failed to load</span>
              </div>
            ) : (
              <img
                src={mediaUrl}
                alt={message.media?.originalName || "Attached photo"}
                className={`bubble-image-preview ${imageLoaded ? "loaded" : "loading"}`}
                crossOrigin="use-credentials"
                loading="lazy"
                onLoad={() => setImageLoaded(true)}
                onError={() => setMediaError(true)}
              />
            )}
            <div className="media-overlay-hint">
              <span>Click to view</span>
            </div>
          </div>
        )}

        {isVideo && (
          <div className="bubble-media-wrapper bubble-video-wrapper" onClick={handleMediaClick} role="button" tabIndex={0} onKeyDown={(e) => e.key === "Enter" && handleMediaClick()}>
            {mediaError ? (
              <div className="media-load-error">
                <span>⚠️ Video failed to load</span>
              </div>
            ) : (
              <>
                <video
                  src={mediaUrl}
                  className="bubble-video-preview"
                  crossOrigin="use-credentials"
                  preload="metadata"
                  onError={() => setMediaError(true)}
                />
                <div className="video-play-badge">
                  <span className="play-icon">▶</span>
                  {message.media?.size && (
                    <span className="video-size-label">{formatFileSize(message.media.size)}</span>
                  )}
                </div>
              </>
            )}
          </div>
        )}

        {message.text && (
          <div className="bubble-text">{message.text}</div>
        )}
          </>
        )}

        <div className="bubble-meta">
          <span className="bubble-time">{time}</span>
          {isMine && !message.deletedForEveryone && (
            <span className={`bubble-status status-${message.status}`} title={`Status: ${message.status}`}>
              {message.status === "read" && "✓✓"}
              {message.status === "delivered" && "✓✓"}
              {message.status === "sent" && "✓"}
            </span>
          )}
        </div>
      </div>
    </div>
  );
}
