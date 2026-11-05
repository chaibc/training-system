#!/usr/bin/env bash
# 翰墨学堂备份脚本：MySQL + 上传文件
# 安装：
#   chmod 750 /data/hanmo/scripts/backup.sh
# 定时任务（每天 03:20）：
#   crontab -u hanmo -e
#   20 3 * * * /data/hanmo/scripts/backup.sh >> /data/hanmo/logs/backup.log 2>&1
set -Eeuo pipefail

ENV_FILE="${ENV_FILE:-/etc/hanmo/hanmo.env}"
BACKUP_DIR="${BACKUP_DIR:-/data/hanmo/backups}"
UPLOAD_DIR_DEFAULT="/data/hanmo/uploads"
RETENTION_DAYS="${RETENTION_DAYS:-14}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "[ERROR] env file not found: $ENV_FILE" >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

UPLOAD_DIR="${UPLOAD_DIR:-$UPLOAD_DIR_DEFAULT}"
STAMP="$(date +%Y%m%d-%H%M%S)"
mkdir -p "$BACKUP_DIR"

DB_FILE="$BACKUP_DIR/hanmo-db-$STAMP.sql.gz"
FILES_FILE="$BACKUP_DIR/hanmo-uploads-$STAMP.tar.gz"

echo "[$(date '+%F %T')] start backup"
mysqldump \
  --host="${DB_HOST:-127.0.0.1}" \
  --port="${DB_PORT:-3306}" \
  --user="${DB_USER}" \
  --password="${DB_PASSWORD}" \
  --single-transaction \
  --routines \
  --triggers \
  --default-character-set=utf8mb4 \
  --no-tablespaces \
  "${DB_NAME}" | gzip -9 > "$DB_FILE"

tar -C "$(dirname "$UPLOAD_DIR")" -czf "$FILES_FILE" "$(basename "$UPLOAD_DIR")"

chmod 600 "$DB_FILE" "$FILES_FILE"
echo "[$(date '+%F %T')] db: $DB_FILE ($(du -h "$DB_FILE" | cut -f1))"
echo "[$(date '+%F %T')] uploads: $FILES_FILE ($(du -h "$FILES_FILE" | cut -f1))"

find "$BACKUP_DIR" -type f \( -name 'hanmo-db-*.sql.gz' -o -name 'hanmo-uploads-*.tar.gz' \) \
  -mtime +"$RETENTION_DAYS" -print -delete

echo "[$(date '+%F %T')] done"
