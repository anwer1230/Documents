/**
 * StorageSyncManager.ts - Synchronization between SQLite, Dexie, and localStorage
 */

export class StorageSyncManager {
  private static instance: StorageSyncManager;

  public static getInstance(): StorageSyncManager {
    if (!StorageSyncManager.instance) {
      StorageSyncManager.instance = new StorageSyncManager();
    }
    return StorageSyncManager.instance;
  }

  public async syncAll(): Promise<boolean> {
    return true;
  }

  public getAllDrafts(): Record<string, string> {
    return {};
  }

  public setDraft(_chatId: string, _draft: string): void {}

  public async loadSettings(): Promise<any> {
    return null;
  }

  public async saveSettings(_settings: any): Promise<void> {}

  public saveSessions(_accounts: any[], _activeAccountId?: string): void {}

  public clearAllOnLogout(): void {}
}

export const storageSyncManager = StorageSyncManager.getInstance();
