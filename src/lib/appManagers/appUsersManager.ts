/**
 * Telegram Web K Users Manager (appUsersManager.ts)
 * Based on morethanwords/tweb src/lib/appManagers/appUsersManager.ts
 */

import { idb } from '../storages/idb';
import { appCache } from '../storages/cache';
import { EventEmitter } from './utils';
import { appPeersManager } from './appPeersManager';

export interface TelegramUserEntity {
  id: string;
  firstName: string;
  lastName?: string;
  username?: string;
  phone?: string;
  photoUrl?: string;
  status?: 'online' | 'offline' | 'recently';
  isBot?: boolean;
  isVerified?: boolean;
  isPremium?: boolean;
  lastSeen?: number;
}

export class AppUsersManager extends EventEmitter {
  private users: Map<string, TelegramUserEntity> = new Map();

  public async getUser(userId: string): Promise<TelegramUserEntity | null> {
    if (this.users.has(userId)) return this.users.get(userId)!;

    const cached = appCache.get<TelegramUserEntity>(`user_${userId}`);
    if (cached) {
      this.users.set(userId, cached);
      return cached;
    }

    try {
      const record = await idb.get('users', userId);
      if (record) {
        const u = record.data || record;
        this.users.set(userId, u);
        appCache.set(`user_${userId}`, u, 30000);
        return u;
      }
    } catch {}

    return null;
  }

  public saveUser(user: TelegramUserEntity): void {
    this.users.set(user.id, user);
    appCache.set(`user_${user.id}`, user, 30000);
    idb.put('users', { id: user.id, username: user.username, phone: user.phone, data: user }).catch(() => {});

    appPeersManager.savePeer({
      id: user.id,
      type: 'user',
      firstName: user.firstName,
      lastName: user.lastName,
      username: user.username,
      photoUrl: user.photoUrl,
      isVerified: user.isVerified,
      isPremium: user.isPremium,
      isBot: user.isBot,
    });

    this.emit('user_update', user);
  }

  public handleUserStatusUpdate(userId: string, status: 'online' | 'offline' | 'recently', lastSeen?: number): void {
    const user = this.users.get(userId);
    if (user) {
      user.status = status;
      if (lastSeen) user.lastSeen = lastSeen;
      this.saveUser(user);
      this.emit('user_status', { userId, status, lastSeen });
    }
  }
}

export const appUsersManager = new AppUsersManager();
export default appUsersManager;
