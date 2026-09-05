/**
 * NotificationCenter.ts - Telegram Central Event Bus
 * 
 * Replicated directly from DrKLO/Telegram Android:
 * org.telegram.messenger.NotificationCenter.java
 */

import { useState, useEffect, useRef, useCallback } from 'react';

export interface NotificationCenterDelegate {
  didReceivedNotification(id: number | string, account: number, ...args: any[]): void;
}

export function normalizeChatIdentifier(id: any): string {
  if (id === null || id === undefined) return '';
  let str = String(id).trim().toLowerCase();
  str = str.replace(/^@/, '');
  str = str.replace(/^(?:custom_|chat_|user_|channel_)+/i, '');
  if (str.startsWith('-100')) {
    str = str.slice(4);
  } else if (str.startsWith('-')) {
    str = str.slice(1);
  }
  return str;
}

export interface SalamTrackOptions {
  chatId: string | number;
  initialGreetingMsgId?: number | string;
  durationSeconds?: number;
  requiredMessages?: number;
  onProgress?: (count: number, active: boolean, remainingSeconds: number) => void;
  onActivityDetected?: (count: number, newMsg: any) => void;
  onMessageReceived?: (msg: any) => void;
}

export interface SalamTrackResult {
  active: boolean;
  messageCount: number;
  messages: any[];
  chatId: string | number;
  initialGreetingMsgId?: number | string;
  cancelled?: boolean;
  reason?: string;
}

export interface SalamSessionController {
  cancel: (reason?: string) => void;
  getMessageCount: () => number;
  getTrackedMessages: () => any[];
  isGroupActive: () => boolean;
  getRemainingSeconds: () => number;
  promise: Promise<SalamTrackResult>;
}

export class NotificationCenter {
  // DrKLO/Telegram Android Event Constants
  public static readonly didReceiveNewMessages = 1;
  public static readonly updateInterfaces = 2;
  public static readonly dialogsNeedReload = 3;
  public static readonly closeChats = 4;
  public static readonly messagesDidLoad = 5;
  public static readonly didReceivedWebsitesList = 6;
  public static readonly didReplacedPhotoInMemCache = 7;
  public static readonly notificationsCountUpdated = 8;
  public static readonly didUpdateConnectionState = 9;
  public static readonly userFullInfoDidLoad = 10;
  public static readonly pinnedInfoDidLoad = 11;
  public static readonly messagePlayingProgressDidChanged = 12;
  public static readonly messagePlayingDidReset = 13;
  public static readonly messagePlayingPlayStateChanged = 14;
  public static readonly recordProgressChanged = 15;
  public static readonly recordStartError = 16;
  public static readonly recordStopped = 17;
  public static readonly chatDidCreated = 18;
  public static readonly chatDidFailCreate = 19;
  public static readonly chatInfoDidLoad = 20;
  public static readonly contactsDidLoad = 21;
  public static readonly userSelectedEmoji = 22;
  public static readonly userSelectedSticker = 23;
  public static readonly themeDidLoad = 24;
  public static readonly needSetDayNightTheme = 25;
  public static readonly didReceivedDraft = 26;
  public static readonly messageReceivedByAck = 27;
  public static readonly messagesDeleted = 28;
  public static readonly messagesRead = 29;
  public static readonly didClearDatabase = 30;
  public static readonly checkClientRole = 31;
  public static readonly storiesUpdated = 32;
  public static readonly topicsDidLoaded = 33;
  public static readonly privacyRulesUpdated = 34;
  public static readonly mainUserInfoChanged = 35;
  public static readonly twoStepStateUpdated = 36;
  public static readonly authorizationsUpdated = 37;
  public static readonly sponsoredMessagesLoaded = 38;
  public static readonly cloudSettingsUpdated = 39;
  public static readonly downloadSettingsUpdated = 40;
  public static readonly appUpdateAvailable = 41;
  public static readonly appUpdateNotModified = 42;
  public static readonly appUpdateProgress = 43;
  public static readonly appUpdateInstallReady = 44;
  public static readonly appDidLogout = 45;
  // Smart Sender / Salam Mode Events
  public static readonly smartSenderWaitingIntervalStarted = 46;
  public static readonly smartSenderWaitingIntervalProgress = 47;
  public static readonly smartSenderWaitingIntervalEnded = 48;
  public static readonly UPDATE_MASK_READ_DIALOG_MESSAGE = 0x0001;
  public static readonly UPDATE_MASK_SELECT_DIALOG = 0x0002;
  public static readonly UPDATE_MASK_SEND_STATE = 0x0004;
  public static readonly UPDATE_MASK_ALL = 0xffff;

