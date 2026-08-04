#!/bin/bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MOD_FOLDER="Diable Avionics Original Flavor"
OUTPUT_DIR="${1:-$ROOT_DIR/dist}"

if [[ "$OUTPUT_DIR" != /* ]]; then
    OUTPUT_DIR="$ROOT_DIR/$OUTPUT_DIR"
fi

if [[ -n "$(git -C "$ROOT_DIR" status --porcelain --untracked-files=normal)" ]]; then
    echo "Release aborted: the working tree has uncommitted changes." >&2
    echo "Commit or stash them so the archive matches a known revision." >&2
    exit 1
fi

ARCHIVE_NAME="Diable Avionics Original Flavor.zip"
ARCHIVE_PATH="$OUTPUT_DIR/$ARCHIVE_NAME"

TEMP_DIR="$(/usr/bin/mktemp -d)"
TEMP_ARCHIVE="$TEMP_DIR/$ARCHIVE_NAME"

cleanup() {
    /bin/rm -rf -- "$TEMP_DIR"
}
trap cleanup EXIT

/bin/mkdir -p -- "$OUTPUT_DIR"

git -C "$ROOT_DIR" archive \
    --worktree-attributes \
    --format=zip \
    --prefix="$MOD_FOLDER/" \
    --output="$TEMP_ARCHIVE" \
    HEAD

/usr/bin/unzip -tqq "$TEMP_ARCHIVE"

BAD_ENTRIES="$(/usr/bin/unzip -Z1 "$TEMP_ARCHIVE" | /usr/bin/grep -E '(^|/)(__MACOSX(/|$)|\.DS_Store$|\._[^/]*$)' || true)"
if [[ -n "$BAD_ENTRIES" ]]; then
    echo "Release aborted: macOS metadata was found in the archive:" >&2
    echo "$BAD_ENTRIES" >&2
    exit 1
fi

DEV_ENTRIES="$(/usr/bin/unzip -Z1 "$TEMP_ARCHIVE" | /usr/bin/grep -E '(^|/)(\.git|\.idea|\.run|out|dist|tools)(/|$)|\.iml$' || true)"
if [[ -n "$DEV_ENTRIES" ]]; then
    echo "Release aborted: development files were found in the archive:" >&2
    echo "$DEV_ENTRIES" >&2
    exit 1
fi

/bin/mv -f -- "$TEMP_ARCHIVE" "$ARCHIVE_PATH"

ARCHIVE_SIZE="$(/usr/bin/du -h "$ARCHIVE_PATH" | /usr/bin/awk '{print $1}')"
echo "Created $ARCHIVE_PATH ($ARCHIVE_SIZE)"
