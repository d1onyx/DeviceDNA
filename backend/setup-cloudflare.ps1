#Requires -Version 5.1
# Deploy the Worker to Cloudflare: KV + required secrets + deploy.
# Windows/PowerShell port of setup-cloudflare.sh.
# Prerequisite: `npx wrangler login` has already been run.
# Full migration checklist: ../MIGRATION.md
#
# Pass secrets as environment variables to avoid interactive prompts, e.g.:
#   $env:DATABASE_URL="postgresql://..."; $env:FIREBASE_WEB_API_KEY="AIzaSy..."
#   ./setup-cloudflare.ps1

$ErrorActionPreference = "Stop"
# Do not auto-throw on a native command's non-zero exit (PS 7.4+); we check $LASTEXITCODE
# explicitly so failures are handled the same way across PowerShell versions.
$PSNativeCommandUseErrorActionPreference = $false
Set-Location -LiteralPath $PSScriptRoot

$Wt = "wrangler.toml"

# 0) Login check
& npx wrangler whoami *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Error "Log in first:  npx wrangler login"
    exit 1
}

if (Select-String -Path $Wt -Pattern "YOUR_FIREBASE_PROJECT_ID" -Quiet) {
    Write-Error "Set FIREBASE_PROJECT_ID in $Wt before deploying."
    exit 1
}

# 1) KV namespace for the Google JWKS cache (create only if id not yet filled in)
if (Select-String -Path $Wt -Pattern "REPLACE_AFTER_wrangler_kv_namespace_create" -Quiet) {
    Write-Host "==> Creating KV namespace PUBLIC_JWK_CACHE_KV"
    $out = & npx wrangler kv namespace create PUBLIC_JWK_CACHE_KV 2>&1 | Out-String
    Write-Host $out
    if ($LASTEXITCODE -ne 0) { exit 1 }

    $match = [regex]::Match($out, 'id = "([a-f0-9]+)"')
    if (-not $match.Success) {
        Write-Error "Could not parse the KV id. Set it manually in $Wt (kv_namespaces.id)."
        exit 1
    }
    $kvId = $match.Groups[1].Value
    (Get-Content -LiteralPath $Wt -Raw) `
        -replace "REPLACE_AFTER_wrangler_kv_namespace_create", $kvId `
        | Set-Content -LiteralPath $Wt -NoNewline
    Write-Host "    KV id=$kvId written to $Wt"
} else {
    Write-Host "==> KV id already configured in $Wt - skipping"
}

function Set-WorkerSecret {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [switch]$Optional
    )

    $value = [Environment]::GetEnvironmentVariable($Name)

    if ([string]::IsNullOrEmpty($value)) {
        if ($Optional) { return }
        Write-Host "==> Setting the $Name secret"
        Write-Host "    (paste $Name when wrangler prompts)"
        & npx wrangler secret put $Name
    } else {
        $label = if ($Optional) { "optional " } else { "" }
        Write-Host "==> Setting the $label$Name secret"
        $value | & npx wrangler secret put $Name
    }
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to set secret $Name."
        exit 1
    }
}

# 2) Required production secrets.
Set-WorkerSecret -Name "DATABASE_URL"
Set-WorkerSecret -Name "FIREBASE_WEB_API_KEY"

# 3) Optional production secrets. Set them as environment variables when needed.
Set-WorkerSecret -Name "INTERNAL_API_KEY" -Optional
Set-WorkerSecret -Name "GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL" -Optional
Set-WorkerSecret -Name "GOOGLE_PLAY_PRIVATE_KEY" -Optional
Set-WorkerSecret -Name "PLAY_RTDN_VERIFICATION_TOKEN" -Optional

# 4) Deploy
Write-Host "==> Deploying the worker"
& npx wrangler deploy
if ($LASTEXITCODE -ne 0) { exit 1 }

Write-Host ""
Write-Host "Done. Copy the worker URL above into android: local.properties -> syncBaseUrl=..."
