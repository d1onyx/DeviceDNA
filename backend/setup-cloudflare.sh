#!/usr/bin/env bash
# Deploy the Worker to Cloudflare: KV + required secrets + deploy.
# Prerequisite: `npx wrangler login` has already been run.
# Values come from the repo-root secrets.properties; an already-exported env var wins.
# Full migration checklist: ../MIGRATION.md
set -euo pipefail
cd "$(dirname "$0")"

WT="wrangler.toml"
REPO_ROOT="$(git rev-parse --show-toplevel)"
# shellcheck source=../scripts/lib/config.sh
source "$REPO_ROOT/scripts/lib/config.sh"

set_from_property() {
    local env_name="$1" prop_name="$2" fallback="${3:-}" value

    if [ -z "${!env_name:-}" ]; then
        value="$(cfg "$prop_name" "$fallback")"
        # A plain `[ -n "$value" ] && export` returns 1 when value is empty, which
        # under `set -e` aborts the whole script silently. Keep the function at exit 0.
        if [ -n "$value" ]; then
            export "$env_name=$value"
        fi
    fi
}

set_from_property FIREBASE_PROJECT_ID firebaseProjectId
set_from_property FIREBASE_WEB_API_KEY firebaseWebApiKey
set_from_property GOOGLE_PLAY_PACKAGE_NAME googlePlayPackageName androidApplicationId
set_from_property GOOGLE_PLAY_PREMIUM_PRODUCT_ID googlePlayPremiumProductId premiumSubProductId
set_from_property INTERNAL_API_KEY internalApiKey
set_from_property GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL googlePlayServiceAccountEmail
set_from_property GOOGLE_PLAY_PRIVATE_KEY googlePlayPrivateKey
set_from_property APPLE_APP_STORE_ISSUER_ID appStoreIssuerId
set_from_property APPLE_APP_STORE_KEY_ID appStoreKeyId
set_from_property APPLE_APP_STORE_PRIVATE_KEY appStorePrivateKey
set_from_property APPLE_APP_BUNDLE_ID iosBundleId
set_from_property APPLE_PREMIUM_PRODUCT_ID iosStoreKitPremiumProductId
set_from_property PLAY_RTDN_VERIFICATION_TOKEN playRtdnVerificationToken

# 0) Login check
if ! npx wrangler whoami >/dev/null 2>&1; then
    echo "Log in first:  npx wrangler login" >&2
    exit 1
fi

# 1) KV namespace for the Google JWKS cache. wrangler.toml is generated, so the id is
#    stored in secrets.properties and the file re-rendered — never patched in place.
[ -f "$WT" ] || "$REPO_ROOT/scripts/sync-config.sh" >/dev/null

if [ -z "$(cfg cloudflareKvId)" ]; then
    echo "==> Creating KV namespace PUBLIC_JWK_CACHE_KV"
    OUT="$(npx wrangler kv namespace create PUBLIC_JWK_CACHE_KV 2>&1)" || { echo "$OUT"; exit 1; }
    echo "$OUT"
    KV_ID="$(printf '%s\n' "$OUT" | grep -oE 'id = "[a-f0-9]+"' | head -1 | sed -E 's/id = "([a-f0-9]+)"/\1/')"
    if [ -z "${KV_ID:-}" ]; then
        echo "Could not parse the KV id. Set cloudflareKvId=... in secrets.properties." >&2
        exit 1
    fi
    set_property cloudflareKvId "$KV_ID"
    "$REPO_ROOT/scripts/sync-config.sh" >/dev/null
    echo "    KV id=$KV_ID written to secrets.properties; $WT regenerated"
else
    echo "==> KV id already set in secrets.properties — skipping"
fi

# 1b) D1 database. Same pattern as KV: create once, store the id in secrets.properties
#     and re-render wrangler.toml.
if [ -z "$(cfg cloudflareD1Id)" ]; then
    echo "==> Creating D1 database DeviceDNA_DB"
    OUT="$(npx wrangler d1 create DeviceDNA_DB 2>&1)" || { echo "$OUT"; exit 1; }
    echo "$OUT"
    # `|| true` so a no-match under `set -o pipefail` surfaces via the explicit
    # empty-id check below instead of aborting the script.
    D1_ID="$(printf '%s\n' "$OUT" | grep -oE '[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}' | head -1 || true)"
    if [ -z "${D1_ID:-}" ]; then
        echo "Could not parse the D1 id. Set cloudflareD1Id=... in secrets.properties." >&2
        exit 1
    fi
    set_property cloudflareD1Id "$D1_ID"
    "$REPO_ROOT/scripts/sync-config.sh" >/dev/null
    echo "    D1 id=$D1_ID written to secrets.properties; $WT regenerated"
else
    echo "==> D1 id already set in secrets.properties — skipping"
fi

put_secret() {
    local name="$1"
    local value="${!name:-}"

    echo "==> Setting the $name secret"
    if [ -n "$value" ]; then
        printf '%s' "$value" | npx wrangler secret put "$name"
    else
        echo "    (paste $name when wrangler prompts)"
        npx wrangler secret put "$name"
    fi
}

put_optional_secret_from_env() {
    local name="$1"
    local value="${!name:-}"

    if [ -n "$value" ]; then
        echo "==> Setting the optional $name secret"
        printf '%s' "$value" | npx wrangler secret put "$name"
    fi
}

# 2) Required production secrets.
put_secret FIREBASE_PROJECT_ID
put_secret FIREBASE_WEB_API_KEY
put_secret APPLE_APP_STORE_ISSUER_ID
put_secret APPLE_APP_STORE_KEY_ID
put_secret APPLE_APP_STORE_PRIVATE_KEY
put_secret APPLE_APP_BUNDLE_ID
put_secret APPLE_PREMIUM_PRODUCT_ID

# 3) Optional production secrets. Pass them as env vars or set them in ../secrets.properties.
put_optional_secret_from_env INTERNAL_API_KEY
put_optional_secret_from_env GOOGLE_PLAY_PACKAGE_NAME
put_optional_secret_from_env GOOGLE_PLAY_PREMIUM_PRODUCT_ID
put_optional_secret_from_env GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL
put_optional_secret_from_env GOOGLE_PLAY_PRIVATE_KEY
put_optional_secret_from_env PLAY_RTDN_VERIFICATION_TOKEN

# 4) Apply D1 migrations to the remote database before deploying.
echo "==> Applying D1 migrations (remote)"
npx wrangler d1 migrations apply DeviceDNA_DB --remote

# 5) Deploy
echo "==> Deploying the worker"
npx wrangler deploy

echo ""
echo "Done. Copy the worker URL above into secrets.properties -> syncBaseUrl=..."
echo "Then rerun ./scripts/sync-config.sh so iOS and .dev.vars pick it up."
