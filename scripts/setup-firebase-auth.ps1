#Requires -Version 5.1
# Full Firebase setup for DeviceDNA: creates the project if needed, enables Google Sign-In,
# registers Android/iOS apps, adds SHA-1, and downloads config files.
# Windows/PowerShell port of setup-firebase-auth.sh.
#
# Usage:
#   ./scripts/setup-firebase-auth.ps1 -ProjectId <PROJECT_ID> [-AndroidSha1 <SHA1>] [-NoIos] [-DisplayName "My App"]
#
# Requires: firebase login  (and optionally gcloud auth login for enabling Google Sign-In)

param(
    [Parameter(Position = 0)][string]$ProjectId = "",
    [Parameter(Position = 1)][string]$AndroidSha1 = "",
    [string]$AndroidPackageName = "",
    [string]$IosBundleId = "",
    [string]$DisplayName = "",
    [switch]$NoIos
)

$ErrorActionPreference = "Stop"
# Do not auto-throw on a native command's non-zero exit (PS 7.4+); firebase CLI calls are
# tolerated/retried explicitly, mirroring the bash script's `|| true` handling.
$PSNativeCommandUseErrorActionPreference = $false

# Run from the repository root so the relative config paths resolve.
$repoRoot = (& git rev-parse --show-toplevel 2>$null)
if ($LASTEXITCODE -eq 0 -and $repoRoot) {
    Set-Location -LiteralPath $repoRoot.Trim()
}

. (Join-Path $PSScriptRoot "lib/Config.ps1")

function Get-LocalProperty {
    param([Parameter(Mandatory = $true)][string]$Name)
    return Get-ConfigProperty -Name $Name
}

if ([string]::IsNullOrWhiteSpace($ProjectId)) { $ProjectId = Get-LocalProperty -Name "firebaseProjectId" }
if ([string]::IsNullOrWhiteSpace($AndroidPackageName)) { $AndroidPackageName = Get-LocalProperty -Name "androidApplicationId" }
if ([string]::IsNullOrWhiteSpace($IosBundleId)) { $IosBundleId = Get-LocalProperty -Name "iosBundleId" }
if ([string]::IsNullOrWhiteSpace($DisplayName)) { $DisplayName = Get-LocalProperty -Name "appDisplayName" }
if ([string]::IsNullOrWhiteSpace($DisplayName)) { $DisplayName = "DeviceDNA" }

if ([string]::IsNullOrWhiteSpace($ProjectId)) {
    Write-Error "Usage: ./scripts/setup-firebase-auth.ps1 -ProjectId <PROJECT_ID> [-AndroidSha1 <SHA1>] [-NoIos]"
    exit 1
}
if ([string]::IsNullOrWhiteSpace($AndroidPackageName)) {
    Write-Error "Set androidApplicationId in secrets.properties or pass -AndroidPackageName before creating Firebase Android config."
    exit 1
}
if (-not $NoIos -and [string]::IsNullOrWhiteSpace($IosBundleId)) {
    Write-Error "Set iosBundleId in secrets.properties, pass -IosBundleId, or use -NoIos."
    exit 1
}

# ── helpers ───────────────────────────────────────────────────────────────────

# Retry until the app shows up (new projects take ~10s to become visible in apps:list).
function Find-App {
    param(
        [string]$Platform,
        [string]$IdProperty,
        [string]$IdValue,
        [int]$Attempts = 1
    )

    for ($i = 1; $i -le $Attempts; $i++) {
        try {
            $raw = & firebase apps:list $Platform --project $ProjectId --json 2>$null
            $data = $raw | ConvertFrom-Json
            $app = @($data.result) | Where-Object { $_.$IdProperty -eq $IdValue } | Select-Object -First 1
            if ($app) { return $app.appId }
        } catch {
            # ignore and retry
        }
        if ($i -lt $Attempts) {
            Write-Host "    waiting for Firebase to propagate... ($i/$Attempts)"
            Start-Sleep -Seconds 5
        }
    }
    return ""
}

function Get-AccessToken {
    if (Get-Command gcloud -ErrorAction SilentlyContinue) {
        $token = & gcloud auth print-access-token 2>$null
        if ($LASTEXITCODE -eq 0) { return $token.Trim() }
    }
    return ""
}

# ── 0) login check ────────────────────────────────────────────────────────────

& firebase projects:list --json *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Error "Not logged in. Run:  firebase login"
    exit 1
}

# ── 1) create or connect to project ──────────────────────────────────────────

$projects = (& firebase projects:list --json 2>$null | ConvertFrom-Json).result
$projectExists = @($projects | Where-Object { $_.projectId -eq $ProjectId }).Count -gt 0

