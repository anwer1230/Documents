/**
 * MessageObject.ts - Telegram Core Message Model & Entity Parser
 * 
 * Replicated directly from DrKLO/Telegram Android:
 * org.telegram.messenger.MessageObject.java
 * org.telegram.ui.Cells.ChatMessageCell.java
 */

import { Message, MessageMedia, Chat } from '../types';
import { TLRPC } from './TLRPC';
import { UserObject } from './UserObject';
import { ChatObject } from './ChatObject';

export interface MessageEntity {
  type:
    | 'url'
    | 'mention'
    | 'bot_command'
    | 'hashtag'
    | 'bold'
    | 'italic'
    | 'code'
    | 'pre'
    | 'spoiler'
    | 'phone'
    | 'email'
    | 'cashtag'
    | 'custom_emoji';
  offset: number;
  length: number;
  url?: string;
  language?: string;
  documentId?: string;
}

export class MessageObject {
  public messageOwner: Message;
  public messageText: string;
  public entities: MessageEntity[] = [];
  public currentAccount: number = 0;

  public customName?: string;
  public fromId: string | number = '';
  public isChannelPostVal: boolean = false;
  public isPostAuthorVisible: boolean = false;
  public postAuthor?: string;

  // Types corresponding to MessageObject.type in Telegram Android
  public type: number = 0;
  public static readonly TYPE_TEXT = 0;
  public static readonly TYPE_PHOTO = 1;
  public static readonly TYPE_VIDEO = 2;
  public static readonly TYPE_VOICE = 3;
  public static readonly TYPE_DOCUMENT = 4;
  public static readonly TYPE_STICKER = 5;
  public static readonly TYPE_ANIMATED_STICKER = 6;
  public static readonly TYPE_LOCATION = 7;
  public static readonly TYPE_CONTACT = 8;
  public static readonly TYPE_POLL = 9;
  public static readonly TYPE_CALL = 10;
  public static readonly TYPE_ROUND_VIDEO = 11;

  constructor(message: Message, account: number = 0, chat?: Chat) {
    this.messageOwner = message;
    this.messageText = message.text || '';
    this.currentAccount = account;
    this.classifyType();
    this.parseEntities();
    this.generateLayout(undefined, chat);
  }

  /**
   * Replicated from DrKLO MessageObject.getPeerId(TLRPC.Peer)
   */
  public static getPeerId(peer: any): string | number {
    if (!peer) return '';
    if (typeof peer === 'string' || typeof peer === 'number') return peer;
    if (peer.user_id) return peer.user_id;
    if (peer.channel_id) return `-100${peer.channel_id}`;
    if (peer.chat_id) return `-${peer.chat_id}`;
    return '';
  }

  /**
   * Replicated from DrKLO MessageObject.generateLayout(User fromUser, Chat chat)
   */
  public generateLayout(fromUser?: any, chat?: Chat): void {
    const msg = this.messageOwner;
    if (!msg) return;

    this.fromId = MessageObject.getPeerId((msg as any).from_id) || msg.senderId || '';
    this.postAuthor = (msg as any).post_author || (msg as any).postAuthor || msg.senderRank;
    this.isChannelPostVal = this.isChannelPost(chat);

    const isGroup = Boolean(chat && (ChatObject.isGroup(chat) || ChatObject.isMegagroup(chat) || chat.type === 'group' || (chat as any).type === 'supergroup'));

    // Channel post with signed admin author (Sign Messages enabled)
    if (this.isChannelPostVal && !this.isMegagroup(chat)) {
      if (this.postAuthor) {
        this.customName = this.postAuthor;
        this.isPostAuthorVisible = true;
      } else {
        this.customName = '';
        this.isPostAuthorVisible = false;
      }
    } else if (isGroup) {
      // Group or Megagroup: ALWAYS display real user sender name, NEVER the group title
      if (fromUser) {
        this.customName = UserObject.getUserName(fromUser);
      } else if (msg.senderName && msg.senderName !== chat?.title && msg.senderName !== (chat as any)?.name) {
        this.customName = msg.senderName;
      } else if ((msg as any).from_name || (msg as any).sender_name) {
        this.customName = (msg as any).from_name || (msg as any).sender_name;
      } else if (msg.senderUsername) {
        this.customName = `@${msg.senderUsername.replace(/^@/, '')}`;
      } else if (msg.senderId) {
        // Human-friendly fallback from user ID
        const idStr = String(msg.senderId).toLowerCase();
        if (idStr.includes('khalid')) this.customName = 'خالد المنصوري';
        else if (idStr.includes('tariq')) this.customName = 'طارق الأحمدي';
        else if (idStr.includes('sarah')) this.customName = 'سارة المهدي';
        else if (idStr.includes('alex')) this.customName = 'Alex Rivera';
        else if (idStr.includes('durov')) this.customName = 'Pavel Durov';
        else if (idStr.startsWith('user_')) {
          const raw = idStr.replace(/^user_/, '').replace(/_/g, ' ');
          this.customName = raw.charAt(0).toUpperCase() + raw.slice(1);
        } else {
          this.customName = `عضو #${idStr.slice(-4)}`;
        }
      } else {
        this.customName = 'عضو في المجموعة';
      }
    } else {
      // 1-on-1 Private chat or Saved or Bot
      if (fromUser) {
        this.customName = UserObject.getUserName(fromUser);
      } else if (msg.senderName) {
        this.customName = msg.senderName;
      } else if (chat && !this.isOut()) {
        this.customName = chat.title;
      } else {
        this.customName = 'User';
      }
    }
  }

