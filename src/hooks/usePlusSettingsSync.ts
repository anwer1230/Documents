import { useState, useEffect, useCallback, useRef } from 'react';
import { PlusConfig } from '../types';
import { telegramDb } from '../core/telegramDexieDb';

/**
 * Default Telegram Plus Configuration
 * Exhaustive default specification for all 12 modules
 */
export const DEFAULT_PLUS_CONFIG: PlusConfig = {
  // 1. General (عام)
  fontFamily: 'System Default',
  keepScreenOn: false,
  proximitySensor: true,
  useExternalBrowser: false,
  hapticFeedback: true,
  bigEmojis: true,
  showDirectShare: true,
  cacheLimitGb: 16,

  // 2. Chats (المحادثات)
  tabsEnabled: true,
  tabsPosition: 'top',
  showUnreadTabsCounter: true,
  hideMutedTabs: false,
  showOnlineStatusDot: true,
  doubleTapAction: 'reply',
  chatSwipeAction: 'archive',
  confirmBeforeCall: true,

  // 3. Stories (القصص)
  hideStoriesBar: false,
  stealthModeStories: false,
  autoSaveStories: false,
  highQualityPlayback: true,
  storySpeed: '1x',
  storyExpirationAlert: false,

  // 4. Messages (الرسائل)
  forwardWithoutQuote: true,
  showUserIdOnMessages: true,
  showExactSeconds: false,
  showEditedHistory: true,
  confirmVoiceNotes: false,
  confirmStickers: false,
  autoTranslateIncoming: false,
  translationProvider: 'telegram',

  // 5. Topics (المواضيع)
  topicsAsTabs: true,
  autoOpenGeneralTopic: false,
  unreadTopicBadges: true,
  quickTopicSearch: true,
  lastTopicMessagePreview: true,

  // 6. Navigation Drawer (درج التصفح)
  drawerShowNightMode: true,
  drawerShowSavedMessages: true,
  drawerShowCalls: true,
  drawerShowContacts: true,
  drawerShowPlusSettings: true,
  drawerShowAccounts: true,
  drawerHeaderStyle: 'standard',

  // 7. Profile (الملف الشخصي)
  profileShowUserId: true,
  profileCopyIdOnTap: true,
  profileShowCommonGroups: true,
  profileHidePhone: false,
  profileQuickActions: true,

  // 8. Notifications (الإشعارات)
  inAppNotificationStyle: 'banner',
  repeatUnreadAlerts: 'off',
  customPrivateTone: 'Default',
  customGroupTone: 'Default',
  vipPriorityAlerts: true,
  filterSpamAlerts: true,

  // 9. Privacy & Security (الخصوصية والأمان)
  ghostMode: false,
  hideOnlineStatus: false,
  hideReadReceipts: false,
  hideTypingIndicator: false,
  antiDeleteMessages: true,
  antiEditMessages: true,
  appLockPasscode: '',
  isAppLockEnabled: false,
  biometricsEnabled: false,
  hiddenChatsLocked: false,

  // 10. Shared Media (الوسائط المتبادلة)
  defaultMediaTab: 'photos',
  gridColumnsCount: 3,
  highResThumbnailPreview: true,
  pipFloatingVideo: true,
  autoPauseAudioOnVideo: true,
  customMediaPath: '/storage/emulated/0/Telegram/Telegram Documents',

  // 11. Downloads (التحميلات)
  autoDownloadWifi: true,
  autoDownloadCellular: false,
  downloadBooster: true,
  maxConcurrentDownloads: 4,
  downloadFinishSound: true,
  autoResumeDownloads: true,

  // 12. Ads (الإعلانات)
  blockSponsoredMessages: true,
  hidePromotedChannels: true,
  blockBotAds: true,
  disablePromoAlerts: true,
};

const STORAGE_KEY_PREFIX = 'tg_plus_config_v2';
const SYNC_CHANNEL_NAME = 'telegram_plus_sync_channel';
const CUSTOM_EVENT_NAME = 'tg_plus_config_updated';

// Shared BroadcastChannel singleton across all hook instances
let globalSyncChannel: BroadcastChannel | null = null;
try {
  if (typeof window !== 'undefined' && 'BroadcastChannel' in window) {
    globalSyncChannel = new BroadcastChannel(SYNC_CHANNEL_NAME);
  }
} catch (e) {
  console.warn('[PlusSync] BroadcastChannel not supported:', e);
}

export interface UsePlusSettingsSyncOptions {
  accountId?: string;
  autoCloudSync?: boolean;
}

export interface UsePlusSettingsSyncReturn {
  config: PlusConfig;
  updateConfig: (patch: Partial<PlusConfig>) => void;
  setConfig: (next: PlusConfig | ((prev: PlusConfig) => PlusConfig)) => void;
  resetConfig: () => void;
  isSynced: boolean;
  isSyncing: boolean;
  lastSyncedAt: number | null;
  syncNow: () => Promise<void>;
  exportConfig: () => string;
  importConfig: (jsonStr: string) => boolean;
}

