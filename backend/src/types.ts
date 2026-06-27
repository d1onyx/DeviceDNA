import type { FirebaseIdToken } from "firebase-auth-cloudflare-workers";

export interface Env {
  FIREBASE_PROJECT_ID: string;
  PUBLIC_JWK_CACHE_KEY: string;
  PUBLIC_JWK_CACHE_KV: KVNamespace;
  DATABASE_URL: string;
}

export interface Variables {
  claims: FirebaseIdToken;
}

export type AppBindings = { Bindings: Env; Variables: Variables };
