import fs from 'fs';
import path from 'path';

export interface AutoReplyRule {
  id: string;
  trigger: string;
  response: string;
  isActive: boolean;
  matchType?: 'exact' | 'contains' | 'regex';
  createdAt: number;
}

export interface PrivateAutoReply {
  id: string;
  senderPattern?: string;
  text: string;
  isActive: boolean;
  createdAt: number;
}

export interface MessageBatch {
  id: string;
  title: string;
  recipientsCount: number;
  status: 'pending' | 'sent' | 'failed';
  createdAt: number;
}

class SQLiteDatabaseService {
  private autoRepliesEnabled: boolean = true;
  private cachedMessages: Map<string, any[]> = new Map();
  private rules: AutoReplyRule[] = [
    {
      id: 'rule_welcome',
      trigger: '/start',
      response: 'مرحباً بك! أنا بوت الترحيب الذكي لتطبيق تليجرام ويب.',
      isActive: true,
      matchType: 'contains',
      createdAt: Date.now(),
    },
    {
      id: 'rule_help',
      trigger: '/help',
      response: 'قائمة الأوامر:\n/start - بدء المحادثة\n/help - المساعدة\n/rules - القواعد',
      isActive: true,
      matchType: 'exact',
      createdAt: Date.now(),
    },
  ];
  private privateAutoReplies: PrivateAutoReply[] = [
    {
      id: 'p_auto_1',
      senderPattern: '*',
      text: 'مرحباً! تلقيت رسالتك وسأقوم بالرد في أقرب وقت ممكن.',
      isActive: false,
      createdAt: Date.now(),
    },
  ];
  private monitorKeywords: string[] = ['تليجرام', 'مهم', 'urgent', 'telegram', 'ton', 'bot'];
  private batches: MessageBatch[] = [];

  saveCachedMessages(peerId: string, messages: any[]): void {
    this.cachedMessages.set(peerId, messages);
  }

  getCachedMessages(peerId: string): any[] {
    return this.cachedMessages.get(peerId) || [];
  }

  isAutoRepliesEnabled(): boolean {
    return this.autoRepliesEnabled;
  }

  setAutoRepliesEnabled(enabled: boolean): void {
    this.autoRepliesEnabled = enabled;
  }

  getRules(): AutoReplyRule[] {
    return this.rules;
  }

  addRule(data: Partial<AutoReplyRule>): AutoReplyRule {
    const newRule: AutoReplyRule = {
      id: 'rule_' + Date.now(),
      trigger: data.trigger || '',
      response: data.response || '',
      isActive: data.isActive ?? true,
      matchType: data.matchType || 'contains',
      createdAt: Date.now(),
    };
    this.rules.unshift(newRule);
    return newRule;
  }

  updateRule(id: string, updates: Partial<AutoReplyRule>): AutoReplyRule | null {
    const idx = this.rules.findIndex((r) => r.id === id);
    if (idx === -1) return null;
    this.rules[idx] = { ...this.rules[idx], ...updates };
    return this.rules[idx];
  }

  deleteRule(id: string): boolean {
    const before = this.rules.length;
    this.rules = this.rules.filter((r) => r.id !== id);
    return this.rules.length < before;
  }

  toggleRule(id: string): AutoReplyRule | null {
    const rule = this.rules.find((r) => r.id === id);
    if (!rule) return null;
    rule.isActive = !rule.isActive;
    return rule;
  }

  getPrivateAutoReplies(): PrivateAutoReply[] {
    return this.privateAutoReplies;
  }

  addPrivateAutoReply(data: Partial<PrivateAutoReply>): PrivateAutoReply {
    const reply: PrivateAutoReply = {
      id: 'p_auto_' + Date.now(),
      senderPattern: data.senderPattern || '*',
      text: data.text || '',
      isActive: data.isActive ?? true,
      createdAt: Date.now(),
    };
    this.privateAutoReplies.unshift(reply);
    return reply;
  }

  updatePrivateAutoReply(id: string, updates: Partial<PrivateAutoReply>): PrivateAutoReply | null {
    const idx = this.privateAutoReplies.findIndex((r) => r.id === id);
    if (idx === -1) return null;
    this.privateAutoReplies[idx] = { ...this.privateAutoReplies[idx], ...updates };
    return this.privateAutoReplies[idx];
  }

  deletePrivateAutoReply(id: string): boolean {
    const before = this.privateAutoReplies.length;
    this.privateAutoReplies = this.privateAutoReplies.filter((r) => r.id !== id);
    return this.privateAutoReplies.length < before;
  }

  togglePrivateAutoReply(id: string): PrivateAutoReply | null {
    const reply = this.privateAutoReplies.find((r) => r.id === id);
    if (!reply) return null;
    reply.isActive = !reply.isActive;
    return reply;
  }

  getMonitorKeywords(): string[] {
    return this.monitorKeywords;
  }

  setMonitorKeywords(keywords: string[]): string[] {
    this.monitorKeywords = keywords;
    return this.monitorKeywords;
  }

  getBatches(): MessageBatch[] {
    return this.batches;
  }

  addBatch(data: Partial<MessageBatch>): MessageBatch {
    const batch: MessageBatch = {
      id: 'batch_' + Date.now(),
      title: data.title || 'حملة رسائل جديدة',
      recipientsCount: data.recipientsCount || 0,
      status: data.status || 'pending',
      createdAt: Date.now(),
    };
    this.batches.unshift(batch);
    return batch;
  }

  deleteBatch(id: string): boolean {
    const before = this.batches.length;
    this.batches = this.batches.filter((b) => b.id !== id);
    return this.batches.length < before;
  }

  getStats(): {
    cachedMessagesTotal: number;
    rulesCount: number;
    privateRepliesCount: number;
    batchesCount: number;
  } {
    let totalMessages = 0;
    for (const list of this.cachedMessages.values()) {
      totalMessages += list.length;
    }
    return {
      cachedMessagesTotal: totalMessages,
      rulesCount: this.rules.length,
      privateRepliesCount: this.privateAutoReplies.length,
      batchesCount: this.batches.length,
    };
  }
}

export const sqliteDatabase = new SQLiteDatabaseService();
