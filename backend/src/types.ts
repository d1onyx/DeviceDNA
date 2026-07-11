import type { FirebaseIdToken } from "firebase-auth-cloudflare-workers";

export interface Env {
  FIREBASE_PROJECT_ID: string;
  PUBLIC_JWK_CACHE_KEY: string;
  PUBLIC_JWK_CACHE_KV: KVNamespace;
  DB: D1Database;
  FIREBASE_WEB_API_KEY: string;
  INTERNAL_API_KEY?: string;
  GOOGLE_PLAY_PACKAGE_NAME?: string;
  GOOGLE_PLAY_PREMIUM_PRODUCT_ID?: string;
  GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL?: string;
  GOOGLE_PLAY_PRIVATE_KEY?: string;
  // Shared secret guarding the Google Play RTDN webhook (POST /play/notifications?token=...).
  // Pub/Sub push cannot send headers, so the token travels in the query string.
  PLAY_RTDN_VERIFICATION_TOKEN?: string;
  // When "true", enables POST /v1/subscription/dev/activate which grants a non-expiring
  // Premium subscription for local/dev testing. Must stay unset/false in prod.
  DEV_SUBSCRIPTIONS_ENABLED?: string;
}

export interface Variables {
  claims: FirebaseIdToken;
  idToken: string;
  firebaseUser: FirebaseAccount;
}

export type AppBindings = { Bindings: Env; Variables: Variables };

export interface FirebaseAccount {
  localId: string;
  email?: string;
  displayName?: string;
  photoUrl?: string;
  disabled?: boolean;
}