  private static instances = new Map<number, NotificationCenter>();
  private static globalInstance: NotificationCenter;

  private observers = new Map<number | string, Set<NotificationCenterDelegate | ((...args: any[]) => void)>>();
  private currentAccount: number;

  public static getInstance(account: number = 0): NotificationCenter {
    if (!NotificationCenter.instances.has(account)) {
      const inst = new NotificationCenter(account);
      NotificationCenter.instances.set(account, inst);
      if (account === 0 && !NotificationCenter.globalInstance) {
        NotificationCenter.globalInstance = inst;
      }
    }
    return NotificationCenter.instances.get(account)!;
  }

  public static getGlobalInstance(): NotificationCenter {
    if (!NotificationCenter.globalInstance) {
      NotificationCenter.globalInstance = NotificationCenter.getInstance(0);
    }
    return NotificationCenter.globalInstance;
  }

  private constructor(account: number = 0) {
    this.currentAccount = account;
  }

  public addObserver(
    observerOrId: NotificationCenterDelegate | ((...args: any[]) => void) | number | string,
    idOrObserver: number | string | NotificationCenterDelegate | ((...args: any[]) => void)
  ): void {
    let id: number | string;
    let observer: NotificationCenterDelegate | ((...args: any[]) => void);

    if (typeof observerOrId === 'number' || (typeof observerOrId === 'string' && typeof idOrObserver === 'function')) {
      id = observerOrId;
      observer = idOrObserver as NotificationCenterDelegate | ((...args: any[]) => void);
    } else {
      observer = observerOrId as NotificationCenterDelegate | ((...args: any[]) => void);
      id = idOrObserver as number | string;
    }

    if (!this.observers.has(id)) {
      this.observers.set(id, new Set());
    }
    this.observers.get(id)!.add(observer);
  }

  public removeObserver(
    observerOrId: NotificationCenterDelegate | ((...args: any[]) => void) | number | string,
    idOrObserver: number | string | NotificationCenterDelegate | ((...args: any[]) => void)
  ): void {
    let id: number | string;
    let observer: NotificationCenterDelegate | ((...args: any[]) => void);

    if (typeof observerOrId === 'number' || (typeof observerOrId === 'string' && typeof idOrObserver === 'function')) {
      id = observerOrId;
      observer = idOrObserver as NotificationCenterDelegate | ((...args: any[]) => void);
    } else {
      observer = observerOrId as NotificationCenterDelegate | ((...args: any[]) => void);
      id = idOrObserver as number | string;
    }

    const list = this.observers.get(id);
    if (list) {
      list.delete(observer);
      if (list.size === 0) {
        this.observers.delete(id);
      }
    }
  }

  public postNotificationName(id: number | string, ...args: any[]): void {
    const list = this.observers.get(id);
    if (list) {
      list.forEach((obs) => {
        try {
          if (typeof obs === 'function') {
            obs(...args);
          } else if (typeof obs.didReceivedNotification === 'function') {
            obs.didReceivedNotification(id, this.currentAccount, ...args);
          }
        } catch (e) {
          console.error('[NotificationCenter] Error in observer callback:', e);
        }
      });
    }

    // Also dispatch a browser CustomEvent for reactive DOM integration
    if (typeof window !== 'undefined') {
      window.dispatchEvent(
        new CustomEvent('tg-notification-center', {
          detail: { id, account: this.currentAccount, args },
        })
      );
    }
  }

