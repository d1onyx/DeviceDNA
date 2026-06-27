#!/usr/bin/env bash
# Linux/bash equivalent of setup-firebase-auth.ps1.
# Switches the Firebase Auth config files to the given project.
#
# Usage:
#   ./scripts/setup-firebase-auth.sh <PROJECT_ID> [SHA1] [--no-ios]
#
# Requires an interactive login first: firebase login
set -euo pipefail

PROJECT_ID="${1:-}"
SHA1="${2:-}"
ANDROID_PACKAGE="com.devstdvad.devicedna"
IOS_BUNDLE_ID="com.devstdvad.devicedna"
DO_IOS=1

for arg in "$@"; do
    [ "$arg" = "--no-ios" ] && DO_IOS=0
done
# If SHA1 is actually a flag, don't treat it as a fingerprint
[ "${SHA1:-}" = "--no-ios" ] && SHA1=""

if [ -z "$PROJECT_ID" ]; then
    echo "Error: provide a PROJECT_ID." >&2
    echo "Example: ./scripts/setup-firebase-auth.sh my-firebase-project AA:BB:CC:..." >&2
    exit 1
fi

# Returns the appId of the first app matching the identifier field.
find_app() {
    local platform="$1" id_prop="$2" id_val="$3"
    firebase apps:list "$platform" --project "$PROJECT_ID" --json 2>/dev/null \
        | python3 -c "import json,sys; d=json.load(sys.stdin); print(next((a['appId'] for a in d.get('result',[]) if a.get('$id_prop')=='$id_val'), ''))"
}

echo "==> Switching to project $PROJECT_ID"
firebase use "$PROJECT_ID"

echo "==> Looking for the Android app ($ANDROID_PACKAGE)"
ANDROID_APP_ID="$(find_app ANDROID packageName "$ANDROID_PACKAGE")"
if [ -z "$ANDROID_APP_ID" ]; then
    echo "    not found — creating"
    firebase apps:create ANDROID "DeviceDNA Android" --package-name "$ANDROID_PACKAGE" --project "$PROJECT_ID" >/dev/null
    ANDROID_APP_ID="$(find_app ANDROID packageName "$ANDROID_PACKAGE")"
fi
[ -n "$ANDROID_APP_ID" ] || { echo "Could not find/create the Android app." >&2; exit 1; }
echo "    appId: $ANDROID_APP_ID"

if [ -n "${SHA1:-}" ]; then
    echo "==> Adding SHA-1: $SHA1"
    firebase apps:android:sha:create "$ANDROID_APP_ID" "$SHA1" --project "$PROJECT_ID" || true
fi

echo "==> Downloading android/google-services.json"
rm -f "android/google-services.json"  # the CLI does not overwrite an existing file
firebase apps:sdkconfig ANDROID "$ANDROID_APP_ID" --project "$PROJECT_ID" --out "android/google-services.json"

HAS_WEB_CLIENT="$(python3 -c "import json; d=json.load(open('android/google-services.json')); print(any(c.get('client_type')==3 for c in d['client'][0].get('oauth_client',[])))")"
if [ "$HAS_WEB_CLIENT" != "True" ]; then
    echo "!!! WARNING: google-services.json has no web OAuth client (client_type 3)."
    echo "    Enable the Google provider in the Firebase Console, add SHA-1 for debug/release, then rerun this script."
fi

if [ "$DO_IOS" = "1" ]; then
    echo "==> Looking for the iOS app ($IOS_BUNDLE_ID)"
    IOS_APP_ID="$(find_app IOS bundleId "$IOS_BUNDLE_ID")"
    if [ -z "$IOS_APP_ID" ]; then
        echo "    not found — creating"
        firebase apps:create IOS "DeviceDNA iOS" --bundle-id "$IOS_BUNDLE_ID" --project "$PROJECT_ID" >/dev/null
        IOS_APP_ID="$(find_app IOS bundleId "$IOS_BUNDLE_ID")"
    fi
    if [ -n "$IOS_APP_ID" ]; then
        echo "    appId: $IOS_APP_ID"
        echo "==> Downloading ios/DeviceDNAApp/GoogleService-Info.plist"
        rm -f "ios/DeviceDNAApp/GoogleService-Info.plist"  # the CLI does not overwrite an existing file
        firebase apps:sdkconfig IOS "$IOS_APP_ID" --project "$PROJECT_ID" --out "ios/DeviceDNAApp/GoogleService-Info.plist"
    fi
fi

# Update the default project in .firebaserc
python3 -c "import json; json.dump({'projects':{'default':'$PROJECT_ID'}}, open('.firebaserc','w'), indent=2)"

echo ""
echo "Done. Config files updated to project $PROJECT_ID."
echo "Reminder: Firebase Console > Authentication > Sign-in method > enable Google + support email."
