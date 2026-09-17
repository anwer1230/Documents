function urlBase64ToUint8Array(base64String: string): Uint8Array {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
  const rawData = window.atob(base64);
  const outputArray = new Uint8Array(rawData.length);
  for (let i = 0; i < rawData.length; ++i) {
    outputArray[i] = rawData.charCodeAt(i);
  }
  return outputArray;
}

export async function getPushSubscription(): Promise<PushSubscription | null> {
  if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
    return null;
  }
  try {
    const reg = await navigator.serviceWorker.ready;
    return await reg.pushManager.getSubscription();
  } catch (err) {
    console.warn('[Push] Error checking subscription:', err);
    return null;
  }
}

export async function subscribeToWebPush(): Promise<{ success: boolean; error?: string }> {
  if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
    return { success: false, error: 'المتصفح لا يدعم إشعارات الويب (Push Notifications)' };
  }

  try {
    const permission = await Notification.requestPermission();
    if (permission !== 'granted') {
      return { success: false, error: 'تم رفض إذن الإشعارات من قبل المتصفح' };
    }

    const res = await fetch('/api/push/status');
    const status = await res.json();
    const vapidKey = status.vapidPublicKey;

    if (!vapidKey) {
      return { success: false, error: 'مفتاح VAPID غير متوفر على الخادم' };
    }

    const reg = await navigator.serviceWorker.ready;
    let sub = await reg.pushManager.getSubscription();

    if (!sub) {
      sub = await reg.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: urlBase64ToUint8Array(vapidKey),
      });
    }

    await fetch('/api/push/subscribe', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ subscription: sub }),
    });

    return { success: true };
  } catch (err: any) {
    console.error('[Push] Subscribe error:', err);
    return { success: false, error: err?.message || 'فشل تفعيل الإشعارات' };
  }
}

export async function unsubscribeFromWebPush(): Promise<boolean> {
  try {
    const sub = await getPushSubscription();
    if (sub) {
      await sub.unsubscribe();
      await fetch('/api/push/unsubscribe', { method: 'POST' });
    }
    return true;
  } catch (err) {
    console.error('[Push] Unsubscribe error:', err);
    return false;
  }
}

export async function sendTestWebPush(title?: string, body?: string): Promise<{ success: boolean; message?: string; error?: string }> {
  try {
    const res = await fetch('/api/push/test', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ title, body }),
    });
    return await res.json();
  } catch (err: any) {
    return { success: false, error: err?.message || 'فشل إرسال الإشعار التجريبي' };
  }
}
