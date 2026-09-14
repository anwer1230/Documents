/**
 * BackgroundSyncService.ts
 *
 * Dedicated background orchestration service that offloads:
 * 1. Live Link Radar & Discovery (regex URL pattern parsing, normalization, de-duplication)
 * 2. Auto-Responder Rules Engine (keyword, exact, regex evaluation, scope matching, rate-limiting)
 * to a dedicated Web Worker off the main UI thread.
 */

import {
  AutoReplyRule,
  LiveDiscoveredLink,
  Message,
  PrivateAutoReplyRule,
} from '../types';
import { telegramDb, initTelegramDexieDb } from './telegramDexieDb';
import { connectionsManager } from './ConnectionsManager';
import { notificationsController } from './NotificationsController';

export interface BackgroundWorkerStatus {
  isWorkerActive: boolean;
  workerType: 'web-worker' | 'main-thread-fallback';
  lastProcessedTimestamp: number;
  totalMessagesProcessed: number;
  totalLinksDiscovered: number;
  totalAutoRepliesTriggered: number;
}

// Inlined Web Worker script code to ensure zero bundler/CORS loading issues in iframe
const WORKER_SCRIPT = `
(function() {
  let autoReplyRules = [];
  let isAutoResponderActive = true;
  let isLiveDiscoverActive = true;
  let isInstantAutoJoinEnabled = false;

  // Rate-limiting memory for auto-responder to avoid feedback loops
  const lastTriggeredMap = new Map();

  // Telegram Link Regex
  const TG_LINK_REGEX = /(?:https?:\\/\\/)?(?:www\\.)?(?:t\\.me|telegram\\.me|telegram\\.dog)\\/(?:\\+([a-zA-Z0-9_-]+)|joinchat\\/([a-zA-Z0-9_-]+)|([a-zA-Z0-9_]{4,}))|tg:\\/\\/join\\?invite=([a-zA-Z0-9_-]+)/gi;

  self.onmessage = function(event) {
    const data = event.data;
    if (!data || !data.type) return;

    switch (data.type) {
      case 'INIT_STATE':
      case 'SYNC_RULES': {
        if (Array.isArray(data.rules)) {
          autoReplyRules = data.rules;
        }
        if (typeof data.isAutoResponderActive === 'boolean') {
          isAutoResponderActive = data.isAutoResponderActive;
        }
        if (typeof data.isLiveDiscoverActive === 'boolean') {
          isLiveDiscoverActive = data.isLiveDiscoverActive;
        }
        if (typeof data.isInstantAutoJoinEnabled === 'boolean') {
          isInstantAutoJoinEnabled = data.isInstantAutoJoinEnabled;
        }
        self.postMessage({ type: 'ACK_SYNC', timestamp: Date.now() });
        break;
      }

      case 'PROCESS_INCOMING_MESSAGE': {
        const { message, chatTitle, chatType, correlationId } = data;
        const text = (message && message.text) ? message.text : '';

        // 1. Off-thread Link Discovery
        let discoveredLinks = [];
        if (isLiveDiscoverActive && text) {
          const matches = text.matchAll(TG_LINK_REGEX);
          for (const match of matches) {
            const rawUrl = match[0];
            const fullUrl = rawUrl.startsWith('http') || rawUrl.startsWith('tg://') ? rawUrl : 'https://' + rawUrl;
            discoveredLinks.push({
              id: 'disc_' + Date.now() + '_' + Math.random().toString(36).substr(2, 6),
              url: fullUrl,
              sourceChatTitle: chatTitle || 'محادثة تلغرام',
              sourceChatId: message.chatId || 'chat_unknown',
              senderName: message.senderName || 'مستخدم',
              timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
              status: isInstantAutoJoinEnabled ? 'joining' : 'pending',
              autoJoined: isInstantAutoJoinEnabled,
            });
          }
        }

        // 2. Off-thread Auto-Responder Rule Evaluation (Strictly Private Chats Only)
        let matchedRule = null;
        let autoReplyPayload = null;

        const isPrivateChat = chatType === 'private';

        if (isAutoResponderActive && !message.isOutgoing && text && autoReplyRules.length > 0 && isPrivateChat) {
          function normalizeAr(str) {
            if (!str) return '';
            return str
              .replace(/[\u064B-\u065F\u0670]/g, '')
              .replace(/[إأآا]/g, 'ا')
              .replace(/ة/g, 'ه')
              .replace(/ى/g, 'ي')
              .trim()
              .toLowerCase();
          }

          const cleanText = text.trim();
          const normMsg = normalizeAr(cleanText);
          const lowerMsg = cleanText.toLowerCase();

          for (const rule of autoReplyRules) {
            const isRuleActive = typeof rule.isEnabled === 'boolean'
              ? rule.isEnabled
              : (typeof rule.is_active === 'boolean' ? rule.is_active : true);
            if (!isRuleActive) continue;

            // Scope filter: Strictly private chat
            if (rule.scope && rule.scope !== 'private' && rule.scope !== 'all') continue;

            // Throttle protection (10 seconds per rule per chat)
            const throttleKey = rule.id + ':' + (message.chatId || 'default');
            const lastTrigger = lastTriggeredMap.get(throttleKey) || 0;
            if (Date.now() - lastTrigger < 10000) {
              continue;
            }

            const rawKw = (rule.keyword || '').trim();
            if (!rawKw) continue;
            const normKw = normalizeAr(rawKw);
            const lowerKw = rawKw.toLowerCase();

            let isMatch = false;
            if (rule.matchType === 'exact') {
              isMatch = (normMsg === normKw) || (lowerMsg === lowerKw);
            } else if (rule.matchType === 'regex') {
              try {
                const re = new RegExp(rule.keyword, 'i');
                isMatch = re.test(text);
              } catch (e) {
                isMatch = false;
              }
            } else {
              isMatch = normMsg.includes(normKw) || lowerMsg.includes(lowerKw);
            }

            if (isMatch) {
              lastTriggeredMap.set(throttleKey, Date.now());
              const replyContent = rule.replyText || rule.reply || '';
              matchedRule = {
                id: rule.id,
                replyText: replyContent,
              };
              autoReplyPayload = {
                ruleId: rule.id,
                replyText: replyContent,
                targetChatId: message.chatId,
                delayMs: 800,
              };
              break;
            }
          }
        }

        self.postMessage({
          type: 'PROCESS_RESULT',
          correlationId,
          messageId: message.id,
          discoveredLinks,
          autoReplyPayload,
          matchedRuleId: matchedRule ? matchedRule.id : null,
          processedAt: Date.now(),
        });
        break;
      }

      case 'PING': {
        self.postMessage({ type: 'PONG', timestamp: Date.now() });
        break;
      }
    }
  };

  self.postMessage({ type: 'WORKER_READY', timestamp: Date.now() });
})();
`;

