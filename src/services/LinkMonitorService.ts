/**
 * LinkMonitorService.ts
 *
 * Dedicated Link Monitor & Auto-Join Radar service.
 * - Scans incoming messages in real-time across all chats.
 * - Detects Telegram group & channel links (both public usernames and private invite links).
 * - Configurable support for private invite links (+, /joinchat/, invite=).
 * - Enforces rate limiting: 1-minute cooldown between joins & maximum 20 joins per hour.
 * - Sends rich notification updates to the local user's private chat (Saved Messages).
 * - Triggers instant cloud cache synchronization upon successful joins.
 * - Supports instant manual join (joinNow) bypassing queues.
 */

import { ConnectionsManager } from '../core/ConnectionsManager';
import { telegramDb } from '../core/telegramDexieDb';
import { TLRPC } from '../core/TLRPC';

export interface MonitoredLinkItem {
  id: string;
  url: string;
  sourceChatTitle: string;
  sourceChatId: string;
  senderName: string;
  timestamp: string;
  status: 'pending' | 'joining' | 'joined' | 'failed' | 'skipped_private_channel' | 'already_member';
  failReason?: string;
  autoJoined: boolean;
  username?: string;
  joinedAt?: string;
}

export type LinkMonitorListener = (state: {
  enabled: boolean;
  allowPrivateInvites: boolean;
  links: MonitoredLinkItem[];
  hourlyCount: number;
  lastJoinTime: number;
}) => void;

export class LinkMonitorService {
  private static instance: LinkMonitorService | null = null;

  // Configuration constants
  private readonly COOLDOWN_MS = 60000; // Strictly 1 minute between joins
  private readonly MAX_HOURLY_JOINS = 20; // Maximum 20 joins per hour (1 every minute)
  private readonly HOURLY_WINDOW_MS = 3600000; // 1 hour sliding window

  // State
  private enabled: boolean = true;
  private allowPrivateInvites: boolean = true;
  private links: MonitoredLinkItem[] = [];
  private lastJoinTime: number = 0;
  private hourlyJoinTimestamps: number[] = [];
  private joinQueue: Array<{
    item: MonitoredLinkItem;
    resolve: (success: boolean) => void;
  }> = [];
  private isProcessingQueue: boolean = false;
  private listeners: Set<LinkMonitorListener> = new Set();

  private constructor() {
    this.loadPersistedState();
    this.setupWindowListeners();
  }

  public static getInstance(): LinkMonitorService {
    if (!LinkMonitorService.instance) {
      LinkMonitorService.instance = new LinkMonitorService();
    }
    return LinkMonitorService.instance;
  }

  /**
   * Loads state from localStorage
   */
  private loadPersistedState(): void {
    if (typeof window === 'undefined') return;

    try {
      // 1. Activation state
      const savedEnabled = localStorage.getItem('tg_radar_auto_join_enabled');
      if (savedEnabled !== null) {
        this.enabled = savedEnabled === 'true';
      } else {
        const legacyEnabled = localStorage.getItem('tg_auto_join_enabled_v1');
        this.enabled = legacyEnabled !== null ? legacyEnabled === 'true' : true;
      }

      // 2. Allow Private Invite links state (default true)
      const savedPrivate = localStorage.getItem('tg_radar_allow_private_invites');
      if (savedPrivate !== null) {
        this.allowPrivateInvites = savedPrivate === 'true';
      } else {
        this.allowPrivateInvites = true;
      }

      // 3. Rate limit timestamps
      const savedLastTime = Number(localStorage.getItem('tg_radar_last_join_time')) || 0;
      this.lastJoinTime = savedLastTime;

      const savedHourly = localStorage.getItem('tg_radar_hourly_joins');
      if (savedHourly) {
        const arr = JSON.parse(savedHourly);
        if (Array.isArray(arr)) {
          const oneHourAgo = Date.now() - this.HOURLY_WINDOW_MS;
          this.hourlyJoinTimestamps = arr.filter((t: number) => typeof t === 'number' && t > oneHourAgo);
        }
      }

      // 4. Captured links cache
      const savedLinks = localStorage.getItem('tg_radar_monitored_links');
      if (savedLinks) {
        const parsed = JSON.parse(savedLinks);
        if (Array.isArray(parsed)) {
          this.links = parsed.slice(0, 100);
        }
      }
    } catch (e) {
      console.warn('[LinkMonitorService] Failed to load persisted state:', e);
    }
  }

