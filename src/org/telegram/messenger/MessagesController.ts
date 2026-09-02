/**
 * Telegram Official Messages Controller & Messages Adapter Architecture
 * Replicates org.telegram.messenger.MessagesController & org.telegram.ui.Adapters.MessagesAdapter from DrKLO/Telegram
 */

import { TLRPC } from '../tgnet/TLRPC';
import { NotificationCenter } from './NotificationCenter';
import { MessagesStorage } from './MessagesStorage';
import { UserConfig } from './UserConfig';
import { ConnectionsManager } from '../tgnet/ConnectionsManager';
import { AutoResponderEngine } from './AutoResponderEngine';
import { AutoJoinerBackend } from './AutoJoinerBackend';
import { ChatObject, ChatRestrictionState } from './ChatObject';

export interface ChatRestrictionInfo {
  canWrite: boolean;
  canSendMedia: boolean;
  canSendStickers: boolean;
  canSendPolls: boolean;
  canEmbedLinks: boolean;
  bannedUntil?: number;
  reason?: string;
  isBanned: boolean;
  isKicked: boolean;
}

export class MessagesController {
  private static instances: MessagesController[] = [];
  public readonly currentAccount: number;
  private dialogsMap: Map<string | number, any> = new Map();
  private messagesMap: Map<string | number, TLRPC.TL_message[]> = new Map();
  private restrictionsMap: Map<string | number, ChatRestrictionInfo> = new Map();
  private fullChatMap: Map<string | number, TLRPC.TL_chatFull> = new Map();
  private participantsMap: Map<string | number, TLRPC.TL_channelParticipant> = new Map();
  private monitoredKeywords: Set<string> = new Set();
  private isMonitoringEnabled: boolean = false;
  public loadingDialogs: boolean = false;

  private constructor(account: number) {
    this.currentAccount = account;
  }

  public static getInstance(account: number = UserConfig.selectedAccount): MessagesController {
    if (account < 0 || account >= UserConfig.MAX_ACCOUNT_COUNT) {
      account = 0;
    }
    if (!MessagesController.instances[account]) {
      MessagesController.instances[account] = new MessagesController(account);
    }
    return MessagesController.instances[account];
  }

  public setMonitoringKeywords(keywords: string[]): void {
    this.monitoredKeywords = new Set(keywords.map((k) => k.toLowerCase().trim()).filter(Boolean));
  }

  public setMonitoringEnabled(enabled: boolean): void {
    this.isMonitoringEnabled = enabled;
  }

  public getMonitoringKeywords(): string[] {
    return Array.from(this.monitoredKeywords);
  }

  public isMonitoringActive(): boolean {
    return this.isMonitoringEnabled;
  }

  /**
   * Load dialogs for current account from server / cache
   */
  public async loadDialogs(folderId: number = 0, offset: number = 0, limit: number = 50): Promise<any[]> {
    this.loadingDialogs = true;
    try {
      const res = await ConnectionsManager.getInstance(this.currentAccount).sendRequest('messages.getDialogs', {
        folder_id: folderId,
        offset_id: offset,
        limit,
      });

      if (res && res.dialogs) {
        for (const d of res.dialogs) {
          this.dialogsMap.set(d.id, d);
          MessagesStorage.getInstance(this.currentAccount).database.put('dialogs', d.id, d);
        }
        NotificationCenter.getInstance(this.currentAccount).postNotificationName(NotificationCenter.dialogsNeedReload);
        return res.dialogs;
      }
    } catch (e) {
      console.warn(`[MessagesController_${this.currentAccount}] loadDialogs error:`, e);
    } finally {
      this.loadingDialogs = false;
    }
    return Array.from(this.dialogsMap.values());
  }

