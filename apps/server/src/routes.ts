import fs from "node:fs";
import crypto from "node:crypto";
import { Router, type Request, type Response } from "express";
import argon2 from "argon2";
import multer from "multer";
import { Types } from "mongoose";
import rateLimit from "express-rate-limit";
import { ALLOWED_MEDIA_MIME_TYPES, conversationSchema, loginSchema, registerSchema } from "@secure-chat/shared";
import { cookieOptions, extractToken, requireAuth, revokeToken, signToken, type AuthRequest } from "./auth.js";
import { config } from "./config.js";
import { Conversation, MediaAsset, Message, User } from "./models.js";
import { accountDto, conversationDto, messageDto, userDto } from "./serializers.js";
import { generateStorageKey, storage } from "./storage.js";
import { detectSignature, extractImageDimensions, sanitizeFilename, stripExifFromJpeg, stripExifFromPng } from "./mediaValidation.js";
import { disconnectSessionSockets, getIO } from "./socket.js";

// Helper for Zod request validation
const parse = <T>(schema: { safeParse: (value: unknown) => any }, value: unknown, res: Response): T | undefined => {
  const result = schema.safeParse(value);
  if (!result.success) {
    res.status(400).json({ error: "Invalid request payload", details: result.error.flatten() });
    return undefined;
  }
  return result.data as T;
};

// Verifies that a conversation exists and the requesting user is a participant (Authorization check)
const ownConversation = async (id: string, userId: string) => {
  if (!Types.ObjectId.isValid(id)) return null;
  return Conversation.findOne({ _id: id, participants: userId });
};

const conversationKey = (firstUserId: string, secondUserId: string) =>
  [firstUserId, secondUserId].sort().join(":");

const isDuplicateKeyError = (error: unknown): boolean =>
  typeof error === "object" && error !== null && "code" in error && error.code === 11000;

// Rate limiter for media uploads: 30 uploads per 10 minutes per IP
const mediaUploadRateLimiter = rateLimit({
  windowMs: 10 * 60 * 1000,
  max: 30,
  message: { error: "Upload rate limit exceeded. Please wait a few moments." },
  standardHeaders: true,
  legacyHeaders: false
});

// Configure multer for bounded temporary disk storage outside public directories
const tempStorage = multer.diskStorage({
  destination: (_req, _file, cb) => {
    cb(null, config.tempStorageDir);
  },
  filename: (_req, _file, cb) => {
    const randomHex = crypto.randomBytes(16).toString("hex");
    cb(null, `upload-${Date.now()}-${randomHex}.tmp`);
  }
});

const upload = multer({
  storage: tempStorage,
  limits: {
    fileSize: config.maxVideoSizeBytes,
    files: 1,
    fields: 5,
    fieldSize: 16 * 1024,
    parts: 10,
    fieldNameSize: 100,
    fieldArrayIndexLimit: 0,
    fieldNestingDepth: 0
  } as any
});

const uploadMiddleware = (req: any, res: any, next: any) => {
  upload.single("file")(req, res, (err: any) => {
    if (err) {
      if (req.file?.path) {
        fs.promises.unlink(req.file.path).catch(() => {});
      }
      if (err.code === "LIMIT_FILE_SIZE") {
        return res.status(400).json({
          error: `File size exceeds the maximum allowed limit (${Math.round(config.maxVideoSizeBytes / (1024 * 1024))}MB)`
        });
      }
      if (err.code === "LIMIT_FILE_COUNT") {
        return res.status(400).json({ error: "Only a single file upload is permitted" });
      }
      if (err.code === "LIMIT_FIELD_VALUE") {
        return res.status(400).json({ error: "Form field value is too large" });
      }
      if (err.code === "LIMIT_FIELD_COUNT") {
        return res.status(400).json({ error: "Too many form fields in upload" });
      }
      if (err.code === "LIMIT_PART_COUNT") {
        return res.status(400).json({ error: "Too many multipart sections in upload" });
      }
      return res.status(400).json({ error: err.message || "Failed to process media upload" });
    }
    req.on("close", () => {
      if (!res.writableEnded && req.file?.path) {
        fs.promises.unlink(req.file.path).catch(() => {});
      }
    });
    next();
  });
};

