import { db } from '../db/sqlite';
import { AppSettings } from '../types/settings';

export const saveSettingsToSQLite = async (settings: AppSettings): Promise<void> => {
  try {
    const json = JSON.stringify(settings);
    await db.run(
      `INSERT OR REPLACE INTO settings (key, value) VALUES ('app_settings', ?)`,
      [json]
    );
    try {
      localStorage.setItem('telegram_web_settings', json);
    } catch (_) {}
  } catch (err) {
    console.warn('[settingsDB] Error saving settings to SQLite:', err);
    try {
      localStorage.setItem('telegram_web_settings', JSON.stringify(settings));
    } catch (_) {}
  }
};

export const loadSettingsFromSQLite = async (): Promise<AppSettings | null> => {
  try {
    const result = await db.get(
      `SELECT value FROM settings WHERE key = 'app_settings'`
    );
    if (result && result.value) {
      return typeof result.value === 'string' ? JSON.parse(result.value) : result.value;
    }
  } catch (err) {
    console.warn('[settingsDB] Error loading settings from SQLite:', err);
  }

  try {
    const local = localStorage.getItem('telegram_web_settings');
    if (local) {
      return JSON.parse(local);
    }
  } catch (_) {}

  return null;
};
