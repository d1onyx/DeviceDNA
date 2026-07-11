import { createMiddleware } from "hono/factory";
import type { AppBindings, FirebaseAccount } from "../types";

// Cache live account lookups for 5 minutes to avoid an RTT to Google on every request.
// Worst-case propagation delay for a newly-disabled account is the same TTL.
const ACCOUNT_CACHE_TTL_SECONDS = 300;

interface AccountLookupResponse {
  users?: FirebaseAccount[];
  error?: {
    message?: string;
  };
}

export const firebaseAccountExists = createMiddleware<AppBindings>(async (c, next) => {
  const claims = c.get("claims");
  const kv = c.env.PUBLIC_JWK_CACHE_KV;
  const cacheKey = `firebase-account:${claims.uid}`;

  // GET/DELETE /v1/me is the explicit "is this account still alive" probe the app runs on
  // startup and resume. Serving it from the 5-minute cache would let a device stay signed in
  // for up to 5 minutes after the account was deleted on another device, so always do a live
  // lookup here. The cache still shortcuts the high-frequency /sync and /subscription calls.
  const isAccountProbe = c.req.path === "/v1/me";

  const cached = isAccountProbe ? null : await kv.get<FirebaseAccount>(cacheKey, "json");
  if (cached) {
    c.set("firebaseUser", cached);
    await next();
    return;
  }

  const idToken = c.get("idToken");
  const response = await fetch(
    `https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=${c.env.FIREBASE_WEB_API_KEY}`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ idToken }),
    },
  );

  const data = (await response.json().catch(() => ({}))) as AccountLookupResponse;
  const user = data.users?.[0];

  if (!response.ok || !user) {
    return c.json({ error: "account_not_found" }, 404);
  }

  if (user.disabled) {
    return c.json({ error: "account_disabled" }, 403);
  }

  await kv.put(cacheKey, JSON.stringify(user), { expirationTtl: ACCOUNT_CACHE_TTL_SECONDS });

  c.set("firebaseUser", user);
  await next();
});
