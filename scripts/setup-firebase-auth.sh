#!/usr/bin/env bash
# Full Firebase setup for DeviceDNA: creates the project if needed, enables Google Sign-In,
# registers Android/iOS apps, adds SHA-1, and downloads config files.
#
# Usage:
#   ./scripts/setup-firebase-auth.sh <PROJECT_ID> [SHA1] [--no-ios] [--display-name "My App"]
#
# Requires: firebase login  (and optionally gcloud auth login for enabling Google Sign-In)
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

PROJECT_ID="${1:-}"
SHA1=""
DO_IOS=1
DISPLAY_NAME="DeviceDNA"
ANDROID_PACKAGE="com.devstdvad.devicedna"
IOS_BUNDLE_ID="com.devstdvad.devicedna"

shift || true
for arg in "$@"; do
    case "$arg" in
        --no-ios) DO_IOS=0 ;;
        --display-name) ;;
        *) [ -z "$SHA1" ] && SHA1="$arg" ;;
    esac
done
for i in "$@"; do
    if [ "$i" = "--display-name" ]; then shift; DISPLAY_NAME="${1:-DeviceDNA}"; break; fi
done

if [ -z "$PROJECT_ID" ]; then
    echo "Usage: ./scripts/setup-firebase-auth.sh <PROJECT_ID> [SHA1] [--no-ios]" >&2
    exit 1
fi

# ── helpers ───────────────────────────────────────────────────────────────────

