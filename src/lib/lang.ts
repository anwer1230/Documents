/**
 * Telegram Web K Localization & Language Engine (lang.ts)
 * Based on morethanwords/tweb src/lib/lang.ts
 */

import { EventEmitter } from './appManagers/utils';

export type SupportedLanguage = 'ar' | 'en';

const STRINGS: Record<SupportedLanguage, Record<string, string>> = {
  ar: {
    connecting: 'جاري الاتصال...',
    updating: 'جاري التحديث...',
    online: 'متصل الآن',
    offline: 'غير متصل',
    typing: 'يكتب...',
    recording_audio: 'يسجل صوتاً...',
    uploading_photo: 'يرفع صورة...',
    saved_messages: 'الرسائل المحفوظة',
    send: 'إرسال',
    cancel: 'إلغاء',
    delete: 'حذف',
    edit: 'تعديل',
    reply: 'رد',
    pin: 'تثبيت',
    unpin: 'إلغاء التثبيت',
    search: 'بحث',
    settings: 'الإعدادات',
  },
  en: {
    connecting: 'Connecting...',
    updating: 'Updating...',
    online: 'online',
    offline: 'offline',
    typing: 'is typing...',
    recording_audio: 'is recording audio...',
    uploading_photo: 'is uploading photo...',
    saved_messages: 'Saved Messages',
    send: 'Send',
    cancel: 'Cancel',
    delete: 'Delete',
    edit: 'Edit',
    reply: 'Reply',
    pin: 'Pin',
    unpin: 'Unpin',
    search: 'Search',
    settings: 'Settings',
  },
};

export class LangEngine extends EventEmitter {
  private currentLang: SupportedLanguage = 'ar';

  constructor() {
    super();
    if (typeof window !== 'undefined') {
      const saved = localStorage.getItem('tweb_lang') as SupportedLanguage;
      if (saved && (saved === 'ar' || saved === 'en')) {
        this.currentLang = saved;
      }
    }
  }

  public get(key: string, fallback?: string): string {
    const dict = STRINGS[this.currentLang];
    if (dict && dict[key]) return dict[key];
    const enDict = STRINGS.en;
    if (enDict && enDict[key]) return enDict[key];
    return fallback || key;
  }

  public setLanguage(lang: SupportedLanguage): void {
    if (this.currentLang !== lang) {
      this.currentLang = lang;
      if (typeof window !== 'undefined') {
        localStorage.setItem('tweb_lang', lang);
        document.documentElement.dir = lang === 'ar' ? 'rtl' : 'ltr';
        document.documentElement.lang = lang;
      }
      this.emit('language_change', lang);
    }
  }

  public getLanguage(): SupportedLanguage {
    return this.currentLang;
  }

  public isRtl(): boolean {
    return this.currentLang === 'ar';
  }
}

export const lang = new LangEngine();
export default lang;
