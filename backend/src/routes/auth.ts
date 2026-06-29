import { Hono } from "hono";
import type { AppBindings } from "../types";

export const authRoutes = new Hono<AppBindings>();

// Exchange a Google ID token for a Firebase session (idToken + refreshToken).
authRoutes.post("/google", async (c) => {
  const body = await c.req.json<{ googleIdToken?: string }>();
  if (!body.googleIdToken) {
    return c.json({ error: "google_id_token_required" }, 400);
  }

  const url = `https://identitytoolkit.googleapis.com/v1/accounts:signInWithIdp?key=${c.env.FIREBASE_WEB_API_KEY}`;
  const res = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      postBody: `id_token=${body.googleIdToken}&providerId=google.com`,
      requestUri: "http://localhost",
      returnIdpCredential: true,
      returnSecureToken: true,
    }),
  });

  if (!res.ok) {
    const err = await res.json();
    return c.json({ error: "auth_failed", detail: err }, 401);
  }

  const data = await res.json() as {
    idToken: string;
    refreshToken: string;
    expiresIn: string;
    localId: string;
    email?: string;
    displayName?: string;
    photoUrl?: string;
  };

  return c.json({
    idToken: data.idToken,
    refreshToken: data.refreshToken,
    expiresIn: data.expiresIn,
    uid: data.localId,
    email: data.email ?? null,
    displayName: data.displayName ?? null,
    photoUrl: data.photoUrl ?? null,
  });
});

// Exchange a refresh token for a fresh Firebase ID token.
authRoutes.post("/refresh", async (c) => {
  const body = await c.req.json<{ refreshToken?: string }>();
  if (!body.refreshToken) {
    return c.json({ error: "refresh_token_required" }, 400);
  }

  const url = `https://securetoken.googleapis.com/v1/token?key=${c.env.FIREBASE_WEB_API_KEY}`;
  const res = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=refresh_token&refresh_token=${encodeURIComponent(body.refreshToken)}`,
  });

  if (!res.ok) {
    const err = await res.json();
    return c.json({ error: "refresh_failed", detail: err }, 401);
  }

  const data = await res.json() as {
    id_token: string;
    refresh_token: string;
    expires_in: string;
    user_id: string;
  };

  return c.json({
    idToken: data.id_token,
    refreshToken: data.refresh_token,
    expiresIn: data.expires_in,
    uid: data.user_id,
  });
});
