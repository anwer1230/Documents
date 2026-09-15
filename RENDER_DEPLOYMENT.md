# دليل نشر تطبيق Telegram Web على منصة Render (Render.com)

تم تجهيز وتكوين هذا المشروع بالكامل ليعمل بشكل فوري وسلس على منصة **Render** كخدمة ويب متكاملة (Full-Stack Web Service) تدعم بروتوكول Telegram MTProto الرسمي واتصالات الـ WebSocket الفورية.

---

## الخيار 1: النشر التلقائي عبر Blueprint (الأسهل والأسرع)

تم تضمين ملف الإعدادات الرسمي `render.yaml` في المستودع، مما يتيح لك نشر التطبيق بضغطة زر واحدة:

1. سجّل الدخول إلى حسابك في موقع [Render.com](https://render.com).
2. من لوحة التحكم (Dashboard)، اضغط على الزر **New +** ثم اختر **Blueprint**.
3. قم بربط مستودع GitHub الخاص بك: `https://github.com/anwer1230/Documents`.
4. حدد الفرع `master`.
5. سيقوم Render تلقائياً باكتشاف ملف `render.yaml` وقراءة جميع إعدادات البناء، أوامر التشغيل، والمتغيرات البيئية المثبتة.
6. اضغط على **Apply** وسيبدأ Render في بناء ونشر التطبيق فوراً.

---

## الخيار 2: النشر اليدوي كخدمة ويب (Web Service)

إذا أردت إنشاء الخدمة يدوياً من لوحة التحكم في Render:

1. اضغط على **New +** ثم اختر **Web Service**.
2. اختر مستودع `anwer1230/Documents` وحدد الفرع `master`.
3. قم بتعبئة الإعدادات التالية:
   - **Name:** `telegram-web` (أو أي اسم تفضله)
   - **Region:** `Frankfurt (EU Central)` أو أي منطقة قريبة منك
   - **Branch:** `master`
   - **Runtime:** `Node`
   - **Build Command:**
     ```bash
     npm install && npm run build
     ```
   - **Start Command:**
     ```bash
     npm start
     ```
   - **Instance Type:** `Free` (أو الباقة المناسبة لك)

4. في قسم **Environment Variables** (المتغيرات البيئية)، أضف المتغيرات التالية:
   - `NODE_ENV`: `production`
   - `RENDER`: `true`
   - `TELEGRAM_API_ID`: `22043994`
   - `TELEGRAM_API_HASH`: `56f64582b363d367280db96586b97801`
   - `VAPID_PUBLIC_KEY`: `BE36BmheMRx2GxzjWpp_4bmXq_hZg55bP_M_vNVysfnjTxns9VCI0hiCHgnRBx0URe_LoxWaAgrS9G9QZbQhOh8`
   - `VAPID_PRIVATE_KEY`: `13NU1_GmeL7bDQcVtlFyuKqsnnsX3XkOyE--2rAQJw4`
   - `VAPID_SUBJECT`: `mailto:anwrfwad178@gmail.com`

5. اضغط على **Deploy Web Service**.

---

## الخيار 3: النشر عبر حاوية Docker (Container)

يحتوي المستودع أيضاً على `Dockerfile` جاهز ومتعدد المراحل (Multi-stage build):
1. عند إنشاء الـ **Web Service**، اختر **Docker** بدلاً من Node.
2. سيقوم Render ببناء الـ Image وتشغيل التطبيق على المنفذ المحدد مع فحص الجاهزية التلقائي (Healthcheck).

---

## مميزات التكوين على Render:
- **دعم اتصالات WebSocket:** يعمل الـ WebSocket لنقل الرسائل والتحديثات اللحظية تلقائياً دون أي إعداد إضافي.
- **مسار فحص الحالة (Health Check):** متاح تلقائياً عبر `/api/health` للتحقق من سلامة الخدمة باستمرار.
- **التوافق التام للمنافذ:** يدعم التطبيق منفذ Render التلقائي المخصص (`process.env.PORT`) بدقة دون أي تعارض مع البيئات الأخرى.
