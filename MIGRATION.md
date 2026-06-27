# Migrating DeviceDNA to your own accounts

To run the project under your own services you replace **4 binding points** (all outside
the code — secrets/config). Full checklist below. Estimated time: ~20–30 min.

## What is tied to accounts

| # | Point | File / location | Service |
|---|-------|-----------------|---------|
| 1 | Android Firebase config | `android/google-services.json` | Firebase (Auth) |
| 2 | Backend Firebase project id | `backend/wrangler.toml` -> `FIREBASE_PROJECT_ID` `[MIGRATE]` | Firebase <-> Worker |
| 3 | Database connection | `DATABASE_URL` secret (`wrangler secret put`) | Neon Postgres |
| 4 | Backend URL in the app | `local.properties` -> `syncBaseUrl` | Cloudflare Worker |

> **Key rule:** `FIREBASE_PROJECT_ID` in `wrangler.toml` (#2) **MUST match** the `project_id`
> inside `android/google-services.json` (#1). Otherwise the Worker rejects the app's
> ID tokens (`invalid_token`).

Prerequisites: Node 18+, Firebase / Neon / Cloudflare accounts, Android SDK installed.

---

## A. Firebase (your project + Google Sign-In)

1. Create a project: [console.firebase.google.com](https://console.firebase.google.com) or
   `firebase projects:create <PROJECT_ID> --display-name "DeviceDNA"`.
2. **Authentication -> Sign-in method -> Google -> Enable** + set a support email.
3. Get the debug SHA-1:
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey \
     -storepass android -keypass android | grep SHA1
   ```
   (for release — the SHA-1 of your release keystore).
4. Drop the config into the app with one script:
   - Linux/macOS: `./scripts/setup-firebase-auth.sh <PROJECT_ID> <SHA1>`
   - Windows: `./scripts/setup-firebase-auth.ps1 -ProjectId <PROJECT_ID> -AndroidSha1 <SHA1>`

   The script registers the Android app (package `com.devstdvad.devicedna`), adds the SHA-1,
   and downloads a new `android/google-services.json`. If it warns about a missing web OAuth
   client — enable the Google provider (step 2) and rerun the script.
5. Remember `<PROJECT_ID>` — needed in step C.

> To use a different package — change `applicationId` and `namespace` in
> `android/build.gradle.kts` (+ the `setup-firebase-auth` package param), and register the
> app under the new package. For a simple migration, keep the package as is.

## B. Neon (your Postgres)

1. Create a project at [neon.tech](https://neon.tech), copy the **pooled** connection string
   (`...sslmode=require`).
2. Apply the schema:
   ```bash
   cd backend
   npm install
   DATABASE_URL="postgresql://...neon.tech/...?sslmode=require" npm run db:migrate
   ```
   This creates the `users` and `devices` tables. (SQL is already generated in
   `backend/drizzle/` — `npm run db:generate` is only needed if you change the schema.)

## C. Cloudflare (your Worker)

1. In `backend/wrangler.toml` set your `FIREBASE_PROJECT_ID` (= `<PROJECT_ID>` from step A).
   Optionally change the worker `name`.
2. Deploy:
   ```bash
   cd backend
   npx wrangler login
   DATABASE_URL="postgresql://...neon.tech/...?sslmode=require" ./setup-cloudflare.sh
   ```
   The script creates the KV namespace, writes its id into `wrangler.toml`, sets the
   `DATABASE_URL` secret, and deploys. At the end it prints a URL like
   `https://<name>.<subdomain>.workers.dev`.

   *(Same thing manually: `wrangler kv namespace create PUBLIC_JWK_CACHE_KV` -> set the id ->
   `wrangler secret put DATABASE_URL` -> `wrangler deploy`.)*

## D. Android (point to your backend)

1. In `local.properties` (not committed) add:
   ```
   syncBaseUrl=https://<name>.<subdomain>.workers.dev
   ```
2. Build:
   ```bash
   ./gradlew :android:assembleDebug
   ```

## E. Verification (end-to-end)

1. Install the app (`./gradlew :android:installDebug`), open it, sign in with Google.
2. Logcat under the `DeviceSync` tag should show `Synced device ...`.
3. A row appears in Neon `devices` and `users`:
   ```bash
   DATABASE_URL="..." node -e "const{neon}=require('@neondatabase/serverless');(async()=>{const sql=neon(process.env.DATABASE_URL);console.log(await sql\`select user_uid, android_id, last_synced_at from devices\`)})()"
   ```
4. Second cold start -> no push (hash matches). Another device on the same account -> a second
   `devices` row, the same `users` row.

Manual backend check with `curl` (the app prints the ID token to Logcat under `DeviceSync`) —
see `backend/README.md`.

---

## Repo layout

The backend lives in `backend/` as a self-contained Cloudflare Worker (its own `package.json`,
`wrangler.toml`, README). It is deployed independently of the Android app. It can later be
split into its own repository while keeping history with
`git subtree split --prefix=backend`.

## Security when handing over the project

- **Do not hand over** your secrets: your `android/google-services.json`, any `*.bak` files,
  `backend/.dev.vars`, the Neon connection string. The new owner generates their own in
  steps A–C.
- Files containing secrets are already in `.gitignore` (`.dev.vars`) — `google-services.json`
  is currently in git; before handover either replace it with the new owner's config or remove
  it from history.
- After handover, **rotate** the Neon password and reissue the Firebase config if they were
  ever exposed.
