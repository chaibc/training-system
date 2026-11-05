#!/usr/bin/env bash
# 翰墨学堂服务健康检查
set -euo pipefail

HEALTH_URL="${HEALTH_URL:-http://127.0.0.1:8080/actuator/health}"
DOMAIN="${DOMAIN:-}"

echo "== systemd =="
systemctl --no-pager --full status hanmo-server | sed -n '1,20p' || true

echo
echo "== backend health =="
curl -fsS --max-time 5 "$HEALTH_URL" || echo "backend health failed"
echo

if [[ -n "$DOMAIN" ]]; then
  echo
  echo "== public https =="
  curl -fsS -o /dev/null -w "https status: %{http_code}\n" "https://$DOMAIN/" || true
  curl -fsS -o /dev/null -w "api status: %{http_code}\n" "https://$DOMAIN/api/public/home" || true
fi

echo
echo "== disk / memory =="
df -h / /data 2>/dev/null || df -h /
free -h

echo
echo "== recent errors =="
journalctl -u hanmo-server -n 30 --no-pager || true
tail -n 30 /data/hanmo/logs/hanmo-server.log 2>/dev/null || true
