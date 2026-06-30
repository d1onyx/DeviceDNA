import { Hono } from "hono";
import { eq } from "drizzle-orm";
import { getDb } from "../db/client";
import { userSubscriptions, users, type UserSubscriptionRow } from "../db/schema";
import type { AppBindings } from "../types";
import {
  mapGooglePlayStatus,
  parseNullableDate,
  verifyGooglePlaySubscription,
  type GooglePlaySubscriptionPurchase,
} from "../lib/google-play";

const activeSubscriptionStatuses = new Set(["active", "trialing", "grace_period", "canceled"]);
const allowedSubscriptionStatuses = new Set([
  "inactive",
  "active",
  "trialing",
  "grace_period",
  "expired",
  "canceled",
]);

export type Db = ReturnType<typeof getDb>;

export interface SubscriptionView {
  premium: boolean;
  subscription: {
    status: string;
    provider: string;
    productId: string | null;
    originalTransactionId: string | null;
    latestTransactionId: string | null;
    expiresAt: string | null;
    updatedAt: string;
  } | null;
}

interface UpsertSubscriptionBody {
  status?: string;
  provider?: string;
  productId?: string | null;
  originalTransactionId?: string | null;
  latestTransactionId?: string | null;
  latestPurchaseToken?: string | null;
  expiresAt?: string | null;
}

export const subscriptionRoutes = new Hono<AppBindings>();
export const internalSubscriptionRoutes = new Hono<AppBindings>();

subscriptionRoutes.get("/subscription", async (c) => {
  const claims = c.get("claims");
  const db = getDb(c.env.DATABASE_URL);

  return c.json(await getSubscriptionView(db, claims.uid));
});

// Dev-only: grant a non-expiring Premium subscription persisted to Neon, mirroring the
// real verify flow (writes to user_subscriptions, returns the same SubscriptionView).
// Gated behind DEV_SUBSCRIPTIONS_ENABLED so it can never be reached in production.
subscriptionRoutes.post("/subscription/dev/activate", async (c) => {
  if (c.env.DEV_SUBSCRIPTIONS_ENABLED !== "true") {
    return c.json({ error: "dev_subscriptions_disabled" }, 403);
  }

  const claims = c.get("claims");
  const productId = c.env.GOOGLE_PLAY_PREMIUM_PRODUCT_ID ?? "devicedna_premium";
  const now = new Date();
  const expiresAt = null;
  const db = getDb(c.env.DATABASE_URL);

  const values = {
    userUid: claims.uid,
    status: "active",
    provider: "dev",
    productId,
    originalTransactionId: null,
    latestTransactionId: null,
    latestPurchaseToken: null,
    expiresAt,
    updatedAt: now,
  };

  await db
    .insert(userSubscriptions)
    .values(values)
    .onConflictDoUpdate({
      target: userSubscriptions.userUid,
      set: {
        status: values.status,
        provider: values.provider,
        productId: values.productId,
        originalTransactionId: values.originalTransactionId,
        latestTransactionId: values.latestTransactionId,
        latestPurchaseToken: values.latestPurchaseToken,
        expiresAt: values.expiresAt,
        updatedAt: values.updatedAt,
      },
    });

  return c.json(await getSubscriptionView(db, claims.uid));
});

subscriptionRoutes.post("/subscription/google-play/verify", async (c) => {
  const claims = c.get("claims");
  const body = await c.req.json<{ productId?: string; purchaseToken?: string } | null>();
  const productId = body?.productId?.trim();
  const purchaseToken = body?.purchaseToken?.trim();
  const expectedProductId = c.env.GOOGLE_PLAY_PREMIUM_PRODUCT_ID ?? "devicedna_premium";

  if (!productId || !purchaseToken) {
    return c.json({ error: "purchase_token_required" }, 400);
  }

  if (productId !== expectedProductId) {
    return c.json({ error: "invalid_product_id" }, 400);
  }

  const packageName = c.env.GOOGLE_PLAY_PACKAGE_NAME;
  const serviceAccountEmail = c.env.GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL;
  const privateKey = c.env.GOOGLE_PLAY_PRIVATE_KEY;
  if (!packageName || !serviceAccountEmail || !privateKey) {
    return c.json({ error: "google_play_not_configured" }, 503);
  }

  const purchase = await verifyGooglePlaySubscription({
    packageName,
    serviceAccountEmail,
    privateKey,
    purchaseToken,
  }).catch((error: unknown) => {
    const detail = error instanceof Error ? error.message : "Google Play verification failed.";
    return c.json({ error: "subscription_verification_failed", detail }, 502);
  });
  if (purchase instanceof Response) {
    return purchase;
  }
  const db = getDb(c.env.DATABASE_URL);
  const result = await upsertGooglePlaySubscription(db, {
    userUid: claims.uid,
    productId,
    purchaseToken,
    purchase,
  });

  if (!result.ok) {
    return result.reason === "product_not_purchased"
      ? c.json({ error: "product_not_purchased" }, 400)
      : c.json({ error: "invalid_google_play_expiry" }, 502);
  }

  return c.json(await getSubscriptionView(db, claims.uid));
});

internalSubscriptionRoutes.use("*", async (c, next) => {
  const expectedApiKey = c.env.INTERNAL_API_KEY;
  const actualApiKey = c.req.header("x-internal-api-key");

  if (!expectedApiKey) {
    return c.json({ error: "internal_api_key_not_configured" }, 503);
  }

  if (!actualApiKey || actualApiKey !== expectedApiKey) {
    return c.json({ error: "unauthorized" }, 401);
  }

  await next();
});

