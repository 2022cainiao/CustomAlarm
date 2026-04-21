#!/usr/bin/env bash

set -euo pipefail

APP_NAME="custom_alarm_preview"
PORT="${PREVIEW_WEB_PORT:-8091}"
DEPLOY_ROOT="${PREVIEW_WEB_DEPLOY_ROOT:-$HOME/custom_alarm_preview}"
PID_FILE="$DEPLOY_ROOT/server.pid"
LOG_FILE="$DEPLOY_ROOT/server.log"

mkdir -p "$DEPLOY_ROOT"
rm -rf "$DEPLOY_ROOT/site"
mkdir -p "$DEPLOY_ROOT/site"

cp -R preview-web/. "$DEPLOY_ROOT/site/"

if [ -f "$PID_FILE" ]; then
  OLD_PID="$(cat "$PID_FILE" || true)"
  if [ -n "${OLD_PID:-}" ] && kill -0 "$OLD_PID" 2>/dev/null; then
    kill "$OLD_PID" 2>/dev/null || true
    sleep 1
  fi
  rm -f "$PID_FILE"
fi

if command -v lsof >/dev/null 2>&1; then
  OCCUPIED_PID="$(lsof -ti tcp:"$PORT" || true)"
  if [ -n "${OCCUPIED_PID:-}" ]; then
    echo "[ERROR] port $PORT is already in use by PID $OCCUPIED_PID"
    exit 1
  fi
fi

cd "$DEPLOY_ROOT/site"
nohup python3 -m http.server "$PORT" --bind 0.0.0.0 >"$LOG_FILE" 2>&1 &
NEW_PID="$!"
echo "$NEW_PID" > "$PID_FILE"

sleep 2

if ! kill -0 "$NEW_PID" 2>/dev/null; then
  echo "[ERROR] preview web server failed to start"
  test -f "$LOG_FILE" && tail -n 50 "$LOG_FILE"
  exit 1
fi

echo "APP_NAME=$APP_NAME"
echo "DEPLOY_ROOT=$DEPLOY_ROOT"
echo "PORT=$PORT"
echo "PID=$NEW_PID"
