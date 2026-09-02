/**
 * Telegram Official MessageObject Entity Parser & Formatting Engine
 * Replicates org.telegram.messenger.MessageObject.java & ChatMessageCell logic
 */

import { TLRPC } from '../tgnet/TLRPC';
import { UserConfig } from './UserConfig';

export interface FormattedMessageEntity {
  type: 'bold' | 'italic' | 'code' | 'pre' | 'url' | 'text_url' | 'mention' | 'hashtag' | 'spoiler' | 'custom_emoji' | 'phone';
  offset: number;
  length: number;
  url?: string;
  documentId?: string;
}

export class MessageObject {
  public messageOwner: TLRPC.TL_message | any;
  public caption?: string;
  public isOut: boolean = false;
  public isUnread: boolean = false;
  public isEdited: boolean = false;
  public isMedia: boolean = false;
  public entities: FormattedMessageEntity[] = [];

  constructor(account: number = 0, message: any, generateLayout: boolean = true) {
    this.messageOwner = message || {};
    this.isOut = Boolean(
      message.out ||
      message.is_out ||
      message.from_id?.user_id === UserConfig.getClientUserId() ||
      message.sender_id === UserConfig.getClientUserId() ||
      message.sender_id === 'me'
    );
    this.isUnread = Boolean(message.unread);
    this.isEdited = Boolean(message.edit_date || message.is_edited);
    this.isMedia = Boolean(message.media || message.photo || message.video || message.voice || message.document);
    this.entities = this.extractEntities();
  }

  public getEntities(): FormattedMessageEntity[] {
    return this.entities;
  }

  public getId(): number {
    return Number(this.messageOwner.id) || 0;
  }

  public getDialogId(): number | string {
    return this.messageOwner.peer_id?.channel_id ||
      this.messageOwner.peer_id?.chat_id ||
      this.messageOwner.peer_id?.user_id ||
      this.messageOwner.chat_id ||
      0;
  }

  public getMessageText(): string {
    return this.messageOwner.message || this.messageOwner.text || '';
  }

  public getReplyToMsgId(): number {
    return this.messageOwner.reply_to?.reply_to_msg_id || this.messageOwner.reply_to_id || 0;
  }

  private extractEntities(): FormattedMessageEntity[] {
    const text = this.getMessageText();
    if (!text) return [];

    const list: FormattedMessageEntity[] = [];

    // Check if raw entities already exist from TL layer
    if (Array.isArray(this.messageOwner.entities)) {
      for (const ent of this.messageOwner.entities) {
        let type: FormattedMessageEntity['type'] = 'url';
        if (ent._?.includes('Bold') || ent.type === 'bold') type = 'bold';
        else if (ent._?.includes('Italic') || ent.type === 'italic') type = 'italic';
        else if (ent._?.includes('Code') || ent.type === 'code') type = 'code';
        else if (ent._?.includes('Pre') || ent.type === 'pre') type = 'pre';
        else if (ent._?.includes('TextUrl') || ent.type === 'text_url') type = 'text_url';
        else if (ent._?.includes('Mention') || ent.type === 'mention') type = 'mention';
        else if (ent._?.includes('Hashtag') || ent.type === 'hashtag') type = 'hashtag';
        else if (ent._?.includes('Spoiler') || ent.type === 'spoiler') type = 'spoiler';
        else if (ent._?.includes('CustomEmoji') || ent.type === 'custom_emoji') type = 'custom_emoji';

        list.push({
          type,
          offset: ent.offset || 0,
          length: ent.length || 0,
          url: ent.url,
          documentId: ent.document_id,
        });
      }
      return list;
    }

    // Auto-extract URL entities
    const urlRegex = /(https?:\/\/[^\s]+)/g;
    let match;
    while ((match = urlRegex.exec(text)) !== null) {
      list.push({
        type: 'url',
        offset: match.index,
        length: match[0].length,
        url: match[0],
      });
    }

    // Auto-extract Mentions (@username)
    const mentionRegex = /@([a-zA-Z0-9_]{4,32})/g;
    while ((match = mentionRegex.exec(text)) !== null) {
      list.push({
        type: 'mention',
        offset: match.index,
        length: match[0].length,
      });
    }

    // Auto-extract Hashtags (#tag)
    const hashtagRegex = /#([\p{L}\p{N}_]+)/gu;
    while ((match = hashtagRegex.exec(text)) !== null) {
      list.push({
        type: 'hashtag',
        offset: match.index,
        length: match[0].length,
      });
    }

    return list;
  }
}
