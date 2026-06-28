#!/usr/bin/env bash
# Daily PostgreSQL backup for the VQSV stack (run via cron).
# Example crontab (keep 14 days):
#   0 3 * * *  /opt/vqsv/deploy/backup-db.sh >> /var/log/vqsv-backup.log 2>&1
set -euo pipefail

cd "$(dirname "$0")/.."
[ -f .env ] && set -a && . ./.env && set +a

BACKUP_DIR="${BACKUP_DIR:-/var/backups/vqsv}"
KEEP_DAYS="${KEEP_DAYS:-14}"
STAMP="$(date +%Y%m%d-%H%M%S)"
mkdir -p "$BACKUP_DIR"

OUT="$BACKUP_DIR/vqsv-${STAMP}.sql.gz"
echo "[$(date)] dumping to $OUT"
docker compose -f docker-compose.prod.yml exec -T db \
  pg_dump -U "${POSTGRES_USER}" "${POSTGRES_DB}" | gzip > "$OUT"

# Prune old backups.
find "$BACKUP_DIR" -name 'vqsv-*.sql.gz' -mtime "+${KEEP_DAYS}" -delete
echo "[$(date)] done. current backups:"
ls -lh "$BACKUP_DIR" | tail -5
