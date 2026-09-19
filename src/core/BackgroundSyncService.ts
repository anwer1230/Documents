/**
 * BackgroundSyncService.ts - Background worker sync runner & automation engine
 */

import { AutoReplyRule } from '../types';

export interface BackgroundWorkerStatus {
  workerType: 'web-worker' | 'fallback';
  status?: string;
}

export class BackgroundSyncService {
  private static instance: BackgroundSyncService;
  private workerStatus: BackgroundWorkerStatus = { workerType: 'web-worker', status: 'idle' };
  private listeners: (() => void)[] = [];
  private autoReplyRules: AutoReplyRule[] = [];
  private autoResponderActive: boolean = true;
  private liveDiscoverActive: boolean = false;
  private instantJoinActive: boolean = false;
  private discoveredLinks: any[] = [];

  public static getInstance(): BackgroundSyncService {
    if (!BackgroundSyncService.instance) {
      BackgroundSyncService.instance = new BackgroundSyncService();
    }
    return BackgroundSyncService.instance;
  }

  public startSync(): void {
    this.workerStatus = { workerType: 'web-worker', status: 'running' };
    this.notify();
  }

  public stopSync(): void {
    this.workerStatus = { workerType: 'web-worker', status: 'idle' };
    this.notify();
  }

  public getWorkerStatus(): BackgroundWorkerStatus {
    return this.workerStatus;
  }

  public subscribe(fn: () => void): () => void {
    this.listeners.push(fn);
    return () => {
      this.listeners = this.listeners.filter((l) => l !== fn);
    };
  }

  private notify(): void {
    this.listeners.forEach((l) => l());
  }

  public processIncomingMessage(...args: any[]): void {}

  // Auto-Responder
  public getAutoReplyRules(): AutoReplyRule[] {
    return this.autoReplyRules;
  }

  public isAutoResponderActive(): boolean {
    return this.autoResponderActive;
  }

  public addAutoReplyRule(rule: Partial<AutoReplyRule> & { keyword: string; replyText: string }): void {
    const fullRule: AutoReplyRule = {
      id: 'rule_' + Date.now(),
      keyword: rule.keyword,
      replyText: rule.replyText,
      matchType: rule.matchType || 'contains',
      scope: rule.scope || 'all',
      isEnabled: rule.isEnabled !== undefined ? rule.isEnabled : true,
      timesTriggered: 0,
    };
    this.autoReplyRules.push(fullRule);
    this.notify();
  }

  public toggleGlobalAutoResponder(active?: boolean): void {
    this.autoResponderActive = active !== undefined ? active : !this.autoResponderActive;
    this.notify();
  }

  public toggleRule(ruleId: string): void {
    const rule = this.autoReplyRules.find((r) => r.id === ruleId);
    if (rule) {
      rule.isEnabled = !rule.isEnabled;
      this.notify();
    }
  }

  public deleteRule(ruleId: string): void {
    this.autoReplyRules = this.autoReplyRules.filter((r) => r.id !== ruleId);
    this.notify();
  }

  // Live Link Discover
  public getDiscoveredLinks(): any[] {
    return this.discoveredLinks;
  }

  public isInstantJoinEnabled(): boolean {
    return this.instantJoinActive;
  }

  public isLiveDiscoverActive(): boolean {
    return this.liveDiscoverActive;
  }

  public toggleLiveDiscover(active?: boolean): void {
    this.liveDiscoverActive = active !== undefined ? active : !this.liveDiscoverActive;
    this.notify();
  }

  public toggleInstantAutoJoin(active?: boolean): void {
    this.instantJoinActive = active !== undefined ? active : !this.instantJoinActive;
    this.notify();
  }

  public async manualJoinDiscoveredLink(linkId: string): Promise<boolean> {
    return true;
  }

  public clearDiscoveredLinks(): void {
    this.discoveredLinks = [];
    this.notify();
  }
}

export const backgroundSyncService = BackgroundSyncService.getInstance();