// ==========================================
// Authentication Routes (/api/auth)
// ==========================================
export const authRouter = Router();

// Register new user
authRouter.post("/register", async (req, res, next) => {
  try {
    const data = parse<any>(registerSchema, req.body, res);
    if (!data) return;

    const exists = await User.exists({
      $or: [{ email: data.email }, { username: data.username }]
    });
    if (exists) {
      return res.status(409).json({ error: "Email or username is already in use" });
    }

    const passwordHash = await argon2.hash(data.password);
    const user = await User.create({
      name: data.name,
      username: data.username,
      email: data.email,
      passwordHash
    });

    const token = signToken(String(user._id));
    res.cookie("token", token, cookieOptions).status(201).json({ user: accountDto(user) });
  } catch (err) {
    if (isDuplicateKeyError(err)) {
      return res.status(409).json({ error: "Email or username is already in use" });
    }
    next(err);
  }
});

// Login existing user
authRouter.post("/login", async (req, res, next) => {
  try {
    const data = parse<any>(loginSchema, req.body, res);
    if (!data) return;

    const user = await User.findOne({ email: data.email }).select("+passwordHash");
    if (!user || !(await argon2.verify(user.passwordHash, data.password))) {
      return res.status(401).json({ error: "Invalid email or password" });
    }

    const token = signToken(String(user._id));
    res.cookie("token", token, cookieOptions).json({ user: accountDto(user) });
  } catch (err) {
    next(err);
  }
});

// Logout user, revoke JWT session token, disconnect session sockets and clear session cookie
authRouter.post("/logout", async (req, res, next) => {
  try {
    const cookieToken = req.cookies?.token;
    const token = extractToken(req);
    if (token) {
      let result: Awaited<ReturnType<typeof revokeToken>>;
      try {
        result = await revokeToken(token);
      } catch (error) {
        console.error("Logout could not persist token revocation:", error);
        return res.status(503).json({ error: "Logout could not be completed. Please try again." });
      }
      if (!result.revoked) {
        if (cookieToken === token) {
          res.clearCookie("token", { path: "/", sameSite: "lax", secure: config.isProduction });
        }
        return res.status(204).send();
      }
      if (result.jti) {
        await disconnectSessionSockets(result.jti);
      }
      if (cookieToken === token) {
        res.clearCookie("token", { path: "/", sameSite: "lax", secure: config.isProduction });
      }
    }
    res.status(204).send();
  } catch (err) {
    next(err);
  }
});

// ==========================================
// Protected Application API Routes (/api)
// ==========================================
export const apiRouter = Router();
apiRouter.use(requireAuth);

// Get current user profile
apiRouter.get("/me", async (req: AuthRequest, res, next) => {
  try {
    const user = await User.findById(req.userId);
    if (!user) return res.status(404).json({ error: "User not found" });
    res.json({ user: accountDto(user) });
  } catch (err) {
    next(err);
  }
});

// Search users by name or username (excludes current user)
apiRouter.get("/users", async (req: AuthRequest, res, next) => {
  try {
    const q = String(req.query.q ?? "").trim();
    if (!q) return res.json({ users: [] });

    const escaped = q.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
    const users = await User.find({
      _id: { $ne: req.userId },
      $or: [
        { name: { $regex: escaped, $options: "i" } },
        { username: { $regex: escaped, $options: "i" } }
      ]
    }).limit(20);

    res.json({ users: users.map(userDto) });
  } catch (err) {
    next(err);
  }
});