export class BackgroundSyncService {
  private static instance: BackgroundSyncService;

  private worker: Worker | null = null;
  private isWorkerReady = false;
  private listeners: Set<() => void> = new Set();

  // State caches
  private autoReplyRules: AutoReplyRule[] = [
    {
      id: 'rule_1',
      keyword: 'السلام عليكم',
      replyText: 'وعليكم السلام ورحمة الله وبركاته، مرحباً بك! كيف يمكنني مساعدتك؟ 🌸',
      matchType: 'contains',
      scope: 'all',
      isEnabled: true,
      timesTriggered: 14,
      lastTriggeredAt: '08:45 AM',
    },
    {
      id: 'rule_2',
      keyword: 'الأسعار',
      replyText: 'أهلاً بك! يمكنك الاطلاع على باقات وأسعار الخدمات عبر الرابط: https://t.me/our_services_bot 💼',
      matchType: 'contains',
      scope: 'all',
      isEnabled: true,
      timesTriggered: 9,
      lastTriggeredAt: '08:30 AM',
    },
    {
      id: 'rule_3',
      keyword: 'رابط القناة',
      replyText: 'تفضل رابط القناة الرسمية: https://t.me/tech_innovators_hub 🚀',
      matchType: 'contains',
      scope: 'all',
      isEnabled: true,
      timesTriggered: 5,
      lastTriggeredAt: '08:12 AM',
    },
  ];