  public hasObservers(id: number | string): boolean {
    return (this.observers.get(id)?.size || 0) > 0;
  }

  /**
   * Tracks incoming messages for a specific group during the 'Salam' waiting interval.
   * Enables the smart sender to verify if the group is active before deciding to edit or delete the message.
   */
  public trackSalamWaitingInterval(options: SalamTrackOptions): SalamSessionController {
    const {
      chatId,
      initialGreetingMsgId,
      durationSeconds = 30,
      requiredMessages = 3,
      onProgress,
      onActivityDetected,
      onMessageReceived,
    } = options;

    const normalizedTarget = normalizeChatIdentifier(chatId);
    const trackedMessages: any[] = [];
    const seenMessageIds = new Set<string | number>();
    let remaining = Math.max(1, durationSeconds);
    let isCancelled = false;
    let cancelReason: string | undefined;

    let timerInterval: any = null;
    let resolvePromise!: (val: SalamTrackResult) => void;
    let rejectPromise!: (err: any) => void;

    const promise = new Promise<SalamTrackResult>((resolve, reject) => {
      resolvePromise = resolve;
      rejectPromise = reject;
    });

    // Incoming messages observer
    const messageObserver = (...args: any[]) => {
      if (isCancelled) return;

      // Extract all candidate message objects from args
      const candidateMsgs: any[] = [];
      for (const arg of args) {
        if (!arg) continue;
        if (Array.isArray(arg)) {
          for (const item of arg) {
            if (item && typeof item === 'object') candidateMsgs.push(item);
          }
        } else if (typeof arg === 'object') {
          candidateMsgs.push(arg);
        }
      }

      for (const msg of candidateMsgs) {
        // Skip outgoing messages sent by the account itself
        const isOut = Boolean(msg.out || msg.isOutgoing || msg.flags?.out);
        if (isOut) continue;

        // Verify destination matches target chatId
        const msgChat =
          msg.chatId ??
          msg.peer_id ??
          msg.dialogId ??
          msg.chat_id ??
          msg.to_id?.channel_id ??
          msg.to_id?.chat_id ??
          args[0];

        const normMsgChat = normalizeChatIdentifier(msgChat);
        const matchesTarget =
          normMsgChat === normalizedTarget ||
          String(msgChat) === String(chatId) ||
          (msg.username && normalizeChatIdentifier(msg.username) === normalizedTarget);

        if (!matchesTarget) continue;

        // Verify message was sent after greeting message
        if (initialGreetingMsgId !== undefined && initialGreetingMsgId !== null && initialGreetingMsgId !== 0) {
          const mId = Number(msg.id);
          const gId = Number(initialGreetingMsgId);
          if (!isNaN(mId) && !isNaN(gId) && mId <= gId) {
            continue;
          }
        }

        // Avoid counting duplicate delivery updates for the same message ID
        const uniqueId = msg.id ?? `${msg.date}_${msg.message?.slice?.(0, 10) || ''}`;
        if (seenMessageIds.has(uniqueId)) continue;
        seenMessageIds.add(uniqueId);

        trackedMessages.push(msg);
        const currentCount = trackedMessages.length;
        const active = currentCount >= requiredMessages;

        onMessageReceived?.(msg);
        onActivityDetected?.(currentCount, msg);
        onProgress?.(currentCount, active, remaining);

        this.postNotificationName(
          NotificationCenter.smartSenderWaitingIntervalProgress,
          chatId,
          currentCount,
          active,
          remaining,
          msg
        );
      }
    };

    // Register observer on didReceiveNewMessages (both numeric 1 and string identifier)
    this.addObserver(NotificationCenter.didReceiveNewMessages, messageObserver);
    this.addObserver('didReceiveNewMessages', messageObserver);

    // Announce start of Salam waiting interval
    this.postNotificationName(
      NotificationCenter.smartSenderWaitingIntervalStarted,
      chatId,
      {
        durationSeconds,
        requiredMessages,
        initialGreetingMsgId,
      }
    );

    const finish = () => {
      if (timerInterval) {
        clearInterval(timerInterval);
        timerInterval = null;
      }
      this.removeObserver(NotificationCenter.didReceiveNewMessages, messageObserver);
      this.removeObserver('didReceiveNewMessages', messageObserver);

      const count = trackedMessages.length;
      const active = count >= requiredMessages;

      this.postNotificationName(
        NotificationCenter.smartSenderWaitingIntervalEnded,
        chatId,
        active,
        count,
        trackedMessages
      );

      resolvePromise({
        active,
        messageCount: count,
        messages: trackedMessages,
        chatId,
        initialGreetingMsgId,
        cancelled: isCancelled,
        reason: cancelReason,
      });
    };

    // Countdown timer
    timerInterval = setInterval(() => {
      remaining -= 1;
      const count = trackedMessages.length;
      const active = count >= requiredMessages;

      onProgress?.(count, active, Math.max(0, remaining));

      if (remaining <= 0) {
        finish();
      }
    }, 1000);

    const controller: SalamSessionController = {
      cancel: (reason = 'cancelled') => {
        if (isCancelled) return;
        isCancelled = true;
        cancelReason = reason;
        finish();
      },
      getMessageCount: () => trackedMessages.length,
      getTrackedMessages: () => [...trackedMessages],
      isGroupActive: () => trackedMessages.length >= requiredMessages,
      getRemainingSeconds: () => Math.max(0, remaining),
      promise,
    };

    return controller;
  }

