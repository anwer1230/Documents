/**
 * Telegram Web K Unified Storage Interface (storages.ts)
 * Based on morethanwords/tweb src/lib/storages/storages.ts
 */

import { idb, AppIDBStorage } from './idb';
import { sessionStorage, AppSessionStorage } from './sessionStorage';
import { stateStorage, AppStateStorage } from './state';
import { appCache, AppCacheStorage } from './cache';

export interface AppStorages {
  idb: AppIDBStorage;
  sessionStorage: AppSessionStorage;
  state: AppStateStorage;
  cache: AppCacheStorage;
}

export const storages: AppStorages = {
  idb,
  sessionStorage,
  state: stateStorage,
  cache: appCache,
};

export { idb, sessionStorage, stateStorage, appCache };
export default storages;
