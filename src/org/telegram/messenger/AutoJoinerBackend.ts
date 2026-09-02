/**
 * Telegram Official AutoJoinerBackend & Link Radar
 * Replicates AutoJoinActivity, importChatInvite, and channels_joinChannel
 */

import { ConnectionsManager } from '../tgnet/ConnectionsManager';
import { NotificationCenter } from './NotificationCenter';

export interface JoinProgress {
  link: string;
  success: boolean;
  title?: string;
  error?: string;
  isPrivate?: boolean;
}

export class AutoJoinerBackend {
  private static instance: AutoJoinerBackend;
  private isRadarActive: boolean = false;
  private joinedHistory: Set<string> = new Set();

  public static getInstance(): AutoJoinerBackend {
    if (!AutoJoinerBackend.instance) {
      AutoJoinerBackend.instance = new AutoJoinerBackend();
    }
    return AutoJoinerBackend.instance;
  }

  public setRadarActive(active: boolean): void {
    this.isRadarActive = active;
  }

  public isRadarEnabled(): boolean {
    return this.isRadarActive;
  }

  /**
   * Extracts Telegram links from any arbitrary text
   */
  public extractLinks(text: string): string[] {
    if (!text) return [];
    const regex = /(?:https?:\/\/)?(?:t\.me|telegram\.me)\/([a-zA-Z0-9_+/-]+)|@([a-zA-Z0-9_]{4,32})/gi;
    const links: string[] = [];
    let match;
    while ((match = regex.exec(text)) !== null) {
      const full = match[0];
      if (!links.includes(full)) {
        links.push(full);
      }
    }
    return links;
  }

  /**
   * Evaluates live stream message for auto-join radar
   */
  public processLiveMessage(text: string): void {
    if (!this.isRadarActive || !text) return;
    const links = this.extractLinks(text);
    for (const link of links) {
      if (!this.joinedHistory.has(link)) {
        this.joinedHistory.add(link);
        this.joinLink(link);
      }
    }
  }

  /**
   * Joins a public channel/group or private invite link
   */
  public async joinLink(rawLink: string): Promise<JoinProgress> {
    const link = rawLink.trim();
    this.joinedHistory.add(link);

    // Private invite hash
    if (link.includes('+') || link.includes('joinchat/')) {
      const hash = link.replace(/.*(?:t\.me\/\+|joinchat\/)/, '').replace('/', '');
      try {
        const resp = await ConnectionsManager.getInstance().sendRequest('importChatInvite', { hash });
        if (resp && resp.success) {
          NotificationCenter.getInstance().postNotificationName(NotificationCenter.chatDidCreated, resp.chat);
          return { link, success: true, title: resp.chat?.title || hash, isPrivate: true };
        }
        return { link, success: false, error: resp?.error || 'تعذر الانضمام للرابط الخاص', isPrivate: true };
      } catch (e: any) {
        return { link, success: false, error: e.message, isPrivate: true };
      }
    }

    // Public channel/supergroup
    const username = link.replace(/.*t\.me\//, '').replace('@', '').trim();
    try {
      const res = await ConnectionsManager.getInstance().sendRequest('resolveUsername', { username });
      if (res && res.peer) {
        const channelId = res.peer.channel_id || res.peer.chat_id;
        const joinResp = await ConnectionsManager.getInstance().sendRequest('joinChannel', { channelId });
        if (joinResp && joinResp.success) {
          NotificationCenter.getInstance().postNotificationName(NotificationCenter.chatDidCreated, joinResp.chat);
          return { link, success: true, title: username, isPrivate: false };
        }
        return { link, success: false, error: joinResp?.error || 'فشل الانضمام للقناة', isPrivate: false };
      }
      return { link, success: false, error: 'المعرف غير موجود أو تم حذفه', isPrivate: false };
    } catch (e: any) {
      return { link, success: false, error: e.message, isPrivate: false };
    }
  }

  /**
   * Batch joins a list of links with staggered delays
   */
  public async joinBatch(
    links: string[],
    delaySeconds: number = 3,
    onProgress?: (index: number, total: number, result: JoinProgress) => void
  ): Promise<JoinProgress[]> {
    const results: JoinProgress[] = [];

    for (let i = 0; i < links.length; i++) {
      const link = links[i];
      const res = await this.joinLink(link);
      results.push(res);
      if (onProgress) {
        onProgress(i + 1, links.length, res);
      }
      if (i < links.length - 1) {
        await new Promise((r) => setTimeout(r, delaySeconds * 1000));
      }
    }

    return results;
  }
}