/**
 * Unified Synchronization Hook for User & Plus Settings
 * Guarantees zero-data-loss multi-layer persistence:
 * 1. React In-Memory State (Instant reactivity)
 * 2. Synchronous LocalStorage (Zero-latency offline boot)
 * 3. Dexie IndexedDB (Durable, large capacity asynchronous storage)
 * 4. BroadcastChannel & StorageEvents (Instant multi-tab & multi-window propagation)
 * 5. In-Memory CustomEvents (Cross-component DOM event-bus)
 * 6. Cloud Backend API / Multi-Session Propagation (`/api/telegram/plus-settings/sync`)
 */
export const usePlusSettingsSync = (
  options: UsePlusSettingsSyncOptions = {}
): UsePlusSettingsSyncReturn => {
  const { accountId = 'global', autoCloudSync = true } = options;
  const storageKey = accountId === 'global' ? STORAGE_KEY_PREFIX : `${STORAGE_KEY_PREFIX}_${accountId}`;

  const [config, setConfigState] = useState<PlusConfig>(() => {
    try {
      if (typeof window !== 'undefined') {
        const saved = localStorage.getItem(storageKey);
        if (saved) {
          return { ...DEFAULT_PLUS_CONFIG, ...JSON.parse(saved) };
        }
      }
    } catch (e) {
      console.warn('[PlusSync] Failed to read initial config from localStorage:', e);
    }
    return DEFAULT_PLUS_CONFIG;
  });

  const [isSynced, setIsSynced] = useState<boolean>(true);
  const [isSyncing, setIsSyncing] = useState<boolean>(false);
  const [lastSyncedAt, setLastSyncedAt] = useState<number | null>(null);

  const debounceTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const latestConfigRef = useRef<PlusConfig>(config);
  latestConfigRef.current = config;

  // Cloud Sync Handler
  const pushToCloud = useCallback(
    async (configToSync: PlusConfig, timestamp: number) => {
      if (!autoCloudSync || typeof window === 'undefined') return;
      try {
        setIsSyncing(true);
        const res = await fetch('/api/telegram/plus-settings/sync', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            accountId,
            config: configToSync,
            updatedAt: timestamp,
          }),
        });
        if (res.ok) {
          setIsSynced(true);
          setLastSyncedAt(timestamp);
        }
      } catch (err) {
        console.warn('[PlusSync] Cloud sync skipped or failed (offline mode):', err);
      } finally {
        setIsSyncing(false);
      }
    },
    [accountId, autoCloudSync]
  );

  // Core multi-layer persistence & propagation orchestrator
  const saveAndBroadcast = useCallback(
    (nextConfig: PlusConfig) => {
      const now = Date.now();
      setIsSynced(false);

      // 1. Synchronous localStorage for instant fallback
      try {
        if (typeof window !== 'undefined') {
          localStorage.setItem(storageKey, JSON.stringify(nextConfig));
          if (accountId === 'global') {
            localStorage.setItem(STORAGE_KEY_PREFIX, JSON.stringify(nextConfig));
          }
        }
      } catch (e) {
        console.error('[PlusSync] LocalStorage write failed:', e);
      }

      // 2. Persistent Dexie IndexedDB
      telegramDb.plusSettings
        .put({ id: accountId, config: nextConfig, updatedAt: now })
        .catch((e) => console.warn('[PlusSync] Dexie DB write failed:', e));

      // 3. BroadcastChannel (Cross-tab & Cross-window instant propagation)
      if (globalSyncChannel) {
        try {
          globalSyncChannel.postMessage({
            type: 'PLUS_CONFIG_UPDATED',
            accountId,
            config: nextConfig,
            timestamp: now,
          });
        } catch (e) {
          console.warn('[PlusSync] BroadcastChannel postMessage failed:', e);
        }
      }

      // 4. In-Memory CustomEvent (Same-tab multi-instance propagation)
      if (typeof window !== 'undefined') {
        try {
          window.dispatchEvent(
            new CustomEvent(CUSTOM_EVENT_NAME, {
              detail: { accountId, config: nextConfig, timestamp: now },
            })
          );
        } catch {}
      }

      // 5. Debounced Cloud Synchronization
      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current);
      }
      debounceTimerRef.current = setTimeout(() => {
        pushToCloud(nextConfig, now);
      }, 400);
    },
    [accountId, storageKey, pushToCloud]
  );

  // Initial Hydration from IndexedDB and Cloud
  useEffect(() => {
    let isMounted = true;

    const hydrateFromStorageAndCloud = async () => {
      // Step A: Hydrate from IndexedDB
      try {
        const record = await telegramDb.plusSettings.get(accountId);
        if (record && record.config && isMounted) {
          setConfigState((prev) => {
            const merged = { ...prev, ...record.config };
            try {
              localStorage.setItem(storageKey, JSON.stringify(merged));
            } catch {}
            return merged;
          });
          setLastSyncedAt(record.updatedAt || null);
        }
      } catch (err) {
        console.warn('[PlusSync] Could not read from Dexie:', err);
      }

      // Step B: Hydrate from Backend Cloud API
      if (autoCloudSync) {
        try {
          const res = await fetch(`/api/telegram/plus-settings?accountId=${encodeURIComponent(accountId)}`);
          if (res.ok && isMounted) {
            const data = await res.json();
            if (data.success && data.config && Object.keys(data.config).length > 0) {
              setConfigState((prev) => {
                const merged = { ...prev, ...data.config };
                try {
                  localStorage.setItem(storageKey, JSON.stringify(merged));
                } catch {}
                telegramDb.plusSettings
                  .put({ id: accountId, config: merged, updatedAt: data.updatedAt || Date.now() })
                  .catch(() => {});
                return merged;
              });
              setLastSyncedAt(data.updatedAt || Date.now());
              setIsSynced(true);
            }
          }
        } catch (e) {
          // Offline fallback is fully functional
        }
      }
    };

    hydrateFromStorageAndCloud();

    // Listener 1: BroadcastChannel
    const handleBroadcast = (evt: MessageEvent) => {
      if (
        evt.data &&
        evt.data.type === 'PLUS_CONFIG_UPDATED' &&
        (evt.data.accountId === accountId || evt.data.accountId === 'global' || accountId === 'global') &&
        evt.data.config
      ) {
        setConfigState((prev) => ({ ...prev, ...evt.data.config }));
        setLastSyncedAt(evt.data.timestamp || Date.now());
        setIsSynced(true);
      }
    };

    // Listener 2: In-window CustomEvent
    const handleLocalEvent = (evt: Event) => {
      const customEvt = evt as CustomEvent<{ accountId: string; config: PlusConfig; timestamp: number }>;
      if (
        customEvt.detail &&
        (customEvt.detail.accountId === accountId || customEvt.detail.accountId === 'global' || accountId === 'global') &&
        customEvt.detail.config
      ) {
        setConfigState((prev) => ({ ...prev, ...customEvt.detail.config }));
        setLastSyncedAt(customEvt.detail.timestamp || Date.now());
        setIsSynced(true);
      }
    };

    // Listener 3: StorageEvent (Cross-context fallback)
    const handleStorage = (evt: StorageEvent) => {
      if ((evt.key === storageKey || evt.key === STORAGE_KEY_PREFIX) && evt.newValue) {
        try {
          const parsed = JSON.parse(evt.newValue);
          setConfigState((prev) => ({ ...prev, ...parsed }));
          setIsSynced(true);
        } catch {}
      }
    };

    if (globalSyncChannel) {
      globalSyncChannel.addEventListener('message', handleBroadcast);
    }
    window.addEventListener(CUSTOM_EVENT_NAME, handleLocalEvent);
    window.addEventListener('storage', handleStorage);

    return () => {
      isMounted = false;
      if (globalSyncChannel) {
        globalSyncChannel.removeEventListener('message', handleBroadcast);
      }
      window.removeEventListener(CUSTOM_EVENT_NAME, handleLocalEvent);
      window.removeEventListener('storage', handleStorage);
      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current);
      }
    };
  }, [accountId, storageKey, autoCloudSync]);

  // Public Actions
  const updateConfig = useCallback(
    (patch: Partial<PlusConfig>) => {
      setConfigState((prev) => {
        const next = { ...prev, ...patch };
        saveAndBroadcast(next);
        return next;
      });
    },
    [saveAndBroadcast]
  );

  const setConfig = useCallback(
    (next: PlusConfig | ((prev: PlusConfig) => PlusConfig)) => {
      setConfigState((prev) => {
        const resolved = typeof next === 'function' ? (next as (p: PlusConfig) => PlusConfig)(prev) : next;
        saveAndBroadcast(resolved);
        return resolved;
      });
    },
    [saveAndBroadcast]
  );

  const resetConfig = useCallback(() => {
    setConfigState(DEFAULT_PLUS_CONFIG);
    saveAndBroadcast(DEFAULT_PLUS_CONFIG);
  }, [saveAndBroadcast]);

  const syncNow = useCallback(async () => {
    await pushToCloud(latestConfigRef.current, Date.now());
  }, [pushToCloud]);

  const exportConfig = useCallback((): string => {
    return JSON.stringify(latestConfigRef.current, null, 2);
  }, []);

  const importConfig = useCallback(
    (jsonStr: string): boolean => {
      try {
        const parsed = JSON.parse(jsonStr);
        if (parsed && typeof parsed === 'object') {
          const merged = { ...DEFAULT_PLUS_CONFIG, ...parsed };
          setConfig(merged);
          return true;
        }
      } catch (e) {
        console.error('[PlusSync] Failed to import configuration:', e);
      }
      return false;
    },
    [setConfig]
  );

  return {
    config,
    updateConfig,
    setConfig,
    resetConfig,
    isSynced,
    isSyncing,
    lastSyncedAt,
    syncNow,
    exportConfig,
    importConfig,
  };
};

/**
 * Convenience Alias for backwards-compatibility
 */
export const usePlusConfig = usePlusSettingsSync;