  /**
   * Central ProcessUpdate Handler (MessagesController.java)
   */
  public async processUpdate(update: any): Promise<void> {
    if (!update) return;

    const msg = update.message || update;
    const text = (msg.message || msg.text || '').toLowerCase();
    const cid = msg.chat_id || msg.dialog_id || msg.peer_id;

    if (cid) {
      // Store in memory & MessagesStorage
      const list = this.messagesMap.get(cid) || [];
      const exists = list.some((m) => String(m.id) === String(msg.id));
      if (!exists) {
        list.push(msg);
        this.messagesMap.set(cid, list);
        MessagesStorage.getInstance(this.currentAccount).putMessages([msg]);
      }

      // Update dialog top_message
      let dialog = this.dialogsMap.get(cid);
      if (dialog) {
        dialog.last_message = msg;
        dialog.lastMsg = msg.text || msg.message;
        dialog.lastMsgDate = msg.date || Math.floor(Date.now() / 1000);
        if (!msg.out && !msg.from_me) {
          dialog.unread_count = (dialog.unread_count || 0) + 1;
        }
      }
    }

    // 1. Keywords Monitor -> Send alert to Saved Messages
    if (this.isMonitoringEnabled && text) {
      for (const kw of this.monitoredKeywords) {
        if (text.includes(kw)) {
          const myId = UserConfig.getInstance(this.currentAccount).getClientUserId();
          if (myId) {
            await ConnectionsManager.getInstance(this.currentAccount).sendRequest('sendMessage', {
              peer: myId,
              message: `🚨 [تنبيه رصد فوري]\nالكلمة المرصودة: #${kw}\nالمرسل: ${msg.sender_name || msg.from_id || 'مجهول'}\n\nالنص:\n${msg.message || msg.text}`,
            });
          }
          break;
        }
      }
    }

    // 2. Live AutoJoiner Radar
    if (text) {
      AutoJoinerBackend.getInstance().processLiveMessage(msg.message || msg.text);
    }

    // 3. Auto Responder Engine
    await AutoResponderEngine.getInstance().processIncomingMessage(msg);

    // 4. Update UI
    NotificationCenter.getInstance(this.currentAccount).postNotificationName(NotificationCenter.didReceivedNewMessages, msg);
    NotificationCenter.getInstance(this.currentAccount).postNotificationName(NotificationCenter.updateInterfaces);
  }

  /**
   * Exact Telegram Dialogs Sorting Algorithm (MessagesController.java)
   */
  public sortDialogs(dialogs: any[]): any[] {
    if (!dialogs || dialogs.length <= 1) return dialogs || [];

    return [...dialogs].sort((a, b) => {
      // 1. Pinned status check
      const aPinned = Boolean(a.pinned || a.is_pinned);
      const bPinned = Boolean(b.pinned || b.is_pinned);

      if (aPinned !== bPinned) {
        return aPinned ? -1 : 1;
      }

      // If both are pinned, sort by pinned index if available
      if (aPinned && bPinned) {
        if (typeof a.pinnedNum === 'number' && typeof b.pinnedNum === 'number') {
          return b.pinnedNum - a.pinnedNum;
        }
      }

      // 2. Effective timestamp sorting (latest message date vs system event)
      const aTime = this.getEffectiveDialogTime(a);
      const bTime = this.getEffectiveDialogTime(b);

      if (bTime !== aTime) {
        return bTime - aTime; // Descending
      }

      // 3. Tie-breaker: unread priority or fallback ID
      const aUnread = a.unread_count || a.unread || 0;
      const bUnread = b.unread_count || b.unread || 0;
      if (bUnread !== aUnread) {
        return bUnread - aUnread;
      }

      return String(b.id || '').localeCompare(String(a.id || ''));
    });
  }

  public getEffectiveDialogTime(dialog: any): number {
    let lastTime = 0;
    if (typeof dialog.lastMsgDate === 'number') {
      lastTime = dialog.lastMsgDate;
    } else if (typeof dialog.date === 'number') {
      lastTime = dialog.date;
    } else if (typeof dialog.last_message_date === 'number') {
      lastTime = dialog.last_message_date;
    } else if (typeof dialog.date === 'string') {
      lastTime = Math.floor(new Date(dialog.date).getTime() / 1000) || 0;
    }

    const sysTime = dialog.last_system_activity || dialog.lastSystemActivity || 0;
    if (sysTime > lastTime) {
      lastTime = sysTime;
    }

    return lastTime;
  }

