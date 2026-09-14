/**
 * KeywordMonitor.ts (src/core/messenger/KeywordMonitor.ts)
 * 
 * TypeScript replica of DrKLO/Telegram KeywordMonitor.java
 * Monitors incoming messages for targeted phrases and forwards structured alerts to Saved Messages.
 */

import { UserConfig } from './UserConfig';
import { messagesStorage } from '../MessagesStorage';
import { NotificationCenter } from '../NotificationCenter';

export class KeywordMonitor {
  private static instances: Map<number, KeywordMonitor> = new Map();
  private currentAccount: number;
  private isEnabled: boolean = true;

  public static readonly TARGET_KEYWORDS: string[] = [
    'اريد مساعدة',
    'ابي مساعدة',
    'من يسوي تكليف',
    'من يحل',
    'عندي بحث',
    'معي واجب',
    'عندي اسايمنت',
    'من يسوي اسايمنت',
    'ابي سكليف',
    'ابي عذر',
    'من يسوي سكليف',
    'ابي شخص مضمون',
    'ابي مختص',
    'هيليب',
    'من يستطيع',
    'تعرفون احد',
    'تعرفون شخص',
    'من يساعدني',
    'من يعرف مختص',
    'مين يعرف يحل واجب',
    'من يحل واجبات الجامعه',
    'أحتاج مساعدتكم',
    'ابي احد يسوي بحث',
    'عندي بحث',
    'مين يعرف مختص',
    'من يعرف احد كويس'
  ];

  public static getInstance(account: number = 0): KeywordMonitor {
    let instance = KeywordMonitor.instances.get(account);
    if (!instance) {
      instance = new KeywordMonitor(account);
      KeywordMonitor.instances.set(account, instance);
    }
    return instance;
  }

  constructor(account: number = 0) {
    this.currentAccount = account;
  }

  public setEnabled(enabled: boolean): void {
    this.isEnabled = enabled;
  }

  public isMonitoringEnabled(): boolean {
    return this.isEnabled;
  }

  public inspectMessage(message: any): void {
    if (!this.isEnabled || !message || !message.text) return;
    if (message.isOutgoing || message.out) return;

    const text = String(message.text).trim();
    const matchedPhrase = this.findMatchingPhrase(text);

    if (matchedPhrase) {
      this.notifySavedMessages(message, matchedPhrase);
    }
  }

  public findMatchingPhrase(text: string): string | null {
    if (!text) return null;
    const normalizedText = text.toLowerCase();

    for (const phrase of KeywordMonitor.TARGET_KEYWORDS) {
      if (phrase && normalizedText.includes(phrase.toLowerCase())) {
        return phrase;
      }
    }
    return null;
  }

  private notifySavedMessages(message: any, matchedPhrase: string): void {
    try {
      const userConfig = UserConfig.getInstance(this.currentAccount);
      const selfUserId = userConfig.getClientUserId() || 'me';

      const chatName = message.chatTitle || message.chatName || message.chatId || 'محادثة خاصة';
      const senderName = message.senderName || message.sender || 'مستخدم';
      const dateStr = new Date(message.timestamp || message.date * 1000 || Date.now()).toLocaleString('ar-EG');

      const alertText = 
`🔔 تنبيه: تم رصد عبارة مستهدفة!

📌 العبارة المطابقة: ${matchedPhrase}
👥 المجموعة / المحادثة: ${chatName}
👤 اسم المرسل: ${senderName}
⏰ الوقت: ${dateStr}

💬 نص الرسالة:
${message.text}`;

      const savedMessage = {
        id: `kw_alert_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`,
        chatId: 'saved_messages',
        senderId: 'system',
        senderName: 'Keyword Monitor Bot',
        text: alertText,
        timestamp: Date.now(),
        isOutgoing: false,
        status: 'read' as const,
      };

      messagesStorage.saveMessage(savedMessage as any);

      NotificationCenter.getInstance(this.currentAccount).postNotificationName(
        NotificationCenter.didReceiveNewMessages,
        'saved_messages',
        [savedMessage]
      );
      NotificationCenter.getInstance(this.currentAccount).postNotificationName(
        NotificationCenter.dialogsNeedReload
      );
    } catch (e) {
      console.error('[KeywordMonitor] Error notifying saved messages:', e);
    }
  }
}

export const keywordMonitor = KeywordMonitor.getInstance(0);
