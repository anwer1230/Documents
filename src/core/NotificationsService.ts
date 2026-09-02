/**
 * NotificationsService & Background Engine
 * Replicated from NotificationsService.java & ApplicationLoader.java (DrKLO/Telegram Android)
 * Handles permanent background listeners, interval schedulers, automated responses,
 * and live link radar.
 */

import { connectionsManager } from './ConnectionsManager';
import { messagesController } from './MessagesController';
import { notificationsController } from './NotificationsController';
import { TLRPC } from './TLRPC';
import { telegramDB } from '../utils/sqliteStorage';
import { telegramDb, initTelegramDexieDb } from './telegramDexieDb';
import {
  SenderBatch,
  MonitorConfig,
  MonitorAlert,
  MyMessagesBatch,
  AutoJoinerTask,
  AutoReplyRule,
  SmartAiService,
  SmartAiPattern,
  LiveDiscoveredLink,
  ProtectionMode,
  Message,
} from '../types';
import { backgroundSyncService } from './BackgroundSyncService';

export class NotificationsService {
  private static instance: NotificationsService;

  // 1. Sender & Scheduler state
  private activeSenderBatches: SenderBatch[] = [];
  private schedulerTimers: Map<string, number> = new Map();
  private currentProtectionMode: ProtectionMode = 'salam';

  // 2. Monitor state
  private monitorConfig: MonitorConfig = {
    isEnabled: false,
    keywords: ['واجب', 'بحث', 'تكليف', 'مشروع', 'مطلوب'],
    sendAlertsToSavedMessages: true,
    browserPushAlerts: true,
  };
  private monitorAlerts: MonitorAlert[] = [];

  // 3. My Messages (Batch Log)
  private batchLogs: MyMessagesBatch[] = [];

  // 4. Auto Joiner Advanced state
  private autoJoinTasks: AutoJoinerTask[] = [];
  private isAutoJoiningActive = false;

  // 5. Auto Responder state
  private autoReplyRules: AutoReplyRule[] = [
    {
      id: 'rule_1',
      keyword: 'السلام عليكم',
      replyText: 'وعليكم السلام ورحمة الله وبركاته، مرحباً بك! كيف يمكنني مساعدتك؟ 🌸',
      matchType: 'contains',
      scope: 'all',
      isEnabled: true,
      timesTriggered: 14,
    },
    {
      id: 'rule_2',
      keyword: 'الأسعار',
      replyText: 'أهلاً بك! يمكنك الاطلاع على باقاتنا وعروضنا الحالية عبر الرابط المثبت أو إرسال تفاصيل طلبك مباشرة ✨',
      matchType: 'contains',
      scope: 'all',
      isEnabled: true,
      timesTriggered: 8,
    },
  ];
  private isAutoResponderGlobal = true;

  // 6. Smart AI Learn (Groq LLM) state
  private groqApiKey = '';
  private isGroqAiEnabled = false;
  private aiServices: SmartAiService[] = [
    {
      id: 'srv_1',
      name: 'حل الواجبات والبحوث',
      description: 'مساعدة طلاب الجامعات في إعداد البحوث وحل التكاليف بدقة أكاديمية',
      keywords: ['واجب', 'بحث', 'تكليف', 'مشروع', 'تقرير', 'برزنتيشن'],
    },
    {
      id: 'srv_2',
      name: 'الترجمة الاحترافية',
      description: 'ترجمة معتمدة وسريعة للنصوص والمقالات الأكاديمية والمهنية',
      keywords: ['ترجمة', 'مقال', 'انجليزي', 'ترجم'],
    },
    {
      id: 'srv_3',
      name: 'التصميم الجرافيكي والبرمجة',
      description: 'تصميم عروض تقديمية وتطوير برمجيات ومواقع',
      keywords: ['تصميم', 'برمجة', 'موقع', 'كود', 'تطبيق', 'باوربوينت'],
    },
  ];
  private aiLearnedPatterns: SmartAiPattern[] = [
    {
      id: 'pat_1',
      triggerContext: 'استفسار عن طريقة الدفع والضمانات',
      recommendedReply: 'هلا بك والله، الدفع متاح عبر تحويل بنكي رسمي ومعتمد، والتسليم يتم خطوة بخطوة مع ضمان التعديل المجاني 👍',
      learnedDate: '2026-08-20',
      isAccepted: true,
    },
  ];

