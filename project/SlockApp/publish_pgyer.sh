#!/bin/bash
#
# publish_pgyer.sh — Upload APK to Pgyer (蒲公英) for distribution
#
# Usage:
#   ./publish_pgyer.sh [path/to/app.apk]
#
# If no APK path is given, defaults to app/build/outputs/apk/debug/app-debug.apk
#
# Environment:
#   PGYER_API_KEY — override the default API key
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DEFAULT_APK="$SCRIPT_DIR/app/build/outputs/apk/debug/app-debug.apk"

PGYER_API_KEY="${PGYER_API_KEY:-1ce4da8a779493a6e075007e15ab289b}"
APK_PATH="${1:-$DEFAULT_APK}"

if [ ! -f "$APK_PATH" ]; then
    echo "ERROR: APK not found at $APK_PATH"
    echo "Run './gradlew assembleDebug' first, or pass the APK path as an argument."
    exit 1
fi

APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
echo "=== Pgyer Publisher ==="
echo "APK: $APK_PATH ($APK_SIZE)"
echo ""

# --- Upload APK ---
echo "Uploading to Pgyer..."
RESPONSE=$(curl -s -X POST \
    "https://api.pgyer.com/apiv2/app/upload" \
    -F "_api_key=$PGYER_API_KEY" \
    -F "buildInstallType=1" \
    -F "file=@$APK_PATH")

CODE=$(echo "$RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin).get('code', -1))" 2>/dev/null || echo "-1")

if [ "$CODE" != "0" ]; then
    MSG=$(echo "$RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin).get('message','unknown error'))" 2>/dev/null || echo "unknown")
    echo "ERROR: Upload failed (code=$CODE): $MSG"
    echo "Response: $RESPONSE"
    exit 1
fi

# --- Parse result ---
BUILD_NAME=$(echo "$RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin)['data']; print(d.get('buildName',''))" 2>/dev/null)
BUILD_VERSION=$(echo "$RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin)['data']; print(d.get('buildVersion',''))" 2>/dev/null)
BUILD_VERSION_NO=$(echo "$RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin)['data']; print(d.get('buildVersionNo',''))" 2>/dev/null)
SHORTCUT_URL=$(echo "$RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin)['data']; print(d.get('buildShortcutUrl',''))" 2>/dev/null)
QR_URL=$(echo "$RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin)['data']; print(d.get('buildQRCodeURL',''))" 2>/dev/null)
BUILD_KEY=$(echo "$RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin)['data']; print(d.get('buildKey',''))" 2>/dev/null)

echo ""
echo "=== Published Successfully ==="
echo "App:       $BUILD_NAME"
echo "Version:   $BUILD_VERSION (build $BUILD_VERSION_NO)"
echo "Download:  https://www.pgyer.com/$SHORTCUT_URL"
echo "QR Code:   $QR_URL"
echo "Build Key: $BUILD_KEY"
echo "============================="
