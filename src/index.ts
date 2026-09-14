/**
 * Telegram Web K Core Entry Point (src/index.ts)
 * Based on morethanwords/tweb src/index.ts
 * Exports the complete official architectural modules and initializes RootScope.
 */

import { rootScope } from './lib/rootScope';
import { storages } from './lib/storages/storages';
import { mtprotoCore } from './lib/mtproto/mtproto';
import { lang } from './lib/lang';

// Export all modules
export * from './lib/rootScope';
export * from './lib/storages/storages';
export * from './lib/storages/idb';
export * from './lib/storages/state';
export * from './lib/storages/cache';
export * from './lib/storages/sessionStorage';
export * from './lib/appManagers/appMessagesManager';
export * from './lib/appManagers/appChatsManager';
export * from './lib/appManagers/appUsersManager';
export * from './lib/appManagers/appUpdatesManager';
export * from './lib/appManagers/appStateManager';
export * from './lib/appManagers/appImManager';
export * from './lib/appManagers/appPeersManager';
export * from './lib/appManagers/appDownloadManager';
export * from './lib/appManagers/appDocsManager';
export * from './lib/appManagers/appPhotosManager';
export * from './lib/appManagers/appStickersManager';
export * from './lib/appManagers/apiManager';
export * from './lib/mtproto/mtproto';
export * from './lib/mtproto/networker';
export * from './lib/mtproto/connection';
export * from './lib/mtproto/timeManager';
export * from './lib/mtproto/serverTimeManager';
export * from './lib/mtproto/crypto';
export * from './lib/files/download';
export * from './lib/files/upload';
export * from './lib/files/fileStorage';
export * from './lib/files/idbFileStorage';
export * from './lib/files/controller';
export * from './lib/rlottie';
export * from './lib/lang';
export * from './config/state';
export * from './config/debug';

export { rootScope, storages, mtprotoCore, lang };

export async function initTelegramWeb(): Promise<void> {
  await rootScope.init();
}

export default rootScope;