# Retry find_app up to N times (new projects take ~10s to become visible in apps:list).
find_app() {
    local platform="$1" id_prop="$2" id_val="$3" attempts="${4:-1}"
    local result=""
    for i in $(seq 1 "$attempts"); do
        result=$(firebase apps:list "$platform" --project "$PROJECT_ID" --json 2>/dev/null \
            | python3 -c "
import json,sys
d=json.load(sys.stdin)
print(next((a['appId'] for a in d.get('result',[]) if a.get('$id_prop')=='$id_val'), ''))
" 2>/dev/null || true)
        [ -n "$result" ] && echo "$result" && return
        [ "$i" -lt "$attempts" ] && echo "    waiting for Firebase to propagate... ($i/$attempts)" >&2 && sleep 5
    done
    echo ""
}

get_access_token() {
    command -v gcloud >/dev/null 2>&1 && gcloud auth print-access-token 2>/dev/null || echo ""
}

# ── 0) login check ────────────────────────────────────────────────────────────

if ! firebase projects:list --json >/dev/null 2>&1; then
    echo "Not logged in. Run:  firebase login" >&2; exit 1
fi

# ── 1) create or connect to project ──────────────────────────────────────────

PROJECT_EXISTS=$(firebase projects:list --json 2>/dev/null \
    | python3 -c "
import json,sys
d=json.load(sys.stdin)
print('yes' if any(p.get('projectId')=='$PROJECT_ID' for p in d.get('result',[])) else 'no')
")

if [ "$PROJECT_EXISTS" = "yes" ]; then
    echo "==> Project $PROJECT_ID already exists — connecting"
else
    echo "==> Creating project $PROJECT_ID (\"$DISPLAY_NAME\")"
    CREATE_OUT=$(firebase projects:create "$PROJECT_ID" --display-name "$DISPLAY_NAME" --non-interactive 2>&1         || firebase projects:create "$PROJECT_ID" --display-name "$DISPLAY_NAME" <<< $'n\n' 2>&1 || true)
    echo "$CREATE_OUT"
    if echo "$CREATE_OUT" | grep -q "already a project with ID"; then
        echo "    Project ID taken globally — trying to connect as a Firebase project..."
        if ! firebase use "$PROJECT_ID" 2>/dev/null; then
            echo "ERROR: Project $PROJECT_ID exists but is not accessible in your Firebase account." >&2
            echo "       Choose a different project ID, or check Firebase Console for this project." >&2
            exit 1
        fi
    else
        echo "    created. Waiting for Firebase to initialize..."
        sleep 8
    fi
fi

echo "==> Switching to project $PROJECT_ID"
firebase use "$PROJECT_ID"

# ── 2) enable Google Sign-In ──────────────────────────────────────────────────

echo "==> Enabling Google Sign-In"
ACCESS_TOKEN="$(get_access_token)"

if [ -n "$ACCESS_TOKEN" ]; then
    RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X PATCH \
        "https://identitytoolkit.googleapis.com/admin/v2/projects/$PROJECT_ID/defaultSupportedIdpConfigs/google.com?updateMask=enabled" \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{"enabled":true}')
    if [ "$RESPONSE" = "200" ]; then
        echo "    Google Sign-In enabled."
    else
        echo "    API returned HTTP $RESPONSE."
        echo "    -> Enable manually: Firebase Console > Authentication > Sign-in method > Google"
    fi
else
    echo "    (gcloud not found — enable manually)"
    echo "    -> Firebase Console > Authentication > Sign-in method > Google > Enable"
fi

# ── 3) Android app ────────────────────────────────────────────────────────────

echo "==> Looking for Android app ($ANDROID_PACKAGE)"
# Retry up to 6 times (30s total) — new projects propagate slowly
ANDROID_APP_ID="$(find_app ANDROID packageName "$ANDROID_PACKAGE" 6)"
if [ -z "$ANDROID_APP_ID" ]; then
    echo "    not found — creating"
    firebase apps:create ANDROID "DeviceDNA Android" \
        --package-name "$ANDROID_PACKAGE" --project "$PROJECT_ID" --non-interactive 2>&1 | grep -v "^$" || true
    ANDROID_APP_ID="$(find_app ANDROID packageName "$ANDROID_PACKAGE" 6)"
fi
[ -n "$ANDROID_APP_ID" ] || { echo "ERROR: Could not find/create Android app." >&2; exit 1; }
echo "    appId: $ANDROID_APP_ID"

if [ -n "${SHA1:-}" ]; then
    echo "==> Adding SHA-1: $SHA1"
    firebase apps:android:sha:create "$ANDROID_APP_ID" "$SHA1" --project "$PROJECT_ID" || true
fi

echo "==> Downloading android/google-services.json"
rm -f "android/google-services.json"
firebase apps:sdkconfig ANDROID "$ANDROID_APP_ID" \
    --project "$PROJECT_ID" --out "android/google-services.json"

HAS_WEB_CLIENT=$(python3 -c "
import json
d=json.load(open('android/google-services.json'))
print(any(c.get('client_type')==3 for c in d['client'][0].get('oauth_client',[])))
")
if [ "$HAS_WEB_CLIENT" != "True" ]; then
    echo ""
    echo "!!! google-services.json has no web OAuth client (client_type 3)."
    echo "    Google Sign-In is not yet active for this project."
    echo "    -> Firebase Console > Authentication > Sign-in method > Google > Enable + support email"
    echo "    -> Then rerun:  ./scripts/setup-firebase-auth.sh $PROJECT_ID"
fi

# ── 4) iOS app (optional) ─────────────────────────────────────────────────────

if [ "$DO_IOS" = "1" ]; then
    echo "==> Looking for iOS app ($IOS_BUNDLE_ID)"
    IOS_APP_ID="$(find_app IOS bundleId "$IOS_BUNDLE_ID" 3)"
    if [ -z "$IOS_APP_ID" ]; then
        echo "    not found — creating"
        firebase apps:create IOS "DeviceDNA iOS" \
            --bundle-id "$IOS_BUNDLE_ID" --project "$PROJECT_ID" --non-interactive 2>&1 | grep -v "^$" || true
        IOS_APP_ID="$(find_app IOS bundleId "$IOS_BUNDLE_ID" 6)"
    fi
    if [ -n "$IOS_APP_ID" ]; then
        echo "    appId: $IOS_APP_ID"
        echo "==> Downloading ios/DeviceDNAApp/GoogleService-Info.plist"
        rm -f "ios/DeviceDNAApp/GoogleService-Info.plist"
        firebase apps:sdkconfig IOS "$IOS_APP_ID" \
            --project "$PROJECT_ID" --out "ios/DeviceDNAApp/GoogleService-Info.plist"
    fi
fi

# ── 5) update .firebaserc ─────────────────────────────────────────────────────

python3 -c "import json; json.dump({'projects':{'default':'$PROJECT_ID'}}, open('.firebaserc','w'), indent=2)"

# ── done ──────────────────────────────────────────────────────────────────────

echo ""
echo "Done. Project: $PROJECT_ID"
echo ""
echo "Next steps:"
echo "  1. (if Google Sign-In not enabled yet) Enable it in Firebase Console + rerun this script"
echo "  2. Get Web API key: Firebase Console > Project settings > General > Web API key"
echo "  3. Deploy Worker:"
echo "     cd backend && FIREBASE_WEB_API_KEY='AIzaSy...' bash setup-cloudflare.sh"
echo "  4. Set backend URL in android/local.properties:"
echo "     syncBaseUrl=https://<worker>.<subdomain>.workers.dev"