internalSubscriptionRoutes.put("/users/:firebaseUid/subscription", async (c) => {
  const firebaseUid = c.req.param("firebaseUid");
  const body = await c.req.json<UpsertSubscriptionBody | null>();

  if (!body || typeof body !== "object") {
    return c.json({ error: "invalid_request_body" }, 400);
  }

  if (!allowedSubscriptionStatuses.has(body.status ?? "")) {
    return c.json({ error: "invalid_subscription_status" }, 400);
  }

  const expiresAt = parseNullableDate(body.expiresAt);
  if (expiresAt === undefined) {
    return c.json({ error: "invalid_expires_at" }, 400);
  }

  const db = getDb(c.env.DATABASE_URL);
  const existingUser = await db
    .select({ firebaseUid: users.firebaseUid })
    .from(users)
    .where(eq(users.firebaseUid, firebaseUid))
    .limit(1);

  if (existingUser.length === 0) {
    return c.json({ error: "user_not_found" }, 404);
  }

  const now = new Date();

  await db
    .insert(userSubscriptions)
    .values({
      userUid: firebaseUid,
      status: body.status!,
      provider: body.provider ?? "manual",
      productId: body.productId ?? null,
      originalTransactionId: body.originalTransactionId ?? null,
      latestTransactionId: body.latestTransactionId ?? null,
      latestPurchaseToken: body.latestPurchaseToken ?? null,
      expiresAt,
      updatedAt: now,
    })
    .onConflictDoUpdate({
      target: userSubscriptions.userUid,
      set: {
        status: body.status!,
        provider: body.provider ?? "manual",
        productId: body.productId ?? null,
        originalTransactionId: body.originalTransactionId ?? null,
        latestTransactionId: body.latestTransactionId ?? null,
        latestPurchaseToken: body.latestPurchaseToken ?? null,
        expiresAt,
        updatedAt: now,
      },
    });

  return c.json(await getSubscriptionView(db, firebaseUid));
});

export async function getSubscriptionView(db: Db, userUid: string): Promise<SubscriptionView> {
  const rows = await db
    .select()
    .from(userSubscriptions)
    .where(eq(userSubscriptions.userUid, userUid))
    .limit(1);

  if (rows.length === 0) {
    return { premium: false, subscription: null };
  }

  return toSubscriptionView(rows[0]);
}

function toSubscriptionView(row: UserSubscriptionRow): SubscriptionView {
  return {
    premium: isPremium(row),
    subscription: {
      status: row.status,
      provider: row.provider,
      productId: row.productId,
      originalTransactionId: row.originalTransactionId,
      latestTransactionId: row.latestTransactionId,
      expiresAt: row.expiresAt?.toISOString() ?? null,
      updatedAt: row.updatedAt.toISOString(),
    },
  };
}

function isPremium(row: UserSubscriptionRow): boolean {
  if (!activeSubscriptionStatuses.has(row.status)) {
    return false;
  }

  if (row.status === "canceled") {
    return row.expiresAt !== null && row.expiresAt.getTime() > Date.now();
  }

  return row.expiresAt === null || row.expiresAt.getTime() > Date.now();
}

export type UpsertGooglePlaySubscriptionResult =
  | { ok: true }
  | { ok: false; reason: "product_not_purchased" | "invalid_expiry" };

/**
 * Persists the authoritative Google Play subscription state for a user, derived from a verified
 * `subscriptionsv2` purchase. Shared by the client-driven verify route and the RTDN webhook so
 * both write identical rows. The line item for `productId` must be present, and its expiry must
 * parse; otherwise the caller decides how to surface the failure.
 */
export async function upsertGooglePlaySubscription(
  db: Db,
  params: {
    userUid: string;
    productId: string;
    purchaseToken: string;
    purchase: GooglePlaySubscriptionPurchase;
    now?: Date;
  },
): Promise<UpsertGooglePlaySubscriptionResult> {
  const lineItem = (params.purchase.lineItems ?? []).find((item) => item.productId === params.productId);
  if (!lineItem) {
    return { ok: false, reason: "product_not_purchased" };
  }

  const expiresAt = parseNullableDate(lineItem.expiryTime);
  if (expiresAt === undefined) {
    return { ok: false, reason: "invalid_expiry" };
  }

  const now = params.now ?? new Date();
  const status = mapGooglePlayStatus(params.purchase.subscriptionState, expiresAt, now);
  const values = {
    userUid: params.userUid,
    status,
    provider: "google_play",
    productId: params.productId,
    originalTransactionId: params.purchase.linkedPurchaseToken ?? null,
    latestTransactionId: lineItem.latestSuccessfulOrderId ?? params.purchase.latestOrderId ?? null,
    latestPurchaseToken: params.purchaseToken,
    expiresAt,
    updatedAt: now,
  };

  await db
    .insert(userSubscriptions)
    .values(values)
    .onConflictDoUpdate({
      target: userSubscriptions.userUid,
      set: {
        status: values.status,
        provider: values.provider,
        productId: values.productId,
        originalTransactionId: values.originalTransactionId,
        latestTransactionId: values.latestTransactionId,
        latestPurchaseToken: values.latestPurchaseToken,
        expiresAt: values.expiresAt,
        updatedAt: values.updatedAt,
      },
    });

  return { ok: true };
}

/**
 * Finds the user that a Google Play purchase token belongs to, matching the token last persisted
 * by purchase verification. Returns null when no user has verified this token yet (e.g. an RTDN
 * arrives before the client has reported the purchase). An upgrade/downgrade mints a new token, so
 * a notification for the new token may not match until the client re-verifies.
 */
export async function findUserUidByPurchaseToken(db: Db, purchaseToken: string): Promise<string | null> {
  const rows = await db
    .select({ userUid: userSubscriptions.userUid })
    .from(userSubscriptions)
    .where(eq(userSubscriptions.latestPurchaseToken, purchaseToken))
    .limit(1);

  return rows[0]?.userUid ?? null;
}
