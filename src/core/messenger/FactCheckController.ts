import { MessageFactCheck } from '../../types';

export const INITIAL_FACT_CHECKS: Record<string, MessageFactCheck> = {};

class FactCheckController {
  private factChecks: Record<string, MessageFactCheck> = { ...INITIAL_FACT_CHECKS };

  public getFactCheck(messageId: string): MessageFactCheck | null {
    return this.factChecks[messageId] || null;
  }

  public getAllFactChecks(): MessageFactCheck[] {
    return Object.values(this.factChecks);
  }

  public addOrUpdateFactCheck(
    messageId: string,
    chatId: string,
    organization: string,
    text: string,
    sourceUrl?: string
  ): MessageFactCheck {
    const record: MessageFactCheck = {
      messageId,
      chatId,
      country: 'Verified',
      organization,
      text,
      sourceUrl: sourceUrl || '',
      checkedAt: new Date().toISOString().split('T')[0],
      isExpanded: false,
    };
    this.factChecks[messageId] = record;
    return record;
  }

  public deleteFactCheck(messageId: string): boolean {
    if (this.factChecks[messageId]) {
      delete this.factChecks[messageId];
      return true;
    }
    return false;
  }
}

export const factCheckController = new FactCheckController();
