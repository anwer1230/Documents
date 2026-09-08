// hooks/useTTS.ts
import { useEffect } from 'react';
import { useSettingsStore } from '../stores/settingsStore';
import { ttsService } from '../services/ttsService';

export const useTTS = () => {
  const { settings } = useSettingsStore();

  useEffect(() => {
    if (!settings.enableTTS) return;

    // الاستماع للأحداث الجديدة
    const handleNewMessage = (event: Event) => {
      const customEvent = event as CustomEvent;
      const detail = customEvent.detail || {};
      const { text, isOutgoing } = detail;

      // نطق الرسالة إذا كانت واردة (وليست من المستخدم نفسه) وليست أمراً
      if (text && !isOutgoing && !String(text).startsWith('/')) {
        ttsService.speakMessage(String(text), settings.ttsLanguage || 'ar-SA');
      }
    };

    window.addEventListener('newMessage', handleNewMessage);
    window.addEventListener('newTelegramMessage', handleNewMessage);

    return () => {
      window.removeEventListener('newMessage', handleNewMessage);
      window.removeEventListener('newTelegramMessage', handleNewMessage);
      ttsService.stop();
    };
  }, [settings.enableTTS, settings.ttsLanguage]);
};