  // 7. Live Link Discover & Instant Auto-Join
  private isLiveLinkDiscoverActive = true;
  private isInstantAutoJoinEnabled = false;
  private discoveredLinks: LiveDiscoveredLink[] = [];

  // Listeners for UI state reactivity
  private stateSubscribers = new Set<() => void>();

  public static getInstance(): NotificationsService {
    if (!NotificationsService.instance) {
      NotificationsService.instance = new NotificationsService();
    }
    return NotificationsService.instance;
  }

  constructor() {
    this.loadInitialStorage();
    this.startBackgroundWatchers();

    // Synchronize reactivity with Web Worker BackgroundSyncService
    backgroundSyncService.subscribe(() => {
      this.notifyStateChange();
    });
  }

  private async loadInitialStorage() {
    try {
      await initTelegramDexieDb();
      const savedBatches = await telegramDb.myMessageBatches.reverse().toArray();
      if (savedBatches && savedBatches.length > 0) {
        this.batchLogs = savedBatches;
      } else {
        this.batchLogs = [
          {
            id: 'batch_101',
            text: 'عرض خاص لجميع الطلاب والطالبات! خصم 20% على جميع الخدمات الأكاديمية والاستشارات 🎓✨',
            hasImages: true,
            imagesCount: 1,
            groupsCount: 12,
            targets: [
              { chatId: 'chat_1', chatTitle: 'جروب المطورين التقني', messageId: 'msg_b1' },
              { chatId: 'chat_2', chatTitle: 'قناة التصميم والإبداع', messageId: 'msg_b2' },
            ],
            date: '2026-08-22',
            timestamp: '08:15 AM',
          },
        ];
        await telegramDb.myMessageBatches.bulkPut(this.batchLogs);
      }

      const savedLinks = await telegramDb.discoveredLinks.reverse().toArray();
      if (savedLinks && savedLinks.length > 0) {
        this.discoveredLinks = savedLinks;
      }

      const savedAlerts = await telegramDb.monitorAlerts.reverse().toArray();
      if (savedAlerts && savedAlerts.length > 0) {
        this.monitorAlerts = savedAlerts;
      }

      // Load monitor config from localStorage
      const storedMonitorCfg = localStorage.getItem('tg_monitor_config_v2');
      if (storedMonitorCfg) {
        try {
          this.monitorConfig = { ...this.monitorConfig, ...JSON.parse(storedMonitorCfg) };
        } catch {}
      }

      this.notifyStateChange();
    } catch (err) {
      console.warn('[Dexie Storage] Failed to load from IndexedDB, using in-memory state:', err);
    }
  }

  public subscribe(listener: () => void): () => void {
    this.stateSubscribers.add(listener);
    return () => this.stateSubscribers.delete(listener);
  }

  private notifyStateChange() {
    this.stateSubscribers.forEach((l) => l());
  }

  private startBackgroundWatchers() {
    console.log('[Telegram NotificationsService] Permanent Background Service started.');
  }

