// hooks/useSoundEffects.ts
import { useSettingsStore } from '../stores/settingsStore';
import { audioService } from '../services/audioService';

export const useSoundEffects = () => {
  const { settings } = useSettingsStore();

  const playSendSound = () => {
    if (!settings.enableSendSound) return;
    audioService.playBubblePop();
  };

  const playClickSound = () => {
    if (!settings.enableClickSound) return;
    audioService.playClick();
  };

  const playNotificationSound = (type?: string) => {
    const soundType = type || settings.notificationSound;
    audioService.playNotification(soundType);
  };

  return { playSendSound, playClickSound, playNotificationSound };
};
