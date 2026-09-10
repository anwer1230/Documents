import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import { CapturedLink } from '../types';

export interface JoinAttemptLog {
  id: string;
  timestamp: number;
  timeString: string;
  url: string;
  normalizedUrl: string;
  chatTitle?: string;
  sourceChat?: string;
  status: 'success' | 'failed' | 'skipped_duplicate' | 'skipped_private' | 'rate_limited' | 'waiting_cooldown' | 'joining';
  statusLabel: string;
  message: string;
  jitterAppliedMs?: number;
  totalWaitMs?: number;
  hourlyCount: number;
  queueRemaining: number;
}

export interface RecentAttemptRecord {
  timestamp: number;
  status: 'success' | 'failed' | 'already_member' | 'skipped';
  failReason?: string;
  targetTitle?: string;
}

export interface LinkJoinQueueState {
  // Queue & Items
  queue: CapturedLink[];
  recentAttempts: Record<string, RecentAttemptRecord>;
  logs: JoinAttemptLog[];

  // Rate Limiting & Safety Timers
  hourlyJoinTimestamps: number[];
  lastJoinTimestamp: number;
  currentJitterMs: number;
  isProcessing: boolean;
  
  // Strict Rate Limiting Configurations
  maxHourlyJoins: number; // Maximum 20 joins per rolling 1 hour
  baseCooldownMs: number; // 60,000 ms (1 minute base interval)
  duplicateCooldownMs: number; // 3,600,000 ms (1 hour anti-duplicate protection)
  minJitterMs: number; // 5,000 ms (5 seconds random jitter)
  maxJitterMs: number; // 25,000 ms (25 seconds random jitter)
  autoJoinEnabled: boolean;

  // Actions
  setAutoJoinEnabled: (enabled: boolean) => void;
  enqueueBatch: (links: CapturedLink[]) => { added: number; skippedDuplicates: number; skippedPrivate: number };
  enqueueSingle: (link: CapturedLink) => { enqueued: boolean; reason?: string };
  removeFromQueue: (url: string) => void;
  clearQueue: () => void;
  clearLogs: () => void;
  addLog: (log: Omit<JoinAttemptLog, 'id' | 'timestamp' | 'timeString'>) => void;
  recordAttempt: (url: string, status: 'success' | 'failed' | 'already_member' | 'skipped', reason?: string, title?: string) => void;
  isRecentDuplicate: (url: string) => { isDuplicate: boolean; reason?: string; minutesAgo?: number };
  calculateNextJitter: () => number;
  recordJoinTimestamp: (timestamp?: number) => void;
  cleanOldTimestamps: () => number;
  setIsProcessing: (processing: boolean) => void;
}

/**
 * Normalizes a Telegram link into a canonical identifier for consistent deduplication
 * Examples:
 *   https://t.me/example_group/ -> example_group
 *   t.me/joinchat/Abc123xyz -> +abc123xyz
 *   @Example_Group -> example_group
 *   tg://join?invite=Abc123xyz -> +abc123xyz
 */
