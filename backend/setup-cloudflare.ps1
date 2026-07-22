#Requires -Version 5.1
# Deploy the Worker to Cloudflare: KV + required secrets + deploy.
# Windows/PowerShell port of setup-cloudflare.sh.
# Prerequisite: `npx wrangler login` has already been run.
# Full migration checklist: ../MIGRATION.md
#
# Pass secrets as environment variables to avoid interactive prompts, e.g.:
#   $env:FIREBASE_PROJECT_ID="..."; $env:FIREBASE_WEB_API_KEY="AIzaSy..."
#   ./setup-cloudflare.ps1

$ErrorActionPreference = "Stop"
# Do not auto-throw on a native command's non-zero exit (PS 7.4+); we check $LASTEXITCODE
# explicitly so failures are handled the same way across PowerShell versions.
$PSNativeCommandUseErrorActionPreference = $false
Set-Location -LiteralPath $PSScriptRoot

$Wt = "wrangler.toml"
. (Join-Path (Split-Path -Parent $PSScriptRoot) "scripts/lib/Config.ps1")

function Set-EnvFromProperty {
    param(
        [Parameter(Mandatory = $true)][string]$EnvName,
        [Parameter(Mandatory = $true)][string]$PropertyName,
        [string]$FallbackPropertyName = ""
    )

    if (-not [string]::IsNullOrEmpty([Environment]::GetEnvironmentVariable($EnvName))) { return }
    $value = Get-ConfigProperty -Name $PropertyName -FallbackName $FallbackPropertyName
    if (-not [string]::IsNullOrWhiteSpace($value)) {
        [Environment]::SetEnvironmentVariable($EnvName, $value, "Process")
    }
}

Set-EnvFromProperty -EnvName "FIREBASE_PROJECT_ID" -PropertyName "firebaseProjectId"
Set-EnvFromProperty -EnvName "FIREBASE_WEB_API_KEY" -PropertyName "firebaseWebApiKey"
Set-EnvFromProperty -EnvName "GOOGLE_PLAY_PACKAGE_NAME" -PropertyName "googlePlayPackageName" -FallbackPropertyName "androidApplicationId"
Set-EnvFromProperty -EnvName "GOOGLE_PLAY_PREMIUM_PRODUCT_ID" -PropertyName "googlePlayPremiumProductId" -FallbackPropertyName "premiumSubProductId"
Set-EnvFromProperty -EnvName "INTERNAL_API_KEY" -PropertyName "internalApiKey"
Set-EnvFromProperty -EnvName "GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL" -PropertyName "googlePlayServiceAccountEmail"
Set-EnvFromProperty -EnvName "GOOGLE_PLAY_PRIVATE_KEY" -PropertyName "googlePlayPrivateKey"
Set-EnvFromProperty -EnvName "APPLE_APP_STORE_ISSUER_ID" -PropertyName "appStoreIssuerId"
Set-EnvFromProperty -EnvName "APPLE_APP_STORE_KEY_ID" -PropertyName "appStoreKeyId"
Set-EnvFromProperty -EnvName "APPLE_APP_STORE_PRIVATE_KEY" -PropertyName "appStorePrivateKey"
Set-EnvFromProperty -EnvName "APPLE_APP_BUNDLE_ID" -PropertyName "iosBundleId"
Set-EnvFromProperty -EnvName "APPLE_PREMIUM_PRODUCT_ID" -PropertyName "iosStoreKitPremiumProductId"
Set-EnvFromProperty -EnvName "PLAY_RTDN_VERIFICATION_TOKEN" -PropertyName "playRtdnVerificationToken"

# 0) Login check
& npx wrangler whoami *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Error "Log in first:  npx wrangler login"
    exit 1
}

# 1) KV namespace for the Google JWKS cache. wrangler.toml is generated, so the id is
#    stored in secrets.properties and the file re-rendered - never patched in place.
if (-not (Test-Path -LiteralPath $Wt)) { Invoke-SyncConfig }