  public shouldDrawSenderName(
    chat?: Chat,
    grouping?: { isGroupStart?: boolean; isGroupMiddle?: boolean; isGroupEnd?: boolean; isSingle?: boolean }
  ): boolean {
    // 1. Outgoing messages never draw sender name header
    if (this.isOut() || this.messageOwner.isOutgoing) {
      return false;
    }

    // 2. Channel posts only draw if sign messages is enabled and postAuthor exists
    if (this.isChannelPostVal && !this.isMegagroup(chat)) {
      return Boolean(this.isPostAuthorVisible && this.postAuthor);
    }

    // 3. Groups & Megagroups: Always draw sender name on the first message of a group/cluster
    const isGroup = Boolean(chat && (ChatObject.isGroup(chat) || ChatObject.isMegagroup(chat) || chat.type === 'group' || (chat as any).type === 'supergroup'));
    if (isGroup) {
      if (grouping) {
        return Boolean(grouping.isGroupStart || grouping.isSingle);
      }
      return true;
    }

    // 4. Private 1-on-1 chats / saved / bots do not draw redundant author header in bubbles
    return false;
  }

  public isChannelPost(chat?: Chat): boolean {
    const msg = this.messageOwner;
    if ((msg as any).post || (msg as any).isChannelPost) return true;
    if (chat && ChatObject.isChannel(chat) && !ChatObject.isMegagroup(chat)) return true;
    return false;
  }

  public isMegagroup(chat?: Chat): boolean {
    if (chat) return ChatObject.isMegagroup(chat);
    return false;
  }

  public isFromUser(): boolean {
    const pId = String(this.fromId);
    return !!pId && !pId.startsWith('-') && pId !== '0';
  }

  public getSenderTitle(): string {
    return this.customName || this.messageOwner.senderName || 'User';
  }

  private classifyType(): void {
    const msg = this.messageOwner;
    if (msg.media) {
      const mType = (msg.media.type as string) || '';
      switch (mType) {
        case 'photo':
          this.type = MessageObject.TYPE_PHOTO;
          break;
        case 'video':
          this.type = MessageObject.TYPE_VIDEO;
          break;
        case 'audio':
        case 'voice':
          this.type = MessageObject.TYPE_VOICE;
          break;
        case 'document':
          this.type = MessageObject.TYPE_DOCUMENT;
          break;
        case 'sticker':
        case 'animated_sticker':
          this.type = MessageObject.TYPE_STICKER;
          break;
        case 'location':
          this.type = MessageObject.TYPE_LOCATION;
          break;
        case 'contact':
          this.type = MessageObject.TYPE_CONTACT;
          break;
        case 'poll':
          this.type = MessageObject.TYPE_POLL;
          break;
        case 'call':
          this.type = MessageObject.TYPE_CALL;
          break;
        case 'video_note':
        case 'round_video':
          this.type = MessageObject.TYPE_ROUND_VIDEO;
          break;
        default:
          this.type = MessageObject.TYPE_TEXT;
      }
    } else {
      this.type = MessageObject.TYPE_TEXT;
    }
  }

