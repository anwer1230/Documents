/**
 * Telegram Web K MTProto API Interface (api.ts)
 * Based on morethanwords/tweb src/lib/mtproto/api.ts
 */

import { timeManager } from './timeManager';
import { apiManager } from '../appManagers/apiManager';

export interface MTProtoRequest {
  id: string;
  method: string;
  params: any;
  msgId: string;
}

export class MTProtoApi {
  public static async call<T = any>(method: string, params: Record<string, any> = {}): Promise<T> {
    const msgId = timeManager.generateMessageId();
    return apiManager.invoke<T>(method, { ...params, _msg_id: msgId });
  }

  public static async getHistory(peerId: string, limit = 50, offsetDate = 0): Promise<any> {
    return this.call('messages.getHistory', { peer: peerId, limit, offset_date: offsetDate });
  }

  public static async sendMessage(peerId: string, text: string, replyToMsgId?: string): Promise<any> {
    return this.call('messages.sendMessage', { peer: peerId, message: text, reply_to_msg_id: replyToMsgId });
  }

  public static async getDialogs(limit = 100): Promise<any> {
    return this.call('messages.getDialogs', { limit });
  }

  public static async getDifference(pts: number, date: number, qts: number): Promise<any> {
    return this.call('updates.getDifference', { pts, date, qts });
  }
}

export default MTProtoApi;