  // ==========================================
  // 1. SENDER & SCHEDULER IMPLEMENTATION
  // ==========================================
  public async executeSendBatch(params: {
    text: string;
    images: string[];
    targetChatIds: string[];
    allChats: { id: string; title: string; type: any }[];
    protectionMode: ProtectionMode;
    isScheduled: boolean;
    intervalMinutes?: number;
    durationHours?: number;
    onMessageCreated?: (chatId: string, text: string, mediaUrl?: string) => void;
  }): Promise<SenderBatch> {
    const batchId = `batch_${Date.now()}`;
    const targetObjs: {
      id: string;
      title: string;
      type: any;
      status: 'sent' | 'failed' | 'skipped' | 'protected';
      messageId: string;
      error?: string;
    }[] = params.targetChatIds.map((id) => {
      const found = params.allChats.find((c) => c.id === id);
      return {
        id,
        title: found?.title || 'مجموعة تيليجرام',
        type: (found?.type || 'group') as any,
        status: 'sent',
        messageId: `msg_${Date.now()}_${Math.random().toString(36).substring(7)}`,
      };
    });

    let successCount = 0;
    let failedCount = 0;

    for (const target of targetObjs) {
      // 1. Check protection & clean text
      let preparedText = params.text;
      if (params.protectionMode === 'smart_clean' || params.protectionMode === 'permanent_clean') {
        preparedText = preparedText
          .replace(/(?:https?:\/\/|t\.me\/)[^\s]+/gi, '')
          .replace(/\b\d{8,14}\b/g, '')
          .trim();
      }

      if (params.protectionMode === 'salam') {
        // Salam mechanism: simulate greeting first
        preparedText = 'السلام عليكم ورحمة الله وبركاته\n' + preparedText;
      }

      try {
        await connectionsManager.sendRequest({
          _: 'TL_messages_sendMessage',
          peer_id: target.id,
          message: preparedText,
          random_id: Math.floor(Math.random() * 1000000),
        });

        // Broadcast to TelegramContext to update UI state
        if (typeof window !== 'undefined') {
          window.dispatchEvent(
            new CustomEvent('tg_send_batch_message', {
              detail: {
                chatId: target.id,
                chatTitle: target.title,
                text: preparedText,
                mediaUrl: params.images[0],
                messageId: target.messageId,
              },
            })
          );
        }

        if (params.onMessageCreated) {
          params.onMessageCreated(target.id, preparedText, params.images[0]);
        }
        target.status = 'sent';
        successCount++;
      } catch (err: any) {
        target.status = 'failed';
        target.error = err?.text || 'SEND_ERROR';
        failedCount++;
      }
    }

    const batch: SenderBatch = {
      id: batchId,
      text: params.text,
      images: params.images,
      targetChats: targetObjs,
      protectionMode: params.protectionMode,
      isScheduled: params.isScheduled,
      intervalMinutes: params.intervalMinutes || 15,
      durationHours: params.durationHours || 0,
      createdAt: new Date().toLocaleDateString(),
      sentAt: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      totalSuccess: successCount,
      totalFailed: failedCount,
      status: params.isScheduled ? 'running' : 'completed',
    };

    this.activeSenderBatches.unshift(batch);
    telegramDb.senderBatches.put(batch).catch(() => {});

    // Save to My Messages Batch Log
    const newBatchLog: MyMessagesBatch = {
      id: batchId,
      text: params.text,
      hasImages: params.images.length > 0,
      imagesCount: params.images.length,
      groupsCount: targetObjs.length,
      targets: targetObjs.map((t) => ({ chatId: t.id, chatTitle: t.title, messageId: t.messageId! })),
      date: new Date().toLocaleDateString(),
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    };
    this.batchLogs.unshift(newBatchLog);
    telegramDb.myMessageBatches.put(newBatchLog).catch(() => {});

    // Set up recurring scheduler if requested
    if (params.isScheduled && (params.intervalMinutes || 15) > 0) {
      const intervalMs = (params.intervalMinutes || 15) * 60 * 1000;
      const timerId = window.setInterval(async () => {
        console.log(`[Scheduled Sender] Running scheduled batch ${batchId}...`);
        for (const target of targetObjs) {
          try {
            await connectionsManager.sendRequest({
              _: 'TL_messages_sendMessage',
              peer_id: target.id,
              message: params.text,
              random_id: Math.floor(Math.random() * 1000000),
            });
            window.dispatchEvent(
              new CustomEvent('tg_send_batch_message', {
                detail: {
                  chatId: target.id,
                  chatTitle: target.title,
                  text: params.text,
                  mediaUrl: params.images[0],
                  messageId: `msg_${Date.now()}_${Math.random().toString(36).substring(7)}`,
                },
              })
            );
          } catch (_) {}
        }
        notificationsController.postNotification({
          category: 'message',
          title: '🔄 تنفيذ الجدولة الدورية للدفعة',
          body: `تم إرسال الحملة إلى ${targetObjs.length} محادثة بنجاح`,
        });
      }, intervalMs);

      this.schedulerTimers.set(batchId, timerId);

      // Handle duration expiration if durationHours > 0
      if (params.durationHours && params.durationHours > 0) {
        setTimeout(() => {
          this.cancelScheduledBatch(batchId);
        }, params.durationHours * 60 * 60 * 1000);
      }
    }

    notificationsController.postNotification({
      category: 'message',
      title: params.isScheduled ? '⏳ تم تفعيل الجدولة الدورية بنجاح' : 'اكتمال دفعة الإرسال بنجاح 🚀',
      body: params.isScheduled
        ? `سيتم الإرسال كل ${params.intervalMinutes || 15} دقيقة إلى ${targetObjs.length} محادثة`
        : `تم إرسال الرسالة إلى ${successCount} مجموعة (${failedCount} فشل)`,
    });

    this.notifyStateChange();
    return batch;
  }

