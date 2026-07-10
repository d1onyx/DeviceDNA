#!/usr/bin/env bash
# Shared reader for secrets.properties — sourced by the other scripts.
# Values fall back to local.properties so a half-migrated checkout still builds.

: "${REPO_ROOT:="$(git rev-parse --show-toplevel)"}"

SECRETS_FILE="$REPO_ROOT/secrets.properties"
LEGACY_FILES=("$REPO_ROOT/local.properties" "$REPO_ROOT/keystore.properties")

# read_property_from <file> <key>
read_property_from() {
    local file="$1" name="$2"
    [ -f "$file" ] || return 0
    awk -F= -v key="$name" '
        $0 !~ /^[[:space:]]*#/ && $1 == key {
            sub(/^[^=]*=/, "")
            gsub(/^[[:space:]]+|[[:space:]]+$/, "")
            print
            exit
        }
    ' "$file"
}

# cfg <key> [fallback_key] — first non-empty match across secrets, then legacy files.
cfg() {
    local name="$1" fallback="${2:-}" value=""
    for file in "$SECRETS_FILE" "${LEGACY_FILES[@]}"; do
        value="$(read_property_from "$file" "$name")"
        [ -n "$value" ] && { printf '%s' "$value"; return 0; }
    done
    [ -n "$fallback" ] && cfg "$fallback"
    return 0
}

# set_property <key> <value> — upsert into secrets.properties, preserving comments.
# Rewrites an existing `key=` line in place (commented or not); appends when absent.
set_property() {
    local name="$1" value="$2"
    require_secrets_file
    SP_NAME="$name" SP_VALUE="$value" SP_FILE="$SECRETS_FILE" python3 - <<'PY'
import os, re

name, value, path = os.environ["SP_NAME"], os.environ["SP_VALUE"], os.environ["SP_FILE"]
with open(path) as f:
    lines = f.read().splitlines()

pattern = re.compile(rf"^#?\s*{re.escape(name)}=")
for i, line in enumerate(lines):
    if pattern.match(line):
        lines[i] = f"{name}={value}"
        break
else:
    lines.append(f"{name}={value}")

with open(path, "w") as f:
    f.write("\n".join(lines) + "\n")
PY
}

# require_secrets_file — fail early with a copy-pasteable fix.
require_secrets_file() {
    [ -f "$SECRETS_FILE" ] && return 0
    echo "Missing secrets.properties. Create it with:" >&2
    echo "    cp secrets.properties.example secrets.properties" >&2
    echo "Then fill in the values and rerun this script." >&2
    exit 1
}

# require_values <key>... — abort listing every empty required key at once.
require_values() {
    local missing=()
    for name in "$@"; do
        [ -z "$(cfg "$name")" ] && missing+=("$name")
    done
    if [ ${#missing[@]} -gt 0 ]; then
        echo "Missing required values in secrets.properties:" >&2
        printf '    %s\n' "${missing[@]}" >&2
        exit 1
    fi
}