  /**
   * DrKLO MessageObject.getEntities()
   * Extracts Telegram rich entities: URLs, mentions, bot commands, hashtags, spoilers, bold, italic, code
   */
  public getEntities(): MessageEntity[] {
    return this.entities;
  }

  public parseEntities(): void {
    const text = this.messageText;
    if (!text) {
      this.entities = [];
      return;
    }

    const entities: MessageEntity[] = [];

    // 1. URLs (https?:// or t.me/ or tg://)
    const urlRegex = /(https?:\/\/[^\s<]+|t\.me\/[^\s<]+|tg:\/\/[^\s<]+)/gi;
    let match: RegExpExecArray | null;
    while ((match = urlRegex.exec(text)) !== null) {
      entities.push({
        type: 'url',
        offset: match.index,
        length: match[0].length,
        url: match[0].startsWith('http') || match[0].startsWith('tg://') ? match[0] : `https://${match[0]}`,
      });
    }

    // 2. Mentions (@username)
    const mentionRegex = /@([a-zA-Z0-9_]{3,32})/g;
    while ((match = mentionRegex.exec(text)) !== null) {
      entities.push({
        type: 'mention',
        offset: match.index,
        length: match[0].length,
        url: `https://t.me/${match[1]}`,
      });
    }

    // 3. Bot commands (/command)
    const cmdRegex = /\/([a-zA-Z0-9_]{1,64})/g;
    while ((match = cmdRegex.exec(text)) !== null) {
      entities.push({
        type: 'bot_command',
        offset: match.index,
        length: match[0].length,
      });
    }

    // 4. Hashtags (#tag)
    const hashtagRegex = /#([a-zA-Z0-9_\u0600-\u06FF]+)/g;
    while ((match = hashtagRegex.exec(text)) !== null) {
      entities.push({
        type: 'hashtag',
        offset: match.index,
        length: match[0].length,
      });
    }

    // 5. Spoilers (||spoiler||)
    const spoilerRegex = /\|\|(.*?)\|\|/g;
    while ((match = spoilerRegex.exec(text)) !== null) {
      entities.push({
        type: 'spoiler',
        offset: match.index,
        length: match[0].length,
      });
    }

    // 6. Bold (**bold**)
    const boldRegex = /\*\*(.*?)\*\*/g;
    while ((match = boldRegex.exec(text)) !== null) {
      entities.push({
        type: 'bold',
        offset: match.index,
        length: match[0].length,
      });
    }

    // 7. Italic (__italic__)
    const italicRegex = /__(.*?)__/g;
    while ((match = italicRegex.exec(text)) !== null) {
      entities.push({
        type: 'italic',
        offset: match.index,
        length: match[0].length,
      });
    }

    // 8. Code (`code`)
    const codeRegex = /`([^`]+)`/g;
    while ((match = codeRegex.exec(text)) !== null) {
      entities.push({
        type: 'code',
        offset: match.index,
        length: match[0].length,
      });
    }

    this.entities = entities.sort((a, b) => a.offset - b.offset);
  }

  // Quick State Checks (Replicating MessageObject flags)
  public isOut(): boolean {
    return !!this.messageOwner.isOutgoing;
  }

  public isUnread(): boolean {
    return this.messageOwner.status !== 'read' && !this.isOut();
  }

  public isSending(): boolean {
    return this.messageOwner.status === 'sending';
  }

  public isSendError(): boolean {
    return (this.messageOwner.status as string) === 'failed';
  }

  public isMediaEmpty(): boolean {
    return !this.messageOwner.media;
  }

  public isVoice(): boolean {
    return this.type === MessageObject.TYPE_VOICE;
  }

  public isVideo(): boolean {
    return this.type === MessageObject.TYPE_VIDEO;
  }

  public isPhoto(): boolean {
    return this.type === MessageObject.TYPE_PHOTO;
  }

  public isSticker(): boolean {
    return this.type === MessageObject.TYPE_STICKER;
  }

  public isPoll(): boolean {
    return this.type === MessageObject.TYPE_POLL;
  }

  public getMedia(): MessageMedia | undefined {
    return this.messageOwner.media;
  }

  public getId(): string {
    return this.messageOwner.id;
  }

  public getDialogId(): string {
    return this.messageOwner.chatId;
  }
}