  public cancelScheduledBatch(batchId: string): boolean {
    const timerId = this.schedulerTimers.get(batchId);
    if (timerId) {
      clearInterval(timerId);
      this.schedulerTimers.delete(batchId);
    }
    const found = this.activeSenderBatches.find((b) => b.id === batchId);
    if (found) {
      found.status = 'paused';
      this.notifyStateChange();
      return true;
    }
    return false;
  }

  // ==========================================
  // 2. LIVE MONITOR & KEYWORD RADAR
  // ==========================================
  public setMonitorConfig(config: Partial<MonitorConfig>) {
    this.monitorConfig = { ...this.monitorConfig, ...config };
    try {
      localStorage.setItem('tg_monitor_config_v2', JSON.stringify(this.monitorConfig));
    } catch {}
    this.notifyStateChange();
  }

  public getMonitorConfig(): MonitorConfig {
    return this.monitorConfig;
  }

  public getMonitorAlerts(): MonitorAlert[] {
    return this.monitorAlerts;
  }

  public clearMonitorAlerts() {
    this.monitorAlerts = [];
    telegramDb.monitorAlerts.clear().catch(() => {});
    this.notifyStateChange();
  }

  public clearAlerts() {
    this.clearMonitorAlerts();
  }

  // ==========================================
  // 3. MY MESSAGES (BATCH LOGS, EDIT & DELETE)
  // ==========================================
  public getBatchLogs(): MyMessagesBatch[] {
    return this.batchLogs;
  }

  public async editBatch(batchId: string, newText: string): Promise<boolean> {
    const batch = this.batchLogs.find((b) => b.id === batchId);
    if (!batch) return false;

    batch.text = newText;
    await telegramDb.myMessageBatches.update(batchId, { text: newText }).catch(() => {});

    for (const target of batch.targets) {
      await connectionsManager.sendRequest({
        _: 'TL_messages_editMessage',
        peer_id: target.chatId,
        id: target.messageId,
        message: newText,
      });
    }

    if (typeof window !== 'undefined') {
      window.dispatchEvent(
        new CustomEvent('tg_batch_edited', {
          detail: {
            batchId,
            newText,
            targets: batch.targets,
          },
        })
      );
    }

    notificationsController.postNotification({
      category: 'message',
      title: 'تم تعديل الدفعة بنجاح ✏️',
      body: `تم تحديث نص الرسالة في ${batch.targets.length} مجموعة بالكامل`,
    });

    this.notifyStateChange();
    return true;
  }

  public async deleteBatch(batchId: string): Promise<boolean> {
    const idx = this.batchLogs.findIndex((b) => b.id === batchId);
    if (idx === -1) return false;

    const batch = this.batchLogs[idx];
    await telegramDb.myMessageBatches.delete(batchId).catch(() => {});

    for (const target of batch.targets) {
      await connectionsManager.sendRequest({
        _: 'TL_messages_deleteMessages',
        peer_id: target.chatId,
        id: target.messageId,
        revoke: true,
      });
    }

    if (typeof window !== 'undefined') {
      window.dispatchEvent(
        new CustomEvent('tg_batch_deleted', {
          detail: {
            batchId,
            targets: batch.targets,
          },
        })
      );
    }

    this.batchLogs.splice(idx, 1);
    notificationsController.postNotification({
      category: 'message',
      title: 'تم حذف الدفعة نهائياً 🗑️',
      body: `تم سحب وحذف الرسائل من كافة المجموعات المرسل إليها`,
    });

    this.notifyStateChange();
    return true;
  }

