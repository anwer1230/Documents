# دليل العمليات والصيانة — Runbook

> **النظام:** Telegram Web Client  
> **الإصدار:** 1.0.0  
> **البيئة:** Production / Staging  
> **آخر تحديث:** 2026-09  

---

## جدول المحتويات

1. [العمليات اليومية والدورية](#1-العمليات-اليومية-والدورية)
2. [استعادة النسخ الاحتياطي](#2-استعادة-النسخ-الاحتياطي)
3. [إدارة الجلسات والأمان](#3-إدارة-الجلسات-والأمان)
4. [معالجة الحوادث الشائعة](#4-معالجة-الحوادث-الشائعة)
5. [الصيانة والترقية](#5-الصيانة-والترقية)
6. [الاتصال وسلسلة التصعيد](#6-الاتصال-وسلسلة-التصعيد)

---

## 1. العمليات اليومية والدورية

### 1.1 مهام Cron المجدولة

```bash
# افتح crontab للتعديل
crontab -e

# أضف المهام التالية:
# نسخ احتياطي كل 6 ساعات
0 */6 * * * /opt/telegram/scripts/backup.sh >> /var/log/telegram/backup.log 2>&1

# اختبار استعادة أسبوعي (كل أحد الساعة 03:00 UTC)
0 3 * * 0 /opt/telegram/scripts/restore-drill.sh >> /var/log/telegram/restore-drill.log 2>&1

# تدوير السجلات يومياً
0 0 * * * /usr/sbin/logrotate /etc/logrotate.d/telegram
```

### 1.2 فحص صحة النظام السريع

```bash
# 1. فحص الخدمة الرئيسية
curl -s http://localhost:3000/api/health | jq .
# المتوقع: {"status":"ok","timestamp":...}

# 2. فحص استخدام الذاكرة لعملية Node
ps aux | grep "[n]ode.*server.ts" | awk '{print $2, $4"%", $6/1024"MB"}'

# 3. فحص الجلسات النشطة
# (عبر نقطة نهاية المراقبة المحمية)
curl -s -H "Authorization: Bearer ${METRICS_TOKEN}" \
  http://localhost:3000/api/metrics/sessions | jq .

# 4. فحص اتصالات WebSocket
ss -tnp | grep :3000 | grep ESTAB | wc -l
```

---

## 2. استعادة النسخ الاحتياطي

### 2.1 سيناريو الاستعادة الطارئة

> ⚠️ **تحذير:** الاستعادة تستبدل قاعدة البيانات الحالية بالكامل.  
> قم بإيقاف الخدمة قبل البدء لتجنب تعارض الكتابة.

#### الخطوة 1: إيقاف الخدمة وأخذ لقطة للحالة الحالية

```bash
# إيقاف استقبال الطلبات
sudo systemctl stop telegram-web

# حفظ قاعدة البيانات الحالية المعطوبة للتحليل الجنائي
mkdir -p /tmp/crash-dump-$(date +%s)
cp /opt/telegram/data/telegram.db /tmp/crash-dump-$(date +%s)/ 2>/dev/null || true
```

#### الخطوة 2: اختيار النسخة الاحتياطية

```bash
# من GCS: عرض آخر 5 نسخ
gsutil ls -l "gs://${GCS_BUCKET}/backups/*.gpg" | tail -n 5

# تنزيل النسخة المطلوبة وبصمتها
LATEST=$(gsutil ls "gs://${GCS_BUCKET}/backups/*.gpg" | tail -n 1)
gsutil cp "${LATEST}" /tmp/
gsutil cp "${LATEST}.sha256" /tmp/
```

#### الخطوة 3: التحقق من البصمة وفك التشفير

```bash
cd /tmp
FILENAME=$(basename "${LATEST}")

# فك التشفير (بالمفتاح المتماثل)
echo "${BACKUP_PASSPHRASE}" | gpg --batch --yes \
  --passphrase-fd 0 \
  --decrypt "${FILENAME}" > backup.tar.gz

# استخراج وفحص المحتوى
mkdir -p /tmp/restore-work
tar -xzf backup.tar.gz -C /tmp/restore-work

# فحص البصمة الداخلية
cd /tmp/restore-work
sha256sum -c sha256sums.txt
```

#### الخطوة 4: التحقق من سلامة SQLite

```bash
RESTORED_DB=$(find /tmp/restore-work -name "*.db" | head -n 1)

# فحص السلامة الهيكلية
sqlite3 "${RESTORED_DB}" "PRAGMA integrity_check;"
# الناتج المطلوب: ok

# فحص عينة من الجداول
sqlite3 "${RESTORED_DB}" "SELECT count(*) FROM users; SELECT count(*) FROM messages;"
```

#### الخطوة 5: استبدال الملف وإعادة التشغيل

```bash
# استبدال ذري
mv "${RESTORED_DB}" /opt/telegram/data/telegram.db
chown telegram:telegram /opt/telegram/data/telegram.db
chmod 640 /opt/telegram/data/telegram.db

# تشغيل الخدمة
sudo systemctl start telegram-web

# التحقق من التشغيل
curl -sf http://localhost:3000/api/health || {
  echo "فشل تشغيل الخدمة بعد الاستعادة!"
  exit 1
}
```

---

## 3. إدارة الجلسات والأمان

### 3.1 إبطال فوري لجميع الجلسات (Emergency Session Revoke)

في حال الاشتباه باختراق، استخدم هذا الإجراء لإجبار جميع المستخدمين على إعادة تسجيل الدخول:

```bash
# خيار 1: عبر نقطة نهاية الإدارة الداخلية
curl -X POST \
  -H "Authorization: Bearer ${ADMIN_SECRET_KEY}" \
  -H "Content-Type: application/json" \
  http://localhost:3000/api/admin/sessions/revoke-all

# خيار 2: إعادة تشغيل العملية (تفريغ ذاكرة الجلسات)
sudo systemctl restart telegram-web
```

### 3.2 إبطال جلسات مستخدم محدد

```bash
curl -X POST \
  -H "Authorization: Bearer ${ADMIN_SECRET_KEY}" \
  -H "Content-Type: application/json" \
  -d '{"userId": "USER_ID_HERE"}' \
  http://localhost:3000/api/admin/sessions/revoke-user
```

### 3.3 تدوير الأسرار (Secret Rotation)

#### CSRF_SECRET:
1. ولّد مفتاحاً جديداً: `openssl rand -hex 32`
2. حدّث ملف `.env`
3. نفّذ reload سلس: `sudo systemctl reload telegram-web`
*(ملاحظة: الجلسات القديمة ستحتاج طلب CSRF token جديد تلقائياً عبر العميل)*

#### SESSION_SECRET:
1. ولّد مفتاحاً جديداً: `openssl rand -hex 32`
2. حدّث `.env`
3. إعادة تشغيل: `sudo systemctl restart telegram-web`
*(سيؤدي لإعادة تسجيل دخول جميع المستخدمين)*

---

## 4. معالجة الحوادث الشائعة

### 4.1 مشكلة: اتصالات WebSocket معلقة أو مرتفعة جداً

**الأعراض:** استهلاك عالي للذاكرة، `ss -tnp` يظهر آلاف الاتصالات.

**التشخيص:**
```bash
# عدد الاتصالات الحالية
netstat -an | grep :3000 | grep ESTABLISHED | wc -l

# هل هناك IP واحد يحتكر الاتصالات؟
netstat -tn 2>/dev/null | grep :3000 | awk '{print $5}' | cut -d: -f1 | sort | uniq -c | sort -nr | head -10
```

**الحل السريع:**
```bash
# حظر IP مسيء فوراً عبر iptables
sudo iptables -A INPUT -s BAD_IP_HERE -p tcp --dport 3000 -j DROP

# خفض حد المهلة في بيئة التشغيل وإعادة التحميل
# WS_IDLE_TIMEOUT_MS=30000 في .env
sudo systemctl reload telegram-web
```

### 4.2 مشكلة: قفل قاعدة بيانات SQLite (`database is locked`)

**الأعراض:** ردود 500 في السجلات، رسائل `SQLITE_BUSY`.

**التشخيص:**
```bash
# التحقق من العمليات التي تفتح الملف
lsof /opt/telegram/data/telegram.db

# فحص وضع الـ WAL
sqlite3 /opt/telegram/data/telegram.db "PRAGMA journal_mode;"
# يجب أن يكون: wal
```

**الحل:**
```bash
# تفعيل وضع WAL فوراً إذا لم يكن مفعلاً
sqlite3 /opt/telegram/data/telegram.db "PRAGMA journal_mode=WAL;"

# تفعيل busy_timeout لانتظار فتح القفل
sqlite3 /opt/telegram/data/telegram.db "PRAGMA busy_timeout=5000;"

# تنظيف نقاط التفتيش العالقة
sqlite3 /opt/telegram/data/telegram.db "PRAGMA wal_checkpoint(TRUNCATE);"
```

---

## 5. الصيانة والترقية

### 5.1 إجراء الترقية بدون انقطاع (Zero-Downtime Deployment)

```bash
#!/usr/bin/env bash
set -e

APP_DIR="/opt/telegram"
NEW_RELEASE_DIR="/opt/telegram-releases/$(date +%Y%m%d_%H%M%S)"

echo "1. أخذ نسخة احتياطية إجبارية قبل الترقية..."
${APP_DIR}/scripts/backup.sh

echo "2. سحب الكود الجديد..."
git clone --depth 1 -b master https://github.com/anwer1230/Documents.git "${NEW_RELEASE_DIR}"

echo "3. تثبيت الاعتمادات وبناء الأصول..."
cd "${NEW_RELEASE_DIR}"
npm ci --only=production
npm run build

echo "4. تشغيل اختبارات التحقق السريع..."
npm test -- tests/security/

echo "5. تبديل الرابط الرمزي (Atomic Switch)..."
ln -sfn "${NEW_RELEASE_DIR}" /opt/telegram_new
mv -Tf /opt/telegram_new /opt/telegram-current

echo "6. إعادة تحميل الخدمة..."
sudo systemctl reload telegram-web

echo "7. التحقق من الصحة بعد الترقية..."
sleep 3
curl -sf http://localhost:3000/api/health || {
  echo "فشل التحقق! التراجع الفوري..."
  exit 1
}
echo "تمت الترقية بنجاح."
```

---

## 6. الاتصال وسلسلة التصعيد

| المستوى | المسؤول | وقت الاستجابة المستهدف | وسيلة الاتصال |
|---|---|---|---|
| L1 | مهندس التشغيل المناوب | < 15 دقيقة | PagerDuty / Slack #ops-alerts |
| L2 | مهندس الأمان / التطوير | < 30 دقيقة | هاتف / Signal |
| L3 | قائد الفريق التقني | < 1 ساعة | هاتف مباشر |

```
حادث -> فحص /api/health -> فحص السجلات -> تطبيق Runbook
                                            |
                         لم يُحل خلال 15 دقيقة؟ -> تصعيد إلى L2
                                            |
                         تأكيد تسريب بيانات؟   -> إيقاف الخدمة + تصعيد فوري L3
```
