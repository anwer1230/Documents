/**
 * Telegram Web K Peers Manager (appPeersManager.ts)
 * Based on morethanwords/tweb src/lib/appManagers/appPeersManager.ts
 */

import { idb } from '../storages/idb';
import { appCache } from '../storages/cache';
import { getPeerType, normalizePeerId } from './utils';

export interface TelegramPeer {
  id: string;
  type: 'user' | 'chat' | 'channel';
  title?: string;
  firstName?: string;
  lastName?: string;
  username?: string;
  photoUrl?: string;
  isVerified?: boolean;
  isPremium?: boolean;
  isBot?: boolean;
}

export class AppPeersManager {
  private peers: Map<string, TelegramPeer> = new Map();

  public getPeerType(peerId: string | number): 'user' | 'chat' | 'channel' {
    return getPeerType(peerId);
  }

  public getPeerId(peer: { id: string | number } | string | number): string {
    if (typeof peer === 'object' && peer !== null && 'id' in peer) {
      return normalizePeerId(peer.id);
    }
    return normalizePeerId(peer as string | number);
  }

  public async getPeer(peerId: string | number): Promise<TelegramPeer | null> {
    const id = normalizePeerId(peerId);
    if (this.peers.has(id)) return this.peers.get(id)!;

    const cached = appCache.get<TelegramPeer>(`peer_${id}`);
    if (cached) {
      this.peers.set(id, cached);
      return cached;
    }

    try {
      const record = await idb.get('peers', id);
      if (record) {
        this.peers.set(id, record);
        appCache.set(`peer_${id}`, record, 60000);
        return record;
      }
    } catch {}

    return null;
  }

  public savePeer(peer: TelegramPeer): void {
    const id = normalizePeerId(peer.id);
    this.peers.set(id, peer);
    appCache.set(`peer_${id}`, peer, 60000);
    idb.put('peers', peer).catch(() => {});
  }

  public getPeerTitle(peer: TelegramPeer | null): string {
    if (!peer) return '';
    if (peer.title) return peer.title;
    const parts = [peer.firstName, peer.lastName].filter(Boolean);
    if (parts.length > 0) return parts.join(' ');
    if (peer.username) return `@${peer.username}`;
    return peer.id;
  }
}

export const appPeersManager = new AppPeersManager();
export default appPeersManager;
