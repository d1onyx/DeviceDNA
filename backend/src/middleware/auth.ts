import { createMiddleware } from "hono/factory";
import { Auth, WorkersKVStoreSingle } from "firebase-auth-cloudflare-workers";
import type { AppBindings } from "../types";

// Verifies the Firebase ID token from the Authorization: Bearer <token> header.
// The signature is checked against Google's public keys (JWKS), cached in KV.
export const firebaseAuth = createMiddleware<AppBindings>(async (c, next) => {
  const header = c.req.header("Authorization") ?? "";
  const match = header.match(/^Bearer\s+(.+)$/i);
  if (!match) {
    return c.json({ error: "missing_bearer_token" }, 401);
  }

  try {
    const auth = Auth.getOrInitialize(
      c.env.FIREBASE_PROJECT_ID,
      WorkersKVStoreSingle.getOrInitialize(c.env.PUBLIC_JWK_CACHE_KEY, c.env.PUBLIC_JWK_CACHE_KV),
    );
    const claims = await auth.verifyIdToken(match[1]);
    c.set("claims", claims);
    c.set("idToken", match[1]);
    await next();
  } catch (_e) {
    return c.json({ error: "invalid_token" }, 401);
  }
});
