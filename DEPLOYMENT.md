# دليل النشر والتشغيل — Deployment Guide

## المتطلبات الأساسية
- Node.js >= 20.0.0
- SQLite 3
- بيئة مدعومة لتشغيل Cloud Run أو حاويات Docker

## خطوات النشر للإنتاج (Production Deployment)

### 1. تثبيت الاعتمادات
```bash
npm ci --only=production
```

### 2. البناء
```bash
npm run build
```

### 3. متغيرات البيئة الأساسية
تأكد من وجود المتغيرات في `.env` أو بيئة السحاب:
```env
PORT=3000
NODE_ENV=production
CSRF_SECRET=<32-char-random-hex>
SESSION_SECRET=<32-char-random-hex>
ALLOWED_ORIGINS=https://app.telegram.internal,https://telegram.example.com
```

### 4. التشغيل
```bash
npm start
```

### 5. فحص الجاهزية
```bash
curl -f http://localhost:3000/api/health
```
