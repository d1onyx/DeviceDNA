# DeviceDNA Sync — Cloudflare Worker

Sync backend: receives a device snapshot from the app, verifies the Firebase ID token,
and stores the user + device in Neon Postgres.

Stack: Hono + Drizzle ORM + `@neondatabase/serverless` + `firebase-auth-cloudflare-workers`.

## API

All `/v1/*` routes require an `Authorization: Bearer <firebase_id_token>` header.

| Method | Path | Description |
|--------|------|-------------|
| GET | `/` | health-check (no auth) |
| GET | `/v1/me` | confirms the Firebase account still exists; returns `{ exists, uid, email, displayName, photoUrl, premium, subscription }` |
| GET | `/v1/subscription` | returns `{ premium, subscription }` for the current Firebase user |
| GET | `/v1/devices/:androidId/status` | `{ exists, snapshotHash, lastSyncedAt }` |
| POST | `/v1/sync` | upsert user + device; `{ synced, lastSyncedAt }` |

All `/v1/*` routes verify the Firebase ID token signature and the live Firebase
Auth account via Identity Toolkit. The Worker also keeps an account registry in
Neon `users`: first login creates the row; later, if Firebase recreates a Google
account with a new UID but the same email, the Worker replaces the old Neon user
row, which cascades old devices through the foreign key, and treats it as a fresh
account. Disabled Firebase accounts return `403 { "error": "account_disabled" }`.

`POST /v1/sync` body:
```json
{
  "androidId": "abc123",
  "deviceName": "Google Pixel 8",
  "manufacturer": "Google",
  "model": "Pixel 8",
  "osVersion": "15",
  "appVersion": "1.4 (4)",
  "snapshotHash": "<sha256>",
  "snapshot": { "device": { }, "cpu": { }, "...": {} }
}
```
User data (uid/email/name/photo) is taken from the verified token claims, **not** from the body.

## Premium subscription storage

Premium state is stored in Neon in `user_subscriptions`, keyed by the Firebase UID.
The app can read it from authenticated `/v1/me` or `/v1/subscription`.

A user is treated as premium when `status` is one of `active`, `trialing`, or
`grace_period`, and `expires_at` is either `NULL` or in the future. Canceled
Google Play subscriptions keep access only when `expires_at` is still in the
future.

Real Google Play purchases are verified by the backend:

```http
POST /v1/subscription/google-play/verify
Authorization: Bearer <firebase_id_token>
Content-Type: application/json

{
  "productId": "devicedna_premium",
  "purchaseToken": "<google_play_purchase_token>"
}
```

Configure the Worker with:

```bash
wrangler secret put GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL
wrangler secret put GOOGLE_PLAY_PRIVATE_KEY
```

And set `GOOGLE_PLAY_PACKAGE_NAME` / `GOOGLE_PLAY_PREMIUM_PRODUCT_ID` in
`wrangler.toml` or secrets.

Subscription writes use an internal endpoint protected by `INTERNAL_API_KEY`.
Set it with `wrangler secret put INTERNAL_API_KEY`.

```bash
curl -s -X PUT "$BASE/internal/users/<firebase_uid>/subscription" \
  -H "x-internal-api-key: $INTERNAL_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "active",
    "provider": "manual",
    "productId": "devicedna_premium",
    "expiresAt": null
  }'
```

## Setup & deploy

Prerequisites: Node 18+, a Cloudflare account, a Neon account.

```bash
cd backend
npm install

# 1) Neon: create a project at https://neon.tech, copy the connection string.

# 2) Apply the committed schema migrations to Neon:
export DATABASE_URL="postgresql://...neon.tech/devicedna?sslmode=require"
npm run db:migrate       # applies it to the database

# 3) Cloudflare: set FIREBASE_PROJECT_ID in wrangler.toml first.
npx wrangler login
DATABASE_URL="postgresql://...neon.tech/devicedna?sslmode=require" \
FIREBASE_WEB_API_KEY="AIzaSy..." \
bash setup-cloudflare.sh
# Windows (PowerShell): set $env:DATABASE_URL / $env:FIREBASE_WEB_API_KEY, then ./setup-cloudflare.ps1

# 4) Local check (optional):
cp .dev.vars.example .dev.vars           # fill in DATABASE_URL and FIREBASE_WEB_API_KEY
npm run dev

# setup-cloudflare.sh / setup-cloudflare.ps1 deploy the Worker and print a URL like:
# https://devicedna-sync.<subdomain>.workers.dev
```

After deploy, put the worker URL into `SYNC_BASE_URL` (android `local.properties` -> syncBaseUrl).

`FIREBASE_PROJECT_ID` in `wrangler.toml` must match the app's Firebase project
(`android/google-services.json` -> `project_info.project_id`).

For full handover steps, required secrets, Google Play subscriptions, and common
failure modes, see `../MIGRATION.md`.

## Quick check with a real token

The app logs the ID token to Logcat (tag `DeviceSync`) in debug builds.

```bash
TOKEN="<firebase_id_token>"
BASE="https://devicedna-sync.<subdomain>.workers.dev"
curl -s "$BASE/v1/me" -H "Authorization: Bearer $TOKEN"
curl -s "$BASE/v1/devices/test123/status" -H "Authorization: Bearer $TOKEN"
curl -s -X POST "$BASE/v1/sync" -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"androidId":"test123","deviceName":"Test","snapshotHash":"h1","snapshot":{"ok":true}}'
```
