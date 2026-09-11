/**
 * Telegram Web K Web Push Notification Manager
 * Handles VAPID key conversion, Service Worker registration,
 * permission request, and subscription management.
 */

export const VAPID_PUBLIC_KEY = 'BE36BmheMRx2GxzjWpp_4bmXq_hZg55bP_M_vNVysfnjTxns9VCI0hiCHgnRBx0URe_LoxWaAgrS9G9QZbQhOh8';

function urlBase64ToUint8Array(base64String: string): Uint8Array {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding)
    .replace(/-/g, '+')
    .replace(/_/g, '/');

  const rawData = window.atob(base64);
  const outputArray = new Uint8Array(rawData.length);

  for (let i = 0; i < rawData.length; ++i) {
    outputArray[i] = rawData.charCodeAt(i);
  }
  return outputArray;
}

export async function registerServiceWorker(): Promise<ServiceWorkerRegistration | null> {
  if (!('serviceWorker' in navigator)) {
    console.warn('[WebPush] Service Workers are not supported in this browser.');
    return null;
  }

  try {
    const registration = await navigator.serviceWorker.register('/sw.js', {
      scope: '/',
    });
    await navigator.serviceWorker.ready;
    console.log('[WebPush] Service Worker registered with scope:', registration.scope);
    return registration;
  } catch (error) {
    console.error('[WebPush] Service Worker registration failed:', error);
    return null;
  }
}

export async function getPushSubscription(): Promise<PushSubscription | null> {
  if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
    return null;
  }

  try {
    const registration = await navigator.serviceWorker.ready;
    return await registration.pushManager.getSubscription();
  } catch (e) {
    return null;
  }
}

export async function subscribeToWebPush(): Promise<{ success: boolean; error?: string }> {
  if (!('Notification' in window)) {
    return { success: false, error: 'هذا المتصفح لا يدعم الإشعارات' };
  }

  if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
    return { success: false, error: 'خدمة Web Push غير مدعومة في المتصفح الحالي' };
  }

  try {
    const permission = await Notification.requestPermission();
    if (permission !== 'granted') {
      return { success: false, error: 'تم رفض إذن إشعارات المتصفح' };
    }

    const registration = await registerServiceWorker();
    if (!registration) {
      return { success: false, error: 'فشل تسجيل Service Worker' };
    }

    const applicationServerKey = urlBase64ToUint8Array(VAPID_PUBLIC_KEY);
    
    // Check existing subscription
    let subscription = await registration.pushManager.getSubscription();
    if (!subscription) {
      subscription = await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey,
      });
    }

    // Send subscription object to server
    const res = await fetch('/api/push/subscribe', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(subscription),
    });

    if (!res.ok) {
      throw new Error('فشل تسجيل الاشتراك على الخادم');
    }

    return { success: true };
  } catch (err: any) {
    console.error('[WebPush] Subscription error:', err);
    return { success: false, error: err.message || 'حدث خطأ أثناء تفعيل الإشعارات' };
  }
}

export async function unsubscribeFromWebPush(): Promise<boolean> {
  try {
    const subscription = await getPushSubscription();
    if (subscription) {
      await subscription.unsubscribe();
    }
    await fetch('/api/push/unsubscribe', { method: 'POST' });
    return true;
  } catch (e) {
    console.warn('[WebPush] Unsubscribe error:', e);
    return false;
  }
}

export async function sendTestWebPush(): Promise<{ success: boolean; message?: string; error?: string }> {
  try {
    const res = await fetch('/api/push/test', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        title: 'Telegram Web Push 🔔',
        body: 'تم استلام الإشعار الفوري بنجاح عبر مفاتيح VAPID وخدمة Service Worker!',
      }),
    });
    const data = await res.json();
    if (res.ok) {
      return { success: true, message: data.message };
    } else {
      return { success: false, error: data.error };
    }
  } catch (err: any) {
    return { success: false, error: err.message || 'فشل إرسال الإشعار التجريبي' };
  }
}
