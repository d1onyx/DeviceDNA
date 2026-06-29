import { Hono } from "hono";
import { eq } from "drizzle-orm";
import { getDb } from "../db/client";
import { userSubscriptions, users, type UserSubscriptionRow } from "../db/schema";
import type { AppBindings } from "../types";

const DEV_SUBSCRIPTION_DURATION_MS = 10 * 60 * 1000;

const activeSubscriptionStatuses = new Set(["active", "trialing", "grace_period", "canceled"]);
const allowedSubscriptionStatuses = new Set([
  "inactive",
  "active",
  "trialing",
  "grace_period",
  "expired",
  "canceled",
]);

type Db = ReturnType<typeof getDb>;

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

// Dev-only: grant a short-lived Premium subscription persisted to Neon, mirroring the
// real verify flow (writes to user_subscriptions, returns the same SubscriptionView).
// Gated behind DEV_SUBSCRIPTIONS_ENABLED so it can never be reached in production.
subscriptionRoutes.post("/subscription/dev/activate", async (c) => {
  if (c.env.DEV_SUBSCRIPTIONS_ENABLED !== "true") {
    return c.json({ error: "dev_subscriptions_disabled" }, 403);
  }

  const claims = c.get("claims");
  const productId = c.env.GOOGLE_PLAY_PREMIUM_PRODUCT_ID ?? "devicedna_premium";
  const now = new Date();
  const expiresAt = new Date(now.getTime() + DEV_SUBSCRIPTION_DURATION_MS);
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
  const lineItem = (purchase.lineItems ?? []).find((item) => item.productId === productId);
  if (!lineItem) {
    return c.json({ error: "product_not_purchased" }, 400);
  }

  const expiresAt = parseNullableDate(lineItem.expiryTime);
  if (expiresAt === undefined) {
    return c.json({ error: "invalid_google_play_expiry" }, 502);
  }

  const now = new Date();
  const status = mapGooglePlayStatus(purchase.subscriptionState, expiresAt, now);
  const db = getDb(c.env.DATABASE_URL);

  await db
    .insert(userSubscriptions)
    .values({
      userUid: claims.uid,
      status,
      provider: "google_play",
      productId,
      originalTransactionId: purchase.linkedPurchaseToken ?? null,
      latestTransactionId: lineItem.latestSuccessfulOrderId ?? purchase.latestOrderId ?? null,
      latestPurchaseToken: purchaseToken,
      expiresAt,
      updatedAt: now,
    })
    .onConflictDoUpdate({
      target: userSubscriptions.userUid,
      set: {
        status,
        provider: "google_play",
        productId,
        originalTransactionId: purchase.linkedPurchaseToken ?? null,
        latestTransactionId: lineItem.latestSuccessfulOrderId ?? purchase.latestOrderId ?? null,
        latestPurchaseToken: purchaseToken,
        expiresAt,
        updatedAt: now,
      },
    });

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

function parseNullableDate(value: string | null | undefined): Date | null | undefined {
  if (value === undefined || value === null) {
    return null;
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return undefined;
  }

  return date;
}

interface VerifyGooglePlaySubscriptionParams {
  packageName: string;
  serviceAccountEmail: string;
  privateKey: string;
  purchaseToken: string;
}

interface GooglePlaySubscriptionPurchase {
  subscriptionState?: string;
  latestOrderId?: string;
  linkedPurchaseToken?: string;
  lineItems?: GooglePlaySubscriptionLineItem[];
}

interface GooglePlaySubscriptionLineItem {
  productId?: string;
  expiryTime?: string;
  latestSuccessfulOrderId?: string;
}

async function verifyGooglePlaySubscription(
  params: VerifyGooglePlaySubscriptionParams,
): Promise<GooglePlaySubscriptionPurchase> {
  const accessToken = await getGoogleAccessToken(params.serviceAccountEmail, params.privateKey);
  const packageName = encodeURIComponent(params.packageName);
  const purchaseToken = encodeURIComponent(params.purchaseToken);
  const url = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/purchases/subscriptionsv2/tokens/${purchaseToken}`;

  const res = await fetch(url, {
    headers: {
      "Authorization": `Bearer ${accessToken}`,
      "Accept": "application/json",
    },
  });

  if (!res.ok) {
    const detail = await res.text();
    throw new Error(`Google Play subscription verification failed: HTTP ${res.status} ${detail}`);
  }

  return await res.json() as GooglePlaySubscriptionPurchase;
}

async function getGoogleAccessToken(serviceAccountEmail: string, privateKey: string): Promise<string> {
  const assertion = await createGoogleServiceAccountJwt(serviceAccountEmail, privateKey);
  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });

  if (!res.ok) {
    const detail = await res.text();
    throw new Error(`Google OAuth token request failed: HTTP ${res.status} ${detail}`);
  }

  const data = await res.json() as { access_token?: string };
  if (!data.access_token) {
    throw new Error("Google OAuth token response did not include access_token.");
  }

  return data.access_token;
}

async function createGoogleServiceAccountJwt(serviceAccountEmail: string, privateKey: string): Promise<string> {
  const nowSeconds = Math.floor(Date.now() / 1000);
  const header = { alg: "RS256", typ: "JWT" };
  const payload = {
    iss: serviceAccountEmail,
    scope: "https://www.googleapis.com/auth/androidpublisher",
    aud: "https://oauth2.googleapis.com/token",
    iat: nowSeconds,
    exp: nowSeconds + 3600,
  };
  const signingInput = `${base64UrlEncode(JSON.stringify(header))}.${base64UrlEncode(JSON.stringify(payload))}`;
  const key = await crypto.subtle.importKey(
    "pkcs8",
    pemToArrayBuffer(privateKey),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    key,
    new TextEncoder().encode(signingInput),
  );

  return `${signingInput}.${base64UrlEncode(signature)}`;
}

function mapGooglePlayStatus(subscriptionState: string | undefined, expiresAt: Date | null, now: Date): string {
  if (expiresAt !== null && expiresAt.getTime() <= now.getTime()) {
    return "expired";
  }

  switch (subscriptionState) {
    case "SUBSCRIPTION_STATE_ACTIVE":
      return "active";
    case "SUBSCRIPTION_STATE_IN_GRACE_PERIOD":
      return "grace_period";
    case "SUBSCRIPTION_STATE_CANCELED":
      return "canceled";
    case "SUBSCRIPTION_STATE_EXPIRED":
      return "expired";
    default:
      return "inactive";
  }
}

function pemToArrayBuffer(pem: string): ArrayBuffer {
  const normalized = pem.replace(/\\n/g, "\n");
  const base64 = normalized
    .replace("-----BEGIN PRIVATE KEY-----", "")
    .replace("-----END PRIVATE KEY-----", "")
    .replace(/\s/g, "");
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i += 1) {
    bytes[i] = binary.charCodeAt(i);
  }

  return bytes.buffer;
}

function base64UrlEncode(input: string | ArrayBuffer): string {
  const bytes = typeof input === "string" ? new TextEncoder().encode(input) : new Uint8Array(input);
  let binary = "";
  for (const byte of bytes) {
    binary += String.fromCharCode(byte);
  }

  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}