if ([string]::IsNullOrWhiteSpace((Get-ConfigProperty -Name "cloudflareKvId"))) {
    Write-Host "==> Creating KV namespace PUBLIC_JWK_CACHE_KV"
    $out = & npx wrangler kv namespace create PUBLIC_JWK_CACHE_KV 2>&1 | Out-String
    Write-Host $out
    if ($LASTEXITCODE -ne 0) { exit 1 }

    $match = [regex]::Match($out, 'id = "([a-f0-9]+)"')
    if (-not $match.Success) {
        Write-Error "Could not parse the KV id. Set cloudflareKvId=... in secrets.properties."
        exit 1
    }
    $kvId = $match.Groups[1].Value
    Set-ConfigProperty -Name "cloudflareKvId" -Value $kvId
    Invoke-SyncConfig
    Write-Host "    KV id=$kvId written to secrets.properties; $Wt regenerated"
} else {
    Write-Host "==> KV id already set in secrets.properties - skipping"
}

# 1b) D1 database. Same pattern as KV: create once, store the id and re-render wrangler.toml.
if ([string]::IsNullOrWhiteSpace((Get-ConfigProperty -Name "cloudflareD1Id"))) {
    Write-Host "==> Creating D1 database DeviceDNA_DB"
    $out = & npx wrangler d1 create DeviceDNA_DB 2>&1 | Out-String
    Write-Host $out
    if ($LASTEXITCODE -ne 0) { exit 1 }

    $match = [regex]::Match($out, '([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})')
    if (-not $match.Success) {
        Write-Error "Could not parse the D1 id. Set cloudflareD1Id=... in secrets.properties."
        exit 1
    }
    $d1Id = $match.Groups[1].Value
    Set-ConfigProperty -Name "cloudflareD1Id" -Value $d1Id
    Invoke-SyncConfig
    Write-Host "    D1 id=$d1Id written to secrets.properties; $Wt regenerated"
} else {
    Write-Host "==> D1 id already set in secrets.properties - skipping"
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
Set-WorkerSecret -Name "FIREBASE_PROJECT_ID"
Set-WorkerSecret -Name "FIREBASE_WEB_API_KEY"
Set-WorkerSecret -Name "APPLE_APP_STORE_ISSUER_ID"
Set-WorkerSecret -Name "APPLE_APP_STORE_KEY_ID"
Set-WorkerSecret -Name "APPLE_APP_STORE_PRIVATE_KEY"
Set-WorkerSecret -Name "APPLE_APP_BUNDLE_ID"
Set-WorkerSecret -Name "APPLE_PREMIUM_PRODUCT_ID"

# 3) Optional production secrets. Set them as environment variables or in ../secrets.properties.
Set-WorkerSecret -Name "INTERNAL_API_KEY" -Optional
Set-WorkerSecret -Name "GOOGLE_PLAY_PACKAGE_NAME" -Optional
Set-WorkerSecret -Name "GOOGLE_PLAY_PREMIUM_PRODUCT_ID" -Optional
Set-WorkerSecret -Name "GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL" -Optional
Set-WorkerSecret -Name "GOOGLE_PLAY_PRIVATE_KEY" -Optional
Set-WorkerSecret -Name "PLAY_RTDN_VERIFICATION_TOKEN" -Optional

# 4) Apply D1 migrations to the remote database before deploying.
Write-Host "==> Applying D1 migrations (remote)"
& npx wrangler d1 migrations apply DeviceDNA_DB --remote
if ($LASTEXITCODE -ne 0) { exit 1 }

# 5) Deploy
Write-Host "==> Deploying the worker"
& npx wrangler deploy
if ($LASTEXITCODE -ne 0) { exit 1 }

Write-Host ""
Write-Host "Done. Copy the worker URL above into secrets.properties -> syncBaseUrl=..."
Write-Host "Then rerun ./scripts/sync-config.sh so iOS and .dev.vars pick it up."
