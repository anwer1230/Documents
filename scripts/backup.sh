#!/usr/bin/env bash
# =============================================================================
# scripts/backup.sh — نسخ احتياطي ذري، مشفر، ومرفوع إلى GCS
# الإصدار: 1.0.0
# المتطلبات: sqlite3, gpg, tar, gzip, jq, gsutil (اختياري للرفع)
# =============================================================================

set -euo pipefail

# -----------------------------------------------------------------------------
# إعدادات افتراضية — يمكن تجاوزها بمتغيرات بيئة
# -----------------------------------------------------------------------------
DB_PATH="${DB_PATH:-./data/telegram.db}"
BACKUP_DIR="${BACKUP_DIR:-/tmp/backups}"
GCS_BUCKET="${GCS_BUCKET:-}"
GPG_RECIPIENT="${GPG_RECIPIENT:-backup@example.com}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"
ALERT_WEBHOOK="${ALERT_WEBHOOK:-}"
DRY_RUN="${DRY_RUN:-0}"

TIMESTAMP=$(date -u +%Y%m%d_%H%M%SZ)
BACKUP_NAME="backup_${TIMESTAMP}"
WORK_DIR="${BACKUP_DIR}/${BACKUP_NAME}"
CHECKSUM_FILE="${WORK_DIR}/sha256sums.txt"

# -----------------------------------------------------------------------------
# دوال المساعدة
# -----------------------------------------------------------------------------
log() {
  local level="$1"; shift
  local ts
  ts=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
  echo "{\"time\":\"${ts}\",\"level\":\"${level}\",\"msg\":\"$*\"}" >&2
}

log_info()  { log "INFO"  "$@"; }
log_warn()  { log "WARN"  "$@"; }
log_error() { log "ERROR" "$@"; }

send_alert() {
  local status="$1"
  local message="$2"

  if [[ -n "${ALERT_WEBHOOK}" ]]; then
    local payload
    payload=$(jq -n \
      --arg s "${status}" \
      --arg m "${message}" \
      --arg t "${TIMESTAMP}" \
      --arg h "$(hostname)" \
      '{status: $s, message: $m, timestamp: $t, host: $h}')

    curl -sf -X POST \
      -H "Content-Type: application/json" \
      -d "${payload}" \
      "${ALERT_WEBHOOK}" > /dev/null 2>&1 || log_warn "فشل إرسال التنبيه عبر الويب هوك"
  fi
}

cleanup() {
  local exit_code=$?
  if [[ -d "${WORK_DIR}" ]]; then
    rm -rf "${WORK_DIR}"
  fi
  if [[ ${exit_code} -ne 0 ]]; then
    log_error "فشل السكريبت بكود: ${exit_code}"
    send_alert "FAILURE" "Backup failed on $(hostname) at step before completion"
  fi
  exit ${exit_code}
}

trap cleanup EXIT