// List conversations for the authenticated user
apiRouter.get("/conversations", async (req: AuthRequest, res, next) => {
  try {
    const items = await Conversation.find({ participants: req.userId })
      .populate("participants")
      .populate({
        path: "lastMessage",
        populate: { path: "mediaAsset" }
      })
      .sort({ lastMessageAt: -1, updatedAt: -1 });

    const conversations = await Promise.all(items.map(async (item) => {
      const lastMessage = item.lastMessage as any;
      const isHiddenForUser = !lastMessage?.deletedForEveryone
        && lastMessage?.deletedFor?.some((userId: Types.ObjectId) => String(userId) === req.userId);
      if (!isHiddenForUser) return conversationDto(item);

      const visibleMessage = await Message.findOne({
        conversationId: item._id,
        deletedForEveryone: { $ne: true },
        deletedFor: { $ne: req.userId }
      }).populate("mediaAsset").sort({ createdAt: -1, _id: -1 });
      const dto = conversationDto(item);
      dto.lastMessage = visibleMessage ? messageDto(visibleMessage) : undefined;
      dto.lastMessageAt = visibleMessage?.createdAt ? new Date(visibleMessage.createdAt).toISOString() : undefined;
      return dto;
    }));
    conversations.sort((a, b) =>
      (b.lastMessageAt ?? b.updatedAt).localeCompare(a.lastMessageAt ?? a.updatedAt)
    );
    res.json({ conversations });
  } catch (err) {
    next(err);
  }
});

// Start or get existing one-to-one conversation
apiRouter.post("/conversations", async (req: AuthRequest, res, next) => {
  try {
    const data = parse<any>(conversationSchema, req.body, res);
    if (!data) return;

    if (data.userId === req.userId) {
      return res.status(400).json({ error: "Cannot create a conversation with yourself" });
    }

    const recipient = await User.findById(data.userId);
    if (!recipient) {
      return res.status(404).json({ error: "Recipient user not found" });
    }

    const participantKey = conversationKey(req.userId!, data.userId);
    let conversation = await Conversation.findOne({ participantKey });

    if (!conversation) {
      conversation = await Conversation.findOne({
        participants: { $all: [req.userId, data.userId], $size: 2 }
      });

      if (conversation) {
        try {
          conversation.participantKey = participantKey;
          await conversation.save();
        } catch (err) {
          if (!isDuplicateKeyError(err)) throw err;
          conversation = await Conversation.findOne({ participantKey });
        }
      } else {
        try {
          conversation = await Conversation.create({
            participants: [req.userId, data.userId],
            participantKey
          });
        } catch (err) {
          if (!isDuplicateKeyError(err)) throw err;
          conversation = await Conversation.findOne({ participantKey });
        }
      }
    }

    if (!conversation) throw new Error("Conversation creation did not produce a conversation");
    await conversation.populate("participants");
    await conversation.populate({
      path: "lastMessage",
      populate: { path: "mediaAsset" }
    });
    res.status(201).json({ conversation: conversationDto(conversation) });
  } catch (err) {
    console.error("POST /conversations error:", err);
    next(err);
  }
});

// Get messages for a specific conversation (Strict Authorization check)
apiRouter.get("/conversations/:id/messages", async (req: AuthRequest, res, next) => {
  try {
    const conversation = await ownConversation(String(req.params.id), req.userId!);
    if (!conversation) {
      return res.status(404).json({ error: "Conversation not found or access denied" });
    }

    const messages = await Message.find({
      conversationId: conversation._id,
      deletedFor: { $ne: req.userId }
    })
      .populate("mediaAsset")
      .sort({ createdAt: -1, _id: -1 })
      .limit(200);
    messages.reverse();

    // Mark unread messages sent by others as read
    await Message.updateMany(
      { conversationId: conversation._id, senderId: { $ne: req.userId }, status: { $ne: "read" } },
      { status: "read" }
    );

    res.json({ messages: messages.map((m) => messageDto(m)) });
  } catch (err) {
    next(err);
  }
});

