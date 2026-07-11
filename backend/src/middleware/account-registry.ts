import { createMiddleware } from "hono/factory";
import { eq } from "drizzle-orm";
import { getDb } from "../db/client";
import { users } from "../db/schema";
import type { AppBindings } from "../types";

export const accountRegistry = createMiddleware<AppBindings>(async (c, next) => {
  const claims = c.get("claims");
  const db = getDb(c.env.DB);
  const now = new Date();

  const existingUid = await db
    .select({ firebaseUid: users.firebaseUid })
    .from(users)
    .where(eq(users.firebaseUid, claims.uid))
    .limit(1);

  if (existingUid.length > 0) {
    await next();
    return;
  }

  if (claims.email) {
    const existingEmail = await db
      .select({ firebaseUid: users.firebaseUid })
      .from(users)
      .where(eq(users.email, claims.email))
      .limit(1);

    if (existingEmail.length > 0) {
      await db.delete(users).where(eq(users.email, claims.email));
    }
  }

  await db.insert(users).values({
    firebaseUid: claims.uid,
    email: claims.email ?? null,
    displayName: (claims.name as string | undefined) ?? null,
    photoUrl: (claims.picture as string | undefined) ?? null,
    updatedAt: now,
  }).onConflictDoNothing();

  await next();
});