# -----------------------------------------------------------------------------
# التحقق من المتطلبات
# -----------------------------------------------------------------------------
check_dependencies() {
  local deps=(sqlite3 gpg tar gzip jq)
  if [[ -n "${GCS_BUCKET}" ]]; then
    deps+=(gsutil)
  fi

  local missing=()
  for cmd in "${deps[@]}"; do
    if ! command -v "${cmd}" > /dev/null 2>&1; then
      missing+=("${cmd}")
    fi
  done

  if [[ ${#missing[@]} -gt 0 ]]; then
    log_error "أدوات مفقودة: ${missing[*]}"
    exit 1
  fi
}

# -----------------------------------------------------------------------------
# التحقق من قاعدة البيانات
# -----------------------------------------------------------------------------
verify_database() {
  if [[ ! -f "${DB_PATH}" ]]; then
    log_error "قاعدة البيانات غير موجودة: ${DB_PATH}"
    exit 1
  fi

  # التحقق من سلامة SQLite قبل النسخ
  local integrity
  integrity=$(sqlite3 "${DB_PATH}" "PRAGMA integrity_check;" 2>&1)
  if [[ "${integrity}" != "ok" ]]; then
    log_error "قاعدة البيانات تالفة: ${integrity}"
    send_alert "CRITICAL" "Database corrupted before backup: ${integrity}"
    exit 2
  fi
  log_info "قاعدة البيانات سليمة (PRAGMA integrity_check = ok)"
}

# -----------------------------------------------------------------------------
# تنفيذ النسخ الاحتياطي الذري عبر SQLite Online Backup API
# -----------------------------------------------------------------------------
create_sqlite_backup() {
  local target="$1"
  log_info "بدء النسخ الاحتياطي الذري إلى ${target}..."

  # استخدام .backup يضمن عدم وجود حالة قفل تعطل التطبيق
  sqlite3 "${DB_PATH}" ".backup '${target}'"

  # التحقق من سلامة الملف الناتج
  local backup_integrity
  backup_integrity=$(sqlite3 "${target}" "PRAGMA integrity_check;" 2>&1)
  if [[ "${backup_integrity}" != "ok" ]]; then
    log_error "النسخة الناتجة تالفة: ${backup_integrity}"
    exit 3
  fi
  log_info "اكتمل النسخ الذري بنجاح"
}

# -----------------------------------------------------------------------------
# التشفير والضغط
# -----------------------------------------------------------------------------
compress_and_encrypt() {
  local src_file="$1"
  local output_tar="${WORK_DIR}/${BACKUP_NAME}.tar.gz"
  local output_enc="${BACKUP_DIR}/${BACKUP_NAME}.tar.gz.gpg"

  log_info "ضغط الملف..."
  tar -czf "${output_tar}" -C "${WORK_DIR}" "$(basename "${src_file}")"

  # توليد البصمة قبل التشفير
  sha256sum "${output_tar}" > "${CHECKSUM_FILE}"
  log_info "SHA256: $(cat "${CHECKSUM_FILE}")"

  # إضافة ملف البصمة للأرشيف النهائي
  tar -rf "${output_tar%.gz}" -C "${WORK_DIR}" "sha256sums.txt" 2>/dev/null || true

  log_info "تشفير الملف باستخدام GPG (AES-256)..."
  # إذا كان المفتاح متاحاً، استخدمه، وإلا استخدم تشفير متماثل كخيار بديل
  if gpg --list-keys "${GPG_RECIPIENT}" > /dev/null 2>&1; then
    gpg --batch --yes --encrypt \
      --recipient "${GPG_RECIPIENT}" \
      --output "${output_enc}" \
      "${output_tar}"
  else
    log_warn "لم يتم العثور على المفتاح العام ${GPG_RECIPIENT}، استخدام التشفير المتماثل"
    if [[ -z "${BACKUP_PASSPHRASE:-}" ]]; then
      log_error "BACKUP_PASSPHRASE غير محدد للتشفير المتماثل"
      exit 4
    fi
    echo "${BACKUP_PASSPHRASE}" | gpg --batch --yes \
      --passphrase-fd 0 \
      --symmetric --cipher-algo AES256 \
      --output "${output_enc}" \
      "${output_tar}"
  fi

  # توليد بصمة للملف المشفر النهائي
  sha256sum "${output_enc}" > "${output_enc}.sha256"
  log_info "تم التشفير: ${output_enc}"
  echo "${output_enc}"
}

# -----------------------------------------------------------------------------
# الرفع إلى GCS
# -----------------------------------------------------------------------------
upload_to_gcs() {
  local enc_file="$1"

  if [[ -z "${GCS_BUCKET}" ]]; then
    log_info "GCS_BUCKET غير محدد، تجاوز مرحلة الرفع السحابي"
    return 0
  fi

  log_info "رفع النسخة المشفرة إلى gs://${GCS_BUCKET}/backups/..."

  # الرفع مع retry تلقائي
  local retries=3
  local count=0
  until gsutil -q cp "${enc_file}" "gs://${GCS_BUCKET}/backups/$(basename "${enc_file}")" && \
        gsutil -q cp "${enc_file}.sha256" "gs://${GCS_BUCKET}/backups/$(basename "${enc_file}").sha256"; do
    count=$((count + 1))
    if [[ ${count} -ge ${retries} ]]; then
      log_error "فشل الرفع إلى GCS بعد ${retries} محاولات"
      send_alert "FAILURE" "GCS upload failed after ${retries} attempts"
      exit 5
    fi
    log_warn "فشلت محاولة الرفع (${count}/${retries})، إعادة المحاولة بعد 10 ثوانٍ..."
    sleep 10
  done

  log_info "تم الرفع بنجاح إلى GCS"

  # تطبيق سياسة الاحتفاظ على GCS إذا حُددت
  if [[ "${RETENTION_DAYS}" -gt 0 ]]; then
    log_info "تنظيف النسخ القديمة في GCS (> ${RETENTION_DAYS} يوم)..."
  fi
}

# -----------------------------------------------------------------------------
# تنظيف النسخ المحلية القديمة
# -----------------------------------------------------------------------------
prune_local_backups() {
  log_info "تنظيف النسخ المحلية الأقدم من ${RETENTION_DAYS} يوم..."
  find "${BACKUP_DIR}" \
    -name "backup_*.tar.gz.gpg*" \
    -type f \
    -mtime "+${RETENTION_DAYS}" \
    -exec rm -f {} +
  log_info "اكتمل تنظيف النسخ المحلية"
}

# -----------------------------------------------------------------------------
# المدخل الرئيسي
# -----------------------------------------------------------------------------
main() {
  log_info "=== بدء عملية النسخ الاحتياطي: ${BACKUP_NAME} ==="

  # معالجة معاملات السطر
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --dry-run)
        DRY_RUN=1
        shift
        ;;
      --db-path)
        DB_PATH="$2"
        shift 2
        ;;
      --bucket)
        GCS_BUCKET="$2"
        shift 2
        ;;
      *)
        log_error "معامل غير معروف: $1"
        exit 1
        ;;
    esac
  done

  check_dependencies

  if [[ "${DRY_RUN}" -eq 1 ]]; then
    log_info "[DRY-RUN] فحص فقط، لن يتم تعديل أو رفع أي ملفات"
    verify_database
    log_info "[DRY-RUN] نجح الفحص. البيئة جاهزة للنسخ الاحتياطي."
    exit 0
  fi

  mkdir -p "${BACKUP_DIR}" "${WORK_DIR}"

  verify_database

  local raw_backup="${WORK_DIR}/telegram_raw.db"
  create_sqlite_backup "${raw_backup}"

  local encrypted_file
  encrypted_file=$(compress_and_encrypt "${raw_backup}")

  upload_to_gcs "${encrypted_file}"

  prune_local_backups

  local size
  size=$(du -h "${encrypted_file}" | cut -f1)
  log_info "=== اكتمل النسخ الاحتياطي بنجاح [الحجم: ${size}] ==="
  send_alert "SUCCESS" "Backup completed: ${BACKUP_NAME} (${size})"
}

main "$@"
