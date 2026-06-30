// Shared Google Play Developer API helpers used by both the client-driven purchase
// verification route and the server-to-server Real-time Developer Notifications (RTDN)
// webhook. Keeping the OAuth/JWT/verification logic here lets both flows resolve the
// authoritative subscription state from the same source of truth.

export interface GooglePlaySubscriptionPurchase {
  subscriptionState?: string;
  latestOrderId?: string;
  linkedPurchaseToken?: string;
  lineItems?: GooglePlaySubscriptionLineItem[];
}

export interface GooglePlaySubscriptionLineItem {
  productId?: string;
  expiryTime?: string;
  latestSuccessfulOrderId?: string;
}

export interface GooglePlayCredentials {
  packageName: string;
  serviceAccountEmail: string;
  privateKey: string;
}

export interface VerifyGooglePlaySubscriptionParams extends GooglePlayCredentials {
  purchaseToken: string;
}

export async function verifyGooglePlaySubscription(
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

/**
 * Maps Google's subscriptionState (plus expiry) to our internal status string. An expiry in
 * the past always wins as "expired" regardless of the reported state, so a revoked or lapsed
 * subscription cannot read as premium.
 */
export function mapGooglePlayStatus(
  subscriptionState: string | undefined,
  expiresAt: Date | null,
  now: Date,
): string {
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

/**
 * Parses an RFC3339 date string. Returns null for null/undefined input and undefined for an
 * unparseable value, so callers can distinguish "no date" from "bad date".
 */
export function parseNullableDate(value: string | null | undefined): Date | null | undefined {
  if (value === undefined || value === null) {
    return null;
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return undefined;
  }

  return date;
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
