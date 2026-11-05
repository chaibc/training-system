#!/usr/bin/env bash
# 将新 jar 发布到 /data/hanmo/app 并重启
# 用法：
#   scp target/hanmo-server.jar root@服务器:/tmp/hanmo-server.jar
#   ssh root@服务器 'bash /data/hanmo/scripts/deploy-jar.sh /tmp/hanmo-server.jar'
set -Eeuo pipefail

NEW_JAR="${1:-/tmp/hanmo-server.jar}"
APP_DIR="/data/hanmo/app"
TARGET="$APP_DIR/hanmo-server.jar"
BACKUP_DIR="/data/hanmo/backups/releases"

if [[ ! -f "$NEW_JAR" ]]; then
  echo "[ERROR] jar not found: $NEW_JAR" >&2
  exit 1
fi

mkdir -p "$APP_DIR" "$BACKUP_DIR"
if [[ -f "$TARGET" ]]; then
  cp -a "$TARGET" "$BACKUP_DIR/hanmo-server-$(date +%Y%m%d-%H%M%S).jar"
fi

install -o hanmo -g hanmo -m 640 "$NEW_JAR" "$TARGET"
systemctl restart hanmo-server
sleep 3
systemctl --no-pager --full status hanmo-server | sed -n '1,15p'

echo
echo "recent log:"
tail -n 30 /data/hanmo/logs/hanmo-server.log 2>/dev/null || true
