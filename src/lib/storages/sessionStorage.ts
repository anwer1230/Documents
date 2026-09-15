/**
 * Telegram Web K Session Storage Layer (sessionStorage.ts)
 * Based on morethanwords/tweb src/lib/storages/sessionStorage.ts
 */

export class AppSessionStorage {
  private memMap: Map<string, any> = new Map();

  public set(key: string, value: any): void {
    try {
      this.memMap.set(key, value);
      if (typeof window !== 'undefined' && window.sessionStorage) {
        window.sessionStorage.setItem(`tweb_${key}`, JSON.stringify(value));
      }
    } catch (e) {
      console.warn('[sessionStorage] fallback error:', e);
    }
  }

  public get<T = any>(key: string, defaultValue: T | null = null): T | null {
    if (this.memMap.has(key)) {
      return this.memMap.get(key) as T;
    }
    try {
      if (typeof window !== 'undefined' && window.sessionStorage) {
        const item = window.sessionStorage.getItem(`tweb_${key}`);
        if (item !== null) {
          const parsed = JSON.parse(item);
          this.memMap.set(key, parsed);
          return parsed as T;
        }
      }
    } catch {}
    return defaultValue;
  }

  public remove(key: string): void {
    this.memMap.delete(key);
    try {
      if (typeof window !== 'undefined' && window.sessionStorage) {
        window.sessionStorage.removeItem(`tweb_${key}`);
      }
    } catch {}
  }

  public clear(): void {
    this.memMap.clear();
    try {
      if (typeof window !== 'undefined' && window.sessionStorage) {
        Object.keys(window.sessionStorage)
          .filter(k => k.startsWith('tweb_'))
          .forEach(k => window.sessionStorage.removeItem(k));
      }
    } catch {}
  }
}

export const sessionStorage = new AppSessionStorage();
export default sessionStorage;