  public static trackSalamWaitingInterval(options: SalamTrackOptions, account: number = 0): SalamSessionController {
    return NotificationCenter.getInstance(account).trackSalamWaitingInterval(options);
  }
}

export const notificationCenter = NotificationCenter.getInstance(0);

/**
 * Direct event listener hook to track incoming messages for a specific group during the Salam window.
 * Returns an unregister function.
 */
export function addSalamMessageListener(
  chatId: string | number,
  callback: (msg: any, currentCount: number, isGroupActive: boolean) => void,
  options?: { initialGreetingMsgId?: number | string; requiredMessages?: number; account?: number }
): () => void {
  const center = NotificationCenter.getInstance(options?.account || 0);
  const normalizedTarget = normalizeChatIdentifier(chatId);
  const requiredMessages = options?.requiredMessages || 3;
  const initialGreetingMsgId = options?.initialGreetingMsgId;
  let count = 0;
  const seenIds = new Set<string | number>();

  const observer = (...args: any[]) => {
    const candidateMsgs: any[] = [];
    for (const arg of args) {
      if (!arg) continue;
      if (Array.isArray(arg)) {
        for (const item of arg) {
          if (item && typeof item === 'object') candidateMsgs.push(item);
        }
      } else if (typeof arg === 'object') {
        candidateMsgs.push(arg);
      }
    }

    for (const msg of candidateMsgs) {
      const isOut = Boolean(msg.out || msg.isOutgoing || msg.flags?.out);
      if (isOut) continue;

      const msgChat =
        msg.chatId ??
        msg.peer_id ??
        msg.dialogId ??
        msg.chat_id ??
        msg.to_id?.channel_id ??
        msg.to_id?.chat_id ??
        args[0];

      if (normalizeChatIdentifier(msgChat) !== normalizedTarget && String(msgChat) !== String(chatId)) {
        continue;
      }

      if (initialGreetingMsgId !== undefined && initialGreetingMsgId !== null) {
        const mId = Number(msg.id);
        const gId = Number(initialGreetingMsgId);
        if (!isNaN(mId) && !isNaN(gId) && mId <= gId) continue;
      }

      const uniqueId = msg.id ?? `${msg.date}_${msg.message?.slice?.(0, 10) || ''}`;
      if (seenIds.has(uniqueId)) continue;
      seenIds.add(uniqueId);

      count++;
      callback(msg, count, count >= requiredMessages);
    }
  };

  center.addObserver(NotificationCenter.didReceiveNewMessages, observer);
  center.addObserver('didReceiveNewMessages', observer);

  return () => {
    center.removeObserver(NotificationCenter.didReceiveNewMessages, observer);
    center.removeObserver('didReceiveNewMessages', observer);
  };
}

