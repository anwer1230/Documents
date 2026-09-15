# البنية التحتية — Terraform Multi-Region Setup

## المتطلبات
- Terraform >= 1.5.0
- Google Cloud SDK (`gcloud`)
- صلاحيات `Owner` أو `Editor` على مشروع GCP

## هيكل المجلدات
```
terraform/
├── environments/
│   ├── prod/        # إعدادات بيئة الإنتاج
│   └── staging/     # إعدادات بيئة التجربة
└── modules/         # الوحدات القابلة لإعادة الاستخدام
    ├── cloud-run/   # نشر خدمات الحاويات
    ├── gcs-backup/  # حاويات التخزين مع سياسات الاستبقاء
    └── load-balancer/# موازن الأحمال العالمي مع شهادة SSL
```

## خطوات النشر لأول مرة (Production)

```bash
cd terraform/environments/prod

# 1. تهيئة الحالة وتنزيل المزودات
terraform init

# 2. معاينة التغييرات
terraform plan

# 3. التطبيق
terraform apply
```

## التغيير الطارئ أثناء الكوارث (Emergency Failover)
لتفعيل المنطقة الثانوية يدوياً عبر Terraform:
عدّل في `modules/load-balancer/main.tf` قيمة `capacity_scaler`:
- `primary`: 0.0
- `secondary`: 1.0
ثم نفذ:
```bash
terraform apply -target=module.root.module.load_balancer
```