// Delete policy: users may hide any message from their own view; only its sender
// may tombstone it for everyone, with no time limit.
apiRouter.delete("/messages/:messageId", async (req: AuthRequest, res, next) => {
  try {
    const messageId = String(req.params.messageId);
    const scope = req.body?.scope;
    if (!Types.ObjectId.isValid(messageId) || (scope !== "me" && scope !== "everyone")) {
      return res.status(400).json({ error: "Invalid message deletion request" });
    }

    const message = await Message.findById(messageId);
    if (!message) return res.status(404).json({ error: "Message not found" });
    const conversation = await ownConversation(String(message.conversationId), req.userId!);
    if (!conversation) return res.status(404).json({ error: "Message not found" });

    if (scope === "everyone" && String(message.senderId) !== req.userId) {
      return res.status(403).json({ error: "Only the sender can delete a message for everyone" });
    }

    let updated = message;
    if (scope === "everyone" && !message.deletedForEveryone) {
      updated = (await Message.findOneAndUpdate(
        { _id: message._id, deletedForEveryone: { $ne: true } },
        { $set: { deletedForEveryone: true, deletedAt: new Date(), text: "" } },
        { new: true }
      )) ?? message;
    } else if (scope === "me" && !message.deletedForEveryone) {
      updated = (await Message.findOneAndUpdate(
        { _id: message._id, deletedForEveryone: { $ne: true } },
        { $addToSet: { deletedFor: req.userId } },
        { new: true }
      )) ?? message;
    }

    const io = getIO();
    if (io) {
      if (scope === "me") {
        io.to(`user:${req.userId}`).emit("message:deleted", {
          messageId: String(message._id),
          conversationId: String(message.conversationId),
          scope: "me",
          userId: req.userId
        });
      } else {
        for (const participant of conversation.participants) {
          io.to(`user:${participant}`).emit("message:deleted", {
            messageId: String(message._id),
            conversationId: String(message.conversationId),
            scope: "everyone"
          });
        }
      }
    }

    if (scope === "everyone" && message.mediaAssetId) {
      const activeReference = await Message.exists({
        mediaAssetId: message.mediaAssetId,
        deletedForEveryone: { $ne: true }
      });
      if (!activeReference) {
        const asset = await MediaAsset.findById(message.mediaAssetId).select("+storageKey");
        if (asset) {
          await storage.delete(asset.storageKey);
          await asset.deleteOne();
        }
      }
    }

    if (scope === "me") return res.json({ deleted: true });
    res.json({ message: messageDto(updated) });
  } catch (err) {
    next(err);
  }
});

