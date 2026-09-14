import { useEffect } from 'react';
import { ttsService } from '../services/ttsService';

export const useTTS = (enabled: boolean = false, language: string = 'ar-SA') => {
  useEffect(() => {
    if (!enabled) return;

    const handleNewMessage = (event: Event) => {
      const customEvent = event as CustomEvent;
      const detail = customEvent.detail || {};
      const { text, isOutgoing, isOut } = detail;

      // Speak message if incoming and not a bot command
      if (text && !isOutgoing && !isOut && !String(text).startsWith('/')) {
        ttsService.speakMessage(String(text), language);
      }
    };

    window.addEventListener('newMessage', handleNewMessage);
    window.addEventListener('newTelegramMessage', handleNewMessage);

    return () => {
      window.removeEventListener('newMessage', handleNewMessage);
      window.removeEventListener('newTelegramMessage', handleNewMessage);
      ttsService.stop();
    };
  }, [enabled, language]);
};

export default useTTS;
