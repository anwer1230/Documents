import { GiftAuctionItem, StarGiftItem } from '../../types';

export const INITIAL_STAR_GIFTS: StarGiftItem[] = [
  { id: 'gift_star_cake', title: 'Delicious Cake', emoji: '🎂', starsPrice: 15, soldCount: 4200, badge: 'Popular' },
  { id: 'gift_star_bear', title: 'Teddy Bear', emoji: '🧸', starsPrice: 25, soldCount: 3100, badge: 'Cute' },
  { id: 'gift_star_diamond', title: 'Sparkling Gem', emoji: '💎', starsPrice: 50, isLimited: true, totalAvailable: 10000, soldCount: 8950, badge: 'Limited' },
  { id: 'gift_star_rocket', title: 'Space Rocket', emoji: '🚀', starsPrice: 100, isLimited: true, totalAvailable: 5000, soldCount: 4920, badge: 'Rare' },
  { id: 'gift_star_crown', title: 'Golden Crown', emoji: '👑', starsPrice: 250, isLimited: true, totalAvailable: 1000, soldCount: 978, badge: 'Legendary' },
  { id: 'gift_star_dragon', title: 'Mythic Dragon', emoji: '🐉', starsPrice: 500, isLimited: true, totalAvailable: 500, soldCount: 495, badge: 'Mythic' },
];

export const INITIAL_GIFT_AUCTIONS: GiftAuctionItem[] = [
  {
    id: 'auction_gift_001',
    giftId: 'gift_star_crown',
    title: 'Crown of Sovereign #007',
    symbol: '👑#7',
    currentBidStars: 1450,
    highestBidderId: 'user_durov',
    highestBidderName: 'Pavel Durov',
    highestBidderAvatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100&h=100&fit=crop&crop=faces',
    minNextBid: 1550,
    endsAt: Date.now() + 1000 * 60 * 60 * 18, // 18 hours
    totalBidsCount: 24,
    recentBids: [
      {
        bidId: 'bid_1',
        userId: 'user_durov',
        userName: 'Pavel Durov',
        userAvatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100&h=100&fit=crop&crop=faces',
        amountStars: 1450,
        timestamp: '10 mins ago',
      },
      {
        bidId: 'bid_2',
        userId: 'user_alex',
        userName: 'Alex Crypto',
        userAvatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100&h=100&fit=crop&crop=faces',
        amountStars: 1300,
        timestamp: '35 mins ago',
      },
    ],
    attributes: [
      { key: 'Model', value: '3D Gold Hologram', rarityPercentage: 1.2 },
      { key: 'Backdrop', value: 'Cosmic Nebula', rarityPercentage: 4.5 },
      { key: 'Mint Number', value: '#007', rarityPercentage: 0.1 },
    ],
  },
  {
    id: 'auction_gift_002',
    giftId: 'gift_star_dragon',
    title: 'Emerald Dragon #042',
    symbol: '🐉#42',
    currentBidStars: 3200,
    highestBidderId: 'user_sara',
    highestBidderName: 'Sara Connor',
    highestBidderAvatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=100&h=100&fit=crop&crop=faces',
    minNextBid: 3350,
    endsAt: Date.now() + 1000 * 60 * 60 * 36, // 36 hours
    totalBidsCount: 48,
    recentBids: [
      {
        bidId: 'bid_d1',
        userId: 'user_sara',
        userName: 'Sara Connor',
        userAvatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=100&h=100&fit=crop&crop=faces',
        amountStars: 3200,
        timestamp: '5 mins ago',
      },
    ],
    attributes: [
      { key: 'Element', value: 'Emerald Fire', rarityPercentage: 2.1 },
      { key: 'Rarity', value: 'Ancient', rarityPercentage: 0.8 },
    ],
  },
];

export class GiftAuctionController {
  private static instances = new Map<number, GiftAuctionController>();
  private currentAccount: number = 0;
  private auctions: GiftAuctionItem[] = [...INITIAL_GIFT_AUCTIONS];
  private starGifts: StarGiftItem[] = [...INITIAL_STAR_GIFTS];

  public static getInstance(accountNum: number = 0): GiftAuctionController {
    if (!GiftAuctionController.instances.has(accountNum)) {
      const inst = new GiftAuctionController();
      inst.currentAccount = accountNum;
      GiftAuctionController.instances.set(accountNum, inst);
    }
    return GiftAuctionController.instances.get(accountNum)!;
  }

  public getAuctions(): GiftAuctionItem[] {
    return this.auctions;
  }

  public getStarGifts(): StarGiftItem[] {
    return this.starGifts;
  }

  public placeBid(
    auctionId: string,
    userId: string,
    userName: string,
    userAvatar: string,
    amountStars: number
  ): { success: boolean; error?: string; updatedAuction?: GiftAuctionItem } {
    const auction = this.auctions.find((a) => a.id === auctionId);
    if (!auction) {
      return { success: false, error: 'Auction not found' };
    }
    if (amountStars < auction.minNextBid) {
      return { success: false, error: `Minimum bid is ${auction.minNextBid} Stars` };
    }

    auction.currentBidStars = amountStars;
    auction.highestBidderId = userId;
    auction.highestBidderName = userName;
    auction.highestBidderAvatar = userAvatar;
    auction.minNextBid = Math.ceil(amountStars * 1.05);
    auction.totalBidsCount += 1;
    auction.recentBids.unshift({
      bidId: `bid_${Date.now()}`,
      userId,
      userName,
      userAvatar,
      amountStars,
      timestamp: 'Just now',
    });

    return { success: true, updatedAuction: { ...auction } };
  }
}

export const giftAuctionController = new GiftAuctionController();