  private isAutoResponderGlobal = true;
  private isLiveLinkDiscoverActive = true;
  private isInstantAutoJoinEnabled = true; // Always running by default
  private discoveredLinks: LiveDiscoveredLink[] = [];

  // Rate Limiting & Queue state for link joins:
  // Strictly 1 minute (60,000ms) between joins & maximum 10 joins per 1 hour
  private lastJoinTime: number = 0;
  private hourlyJoinTimestamps: number[] = [];
  private joinQueue: Array<{
    linkId: string;
    resolve: (success: boolean) => void;
    reject: (err: any) => void;
  }> = [];
  private isProcessingQueue: boolean = false;

  // Metrics
  private statusMetrics: BackgroundWorkerStatus = {
    isWorkerActive: false,
    workerType: 'main-thread-fallback',
    lastProcessedTimestamp: Date.now(),
    totalMessagesProcessed: 0,
    totalLinksDiscovered: 0,
    totalAutoRepliesTriggered: 0,
  };

  // Pending callbacks map for incoming message processing
  private pendingCallbacks = new Map<string, (autoReplyText: string) => void>();
  private fallbackThrottleMap = new Map<string, number>();

  private constructor() {
    this.initWorker();
    this.initStorage();

    if (typeof window !== 'undefined') {
      window.addEventListener('telegram:session_revoked', (e: any) => {
        const reason = e?.detail?.reason || 'AUTH_KEY_UNREGISTERED';
        this.handleSessionRevoked(reason);
      });
    }
  }

  public handleSessionRevoked(reason: string = 'AUTH_KEY_UNREGISTERED') {
    console.warn(`[BackgroundSyncService] Handling session revocation (${reason}): halting automation & clearing pending callbacks.`);
    this.pendingCallbacks.clear();
    this.joinQueue.forEach((q) => q.resolve(false));
    this.joinQueue = [];
    this.isProcessingQueue = false;
    this.isInstantAutoJoinEnabled = false;
    this.notifyStateChange();
  }

  public static getInstance(): BackgroundSyncService {
    if (!BackgroundSyncService.instance) {
      BackgroundSyncService.instance = new BackgroundSyncService();
    }
    return BackgroundSyncService.instance;
  }

  private initWorker() {
    try {
      if (typeof window !== 'undefined' && window.Worker && typeof Blob !== 'undefined') {
        const blob = new Blob([WORKER_SCRIPT], { type: 'application/javascript' });
        const workerUrl = URL.createObjectURL(blob);
        this.worker = new Worker(workerUrl);

        this.worker.onmessage = this.handleWorkerMessage.bind(this);
        this.worker.onerror = (err) => {
          console.warn('[BackgroundSyncService] Worker error, switching to fallback:', err);
          this.statusMetrics.isWorkerActive = false;
          this.statusMetrics.workerType = 'main-thread-fallback';
          this.notifyStateChange();
        };

        this.statusMetrics.isWorkerActive = true;
        this.statusMetrics.workerType = 'web-worker';
      } else {
        this.statusMetrics.workerType = 'main-thread-fallback';
      }
    } catch (e) {
      console.warn('[BackgroundSyncService] Web Worker initialization failed, using fallback:', e);
      this.statusMetrics.workerType = 'main-thread-fallback';
    }
  }

  private async initStorage() {
    try {
      await initTelegramDexieDb();
      const savedLinks = await telegramDb.discoveredLinks.reverse().toArray();
      if (savedLinks && savedLinks.length > 0) {
        this.discoveredLinks = savedLinks;
      }

      // Load active private auto-replies from SQLite on initialization
      if (typeof window !== 'undefined') {
        fetch('/api/auto-replies/private/list')
          .then((res) => res.json())
          .then((data) => {
            if (data?.success && Array.isArray(data.rules)) {
              this.syncFromPrivateAutoReplies(data.rules);
            }
          })
          .catch((err) => {
            console.warn('[BackgroundSyncService] Note fetching initial rules:', err);
          });
      }

      this.syncStateToWorker();
      this.notifyStateChange();
    } catch (e) {
      console.warn('[BackgroundSyncService] IndexedDB init note:', e);
    }
  }