  /**
   * Persists state to localStorage
   */
  private savePersistedState(): void {
    if (typeof window === 'undefined') return;

    try {
      localStorage.setItem('tg_radar_auto_join_enabled', String(this.enabled));
      localStorage.setItem('tg_auto_join_enabled_v1', String(this.enabled));
      localStorage.setItem('tg_radar_allow_private_invites', String(this.allowPrivateInvites));
      localStorage.setItem('tg_radar_last_join_time', String(this.lastJoinTime));
      localStorage.setItem('tg_radar_hourly_joins', JSON.stringify(this.hourlyJoinTimestamps));
      localStorage.setItem('tg_radar_monitored_links', JSON.stringify(this.links.slice(0, 100)));
    } catch (e) {
      console.warn('[LinkMonitorService] Failed to save state:', e);
    }
  }

  /**
   * Sets up window listeners for cross-component and external message routing
   */
  private setupWindowListeners(): void {
    if (typeof window === 'undefined') return;

    // Listen for incoming messages dispatched globally
    window.addEventListener('tg-incoming-message', ((event: CustomEvent) => {
      const { message, chatTitle } = event.detail || {};
      if (message) {
        this.scanIncomingMessage(message, chatTitle);
      }
    }) as EventListener);
  }

  /**
   * Whether the radar auto-join is currently enabled
   */
  public isEnabled(): boolean {
    return this.enabled;
  }

  /**
   * Sets radar activation state
   */
  public setEnabled(value: boolean): void {
    this.enabled = value;
    this.savePersistedState();
    this.notifyListeners();
  }

  /**
   * Toggles radar activation state
   */
  public toggle(): boolean {
    this.setEnabled(!this.enabled);
    return this.enabled;
  }

  /**
   * Whether auto-joining private invite links is allowed
   */
  public isAllowPrivateInvites(): boolean {
    return this.allowPrivateInvites;
  }

  /**
   * Sets whether auto-joining private invite links is allowed
   */
  public setAllowPrivateInvites(value: boolean): void {
    this.allowPrivateInvites = value;
    this.savePersistedState();
    this.notifyListeners();
  }

  /**
   * Gets the remaining cooldown time in seconds before next join
   */
  public getCooldownRemaining(): number {
    if (!this.lastJoinTime) return 0;
    const elapsed = Date.now() - this.lastJoinTime;
    return Math.max(0, Math.ceil((this.COOLDOWN_MS - elapsed) / 1000));
  }

  /**
   * Gets the number of joins performed in the last 1 hour
   */
  public getHourlyCount(): number {
    const oneHourAgo = Date.now() - this.HOURLY_WINDOW_MS;
    this.hourlyJoinTimestamps = this.hourlyJoinTimestamps.filter((t) => t > oneHourAgo);
    return this.hourlyJoinTimestamps.length;
  }

  public getHourlyLimit(): number {
    return this.MAX_HOURLY_JOINS;
  }

  /**
   * Gets timestamp of the last executed join
   */
  public getLastJoinTime(): number {
    return this.lastJoinTime;
  }

  /**
   * Returns list of all captured and monitored links
   */
  public getLinks(): MonitoredLinkItem[] {
    return [...this.links];
  }

  /**
   * Clears the history of discovered links
   */
  public clearLinks(): void {
    this.links = [];
    this.savePersistedState();
    this.notifyListeners();
  }

  /**
   * Subscribes a listener to radar state updates
   */
  public addListener(listener: LinkMonitorListener): () => void {
    this.listeners.add(listener);
    listener({
      enabled: this.enabled,
      allowPrivateInvites: this.allowPrivateInvites,
      links: this.links,
      hourlyCount: this.getHourlyCount(),
      lastJoinTime: this.lastJoinTime,
    });
    return () => {
      this.listeners.delete(listener);
    };
  }

