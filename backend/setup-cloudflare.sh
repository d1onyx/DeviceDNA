#!/usr/bin/env bash
# Deploy the Worker to Cloudflare: KV + DATABASE_URL secret + deploy.
# Prerequisite: `npx wrangler login` has already been run.
# Full migration checklist: ../MIGRATION.md
set -euo pipefail
cd "$(dirname "$0")"

WT="wrangler.toml"

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
    sed -i "s/REPLACE_AFTER_wrangler_kv_namespace_create/$KV_ID/" "$WT"
    echo "    KV id=$KV_ID written to $WT"
else
    echo "==> KV id already configured in $WT — skipping"
fi

# 2) DATABASE_URL secret (Neon). Pass via env DATABASE_URL, otherwise wrangler will prompt.
echo "==> Setting the DATABASE_URL secret"
if [ -n "${DATABASE_URL:-}" ]; then
    printf '%s' "$DATABASE_URL" | npx wrangler secret put DATABASE_URL
else
    echo "    (paste the Neon connection string when wrangler prompts)"
    npx wrangler secret put DATABASE_URL
fi

# 3) Deploy
echo "==> Deploying the worker"
npx wrangler deploy

echo ""
echo "Done. Copy the worker URL above into android: local.properties -> syncBaseUrl=..."
