export interface AppSettings {
  // المظهر
  theme: 'light' | 'dark';
  
  // الإشعارات والأصوات
  notificationSound: 'classic' | 'beep' | 'chime' | 'bubble' | 'silent';
  enableSendSound: boolean;
  enableClickSound: boolean;
  
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
  dataSaver: false,
  autoDownloadMedia: true,
  preloadThumbnails: true,
  enableTTS: false,
  ttsLanguage: 'ar-SA',
};