  // ==========================================
  // 4. AUTO JOINER ADVANCED
  // ==========================================
  public extractLinksFromRawText(text: string): string[] {
    const regex = /(?:https?:\/\/)?(?:t(?:elegram)?\.me\/(?:\+|joinchat\/)?|@)([A-Za-z0-9_+\-\/]+)/gi;
    const matches = text.match(regex) || [];
    return Array.from(new Set(matches));
  }

  public async startAutoJoinTasks(links: string[], onProgress?: (processed: number, total: number) => void) {
    this.isAutoJoiningActive = true;
    const tasks: AutoJoinerTask[] = links.map((url) => ({
      id: `task_${Date.now()}_${Math.random().toString(36).substring(7)}`,
      url,
      type: url.includes('+') || url.includes('joinchat') ? 'private' : 'public',
      status: 'pending',
    }));

    this.autoJoinTasks = tasks;
    this.notifyStateChange();

    let processed = 0;
    for (const task of tasks) {
      if (!this.isAutoJoiningActive) break;
      task.status = 'joining';
      this.notifyStateChange();

      // Short delay between joins to respect MTProto flood control
      await new Promise((res) => setTimeout(res, 1200));

      let chatTitle = 'مجموعة منضمة حديثاً';
      let username = '';
      let isSuccess = false;

      if (task.type === 'private') {
        const hash = task.url.substring(task.url.lastIndexOf('/') + 1).replace('+', '');
        try {
          await connectionsManager.sendRequest({
            _: 'TL_messages_importChatInvite',
            hash,
          });
          task.status = 'joined';
          chatTitle = `قناة خاصة (${hash.slice(0, 8)})`;
          isSuccess = true;
        } catch (e: any) {
          task.status = 'invalid';
          task.errorReason = e?.text || 'INVITE_HASH_EXPIRED';
        }
      } else {
        username = task.url.substring(task.url.lastIndexOf('/') + 1).replace('@', '');
        try {
          await connectionsManager.sendRequest({
            _: 'TL_channels_joinChannel',
            channel: username,
          });
          task.status = 'joined';
          chatTitle = username.replace(/_/g, ' ');
          isSuccess = true;
        } catch (e: any) {
          task.status = 'invalid';
          task.errorReason = e?.text || 'CHANNEL_PRIVATE';
        }
      }

      // If joined successfully, trigger chat addition in TelegramContext
      if (isSuccess && typeof window !== 'undefined') {
        window.dispatchEvent(
          new CustomEvent('tg_chat_joined', {
            detail: {
              id: `joined_${Date.now()}_${Math.random().toString(36).substr(2, 4)}`,
              title: chatTitle,
              username: username ? `@${username}` : undefined,
              type: task.type === 'private' ? 'channel' : 'group',
              url: task.url,
            },
          })
        );
      }

      task.processedAt = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
      processed++;
      if (onProgress) onProgress(processed, tasks.length);
      this.notifyStateChange();
    }

    this.isAutoJoiningActive = false;
    notificationsController.postNotification({
      category: 'channel_post',
      title: 'اكتمال عملية الانضمام التلقائي ✨',
      body: `تمت معالجة ${tasks.length} رابط بنجاح`,
    });
    this.notifyStateChange();
  }

  public getAutoJoinTasks(): AutoJoinerTask[] {
    return this.autoJoinTasks;
  }

  public stopAutoJoin() {
    this.isAutoJoiningActive = false;
    this.notifyStateChange();
  }

