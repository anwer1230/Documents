/**
 * Telegram Web K In-Memory LRU & Cache Layer (cache.ts)
 * Based on morethanwords/tweb src/lib/storages/cache.ts
 */

interface CacheEntry<T> {
  value: T;
  expiresAt?: number;
  lastAccessed: number;
}

export class AppCacheStorage {
  private cache: Map<string, CacheEntry<any>> = new Map();
  private maxItems: number;

  constructor(maxItems = 1000) {
    this.maxItems = maxItems;
  }

  public set<T>(key: string, value: T, ttlMs?: number): void {
    if (this.cache.size >= this.maxItems) {
      this.evictLru();
    }
    const expiresAt = ttlMs ? Date.now() + ttlMs : undefined;
    this.cache.set(key, {
      value,
      expiresAt,
      lastAccessed: Date.now(),
    });
  }

  public get<T>(key: string): T | null {
    const entry = this.cache.get(key);
    if (!entry) return null;

    if (entry.expiresAt && Date.now() > entry.expiresAt) {
      this.cache.delete(key);
      return null;
    }

    entry.lastAccessed = Date.now();
    return entry.value as T;
  }

  public has(key: string): boolean {
    return this.get(key) !== null;
  }

  public delete(key: string): boolean {
    return this.cache.delete(key);
  }

  public clear(): void {
    this.cache.clear();
  }

  private evictLru(): void {
    let oldestKey: string | null = null;
    let oldestTime = Infinity;

    for (const [key, entry] of this.cache.entries()) {
      if (entry.lastAccessed < oldestTime) {
        oldestTime = entry.lastAccessed;
        oldestKey = key;
      }
    }

    if (oldestKey) {
      this.cache.delete(oldestKey);
    }
  }
}

export const appCache = new AppCacheStorage(2000);
export default appCache;
