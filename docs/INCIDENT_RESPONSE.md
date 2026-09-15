# خطة الاستجابة للحوادث الأمنية — Incident Response Plan

## المراحل الرئيسية للتعامل مع الحوادث

### 1. الكشف والتحديد (Identification)
- رصد تنبيهات Cloud Monitoring (`monitoring/alerts.yaml`).
- تقارير غير طبيعية في سجلات الخادم ومحاولات اختراق أو نشاط غير مصرح به.

### 2. الاحتواء والعزل (Containment)
- إبطال الجلسات المشبوهة فوراً:
  ```bash
  curl -X POST -H "Authorization: Bearer ${ADMIN_SECRET_KEY}" http://localhost:3000/api/admin/sessions/revoke-all
  ```
- حظر عناوين IP المسيئة عبر جدار الحماية (iptables أو Cloud Armor).

### 3. الاستئصال والمعالجة (Eradication)
- سد الثغرة المكتشفة وتدوير جميع الأسرار (`CSRF_SECRET`, `SESSION_SECRET`).
- مراجعة سلامة قاعدة البيانات (`PRAGMA integrity_check`).

### 4. التعافي واستعادة الخدمة (Recovery)
- استعادة البيانات النظيفة في حال حدوث تخريب (`scripts/restore-drill.sh` / `docs/RUNBOOK.md`).
- التحقق من عمل نقطة `/api/health`.

### 5. مراجعة ما بعد الحادث (Post-Incident Review)
- كتابة تقرير الحادث والدروس المستفادة، وتحديث الـ Runbook واختبارات الأمان.
