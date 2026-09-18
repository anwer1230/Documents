/**
 * In-Memory & Redis-Compatible High-Performance Tier-1 Hot Message Cache
 * Provides sub-millisecond access for recent chat messages, LRU eviction, and automatic TTL expiration.
 */

interface CacheEntry<T> {
  data: T;
  expiresAt: number;
  lastAccessed: number;
}

export class RedisCacheService {
  private cache: Map<string, CacheEntry<any>> = new Map();
  private maxEntries: number = 1000;
  private defaultTTLMs: number = 5 * 60 * 1000; // 5 minutes
  private hits: number = 0;
  private misses: number = 0;
  private evictions: number = 0;

  constructor(maxEntries: number = 1000) {
    this.maxEntries = maxEntries;

    // Periodic sweep for expired entries every 60 seconds
    setInterval(() => {
      this.purgeExpired();
    }, 60000).unref?.();
  }

  public async getHotMessages(key: string): Promise<any[] | null> {
    const entry = this.cache.get(key);
    if (!entry) {
      this.misses++;
      return null;
    }

    if (Date.now() > entry.expiresAt) {
      this.cache.delete(key);
      this.misses++;
      return null;
    }

    entry.lastAccessed = Date.now();
    this.hits++;
    return entry.data;
  }

  public async setHotMessages(key: string, messages: any[], ttlSeconds: number = 300): Promise<void> {
    // Evict least recently used if at capacity
    if (this.cache.size >= this.maxEntries && !this.cache.has(key)) {
      this.evictLRU();
    }

    this.cache.set(key, {
      data: messages,
      expiresAt: Date.now() + ttlSeconds * 1000,
      lastAccessed: Date.now(),
    });
  }

  public async invalidateChat(key: string): Promise<void> {
    this.cache.delete(key);
  }

  public async clearAll(): Promise<void> {
    this.cache.clear();
    this.hits = 0;
    this.misses = 0;
    this.evictions = 0;
  }

  public getStats(): {
    hits: number;
    misses: number;
    hitRatio: string;
    keysCount: number;
    evictions: number;
    status: string;
    engine: string;
  } {
    const total = this.hits + this.misses;
    const ratio = total > 0 ? ((this.hits / total) * 100).toFixed(1) + '%' : '0%';
    return {
      hits: this.hits,
      misses: this.misses,
      hitRatio: ratio,
      keysCount: this.cache.size,
      evictions: this.evictions,
      status: 'online',
      engine: 'In-Memory Redis-Compatible L1',
    };
  }

  private purgeExpired(): void {
    const now = Date.now();
    for (const [key, entry] of this.cache.entries()) {
      if (now > entry.expiresAt) {
        this.cache.delete(key);
      }
    }
  }

  private evictLRU(): void {
    let oldestKey: string | null = null;
    let oldestAccess = Infinity;

    for (const [key, entry] of this.cache.entries()) {
      if (entry.lastAccessed < oldestAccess) {
        oldestAccess = entry.lastAccessed;
        oldestKey = key;
      }
    }

    if (oldestKey) {
      this.cache.delete(oldestKey);
      this.evictions++;
    }
  }
}

export const redisCache = new RedisCacheService(2000);
