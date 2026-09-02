import { MessageFactCheck } from '../../types';

export const INITIAL_FACT_CHECKS: Record<string, MessageFactCheck> = {
  msg_telegram_update: {
    messageId: 'msg_telegram_update',
    chatId: 'chat_telegram_news',
    country: 'International',
    organization: 'Telegram Official Verification',
    organizationLogo: 'https://telegram.org/img/t_logo.png',
    text: 'This announcement has been verified as authentic by Telegram Product Operations.',
    sourceUrl: 'https://telegram.org/blog',
    checkedAt: '2026-08-30',
    isExpanded: false,
  },
  msg_crypto_alert: {
    messageId: 'msg_crypto_alert',
    chatId: 'chat_crypto',
    country: 'Global',
    organization: 'Community Fact-Check (CertiK)',
    organizationLogo: 'https://cryptologos.cc/logos/toncoin-ton-logo.png',
    text: 'Warning: Third-party bot claims of guaranteed 200% returns have been identified as deceptive schemes.',
    sourceUrl: 'https://community.telegram.org/factcheck/7841',
    checkedAt: '2026-08-28',
    isExpanded: false,
  },
};

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
      sourceUrl,
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