  public syncFromPrivateAutoReplies(rules: PrivateAutoReplyRule[]) {
    this.autoReplyRules = rules.map((r) => ({
      id: r.id,
      keyword: r.keyword,
      replyText: r.reply,
      matchType: 'contains' as const,
      scope: 'private' as const,
      isEnabled: Boolean(r.is_active),
      timesTriggered: 0,
    }));
    this.syncStateToWorker();
    this.notifyStateChange();
  }

  public syncStateToWorker() {
    if (this.worker && this.isWorkerReady) {
      this.worker.postMessage({
        type: 'SYNC_RULES',
        rules: this.autoReplyRules,
        isAutoResponderActive: this.isAutoResponderGlobal,
        isLiveDiscoverActive: this.isLiveLinkDiscoverActive,
        isInstantAutoJoinEnabled: this.isInstantAutoJoinEnabled,
      });
    }
  }

  private handleWorkerMessage(event: MessageEvent) {
    const data = event.data;
    if (!data || !data.type) return;

    switch (data.type) {
      case 'WORKER_READY': {
        this.isWorkerReady = true;
        this.syncStateToWorker();
        break;
      }

      case 'PROCESS_RESULT': {
        this.statusMetrics.totalMessagesProcessed++;
        this.statusMetrics.lastProcessedTimestamp = Date.now();

        // 1. Handle off-thread Discovered Links
        if (Array.isArray(data.discoveredLinks) && data.discoveredLinks.length > 0) {
          for (const newLink of data.discoveredLinks) {
            // Point 2: Strictly prohibit private channels (+ or joinchat)
            if (this.isPrivateChannelLink(newLink.url)) {
              newLink.status = 'failed';
              newLink.failReason = 'PRIVATE_CHANNEL_NOT_ALLOWED';
              newLink.autoJoined = false;
            }

            this.discoveredLinks.unshift(newLink);
            telegramDb.discoveredLinks.put(newLink).catch(() => {});
            this.statusMetrics.totalLinksDiscovered++;

            // Trigger rate-limited auto-join queue if enabled and not failed
            if (this.isInstantAutoJoinEnabled && newLink.status !== 'failed') {
              this.manualJoinDiscoveredLink(newLink.id);
            }
          }
        }

        // 2. Handle off-thread Auto-Responder matches
        if (data.autoReplyPayload && data.autoReplyPayload.replyText) {
          const { ruleId, replyText, delayMs } = data.autoReplyPayload;

          // Increment rule trigger stats
          const rule = this.autoReplyRules.find((r) => r.id === ruleId);
          if (rule) {
            rule.timesTriggered = (rule.timesTriggered || 0) + 1;
            rule.lastTriggeredAt = new Date().toLocaleTimeString([], {
              hour: '2-digit',
              minute: '2-digit',
            });
          }

          this.statusMetrics.totalAutoRepliesTriggered++;

          // Dispatch callback to send message
          const cb = this.pendingCallbacks.get(data.correlationId);
          if (cb) {
            setTimeout(() => {
              cb(replyText);
              this.pendingCallbacks.delete(data.correlationId);
            }, delayMs || 600);
          }
        } else {
          this.pendingCallbacks.delete(data.correlationId);
        }

        this.notifyStateChange();
        break;
      }

      case 'ACK_SYNC':
      case 'PONG': {
        break;
      }
    }
  }

  // ==========================================
  // INCOMING MESSAGE MONITORING (NON-BLOCKING)
  // ==========================================
  public processIncomingMessage(
    message: Message,
    chatTitle: string,
    chatType: 'private' | 'group' | 'channel' = 'group',
    onAutoReply?: (replyText: string) => void
  ) {
    const correlationId = `corr_${Date.now()}_${Math.random().toString(36).substr(2, 6)}`;
    if (onAutoReply) {
      this.pendingCallbacks.set(correlationId, onAutoReply);
    }

    if (this.worker && this.isWorkerReady) {
      // Offload to Web Worker thread
      this.worker.postMessage({
        type: 'PROCESS_INCOMING_MESSAGE',
        message: {
          id: message.id,
          text: message.text,
          chatId: message.chatId,
          senderName: message.senderName,
          isOutgoing: message.isOutgoing,
        },
        chatTitle,
        chatType,
        correlationId,
      });
    } else {
      // Synchronous fallback if worker is unavailable
      this.fallbackProcessIncomingMessage(message, chatTitle, chatType, onAutoReply);
    }
  }

