# Secrets & configuration

Every account-specific value lives in **one gitignored file at the repo root: `secrets.properties`**.
Nothing else needs to be edited by hand.

```bash
cp secrets.properties.example secrets.properties   # fill in the blanks
./scripts/sync-config.sh                           # fan the values out
```

`secrets.properties.example` documents every key inline: what it is, where to get it,
and whether it is required. This document explains the plumbing around it.

## How values reach each toolchain

| Consumer | Mechanism | Regenerate with |
|---|---|---|
| Android (Gradle) | reads `secrets.properties` directly | — |
| iOS (XcodeGen) | `ios/settings.local.yml`, included by `ios/project.yml` | `./scripts/sync-config.sh` |
| Firebase CLI | `.firebaserc` | `./scripts/sync-config.sh` |
| Worker config | `backend/wrangler.toml`, from `wrangler.toml.template` | `./scripts/sync-config.sh` |
| Worker (local `wrangler dev`) | `backend/.dev.vars` | `./scripts/sync-config.sh` |
| Worker (production) | Cloudflare Worker secrets | `cd backend && ./setup-cloudflare.sh` |

Every generated file is gitignored. Never edit them — the next `sync-config.sh` run
overwrites them.

The Worker name and KV namespace id are not secret, but they *are* tied to one
Cloudflare account, so they live in `secrets.properties` too (`cloudflareWorkerName`,
`cloudflareKvId`). `wrangler` has no variable interpolation, hence the template.
`setup-cloudflare.sh` creates the KV namespace on first run and writes the id back.

The only values outside `secrets.properties` are the Firebase config files —
`android/google-services.json` and `ios/DeviceDNAApp/GoogleService-Info.plist` —
downloaded by `scripts/setup-firebase-auth.sh` and gitignored.

## First-time setup, in order

1. **Firebase** — `./scripts/setup-firebase-auth.sh <PROJECT_ID> [SHA1]`
   Creates the project, registers the Android/iOS apps, downloads both config files
   and writes `.firebaserc`.
   Then copy `REVERSED_CLIENT_ID` out of `ios/DeviceDNAApp/GoogleService-Info.plist`
   into `iosGoogleReversedClientId`, and the Web API key from
   *Firebase Console → Project settings → General → Web API Key* into `firebaseWebApiKey`.

2. **Neon** — create the database, copy the pooled connection string into `databaseUrl`.

3. **Cloudflare** — `cd backend && ./setup-cloudflare.sh`
   Creates the KV namespace (writing `cloudflareKvId` back into `secrets.properties`),
   uploads every secret, deploys. Copy the printed worker URL into `syncBaseUrl`, then
   rerun `./scripts/sync-config.sh`.

4. **Play Console / App Store Connect** — create the subscription products, then fill
   `premiumSubProductId` and `iosStoreKitPremiumProductId`.

5. **AdMob** — create the app + ad units, fill the six `adMob*` / `iosAdMob*` keys.
   Left empty, debug builds fall back to Google's official test ids.

6. **Release signing** — generate the upload keystore, fill the four `release*` keys.
   Required only for `assembleRelease` / `bundleRelease`.

## Which values are actually secret

Treat as credentials — leaking them is a real incident:

`firebaseWebApiKey`, `databaseUrl`, `internalApiKey`, `playRtdnVerificationToken`,
`googlePlayPrivateKey`, `cfgApiKey`, and the four `release*` signing values.

The rest (bundle ids, ad unit ids, `syncBaseUrl`, `cfgPubKey`) are public by nature —
they ship inside the app binary. `cfgPubKey` is a *public* key: it only verifies a
signature, so it cannot be used to forge one.

## CI

CI has no `secrets.properties`. Gradle falls back to `-P` project properties and, for
signing, to environment variables:

```
ANDROID_RELEASE_STORE_FILE  ANDROID_RELEASE_STORE_PASSWORD
ANDROID_RELEASE_KEY_ALIAS   ANDROID_RELEASE_KEY_PASSWORD
```

`setup-cloudflare.sh` likewise prefers an already-exported env var over the file, so a
deploy job can pass `DATABASE_URL`, `FIREBASE_WEB_API_KEY`, … straight from CI secrets.

## Legacy fallback

Values still sitting in `local.properties` or `keystore.properties` keep working —
Gradle and the setup scripts check `secrets.properties` first, then those two. This
exists so a half-migrated checkout still builds. New values belong in
`secrets.properties`; `local.properties` should hold only `sdk.dir`, since Android
Studio rewrites that file on its own.
