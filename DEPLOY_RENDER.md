# 🚀 دليل النشر على منصة ريندر (Deploying to Render.com)

تم تجهيز هذا المشروع ليعمل بسلاسة وبشكل مباشر على منصة **Render.com** كـ **Web Service** أو عبر **Render Blueprints (render.yaml)** أو عبر **Docker Container**.

---

## 🛠️ 1. بيانات وإعدادات البيئة (Environment Variables)

تم دمج كافة المتغيرات في ملف التكوين المركزي `src/config/envConfig.ts`، ويقوم النظام بقراءتها تلقائياً من متغيرات البيئة (`process.env`) أو من الملف، مع قيم افتراضية مدمجة:

| اسم المتغير | القيمة الافتراضية المدمجة | الوصف |
| :--- | :--- | :--- |
| **`API_ID`** | `22043994` | معرف تطبيق تيليجرام الرسمي الخاص بك |
| **`API_HASH`** | `56f64582b363d367280db96586b97801` | مفتاح التشفير و Hash لتطبيق تيليجرام |
| **`TDLIB_API_HASH`**| `56f64582b363d367280db96586b97801` | مفتاح TDLIB الخاص |
| **`SESSION_SECRET`** | `tg_session_anwer_foud_secure_key_2026` | مفتاح حماية وتشفير الجلسات |
| **`GEMINI_API_KEY`** | *(اختياري من حسابك)* | مفتاح ذكاء اصطناعي Gemini |
| **`GROQ_API_KEY`** | *(اختياري من حسابك)* | مفتاح Groq Llama AI |
| **`NODE_ENV`** | `production` | بيئة التشغيل الإنتاجية |
| **`PORT`** | يتم ضبطه تلقائياً بواسطة ريندر (أو `3000`) | منفذ تشغيل الخادم |

---

## ⚡ 2. أوامر النشر الأساسية في ريندر (Render Dashboard Settings)

عند إنشاء **Web Service** جديدة على ريندر، اختر الإعدادات التالية:

* **Name:** `telegram-anwer-web`
* **Environment:** `Node`
* **Region:** Frankfurt (أو أي منطقة تفضلها)
* **Branch:** `main`
* **Build Command:**
  ```bash
  npm install && npm run build
  ```
* **Start Command:**
  ```bash
  npm start
  ```
* **Health Check Path:**
  ```text
  /api/telegram/status
  ```

---

## 📄 3. النشر التلقائي عبر ملف Blueprint (`render.yaml`)

يحتوي المشروع على ملف `render.yaml` جاهز. للنشر بنقرة واحدة:
1. اذهب إلى حسابك في **[Render Dashboard](https://dashboard.render.com)**.
2. انقر على زر **New +** ثم اختر **Blueprint**.
3. اختر مستودع الجيت الخاص بك (Repository).
4. سيقوم ريندر بقراءة ملف `render.yaml` وضبط كافة المتغيرات وأوامر البناء والتشغيل تلقائياً.

---

## 🐳 4. النشر كحاوية Docker (اختياري)

إذا كنت تفضل نشر التطبيق كـ Docker Container:
* يحتوي المشروع على `Dockerfile` محسّن ومتعدد المراحل (Multi-stage build) بحجم خفيف جداً (Alpine Node 20).
* في ريندر اختر **Docker** كبيئة تشغيل وسيقوم بالبناء والتشغيل مباشرة.

---

## 🔍 5. فحص جاهزية الخادم والبيئة بعد النشر

بعد اكتمال النشر، يمكنك فحص حالة الخادم والمتغيرات من خلال الروابط:
* `https://your-app-name.onrender.com/api/telegram/status`
* `https://your-app-name.onrender.com/api/env/info`
