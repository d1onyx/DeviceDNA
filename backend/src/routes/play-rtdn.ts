import { Hono } from "hono";
import { getDb } from "../db/client";
import { verifyGooglePlaySubscription } from "../lib/google-play";
import {
  findUserUidByPurchaseToken,
  upsertGooglePlaySubscription,
} from "./subscriptions";
import type { AppBindings } from "../types";

// Google Play Real-time Developer Notifications (RTDN) webhook. Google publishes a Pub/Sub
// message whenever a subscription changes state (renew, cancel, revoke, expire, grace period,
// …) and Pub/Sub pushes it here. We do NOT trust the notification's type blindly: every
// actionable notification triggers a fresh verification against the Play Developer API, and we
// persist whatever authoritative state Google returns. This is what makes a Google-Play-side
// cancellation/refund propagate to Neon (and therefore revoke premium) without the user having
// to reopen the app.
//
// Auth: Pub/Sub push cannot send custom headers, so the endpoint is protected by a shared
// secret in the query string (?token=...), matched against PLAY_RTDN_VERIFICATION_TOKEN.

export const playRtdnRoutes = new Hono<AppBindings>();

interface PubSubEnvelope {
  message?: {
    data?: string;
    messageId?: string;
    publishTime?: string;
  };
  subscription?: string;
}

interface DeveloperNotification {
  version?: string;
  packageName?: string;
  eventTimeMillis?: string;
  subscriptionNotification?: {
    version?: string;
    notificationType?: number;
    purchaseToken?: string;
    subscriptionId?: string;
  };
  voidedPurchaseNotification?: {
    purchaseToken?: string;
    orderId?: string;
    productType?: number; // 1 = subscription, 2 = one-time product
  };
  testNotification?: {
    version?: string;
  };
}

playRtdnRoutes.post("/notifications", async (c) => {
  const expectedToken = c.env.PLAY_RTDN_VERIFICATION_TOKEN;
  if (!expectedToken) {
    return c.json({ error: "play_rtdn_not_configured" }, 503);
  }
  if (c.req.query("token") !== expectedToken) {
    return c.json({ error: "unauthorized" }, 401);
  }

  const envelope = await c.req.json<PubSubEnvelope | null>().catch(() => null);
  const data = envelope?.message?.data;
  if (!data) {
    // Not a well-formed Pub/Sub push. Ack so Pub/Sub stops retrying a message we can't parse.
    return c.body(null, 204);
  }

  let notification: DeveloperNotification;
  try {
    notification = JSON.parse(decodeBase64(data)) as DeveloperNotification;
  } catch {
    return c.body(null, 204);
  }

  // Google sends a one-off test notification when the Pub/Sub topic is first linked.
  if (notification.testNotification) {
    return c.body(null, 204);
  }

  const purchaseToken =
    notification.subscriptionNotification?.purchaseToken ??
    (notification.voidedPurchaseNotification?.productType === 1
      ? notification.voidedPurchaseNotification?.purchaseToken
      : undefined);

  // Only subscription-related notifications are actionable here. Ack everything else.
  if (!purchaseToken) {
    return c.body(null, 204);
  }

  const packageName = c.env.GOOGLE_PLAY_PACKAGE_NAME;
  const serviceAccountEmail = c.env.GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL;
  const privateKey = c.env.GOOGLE_PLAY_PRIVATE_KEY;
  if (!packageName || !serviceAccountEmail || !privateKey) {
    // Credentials missing: 503 lets Pub/Sub retry once the worker is configured.
    return c.json({ error: "google_play_not_configured" }, 503);
  }

  const db = getDb(c.env.DATABASE_URL);
  const userUid = await findUserUidByPurchaseToken(db, purchaseToken);
  if (!userUid) {
    // The client hasn't verified this token yet (or it's an upgraded token). Nothing to update.
    return c.body(null, 204);
  }

  const productId =
    notification.subscriptionNotification?.subscriptionId ??
    c.env.GOOGLE_PLAY_PREMIUM_PRODUCT_ID?.trim();
  if (!productId) {
    return c.json({ error: "google_play_product_not_configured" }, 503);
  }

  let purchase;
  try {
    purchase = await verifyGooglePlaySubscription({
      packageName,
      serviceAccountEmail,
      privateKey,
      purchaseToken,
    });
  } catch {
    // Transient Google API failure: signal retry so we don't drop the state change.
    return c.json({ error: "subscription_verification_failed" }, 502);
  }

  // Persist whatever Google reports. A canceled-but-still-paid subscription stays premium until
  // its expiry; a revoked/expired one flips to a non-premium status immediately.
  await upsertGooglePlaySubscription(db, { userUid, productId, purchaseToken, purchase });

  return c.body(null, 204);
});

function decodeBase64(value: string): string {
  // Pub/Sub uses standard base64; tolerate URL-safe variants just in case.
  const normalized = value.replace(/-/g, "+").replace(/_/g, "/");
  const binary = atob(normalized);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i += 1) {
    bytes[i] = binary.charCodeAt(i);
  }

  return new TextDecoder().decode(bytes);
}