export const normalizeTelegramUrl = (rawUrl: string): string => {
  if (!rawUrl) return '';
  let u = rawUrl.trim().toLowerCase();
  u = u.replace(/^https?:\/\//, '');
  u = u.replace(/^tg:\/\/join\?invite=/, '+');
  u = u.replace(/^(?:www\.)?(?:t\.me|telegram\.me|telegram\.dog)\//, '');
  u = u.replace(/^joinchat\//, '+');
  u = u.replace(/^@/, '');
  u = u.split('?')[0].split('#')[0];
  u = u.replace(/\/+$/, '');
  return u;
};

export const isPrivateTelegramLink = (url: string): boolean => {
  if (!url) return false;
  const lower = url.toLowerCase();
  return (
    lower.includes('+') ||
    lower.includes('joinchat') ||
    lower.includes('invite=') ||
    lower.includes('tg://join?invite=')
  );
};

export const useLinkJoinQueueStore = create<LinkJoinQueueState>()(
  persist(
    (set, get) => ({
      queue: [],
      recentAttempts: {},
      logs: [],
      hourlyJoinTimestamps: [],
      lastJoinTimestamp: 0,
      currentJitterMs: 8000,
      isProcessing: false,
      maxHourlyJoins: 20, // Strict maximum: 20 joins per rolling 60 minutes
      baseCooldownMs: 60000, // 60 seconds (1 minute interval)
      duplicateCooldownMs: 3600000, // 60 minutes window preventing repeat join attempts
      minJitterMs: 5000, // 5s min random jitter
      maxJitterMs: 25000, // 25s max random jitter
      autoJoinEnabled: true,

      setAutoJoinEnabled: (enabled: boolean) => {
        set({ autoJoinEnabled: enabled });
      },

      /**
       * Calculates a dynamic random jitter delay between minJitterMs and maxJitterMs
       * to break bot detection patterns and avoid Telegram anti-spam blocks.
       */
      calculateNextJitter: () => {
        const { minJitterMs, maxJitterMs } = get();
        const jitter = Math.floor(Math.random() * (maxJitterMs - minJitterMs + 1)) + minJitterMs;
        set({ currentJitterMs: jitter });
        return jitter;
      },

      /**
       * Checks if a URL was attempted recently or has already succeeded,
       * preventing duplicate join attempts in a short timeframe.
       */
      isRecentDuplicate: (url: string) => {
        const { recentAttempts, duplicateCooldownMs } = get();
        const normalized = normalizeTelegramUrl(url);
        if (!normalized) return { isDuplicate: false };

        const record = recentAttempts[normalized];
        if (!record) return { isDuplicate: false };

        const elapsedMs = Date.now() - record.timestamp;
        const minutesAgo = Math.max(0, Math.floor(elapsedMs / 60000));

        // If succeeded or already a member, permanently or persistently prevent re-attempt
        if (record.status === 'success' || record.status === 'already_member') {
          return {
            isDuplicate: true,
            reason: `أنت عضو بالفعل أو تم الانضمام بنجاح مسبقاً (قبل ${minutesAgo} دقيقة)`,
            minutesAgo,
          };
        }

        // If attempted within duplicate cooldown window (e.g. 60 minutes)
        if (elapsedMs < duplicateCooldownMs) {
          return {
            isDuplicate: true,
            reason: `تمت محاولة الانضمام لهذا الرابط قبل ${minutesAgo} دقيقة (فترة منع التكرار)`,
            minutesAgo,
          };
        }

        return { isDuplicate: false };
      },

      /**
       * Records an attempt in the recentAttempts cache for anti-duplicate tracking
       */
      recordAttempt: (url, status, reason, title) => {
        const normalized = normalizeTelegramUrl(url);
        if (!normalized) return;

        set((state) => {
          const now = Date.now();
          const updated = {
            ...state.recentAttempts,
            [normalized]: {
              timestamp: now,
              status,
              failReason: reason,
              targetTitle: title,
            },
          };
          // Clean up entries older than 24 hours to keep localStorage lean
          const oneDayAgo = now - 86400000;
          for (const key in updated) {
            if (updated[key].timestamp < oneDayAgo) {
              delete updated[key];
            }
          }
          return { recentAttempts: updated };
        });
      },

      /**
       * Safely enqueues a batch of captured links, enforcing:
       * 1. Private channel exclusion
       * 2. Anti-duplicate prevention (recent attempts check)
       * 3. In-queue deduplication
       */
      enqueueBatch: (links: CapturedLink[]) => {
        const state = get();
        let added = 0;
        let skippedDuplicates = 0;
        let skippedPrivate = 0;

        const newQueue = [...state.queue];

        for (const link of links) {
          if (!link.url) continue;

          // 1. Private channel check
          if (isPrivateTelegramLink(link.url)) {
            skippedPrivate++;
            state.addLog({
              url: link.url,
              normalizedUrl: normalizeTelegramUrl(link.url),
              chatTitle: link.extractedTitle || link.chat_title,
              sourceChat: link.source_chat || link.sourceChatTitle,
              status: 'skipped_private',
              statusLabel: '🔒 قناة خاصة (تم التخطي)',
              message: 'تم تخطي الرابط تلقائياً طبقاً لضوابط الرادار (الانضمام مقتصر على المجموعات العامة)',
              hourlyCount: state.hourlyJoinTimestamps.filter((t) => Date.now() - t < 3600000).length,
              queueRemaining: newQueue.length,
            });
            continue;
          }

          // 2. Duplicate check (within recent timeframe or already member)
          const dupCheck = state.isRecentDuplicate(link.url);
          if (dupCheck.isDuplicate) {
            skippedDuplicates++;
            state.addLog({
              url: link.url,
              normalizedUrl: normalizeTelegramUrl(link.url),
              chatTitle: link.extractedTitle || link.chat_title,
              sourceChat: link.source_chat || link.sourceChatTitle,
              status: 'skipped_duplicate',
              statusLabel: '🛡️ منع تكرار المحاولة',
              message: dupCheck.reason || 'تم تخطي الرابط لمنع التكرار خلال فترة زمنية قصيرة',
              hourlyCount: state.hourlyJoinTimestamps.filter((t) => Date.now() - t < 3600000).length,
              queueRemaining: newQueue.length,
            });
            continue;
          }

          // 3. In-queue check
          const normalized = normalizeTelegramUrl(link.url);
          const alreadyInQueue = newQueue.some((q) => normalizeTelegramUrl(q.url) === normalized);
          if (alreadyInQueue) {
            skippedDuplicates++;
            continue;
          }

          // Valid link: add to queue
          newQueue.push({
            ...link,
            status: 'pending',
            status_text: `⏳ في طابور الانضمام (دور #${newQueue.length + 1})`,
          });
          added++;
        }

        if (added > 0) {
          set({ queue: newQueue });
        }

        return { added, skippedDuplicates, skippedPrivate };
      },

      enqueueSingle: (link: CapturedLink) => {
        const res = get().enqueueBatch([link]);
        return {
          enqueued: res.added > 0,
          reason: res.skippedDuplicates > 0 ? 'مكرر' : res.skippedPrivate > 0 ? 'خاصة' : undefined,
        };
      },

      removeFromQueue: (url: string) => {
        const normalized = normalizeTelegramUrl(url);
        set((state) => ({
          queue: state.queue.filter((q) => normalizeTelegramUrl(q.url) !== normalized),
        }));
      },

      clearQueue: () => {
        set({ queue: [] });
      },

      clearLogs: () => {
        set({ logs: [] });
      },

      addLog: (logData) => {
        const now = Date.now();
        const timeString = new Date(now).toLocaleTimeString('ar-SA', {
          hour: '2-digit',
          minute: '2-digit',
          second: '2-digit',
        });
        const entry: JoinAttemptLog = {
          ...logData,
          id: `log_${now}_${Math.random().toString(36).substr(2, 6)}`,
          timestamp: now,
          timeString,
        };

        set((state) => ({
          logs: [entry, ...state.logs].slice(0, 200), // Keep latest 200 log entries
        }));
      },

      recordJoinTimestamp: (timestamp = Date.now()) => {
        set((state) => {
          const oneHourAgo = timestamp - 3600000;
          const updatedTimestamps = [...state.hourlyJoinTimestamps.filter((t) => t > oneHourAgo), timestamp];
          return {
            lastJoinTimestamp: timestamp,
            hourlyJoinTimestamps: updatedTimestamps,
          };
        });
      },

      cleanOldTimestamps: () => {
        const now = Date.now();
        const oneHourAgo = now - 3600000;
        let count = 0;
        set((state) => {
          const filtered = state.hourlyJoinTimestamps.filter((t) => t > oneHourAgo);
          count = filtered.length;
          return { hourlyJoinTimestamps: filtered };
        });
        return count;
      },

      setIsProcessing: (processing: boolean) => {
        set({ isProcessing: processing });
      },
    }),
    {
      name: 'tg_radar_link_join_queue_v2',
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({
        queue: state.queue,
        recentAttempts: state.recentAttempts,
        logs: state.logs.slice(0, 100),
        hourlyJoinTimestamps: state.hourlyJoinTimestamps,
        lastJoinTimestamp: state.lastJoinTimestamp,
        autoJoinEnabled: state.autoJoinEnabled,
      }),
    }
  )
);