  private fallbackProcessIncomingMessage(
    message: Message,
    chatTitle: string,
    chatType: 'private' | 'group' | 'channel',
    onAutoReply?: (replyText: string) => void
  ) {
    const text = message.text || '';
    this.statusMetrics.totalMessagesProcessed++;
    this.statusMetrics.lastProcessedTimestamp = Date.now();

    // 1. Link radar fallback
    if (this.isLiveLinkDiscoverActive && text) {
      const TG_LINK_REGEX =
        /(?:https?:\/\/)?(?:www\.)?(?:t\.me|telegram\.me|telegram\.dog)\/(?:\+([a-zA-Z0-9_-]+)|joinchat\/([a-zA-Z0-9_-]+)|([a-zA-Z0-9_]{4,}))|tg:\/\/join\?invite=([a-zA-Z0-9_-]+)/gi;
      const matches = text.matchAll(TG_LINK_REGEX);
      for (const match of matches) {
        const rawUrl = match[0];
        const fullUrl =
          rawUrl.startsWith('http') || rawUrl.startsWith('tg://') ? rawUrl : 'https://' + rawUrl;

        const isPrivate = this.isPrivateChannelLink(fullUrl);
        const discItem: LiveDiscoveredLink = {
          id: 'disc_' + Date.now() + '_' + Math.random().toString(36).substr(2, 6),
          url: fullUrl,
          sourceChatTitle: chatTitle || 'محادثة تلغرام',
          sourceChatId: message.chatId || 'chat_unknown',
          senderName: message.senderName || 'مستخدم',
          timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
          status: isPrivate ? 'skipped_private_channel' : 'pending',
          autoJoined: false,
          failReason: isPrivate ? 'قناة خاصة - تم التخطي طبقاً لتعليمات الرادار (لا ينضم للقنوات الخاصة)' : undefined,
        };

        this.discoveredLinks.unshift(discItem);
        telegramDb.discoveredLinks.put(discItem).catch(() => {});
        this.statusMetrics.totalLinksDiscovered++;

        if (this.isInstantAutoJoinEnabled && !isPrivate) {
          this.manualJoinDiscoveredLink(discItem.id);
        }
      }
    }

    // 2. Auto responder fallback (Strictly Private Chats Only)
    const isPrivateChat = chatType === 'private';
    if (this.isAutoResponderGlobal && !message.isOutgoing && text && onAutoReply && isPrivateChat) {
      const normalizeAr = (str: string) => {
        if (!str) return '';
        return str
          .replace(/[\u064B-\u065F\u0670]/g, '')
          .replace(/[إأآا]/g, 'ا')
          .replace(/ة/g, 'ه')
          .replace(/ى/g, 'ي')
          .trim()
          .toLowerCase();
      };

      const cleanText = text.trim();
      const normMsg = normalizeAr(cleanText);
      const lowerMsg = cleanText.toLowerCase();

      for (const rule of this.autoReplyRules) {
        if (!rule.isEnabled) continue;
        if (rule.scope && rule.scope !== 'private' && rule.scope !== 'all') continue;

        // Throttle 10s per rule per chat
        const throttleKey = rule.id + ':' + (message.chatId || 'default');
        const lastTrigger = this.fallbackThrottleMap.get(throttleKey) || 0;
        if (Date.now() - lastTrigger < 10000) {
          continue;
        }

        const rawKw = (rule.keyword || '').trim();
        if (!rawKw) continue;
        const normKw = normalizeAr(rawKw);
        const lowerKw = rawKw.toLowerCase();

        let isMatch = false;
        if (rule.matchType === 'exact') {
          isMatch = normMsg === normKw || lowerMsg === lowerKw;
        } else if (rule.matchType === 'regex') {
          try {
            const re = new RegExp(rule.keyword, 'i');
            isMatch = re.test(text);
          } catch (e) {
            isMatch = false;
          }
        } else {
          isMatch = normMsg.includes(normKw) || lowerMsg.includes(lowerKw);
        }

        if (isMatch) {
          this.fallbackThrottleMap.set(throttleKey, Date.now());
          rule.timesTriggered = (rule.timesTriggered || 0) + 1;
          rule.lastTriggeredAt = new Date().toLocaleTimeString([], {
            hour: '2-digit',
            minute: '2-digit',
          });
          this.statusMetrics.totalAutoRepliesTriggered++;
          const replyContent = rule.replyText || (rule as any).reply || '';
          setTimeout(() => {
            onAutoReply(replyContent);
          }, 800);
          break;
        }
      }
    }

    this.notifyStateChange();
  }

