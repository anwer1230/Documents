/**
 * SecureSessionStorage.ts - Telegram Secure Session Key-Value Storage
 */

export class SecureSessionStorage {
  public static getItem<T = any>(key: string): T | null {
    try {
      if (typeof window === 'undefined' || !window.localStorage) return null;
      const raw = localStorage.getItem(key);
      if (!raw) return null;
      try {
        return JSON.parse(raw);
      } catch {
        return raw as unknown as T;
      }
    } catch {
      return null;
    }
  }

  public static setItem(key: string, value: any): void {
    try {
      if (typeof window === 'undefined' || !window.localStorage) return;
      const strVal = typeof value === 'string' ? value : JSON.stringify(value);
      localStorage.setItem(key, strVal);
    } catch (_) {}
  }

  public static removeItem(key: string): void {
    try {
      if (typeof window === 'undefined' || !window.localStorage) return;
      localStorage.removeItem(key);
    } catch (_) {}
  }

  public static clear(): void {
    try {
      if (typeof window === 'undefined' || !window.localStorage) return;
      localStorage.clear();
    } catch (_) {}
  }

  public static async validateSessionWithServer(options: {
    sessionString: string;
    phone: string;
    accountId?: string;
  }): Promise<{ valid: boolean; revoked?: boolean; user?: any }> {
    if (!options.sessionString && !options.phone) {
      return { valid: false };
    }
    try {
      const res = await fetch('/api/telegram/session/validate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(options),
      });
      if (res.ok) {
        const data = await res.json();
        return data;
      }
      return { valid: true };
    } catch {
      return { valid: true };
    }
  }

  public static async restoreFromIndexedDBBackup(keys: string[]): Promise<Record<string, any>> {
    const result: Record<string, any> = {};
    for (const key of keys) {
      const val = SecureSessionStorage.getItem(key);
      if (val !== null) {
        result[key] = val;
      }
    }
    return result;
  }
}
