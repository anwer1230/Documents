export interface AppSettings {
  // المظهر
  theme: 'light' | 'dark';
  
  // الإشعارات والأصوات
  notificationSound: 'classic' | 'beep' | 'chime' | 'bubble' | 'silent';
  enableSendSound: boolean;
  enableClickSound: boolean;
  muteChatSounds?: boolean;
  soundVolume?: number; // 0 .. 100
  
  // توفير البيانات
  dataSaver: boolean;
  autoDownloadMedia: boolean;
  preloadThumbnails: boolean;
  
  // النطق الآلي (TTS)
  enableTTS: boolean;
  ttsLanguage: string; // 'ar' للعربية
}

export const DEFAULT_SETTINGS: AppSettings = {
  theme: 'dark',
  notificationSound: 'classic',
  enableSendSound: true,
  enableClickSound: true,
  muteChatSounds: false,
  soundVolume: 80,
  dataSaver: false,
  autoDownloadMedia: true,
  preloadThumbnails: true,
  enableTTS: false,
  ttsLanguage: 'ar-SA',
};
