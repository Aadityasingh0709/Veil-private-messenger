import assert from "node:assert/strict";
import jwt from "jsonwebtoken";
import { Types } from "mongoose";
import { config } from "./config.js";
import { RevocationStoreUnavailableError, revokeToken, signToken, verifyToken } from "./auth.js";
import { RevokedToken } from "./models.js";

const originalExists = RevokedToken.exists;
const originalCreate = RevokedToken.create;

try {
  Object.defineProperty(RevokedToken, "exists", {
    configurable: true,
    writable: true,
    value: async () => {
      throw new Error("simulated revocation lookup failure");
    }
  });

  const token = signToken(new Types.ObjectId().toString());
  await assert.rejects(verifyToken(token), RevocationStoreUnavailableError);

  Object.defineProperty(RevokedToken, "create", {
    configurable: true,
    writable: true,
    value: async () => {
      throw new Error("simulated revocation write failure");
    }
  });
  await assert.rejects(revokeToken(token), RevocationStoreUnavailableError);

  const expiredToken = jwt.sign(
    { sub: new Types.ObjectId().toString(), jti: "expired-test-token" },
    config.jwtSecret,
    { expiresIn: -1 }
  );
  const expiredResult = await revokeToken(expiredToken);
  assert.equal(expiredResult.revoked, false);

  console.log("✓ Revocation lookup failures fail closed");
  console.log("✓ Revocation write failures prevent successful logout state");
  console.log("✓ Expired credentials cannot be revoked into a valid session");
} finally {
  Object.defineProperty(RevokedToken, "exists", { configurable: true, writable: true, value: originalExists });
  Object.defineProperty(RevokedToken, "create", { configurable: true, writable: true, value: originalCreate });
}