  // ==========================================
  // AUTO RESPONDER CONTROLLER METHODS
  // ==========================================
  public getAutoReplyRules(): AutoReplyRule[] {
    return this.autoReplyRules;
  }

  public setAutoReplyRules(rules: AutoReplyRule[]) {
    this.autoReplyRules = rules;
    this.syncStateToWorker();
    this.notifyStateChange();
  }

  public addAutoReplyRule(rule: Omit<AutoReplyRule, 'id' | 'timesTriggered'>) {
    const newRule: AutoReplyRule = {
      ...rule,
      id: `rule_${Date.now()}`,
      timesTriggered: 0,
    };
    this.autoReplyRules.unshift(newRule);
    this.syncStateToWorker();
    this.notifyStateChange();
  }

  public toggleRule(ruleId: string) {
    const rule = this.autoReplyRules.find((r) => r.id === ruleId);
    if (rule) {
      rule.isEnabled = !rule.isEnabled;
      this.syncStateToWorker();
      this.notifyStateChange();
    }
  }

  public deleteRule(ruleId: string) {
    this.autoReplyRules = this.autoReplyRules.filter((r) => r.id !== ruleId);
    this.syncStateToWorker();
    this.notifyStateChange();
  }

  public toggleGlobalAutoResponder(enabled: boolean) {
    this.isAutoResponderGlobal = enabled;
    this.syncStateToWorker();
    this.notifyStateChange();
  }

  public isAutoResponderActive(): boolean {
    return this.isAutoResponderGlobal;
  }

  // ==========================================
  // LIVE LINK DISCOVER CONTROLLER METHODS
  // ==========================================
  public getDiscoveredLinks(): LiveDiscoveredLink[] {
    return this.discoveredLinks;
  }

  public toggleLiveDiscover(enabled: boolean) {
    this.isLiveLinkDiscoverActive = enabled;
    this.syncStateToWorker();
    this.notifyStateChange();
  }

  public isLiveDiscoverActive(): boolean {
    return this.isLiveLinkDiscoverActive;
  }

