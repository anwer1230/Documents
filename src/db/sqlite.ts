import { sqliteStorage } from '../utils/sqliteStorage';
import { DEFAULT_SETTINGS } from '../types/settings';

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

  // إذا لم توجد إعدادات، أنشئ القيم الافتراضية محلياً دون استيراد دائري
  try {
    const existing = await db.get(`SELECT value FROM settings WHERE key = 'app_settings'`);
    if (!existing || !existing.value) {
      const json = JSON.stringify(DEFAULT_SETTINGS);
      await db.run(
        `INSERT OR REPLACE INTO settings (key, value) VALUES ('app_settings', ?)`,
        [json]
      );
    }
  } catch (err) {
    console.warn('[initDB] Failed seeding default settings in sqlite:', err);
  }
};
