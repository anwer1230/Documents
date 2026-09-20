/**
 * StorageSyncManager.ts - Synchronization between SQLite, Dexie, and localStorage
 */

import { draftSyncService } from '../services/DraftSyncService';

export class StorageSyncManager {
  private static instance: StorageSyncManager;

  public static getInstance(): StorageSyncManager {
    if (!StorageSyncManager.instance) {
      StorageSyncManager.instance = new StorageSyncManager();
    }
    return StorageSyncManager.instance;
  }

  public async syncAll(): Promise<boolean> {
    draftSyncService.flushPendingWrites();
    return true;
  }

  public getAllDrafts(): Record<string, string> {
    return draftSyncService.getAllDrafts();
  }

  public setDraft(chatId: string, draft: string): void {
    draftSyncService.saveDraft(chatId, draft);
  }

  public async loadSettings(): Promise<any> {
    return null;
  }

  public async saveSettings(_settings: any): Promise<void> {}

  public saveSessions(accounts: any[], activeAccountId?: string): void {
    try {
      if (typeof window === 'undefined' || !window.localStorage) return;
      if (Array.isArray(accounts) && accounts.length > 0) {
        localStorage.setItem('tg_multi_accounts_v3', JSON.stringify(accounts));
        localStorage.setItem('tg_accounts', JSON.stringify(accounts));
        const active = accounts.find((a) => a.id === activeAccountId || a.isActive) || accounts[0];
        if (active) {
          if (active.sessionString) {
            localStorage.setItem('tg_session_string', active.sessionString);
          }
          if (active.phone || active.user?.phone) {
            localStorage.setItem('tg_phone', active.phone || active.user?.phone || '');
          }
          if (active.id) {
            localStorage.setItem('tg_active_account_id_v3', active.id);
          }
          localStorage.setItem('tg_auth_session_active', 'true');
        }
      }
    } catch (_) {}
  }

  public clearAllOnLogout(): void {
    try {
      if (typeof window === 'undefined' || !window.localStorage) return;
      localStorage.removeItem('tg_session_string');
      localStorage.removeItem('tg_phone');
      localStorage.removeItem('tg_auth_session_active');
      localStorage.removeItem('tg_auth_screen_registered_user');
      localStorage.removeItem('tg_multi_accounts_v3');
      localStorage.removeItem('tg_accounts');
      localStorage.removeItem('tg_active_account_id_v3');
      localStorage.setItem('tg_explicitly_logged_out', 'true');
    } catch (_) {}
  }
}

export const storageSyncManager = StorageSyncManager.getInstance();
