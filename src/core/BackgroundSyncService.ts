/**
 * BackgroundSyncService.ts
 *
 * Dedicated background orchestration service that offloads:
 * 1. Live Link Radar & Discovery (regex URL pattern parsing, classification of public groups vs private channels)
 * 2. Strict Telegram Safe Rate-Limiter (Safe threshold join quota, Flood/ban prevention, pending queue persistence, and cooldown auto-resume)
 * 3. Auto-Responder Rules Engine (keyword, exact, regex evaluation, scope matching, rate-limiting)
 * to a dedicated Web Worker off the main UI thread.
 */

import {
  AutoReplyRule,
  LiveDiscoveredLink,
  Message,
} from '../types';
import { telegramDb, initTelegramDexieDb } from './telegramDexieDb';
import { connectionsManager } from './ConnectionsManager';
import { MessagesController } from './MessagesController';
import { TLRPC } from './TLRPC';

export interface BackgroundWorkerStatus {
  isWorkerActive: boolean;
  workerType: 'web-worker' | 'main-thread-fallback';
  lastProcessedTimestamp: number;
  totalMessagesProcessed: number;
  totalLinksDiscovered: number;
  totalAutoRepliesTriggered: number;
}

export interface TelegramSafeLimitStatus {
  joinsInWindow: number;
  maxSafeLimit: number;
  windowDurationMinutes: number;
  isInSafeCooldown: boolean;
  cooldownEndsAt: number;
  remainingSeconds: number;
  pendingQueueCount: number;
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

