import { createMiddleware } from "hono/factory";
import type { AppBindings, FirebaseAccount } from "../types";

interface AccountLookupResponse {
  users?: FirebaseAccount[];
  error?: {
    message?: string;
  };
}

export const firebaseAccountExists = createMiddleware<AppBindings>(async (c, next) => {
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

  c.set("firebaseUser", user);
  await next();
});