// Upload a media message to a conversation with bounded temporary disk storage
apiRouter.post("/conversations/:id/media", mediaUploadRateLimiter, uploadMiddleware, async (req: AuthRequest, res: Response, next) => {
  let tempFilePath: string | null = req.file?.path ?? null;
  let savedStorageKey: string | null = null;
  try {
    const conversation = await ownConversation(String(req.params.id), req.userId!);
    if (!conversation) {
      if (tempFilePath) await fs.promises.unlink(tempFilePath).catch(() => {});
      return res.status(404).json({ error: "Conversation not found or access denied" });
    }

    const file = req.file;
    if (!file || !tempFilePath) {
      return res.status(400).json({ error: "No media file was uploaded" });
    }

    // Inspect file on disk to prevent memory exhaustion
    const stat = await fs.promises.stat(tempFilePath);
    if (stat.size === 0) {
      await fs.promises.unlink(tempFilePath).catch(() => {});
      return res.status(400).json({ error: "No media file was uploaded" });
    }

    // Read initial 64 bytes to detect magic byte signature
    const headerBuffer = Buffer.alloc(Math.min(64, stat.size));
    const fd = await fs.promises.open(tempFilePath, "r");
    try {
      await fd.read(headerBuffer, 0, headerBuffer.length, 0);
    } finally {
      await fd.close();
    }

    const detected = detectSignature(headerBuffer);
    if (!detected || !ALLOWED_MEDIA_MIME_TYPES.includes(detected.mimeType)) {
      await fs.promises.unlink(tempFilePath).catch(() => {});
      return res.status(400).json({
        error: "Unsupported file format. Please upload a valid JPEG, PNG, WebP image or MP4, WebM video."
      });
    }

    const { mimeType, ext, kind } = detected;

    // Enforce size limits before reading full file into memory
    const sizeLimit = kind === "image" ? config.maxImageSizeBytes : config.maxVideoSizeBytes;
    if (stat.size > sizeLimit) {
      await fs.promises.unlink(tempFilePath).catch(() => {});
      const limitMb = Math.round(sizeLimit / (1024 * 1024));
      return res.status(400).json({
        error: `File size exceeds the maximum allowed limit of ${limitMb}MB for ${kind}s.`
      });
    }

    const storageKey = generateStorageKey(kind, ext);
    let finalSize = stat.size;
    let width: number | undefined;
    let height: number | undefined;

    if (kind === "image") {
      // Images are buffered into memory to strip EXIF and extract dimensions (bounded by maxImageSizeBytes, e.g. 10MB)
      const rawBuffer = Buffer.from(await fs.promises.readFile(tempFilePath));
      let processed: Buffer = rawBuffer;
      if (mimeType === "image/jpeg") {
        processed = stripExifFromJpeg(rawBuffer);
      } else if (mimeType === "image/png") {
        processed = stripExifFromPng(rawBuffer);
      }
      const dims = extractImageDimensions(processed, mimeType as any);
      width = dims.width;
      height = dims.height;
      finalSize = processed.length;

      await storage.save(storageKey, processed);
      savedStorageKey = storageKey;
    } else {
      // Videos are streamed/copied directly to final storage from temporary file without heap buffering
      await storage.saveFile(storageKey, tempFilePath);
      savedStorageKey = storageKey;
    }

    // Clean up temporary file once stored
    await fs.promises.unlink(tempFilePath).catch(() => {});
    tempFilePath = null;

    const sanitizedFilename = sanitizeFilename(file.originalname, ext);

    const asset = await MediaAsset.create({
      uploaderId: req.userId,
      conversationId: conversation._id,
      storageKey,
      mimeType,
      size: finalSize,
      originalName: sanitizedFilename,
      mediaKind: kind,
      ...(width ? { width } : {}),
      ...(height ? { height } : {})
    });

    const caption = typeof req.body.caption === "string" ? req.body.caption.trim().slice(0, 4000) : "";
    const message = await Message.create({
      conversationId: conversation._id,
      senderId: req.userId,
      text: caption,
      type: kind,
      mediaAssetId: asset._id,
      status: "sent"
    });

    conversation.lastMessage = message._id;
    conversation.lastMessageAt = message.createdAt;
    await conversation.save();

    // Persisted successfully: clear savedStorageKey so cleanup will not delete it
    savedStorageKey = null;

    (message as any).mediaAsset = asset;
    const dto = messageDto(message);

    // Broadcast new message in real time to conversation participants
    const io = getIO();
    if (io) {
      for (const participant of conversation.participants) {
        io.to(`user:${participant}`).emit("message:new", dto);
      }
    }

    res.status(201).json({ message: dto });
  } catch (err) {
    if (tempFilePath) {
      await fs.promises.unlink(tempFilePath).catch(() => {});
    }
    if (savedStorageKey) {
      await storage.delete(savedStorageKey).catch(() => {});
    }
    next(err);
  }
});

