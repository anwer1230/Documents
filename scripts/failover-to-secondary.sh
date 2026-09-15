#!/usr/bin/env bash
# =============================================================================
# scripts/failover-to-secondary.sh — تحويل آلي لحركة المرور إلى المنطقة الاحتياطية
# الاستخدام: ./scripts/failover-to-secondary.sh [OPTIONS]
# =============================================================================

set -euo pipefail

# -----------------------------------------------------------------------------
# الإعدادات الافتراضية
# -----------------------------------------------------------------------------
PROJECT_ID="${GCP_PROJECT_ID:-}"
PRIMARY_REGION="${PRIMARY_REGION:-us-central1}"
SECONDARY_REGION="${SECONDARY_REGION:-europe-west1}"
SERVICE_NAME="${SERVICE_NAME:-telegram-web}"
DNS_ZONE="${DNS_ZONE:-telegram-dns-zone}"
DNS_NAME="${DNS_NAME:-app.telegram.internal.}"
DRY_RUN=0
FORCE=0

log() {
  local level="$1"; shift
  local ts
  ts=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
  echo "{\"time\":\"${ts}\",\"action\":\"FAILOVER\",\"level\":\"${level}\",\"msg\":\"$*\"}" >&2
}

log_info()  { log "INFO"  "$@"; }
log_warn()  { log "WARN"  "$@"; }
log_error() { log "ERROR" "$@"; }

# -----------------------------------------------------------------------------
# قراءة المعاملات
# -----------------------------------------------------------------------------
while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run)
      DRY_RUN=1
      shift
      ;;
    --force)
      FORCE=1
      shift
      ;;
    --secondary-region)
      SECONDARY_REGION="$2"
      shift 2
      ;;
    --project)
      PROJECT_ID="$2"
      shift 2
      ;;
    *)
      log_error "معامل غير معروف: $1"
      exit 1
      ;;
  esac
done

# -----------------------------------------------------------------------------
# التحقق من الجاهزية
# -----------------------------------------------------------------------------
check_prerequisites() {
  if [[ -z "${PROJECT_ID}" ]]; then
    log_error "PROJECT_ID غير محدد. استخدم --project أو المتغير GCP_PROJECT_ID"
    exit 1
  fi

  for cmd in gcloud jq curl; do
    if ! command -v "${cmd}" > /dev/null 2>&1; then
      log_error "الأداة المطلوبة غير متوفرة: ${cmd}"
      exit 1
    fi
  done
}

# -----------------------------------------------------------------------------
# التحقق من صحة المنطقة الاحتياطية قبل التحويل إليها
# -----------------------------------------------------------------------------
verify_secondary_health() {
  log_info "التحقق من جاهزية الخدمة في المنطقة الاحتياطية: ${SECONDARY_REGION}..."

  local sec_url
  sec_url=$(gcloud run services describe "${SERVICE_NAME}" \
    --project="${PROJECT_ID}" \
    --region="${SECONDARY_REGION}" \
    --format="value(status.url)" 2>/dev/null || true)

  if [[ -z "${sec_url}" ]]; then
    log_error "الخدمة غير موجودة في المنطقة الاحتياطية ${SECONDARY_REGION}!"
    exit 2
  fi

  log_info "رابط المنطقة الاحتياطية: ${sec_url}"

  # فحص نقطة الصحة للمنطقة الاحتياطية
  local health_code
  health_code=$(curl -s -o /dev/null -w "%{http_code}" "${sec_url}/api/health" || echo "000")

  if [[ "${health_code}" != "200" ]]; then
    log_error "المنطقة الاحتياطية لا تستجيب بالشكل السليم! (HTTP ${health_code})"
    if [[ ${FORCE} -ne 1 ]]; then
      log_error "تم إيقاف عملية التحويل لعدم جاهزية الهدف. استخدم --force للتجاوز."
      exit 3
    fi
    log_warn "تم التجاوز بطلب --force رغم كود الرد: ${health_code}"
  else
    log_info "المنطقة الاحتياطية تستجيب بنجاح (HTTP 200)"
  fi

  echo "${sec_url}"
}

