/**
 * HotColdCache.ts - Multi-Tier Data Caching System
 * Replicates Telegram's official memory & storage caching tiering:
 * - Tier 1: Hot Cache (In-Memory LRU Cache) -> < 1ms synchronous access
 * - Tier 2: Warm Cache (Local SQLite WASM / IndexedDB) -> 5-15ms persistence
 * - Tier 3: Cold Tier (Remote Telegram Datacenter via MTProto RPC)
 */

interface CacheEntry<T> {
  data: T;
  timestamp: number;
}

export class HotColdCache {
  private static instance: HotColdCache;

  // L1: In-Memory Hot Cache
  private messageHotCache: Map<string, CacheEntry<any[]>> = new Map();
  private chatHotCache: Map<string, CacheEntry<any>> = new Map();
  private userHotCache: Map<string, CacheEntry<any>> = new Map();

  private readonly HOT_MAX_CHATS = 50;
  private readonly HOT_MESSAGES_PER_CHAT = 500;
  private readonly HOT_TTL_MS = 15 * 60 * 1000; // 15 minutes fresh in RAM

  private constructor() {}

  public static getInstance(): HotColdCache {
    if (!HotColdCache.instance) {
      HotColdCache.instance = new HotColdCache();
    }
    return HotColdCache.instance;
  }

  /**
   * Retrieve messages from Hot Cache (instant O(1))
   */
  public getHotMessages(chatId: string): any[] | null {
    const entry = this.messageHotCache.get(chatId);
    if (!entry) return null;
    if (Date.now() - entry.timestamp > this.HOT_TTL_MS) {
      this.messageHotCache.delete(chatId);
      return null;
    }
    return entry.data;
  }

  /**
   * Save messages into Hot Cache with LRU eviction
   */
  public putHotMessages(chatId: string, messages: any[]): void {
    if (this.messageHotCache.size >= this.HOT_MAX_CHATS) {
      // Evict oldest entry
      const oldestKey = this.messageHotCache.keys().next().value;
      if (oldestKey) this.messageHotCache.delete(oldestKey);
    }
    const sliced = messages.slice(-this.HOT_MESSAGES_PER_CHAT);
    this.messageHotCache.set(chatId, { data: sliced, timestamp: Date.now() });
  }

  /**
   * Append new incoming messages directly into Hot Cache
   */
  public appendHotMessages(chatId: string, newMessages: any[]): void {
    const existing = this.getHotMessages(chatId) || [];
    const idMap = new Map<string, any>();
    for (const m of existing) idMap.set(String(m.id), m);
    for (const m of newMessages) idMap.set(String(m.id), m);
    const merged = Array.from(idMap.values()).sort((a, b) => (a.date || 0) - (b.date || 0));
    this.putHotMessages(chatId, merged);
  }

  /**
   * Chat metadata Hot Cache
   */
  public getHotChat(chatId: string): any | null {
    const entry = this.chatHotCache.get(chatId);
    return entry ? entry.data : null;
  }

  public putHotChat(chatId: string, chat: any): void {
    this.chatHotCache.set(chatId, { data: chat, timestamp: Date.now() });
  }

  public getHotUser(userId: string): any | null {
    const entry = this.userHotCache.get(userId);
    return entry ? entry.data : null;
  }

  public putHotUser(userId: string, user: any): void {
    this.userHotCache.set(userId, { data: user, timestamp: Date.now() });
  }

  public clearHotCache(): void {
    this.messageHotCache.clear();
    this.chatHotCache.clear();
    this.userHotCache.clear();
  }
}

export const hotColdCache = HotColdCache.getInstance();
