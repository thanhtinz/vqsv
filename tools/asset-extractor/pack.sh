#!/usr/bin/env bash
# Regenerate the baked-in client asset pack from a game JAR.
# The pack (base64-chunked zip) is committed to the repo so clients need NO JAR
# at build/run time — GameAssets unpacks it in memory on first use.
#
# Usage:  tools/asset-extractor/pack.sh <path-to-game.jar>
set -euo pipefail

JAR="${1:?usage: pack.sh <game.jar>}"
HERE="$(cd "$(dirname "$0")" && pwd)"
OUT="$HERE/out"
DEST="$HERE/../../clients/core/assets"

echo "[1/3] extracting assets from $JAR"
rm -rf "$OUT"
python3 "$HERE/extract.py" "$JAR" "$OUT" >/dev/null

echo "[2/3] zipping client asset set (png + json metadata)"
TMPZIP="$(mktemp -u).zip"
( cd "$OUT" && zip -qr -X "$TMPZIP" png spr map ui meta mod )

echo "[3/3] base64 + split into $DEST/game.pack.NNN.b64"
rm -f "$DEST"/game.pack.*.b64
base64 -w0 "$TMPZIP" > "$TMPZIP.b64"
( cd "$DEST" && split -b 460000 -d -a 3 "$TMPZIP.b64" game.pack. && for f in game.pack.[0-9]*; do mv "$f" "$f.b64"; done )
rm -f "$TMPZIP" "$TMPZIP.b64"
ls -la "$DEST"/game.pack.*.b64
echo "done — commit the updated game.pack.*.b64 files."