  // ==========================================
  // 5. AUTO RESPONDER IMPLEMENTATION
  // ==========================================
  public getAutoReplyRules(): AutoReplyRule[] {
    return backgroundSyncService.getAutoReplyRules();
  }

  public addAutoReplyRule(rule: Omit<AutoReplyRule, 'id' | 'timesTriggered'>) {
    backgroundSyncService.addAutoReplyRule(rule);
  }

  public toggleRule(id: string) {
    backgroundSyncService.toggleRule(id);
  }

  public deleteRule(id: string) {
    backgroundSyncService.deleteRule(id);
  }

  public isAutoResponderActive(): boolean {
    return backgroundSyncService.isAutoResponderActive();
  }

  public toggleGlobalAutoResponder(val: boolean) {
    backgroundSyncService.toggleGlobalAutoResponder(val);
  }

  // ==========================================
  // 6. SMART AI LEARN (Groq LLM)
  // ==========================================
  public setGroqApiKey(key: string) {
    this.groqApiKey = key;
    this.notifyStateChange();
  }

  public getGroqApiKey(): string {
    return this.groqApiKey;
  }

  public toggleGroqAi(enabled: boolean) {
    this.isGroqAiEnabled = enabled;
    this.notifyStateChange();
  }

  public isGroqEnabled(): boolean {
    return this.isGroqAiEnabled;
  }

  public getAiServices(): SmartAiService[] {
    return this.aiServices;
  }

  public addAiService(service: Omit<SmartAiService, 'id'>) {
    this.aiServices.push({
      ...service,
      id: `srv_${Date.now()}`,
    });
    this.notifyStateChange();
  }

  public deleteAiService(id: string) {
    this.aiServices = this.aiServices.filter((s) => s.id !== id);
    this.notifyStateChange();
  }

  public getAiPatterns(): SmartAiPattern[] {
    return this.aiLearnedPatterns;
  }