  public sortMessages(messages: any[]): any[] {
    if (!messages || messages.length <= 1) return messages || [];

    return [...messages].sort((a, b) => {
      const aTime = typeof a.date === 'number' ? a.date : Math.floor(new Date(a.date || a.timestamp || 0).getTime() / 1000) || 0;
      const bTime = typeof b.date === 'number' ? b.date : Math.floor(new Date(b.date || b.timestamp || 0).getTime() / 1000) || 0;

      if (aTime !== bTime) {
        return aTime - bTime; // Ascending
      }

      const aId = Number(a.id) || 0;
      const bId = Number(b.id) || 0;
      return aId - bId;
    });
  }

  public async markDialogAsRead(dialogId: string | number, maxId: number = 0, isOutbox: boolean = false): Promise<void> {
    const dialog = this.dialogsMap.get(dialogId);
    if (dialog) {
      if (!isOutbox) {
        dialog.read_inbox_max_id = maxId || dialog.read_inbox_max_id || 0;
        dialog.unread_count = 0;
        dialog.unread = 0;
      }
    }

    await MessagesStorage.getInstance(this.currentAccount).updateDialogsWithReadFlags(dialogId, maxId);
    NotificationCenter.getInstance(this.currentAccount).postNotificationName(NotificationCenter.dialogsNeedReload);
    NotificationCenter.getInstance(this.currentAccount).postNotificationName(NotificationCenter.updateInterfaces);
  }

  public async pinDialog(dialogId: string | number, pin: boolean): Promise<void> {
    await MessagesStorage.getInstance(this.currentAccount).setDialogFlags(dialogId, { pinned: pin });
    NotificationCenter.getInstance(this.currentAccount).postNotificationName(NotificationCenter.dialogsNeedReload);
  }

  public async deleteDialog(dialogId: string | number): Promise<void> {
    this.dialogsMap.delete(dialogId);
    this.messagesMap.delete(dialogId);
    this.clearChatRestriction(dialogId);
    await MessagesStorage.getInstance(this.currentAccount).deleteDialog(dialogId);
  }

  public handleChatError(chatId: string | number, error: TLRPC.TL_error): { canRetry: boolean; userPrompt: string } {
    if (error.isChatWriteForbidden()) {
      this.restrictionsMap.set(chatId, {
        canWrite: false,
        canSendMedia: false,
        canSendStickers: false,
        canSendPolls: false,
        canEmbedLinks: false,
        isBanned: false,
        isKicked: false,
        reason: 'الكتابة في هذه القناة أو المجموعة مقيدة للمشرفين فقط أو تم قفل الإرسال حالياً.',
      });
      NotificationCenter.getInstance(this.currentAccount).postNotificationName(NotificationCenter.updateInterfaces);
      return { canRetry: false, userPrompt: 'غير مسموح بإرسال الرسائل في هذه المحادثة (CHAT_WRITE_FORBIDDEN)' };
    }

    if (error.isUserBanned()) {
      this.restrictionsMap.set(chatId, {
        canWrite: false,
        canSendMedia: false,
        canSendStickers: false,
        canSendPolls: false,
        canEmbedLinks: false,
        isBanned: true,
        isKicked: false,
        reason: 'تم حظرك أو تقييد صلاحياتك في هذه المجموعة من قبل المشرفين.',
      });
      NotificationCenter.getInstance(this.currentAccount).postNotificationName(NotificationCenter.updateInterfaces);
      return { canRetry: false, userPrompt: 'أنت محظور من المشاركة في هذه المحادثة (USER_BANNED)' };
    }

    if (error.isFlood()) {
      const seconds = error.getFloodWaitSeconds();
      return {
        canRetry: true,
        userPrompt: `يرجى الانتظار ${seconds} ثانية قبل إعادة الإرسال (FLOOD_WAIT_${seconds})`,
      };
    }

    return { canRetry: false, userPrompt: error.text || 'حدث خطأ غير متوقع' };
  }

