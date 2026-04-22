#!/usr/bin/env bash

set -euo pipefail

PUBLIC_PORT="${PREVIEW_PUBLIC_PORT:-8090}"
UPSTREAM_HOST="${PREVIEW_WEB_HOST:-127.0.0.1}"
UPSTREAM_PORT="${PREVIEW_WEB_PORT:-8091}"
NGINX_CONF_DIR="${PREVIEW_NGINX_CONF_DIR:-/etc/nginx/conf.d}"
NGINX_CONF_NAME="${PREVIEW_NGINX_CONF_NAME:-custom_alarm_preview.conf}"
TARGET_CONF="$NGINX_CONF_DIR/$NGINX_CONF_NAME"

set_output() {
  if [ -n "${GITHUB_OUTPUT:-}" ]; then
    echo "$1=$2" >> "$GITHUB_OUTPUT"
  fi
}

if ! command -v nginx >/dev/null 2>&1; then
  echo "[WARN] nginx not found, skipping preview proxy setup"
  set_output "nginx_available" "false"
  set_output "public_port" "$PUBLIC_PORT"
  set_output "upstream" "$UPSTREAM_HOST:$UPSTREAM_PORT"
  exit 0
fi

if ! command -v sudo >/dev/null 2>&1; then
  echo "[ERROR] sudo not found"
  exit 1
fi

TMP_CONF="$(mktemp)"
sed \
  -e "s/__PUBLIC_PORT__/$PUBLIC_PORT/g" \
  -e "s/__UPSTREAM_HOST__/$UPSTREAM_HOST/g" \
  -e "s/__UPSTREAM_PORT__/$UPSTREAM_PORT/g" \
  deploy/nginx_preview_web.conf > "$TMP_CONF"

sudo mkdir -p "$NGINX_CONF_DIR"
sudo cp "$TMP_CONF" "$TARGET_CONF"
rm -f "$TMP_CONF"

sudo nginx -t
sudo systemctl reload nginx

set_output "nginx_available" "true"
set_output "public_port" "$PUBLIC_PORT"
set_output "upstream" "$UPSTREAM_HOST:$UPSTREAM_PORT"

echo "NGINX_CONF=$TARGET_CONF"
echo "PUBLIC_PORT=$PUBLIC_PORT"
echo "UPSTREAM=$UPSTREAM_HOST:$UPSTREAM_PORT"
