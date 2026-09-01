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
    // Default boost data for channels
    this.boostStore['chat_telegram_news'] = {
      chatId: 'chat_telegram_news',
      currentLevel: 7,
      currentBoosts: 84,
      boostsToNextLevel: 16,
      myBoostsCount: 2,
      canBoost: true,
      boostUrl: 'https://t.me/boost/telegram_news',
      unlockedPerks: [
        { level: 1, title: 'Stories for Channels', titleAr: 'نشر القصص للقناة', description: 'Publish up to 1 story per day', isUnlocked: true },
        { level: 2, title: 'Custom Channel Status', titleAr: 'رمز حالة مخصص للقناة', description: 'Set an emoji status next to channel title', isUnlocked: true },
        { level: 3, title: 'Custom Wallpaper', titleAr: 'خلفية مخصصة للمحادثة', description: 'Set a custom chat wallpaper for all subscribers', isUnlocked: true },
        { level: 4, title: 'Custom Link Style', titleAr: 'نمط روابط مخصص', description: 'Style channel links with unique colors', isUnlocked: true },
        { level: 5, title: 'Custom Emoji Reactions', titleAr: 'تفاعلات مخصصة حصرية', description: 'Enable custom animated emoji reactions', isUnlocked: true },
        { level: 6, title: 'Voice-to-Text Transcription', titleAr: 'تحويل الصوت إلى نص للمشتركين', description: 'Automatic transcription for voice messages', isUnlocked: true },
        { level: 7, title: 'Cover Color & Profile Badge', titleAr: 'لون الغلاف وشارة الملف الشخصي', description: 'Custom cover gradient banner', isUnlocked: true },
        { level: 8, title: 'Extended Story Limit (8/day)', titleAr: 'رفع حد القصص إلى 8 يومياً', description: 'Publish up to 8 stories per day', isUnlocked: false },
        { level: 9, title: 'Custom Emoji Pack', titleAr: 'حزمة رموز تعبيرية باسم القناة', description: 'Subscribers can use channel pack anywhere', isUnlocked: false },
        { level: 10, title: 'Maximum Perks & Star Boost', titleAr: 'أقصى مميزات التعزيز ونجوم مجانية', description: 'All perks unlocked', isUnlocked: false },
      ],
    };
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