  public async generateGroqGulfReply(userMessage: string): Promise<string> {
    if (!this.groqApiKey) {
      // Intelligent Gulf template fallback
      return this.getSmartGulfFallback(userMessage);
    }

    try {
      const response = await fetch('https://api.groq.com/openai/v1/chat/completions', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${this.groqApiKey}`,
        },
        body: JSON.stringify({
          model: 'llama-3.3-70b-versatile',
          messages: [
            {
              role: 'system',
              content:
                'أنت مساعد ذكي احترافي. رد بلهجة خليجية ودية، مختصرة جداً (جملة أو جملتين كحد أقصى)، وبشكل لبق ومباشر يلبي استفسار العميل فوراً.',
            },
            {
              role: 'user',
              content: userMessage,
            },
          ],
          temperature: 0.6,
          max_tokens: 150,
        }),
      });

      if (response.ok) {
        const data = await response.json();
        const text = data.choices?.[0]?.message?.content?.trim();
        if (text) return text;
      }
    } catch (e) {
      console.warn('[Groq AI Engine] Fallback used due to network or key:', e);
    }

    return this.getSmartGulfFallback(userMessage);
  }

  private getSmartGulfFallback(userMessage: string): string {
    const lower = userMessage.toLowerCase();
    if (lower.includes('سعر') || lower.includes('كم') || lower.includes('تكلفة')) {
      return 'يا هلا وغلا! تفاصيل الأسعار حسب متطلباتك بالضبط، أرسل لي التفاصيل وأبشر بأفضل عرض وخصم يرضيك 🌟';
    }
    if (lower.includes('واجب') || lower.includes('بحث') || lower.includes('مشروع')) {
      return 'أهلاً بك! نعم نقدر نساعدك بأعلى دقة أكاديمية وتسليم سريع، تواصل معي بالتفاصيل وتدلل 👍';
    }
    if (lower.includes('متى') || lower.includes('وقت') || lower.includes('تسليم')) {
      return 'يا مرحبا، التسليم يتم بأسرع وقت ممكن حسب موعدك المحدد مع مراجعة وتعديل مجاني ⏱️';
    }
    return 'هلا والله ومسهلا! تسعدنا خدمتك بكل سرور، كيف أقدر أساعدك اليوم؟ ✨';
  }

  // ==========================================
  // 7. LIVE LINK DISCOVER & INSTANT AUTO-JOIN
  // ==========================================
  public isLiveDiscoverActive(): boolean {
    return backgroundSyncService.isLiveDiscoverActive();
  }

  public toggleLiveDiscover(val: boolean) {
    backgroundSyncService.toggleLiveDiscover(val);
  }

  public isInstantJoinEnabled(): boolean {
    return backgroundSyncService.isInstantJoinEnabled();
  }

  public toggleInstantAutoJoin(val: boolean) {
    backgroundSyncService.toggleInstantAutoJoin(val);
  }

  public getDiscoveredLinks(): LiveDiscoveredLink[] {
    return backgroundSyncService.getDiscoveredLinks();
  }

  public clearDiscoveredLinks() {
    backgroundSyncService.clearDiscoveredLinks();
  }

  public async manualJoinDiscoveredLink(linkId: string): Promise<boolean> {
    const res = await backgroundSyncService.manualJoinDiscoveredLink(linkId);
    return res.success;
  }

  // ==========================================
  // INCOMING MESSAGE DISPATCHER (HOOKS ALL SYSTEMS)
  // ==========================================
  public async handleIncomingMessage(
    message: Message,
    chatTitle: string,
    onAutoReply?: (replyText: string) => void
  ) {
    const text = message.text || '';

    // 1. Keyword Monitor Engine (Replicating DrKLO Live Message Scanner)
    if (this.monitorConfig.isEnabled && this.monitorConfig.keywords.length > 0) {
      for (const kw of this.monitorConfig.keywords) {
        if (kw.trim() && text.toLowerCase().includes(kw.toLowerCase())) {
          const alert: MonitorAlert = {
            id: `alert_${Date.now()}`,
            keyword: kw,
            sourceChatId: message.chatId,
            sourceChatTitle: chatTitle,
            senderName: message.senderName || 'مستخدم',
            messageText: text,
            timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
          };
          this.monitorAlerts.unshift(alert);
          telegramDb.monitorAlerts.put(alert).catch(() => {});

          // Forward to saved messages if enabled
          if (this.monitorConfig.sendAlertsToSavedMessages && typeof window !== 'undefined') {
            window.dispatchEvent(
              new CustomEvent('tg_send_saved_message', {
                detail: {
                  text: `🚨 تنبيه رصد كلمة مفتاحية: [${kw}]\n📍 المصدر: ${chatTitle}\n👤 الكاتب: ${message.senderName || 'مستخدم'}\n💬 الرسالة: ${text}\n⏰ الوقت: ${alert.timestamp}`,
                },
              })
            );
          }

          // Construct DrKLO-compliant Intent metadata:
          // 1. chatId: dialog_id
          // 2. messageId: message.id (for scroll-to-message)
          // 3. senderId: message.from_id (for direct 1-on-1 private chat)
          // 4. chatUsername & senderUsername for deep links
          notificationsController.postNotification({
            category: 'keyword_alert',
            title: `🚨 كلمة مراقبة: [${kw}]`,
            body: `💬 الرسالة: ${text}\n📍 المصدر: ${chatTitle}`,
            avatar: message.senderAvatar,
            chatId: String(message.chatId || ''),
            chatTitle: chatTitle,
            messageId: String(message.id),
            senderId: message.senderId ? String(message.senderId) : (message.senderName ? `user_${message.senderName.replace(/\s+/g, '_')}` : undefined),
            senderName: message.senderName || 'مستخدم',
            senderUsername: message.senderUsername,
            keyword: kw,
            messageText: text,
            replyAction: true,
            isSilent: !this.monitorConfig.browserPushAlerts,
          });
          break;
        }
      }
    }

    // 2. Groq Smart AI Engine
    if (this.isGroqAiEnabled && !message.isOutgoing && text && onAutoReply) {
      const reply = await this.generateGroqGulfReply(text);
      setTimeout(() => {
        onAutoReply(reply);
      }, 1000);
    }
  }
}

export const notificationsService = NotificationsService.getInstance();
