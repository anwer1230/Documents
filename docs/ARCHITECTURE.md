# بنية النظام ومعماريته — System Architecture

## نظرة عامة
تطبيق عميل تيليجرام للويب يعتمد على بنية متكاملة Client-Server:
- **الواجهة الأمامية (Frontend):** React + Tailwind CSS + Lucide Icons مع دعم WebSockets للتحديثات الفورية وإدارة الحالة المحلية.
- **الخادم الخلفي (Backend):** Node.js / Express وWebSocket Server مدمج مع طبقة أمان شاملة (Helmet, CSRF protection, Rate Limiting, Origin verification).
- **قاعدة البيانات:** SQLite3 مع تفعيل وضع WAL وPRAGMA busy_timeout لضمان التزامن والأداء العالي.
- **إدارة الجلسات والأمان:** جلسات مبنية على التشفير مع دعم SRP لمصادقة تيليجرام وحماية الترويسات.
- **البنية التحتية والتعافي:** Terraform Multi-Region جاهز لـ Google Cloud Run مع تخزين سحابي آلي ومشفر لنسخ الاحتياط في Google Cloud Storage (GCS).
