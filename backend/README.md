# DeviceDNA Sync — Cloudflare Worker

Sync backend: receives a device snapshot from the app, verifies the Firebase ID token,
and stores the user + device in Neon Postgres.

Stack: Hono + Drizzle ORM + `@neondatabase/serverless` + `firebase-auth-cloudflare-workers`.

## API

All `/v1/*` routes require an `Authorization: Bearer <firebase_id_token>` header.

| Method | Path | Description |
|--------|------|-------------|
| GET | `/` | health-check (no auth) |
| GET | `/v1/devices/:androidId/status` | `{ exists, snapshotHash, lastSyncedAt }` |
| POST | `/v1/sync` | upsert user + device; `{ synced, lastSyncedAt }` |

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

## Setup & deploy

Prerequisites: Node 18+, a Cloudflare account, a Neon account.

```bash
cd backend
npm install

# 1) Neon: create a project at https://neon.tech, copy the connection string.

# 2) Apply the schema to Neon:
export DATABASE_URL="postgresql://...neon.tech/devicedna?sslmode=require"
npm run db:generate      # generates SQL in ./drizzle (safe to commit)
npm run db:migrate       # applies it to the database

# 3) Cloudflare:
npx wrangler login
npx wrangler kv namespace create PUBLIC_JWK_CACHE_KV
#   -> put the returned id into wrangler.toml (kv_namespaces.id)
npx wrangler secret put DATABASE_URL     # paste the same Neon URL

# 4) Local check (optional):
cp .dev.vars.example .dev.vars           # fill in DATABASE_URL
npm run dev

# 5) Deploy:
npm run deploy
#   -> you get a URL like https://devicedna-sync.<subdomain>.workers.dev
```

After deploy, put the worker URL into `SYNC_BASE_URL` (android `local.properties` -> syncBaseUrl).

`FIREBASE_PROJECT_ID` in `wrangler.toml` must match the app's Firebase project
(`device-dna-test`).

## Quick check with a real token

The app logs the ID token to Logcat (tag `DeviceSync`) in debug builds.

```bash
TOKEN="<firebase_id_token>"
BASE="https://devicedna-sync.<subdomain>.workers.dev"
curl -s "$BASE/v1/devices/test123/status" -H "Authorization: Bearer $TOKEN"
curl -s -X POST "$BASE/v1/sync" -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"androidId":"test123","deviceName":"Test","snapshotHash":"h1","snapshot":{"ok":true}}'
```
