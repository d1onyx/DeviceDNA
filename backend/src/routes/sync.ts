import { Hono } from "hono";
import { and, eq } from "drizzle-orm";
import { getDb } from "../db/client";
import { devices, users } from "../db/schema";
import { firebaseAuth } from "../middleware/auth";
import type { AppBindings } from "../types";

export const syncRoutes = new Hono<AppBindings>();

// All /v1 routes require a valid Firebase ID token
syncRoutes.use("*", firebaseAuth);

// Cheap sync-status check for a specific device
syncRoutes.get("/devices/:androidId/status", async (c) => {
  const claims = c.get("claims");
  const androidId = c.req.param("androidId");
  const db = getDb(c.env.DATABASE_URL);

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
  snapshot?: unknown;
}

// Full device snapshot push. Upserts user + device by (uid, androidId).
syncRoutes.post("/sync", async (c) => {
  const claims = c.get("claims");
  const body = (await c.req.json()) as SyncBody;

  if (!body?.androidId) {
    return c.json({ error: "android_id_required" }, 400);
  }

  const db = getDb(c.env.DATABASE_URL);
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

  // 2) Device — one row per (user, androidId)
  await db
    .insert(devices)
    .values({
      userUid: claims.uid,
      androidId: body.androidId,
      deviceName: body.deviceName ?? null,
      manufacturer: body.manufacturer ?? null,
      model: body.model ?? null,
      osVersion: body.osVersion ?? null,
      appVersion: body.appVersion ?? null,
      snapshot: body.snapshot ?? null,
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
        snapshot: body.snapshot ?? null,
        snapshotHash: body.snapshotHash ?? null,
        lastSyncedAt: now,
      },
    });

  return c.json({ synced: true, lastSyncedAt: now.toISOString() });
});
