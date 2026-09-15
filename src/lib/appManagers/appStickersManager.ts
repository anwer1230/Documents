/**
 * Telegram Web K Stickers Manager (appStickersManager.ts)
 * Based on morethanwords/tweb src/lib/appManagers/appStickersManager.ts
 */

export interface StickerItem {
  id: string;
  emoji: string;
  url: string;
  isAnimated?: boolean;
  isVideo?: boolean;
}

export interface StickerSet {
  id: string;
  title: string;
  shortName: string;
  count: number;
  stickers: StickerItem[];
}

export class AppStickersManager {
  private sets: Map<string, StickerSet> = new Map();

  public getStickerSet(setId: string): StickerSet | null {
    return this.sets.get(setId) || null;
  }

  public saveStickerSet(set: StickerSet): void {
    this.sets.set(set.id, set);
  }

  public getAllSets(): StickerSet[] {
    return Array.from(this.sets.values());
  }
}

export const appStickersManager = new AppStickersManager();
export default appStickersManager;
