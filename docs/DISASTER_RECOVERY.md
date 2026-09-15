# خطة التعافي من الكوارث — Disaster Recovery Plan (DRP)

> **النظام:** Telegram Web Client  
> **الإصدار:** 1.0.0  
> **مستوى الأهمية:** Tier 1 — Critical  
> **آخر مراجعة:** 2026-09  

---

## 1. أهداف التعافي (Recovery Objectives)

| المعيار | القيمة المستهدفة | القيمة القصوى المقبولة | الشرح |
|---|---|---|---|
| **RPO** (نقطة الاستعادة) | **6 ساعات** | 12 ساعة | أقصى حجم بيانات مفقودة مسموح به (تردد النسخ الاحتياطي) |
| **RTO** (زمن الاستعادة) | **30 دقيقة** | 60 دقيقة | أقصى وقت لإعادة تشغيل الخدمة بكامل طاقتها بعد وقوع الكارثة |
| **MTTR** (متوسط زمن الإصلاح) | **45 دقيقة** | 90 دقيقة | الوقت المعتاد للتشخيص وتطبيق خطة التعافي |

---

## 2. تصنيف مستويات الكوارث (Disaster Severity Levels)

```
[Level 1: انقطاع موضعي]
  └── تلف حاوية Cloud Run واحدة -> إعادة تشغيل تلقائية (< 2 دقيقة)

[Level 2: تلف البيانات / Data Corruption]
  └── فساد SQLite -> استعادة من آخر نسخة سليمة (< 30 دقيقة)

[Level 3: انقطاع المنطقة الجغرافية بالكامل / Regional Outage]
  └── توقف منطقة GCP بالكامل -> تفعيل Failover للمنطقة الاحتياطية (< 20 دقيقة)

[Level 4: فقدان شامل للبنية التحتية / Total Loss]
  └── تدمير حساب السحاب بالكامل -> إعادة بناء عبر Terraform في حساب بديل (< 2 ساعة)
```

---

## 3. سيناريوهات التعافي التفصيلية

### السيناريو أ: تلف قاعدة البيانات المحلية (Data Corruption)
**الهدف:** استعادة الخدمة مع RPO < 6 ساعات.

1. **التعرف:** تنبيه P0 من Cloud Monitoring بخصوص `PRAGMA integrity_check` أو فشل `/api/health`.
2. **عزل المشكلة:**
   ```bash
   # حظر الكتابة لحماية الملف من تفاقم التلف
   sudo chmod 440 /opt/telegram/data/telegram.db
   ```
3. **تنفيذ الاستعادة:**
   راجع [docs/RUNBOOK.md — القسم 2.1](RUNBOOK.md#21-سيناريو-الاستعادة-الطارئة).
4. **التحقق بعد الاستعادة:**
   ```bash
   /opt/telegram/scripts/restore-drill.sh
   ```

---

### السيناريو ب: انقطاع المنطقة الرئيسية (Primary Region Failover)
**الهدف:** تحويل حركة المرور إلى المنطقة الاحتياطية مع RTO < 20 دقيقة.

```
[مستخدمو التيليجرام]
       │
       ▼
[Cloud DNS / Global Load Balancer]
       │
   ┌───┴──────────────────────┐
   │ (الرئيسية)                │ (الاحتياطية - مفعلة الآن)
   ▼                          ▼
[us-central1]               [europe-west1]
❌ منطقة متوقفة               ✅ تعمل وتخدم الطلبات
```

#### خطوات التحويل الفوري (Failover):

```bash
# 1. تشغيل سكريبت التحويل إلى المنطقة الاحتياطية
./scripts/failover-to-secondary.sh --region europe-west1

# 2. تحديث سجل DNS إذا كان التحويل عبر DNS
gcloud dns record-sets transaction start --zone="telegram-zone"
gcloud dns record-sets transaction remove --zone="telegram-zone" \
  --name="app.example.com." --type="A" --ttl=60 "PRIMARY_IP"
gcloud dns record-sets transaction add --zone="telegram-zone" \
  --name="app.example.com." --type="A" --ttl=60 "SECONDARY_IP"
gcloud dns record-sets transaction execute --zone="telegram-zone"

# 3. التحقق من وصول حركة المرور للمنطقة الجديدة
curl -sI https://app.example.com/api/health | grep -i "x-served-by"
```

---

## 4. مصفوفة الصلاحيات أثناء الطوارئ

```
حادث كارثي معلن
       │
       ├── القائد التقني (Incident Commander): يتخذ قرار التحويل الإقليمي (Failover)
       │
       ├── مهندس البنية التحتية (Ops Lead): ينفذ السكريبتات واستعادة النسخ
       │
       └── مسؤول الاتصال (Comms Lead): يحدث صفحة الحالة (Status Page) والعملاء
```

---

## 5. جدول التمارين الدورية (DR Drills Schedule)

| التمرين | التردد | المسؤول | معيار النجاح |
|---|---|---|---|
| **Restore Drill الآلي** | أسبوعياً (أحد 03:00 UTC) | مجدول عبر Cron | تقرير نجاح بدون أخطاء |
| **Failover محاكى للمنطقة الاحتياطية** | كل 3 أشهر | فريق العمليات | RTO < 20 دقيقة وبدون فقدان بيانات |
| **مراجعة وتدوير المفاتيح** | كل 6 أشهر | مسؤول الأمان | عدم انقطاع الخدمة أثناء التدوير |
