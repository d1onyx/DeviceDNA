import type { FirebaseIdToken } from "firebase-auth-cloudflare-workers";

export interface Env {
  FIREBASE_PROJECT_ID: string;
  PUBLIC_JWK_CACHE_KEY: string;
  PUBLIC_JWK_CACHE_KV: KVNamespace;
  DATABASE_URL: string;
  FIREBASE_WEB_API_KEY: string;
  INTERNAL_API_KEY?: string;
  GOOGLE_PLAY_PACKAGE_NAME?: string;
  GOOGLE_PLAY_PREMIUM_PRODUCT_ID?: string;
  GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL?: string;
  GOOGLE_PLAY_PRIVATE_KEY?: string;
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
