import { ChannelBoostData } from '../../types';

export class ChannelBoostsController {
  private static instances = new Map<number, ChannelBoostsController>();
  private currentAccount: number = 0;
  private boostStore: Record<string, ChannelBoostData> = {};

  public static getInstance(accountNum: number = 0): ChannelBoostsController {
    if (!ChannelBoostsController.instances.has(accountNum)) {
      const inst = new ChannelBoostsController();
      inst.currentAccount = accountNum;
      ChannelBoostsController.instances.set(accountNum, inst);
    }
    return ChannelBoostsController.instances.get(accountNum)!;
  }

  constructor() {
    // Dynamic boost data initialized on demand
  }

  public getChannelBoost(chatId: string): ChannelBoostData {
    if (!this.boostStore[chatId]) {
      this.boostStore[chatId] = {
        chatId,
        currentLevel: 1,
        currentBoosts: 3,
        boostsToNextLevel: 7,
        myBoostsCount: 0,
        canBoost: true,
        boostUrl: `https://t.me/boost/${chatId}`,
        unlockedPerks: [
          { level: 1, title: 'Stories for Channels', titleAr: 'نشر القصص للقناة', description: 'Publish 1 story per day', isUnlocked: true },
          { level: 2, title: 'Custom Channel Status', titleAr: 'رمز حالة مخصص للقناة', description: 'Emoji status in title', isUnlocked: false },
          { level: 3, title: 'Custom Wallpaper', titleAr: 'خلفية مخصصة للمحادثة', description: 'Channel wallpaper', isUnlocked: false },
        ],
      };
    }
    return this.boostStore[chatId];
  }

  public boostChannel(chatId: string): { success: boolean; boostData: ChannelBoostData } {
    const data = this.getChannelBoost(chatId);
    data.currentBoosts += 1;
    data.myBoostsCount += 1;
    if (data.boostsToNextLevel > 1) {
      data.boostsToNextLevel -= 1;
    } else {
      data.currentLevel += 1;
      data.boostsToNextLevel = 10;
      data.unlockedPerks.forEach((p) => {
        if (p.level <= data.currentLevel) p.isUnlocked = true;
      });
    }
    return { success: true, boostData: { ...data } };
  }
}

export const channelBoostsController = new ChannelBoostsController();
