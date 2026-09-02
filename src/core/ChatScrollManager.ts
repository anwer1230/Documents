/**
 * ChatScrollManager.ts
 * Replicates DrKLO/Telegram Android ChatActivity scroll position restoration mechanism:
 * - firstVisibleItemPosition / firstVisibleItemIndex
 * - topOffset (exact pixel offset of the top item relative to container)
 * - isNearBottom
 * - visibleItemsCount
 * - lastViewedMessageId
 */

import { MessagesStorage } from './MessagesStorage';

export interface ChatScrollState {
  chatId: string;
  scrollTop: number;
  scrollHeight: number;
  clientHeight: number;
  firstVisibleMessageId?: string;
  topOffset: number;
  firstVisibleItemPosition: number;
  isNearBottom: boolean;
  visibleCount: number;
  savedTimestamp: number;
}

export class ChatScrollManager {
  private static instance: ChatScrollManager;
  private scrollStates: Map<string, ChatScrollState> = new Map();
  private currentAccount: number = 0;

  public static getInstance(currentAccount: number = 0): ChatScrollManager {
    if (!ChatScrollManager.instance) {
      ChatScrollManager.instance = new ChatScrollManager(currentAccount);
    }
    return ChatScrollManager.instance;
  }

  constructor(currentAccount: number = 0) {
    this.currentAccount = currentAccount;
  }

  /**
   * Saves current scroll metrics for a chat (equivalent to ChatActivity.onPause / saveScrollPosition)
   */
  public saveScrollPosition(
    chatId: string,
    container: HTMLElement | null,
    visibleCount: number,
    isNearBottom: boolean
  ): void {
    if (!chatId || !container) return;

    const scrollTop = container.scrollTop;
    const scrollHeight = container.scrollHeight;
    const clientHeight = container.clientHeight;

    // Find the first visible message element
    const messageElements = container.querySelectorAll<HTMLElement>('[data-message-id]');
    let firstVisibleMessageId: string | undefined;
    let topOffset = 0;
    let firstVisibleItemPosition = 0;

    const containerRect = container.getBoundingClientRect();

    for (let i = 0; i < messageElements.length; i++) {
      const el = messageElements[i];
      const rect = el.getBoundingClientRect();
      // First element whose bottom edge is below container top
      if (rect.bottom >= containerRect.top) {
        firstVisibleMessageId = el.getAttribute('data-message-id') || undefined;
        topOffset = rect.top - containerRect.top;
        firstVisibleItemPosition = i;
        break;
      }
    }

    const state: ChatScrollState = {
      chatId,
      scrollTop,
      scrollHeight,
      clientHeight,
      firstVisibleMessageId,
      topOffset,
      firstVisibleItemPosition,
      isNearBottom,
      visibleCount,
      savedTimestamp: Date.now(),
    };

    this.scrollStates.set(chatId, state);

    // Save to MessagesStorage for SQLite/session persistence
    try {
      MessagesStorage.getInstance(this.currentAccount).saveChatScrollPosition(
        chatId,
        firstVisibleItemPosition,
        topOffset,
        firstVisibleMessageId || 0,
        isNearBottom
      );
    } catch (e) {}
  }

  /**
   * Retrieves saved scroll position for a chat
   */
  public getScrollPosition(chatId: string): ChatScrollState | undefined {
    let state = this.scrollStates.get(chatId);
    if (!state) {
      // Try restoring from MessagesStorage
      const persistent = MessagesStorage.getInstance(this.currentAccount).getChatScrollPosition(chatId);
      if (persistent) {
        state = {
          chatId,
          scrollTop: 0,
          scrollHeight: 0,
          clientHeight: 0,
          firstVisibleMessageId: persistent.messageId !== '0' ? persistent.messageId : undefined,
          topOffset: persistent.topOffset || 0,
          firstVisibleItemPosition: persistent.position || 0,
          isNearBottom: persistent.isAtBottom,
          visibleCount: 30,
          savedTimestamp: Date.now(),
        };
        this.scrollStates.set(chatId, state);
      }
    }
    return state;
  }

  /**
   * Clears saved scroll position (e.g. when chat is deleted)
   */
  public clearScrollPosition(chatId: string): void {
    this.scrollStates.delete(chatId);
  }

  /**
   * Restores scroll position in DOM container (equivalent to ChatActivity.scrollToPositionWithOffset)
   */
  public restoreScroll(
    chatId: string,
    container: HTMLElement | null,
    fallbackToBottom: boolean = true
  ): boolean {
    if (!chatId || !container) return false;

    const state = this.getScrollPosition(chatId);
    if (!state) {
      if (fallbackToBottom) {
        container.scrollTop = container.scrollHeight;
      }
      return false;
    }

    if (state.isNearBottom) {
      container.scrollTop = container.scrollHeight;
      return true;
    }

    // Try restoring by target message element if available
    if (state.firstVisibleMessageId) {
      const el = container.querySelector<HTMLElement>(`[data-message-id="${state.firstVisibleMessageId}"]`);
      if (el) {
        const containerRect = container.getBoundingClientRect();
        const elRect = el.getBoundingClientRect();
        const currentRelTop = elRect.top - containerRect.top;
        const diff = currentRelTop - state.topOffset;
        container.scrollTop += diff;
        return true;
      }
    }

    // Fallback: restore exact scrollTop with offset correction if scrollHeight matches or changed
    if (state.scrollHeight > 0 && container.scrollHeight > 0) {
      const heightDiff = container.scrollHeight - state.scrollHeight;
      container.scrollTop = Math.max(0, state.scrollTop + heightDiff);
    } else if (state.scrollTop > 0) {
      container.scrollTop = state.scrollTop;
    } else {
      container.scrollTop = container.scrollHeight;
    }

    return true;
  }
}

export const chatScrollManager = ChatScrollManager.getInstance();
