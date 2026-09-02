/**
 * org.telegram.ui.ChatActivity
 * Replicated directly from ChatActivity.java in DrKLO/Telegram Android
 * 
 * Bridges editor components with SendMessagesHelper and MTProto TLRPC.
 */

import { TLRPC } from '../tgnet/TLRPC';
import { SendMessagesHelper, SendingParams } from '../messenger/SendMessagesHelper';
import { TextStyleSpan } from './Components/TextStyleSpan';
import { Message, MessageMedia } from '../../../types';

export class ChatActivity {
  private currentAccount: number = 0;
  private dialogId: string = '';
  private replyingToMsgId?: string | number;

  constructor(dialogId: string = '', accountNum: number = 0) {
    this.dialogId = dialogId;
    this.currentAccount = accountNum;
  }

  public setDialogId(dialogId: string): void {
    this.dialogId = dialogId;
  }

  public setReplyingToMsgId(msgId?: string | number): void {
    this.replyingToMsgId = msgId;
  }

  /**
   * Applies formatting style to the selected text range in the editor
   */
  public applyFormattingToText(
    fullText: string,
    selectionStart: number,
    selectionEnd: number,
    styleFlag: number,
    url?: string
  ): { updatedText: string; newCursorStart: number; newCursorEnd: number } {
    if (selectionStart < 0 || selectionEnd < 0) {
      return { updatedText: fullText, newCursorStart: 0, newCursorEnd: 0 };
    }

    const start = Math.min(selectionStart, selectionEnd);
    const end = Math.max(selectionStart, selectionEnd);
    const selected = fullText.substring(start, end) || 'نص';

    let prefix = '';
    let suffix = '';

    switch (styleFlag) {
      case TextStyleSpan.FLAG_STYLE_BOLD:
        prefix = '**';
        suffix = '**';
        break;
      case TextStyleSpan.FLAG_STYLE_ITALIC:
        prefix = '_';
        suffix = '_';
        break;
      case TextStyleSpan.FLAG_STYLE_MONO:
        prefix = '`';
        suffix = '`';
        break;
      case TextStyleSpan.FLAG_STYLE_PRE:
        prefix = '```\n';
        suffix = '\n```';
        break;
      case TextStyleSpan.FLAG_STYLE_BLOCKQUOTE:
        prefix = '> ';
        suffix = '\n';
        break;
      case TextStyleSpan.FLAG_STYLE_SPOILER:
        prefix = '||';
        suffix = '||';
        break;
      case TextStyleSpan.FLAG_STYLE_STRIKE:
        prefix = '~';
        suffix = '~';
        break;
      case TextStyleSpan.FLAG_STYLE_UNDERLINE:
        prefix = '__';
        suffix = '__';
        break;
      case TextStyleSpan.FLAG_STYLE_URL:
        prefix = '[';
        suffix = `](${url || 'https://'})`;
        break;
      default:
        prefix = '';
        suffix = '';
    }

    const updatedText = fullText.substring(0, start) + prefix + selected + suffix + fullText.substring(end);
    const newCursorStart = start + prefix.length;
    const newCursorEnd = newCursorStart + selected.length;

    return { updatedText, newCursorStart, newCursorEnd };
  }

  /**
   * Converts rich formatted text into an ArrayList of TLRPC.MessageEntity
   */
  public getEntities(text: string): TLRPC.MessageEntity[] {
    const helper = SendMessagesHelper.getInstance(this.currentAccount);
    return helper.parseEntities(text).entities;
  }

  /**
   * Processes outgoing message transmission via SendMessagesHelper & TL_messages_sendMessage
   */
  public async processSendMessage(
    text: string,
    replyToMsgId?: string | number,
    params?: SendingParams
  ): Promise<Message | null> {
    if (!text || !text.trim() || !this.dialogId) {
      return null;
    }

    const replyId = replyToMsgId !== undefined ? replyToMsgId : this.replyingToMsgId;
    const helper = SendMessagesHelper.getInstance(this.currentAccount);

    return await helper.sendMessage(
      this.dialogId,
      text,
      replyId,
      undefined,
      params
    );
  }

  /**
   * Processes outgoing media transmission via SendMessagesHelper & TL_messages_sendMedia
   */
  public async processSendMedia(
    media: MessageMedia,
    caption: string = '',
    replyToMsgId?: string | number,
    params?: SendingParams
  ): Promise<Message | null> {
    if (!media || !this.dialogId) {
      return null;
    }

    const replyId = replyToMsgId !== undefined ? replyToMsgId : this.replyingToMsgId;
    const helper = SendMessagesHelper.getInstance(this.currentAccount);

    return await helper.sendMedia(
      this.dialogId,
      media,
      caption,
      replyId,
      undefined,
      params
    );
  }

  /**
   * Processes message forwarding via SendMessagesHelper & TL_messages_forwardMessages
   */
  public async processForwardMessages(
    targetChatId: string,
    messageIds: (string | number)[],
    dropAuthor: boolean = false,
    silent: boolean = false
  ): Promise<Message[]> {
    if (!this.dialogId || !targetChatId || messageIds.length === 0) return [];
    const helper = SendMessagesHelper.getInstance(this.currentAccount);
    return await helper.forwardMessages(this.dialogId, targetChatId, messageIds, dropAuthor, silent);
  }

  /**
   * Processes message edit via SendMessagesHelper & TL_messages_editMessage
   */
  public async processEditMessage(
    messageId: string | number,
    newText: string
  ): Promise<Message | null> {
    if (!this.dialogId || !messageId || !newText.trim()) return null;
    const helper = SendMessagesHelper.getInstance(this.currentAccount);
    return await helper.editMessage(this.dialogId, messageId, newText);
  }

  /**
   * Processes message deletion via SendMessagesHelper & TL_messages_deleteMessages
   */
  public async processDeleteMessages(
    messageIds: (string | number)[],
    revoke: boolean = true
  ): Promise<boolean> {
    if (!this.dialogId || messageIds.length === 0) return false;
    const helper = SendMessagesHelper.getInstance(this.currentAccount);
    return await helper.deleteMessages(this.dialogId, messageIds, revoke);
  }

  /**
   * Processes message pinning via SendMessagesHelper & TL_messages_updatePinnedMessage
   */
  public async processPinMessage(
    messageId: string | number,
    silent: boolean = false,
    unpin: boolean = false
  ): Promise<boolean> {
    if (!this.dialogId || !messageId) return false;
    const helper = SendMessagesHelper.getInstance(this.currentAccount);
    return await helper.updatePinnedMessage(this.dialogId, messageId, silent, unpin);
  }

  /**
   * Processes scheduling message via SendMessagesHelper with schedule_date
   */
  public async processScheduleMessage(
    text: string,
    scheduleTimestamp: number
  ): Promise<Message | null> {
    if (!this.dialogId || !text.trim()) return null;
    const helper = SendMessagesHelper.getInstance(this.currentAccount);
    return await helper.sendMessage(
      this.dialogId,
      text,
      this.replyingToMsgId,
      undefined,
      undefined,
      scheduleTimestamp
    );
  }
}
