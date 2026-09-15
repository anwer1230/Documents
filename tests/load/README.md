# اختبارات الحمل — Load Testing Guide

## اختبار اتصالات WebSocket المتزامنة
يقوم السكريبت `tests/load/ws-connections.js` بمحاكاة فتح اتصالات متزامنة للتحقق من صمود خادم WebSocket وسياسات Rate Limiting وعزل الموارد.

### طريقة التشغيل:
```bash
# تشغيل 50 عميل متزامن ضد الخادم المحلي
node tests/load/ws-connections.js ws://localhost:3000/api/ws 50

# تشغيل 200 عميل لاختبار ضغط الخادم
node tests/load/ws-connections.js ws://localhost:3000/api/ws 200
```
