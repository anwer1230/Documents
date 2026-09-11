// Web Push Utility with permanently fixed VAPID keys
export const VAPID_PUBLIC_KEY = 'BE36BmheMRx2GxzjWpp_4bmXq_hZg55bP_M_vNVysfnjTxns9VCI0hiCHgnRBx0URe_LoxWaAgrS9G9QZbQhOh8';

export function urlBase64ToUint8Array(base64String: string): Uint8Array {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
  const rawData = window.atob(base64);
  const outputArray = new Uint8Array(rawData.length);
  for (let i = 0; i < rawData.length; ++i) {
    outputArray[i] = rawData.charCodeAt(i);
  }
  return outputArray;
}

export async function subscribeToWebPush(): Promise<{ success: boolean; message: string }> {
  if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
    return { success: false, message: 'المتصفح لا يدعم خدمة إشعارات Web Push' };
  }

  try {
    const permission = await Notification.requestPermission();
    if (permission !== 'granted') {
      return { success: false, message: 'تم رفض إذن الإشعارات من قبل المستخدم' };
    }

    const reg = await navigator.serviceWorker.ready;
    let subscription = await reg.pushManager.getSubscription();

    if (!subscription) {
      subscription = await reg.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: urlBase64ToUint8Array(VAPID_PUBLIC_KEY),
      });
    }

    const res = await fetch('/api/push/subscribe', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(subscription),
    });

    if (!res.ok) {
      throw new Error('فشل تسجيل الاشتراك في الخادم');
    }

    return { success: true, message: 'تم تفعيل إشعارات Web Push بنجاح عبر مفاتيح VAPID' };
  } catch (err: any) {
    console.error('[WebPush Subscribe Error]', err);
    return { success: false, message: err?.message || 'فشل تفعيل الإشعارات' };
  }
}

export async function sendTestWebPush(): Promise<{ success: boolean; message: string }> {
  try {
    const res = await fetch('/api/push/send-test', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
    });
    const data = await res.json();
    if (!res.ok || data.error) {
      throw new Error(data.error || 'فشل إرسال الإشعار التجريبي');
    }
    return { success: true, message: data.message || 'تم إرسال إشعار تجريبي بنجاح' };
  } catch (err: any) {
    return { success: false, message: err?.message || 'حدث خطأ أثناء إرسال الإشعار' };
  }
}
