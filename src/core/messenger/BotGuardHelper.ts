export interface BotGuardChallenge {
  queryId: string;
  botId: string;
  botName: string;
  botAvatar: string;
  actionName: string;
  isVerified: boolean;
  status: 'pending' | 'verified' | 'failed';
}

class BotGuardHelper {
  private activeChallenges: Record<string, BotGuardChallenge> = {
    guard_001: {
      queryId: 'guard_001',
      botId: 'bot_guard',
      botName: 'Telegram Shield Guard',
      botAvatar: 'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=100&h=100&fit=crop&crop=faces',
      actionName: 'Verify Human Check & Anti-Spam Gate',
      isVerified: true,
      status: 'verified',
    },
  };

  public verifyChallenge(queryId: string): boolean {
    if (this.activeChallenges[queryId]) {
      this.activeChallenges[queryId].status = 'verified';
      this.activeChallenges[queryId].isVerified = true;
      return true;
    }
    return false;
  }

  public getChallenge(queryId: string): BotGuardChallenge | null {
    return this.activeChallenges[queryId] || null;
  }
}

export const botGuardHelper = new BotGuardHelper();
