/**
 * Telegram Official AutoResponder Engine
 * Handles rule evaluation, keyword matching, and automated response dispatching
 */

import { ConnectionsManager } from '../tgnet/ConnectionsManager';
import { UserConfig } from './UserConfig';

export interface AutoReplyRuleItem {
  id: string;
  trigger: string;
  response: string;
  matchType?: 'exact' | 'contains' | 'regex';
  isActive: boolean;
  delayMs?: number;
}

export class AutoResponderEngine {
  private static instance: AutoResponderEngine;
  private isEnabled: boolean = true;
  private rules: AutoReplyRuleItem[] = [];

  private constructor() {
    this.loadRules();
  }

  public static getInstance(): AutoResponderEngine {
    if (!AutoResponderEngine.instance) {
      AutoResponderEngine.instance = new AutoResponderEngine();
    }
    return AutoResponderEngine.instance;
  }

  public setEnabled(enabled: boolean): void {
    this.isEnabled = enabled;
  }

  public isAutoReplyActive(): boolean {
    return this.isEnabled;
  }

  public getRules(): AutoReplyRuleItem[] {
    return this.rules;
  }

  public addRule(trigger: string, response: string, matchType: 'exact' | 'contains' | 'regex' = 'contains'): void {
    const newRule: AutoReplyRuleItem = {
      id: String(Date.now()),
      trigger: trigger.trim().toLowerCase(),
      response: response.trim(),
      matchType,
      isActive: true,
      delayMs: 1000,
    };
    this.rules.push(newRule);
    this.saveRules();
  }

  public removeRule(id: string): void {
    this.rules = this.rules.filter((r) => r.id !== id);
    this.saveRules();
  }

  public toggleRule(id: string): void {
    const rule = this.rules.find((r) => r.id === id);
    if (rule) {
      rule.isActive = !rule.isActive;
      this.saveRules();
    }
  }

  /**
   * Processes incoming message and auto-replies if rule matches
   */
  public async processIncomingMessage(msg: any): Promise<boolean> {
    if (!this.isEnabled || !msg || msg.out || msg.from_me) return false;
    const text = (msg.message || msg.text || '').trim().toLowerCase();
    if (!text) return false;

    const myId = UserConfig.getClientUserId();
    const senderId = msg.from_id?.user_id || msg.sender_id || msg.from_id;
    if (String(senderId) === String(myId)) return false;

    for (const rule of this.rules) {
      if (!rule.isActive) continue;

      let isMatch = false;
      if (rule.matchType === 'exact') {
        isMatch = text === rule.trigger;
      } else if (rule.matchType === 'regex') {
        try {
          const rx = new RegExp(rule.trigger, 'i');
          isMatch = rx.test(text);
        } catch {
          isMatch = false;
        }
      } else {
        isMatch = text.includes(rule.trigger);
      }

      if (isMatch) {
        const chatId = msg.peer_id?.channel_id || msg.peer_id?.chat_id || msg.peer_id?.user_id || msg.chat_id || senderId;
        const delay = rule.delayMs || 1000;

        setTimeout(async () => {
          try {
            await ConnectionsManager.getInstance().sendRequest('sendMessage', {
              peer: chatId,
              message: rule.response,
              reply_to: msg.id,
            });
          } catch (e) {
            console.error('[AutoResponder] Failed to send reply:', e);
          }
        }, delay);

        return true;
      }
    }

    return false;
  }

  private loadRules(): void {
    try {
      const saved = localStorage.getItem('tg_auto_reply_rules');
      if (saved) {
        this.rules = JSON.parse(saved);
      } else {
        // Default seed rules
        this.rules = [
          {
            id: '1',
            trigger: 'السلام عليكم',
            response: 'وعليكم السلام ورحمة الله وبركاته، أهلاً بك! كيف يمكنني خدمتك اليوم؟',
            matchType: 'contains',
            isActive: true,
            delayMs: 1200,
          },
          {
            id: '2',
            trigger: 'السعر',
            response: 'مرحباً بك! يمكنك الاطلاع على تفاصيل الأسعار والعروض عبر الرسائل المباشرة أو الملف التعريفي.',
            matchType: 'contains',
            isActive: true,
            delayMs: 1500,
          },
        ];
      }
    } catch {}
  }

  private saveRules(): void {
    try {
      localStorage.setItem('tg_auto_reply_rules', JSON.stringify(this.rules));
    } catch {}
  }
}
