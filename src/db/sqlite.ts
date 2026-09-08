import { sqliteStorage } from '../utils/sqliteStorage';
import { DEFAULT_SETTINGS, AppSettings } from '../types/settings';
import { loadSettingsFromSQLite, saveSettingsToSQLite } from '../services/settingsDB';

export const db = {
  run: async (sql: string, params?: any[]): Promise<void> => {
    await sqliteStorage.run(sql, params);
  },
  get: async (sql: string, params?: any[]): Promise<any | null> => {
    return await sqliteStorage.get(sql, params);
  },
};

export const initDB = async () => {
  await sqliteStorage.init();
  await db.run(`
    CREATE TABLE IF NOT EXISTS settings (
      key TEXT PRIMARY KEY,
      value TEXT
    )
  `);

  // إذا لم توجد إعدادات، أنشئ القيم الافتراضية
  const existing = await loadSettingsFromSQLite();
  if (!existing) {
    await saveSettingsToSQLite(DEFAULT_SETTINGS);
  }
};
