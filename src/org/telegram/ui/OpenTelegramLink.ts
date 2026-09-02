/**
 * Telegram Official OpenTelegramLink & ChatInviteActivity logic
 * Replicates org.telegram.ui.LaunchActivity.OpenTelegramLink & org.telegram.ui.ChatInviteActivity
 * 
 * Supports:
 * - Parsing t.me/+, t.me/joinchat, tg://resolve, tg://join, @username
 * - Resolving invite hashes and channel usernames via MTProto / TDLib calls
 * - Request approval handling (join request) and instant join flow
 */

import { TLRPC } from '../tgnet/TLRPC';

export interface TelegramParsedLink {
  type: 'username' | 'invite' | 'bot_start' | 'message_link' | 'phone' | 'unknown';
  username?: string;
  inviteHash?: string;
  botStartParam?: string;
  chatId?: string | number;
  messageId?: number;
  phoneNumber?: string;
}

export interface ChatInviteSheetData {
  hash: string;
  title: string;
  about?: string;
  photo?: string;
  participantsCount: number;
  isChannel: boolean;
  isMegagroup: boolean;
  requestNeeded: boolean;
  verified: boolean;
}

export class OpenTelegramLink {
  /**
   * Parses any Telegram URL or internal URI pattern
   */
  public static parseLink(url: string): TelegramParsedLink {
    if (!url) return { type: 'unknown' };

    const clean = url.trim();

    // 1. Private Invite links (t.me/+Hash or t.me/joinchat/Hash)
    const inviteMatch = clean.match(/(?:https?:\/\/)?(?:www\.)?(?:t\.me|telegram\.me)\/(?:\+|joinchat\/)([a-zA-Z0-9_-]+)/i)
      || clean.match(/^tg:\/\/join\?invite=([a-zA-Z0-9_-]+)/i);
    if (inviteMatch) {
      return { type: 'invite', inviteHash: inviteMatch[1] };
    }

    // 2. Direct message link (t.me/username/1234)
    const msgMatch = clean.match(/(?:https?:\/\/)?(?:www\.)?(?:t\.me|telegram\.me)\/([a-zA-Z0-9_]{4,32})\/(\d+)/i);
    if (msgMatch) {
      return { type: 'message_link', username: msgMatch[1], messageId: parseInt(msgMatch[2], 10) };
    }

    // 3. Bot start param (t.me/bot?start=xxx)
    const botStartMatch = clean.match(/(?:https?:\/\/)?(?:www\.)?(?:t\.me|telegram\.me)\/([a-zA-Z0-9_]{4,32})\?start=([a-zA-Z0-9_-]+)/i)
      || clean.match(/^tg:\/\/resolve\?domain=([a-zA-Z0-9_]{4,32})&start=([a-zA-Z0-9_-]+)/i);
    if (botStartMatch) {
      return { type: 'bot_start', username: botStartMatch[1], botStartParam: botStartMatch[2] };
    }

    // 4. Public username / channel / group (t.me/username or @username)
    const userMatch = clean.match(/(?:https?:\/\/)?(?:www\.)?(?:t\.me|telegram\.me)\/([a-zA-Z0-9_]{4,32})/i)
      || clean.match(/^@([a-zA-Z0-9_]{4,32})$/i)
      || clean.match(/^tg:\/\/resolve\?domain=([a-zA-Z0-9_]{4,32})/i);
    if (userMatch) {
      return { type: 'username', username: userMatch[1] };
    }

    // 5. Phone link (tg://resolve?phone=xxx)
    const phoneMatch = clean.match(/^tg:\/\/resolve\?phone=(\+?\d+)/i);
    if (phoneMatch) {
      return { type: 'phone', phoneNumber: phoneMatch[1] };
    }

    return { type: 'unknown' };
  }

  /**
   * Checks invite link info via Telegram backend
   */
  public static async checkInvite(hash: string): Promise<{ success: boolean; invite?: ChatInviteSheetData; error?: string }> {
    try {
      const res = await fetch(`/api/telegram/check_invite?hash=${encodeURIComponent(hash)}`);
      if (!res.ok) {
        throw new Error('Failed to resolve invite link');
      }
      const data = await res.json();
      return {
        success: true,
        invite: {
          hash,
          title: data.title || 'مجموعة تليجرام خاصة',
          about: data.about || '',
          photo: data.photo || '',
          participantsCount: data.participants_count || 1,
          isChannel: data.is_channel || false,
          isMegagroup: data.is_megagroup || true,
          requestNeeded: data.request_needed || false,
          verified: data.verified || false,
        },
      };
    } catch (e: any) {
      return {
        success: false,
        error: e.message || 'رابط الدعوة منتهي الصلاحية أو غير صالح',
      };
    }
  }

  /**
   * Joins a channel or group using invite hash or public username
   */
  public static async importChatInvite(hash: string): Promise<{ success: boolean; chat?: any; error?: TLRPC.TL_error }> {
    try {
      const res = await fetch('/api/telegram/import_invite', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ hash }),
      });
      const data = await res.json();
      if (!res.ok || !data.success) {
        const tlError = new TLRPC.TL_error(res.status, data.error || 'INVITE_HASH_INVALID');
        return { success: false, error: tlError };
      }
      return { success: true, chat: data.chat };
    } catch (e: any) {
      return { success: false, error: new TLRPC.TL_error(500, e.message || 'CONNECTION_ERROR') };
    }
  }
}
