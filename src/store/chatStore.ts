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

  // Persistent last read positions per chat (chatId -> ChatReadPosition)
  public lastReadPositions: Record<string, ChatReadPosition> = {};

  // In-memory session scroll map
  private sessionScrollMap: Map<string, InSessionScrollState> = new Map();

  // Tracks chats visited during the current session
  private visitedChatsInCurrentSession: Set<string> = new Set();

  // Threshold in pixels to consider user "at bottom"
  private readonly NEAR_BOTTOM_THRESHOLD = 140;
  private readonly STORAGE_KEY = 'tg_last_read_positions';

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
   * Load persistent scroll positions from localStorage
   */
  private initFromStorage(): void {
    if (typeof window === 'undefined') return;
    try {
      const stored = localStorage.getItem(this.STORAGE_KEY);
      if (stored) {
        const parsed = JSON.parse(stored);
        if (parsed && typeof parsed === 'object') {
          this.lastReadPositions = parsed;
          // Synchronize to session map as well
          Object.values(parsed).forEach((item: any) => {
            if (item && item.chatId) {
              this.sessionScrollMap.set(item.chatId, item);
            }
          });
        }
      }
    } catch (err) {
      console.warn('[chatStore] Error reading lastReadPositions from localStorage:', err);
    }
  }

  /**
   * Persist lastReadPositions safely to localStorage
   */
  private persistToStorage(): void {
    if (typeof window === 'undefined') return;
    try {
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(this.lastReadPositions));
    } catch (err) {
      console.warn('[chatStore] Error saving lastReadPositions to localStorage:', err);
    }
  }

  /**
   * Check if a chat exists in lastReadPositions
   */
  public hasLastReadPosition(chatId: string): boolean {
    if (!chatId) return false;
    return Boolean(this.lastReadPositions[chatId]);
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
      this.sessionScrollMap.delete(chatId);
      this.visitedChatsInCurrentSession.delete(chatId);
    } else {
      this.lastReadPositions = {};
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