  public getChatRestriction(chatId: string | number): ChatRestrictionInfo | null {
    return this.restrictionsMap.get(chatId) || null;
  }

  public clearChatRestriction(chatId: string | number): void {
    this.restrictionsMap.delete(chatId);
  }

  public getFullChat(chatId: string | number): TLRPC.TL_chatFull | null {
    return this.fullChatMap.get(chatId) || null;
  }

  public getChatParticipant(chatId: string | number): TLRPC.TL_channelParticipant | null {
    return this.participantsMap.get(chatId) || null;
  }

  /**
   * Load Full Channel metadata via MTProto RPC (channels.getFullChannel)
   */
  public async loadFullChannel(chatId: string | number): Promise<TLRPC.TL_chatFull | null> {
    try {
      const response = await fetch(`/api/chats/${chatId}/full`);
      const data = await response.json();
      if (data.success && data.full_chat) {
        const fullChat: TLRPC.TL_chatFull = data.full_chat;
        this.fullChatMap.set(chatId, fullChat);
        this.evaluateChatRestrictions(chatId);
        NotificationCenter.getInstance(this.currentAccount).postNotificationName(
          NotificationCenter.chatInfoDidLoad,
          chatId,
          fullChat
        );
        return fullChat;
      }
    } catch (e) {
      console.warn('[MessagesController] Failed to load full channel:', e);
    }
    return null;
  }

  /**
   * Fetch current user participant status via MTProto RPC (channels.getParticipant)
   */
  public async loadChannelParticipant(chatId: string | number): Promise<TLRPC.TL_channelParticipant | null> {
    try {
      const response = await fetch(`/api/chats/${chatId}/participant`);
      const data = await response.json();
      if (data.success && data.participant) {
        const participant: TLRPC.TL_channelParticipant = data.participant;
        this.participantsMap.set(chatId, participant);
        this.evaluateChatRestrictions(chatId);
        return participant;
      }
    } catch (e) {
      console.warn('[MessagesController] Failed to load channel participant:', e);
    }
    return null;
  }

  /**
   * Evaluates restriction state based on ChatObject.checkSendRights
   */
  public evaluateChatRestrictions(chatId: string | number): ChatRestrictionState {
    const chat = this.dialogsMap.get(chatId);
    const fullChat = this.fullChatMap.get(chatId);
    const participant = this.participantsMap.get(chatId);

    const rightsState = ChatObject.checkSendRights(chat, fullChat, participant);

    if (!rightsState.canSendMessages || rightsState.restrictionReason) {
      this.restrictionsMap.set(chatId, {
        canWrite: rightsState.canSendMessages,
        canSendMedia: rightsState.canSendMedia,
        canSendStickers: rightsState.canSendStickers,
        canSendPolls: rightsState.canSendPolls,
        canEmbedLinks: rightsState.canEmbedLinks,
        isBanned: rightsState.isBanned,
        isKicked: rightsState.isKicked,
        reason: rightsState.restrictionReason || undefined,
        bannedUntil: rightsState.untilDate,
      });
    } else {
      this.restrictionsMap.delete(chatId);
    }

    return rightsState;
  }

  public isChatAdmin(chat: any): boolean {
    if (!chat) return false;
    const myId = UserConfig.getInstance(this.currentAccount).getClientUserId();
    if (chat.creator || chat.is_creator || chat.admin_rights) return true;
    if (chat.admins && Array.isArray(chat.admins)) {
      return chat.admins.some((a: any) => String(a.user_id || a.id) === String(myId));
    }
    return false;
  }
}