  public toggleInstantAutoJoin(enabled: boolean) {
    this.isInstantAutoJoinEnabled = enabled;
    this.syncStateToWorker();
    this.notifyStateChange();
    try {
      localStorage.setItem('tg_radar_auto_join_enabled', String(enabled));
      localStorage.setItem('tg_auto_join_enabled_v1', String(enabled));
    } catch {}
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('tg-radar-toggle-sync', { detail: { enabled } }));
    }
  }

  public isInstantJoinEnabled(): boolean {
    return this.isInstantAutoJoinEnabled;
  }

  public clearDiscoveredLinks() {
    this.joinQueue.forEach((q) => q.resolve(false));
    this.joinQueue = [];
    this.isProcessingQueue = false;
    this.discoveredLinks = [];
    telegramDb.discoveredLinks.clear().catch(() => {});
    this.notifyStateChange();
  }

  // Point 2: Helper to detect private channel invite links (+ or joinchat)
  public isPrivateChannelLink(url: string): boolean {
    if (!url) return false;
    return url.includes('+') || url.includes('joinchat') || url.includes('invite=');
  }

  // Point 1: Rate-limited join function (queued sequentially with cooldown)
  public async manualJoinDiscoveredLink(linkId: string): Promise<boolean> {
    const item = this.discoveredLinks.find((l) => l.id === linkId);
    if (!item) return false;

    // Already joined
    if (item.status === 'joined') {
      return true;
    }

    // Prevent duplicate entries in queue
    if (this.joinQueue.some((q) => q.linkId === linkId)) {
      return false;
    }

    // Update status to pending while waiting in queue
    if (item.status !== 'joining') {
      item.status = 'pending';
      this.notifyStateChange();
    }

    return new Promise<boolean>((resolve, reject) => {
      this.joinQueue.push({ linkId, resolve, reject });
      this.processJoinQueue();
    });
  }

  // Sequential Queue Processor: enforces safety delay between any join actions
  private async processJoinQueue(): Promise<void> {
    if (this.isProcessingQueue) {
      return;
    }
    if (this.joinQueue.length === 0) {
      return;
    }

    this.isProcessingQueue = true;

    while (this.joinQueue.length > 0) {
      const queueItem = this.joinQueue[0];
      const item = this.discoveredLinks.find((l) => l.id === queueItem.linkId);

      if (!item) {
        this.joinQueue.shift();
        queueItem.resolve(false);
        continue;
      }

      // Rate Limiting Rule 1: Hourly limit (Max 20 joins per 1 hour)
      const oneHourAgo = Date.now() - 3600000;
      this.hourlyJoinTimestamps = this.hourlyJoinTimestamps.filter((t) => t > oneHourAgo);

      if (this.hourlyJoinTimestamps.length >= 20) {
        const oldestJoin = this.hourlyJoinTimestamps[0];
        const waitHourlyMs = 3600000 - (Date.now() - oldestJoin) + 1000;
        if (waitHourlyMs > 0) {
          console.warn(`[BackgroundSyncService] Hourly limit reached (20 joins/hr). Waiting ${Math.ceil(waitHourlyMs / 1000)}s...`);
          await new Promise((resolve) => setTimeout(resolve, waitHourlyMs));
        }
      }

      // Rate Limiting Rule 2: Minimum 1 minute (60,000ms) interval between joins
      const now = Date.now();
      const timeSinceLastJoin = now - this.lastJoinTime;
      const cooldownMs = 60000; // 1 minute interval

      if (this.lastJoinTime > 0 && timeSinceLastJoin < cooldownMs) {
        const waitMs = cooldownMs - timeSinceLastJoin;
        console.log(`[BackgroundSyncService] Waiting 1-minute safety cooldown (${Math.ceil(waitMs / 1000)}s)...`);
        await new Promise((resolve) => setTimeout(resolve, waitMs));
      }

      // Set status to joining
      item.status = 'joining';
      this.notifyStateChange();

      let success = false;
      const isPrivate = this.isPrivateChannelLink(item.url);
      try {
        if (isPrivate) {
          let hash = '';
          if (item.url.includes('+')) {
            hash = item.url.split('+')[1].split('/')[0].split('?')[0];
          } else if (item.url.includes('joinchat/')) {
            hash = item.url.split('joinchat/')[1].split('/')[0].split('?')[0];
          } else if (item.url.includes('invite=')) {
            hash = item.url.split('invite=')[1].split('&')[0];
          }

          if (hash) {
            await connectionsManager.sendRequest({
              _: 'TL_messages_importChatInvite',
              hash,
            });
          } else {
            throw new Error('INVITE_HASH_INVALID');
          }
        } else {
          const username = item.url
            .replace(/^https?:\/\/(?:www\.)?(?:t\.me|telegram\.me|telegram\.dog)\//i, '')
            .replace(/^tg:\/\/resolve\?domain=/i, '')
            .replace(/^@/, '')
            .split('/')[0]
            .split('?')[0];

          await connectionsManager.sendRequest({
            _: 'TL_channels_joinChannel',
            channel: { _: 'inputChannel', channel_id: username, access_hash: '0' },
          });
        }

        // Record join timestamps for rate limiter (1 min cooldown & 10/hr max)
        this.lastJoinTime = Date.now();
        this.hourlyJoinTimestamps.push(Date.now());

        // Point 3: Immediate notification to Saved Messages upon successful join
        const nowTime = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        const groupTitle =
          item.sourceChatTitle && item.sourceChatTitle !== 'محادثة تلغرام'
            ? item.sourceChatTitle
            : isPrivate
            ? 'مجموعة خاصة عبر رابط دعوة'
            : `@${item.url.split('/').pop()}`;
        const hourlyCount = this.hourlyJoinTimestamps.length;
        const typeText = isPrivate ? 'لمجموعة/قناة عبر رابط دعوة خاص' : 'لمجموعة عامة';
        const savedMessageText =
          `🔔 **رادار المراقبة والانضمام الفوري** ⚡\n\n` +
          `✅ **تم الانضمام التلقائي بنجاح ${typeText}:**\n` +
          `👥 **اسم المجموعة:** ${groupTitle}\n` +
          `🔗 **الرابط:** ${item.url}\n` +
          `💬 **محادثة المصدر:** ${item.sourceChatTitle || 'محادثة'}\n` +
          `👤 **المرسل:** ${item.senderName || 'مستخدم'}\n\n` +
          `🛡️ **ضوابط الأمان والحدود:**\n` +
          `⏱️ **الفاصل الزمني المطبق:** دقيقة واحدة بين الانضمامات\n` +
          `📊 **إحصائية الساعة:** ${hourlyCount}/10 انضمامات خلال الساعة الأخيرة\n` +
          `🕒 **توقيت الانضمام:** ${nowTime}`;

        try {
          await connectionsManager.sendRequest({
            _: 'TL_messages_sendMessage',
            peer_id: 'chat_saved_messages',
            message: savedMessageText,
            random_id: Math.floor(Math.random() * 1000000),
          });
        } catch (savedErr) {
          console.warn('[BackgroundSyncService] Notice: could not deliver notification to Saved Messages:', savedErr);
        }

        // Trigger synchronization routine immediately so local cache updates
        if (typeof connectionsManager.syncInitializationRoutine === 'function') {
          connectionsManager.syncInitializationRoutine().catch(() => {});
        }

        // Dispatch window event for UI update
        if (typeof window !== 'undefined') {
          window.dispatchEvent(
            new CustomEvent('tg-radar-link-joined', {
              detail: {
                url: item.url,
                groupTitle,
                joinedAt: nowTime,
                hourlyCount,
              },
            })
          );
        }

        item.status = 'joined';
        item.autoJoined = this.isInstantAutoJoinEnabled;
        await telegramDb.discoveredLinks
          .update(item.id, { status: 'joined', autoJoined: item.autoJoined })
          .catch(() => {});
        this.notifyStateChange();
        success = true;
      } catch (e: any) {
        const errorText = e?.text || e?.message || 'JOIN_FAILED';
        if (errorText.includes('ALREADY_PARTICIPANT') || errorText.includes('USER_ALREADY_PARTICIPANT')) {
          item.status = 'joined';
          item.failReason = undefined;
          success = true;
        } else if (errorText.includes('INVITE_REQUEST_SENT')) {
          item.status = 'pending';
          item.failReason = 'تم إرسال طلب الانضمام، بانتظار موافقة المشرف ⏳';
          success = true;
        } else {
          item.status = 'failed';
          item.failReason = errorText;
          success = false;
        }
        await telegramDb.discoveredLinks
          .update(item.id, { status: item.status, failReason: item.failReason })
          .catch(() => {});
        this.notifyStateChange();
      }

      // Dequeue and resolve promise
      this.joinQueue.shift();
      queueItem.resolve(success);
    }

    this.isProcessingQueue = false;
  }

  // ==========================================
  // STATUS & PUBSUB
  // ==========================================
  public getWorkerStatus(): BackgroundWorkerStatus {
    return { ...this.statusMetrics };
  }

  public subscribe(listener: () => void): () => void {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  }

  private notifyStateChange() {
    this.listeners.forEach((listener) => {
      try {
        listener();
      } catch (err) {
        console.error('[BackgroundSyncService] Listener callback error:', err);
      }
    });
  }
}

export const backgroundSyncService = BackgroundSyncService.getInstance();
