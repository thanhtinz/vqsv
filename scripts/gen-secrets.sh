#!/usr/bin/env bash
# Generate strong secrets into .env (in place). Idempotent only for the
# CHANGE_ME placeholders — existing real values are left untouched.
set -euo pipefail

cd "$(dirname "$0")/.."

if [ ! -f .env ]; then
  echo "No .env found. Run: cp .env.example .env" >&2
  exit 1
fi

gen() { openssl rand -base64 "${1:-32}" | tr -d '\n/+=' | head -c "${2:-40}"; }

replace() {
  local key="$1" val="$2"
  if grep -q "^${key}=CHANGE_ME" .env; then
    # Escape & and / for sed replacement.
    local esc; esc=$(printf '%s' "$val" | sed -e 's/[&/\]/\\&/g')
    sed -i "s/^${key}=.*/${key}=${esc}/" .env
    echo "  set ${key}"
  else
    echo "  skip ${key} (already set)"
  fi
}

echo "Generating secrets into .env ..."
replace POSTGRES_PASSWORD "$(gen 32 40)"
replace REDIS_PASSWORD    "$(gen 32 40)"
replace JWT_SECRET        "$(openssl rand -base64 48 | tr -d '\n')"
echo "Done. Remember to set PUBLIC_DOMAIN and ACME_EMAIL in .env."