  private notifyListeners(): void {
    const state = {
      enabled: this.enabled,
      allowPrivateInvites: this.allowPrivateInvites,
      links: this.links,
      hourlyCount: this.getHourlyCount(),
      lastJoinTime: this.lastJoinTime,
    };
    this.listeners.forEach((l) => {
      try {
        l(state);
      } catch (err) {
        console.error('[LinkMonitorService] Listener error:', err);
      }
    });

    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('tg-radar-state-change', { detail: state }));
    }
  }

  /**
   * Checks if a link points to a private channel or invite link
   */
  public isPrivateChannelLink(url: string): boolean {
    if (!url) return false;
    const lower = url.toLowerCase();
    return (
      lower.includes('/+') ||
      lower.includes('/joinchat/') ||
      lower.includes('join?invite=') ||
      lower.includes('invite=')
    );
  }

  /**
   * Extracts private invite hash from URL
   */
  public extractInviteHash(url: string): string | null {
    if (!url) return null;
    const plusMatch = url.match(/(?:\+|\/joinchat\/|invite=)([a-zA-Z0-9_-]{10,})/i);
    if (plusMatch && plusMatch[1]) {
      return plusMatch[1];
    }
    const clean = url
      .replace(/^(https?:\/\/)?(?:www\.)?(?:t\.me|telegram\.me|telegram\.dog)\/(joinchat\/|\+)?/i, '')
      .split('?')[0]
      .split('/')[0]
      .trim();
    if (clean && clean.length >= 10) {
      return clean;
    }
    return null;
  }

  /**
   * Checks if a link is a public Telegram group or channel link
   */
  public isPublicGroupLink(url: string): boolean {
    if (!url || this.isPrivateChannelLink(url)) return false;
    const publicMatch = url.match(/(?:https?:\/\/)?(?:www\.)?(?:t\.me|telegram\.me|telegram\.dog)\/([a-zA-Z0-9_]{4,})/i);
    return Boolean(
      publicMatch && !['joinchat', 'c', 'addstickers', 'proxy', 'share', 'login'].includes(publicMatch[1].toLowerCase())
    );
  }

  /**
   * Extracts Telegram links from text
   */
  public extractLinks(text: string): string[] {
    if (!text) return [];
    const TG_LINK_REGEX =
      /(?:https?:\/\/)?(?:www\.)?(?:t\.me|telegram\.me|telegram\.dog)\/(?:\+([a-zA-Z0-9_-]+)|joinchat\/([a-zA-Z0-9_-]+)|([a-zA-Z0-9_]{4,}))|tg:\/\/join\?invite=([a-zA-Z0-9_-]+)/gi;
    const matches = text.match(TG_LINK_REGEX);
    if (!matches) return [];

    return Array.from(new Set(matches)).map((rawUrl) =>
      rawUrl.startsWith('http') || rawUrl.startsWith('tg://') ? rawUrl : 'https://' + rawUrl
    );
  }

  /**
   * Scans an incoming message in real-time across any chat
   */
  public scanIncomingMessage(
    message: {
      id?: string | number;
      text?: string;
      chatId?: string;
      senderName?: string;
      isOut?: boolean;
    },
    chatTitle?: string
  ): void {
    if (!message || !message.text) return;
    // Don't monitor our own sent outgoing messages
    if (message.isOut) return;

    const urls = this.extractLinks(message.text);
    if (urls.length === 0) return;

    for (const fullUrl of urls) {
      this.processFoundLink(fullUrl, message, chatTitle);
    }
  }

  /**
   * Processes a discovered link according to radar rules
   */
  private processFoundLink(
    url: string,
    message: { id?: string | number; chatId?: string; senderName?: string },
    chatTitle?: string
  ): void {
    const isPrivate = this.isPrivateChannelLink(url);
    const existingIndex = this.links.findIndex((l) => l.url.toLowerCase() === url.toLowerCase());

    const shouldSkipPrivate = isPrivate && !this.allowPrivateInvites;

    const item: MonitoredLinkItem = {
      id:
        existingIndex >= 0
          ? this.links[existingIndex].id
          : 'radar_' + Date.now() + '_' + Math.random().toString(36).substr(2, 6),
      url,
      sourceChatTitle: chatTitle || 'محادثة تلغرام',
      sourceChatId: message.chatId || 'chat_unknown',
      senderName: message.senderName || 'مستخدم',
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      status: shouldSkipPrivate ? 'skipped_private_channel' : 'pending',
      failReason: shouldSkipPrivate
        ? 'قناة خاصة - تم التخطي (الانضمام للقنوات الخاصة معطل)'
        : undefined,
      autoJoined: false,
    };

    if (existingIndex >= 0) {
      this.links[existingIndex] = { ...this.links[existingIndex], ...item };
    } else {
      this.links.unshift(item);
    }

    if (this.links.length > 150) {
      this.links = this.links.slice(0, 150);
    }

    this.savePersistedState();
    this.notifyListeners();

    // Persist in IndexedDB
    try {
      telegramDb.discoveredLinks
        .put({
          id: item.id,
          url: item.url,
          sourceChatTitle: item.sourceChatTitle,
          sourceChatId: item.sourceChatId,
          senderName: item.senderName,
          timestamp: item.timestamp,
          status: item.status,
          failReason: item.failReason,
          autoJoined: item.autoJoined,
        })
        .catch(() => {});
    } catch {}

    // If private channel links are disabled by setting, skip it
    if (shouldSkipPrivate) {
      console.log(`[LinkMonitorService] Private channel link skipped per setting: ${url}`);
      return;
    }

    // If auto-join radar is enabled, enqueue for joining
    if (this.enabled) {
      this.enqueueJoin(item);
    }
  }

  /**
   * Enqueues a link for rate-limited auto-joining
   */
  public enqueueJoin(item: MonitoredLinkItem): Promise<boolean> {
    if (this.isPrivateChannelLink(item.url) && !this.allowPrivateInvites) {
      item.status = 'skipped_private_channel';
      item.failReason = 'قناة خاصة - تم التخطي (الانضمام للقنوات الخاصة معطل)';
      this.savePersistedState();
      this.notifyListeners();
      return Promise.resolve(false);
    }

    return new Promise((resolve) => {
      // Avoid duplicate queueing
      if (this.joinQueue.some((q) => q.item.url.toLowerCase() === item.url.toLowerCase())) {
        resolve(false);
        return;
      }

      this.joinQueue.push({ item, resolve });
      this.processQueue();
    });
  }

  /**
   * Sequentially processes the join queue obeying:
   * 1. Strictly 1-minute (60,000 ms) safety cooldown between joins
   * 2. Max 20 joins per 1 hour sliding window
   */
  private async processQueue(): Promise<void> {
    if (this.isProcessingQueue) return;
    this.isProcessingQueue = true;

    try {
      while (this.joinQueue.length > 0) {
        const queueEntry = this.joinQueue[0];
        const { item, resolve } = queueEntry;

        // Verify if private channel is allowed
        const isPrivate = this.isPrivateChannelLink(item.url);
        if (isPrivate && !this.allowPrivateInvites) {
          this.joinQueue.shift();
          item.status = 'skipped_private_channel';
          item.failReason = 'قناة خاصة - تم التخطي (الانضمام للقنوات الخاصة معطل)';
          this.savePersistedState();
          this.notifyListeners();
          resolve(false);
          continue;
        }

        // 1. Check Hourly Rate Limit (Max 20 joins per hour)
        const oneHourAgo = Date.now() - this.HOURLY_WINDOW_MS;
        this.hourlyJoinTimestamps = this.hourlyJoinTimestamps.filter((t) => t > oneHourAgo);

        if (this.hourlyJoinTimestamps.length >= this.MAX_HOURLY_JOINS) {
          const oldestJoin = this.hourlyJoinTimestamps[0];
          const waitHourlyMs = this.HOURLY_WINDOW_MS - (Date.now() - oldestJoin) + 1000;
          if (waitHourlyMs > 0) {
            console.warn(
              `[LinkMonitorService] Hourly limit reached (${this.MAX_HOURLY_JOINS}/hr). Waiting ${Math.ceil(
                waitHourlyMs / 1000
              )}s...`
            );
            await new Promise((r) => setTimeout(r, waitHourlyMs));
          }
        }

        // 2. Check 1-minute cooldown between consecutive joins
        const now = Date.now();
        const timeSinceLastJoin = now - this.lastJoinTime;

        if (this.lastJoinTime > 0 && timeSinceLastJoin < this.COOLDOWN_MS) {
          const waitMs = this.COOLDOWN_MS - timeSinceLastJoin;
          console.log(`[LinkMonitorService] Waiting 1-minute safety cooldown (${Math.ceil(waitMs / 1000)}s)...`);
          await new Promise((r) => setTimeout(r, waitMs));
        }

        // 3. Execute the actual join via MTProto
        item.status = 'joining';
        this.notifyListeners();

        let success = false;
        if (isPrivate) {
          const hash = this.extractInviteHash(item.url);
          if (hash) {
            success = await this.executePrivateJoin(item, hash);
          } else {
            item.status = 'failed';
            item.failReason = 'تعذر استخراج رمز الدعوة';
            this.savePersistedState();
            this.notifyListeners();
          }
        } else {
          success = await this.executePublicJoin(item);
        }

        // Dequeue entry and resolve
        this.joinQueue.shift();
        resolve(success);
      }
    } finally {
      this.isProcessingQueue = false;
    }
  }

  /**
   * Executes public group join and sends updates to the user's private chat (Saved Messages)
   */
  private async executePublicJoin(item: MonitoredLinkItem): Promise<boolean> {
    const publicMatch = item.url.match(
      /(?:https?:\/\/)?(?:www\.)?(?:t\.me|telegram\.me|telegram\.dog)\/([a-zA-Z0-9_]{4,})/i
    );
    const username = publicMatch ? publicMatch[1] : '';

    if (!username) {
      item.status = 'failed';
      item.failReason = 'تعذر استخراج اسم المجموعة العام';
      this.savePersistedState();
      this.notifyListeners();
      return false;
    }

    try {
      const connectionsManager = ConnectionsManager.getInstance();

      // Send real MTProto join request
      await connectionsManager.sendRequest({
        _: 'TL_channels_joinChannel',
        channel: { _: 'inputChannel', channel_id: username, access_hash: '0' },
      });

      // Update rate-limiting timestamps
      const joinTimestamp = Date.now();
      this.lastJoinTime = joinTimestamp;
      this.hourlyJoinTimestamps.push(joinTimestamp);

      item.status = 'joined';
      item.autoJoined = true;
      item.username = username;
      item.joinedAt = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

      this.savePersistedState();
      this.notifyListeners();

      // Send notification update to user's private chat (Saved Messages)
      await this.sendNotificationToPrivateChat(item, username, false);

      // Invoke cloud syncInitializationRoutine immediately so local cache updates
      if (typeof connectionsManager.syncInitializationRoutine === 'function') {
        connectionsManager.syncInitializationRoutine().catch(() => {});
      }

      // Dispatch global window event
      if (typeof window !== 'undefined') {
        window.dispatchEvent(
          new CustomEvent('tg-radar-link-joined', {
            detail: {
              url: item.url,
              groupTitle: item.sourceChatTitle || `@${username}`,
              joinedAt: item.joinedAt,
              hourlyCount: this.getHourlyCount(),
            },
          })
        );
      }

      return true;
    } catch (err: any) {
      console.warn('[LinkMonitorService] Failed to auto-join public group:', err);
      const errMsg = err?.message || err?.text || 'خطأ أثناء الانضمام';

      if (errMsg.includes('ALREADY_PARTICIPANT') || errMsg.includes('USER_ALREADY_PARTICIPANT')) {
        item.status = 'already_member';
      } else if (errMsg.includes('INVITE_REQUEST_SENT')) {
        item.status = 'pending';
        item.failReason = 'تم إرسال طلب الانضمام، بانتظار موافقة المشرف ⏳';
      } else if (errMsg.includes('CHANNELS_TOO_MUCH')) {
        item.status = 'failed';
        item.failReason = 'تم الوصول للحد الأقصى للقنوات والمجموعات في حسابك';
      } else {
        item.status = 'failed';
        item.failReason = errMsg;
      }

      this.savePersistedState();
      this.notifyListeners();
      return false;
    }
  }

  /**
   * Executes private invite link join via messages.importChatInvite
   */
  private async executePrivateJoin(item: MonitoredLinkItem, hash: string): Promise<boolean> {
    try {
      const connectionsManager = ConnectionsManager.getInstance();

      // Send real MTProto import invite request
      const res: any = await connectionsManager.sendRequest({
        _: 'TL_messages_importChatInvite',
        hash,
      });

      // Update rate-limiting timestamps
      const joinTimestamp = Date.now();
      this.lastJoinTime = joinTimestamp;
      this.hourlyJoinTimestamps.push(joinTimestamp);

      if (res?.status === 'pending_approval') {
        item.status = 'pending';
        item.failReason = 'تم إرسال طلب الانضمام، بانتظار موافقة المشرف ⏳';
        this.savePersistedState();
        this.notifyListeners();
        return true;
      }

      item.status = 'joined';
      item.autoJoined = true;
      item.joinedAt = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

      this.savePersistedState();
      this.notifyListeners();

      // Send notification update to user's private chat (Saved Messages)
      await this.sendNotificationToPrivateChat(item, hash, true);

      // Invoke cloud syncInitializationRoutine immediately so local cache updates
      if (typeof connectionsManager.syncInitializationRoutine === 'function') {
        connectionsManager.syncInitializationRoutine().catch(() => {});
      }

      // Dispatch global window event
      if (typeof window !== 'undefined') {
        window.dispatchEvent(
          new CustomEvent('tg-radar-link-joined', {
            detail: {
              url: item.url,
              groupTitle: item.sourceChatTitle || 'مجموعة خاصة',
              joinedAt: item.joinedAt,
              hourlyCount: this.getHourlyCount(),
            },
          })
        );
      }

      return true;
    } catch (err: any) {
      console.warn('[LinkMonitorService] Failed to auto-join private group:', err);
      const errMsg = err?.message || err?.text || 'خطأ أثناء الانضمام';

      if (errMsg.includes('ALREADY_PARTICIPANT') || errMsg.includes('USER_ALREADY_PARTICIPANT')) {
        item.status = 'already_member';
      } else if (errMsg.includes('INVITE_REQUEST_SENT')) {
        item.status = 'pending';
        item.failReason = 'تم إرسال طلب الانضمام، بانتظار موافقة المشرف ⏳';
      } else if (errMsg.includes('CHANNELS_TOO_MUCH')) {
        item.status = 'failed';
        item.failReason = 'تم الوصول للحد الأقصى للقنوات والمجموعات في حسابك';
      } else {
        item.status = 'failed';
        item.failReason = errMsg;
      }

      this.savePersistedState();
      this.notifyListeners();
      return false;
    }
  }

  /**
   * Manual direct join bypassing queue restrictions
   */
  public async joinNow(urlOrItem: string | MonitoredLinkItem): Promise<{ success: boolean; message?: string }> {
    const url = typeof urlOrItem === 'string' ? urlOrItem : urlOrItem.url;
    let item: MonitoredLinkItem | undefined =
      typeof urlOrItem === 'object' ? urlOrItem : this.links.find((l) => l.url.toLowerCase() === url.toLowerCase());

    if (!item) {
      item = {
        id: 'radar_manual_' + Date.now(),
        url,
        sourceChatTitle: 'انضمام يدوي',
        sourceChatId: 'manual',
        senderName: 'أنا',
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        status: 'joining',
        autoJoined: false,
      };
      this.links.unshift(item);
    } else {
      item.status = 'joining';
    }
    this.savePersistedState();
    this.notifyListeners();

    const isPrivate = this.isPrivateChannelLink(url);
    let success = false;

    if (isPrivate) {
      const hash = this.extractInviteHash(url);
      if (hash) {
        success = await this.executePrivateJoin(item, hash);
      } else {
        item.status = 'failed';
        item.failReason = 'تعذر استخراج رمز الدعوة';
        this.savePersistedState();
        this.notifyListeners();
        return { success: false, message: 'تعذر استخراج رمز الدعوة' };
      }
    } else {
      success = await this.executePublicJoin(item);
    }

    if (item.status === 'already_member') {
      return { success: true, message: 'أنت عضو بالفعل في هذه المجموعة/القناة' };
    }
    if (item.failReason?.includes('موافقة المشرف')) {
      return { success: true, message: 'تم إرسال طلب الانضمام، بانتظار موافقة المشرف' };
    }
    if (success) {
      return { success: true, message: 'تم الانضمام بنجاح!' };
    }
    return { success: false, message: item.failReason || 'فشل الانضمام للمجموعة' };
  }

  /**
   * Sends rich notification update to the local user's private chat (Saved Messages / chat_saved)
   */
  private async sendNotificationToPrivateChat(
    item: MonitoredLinkItem,
    targetIdentifier: string,
    isPrivate: boolean
  ): Promise<void> {
    const groupTitle =
      item.sourceChatTitle && item.sourceChatTitle !== 'محادثة تلغرام'
        ? item.sourceChatTitle
        : isPrivate
        ? 'مجموعة خاصة عبر رابط دعوة'
        : `@${targetIdentifier}`;
    const nowTime = item.joinedAt || new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    const hourlyCount = this.getHourlyCount();

    const typeDesc = isPrivate ? 'لمجموعة/قناة عبر رابط دعوة خاص' : 'لمجموعة عامة';

    const notificationMessage =
      `🔔 **رادار المراقبة والانضمام الفوري (Link Monitor Radar)** ⚡\n\n` +
      `✅ **تم الانضمام التلقائي بنجاح ${typeDesc}:**\n` +
      `👥 **اسم المجموعة:** ${groupTitle}\n` +
      `🔗 **الرابط:** ${item.url}\n` +
      `💬 **محادثة المصدر:** ${item.sourceChatTitle || 'محادثة'}\n` +
      `👤 **المرسل:** ${item.senderName || 'مستخدم'}\n\n` +
      `🛡️ **ضوابط الأمان والحدود:**\n` +
      `⏱️ **الفاصل الزمني المطبق:** دقيقة واحدة بين الانضمامات\n` +
      `📊 **إحصائية الساعة:** ${hourlyCount}/${this.MAX_HOURLY_JOINS} انضمامات خلال الساعة الأخيرة\n` +
      `🕒 **توقيت الانضمام:** ${nowTime}`;

    try {
      const connectionsManager = ConnectionsManager.getInstance();
      await connectionsManager.sendRequest({
        _: 'TL_messages_sendMessage',
        peer: { _: 'inputPeerSelf' },
        message: notificationMessage,
        random_id: Math.floor(Math.random() * 1e9),
      });
    } catch (sendErr) {
      console.warn('[LinkMonitorService] Failed to send notification to Saved Messages via RPC, trying endpoint:', sendErr);
      try {
        const sessionString =
          localStorage.getItem('tg_session_string') ||
          localStorage.getItem('telegram_session_string') ||
          '';
        const phone =
          localStorage.getItem('tg_phone') ||
          localStorage.getItem('telegram_phone') ||
          '';
        if (sessionString && phone) {
          await fetch('/api/telegram/send-message', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              chatId: 'chat_saved',
              text: notificationMessage,
              sessionString,
              phone,
            }),
          });
        }
      } catch (postErr) {
        console.warn('[LinkMonitorService] Failed to post notification via fallback endpoint:', postErr);
      }
    }
  }
}

// Global instance export
export const linkMonitorService = LinkMonitorService.getInstance();
