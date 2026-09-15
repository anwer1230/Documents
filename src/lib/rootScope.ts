/**
 * Telegram Web K Root Scope (rootScope.ts)
 * Based on morethanwords/tweb src/lib/rootScope.ts
 * Central registry and dependency root holding singletons for all app managers, storages, and MTProto.
 */

import { EventEmitter } from './appManagers/utils';
import { storages, AppStorages } from './storages/storages';
import { appMessagesManager, AppMessagesManager } from './appManagers/appMessagesManager';
import { appChatsManager, AppChatsManager } from './appManagers/appChatsManager';
import { appUsersManager, AppUsersManager } from './appManagers/appUsersManager';
import { appUpdatesManager, AppUpdatesManager } from './appManagers/appUpdatesManager';
import { appStateManager, AppStateManager } from './appManagers/appStateManager';
import { appImManager, AppImManager } from './appManagers/appImManager';
import { appPeersManager, AppPeersManager } from './appManagers/appPeersManager';
import { appDownloadManager, AppDownloadManager } from './appManagers/appDownloadManager';
import { appDocsManager, AppDocsManager } from './appManagers/appDocsManager';
import { appPhotosManager, AppPhotosManager } from './appManagers/appPhotosManager';
import { appStickersManager, AppStickersManager } from './appManagers/appStickersManager';
import { apiManager, ApiManager } from './appManagers/apiManager';
import { mtprotoCore, MTProtoCore } from './mtproto/mtproto';
import { filesController, FilesController } from './files/controller';

export interface AppManagers {
  appMessagesManager: AppMessagesManager;
  appChatsManager: AppChatsManager;
  appUsersManager: AppUsersManager;
  appUpdatesManager: AppUpdatesManager;
  appStateManager: AppStateManager;
  appImManager: AppImManager;
  appPeersManager: AppPeersManager;
  appDownloadManager: AppDownloadManager;
  appDocsManager: AppDocsManager;
  appPhotosManager: AppPhotosManager;
  appStickersManager: AppStickersManager;
  apiManager: ApiManager;
}

export class RootScope extends EventEmitter {
  public managers: AppManagers;
  public storages: AppStorages;
  public mtproto: MTProtoCore;
  public files: FilesController;
  private isInitialized = false;

  constructor() {
    super();
    this.storages = storages;
    this.mtproto = mtprotoCore;
    this.files = filesController;
    this.managers = {
      appMessagesManager,
      appChatsManager,
      appUsersManager,
      appUpdatesManager,
      appStateManager,
      appImManager,
      appPeersManager,
      appDownloadManager,
      appDocsManager,
      appPhotosManager,
      appStickersManager,
      apiManager,
    };
  }

  public async init(): Promise<void> {
    if (this.isInitialized) return;
    this.isInitialized = true;

    // 1. Initialize storages and state
    await this.storages.state.init();

    // 2. Initialize updates manager with stored PTS
    await this.managers.appUpdatesManager.init();

    // 3. Connect MTProto and networkers
    this.mtproto.start().catch((err) => {
      console.warn('[RootScope] MTProto background start warning:', err);
    });

    this.emit('initialized', true);
  }
}

export const rootScope = new RootScope();

// Attach to global window in development for inspection if available
if (typeof window !== 'undefined') {
  (window as any).twebRootScope = rootScope;
}

export default rootScope;
