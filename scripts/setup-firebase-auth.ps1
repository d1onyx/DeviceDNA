param(
    [string]$ProjectId = "projektdna-4fc7d",
    [string]$AndroidPackageName = "com.devstdvad.devicedna",
    [string]$IosBundleId = "com.devstdvad.devicedna",
    [string]$AndroidSha1 = ""
)

$ErrorActionPreference = "Stop"

function Invoke-FirebaseJson {
    param([string[]]$Arguments)

    $output = & firebase @Arguments --json
    if ($LASTEXITCODE -ne 0) {
        throw "firebase $($Arguments -join ' ') failed."
    }

    return $output | ConvertFrom-Json
}

function Get-FirebaseApp {
    param(
        [string]$Platform,
        [string]$IdentifierProperty,
        [string]$IdentifierValue
    )

    $apps = Invoke-FirebaseJson @("apps:list", $Platform, "--project", $ProjectId)
    return @($apps.result) | Where-Object { $_.$IdentifierProperty -eq $IdentifierValue } | Select-Object -First 1
}

firebase use $ProjectId
if ($LASTEXITCODE -ne 0) {
    throw "Unable to select Firebase project $ProjectId."
}

$androidApp = Get-FirebaseApp "ANDROID" "packageName" $AndroidPackageName
if (-not $androidApp) {
    Invoke-FirebaseJson @("apps:create", "ANDROID", "DeviceDNA Android", "--package-name", $AndroidPackageName, "--project", $ProjectId) | Out-Null
    $androidApp = Get-FirebaseApp "ANDROID" "packageName" $AndroidPackageName
}

if (-not $androidApp) {
    throw "Unable to find or create Android Firebase app for $AndroidPackageName."
}

if ($AndroidSha1.Trim().Length -gt 0) {
    firebase apps:android:sha:create $androidApp.appId $AndroidSha1 --project $ProjectId
    if ($LASTEXITCODE -ne 0) {
        throw "Unable to add Android SHA-1 fingerprint."
    }
}

firebase apps:sdkconfig ANDROID $androidApp.appId --project $ProjectId --out "android/google-services.json"
if ($LASTEXITCODE -ne 0) {
    throw "Unable to write android/google-services.json."
}

$androidConfig = Get-Content "android/google-services.json" -Raw | ConvertFrom-Json
$androidOauthClients = @($androidConfig.client[0].oauth_client)
$androidWebClients = @($androidOauthClients | Where-Object { $_.client_type -eq 3 })
if ($androidWebClients.Count -eq 0) {
    Write-Warning "android/google-services.json does not contain a web OAuth client. Enable the Google auth provider, add SHA-1 fingerprints for debug/release builds, then rerun this script."
}

$iosApp = Get-FirebaseApp "IOS" "bundleId" $IosBundleId
if (-not $iosApp) {
    Invoke-FirebaseJson @("apps:create", "IOS", "DeviceDNA iOS", "--bundle-id", $IosBundleId, "--project", $ProjectId) | Out-Null
    $iosApp = Get-FirebaseApp "IOS" "bundleId" $IosBundleId
}

if (-not $iosApp) {
    throw "Unable to find or create iOS Firebase app for $IosBundleId."
}

firebase apps:sdkconfig IOS $iosApp.appId --project $ProjectId --out "ios/DeviceDNAApp/GoogleService-Info.plist"
if ($LASTEXITCODE -ne 0) {
    throw "Unable to write ios/DeviceDNAApp/GoogleService-Info.plist."
}

Write-Host "Firebase app configs refreshed."
Write-Host "Enable Authentication > Sign-in method > Google in Firebase Console if it is not already enabled."
Write-Host "For iOS, add GoogleService-Info.plist to the Xcode target and set URL Types to its REVERSED_CLIENT_ID."
