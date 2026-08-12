#!/bin/bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MOD_FOLDER="$(basename "$ROOT_DIR")"
OUTPUT_DIR="${1:-$ROOT_DIR/dist}"

if [[ "$OUTPUT_DIR" != /* ]]; then
    OUTPUT_DIR="$ROOT_DIR/$OUTPUT_DIR"
fi

ARCHIVE_NAME="$MOD_FOLDER - Playtest Alpha.zip"
ARCHIVE_PATH="$OUTPUT_DIR/$ARCHIVE_NAME"

TEMP_DIR="$(/usr/bin/mktemp -d)"
TEMP_ARCHIVE="$TEMP_DIR/$ARCHIVE_NAME"

cleanup() {
    /bin/rm -rf -- "$TEMP_DIR"
}
trap cleanup EXIT

/bin/mkdir -p -- "$OUTPUT_DIR"

(
    cd "$(dirname "$ROOT_DIR")"
    /usr/bin/zip -qry "$TEMP_ARCHIVE" "$MOD_FOLDER" \
        -x "$MOD_FOLDER/.git" \
           "$MOD_FOLDER/.git/*" \
           "$MOD_FOLDER/.idea" \
           "$MOD_FOLDER/.idea/*" \
           "$MOD_FOLDER/.run" \
           "$MOD_FOLDER/.run/*" \
           "$MOD_FOLDER/out" \
           "$MOD_FOLDER/out/*" \
           "$MOD_FOLDER/dist" \
           "$MOD_FOLDER/dist/*" \
           "$MOD_FOLDER/tools" \
           "$MOD_FOLDER/tools/*" \
           "$MOD_FOLDER/*.iml" \
           "$MOD_FOLDER/.gitattributes" \
           "$MOD_FOLDER/.gitignore" \
           '*/.DS_Store' \
           '*/Thumbs.db' \
           '*/Desktop.ini' \
           '*/__MACOSX/*' \
           '*/._*'
)

/usr/bin/unzip -tqq "$TEMP_ARCHIVE"

DEV_ENTRIES="$(
    /usr/bin/unzip -Z1 "$TEMP_ARCHIVE" \
        | /usr/bin/grep -E '(^|/)(\.git|\.idea|\.run|out|dist|tools)(/|$)|\.iml$|(^|/)(\.DS_Store|Thumbs\.db|Desktop\.ini)$|(^|/)__MACOSX(/|$)|(^|/)\._[^/]*$' \
        || true
)"
if [[ -n "$DEV_ENTRIES" ]]; then
    echo "Playtest archive aborted: development files were found:" >&2
    echo "$DEV_ENTRIES" >&2
    exit 1
fi

/bin/mv -f -- "$TEMP_ARCHIVE" "$ARCHIVE_PATH"

ARCHIVE_SIZE="$(/usr/bin/du -h "$ARCHIVE_PATH" | /usr/bin/awk '{print $1}')"
echo "Created $ARCHIVE_PATH ($ARCHIVE_SIZE)"