// Helper to serve private media securely with participant check & HTTP byte-range requests
const serveMedia = async (
  mediaId: string,
  userId: string,
  req: Request,
  res: Response,
  expectedConversationId?: string
) => {
  // Always set non-persistent private caching policy on sensitive media responses
  res.setHeader("Cache-Control", "private, no-store");
  res.setHeader("Pragma", "no-cache");

  if (!Types.ObjectId.isValid(mediaId)) {
    return res.status(404).json({ error: "Media not found" });
  }

  const asset = await MediaAsset.findById(mediaId).select("+storageKey");
  if (!asset) {
    return res.status(404).json({ error: "Media not found" });
  }

  const visibleMessageExists = await Message.exists({
    mediaAssetId: asset._id,
    deletedForEveryone: { $ne: true },
    deletedFor: { $ne: userId }
  });
  if (!visibleMessageExists) {
    return res.status(404).json({ error: "Media not found" });
  }

  // Cross-conversation validation: ensure media belongs to requested conversation
  if (expectedConversationId) {
    if (!Types.ObjectId.isValid(expectedConversationId) || String(asset.conversationId) !== expectedConversationId) {
      return res.status(404).json({ error: "Media not found or access denied" });
    }
  }

  // Strict authorization: user must be participant in the conversation
  const isParticipant = await Conversation.exists({
    _id: asset.conversationId,
    participants: userId
  });
  if (!isParticipant) {
    return res.status(404).json({ error: "Media not found or access denied" });
  }

  const fileExists = await storage.exists(asset.storageKey);
  if (!fileExists) {
    return res.status(404).json({ error: "Media file unavailable" });
  }

  const totalSize = asset.size;
  const rangeHeader = req.headers.range;

  if (rangeHeader) {
    const match = rangeHeader.match(/bytes=(\d*)-(\d*)/);
    if (!match) {
      res.status(416).setHeader("Content-Range", `bytes */${totalSize}`).end();
      return;
    }

    let start: number;
    let end: number;

    if (!match[1] && match[2]) {
      // Suffix byte range: e.g. bytes=-50 (last 50 bytes)
      const suffixLength = parseInt(match[2], 10);
      if (isNaN(suffixLength) || suffixLength <= 0) {
        res.status(416).setHeader("Content-Range", `bytes */${totalSize}`).end();
        return;
      }
      start = Math.max(0, totalSize - suffixLength);
      end = totalSize - 1;
    } else {
      start = match[1] ? parseInt(match[1], 10) : 0;
      end = match[2] ? parseInt(match[2], 10) : totalSize - 1;
    }

    if (isNaN(start) || start >= totalSize || isNaN(end) || end < start) {
      res.status(416).setHeader("Content-Range", `bytes */${totalSize}`).end();
      return;
    }
    if (end >= totalSize) {
      end = totalSize - 1;
    }

    const chunkSize = end - start + 1;
    res.status(206);
    res.setHeader("Content-Range", `bytes ${start}-${end}/${totalSize}`);
    res.setHeader("Accept-Ranges", "bytes");
    res.setHeader("Content-Length", chunkSize);
    res.setHeader("Content-Type", asset.mimeType);
    res.setHeader("Content-Security-Policy", "default-src 'none'");
    res.setHeader("X-Content-Type-Options", "nosniff");
    res.setHeader("Cache-Control", "private, no-store");
    res.setHeader("Pragma", "no-cache");
    res.setHeader("Content-Disposition", `inline; filename="${encodeURIComponent(asset.originalName)}"`);

    const stream = storage.createReadStream(asset.storageKey, { start, end });
    res.on("close", () => {
      stream.destroy();
    });
    stream.on("error", () => {
      if (!res.headersSent) res.status(500).end();
    });
    stream.pipe(res);
  } else {
    res.status(200);
    res.setHeader("Content-Length", totalSize);
    res.setHeader("Content-Type", asset.mimeType);
    res.setHeader("Accept-Ranges", "bytes");
    res.setHeader("Content-Security-Policy", "default-src 'none'");
    res.setHeader("X-Content-Type-Options", "nosniff");
    res.setHeader("Cache-Control", "private, no-store");
    res.setHeader("Pragma", "no-cache");
    res.setHeader("Content-Disposition", `inline; filename="${encodeURIComponent(asset.originalName)}"`);

    const stream = storage.createReadStream(asset.storageKey);
    res.on("close", () => {
      stream.destroy();
    });
    stream.on("error", () => {
      if (!res.headersSent) res.status(500).end();
    });
    stream.pipe(res);
  }
};

// Retrieve media in the context of a conversation
apiRouter.get("/conversations/:id/media/:mediaId", async (req: AuthRequest, res: Response, next) => {
  try {
    await serveMedia(String(req.params.mediaId), req.userId!, req, res, String(req.params.id));
  } catch (err) {
    next(err);
  }
});

// Retrieve media by media ID directly (still strictly authorized by conversation membership)
apiRouter.get("/media/:mediaId", async (req: AuthRequest, res: Response, next) => {
  try {
    await serveMedia(String(req.params.mediaId), req.userId!, req, res);
  } catch (err) {
    next(err);
  }
});