export interface UseSalamMessageTrackerResult {
  isTracking: boolean;
  messageCount: number;
  requiredMessages: number;
  isGroupActive: boolean;
  remainingSeconds: number;
  messages: any[];
  startTracking: (trackOptions?: Partial<SalamTrackOptions>) => Promise<SalamTrackResult>;
  stopTracking: () => void;
  reset: () => void;
}

/**
 * React Hook to track new messages within the 'Salam' waiting interval.
 * Allows the smart sender to verify if the group is active before deciding to edit or delete the message.
 */
export function useSalamMessageTracker(
  defaultChatId?: string | number,
  defaultOptions?: Partial<SalamTrackOptions>
): UseSalamMessageTrackerResult {
  const [isTracking, setIsTracking] = useState<boolean>(false);
  const [messageCount, setMessageCount] = useState<number>(0);
  const [requiredMessages, setRequiredMessages] = useState<number>(defaultOptions?.requiredMessages || 3);
  const [isGroupActive, setIsGroupActive] = useState<boolean>(false);
  const [remainingSeconds, setRemainingSeconds] = useState<number>(defaultOptions?.durationSeconds || 30);
  const [messages, setMessages] = useState<any[]>([]);

  const controllerRef = useRef<SalamSessionController | null>(null);

  const stopTracking = useCallback(() => {
    if (controllerRef.current) {
      controllerRef.current.cancel('stopped_by_user');
      controllerRef.current = null;
    }
    setIsTracking(false);
  }, []);

  const reset = useCallback(() => {
    stopTracking();
    setMessageCount(0);
    setIsGroupActive(false);
    setRemainingSeconds(defaultOptions?.durationSeconds || 30);
    setMessages([]);
  }, [stopTracking, defaultOptions?.durationSeconds]);

  const startTracking = useCallback(
    (options?: Partial<SalamTrackOptions>): Promise<SalamTrackResult> => {
      stopTracking();

      const effectiveChatId = options?.chatId ?? defaultChatId;
      if (!effectiveChatId) {
        return Promise.reject(new Error('chatId is required to start tracking'));
      }

      const effectiveReq = options?.requiredMessages ?? defaultOptions?.requiredMessages ?? 3;
      const effectiveDuration = options?.durationSeconds ?? defaultOptions?.durationSeconds ?? 30;

      setIsTracking(true);
      setMessageCount(0);
      setRequiredMessages(effectiveReq);
      setIsGroupActive(false);
      setRemainingSeconds(effectiveDuration);
      setMessages([]);

      const center = NotificationCenter.getGlobalInstance();
      const ctrl = center.trackSalamWaitingInterval({
        chatId: effectiveChatId,
        initialGreetingMsgId: options?.initialGreetingMsgId ?? defaultOptions?.initialGreetingMsgId,
        durationSeconds: effectiveDuration,
        requiredMessages: effectiveReq,
        onProgress: (count, active, remSec) => {
          setMessageCount(count);
          setIsGroupActive(active);
          setRemainingSeconds(remSec);
          options?.onProgress?.(count, active, remSec);
        },
        onActivityDetected: (count, newMsg) => {
          setMessages((prev) => [...prev, newMsg]);
          options?.onActivityDetected?.(count, newMsg);
        },
      });

      controllerRef.current = ctrl;

      return ctrl.promise.then(
        (res) => {
          setIsTracking(false);
          setMessageCount(res.messageCount);
          setIsGroupActive(res.active);
          setRemainingSeconds(0);
          setMessages(res.messages);
          return res;
        },
        (err) => {
          setIsTracking(false);
          throw err;
        }
      );
    },
    [defaultChatId, defaultOptions, stopTracking]
  );

  useEffect(() => {
    return () => {
      if (controllerRef.current) {
        controllerRef.current.cancel('unmounted');
      }
    };
  }, []);

  return {
    isTracking,
    messageCount,
    requiredMessages,
    isGroupActive,
    remainingSeconds,
    messages,
    startTracking,
    stopTracking,
    reset,
  };
}
