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
  notificationSound: 'silent',
  enableSendSound: false,
  enableClickSound: true,
  muteChatSounds: true,
  soundVolume: 0,
  dataSaver: false,
  autoDownloadMedia: true,
  preloadThumbnails: true,
  enableTTS: false,
  ttsLanguage: 'ar-SA',
};
