import type { NextFunction, Request, Response } from "express";
import jwt from "jsonwebtoken";
import crypto from "node:crypto";
import { config } from "./config.js";
import { RevokedToken } from "./models.js";

export type AuthRequest = Request & { userId?: string; tokenJti?: string };

export interface TokenPayload {
  sub: string;
  jti?: string;
  exp?: number;
  iat?: number;
}

// In-memory set for low-latency revocation checks
const revokedJtiCache = new Set<string>();

export const signToken = (userId: string) => {
  const jti = crypto.randomUUID();
  return jwt.sign({ sub: userId, jti }, config.jwtSecret, { expiresIn: "7d" });
};

export const isTokenRevoked = async (jti?: string): Promise<boolean> => {
  if (!jti) return false;
  if (revokedJtiCache.has(jti)) return true;
  try {
    const exists = await RevokedToken.exists({ jti });
    if (exists) {
      revokedJtiCache.add(jti);
      return true;
    }
  } catch {
    // If database check fails, fallback to cache
  }
  return false;
};

export const revokeToken = async (token: string): Promise<{ revoked: boolean; jti?: string; userId?: string }> => {
  try {
    const decoded = jwt.decode(token) as TokenPayload | null;
    if (!decoded) return { revoked: false };
    const jti = decoded.jti || crypto.createHash("sha256").update(token).digest("hex");
    revokedJtiCache.add(jti);
    const expiresAt = decoded.exp ? new Date(decoded.exp * 1000) : new Date(Date.now() + 7 * 24 * 60 * 60 * 1000);
    await RevokedToken.create({
      jti,
      userId: decoded.sub,
      expiresAt
    }).catch((err: any) => {
      if (err.code !== 11000) console.error("Error storing revoked token:", err.message);
    });
    return { revoked: true, jti, userId: decoded.sub };
  } catch {
    return { revoked: false };
  }
};

export const verifyToken = async (token: string): Promise<{ userId: string; jti: string }> => {
  const decoded = jwt.verify(token, config.jwtSecret) as TokenPayload;
  const jti = decoded.jti || crypto.createHash("sha256").update(token).digest("hex");
  if (await isTokenRevoked(jti)) {
    throw new Error("Token has been revoked");
  }
  return { userId: decoded.sub, jti };
};

export const extractToken = (req: Request): string | undefined => {
  const cookieToken = req.cookies?.token;
  const authHeader = req.headers.authorization;
  const bearerToken = authHeader ? authHeader.replace(/^Bearer\s+/i, "").trim() : undefined;
  // NOTE: Token via query parameter (req.query.token) is intentionally NOT supported for security.
  return cookieToken || bearerToken || undefined;
};

export const requireAuth = async (req: AuthRequest, res: Response, next: NextFunction) => {
  try {
    const token = extractToken(req);
    if (!token) {
      return res.status(401).json({ error: "Authentication required" });
    }
    const { userId, jti } = await verifyToken(token);
    req.userId = userId;
    req.tokenJti = jti;
    next();
  } catch {
    res.status(401).json({ error: "Invalid or expired session" });
  }
};

export const cookieOptions = {
  httpOnly: true,
  sameSite: "lax" as const,
  secure: config.isProduction,
  maxAge: 7 * 24 * 60 * 60 * 1000,
  path: "/"
};
