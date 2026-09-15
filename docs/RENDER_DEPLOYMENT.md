# دليل نشر تطبيق Telegram Web على منصة Render (Render Deployment Guide)

يوفر هذا الدليل الخطوات الدقيقة والتفصيلية لنشر التطبيق بنجاح عبر خوادم **Render** سواء بالطريقة التلقائية باستخدام `render.yaml` (Infrastructure as Code) أو بالطريقة اليدوية عبر لوحة تحكم Render.

---

## الطريقة الأولى: النشر التلقائي عبر Blueprint (موصى بها 🚀)

1. ادخل إلى حسابك في [لوحة تحكم Render](https://dashboard.render.com/).
2. اضغط على زر **New +** في أعلى الصفحة واختر **Blueprint**.
3. قم بربط مستودع GitHub الخاص بك:
   ```
   https://github.com/anwer1230/Documents
   ```
4. سيتعرف Render تلقائياً على ملف `render.yaml` الموجود في المستودع.
5. سيطلب منك مراجعة المتغيرات والضغط على **Apply**.
6. بعد إنشاء الخدمة والحصول على الرابط (`https://your-service-name.onrender.com`)، أضف الرابط إلى متغير `ALLOWED_ORIGINS` في إعدادات البيئة.

---

## الطريقة الثانية: النشر اليدوي خطوة بخطوة (Web Service)

إذا أردت إنشاء الخدمة يدوياً دون استخدام Blueprint:

### 1. إعدادات الخدمة الأساسية (Basic Settings)
* **نوع الخدمة (Service Type):** `Web Service`
* **المستودع (Repository):** `https://github.com/anwer1230/Documents`
* **الفرع (Branch):** `master`
* **بيئة التشغيل (Runtime):** `Node`
* **المنطقة (Region):** `Frankfurt (EU Central)` (الأقرب لمراكز بيانات تيليجرام الأوروبية DC2/DC4) أو أي منطقة تناسبك.
* **الخطة (Plan):** `Free` (أو `Starter` / `Standard` لأداء أعلى وعدم التوقف المؤقت).

### 2. أوامر البناء والتشغيل (Build & Start Commands)
* **أمر البناء (Build Command):**
  ```bash
  npm install && npm run build
  ```
* **أمر التشغيل (Start Command):**
  ```bash
  npm start
  ```

### 3. مسار فحص الجاهزية والصحة (Health Check Path)
* **Health Check Path:**
  ```text
  /api/health
  ```

---

## 4. متغيرات البيئة في ريندر (Environment Variables)

أضف المتغيرات التالية في تبويب **Environment** داخل الخدمة في Render:

| اسم المتغير (Key) | القيمة (Value) | الوصف |
| :--- | :--- | :--- |
| `NODE_ENV` | `production` | نمط الإنتاج المشدد |
| `NODE_VERSION` | `22.14.0` | إصدار Node.js لدعم `node:sqlite` و MTProto |
| `TELEGRAM_API_ID` | `22043994` | معرف تطبيق تيليجرام المعتمد |
| `TELEGRAM_API_HASH` | `56f64582b363d367280db96586b97801` | هاش تطبيق تيليجرام المعتمد |
| `VAPID_PUBLIC_KEY` | `BE36BmheMRx2GxzjWpp_4bmXq_hZg55bP_M_vNVysfnjTxns9VCI0hiCHgnRBx0URe_LoxWaAgrS9G9QZbQhOh8` | مفتاح Web Push العام |
| `VAPID_PRIVATE_KEY` | `13NU1_GmeL7bDQcVtlFyuKqsnnsX3XkOyE--2rAQJw4` | مفتاح Web Push الخاص |
| `VAPID_SUBJECT` | `mailto:anwerfoud80@gmail.com` | البريد الإلكتروني المعتمد لخدمة VAPID |
| `ALLOWED_ORIGINS` | `https://your-app-name.onrender.com` | رابط تطبيقك على ريندر لحماية الطلبات من هجمات CSRF/CORS |
| `WS_STRICT_MODE` | `true` | تأمين وتشفير اتصالات WebSocket |
| `COOKIE_STRICT_MODE` | `true` | تشديد حماية ملفات تعريف الارتباط |
| `SESSION_TTL_DAYS` | `30` | مدة صلاحية الجلسة بالأيام |
| `RATE_LIMIT_HTTP_PER_MIN` | `120` | حماية الخادم من إغراق الطلبات (120 طلب/دقيقة) |
| `RATE_LIMIT_HTTP_WRITE_PER_MIN` | `30` | حماية نقاط التعديل والكتابة (30 طلب/دقيقة) |
| `LOG_LEVEL` | `info` | مستوى سجلات الخادم |
| `GEMINI_API_KEY` | *(اختياري)* | مفتاح Google Gemini إذا رغبت بتفعيل ميزات الذكاء الاصطناعي |

---

## 5. ملاحظات هامة لضمان أفضل أداء على Render

1. **المنفذ الافتراضي (Port):** يقوم ريندر تلقائياً بتمرير المنفذ في متغير `PORT` أو توجيه حركة المرور إلى المنفذ الافتراضي. تم تهيئة الخادم للاستماع تلقائياً للطلبات وخدمة ملفات الواجهة والـ API معاً.
2. **اتصالات WebSocket ومزامنة تيليجرام:** منصة Render تدعم بالكامل WebSocket المباشر ومقابس MTProto طويلة الأمد دون أي إعدادات إضافية.
3. **تنبيه لخطة Render المجانية (Free Tier Spin-down):** في الخطة المجانية، يدخل التطبيق في وضع السكون بعد 15 دقيقة من عدم النشاط. في حال أردت بقاء مستمع رسائل تيليجرام يعمل على مدار الساعة واستقبال الإشعارات الفورية دون أي انقطاع، يُفضل استخدام خطة **Starter ($7/month)** أو ضبط أداة فحص دورية (Ping/Uptime Robot) على مسار `/api/health`.
