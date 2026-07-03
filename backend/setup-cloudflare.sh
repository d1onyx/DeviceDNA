#!/usr/bin/env bash
# Deploy the Worker to Cloudflare: KV + required secrets + deploy.
# Prerequisite: `npx wrangler login` has already been run.
# Full migration checklist: ../MIGRATION.md
set -euo pipefail
cd "$(dirname "$0")"

WT="wrangler.toml"
ROOT_PROPERTIES="../local.properties"

read_property() {
    local name="$1"
    [ -f "$ROOT_PROPERTIES" ] || return 0
    awk -F= -v key="$name" '
        $0 !~ /^[[:space:]]*#/ && $1 == key {
            sub(/^[^=]*=/, "")
            gsub(/^[[:space:]]+|[[:space:]]+$/, "")
            print
            exit
        }
    ' "$ROOT_PROPERTIES"
}

set_from_property() {
    local env_name="$1"
    local prop_name="$2"
    local fallback="${3:-}"

    if [ -z "${!env_name:-}" ]; then
        local value
        value="$(read_property "$prop_name")"
        if [ -z "$value" ] && [ -n "$fallback" ]; then
            value="$(read_property "$fallback")"
        fi
        if [ -n "$value" ]; then
            export "$env_name=$value"
        fi
    fi
}

set_from_property FIREBASE_PROJECT_ID firebaseProjectId
set_from_property FIREBASE_WEB_API_KEY firebaseWebApiKey
set_from_property DATABASE_URL databaseUrl
set_from_property GOOGLE_PLAY_PACKAGE_NAME googlePlayPackageName androidApplicationId
set_from_property GOOGLE_PLAY_PREMIUM_PRODUCT_ID googlePlayPremiumProductId premiumSubProductId

# 0) Login check
if ! npx wrangler whoami >/dev/null 2>&1; then
    echo "Log in first:  npx wrangler login" >&2
    exit 1
fi

# 1) KV namespace for the Google JWKS cache (create only if id not yet filled in)
if grep -q 'REPLACE_AFTER_wrangler_kv_namespace_create' "$WT"; then
    echo "==> Creating KV namespace PUBLIC_JWK_CACHE_KV"
    OUT="$(npx wrangler kv namespace create PUBLIC_JWK_CACHE_KV 2>&1)" || { echo "$OUT"; exit 1; }
    echo "$OUT"
    KV_ID="$(printf '%s\n' "$OUT" | grep -oE 'id = "[a-f0-9]+"' | head -1 | sed -E 's/id = "([a-f0-9]+)"/\1/')"
    if [ -z "${KV_ID:-}" ]; then
        echo "Could not parse the KV id. Set it manually in $WT (kv_namespaces.id)." >&2
        exit 1
    fi
    TMP_WT="$(mktemp)"
    sed "s/REPLACE_AFTER_wrangler_kv_namespace_create/$KV_ID/" "$WT" > "$TMP_WT"
    mv "$TMP_WT" "$WT"
    echo "    KV id=$KV_ID written to $WT"
else
    echo "==> KV id already configured in $WT — skipping"
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
put_secret DATABASE_URL
put_secret FIREBASE_WEB_API_KEY

# 3) Optional production secrets. Pass them as env vars or set them in ../local.properties.
put_optional_secret_from_env INTERNAL_API_KEY
put_optional_secret_from_env GOOGLE_PLAY_PACKAGE_NAME
put_optional_secret_from_env GOOGLE_PLAY_PREMIUM_PRODUCT_ID
put_optional_secret_from_env GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL
put_optional_secret_from_env GOOGLE_PLAY_PRIVATE_KEY
put_optional_secret_from_env PLAY_RTDN_VERIFICATION_TOKEN

# 4) Deploy
echo "==> Deploying the worker"
npx wrangler deploy

echo ""
echo "Done. Copy the worker URL above into android: local.properties -> syncBaseUrl=..."
