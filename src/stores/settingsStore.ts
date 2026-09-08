import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import { AppSettings, DEFAULT_SETTINGS } from '../types/settings';
import { saveSettingsToSQLite, loadSettingsFromSQLite } from '../services/settingsDB';
import { audioService } from '../services/audioService';
import { telegramAudio } from '../utils/audioNotification';

export const applyTheme = (theme: 'light' | 'dark') => {
  if (typeof document === 'undefined') return;
  document.documentElement.setAttribute('data-theme', theme);
  if (theme === 'dark') {
    document.documentElement.classList.add('theme-dark');
    document.documentElement.classList.remove('theme-day');
  } else {
    document.documentElement.classList.add('theme-day');
    document.documentElement.classList.remove('theme-dark');
  }
  try {
    localStorage.setItem('theme', theme);
  } catch (_) {}
};

export const applySettings = (settings: AppSettings) => {
  if (settings.theme) {
    applyTheme(settings.theme);
  }
  if (typeof settings.soundVolume === 'number') {
    audioService.setVolume(settings.soundVolume);
    telegramAudio.setVolume(settings.soundVolume);
  }
  if (typeof settings.muteChatSounds === 'boolean') {
    audioService.setMuted(settings.muteChatSounds);
    telegramAudio.setMuted(settings.muteChatSounds);
  }
};

interface SettingsStore {
  settings: AppSettings;
  updateSettings: (newSettings: Partial<AppSettings>) => Promise<void>;
  loadSettings: () => Promise<void>;
  resetToDefault: () => Promise<void>;
}

export const useSettingsStore = create<SettingsStore>()(
  persist(
    (set, get) => ({
      settings: DEFAULT_SETTINGS,

      updateSettings: async (newSettings: Partial<AppSettings>) => {
        const updated = { ...get().settings, ...newSettings };
        set({ settings: updated });

        // حفظ في SQLite
        await saveSettingsToSQLite(updated);

        // تطبيق التغييرات فوراً (مثل تغيير الثيم)
        applySettings(updated);
      },

      loadSettings: async () => {
        const saved = await loadSettingsFromSQLite();
        if (saved) {
          set({ settings: saved });
          applySettings(saved);
        } else {
          applySettings(get().settings);
        }
      },

      resetToDefault: async () => {
        set({ settings: DEFAULT_SETTINGS });
        await saveSettingsToSQLite(DEFAULT_SETTINGS);
        applySettings(DEFAULT_SETTINGS);
      },
    }),
    {
      name: 'telegram-web-settings-storage',
      storage: createJSONStorage(() => localStorage),
    }
  )
);
