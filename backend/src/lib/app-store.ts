const APP_STORE_PRODUCTION_URL = "https://api.storekit.apple.com";
const APP_STORE_SANDBOX_URL = "https://api.storekit-sandbox.apple.com";

export interface AppStoreServerConfig {
  issuerId: string;
  keyId: string;
  privateKey: string;
  bundleId: string;
  premiumProductId: string;
}

export interface VerifiedAppStoreTransaction {
  transactionId: string;
  originalTransactionId: string;
  productId: string;
  purchaseDate: Date;
  expiresAt: Date;
  environment: "production" | "sandbox";
  active: boolean;
}

interface TransactionInfoResponse {
  signedTransactionInfo?: string;
}

interface AppStoreErrorResponse {
  errorCode?: number;
  errorMessage?: string;
}

interface TransactionPayload {
  transactionId?: unknown;
  originalTransactionId?: unknown;
  productId?: unknown;
  bundleId?: unknown;
  purchaseDate?: unknown;
  expiresDate?: unknown;
  revocationDate?: unknown;
  environment?: unknown;
  isUpgraded?: unknown;
}

export class AppStoreApiError extends Error {
  constructor(
    readonly status: number,
    readonly errorCode: number | undefined,
    message: string,
  ) {
    super(message);
  }
}

/**
 * Looks up a transaction directly on Apple's authenticated App Store Server API.
 * The returned JWS payload is decoded only after it arrives over Apple's HTTPS API;
 * callers must still validate every identity, product, environment and time field.
 */
export async function verifyAppStoreTransaction(
  config: AppStoreServerConfig,
  transactionId: string,
  now = new Date(),
): Promise<VerifiedAppStoreTransaction> {
  const jwt = await createAppStoreJwt(config, now);

  let response: TransactionInfoResponse;
  let environment: "production" | "sandbox" = "production";
  try {
    response = await getTransaction(APP_STORE_PRODUCTION_URL, transactionId, jwt);
  } catch (error) {
    if (!(error instanceof AppStoreApiError) || error.status !== 404) throw error;
    environment = "sandbox";
    response = await getTransaction(APP_STORE_SANDBOX_URL, transactionId, jwt);
  }

  const signedTransaction = response.signedTransactionInfo;
  if (!signedTransaction) throw new Error("Apple response did not include signedTransactionInfo.");
  const payload = decodeTransactionPayload(signedTransaction);

  const verifiedTransactionId = requiredString(payload.transactionId, "transactionId");
  const originalTransactionId = requiredString(payload.originalTransactionId, "originalTransactionId");
  const productId = requiredString(payload.productId, "productId");
  const bundleId = requiredString(payload.bundleId, "bundleId");
  const payloadEnvironment = requiredString(payload.environment, "environment").toLowerCase();
  const purchaseDate = requiredDate(payload.purchaseDate, "purchaseDate");
  const expiresAt = requiredDate(payload.expiresDate, "expiresDate");

  if (verifiedTransactionId !== transactionId) throw new Error("Apple returned a different transaction.");
  if (bundleId !== config.bundleId) throw new Error("App Store transaction belongs to another app.");
  if (productId !== config.premiumProductId) throw new Error("Unexpected App Store product.");
  if (payloadEnvironment !== environment) throw new Error("App Store transaction environment mismatch.");

  const revoked = typeof payload.revocationDate === "number";
  const upgraded = payload.isUpgraded === true;
  return {
    transactionId: verifiedTransactionId,
    originalTransactionId,
    productId,
    purchaseDate,
    expiresAt,
    environment,
    active: !revoked && !upgraded && expiresAt.getTime() > now.getTime(),
  };
}

async function getTransaction(
  baseUrl: string,
  transactionId: string,
  jwt: string,
): Promise<TransactionInfoResponse> {
  const response = await fetch(
    `${baseUrl}/inApps/v1/transactions/${encodeURIComponent(transactionId)}`,
    { headers: { Authorization: `Bearer ${jwt}`, Accept: "application/json" } },
  );
  const body: TransactionInfoResponse & AppStoreErrorResponse =
    await response.json<TransactionInfoResponse & AppStoreErrorResponse>().catch(() => ({}));
  if (!response.ok) {
    throw new AppStoreApiError(
      response.status,
      body.errorCode,
      body.errorMessage ?? `App Store Server API returned HTTP ${response.status}.`,
    );
  }
  return body;
}

async function createAppStoreJwt(config: AppStoreServerConfig, now: Date): Promise<string> {
  const issuedAt = Math.floor(now.getTime() / 1000);
  const header = base64UrlJson({ alg: "ES256", kid: config.keyId, typ: "JWT" });
  const payload = base64UrlJson({
    iss: config.issuerId,
    iat: issuedAt,
    exp: issuedAt + 300,
    aud: "appstoreconnect-v1",
    bid: config.bundleId,
  });
  const input = `${header}.${payload}`;
  const key = await crypto.subtle.importKey(
    "pkcs8",
    pemToArrayBuffer(config.privateKey),
    { name: "ECDSA", namedCurve: "P-256" },
    false,
    ["sign"],
  );
  const signature = await crypto.subtle.sign(
    { name: "ECDSA", hash: "SHA-256" },
    key,
    new TextEncoder().encode(input),
  );
  return `${input}.${base64Url(new Uint8Array(signature))}`;
}

function decodeTransactionPayload(jws: string): TransactionPayload {
  const parts = jws.split(".");
  if (parts.length !== 3) throw new Error("Apple returned malformed signed transaction data.");
  try {
    return JSON.parse(new TextDecoder().decode(base64UrlDecode(parts[1]))) as TransactionPayload;
  } catch {
    throw new Error("Apple returned an unreadable transaction payload.");
  }
}

function requiredString(value: unknown, field: string): string {
  if (typeof value !== "string" || value.trim().length === 0) {
    throw new Error(`Apple transaction is missing ${field}.`);
  }
  return value;
}

function requiredDate(value: unknown, field: string): Date {
  if (typeof value !== "number" || !Number.isFinite(value)) {
    throw new Error(`Apple transaction is missing ${field}.`);
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) throw new Error(`Apple transaction has invalid ${field}.`);
  return date;
}

function pemToArrayBuffer(pem: string): ArrayBuffer {
  const normalized = pem.replaceAll("\\n", "\n");
  const base64 = normalized
    .replace(/-----BEGIN PRIVATE KEY-----/g, "")
    .replace(/-----END PRIVATE KEY-----/g, "")
    .replace(/\s/g, "");
  if (!base64) throw new Error("App Store private key is empty.");
  return Uint8Array.from(atob(base64), (character) => character.charCodeAt(0)).buffer;
}

function base64UrlJson(value: unknown): string {
  return base64Url(new TextEncoder().encode(JSON.stringify(value)));
}

function base64Url(bytes: Uint8Array): string {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replaceAll("+", "-").replaceAll("/", "_").replace(/=+$/g, "");
}

function base64UrlDecode(value: string): Uint8Array {
  const base64 = value.replaceAll("-", "+").replaceAll("_", "/").padEnd(Math.ceil(value.length / 4) * 4, "=");
  return Uint8Array.from(atob(base64), (character) => character.charCodeAt(0));
}
