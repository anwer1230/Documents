#!/usr/bin/env bash
# =============================================================================
# scripts/deploy-alerts.sh — نشر سياسات التنبيه إلى Google Cloud Monitoring
# الاستخدام: ./scripts/deploy-alerts.sh [PROJECT_ID] [NOTIFICATION_CHANNEL_ID]
# =============================================================================

set -euo pipefail

PROJECT_ID="${1:-${GCP_PROJECT_ID:-}}"
CHANNEL_ID="${2:-${ALERT_NOTIFICATION_CHANNEL:-}}"
ALERTS_FILE="$(dirname "$0")/../monitoring/alerts.yaml"

if [[ -z "${PROJECT_ID}" ]]; then
  echo "خطأ: يجب تحديد PROJECT_ID كمعامل أول أو في المتغير GCP_PROJECT_ID" >&2
  exit 1
fi

if [[ ! -f "${ALERTS_FILE}" ]]; then
  echo "خطأ: ملف التنبيهات غير موجود: ${ALERTS_FILE}" >&2
  exit 1
fi

echo "=== نشر سياسات التنبيه إلى المشروع: ${PROJECT_ID} ==="

# التحقق من gcloud
if ! command -v gcloud > /dev/null 2>&1; then
  echo "خطأ: gcloud CLI غير مثبت" >&2
  exit 1
fi

# تثبيت/تحديث كل سياسة
gcloud alpha monitoring policies create \
  --project="${PROJECT_ID}" \
  --policy-from-file="${ALERTS_FILE}" \
  ${CHANNEL_ID:+--notification-channels="${CHANNEL_ID}"}

echo "=== تم نشر التنبيهات بنجاح ==="