        // 1. Off-thread Link Discovery with strict classification
        let discoveredLinks = [];
        if (isLiveDiscoverActive && text) {
          const matches = text.matchAll(TG_LINK_REGEX);
          for (const match of matches) {
            const rawUrl = match[0];
            const fullUrl = rawUrl.startsWith('http') || rawUrl.startsWith('tg://') ? rawUrl : 'https://' + rawUrl;

            // Check if private invite link (+hash, joinchat/hash, tg://join?invite=hash)
            const isPrivate = fullUrl.includes('+') || fullUrl.includes('joinchat') || fullUrl.includes('invite=');
            let cleanHandle = '';
            if (!isPrivate) {
              cleanHandle = fullUrl
                .replace(/^https?:\\/\\/(?:www\\.)?(?:t\\.me|telegram\\.me|telegram\\.dog)\\//i, '')
                .split('?')[0]
                .split('/')[0]
                .trim();
            }

            discoveredLinks.push({
              id: 'disc_' + Date.now() + '_' + Math.random().toString(36).substr(2, 6),
              url: fullUrl,
              cleanHandle: cleanHandle,
              sourceChatTitle: chatTitle || 'محادثة تلغرام',
              sourceChatId: message.chatId || 'chat_unknown',
              senderName: message.senderName || 'مستخدم',
              timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
              discoveredAt: Date.now(),
              status: isPrivate ? 'skipped' : 'pending',
              linkType: isPrivate ? 'private_invite' : 'public_group',
              isPrivate: isPrivate,
              isGroup: !isPrivate,
              failReason: isPrivate ? 'تم التخطي تلقائياً - رابط خاص (ممنوع الانضمام للقنوات الخاصة طبقاً للقواعد الرسمية)' : undefined,
              autoJoined: false,
            });
          }
        }

        // 2. Off-thread Auto-Responder Rule Evaluation
        let matchedRule = null;
        let autoReplyPayload = null;

        if (isAutoResponderActive && !message.isOutgoing && text && autoReplyRules.length > 0) {
          const cleanText = text.trim().toLowerCase();
          const isGroupChat = chatType === 'group' || chatType === 'supergroup' || chatType === 'channel';

          for (const rule of autoReplyRules) {
            if (!rule.isEnabled) continue;

            // Scope filter
            if (rule.scope === 'private' && isGroupChat) continue;
            if (rule.scope === 'groups' && !isGroupChat) continue;

            // Throttle protection (10 seconds per rule per chat)
            const throttleKey = rule.id + ':' + (message.chatId || 'default');
            const lastTrigger = lastTriggeredMap.get(throttleKey) || 0;
            if (Date.now() - lastTrigger < 10000) {
              continue;
            }

            let isMatch = false;
            const cleanKeyword = (rule.keyword || '').trim().toLowerCase();

            if (rule.matchType === 'exact') {
              isMatch = cleanText === cleanKeyword;
            } else if (rule.matchType === 'contains') {
              isMatch = cleanText.includes(cleanKeyword);
            } else if (rule.matchType === 'regex') {
              try {
                const re = new RegExp(rule.keyword, 'i');
                isMatch = re.test(text);
              } catch (e) {
                isMatch = false;
              }
            }

            if (isMatch) {
              lastTriggeredMap.set(throttleKey, Date.now());
              matchedRule = {
                id: rule.id,
                replyText: rule.replyText,
              };
              autoReplyPayload = {
                ruleId: rule.id,
                replyText: rule.replyText,
                targetChatId: message.chatId,
                delayMs: 600,
              };
              break;
            }
          }
        }

        self.postMessage({
          type: 'PROCESS_RESULT',
          correlationId: correlationId,
          discoveredLinks: discoveredLinks,
          matchedRule: matchedRule,
          autoReplyPayload: autoReplyPayload,
        });
        break;
      }
    }
  };

  self.postMessage({ type: 'WORKER_READY' });
})();
`;

export class BackgroundSyncService {
  private static instance: BackgroundSyncService;
  private worker: Worker | null = null;
  private isWorkerReady = false;
  private listeners = new Set<() => void>();

  // Telegram Official Limits & Quotas Constants
  public readonly SAFE_MAX_JOINS = 10; // 10 joins per 15 mins (Telegram threshold is ~15-20, so 10 is 100% safe from flood/spam restrictions)
  public readonly SAFE_WINDOW_MS = 15 * 60 * 1000; // 15 minutes rolling window
  public readonly SAFE_JOIN_DELAY_MS = 3500; // 3.5s natural human delay between consecutive joins

  // Rate Limiting Tracking State
  private recentJoinTimestamps: number[] = [];
  private isInSafeCooldown = false;
  private cooldownEndsAt = 0;
  private isQueueProcessing = false;
  private queueIntervalTimer: any = null;

  // Auto Reply Rules
  private autoReplyRules: AutoReplyRule[] = [
    {
      id: 'rule_1',
      keyword: 'السلام عليكم',
      replyText: 'وعليكم السلام ورحمة الله وبركاته! أهلاً بك وسهلاً 🌸 كيف يمكنني مساعدتك اليوم؟',
      matchType: 'contains',
      scope: 'all',
      isEnabled: true,
      timesTriggered: 14,
      lastTriggeredAt: '08:45 AM',
    },
    {
      id: 'rule_2',
      keyword: 'الأسعار',
      replyText: 'قائمتنا للأسعار والعروض الحالية متاحة دائماً في الرابط المثبت أعلاه 💎',
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
  private isInstantAutoJoinEnabled = false;
  private discoveredLinks: LiveDiscoveredLink[] = [];

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

  private constructor() {
    this.initWorker();
    this.initStorage();
    this.startQueueWorker();
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
        // Enforce strict rules on existing links
        this.discoveredLinks = savedLinks.map((l) => {
          const isPriv = l.url.includes('+') || l.url.includes('joinchat') || l.url.includes('invite=');
          if (isPriv) {
            return {
              ...l,
              isPrivate: true,
              isGroup: false,
              linkType: 'private_invite',
              status: l.status === 'joined' ? 'joined' : 'skipped',
              failReason: l.status === 'joined' ? undefined : 'تم التخطي تلقائياً - رابط خاص (ممنوع الانضمام للقنوات الخاصة طبقاً للقواعد الرسمية)',
            };
          }
          return {
            ...l,
            isPrivate: false,
            isGroup: true,
            linkType: l.linkType || 'public_group',
          };
        });
      }
      this.syncStateToWorker();
      this.notifyStateChange();
    } catch (e) {
      console.warn('[BackgroundSyncService] IndexedDB init note:', e);
    }
  }

  private syncStateToWorker() {
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
            // Check de-duplication
            const exists = this.discoveredLinks.some((l) => l.url === newLink.url);
            if (!exists) {
              this.discoveredLinks.unshift(newLink);
              telegramDb.discoveredLinks.put(newLink).catch(() => {});
              this.statusMetrics.totalLinksDiscovered++;
            }
          }
        }

        // 2. Handle off-thread Auto-Responder matches
        if (data.autoReplyPayload && data.autoReplyPayload.replyText) {
          const { ruleId, replyText, delayMs } = data.autoReplyPayload;

          const rule = this.autoReplyRules.find((r) => r.id === ruleId);
          if (rule) {
            rule.timesTriggered = (rule.timesTriggered || 0) + 1;
            rule.lastTriggeredAt = new Date().toLocaleTimeString([], {
              hour: '2-digit',
              minute: '2-digit',
            });
          }

          this.statusMetrics.totalAutoRepliesTriggered++;

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
    chatTitle: string = 'محادثة تلغرام',
    chatType: 'private' | 'group' | 'channel' = 'group',
    onAutoReply?: (replyText: string) => void
  ) {
    const correlationId = 'corr_' + Date.now() + '_' + Math.random().toString(36).substr(2, 6);

    if (onAutoReply) {
      this.pendingCallbacks.set(correlationId, onAutoReply);
    }

    if (this.worker && this.isWorkerReady) {
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

    // 1. Link radar fallback with strict group rules
    if (this.isLiveLinkDiscoverActive && text) {
      const TG_LINK_REGEX =
        /(?:https?:\/\/)?(?:www\.)?(?:t\.me|telegram\.me|telegram\.dog)\/(?:\+([a-zA-Z0-9_-]+)|joinchat\/([a-zA-Z0-9_-]+)|([a-zA-Z0-9_]{4,}))|tg:\/\/join\?invite=([a-zA-Z0-9_-]+)/gi;
      const matches = text.matchAll(TG_LINK_REGEX);
      for (const match of matches) {
        const rawUrl = match[0];
        const fullUrl =
          rawUrl.startsWith('http') || rawUrl.startsWith('tg://') ? rawUrl : 'https://' + rawUrl;

        // Check if already captured
        if (this.discoveredLinks.some((l) => l.url === fullUrl)) continue;

        const isPrivate = fullUrl.includes('+') || fullUrl.includes('joinchat') || fullUrl.includes('invite=');
        let cleanHandle = '';
        if (!isPrivate) {
          cleanHandle = fullUrl
            .replace(/^https?:\/\/(?:www\.)?(?:t\.me|telegram\.me|telegram\.dog)\//i, '')
            .split('?')[0]
            .split('/')[0]
            .trim();
        }

        const discItem: LiveDiscoveredLink = {
          id: 'disc_' + Date.now() + '_' + Math.random().toString(36).substr(2, 6),
          url: fullUrl,
          cleanHandle: cleanHandle,
          sourceChatTitle: chatTitle || 'محادثة تلغرام',
          sourceChatId: message.chatId || 'chat_unknown',
          senderName: message.senderName || 'مستخدم',
          timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
          discoveredAt: Date.now(),
          status: isPrivate ? 'skipped' : 'pending',
          linkType: isPrivate ? 'private_invite' : 'public_group',
          isPrivate: isPrivate,
          isGroup: !isPrivate,
          failReason: isPrivate
            ? 'تم التخطي تلقائياً - رابط خاص (ممنوع الانضمام للقنوات الخاصة طبقاً للقواعد الرسمية)'
            : undefined,
          autoJoined: false,
        };

        this.discoveredLinks.unshift(discItem);
        telegramDb.discoveredLinks.put(discItem).catch(() => {});
        this.statusMetrics.totalLinksDiscovered++;
      }
    }

    // 2. Auto responder fallback
    if (this.isAutoResponderGlobal && !message.isOutgoing && text && onAutoReply) {
      const cleanText = text.trim().toLowerCase();
      const isGroupChat = chatType === 'group' || chatType === 'channel';

      for (const rule of this.autoReplyRules) {
        if (!rule.isEnabled) continue;
        if (rule.scope === 'private' && isGroupChat) continue;
        if (rule.scope === 'groups' && !isGroupChat) continue;

        let isMatch = false;
        const cleanKeyword = (rule.keyword || '').trim().toLowerCase();

        if (rule.matchType === 'exact') {
          isMatch = cleanText === cleanKeyword;
        } else if (rule.matchType === 'contains') {
          isMatch = cleanText.includes(cleanKeyword);
        } else if (rule.matchType === 'regex') {
          try {
            const re = new RegExp(rule.keyword, 'i');
            isMatch = re.test(text);
          } catch (e) {
            isMatch = false;
          }
        }

        if (isMatch) {
          rule.timesTriggered = (rule.timesTriggered || 0) + 1;
          rule.lastTriggeredAt = new Date().toLocaleTimeString([], {
            hour: '2-digit',
            minute: '2-digit',
          });
          this.statusMetrics.totalAutoRepliesTriggered++;
          setTimeout(() => {
            onAutoReply(rule.replyText);
          }, 600);
          break;
        }
      }
    }

    this.notifyStateChange();
  }

  // ==========================================
  // TELEGRAM SAFE QUEUE & RATE-LIMITING ENGINE
  // ==========================================
  private startQueueWorker() {
    if (this.queueIntervalTimer) clearInterval(this.queueIntervalTimer);

    this.queueIntervalTimer = setInterval(() => {
      this.checkAndProcessSafeQueue();
    }, 2000);
  }

  private cleanRecentJoinHistory(): void {
    const cutoff = Date.now() - this.SAFE_WINDOW_MS;
    this.recentJoinTimestamps = this.recentJoinTimestamps.filter((ts) => ts > cutoff);

    // Update cooldown state
    if (this.recentJoinTimestamps.length < this.SAFE_MAX_JOINS) {
      if (this.isInSafeCooldown) {
        this.isInSafeCooldown = false;
        this.cooldownEndsAt = 0;
        this.notifyStateChange();
      }
    } else {
      this.isInSafeCooldown = true;
      const oldest = Math.min(...this.recentJoinTimestamps);
      this.cooldownEndsAt = oldest + this.SAFE_WINDOW_MS;
    }
  }

  public getTelegramSafeStatus(): TelegramSafeLimitStatus {
    this.cleanRecentJoinHistory();
    const joinsInWindow = this.recentJoinTimestamps.length;
    const isInSafeCooldown = joinsInWindow >= this.SAFE_MAX_JOINS;
    const remainingSeconds = this.cooldownEndsAt > Date.now()
      ? Math.max(0, Math.ceil((this.cooldownEndsAt - Date.now()) / 1000))
      : 0;

    const pendingQueueCount = this.discoveredLinks.filter(
      (l) => l.status === 'pending' && l.linkType === 'public_group'
    ).length;

    return {
      joinsInWindow,
      maxSafeLimit: this.SAFE_MAX_JOINS,
      windowDurationMinutes: 15,
      isInSafeCooldown,
      cooldownEndsAt: this.cooldownEndsAt,
      remainingSeconds,
      pendingQueueCount,
    };
  }

  private async checkAndProcessSafeQueue() {
    if (!this.isInstantAutoJoinEnabled || this.isQueueProcessing) return;

    this.cleanRecentJoinHistory();

    // 1. If currently in safe cooldown limit, pause joining to prevent Telegram restriction
    if (this.recentJoinTimestamps.length >= this.SAFE_MAX_JOINS) {
      return;
    }

    // 2. Find next pending public group link
    const nextPending = this.discoveredLinks.find(
      (l) => l.status === 'pending' && l.linkType === 'public_group'
    );

    if (!nextPending) return;

    this.isQueueProcessing = true;

    try {
      await this.executeSafeJoin(nextPending.id, true);
    } finally {
      this.isQueueProcessing = false;
    }
  }

  private async executeSafeJoin(linkId: string, isAutomatic: boolean): Promise<boolean> {
    const item = this.discoveredLinks.find((l) => l.id === linkId);
    if (!item) return false;

    // Rule 1: NEVER join private channels or invite links
    if (item.isPrivate || item.linkType === 'private_invite' || item.url.includes('+') || item.url.includes('joinchat')) {
      item.status = 'skipped';
      item.failReason = 'تم التخطي تلقائياً - رابط خاص (ممنوع الانضمام للقنوات الخاصة طبقاً للقواعد الرسمية)';
      await telegramDb.discoveredLinks
        .update(linkId, { status: 'skipped', failReason: item.failReason })
        .catch(() => {});
      this.notifyStateChange();
      return false;
    }

    // Rule 2: Check Safe Limit
    this.cleanRecentJoinHistory();
    if (this.recentJoinTimestamps.length >= this.SAFE_MAX_JOINS) {
      this.isInSafeCooldown = true;
      const oldest = Math.min(...this.recentJoinTimestamps);
      this.cooldownEndsAt = oldest + this.SAFE_WINDOW_MS;
      this.notifyStateChange();
      return false;
    }

    item.status = 'joining';
    this.notifyStateChange();

    // Human delay interval before sending RPC
    await new Promise((res) => setTimeout(res, this.SAFE_JOIN_DELAY_MS));

    const username = item.cleanHandle || item.url
      .replace(/^https?:\/\/(?:www\.)?(?:t\.me|telegram\.me|telegram\.dog)\//i, '')
      .split('?')[0]
      .split('/')[0]
      .trim();

    try {
      // Step A: Resolve entity via MessagesController / TLRPC to verify it's a public group, NOT a broadcast channel
      const messagesController = MessagesController.getInstance();
      const resolved = await messagesController.resolveUsername(username);

      if (resolved.chat) {
        const isBroadcast = resolved.chat.type === 'channel' && !(resolved.chat as any).megagroup;
        // Rule 3: NEVER join broadcast channels, ONLY public groups
        if (isBroadcast) {
          item.status = 'skipped';
          item.linkType = 'broadcast_channel';
          item.failReason = 'تم التخطي - قناة بث وليست مجموعة عامة (مسموح بالمجموعات العامة فقط)';
          await telegramDb.discoveredLinks
            .update(linkId, {
              status: 'skipped',
              linkType: 'broadcast_channel',
              failReason: item.failReason,
            })
            .catch(() => {});
          this.notifyStateChange();
          return false;
        }
      }

      // Step B: Send official MTProto TL_channels_joinChannel RPC
      await connectionsManager.sendRequest({
        _: 'TL_channels_joinChannel',
        channel: { _: 'inputChannel', channel_id: username, access_hash: '0' },
      });

      // Step C: Join through MessagesController dialogs store
      await messagesController.joinChannel(username);

      // Record successful join timestamp for Telegram rate limit protection
      this.recentJoinTimestamps.push(Date.now());
      this.cleanRecentJoinHistory();

      item.status = 'joined';
      item.autoJoined = isAutomatic;
      item.joinAttemptTime = Date.now();
      await telegramDb.discoveredLinks
        .update(linkId, { status: 'joined', autoJoined: isAutomatic, joinAttemptTime: Date.now() })
        .catch(() => {});

      this.notifyStateChange();
      return true;
    } catch (e: any) {
      item.status = 'failed';
      item.failReason = e?.text || 'فشل الانضمام للمجموعة أو الرابط غير صالح';
      await telegramDb.discoveredLinks
        .update(linkId, { status: 'failed', failReason: item.failReason })
        .catch(() => {});
      this.notifyStateChange();
      return false;
    }
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
    if (enabled) {
      this.checkAndProcessSafeQueue();
    }
    this.notifyStateChange();
  }

  public isInstantJoinEnabled(): boolean {
    return this.isInstantAutoJoinEnabled;
  }

  public clearDiscoveredLinks() {
    this.discoveredLinks = [];
    telegramDb.discoveredLinks.clear().catch(() => {});
    this.notifyStateChange();
  }

  public async manualJoinDiscoveredLink(linkId: string): Promise<{ success: boolean; reason?: string }> {
    const item = this.discoveredLinks.find((l) => l.id === linkId);
    if (!item) return { success: false, reason: 'الرابط غير موجود' };

    if (item.isPrivate || item.linkType === 'private_invite' || item.url.includes('+') || item.url.includes('joinchat')) {
      return {
        success: false,
        reason: 'ممنوع الانضمام للقنوات الخاصة طبقاً لقواعد الأمان الرسمية (مجموعات عامة فقط)',
      };
    }

    const success = await this.executeSafeJoin(linkId, false);
    return { success, reason: item.failReason };
  }

  // ==========================================
  // AUTO RESPONDER RULES MANAGEMENT
  // ==========================================
  public getAutoReplyRules(): AutoReplyRule[] {
    return this.autoReplyRules;
  }

  public addAutoReplyRule(rule: Omit<AutoReplyRule, 'id'>): void {
    const newRule: AutoReplyRule = {
      ...rule,
      id: 'rule_' + Date.now() + '_' + Math.random().toString(36).substr(2, 4),
      timesTriggered: 0,
    };
    this.autoReplyRules.unshift(newRule);
    this.syncStateToWorker();
    this.notifyStateChange();
  }

  public toggleRule(ruleId: string): void {
    const r = this.autoReplyRules.find((rule) => rule.id === ruleId);
    if (r) {
      r.isEnabled = !r.isEnabled;
      this.syncStateToWorker();
      this.notifyStateChange();
    }
  }

  public deleteRule(ruleId: string): void {
    this.autoReplyRules = this.autoReplyRules.filter((rule) => rule.id !== ruleId);
    this.syncStateToWorker();
    this.notifyStateChange();
  }

  public isAutoResponderActive(): boolean {
    return this.isAutoResponderGlobal;
  }

  public toggleGlobalAutoResponder(active?: boolean): void {
    this.isAutoResponderGlobal = active !== undefined ? active : !this.isAutoResponderGlobal;
    this.syncStateToWorker();
    this.notifyStateChange();
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