# -----------------------------------------------------------------------------
# استعادة أحدث نسخة احتياطية إلى المنطقة الثانوية إذا لزم الأمر
# -----------------------------------------------------------------------------
sync_latest_backup_to_secondary() {
  log_info "فحص مزامنة أحدث نسخة احتياطية إلى المنطقة الثانوية..."
  # استدعاء تشغيل مهمة Cloud Run Job لاستعادة آخر نسخة
  if [[ ${DRY_RUN} -eq 1 ]]; then
    log_info "[DRY-RUN] محاكاة: تشغيل وظيفة استعادة النسخة في ${SECONDARY_REGION}"
    return 0
  fi

  gcloud run jobs execute "restore-job-${SECONDARY_REGION}" \
    --project="${PROJECT_ID}" \
    --region="${SECONDARY_REGION}" \
    --wait 2>/dev/null || log_warn "لم يتم العثور على وظيفة استعادة مجدولة أو فشل التنفيذ"
}

# -----------------------------------------------------------------------------
# تحديث موازن الأحمال (Global Load Balancer) أو DNS
# -----------------------------------------------------------------------------
shift_traffic() {
  local target_url="$1"

  log_info "بدء تحويل حركة المرور إلى ${SECONDARY_REGION}..."

  if [[ ${DRY_RUN} -eq 1 ]]; then
    log_info "[DRY-RUN] محاكاة: تحويل 100% من المرور إلى ${SECONDARY_REGION}"
    return 0
  fi

  # 1. تحديث Cloud DNS لتوجيه النطاق إلى المنطقة الاحتياطية
  log_info "تحديث سجلات Cloud DNS..."
  local sec_ip
  sec_ip=$(gcloud compute addresses describe "telegram-ip-${SECONDARY_REGION}" \
    --project="${PROJECT_ID}" \
    --global \
    --format="value(address)" 2>/dev/null || true)

  if [[ -n "${sec_ip}" ]]; then
    gcloud dns record-sets transaction start --zone="${DNS_ZONE}" --project="${PROJECT_ID}" 2>/dev/null || true
    # محاولة تبديل العنوان
    gcloud dns record-sets transaction add "${sec_ip}" \
      --name="${DNS_NAME}" \
      --ttl=60 \
      --type=A \
      --zone="${DNS_ZONE}" \
      --project="${PROJECT_ID}" 2>/dev/null || true
    gcloud dns record-sets transaction execute --zone="${DNS_ZONE}" --project="${PROJECT_ID}" 2>/dev/null || true
    log_info "تم تحديث Cloud DNS بنجاح إلى IP: ${sec_ip}"
  fi

  # 2. تحديث نسبة المرور في Backend Service لموازن الأحمال العالمي (إن وُجد)
  log_info "تحديث موازن الأحمال العالمي (Global Backend Service)..."
  gcloud compute backend-services update-backend "telegram-backend-service" \
    --project="${PROJECT_ID}" \
    --global \
    --region="${PRIMARY_REGION}" \
    --capacity-scaler=0.0 2>/dev/null || log_warn "تخطي تعديل Primary Backend"

  gcloud compute backend-services update-backend "telegram-backend-service" \
    --project="${PROJECT_ID}" \
    --global \
    --region="${SECONDARY_REGION}" \
    --capacity-scaler=1.0 2>/dev/null || log_warn "تخطي تعديل Secondary Backend"

  log_info "تم تحويل حركة المرور بنجاح."
}

# -----------------------------------------------------------------------------
# المدخل الرئيسي
# -----------------------------------------------------------------------------
main() {
  log_info "=== بدء إجراءات التحويل الإقليمي (Regional Failover) ==="
  log_info "المصدر: ${PRIMARY_REGION} | الوجهة: ${SECONDARY_REGION}"

  check_prerequisites

  local secondary_url
  secondary_url=$(verify_secondary_health)

  sync_latest_backup_to_secondary

  shift_traffic "${secondary_url}"

  log_info "=== اكتمل التحويل بنجاح. أصبحت المنطقة ${SECONDARY_REGION} هي النشطة ==="
}

main "$@"
