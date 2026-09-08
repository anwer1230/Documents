// services/mediaService.ts
import { AppSettings, DEFAULT_SETTINGS } from '../types/settings';

export class MediaService {
  private settings: AppSettings;

  constructor(settings: AppSettings = DEFAULT_SETTINGS) {
    this.settings = settings;
  }

  // التحقق مما إذا كان يجب تحميل وسائط تلقائياً
  shouldAutoDownload(_mediaType: 'photo' | 'video' | 'audio' | 'file'): boolean {
    if (this.settings.dataSaver) return false;
    return this.settings.autoDownloadMedia;
  }

  // تحميل صورة مصغرة (thumbnail) مع مراعاة الإعدادات
  async loadThumbnail(chatId: string | number, messageId: string | number): Promise<string | null> {
    if (!this.settings.preloadThumbnails) return null;

    try {
      const response = await fetch(`/api/thumbnails/${chatId}/${messageId}`);
      if (response.ok) {
        const blob = await response.blob();
        return URL.createObjectURL(blob);
      }
    } catch (_) {}
    return null;
  }

  updateSettings(newSettings: AppSettings) {
    this.settings = newSettings;
  }
}

export const mediaService = new MediaService();
