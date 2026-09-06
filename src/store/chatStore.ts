/**
 * chatStore.ts
 * Manages chat scroll positions, read message persistence, and smart auto-scrolling
 * Replicates official Telegram scroll behavior:
 * - Stores lastReadPositions (chatId -> { lastReadMessageId, scrollTop, scrollHeight, isNearBottom, lastUpdated }) in localStorage
 * - When opening a chat for the first time: immediately scrolls to bottom (scrollToBottom)
 * - When returning to a chat: navigates to lastReadMessageId / last saved reading position, never jumping to top
 * - Smart scroll on new messages: smooth scroll to bottom if near bottom or outgoing, preserve reading offset if scrolled up
 * - Live update on scroll: updates lastReadMessageId and scroll position dynamically
 */

import { Chat, Message } from '../types';
import { telegramDB } from '../utils/sqliteStorage';

export interface ChatReadPosition {
  chatId: string;
  lastReadMessageId?: string;
  scrollTop: number;
  scrollHeight: number;
  isNearBottom: boolean;
  lastUpdated: number;
}

export interface InSessionScrollState extends ChatReadPosition {}

export class ChatStore {
  private static instance: ChatStore;

  // Persistent Record<string, number> for last read message IDs / scroll positions (as requested)
  public ScrollPositions: Record<string, number> = {};

  // Persistent rich last read positions per chat (chatId -> ChatReadPosition)
  public lastReadPositions: Record<string, ChatReadPosition> = {};

  // In-memory session scroll map
  private sessionScrollMap: Map<string, InSessionScrollState> = new Map();

  // Tracks chats visited during the current session
  private visitedChatsInCurrentSession: Set<string> = new Set();

  // Threshold in pixels to consider user "at bottom"
  private readonly NEAR_BOTTOM_THRESHOLD = 140;
  private readonly STORAGE_KEY = 'tg_last_read_positions';
  private readonly SCROLL_POSITIONS_KEY = 'tg_scroll_positions';

  // Offline-First Cache storage keys
  private readonly CHATS_STORAGE_KEY = 'tg_offline_cached_chats_v1';
  private readonly MESSAGES_STORAGE_PREFIX = 'tg_offline_cached_msgs_';
  private readonly MESSAGES_INDEX_KEY = 'tg_offline_cached_chat_ids_v1';

  // Synchronous in-memory caches to guarantee ZERO white screens on startup
  private cachedChats: Chat[] = [];
  private cachedMessages: Map<string, Message[]> = new Map();
  private cachedChatIds: Set<string> = new Set();

  constructor() {
    this.initFromStorage();
  }

  public static getInstance(): ChatStore {
    if (!ChatStore.instance) {
      ChatStore.instance = new ChatStore();
    }
    return ChatStore.instance;
  }

