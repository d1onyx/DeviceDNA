import { Hono } from "hono";
import { and, eq } from "drizzle-orm";
import { getDb } from "../db/client";
import { devices, users } from "../db/schema";
import { writeSnapshot, type DeviceSnapshot } from "../lib/snapshot";
import { cancelAccountSubscriptionBeforeDelete, getSubscriptionView } from "./subscriptions";
import type { AppBindings } from "../types";

export const syncRoutes = new Hono<AppBindings>();

// Current authenticated account. Used by the app on startup to reject deleted
// Firebase Auth users before opening the signed-in UI.
syncRoutes.get("/me", async (c) => {
  const user = c.get("firebaseUser");
  const claims = c.get("claims");
  const db = getDb(c.env.DB);
  const subscription = await getSubscriptionView(db, claims.uid);

  return c.json({
    exists: true,
    uid: user.localId,
    email: user.email ?? null,
    displayName: user.displayName ?? null,
    photoUrl: user.photoUrl ?? null,
    premium: subscription.premium,
    subscription: subscription.subscription,
  });
});

// Delete the account and all associated data (App Store 5.1.1(v) / Google Play requirement).
// The app calls this while still authenticated, right before it deletes the Firebase Auth user.
// Removing the users row cascades to devices + user_subscriptions via their onDelete FKs.
syncRoutes.delete("/me", async (c) => {
  const claims = c.get("claims");
  const db = getDb(c.env.DB);

  const cancellation = await cancelAccountSubscriptionBeforeDelete(db, c.env, claims.uid);
  if (!cancellation.ok) {
    const status = cancellation.error === "subscription_cancellation_failed" ? 502 : 503;
    return c.json(
      {
        error: cancellation.error,
        detail: cancellation.detail ?? null,
      },
      status,
    );
  }

  await db.delete(users).where(eq(users.firebaseUid, claims.uid));
  return c.json({ deleted: true, subscriptionCanceled: cancellation.canceled });
});

// Cheap sync-status check for a specific device
syncRoutes.get("/devices/:androidId/status", async (c) => {
  const claims = c.get("claims");
  const androidId = c.req.param("androidId");
  const db = getDb(c.env.DB);

  const rows = await db
    .select({ snapshotHash: devices.snapshotHash, lastSyncedAt: devices.lastSyncedAt })
    .from(devices)
    .where(and(eq(devices.userUid, claims.uid), eq(devices.androidId, androidId)))
    .limit(1);

  if (rows.length === 0) {
    return c.json({ exists: false, snapshotHash: null, lastSyncedAt: null });
  }
  return c.json({
    exists: true,
    snapshotHash: rows[0].snapshotHash,
    lastSyncedAt: rows[0].lastSyncedAt,
  });
});

interface SyncBody {
  androidId: string;
  deviceName?: string;
  manufacturer?: string;
  model?: string;
  osVersion?: string;
  appVersion?: string;
  snapshotHash?: string;
  snapshot?: DeviceSnapshot;
}

// Full device snapshot push. Upserts user + device by (uid, androidId).
syncRoutes.post("/sync", async (c) => {
  const claims = c.get("claims");
  const body = (await c.req.json()) as SyncBody;

  if (!body?.androidId) {
    return c.json({ error: "android_id_required" }, 400);
  }

  const db = getDb(c.env.DB);
  const now = new Date();

  // 1) User (data comes from VERIFIED claims, not from the request body)
  await db
    .insert(users)
    .values({
      firebaseUid: claims.uid,
      email: claims.email ?? null,
      displayName: (claims.name as string | undefined) ?? null,
      photoUrl: (claims.picture as string | undefined) ?? null,
      updatedAt: now,
    })
    .onConflictDoUpdate({
      target: users.firebaseUid,
      set: {
        email: claims.email ?? null,
        displayName: (claims.name as string | undefined) ?? null,
        photoUrl: (claims.picture as string | undefined) ?? null,
        updatedAt: now,
      },
    });

  // 2) Device — one row per (user, androidId). Keep the row id stable across
  // re-syncs so the normalized snapshot tables can be re-keyed to it.
  const [device] = await db
    .insert(devices)
    .values({
      userUid: claims.uid,
      androidId: body.androidId,
      deviceName: body.deviceName ?? null,
      manufacturer: body.manufacturer ?? null,
      model: body.model ?? null,
      osVersion: body.osVersion ?? null,
      appVersion: body.appVersion ?? null,
      snapshotHash: body.snapshotHash ?? null,
      lastSyncedAt: now,
    })
    .onConflictDoUpdate({
      target: [devices.userUid, devices.androidId],
      set: {
        deviceName: body.deviceName ?? null,
        manufacturer: body.manufacturer ?? null,
        model: body.model ?? null,
        osVersion: body.osVersion ?? null,
        appVersion: body.appVersion ?? null,
        snapshotHash: body.snapshotHash ?? null,
        lastSyncedAt: now,
      },
    })
    .returning({ id: devices.id });

  // 3) Full diagnostics snapshot, exploded into the normalized tables.
  if (body.snapshot) {
    await writeSnapshot(db, device.id, body.snapshot);
  }

  return c.json({ synced: true, lastSyncedAt: now.toISOString() });
});
