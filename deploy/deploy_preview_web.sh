#!/usr/bin/env bash

set -euo pipefail

APP_NAME="custom_alarm_preview"
PORT="${PREVIEW_WEB_PORT:-8091}"
HOST="${PREVIEW_WEB_HOST:-127.0.0.1}"
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
    if kill -0 "$OLD_PID" 2>/dev/null; then
      kill -9 "$OLD_PID" 2>/dev/null || true
    fi
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
if ! command -v python3 >/dev/null 2>&1; then
  echo "[ERROR] python3 not found"
  exit 1
fi

: > "$LOG_FILE"

setsid env -u RUNNER_TRACKING_ID nohup python3 -m http.server "$PORT" --bind "$HOST" \
  >"$LOG_FILE" 2>&1 < /dev/null &
NEW_PID="$!"
echo "$NEW_PID" > "$PID_FILE"

sleep 2

if ! kill -0 "$NEW_PID" 2>/dev/null; then
  echo "[ERROR] preview web server failed to start"
  test -f "$LOG_FILE" && tail -n 50 "$LOG_FILE"
  exit 1
fi

if command -v curl >/dev/null 2>&1; then
  if ! curl -fsS "http://$HOST:$PORT/" >/dev/null; then
    echo "[ERROR] preview web health check failed"
    test -f "$LOG_FILE" && tail -n 50 "$LOG_FILE"
    exit 1
  fi
fi

echo "APP_NAME=$APP_NAME"
echo "DEPLOY_ROOT=$DEPLOY_ROOT"
echo "HOST=$HOST"
echo "PORT=$PORT"
echo "PID=$NEW_PID"
