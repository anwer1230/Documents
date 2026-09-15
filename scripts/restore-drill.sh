#!/usr/bin/env bash
# =============================================================================
# scripts/restore-drill.sh — اختبار محاكاة الاستعادة الآلي (Automated Restore Drill)
# يهدف إلى التحقق من قابلية قراءة واستعادة النسخ الاحتياطية دورياً
# دون المساس بالبيئة الحية
# =============================================================================

set -euo pipefail

# -----------------------------------------------------------------------------
# إعدادات
# -----------------------------------------------------------------------------
GCS_BUCKET="${GCS_BUCKET:-}"
DRILL_DIR="${DRILL_DIR:-/tmp/restore-drill-$(date +%Y%m%d_%H%M%S)}"
BACKUP_PASSPHRASE="${BACKUP_PASSPHRASE:-}"
ALERT_WEBHOOK="${ALERT_WEBHOOK:-}"
REPORT_FILE="${DRILL_DIR}/drill-report.json"

log() {
  local level="$1"; shift
  local ts
  ts=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
  echo "{\"time\":\"${ts}\",\"drill\":\"RESTORE\",\"level\":\"${level}\",\"msg\":\"$*\"}" >&2
}

log_info()  { log "INFO"  "$@"; }
log_warn()  { log "WARN"  "$@"; }
log_error() { log "ERROR" "$@"; }

# تنظيف شامل عند الخروج
cleanup() {
  local exit_code=$?
  if [[ -d "${DRILL_DIR}" ]]; then
    # حذف البيانات المستعادة المؤقتة فوراً لحماية الخصوصية
    rm -rf "${DRILL_DIR}"
  fi
  exit ${exit_code}
}

trap cleanup EXIT