  /**
   * Load persistent scroll positions & offline-cached chats and messages from localStorage
   */
  private initFromStorage(): void {
    if (typeof window === 'undefined') return;
    try {
      // 1. Load tg_scroll_positions (chatId -> number)
      const storedPositions = localStorage.getItem(this.SCROLL_POSITIONS_KEY);
      if (storedPositions) {
        const parsedPositions = JSON.parse(storedPositions);
        if (parsedPositions && typeof parsedPositions === 'object') {
          this.ScrollPositions = parsedPositions;
        }
      }

      // 2. Load rich lastReadPositions
      const stored = localStorage.getItem(this.STORAGE_KEY);
      if (stored) {
        const parsed = JSON.parse(stored);
        if (parsed && typeof parsed === 'object') {
          this.lastReadPositions = parsed;
          // Synchronize to session map as well
          Object.values(parsed).forEach((item: any) => {
            if (item && item.chatId) {
              this.sessionScrollMap.set(item.chatId, item);
              if (item.lastReadMessageId && !this.ScrollPositions[item.chatId]) {
                const numericId = Number(item.lastReadMessageId);
                if (!isNaN(numericId)) {
                  this.ScrollPositions[item.chatId] = numericId;
                }
              }
            }
          });
        }
      }

      // 3. Load offline-cached chats
      const storedChats = localStorage.getItem(this.CHATS_STORAGE_KEY);
      if (storedChats) {
        try {
          const parsedChats = JSON.parse(storedChats);
          if (Array.isArray(parsedChats) && parsedChats.length > 0) {
            this.cachedChats = parsedChats;
          }
        } catch (e) {
          console.warn('[chatStore] Error parsing cached chats:', e);
        }
      }

      // 4. Load offline-cached chat message IDs index
      const storedIndex = localStorage.getItem(this.MESSAGES_INDEX_KEY);
      if (storedIndex) {
        try {
          const parsedIndex = JSON.parse(storedIndex);
          if (Array.isArray(parsedIndex)) {
            this.cachedChatIds = new Set(parsedIndex);
            // Pre-load cached messages for quick access
            for (const cId of parsedIndex) {
              const msgKey = this.MESSAGES_STORAGE_PREFIX + cId;
              const rawMsgs = localStorage.getItem(msgKey);
              if (rawMsgs) {
                try {
                  const msgs = JSON.parse(rawMsgs);
                  if (Array.isArray(msgs)) {
                    this.cachedMessages.set(cId, msgs);
                  }
                } catch {}
              }
            }
          }
        } catch (e) {
          console.warn('[chatStore] Error parsing cached message index:', e);
        }
      }
    } catch (err) {
      console.warn('[chatStore] Error reading positions from localStorage:', err);
    }
  }