if ($projectExists) {
    Write-Host "==> Project $ProjectId already exists - connecting"
} else {
    Write-Host "==> Creating project $ProjectId (`"$DisplayName`")"
    $createOut = & firebase projects:create $ProjectId --display-name $DisplayName --non-interactive 2>&1 | Out-String
    Write-Host $createOut
    if ($createOut -match "already a project with ID") {
        Write-Host "    Project ID taken globally - trying to connect as a Firebase project..."
        & firebase use $ProjectId 2>$null
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Project $ProjectId exists but is not accessible in your Firebase account. Choose a different project ID, or check Firebase Console."
            exit 1
        }
    } else {
        Write-Host "    created. Waiting for Firebase to initialize..."
        Start-Sleep -Seconds 8
    }
}

Write-Host "==> Switching to project $ProjectId"
& firebase use $ProjectId
if ($LASTEXITCODE -ne 0) {
    Write-Error "Unable to select Firebase project $ProjectId."
    exit 1
}

# ── 2) enable Google Sign-In ──────────────────────────────────────────────────

Write-Host "==> Enabling Google Sign-In"
$accessToken = Get-AccessToken

if ($accessToken) {
    $uri = "https://identitytoolkit.googleapis.com/admin/v2/projects/$ProjectId/defaultSupportedIdpConfigs/google.com?updateMask=enabled"
    try {
        Invoke-RestMethod -Method Patch -Uri $uri `
            -Headers @{ Authorization = "Bearer $accessToken" } `
            -ContentType "application/json" `
            -Body '{"enabled":true}' | Out-Null
        Write-Host "    Google Sign-In enabled."
    } catch {
        Write-Host "    API call failed: $($_.Exception.Message)"
        Write-Host "    -> Enable manually: Firebase Console > Authentication > Sign-in method > Google"
    }
} else {
    Write-Host "    (gcloud not found - enable manually)"
    Write-Host "    -> Firebase Console > Authentication > Sign-in method > Google > Enable"
}

# ── 3) Android app ────────────────────────────────────────────────────────────

Write-Host "==> Looking for Android app ($AndroidPackageName)"
$androidAppId = Find-App "ANDROID" "packageName" $AndroidPackageName 6
if (-not $androidAppId) {
    Write-Host "    not found - creating"
    & firebase apps:create ANDROID "DeviceDNA Android" --package-name $AndroidPackageName --project $ProjectId --non-interactive 2>&1 | Out-Null
    $androidAppId = Find-App "ANDROID" "packageName" $AndroidPackageName 6
}
if (-not $androidAppId) {
    Write-Error "Could not find/create Android app."
    exit 1
}
Write-Host "    appId: $androidAppId"

if (-not [string]::IsNullOrWhiteSpace($AndroidSha1)) {
    Write-Host "==> Adding SHA-1: $AndroidSha1"
    & firebase apps:android:sha:create $androidAppId $AndroidSha1 --project $ProjectId
}

Write-Host "==> Downloading android/google-services.json"
Remove-Item -LiteralPath "android/google-services.json" -ErrorAction SilentlyContinue
& firebase apps:sdkconfig ANDROID $androidAppId --project $ProjectId --out "android/google-services.json"
if ($LASTEXITCODE -ne 0) {
    Write-Error "Unable to write android/google-services.json."
    exit 1
}

$androidConfig = Get-Content -LiteralPath "android/google-services.json" -Raw | ConvertFrom-Json
$hasWebClient = @($androidConfig.client[0].oauth_client | Where-Object { $_.client_type -eq 3 }).Count -gt 0
if (-not $hasWebClient) {
    Write-Host ""
    Write-Host "!!! google-services.json has no web OAuth client (client_type 3)."
    Write-Host "    Google Sign-In is not yet active for this project."
    Write-Host "    -> Firebase Console > Authentication > Sign-in method > Google > Enable + support email"
    Write-Host "    -> Then rerun:  ./scripts/setup-firebase-auth.ps1 -ProjectId $ProjectId"
}

# ── 4) iOS app (optional) ─────────────────────────────────────────────────────

if (-not $NoIos) {
    Write-Host "==> Looking for iOS app ($IosBundleId)"
    $iosAppId = Find-App "IOS" "bundleId" $IosBundleId 3
    if (-not $iosAppId) {
        Write-Host "    not found - creating"
        & firebase apps:create IOS "DeviceDNA iOS" --bundle-id $IosBundleId --project $ProjectId --non-interactive 2>&1 | Out-Null
        $iosAppId = Find-App "IOS" "bundleId" $IosBundleId 6
    }
    if ($iosAppId) {
        Write-Host "    appId: $iosAppId"
        Write-Host "==> Downloading ios/DeviceDNAApp/GoogleService-Info.plist"
        Remove-Item -LiteralPath "ios/DeviceDNAApp/GoogleService-Info.plist" -ErrorAction SilentlyContinue
        & firebase apps:sdkconfig IOS $iosAppId --project $ProjectId --out "ios/DeviceDNAApp/GoogleService-Info.plist"
    }
}

# ── 5) update .firebaserc ─────────────────────────────────────────────────────

@{ projects = @{ default = $ProjectId } } | ConvertTo-Json | Set-Content -LiteralPath ".firebaserc"

# ── done ──────────────────────────────────────────────────────────────────────

Write-Host ""
Write-Host "Done. Project: $ProjectId"
Write-Host ""
Write-Host "Next steps:"
Write-Host "  1. (if Google Sign-In not enabled yet) Enable it in Firebase Console + rerun this script"
Write-Host "  2. Get Web API key: Firebase Console > Project settings > General > Web API key"
Write-Host "  3. Deploy Worker:"
Write-Host "     cd backend; `$env:FIREBASE_WEB_API_KEY='AIzaSy...'; ./setup-cloudflare.ps1"
Write-Host "  4. Set the backend URL in secrets.properties:"
Write-Host "     syncBaseUrl=https://<worker>.<subdomain>.workers.dev"