# -----------------------------------------------------------------------------
# خطوات التمرين
# -----------------------------------------------------------------------------
main() {
  log_info "=== بدء تمرين محاكاة الاستعادة (Restore Drill) ==="
  local start_time
  start_time=$(date +%s)

  mkdir -p "${DRILL_DIR}"

  # 1. تحديد النسخة المستهدفة
  local target_backup=""
  local target_sha=""

  if [[ -n "${GCS_BUCKET}" ]]; then
    log_info "البحث عن أحدث نسخة في GCS: gs://${GCS_BUCKET}/backups/..."
    target_backup=$(gsutil ls "gs://${GCS_BUCKET}/backups/*.gpg" 2>/dev/null | tail -n 1 || true)
    if [[ -z "${target_backup}" ]]; then
      log_error "لم يتم العثور على أي نسخ في GCS!"
      exit 1
    fi
    log_info "أحدث نسخة: ${target_backup}"

    # تنزيل النسخة والبصمة
    gsutil -q cp "${target_backup}" "${DRILL_DIR}/backup.tar.gz.gpg"
    gsutil -q cp "${target_backup}.sha256" "${DRILL_DIR}/backup.tar.gz.gpg.sha256" || true
  else
    # البحث محلياً إذا لم يكن GCS متاحاً
    log_info "GCS غير محدد، البحث في النسخ المحلية /tmp/backups/..."
    local local_backup
    local_backup=$(find /tmp/backups -name "backup_*.tar.gz.gpg" -type f 2>/dev/null | sort | tail -n 1 || true)
    if [[ -z "${local_backup}" ]]; then
      log_error "لم يتم العثور على أي نسخ محلية للاختبار!"
      exit 1
    fi
    cp "${local_backup}" "${DRILL_DIR}/backup.tar.gz.gpg"
  fi

  # 2. فحص بصمة الملف المشفر
  if [[ -f "${DRILL_DIR}/backup.tar.gz.gpg.sha256" ]]; then
    log_info "فحص بصمة الملف المشفر..."
    cd "${DRILL_DIR}"
    local expected_hash
    expected_hash=$(awk '{print $1}' backup.tar.gz.gpg.sha256)
    local actual_hash
    actual_hash=$(sha256sum backup.tar.gz.gpg | awk '{print $1}')
    if [[ "${expected_hash}" != "${actual_hash}" ]]; then
      log_error "عدم تطابق البصمة الخارجية! متوقع: ${expected_hash}، وجد: ${actual_hash}"
      exit 2
    fi
    log_info "تطابق البصمة الخارجية: بنجاح"
  fi

  # 3. فك التشفير
  log_info "فك تشفير النسخة في بيئة معزولة..."
  if [[ -n "${BACKUP_PASSPHRASE}" ]]; then
    echo "${BACKUP_PASSPHRASE}" | gpg --batch --yes \
      --passphrase-fd 0 \
      --decrypt "${DRILL_DIR}/backup.tar.gz.gpg" > "${DRILL_DIR}/backup.tar.gz" 2>/dev/null
  else
    gpg --batch --yes \
      --decrypt "${DRILL_DIR}/backup.tar.gz.gpg" > "${DRILL_DIR}/backup.tar.gz" 2>/dev/null || {
        log_error "فشل فك التشفير: لا يوجد مفتاح ولا كلمة مرور محددة"
        exit 3
      }
  fi
  log_info "تم فك التشفير بنجاح"

  # 4. الاستخراج وفحص البصمة الداخلية
  mkdir -p "${DRILL_DIR}/extracted"
  tar -xzf "${DRILL_DIR}/backup.tar.gz" -C "${DRILL_DIR}/extracted"

  if [[ -f "${DRILL_DIR}/extracted/sha256sums.txt" ]]; then
    log_info "التحقق من البصمات الداخلية..."
    cd "${DRILL_DIR}/extracted"
    sha256sum -c sha256sums.txt --status || {
      log_error "فشل تطابق البصمة الداخلية للملفات المستخرجة!"
      exit 4
    }
    log_info "تطابق البصمات الداخلية: بنجاح"
  fi

  # 5. اختبار قاعدة البيانات المستعادة
  local restored_db
  restored_db=$(find "${DRILL_DIR}/extracted" -name "*.db" | head -n 1)

  if [[ -z "${restored_db}" || ! -f "${restored_db}" ]]; then
    log_error "لم يتم العثور على ملف قاعدة بيانات داخل الأرشيف!"
    exit 5
  fi

  log_info "تشغيل PRAGMA integrity_check على النسخة المستعادة..."
  local integrity
  integrity=$(sqlite3 "${restored_db}" "PRAGMA integrity_check;" 2>&1)
  if [[ "${integrity}" != "ok" ]]; then
    log_error "فحص السلامة الهيكلية فشل: ${integrity}"
    exit 6
  fi
  log_info "سلامة الهيكل: ok"

  # 6. اختبار استعلامات القراءة الأساسية (Smoke Queries)
  log_info "تشغيل استعلامات التحقق من البيانات..."
  local table_count
  table_count=$(sqlite3 "${restored_db}" "SELECT count(*) FROM sqlite_master WHERE type='table';" 2>&1)
  log_info "عدد الجداول المستعادة: ${table_count}"

  if [[ "${table_count}" -eq 0 ]]; then
    log_error "قاعدة البيانات المستعادة فارغة لا تحتوي على جداول!"
    exit 7
  fi

  # 7. حساب زمن التمرين وتسجيل النتيجة
  local end_time
  end_time=$(date +%s)
  local duration=$((end_time - start_time))

  log_info "=== اكتمل تمرين الاستعادة بنجاح في ${duration} ثانية ==="

  # إرسال تقرير نجاح إذا كان الويب هوك معرّفاً
  if [[ -n "${ALERT_WEBHOOK}" ]]; then
    local payload
    payload=$(jq -n \
      --arg status "DRILL_SUCCESS" \
      --arg dur "${duration}" \
      --arg tables "${table_count}" \
      --arg ts "$(date -u +"%Y-%m-%dT%H:%M:%SZ")" \
      '{event: $status, duration_seconds: $dur, tables_verified: $tables, timestamp: $ts}')

    curl -sf -X POST \
      -H "Content-Type: application/json" \
      -d "${payload}" \
      "${ALERT_WEBHOOK}" > /dev/null 2>&1 || true
  fi
}

main "$@"