  /**
   * Persist both ScrollPositions and lastReadPositions safely to localStorage
   */
  private persistToStorage(): void {
    if (typeof window === 'undefined') return;
    try {
      localStorage.setItem(this.SCROLL_POSITIONS_KEY, JSON.stringify(this.ScrollPositions));
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(this.lastReadPositions));
    } catch (err) {
      console.warn('[chatStore] Error saving positions to localStorage:', err);
    }
  }

  // ==========================================
  // OFFLINE-FIRST CACHE OPS FOR CHATS & MESSAGES
  // ==========================================

  /**
   * Save chats list immediately to memory, localStorage, and IndexedDB SQLite
   */
  public saveChats(chats: Chat[]): void {
    if (!Array.isArray(chats) || chats.length === 0) return;
    this.cachedChats = chats;
    if (typeof window !== 'undefined') {
      try {
        localStorage.setItem(this.CHATS_STORAGE_KEY, JSON.stringify(chats));
      } catch (e) {
        console.warn('[chatStore] Error saving cached chats to localStorage:', e);
      }
    }
    try {
      telegramDB.saveChats(chats);
    } catch {}
  }

  /**
   * Retrieve cached chats synchronously (Zero-latency cache first)
   */
  public getCachedChats(): Chat[] {
    if (this.cachedChats && this.cachedChats.length > 0) {
      return this.cachedChats;
    }
    if (typeof window !== 'undefined') {
      try {
        const stored = localStorage.getItem(this.CHATS_STORAGE_KEY);
        if (stored) {
          const parsed = JSON.parse(stored);
          if (Array.isArray(parsed) && parsed.length > 0) {
            this.cachedChats = parsed;
            return parsed;
          }
        }
      } catch {}
    }
    try {
      const sqliteChats = telegramDB.getChats();
      if (sqliteChats && sqliteChats.length > 0) {
        this.cachedChats = sqliteChats;
        return sqliteChats;
      }
    } catch {}
    return [];
  }

  /**
   * Save messages list for a chat immediately to memory, localStorage, and IndexedDB SQLite
   */
  public saveMessages(chatId: string, messages: Message[]): void {
    if (!chatId || !Array.isArray(messages)) return;
    this.cachedMessages.set(chatId, messages);
    this.cachedChatIds.add(chatId);

    if (typeof window !== 'undefined') {
      try {
        localStorage.setItem(this.MESSAGES_STORAGE_PREFIX + chatId, JSON.stringify(messages));
        localStorage.setItem(this.MESSAGES_INDEX_KEY, JSON.stringify(Array.from(this.cachedChatIds)));
      } catch (e) {
        console.warn(`[chatStore] Error saving cached messages for ${chatId}:`, e);
      }
    }
    try {
      telegramDB.saveMessages(messages);
    } catch {}
  }

  /**
   * Save or update a single message immediately to memory, localStorage, and IndexedDB SQLite
   */
  public saveMessage(chatId: string, message: Message): void {
    if (!chatId || !message || !message.id) return;
    const existing = this.getCachedMessages(chatId);
    const existsIndex = existing.findIndex((m) => m.id === message.id);
    let updated: Message[];
    if (existsIndex >= 0) {
      updated = [...existing];
      updated[existsIndex] = message;
    } else {
      updated = [...existing, message];
    }
    this.saveMessages(chatId, updated);
    try {
      telegramDB.saveMessage(message);
    } catch {}
  }

  /**
   * Retrieve cached messages for a chat synchronously (Zero-latency cache first)
   */
  public getCachedMessages(chatId: string): Message[] {
    if (!chatId) return [];
    if (this.cachedMessages.has(chatId)) {
      const msgs = this.cachedMessages.get(chatId)!;
      if (msgs.length > 0) return msgs;
    }
    if (typeof window !== 'undefined') {
      try {
        const stored = localStorage.getItem(this.MESSAGES_STORAGE_PREFIX + chatId);
        if (stored) {
          const parsed = JSON.parse(stored);
          if (Array.isArray(parsed) && parsed.length > 0) {
            this.cachedMessages.set(chatId, parsed);
            this.cachedChatIds.add(chatId);
            return parsed;
          }
        }
      } catch {}
    }
    try {
      const sqliteMsgs = telegramDB.getMessagesForChat(chatId);
      if (sqliteMsgs && sqliteMsgs.length > 0) {
        this.cachedMessages.set(chatId, sqliteMsgs);
        return sqliteMsgs;
      }
    } catch {}
    return [];
  }

  /**
   * Get all cached messages across all chats synchronously
   */
  public getAllCachedMessages(): Record<string, Message[]> {
    const result: Record<string, Message[]> = {};
    for (const [cId, msgs] of this.cachedMessages.entries()) {
      if (msgs && msgs.length > 0) {
        result[cId] = msgs;
      }
    }
    for (const cId of this.cachedChatIds) {
      if (!result[cId]) {
        const msgs = this.getCachedMessages(cId);
        if (msgs.length > 0) {
          result[cId] = msgs;
        }
      }
    }
    return result;
  }

  /**
   * Get the saved scroll position number from ScrollPositions
   */
  public getScrollPosition(chatId: string): number | undefined {
    if (!chatId) return undefined;
    return this.ScrollPositions[chatId];
  }

  /**
   * Set the saved scroll position number in ScrollPositions
   */
  public setScrollPosition(chatId: string, position: number): void {
    if (!chatId || position === undefined || isNaN(position)) return;
    this.ScrollPositions[chatId] = position;
    this.persistToStorage();
  }

  /**
   * Check if a chat exists in lastReadPositions or ScrollPositions
   */
  public hasLastReadPosition(chatId: string): boolean {
    if (!chatId) return false;
    return Boolean(this.lastReadPositions[chatId] || this.ScrollPositions[chatId] !== undefined);
  }

  /**
   * Get the saved read position for a chat
   */
  public getLastReadPosition(chatId: string): ChatReadPosition | undefined {
    if (!chatId) return undefined;
    return this.lastReadPositions[chatId];
  }

  /**
   * Get the last read message ID for a chat
   */
  public getLastReadMessageId(chatId: string): string | undefined {
    return this.getLastReadPosition(chatId)?.lastReadMessageId;
  }

  /**
   * Check if a chat was already visited in the current session
   */
  public hasVisitedInCurrentSession(chatId: string): boolean {
    return this.visitedChatsInCurrentSession.has(chatId);
  }

  /**
   * Mark a chat as opened/visited in the current session
   */
  public markChatVisitedInCurrentSession(chatId: string): void {
    if (!chatId) return;
    this.visitedChatsInCurrentSession.add(chatId);
  }

  /**
   * Saves the scroll & last-read position for a chat
   */
  public saveLastReadPosition(
    chatId: string,
    data: {
      lastReadMessageId?: string;
      scrollTop?: number;
      scrollHeight?: number;
      isNearBottom?: boolean;
    }
  ): void {
    if (!chatId) return;

    const existing = this.lastReadPositions[chatId];
    const isNearBottom = data.isNearBottom !== undefined ? data.isNearBottom : (existing?.isNearBottom ?? true);
    const scrollTop = data.scrollTop !== undefined ? data.scrollTop : (existing?.scrollTop ?? 0);
    const scrollHeight = data.scrollHeight !== undefined ? data.scrollHeight : (existing?.scrollHeight ?? 0);
    const lastReadMessageId = data.lastReadMessageId !== undefined ? data.lastReadMessageId : existing?.lastReadMessageId;

    const updated: ChatReadPosition = {
      chatId,
      lastReadMessageId,
      scrollTop,
      scrollHeight,
      isNearBottom,
      lastUpdated: Date.now(),
    };

    this.lastReadPositions[chatId] = updated;
    this.sessionScrollMap.set(chatId, updated);
    this.visitedChatsInCurrentSession.add(chatId);

    // Also update ScrollPositions mapping
    if (lastReadMessageId) {
      const numId = Number(lastReadMessageId);
      this.ScrollPositions[chatId] = !isNaN(numId) ? numId : scrollTop;
    } else if (scrollTop > 0) {
      this.ScrollPositions[chatId] = scrollTop;
    }

    this.persistToStorage();
  }

  /**
   * Updates last read message ID specifically
   */
  public updateLastReadMessageId(
    chatId: string,
    messageId: string,
    isNearBottom: boolean = false,
    scrollTop: number = 0,
    scrollHeight: number = 0
  ): void {
    this.saveLastReadPosition(chatId, {
      lastReadMessageId: messageId,
      isNearBottom,
      scrollTop,
      scrollHeight,
    });
  }

  /**
   * Legacy & in-session compatibility method
   */
  public saveSessionScrollPosition(
    chatId: string,
    scrollTop: number,
    scrollHeight: number,
    isNearBottom: boolean,
    lastReadMessageId?: string
  ): void {
    this.saveLastReadPosition(chatId, {
      scrollTop,
      scrollHeight,
      isNearBottom,
      lastReadMessageId,
    });
  }

  /**
   * Returns saved position for a chat
   */
  public getSessionScrollPosition(chatId: string): InSessionScrollState | null {
    if (!chatId) return null;
    return this.getLastReadPosition(chatId) || this.sessionScrollMap.get(chatId) || null;
  }

  /**
   * Clears saved scroll position for a chat or all chats
   */
  public clearSessionScroll(chatId?: string): void {
    if (chatId) {
      delete this.lastReadPositions[chatId];
      delete this.ScrollPositions[chatId];
      this.sessionScrollMap.delete(chatId);
      this.visitedChatsInCurrentSession.delete(chatId);
    } else {
      this.lastReadPositions = {};
      this.ScrollPositions = {};
      this.sessionScrollMap.clear();
      this.visitedChatsInCurrentSession.clear();
    }
    this.persistToStorage();
  }

  /**
   * Determines if container scroll offset is near the bottom
   */
  public isNearBottom(container: HTMLElement | null, threshold = this.NEAR_BOTTOM_THRESHOLD): boolean {
    if (!container) return true;
    const distanceToBottom = container.scrollHeight - container.scrollTop - container.clientHeight;
    return distanceToBottom <= threshold;
  }

  /**
   * Smart scroll helper: scrolls to bottom if user was near bottom or outgoing message
   */
  public smartScrollToBottom(
    container: HTMLElement | null,
    isOutgoing: boolean = false,
    force: boolean = false
  ): boolean {
    if (!container) return false;
    const nearBottom = this.isNearBottom(container);
    if (force || isOutgoing || nearBottom) {
      container.scrollTop = container.scrollHeight;
      return true;
    }
    return false;
  }
}

export const chatStore = ChatStore.getInstance();

