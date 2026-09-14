/**
 * Telegram Web K Photos Manager (appPhotosManager.ts)
 * Based on morethanwords/tweb src/lib/appManagers/appPhotosManager.ts
 */

export interface PhotoDimensions {
  width: number;
  height: number;
}

export class AppPhotosManager {
  private urlCache: Map<string, string> = new Map();

  public calculateDimensions(
    originalWidth: number,
    originalHeight: number,
    maxWidth = 400,
    maxHeight = 400
  ): PhotoDimensions {
    if (!originalWidth || !originalHeight) return { width: maxWidth, height: maxHeight };

    let width = originalWidth;
    let height = originalHeight;

    if (width > maxWidth) {
      height = Math.round((height * maxWidth) / width);
      width = maxWidth;
    }

    if (height > maxHeight) {
      width = Math.round((width * maxHeight) / height);
      height = maxHeight;
    }

    return { width: Math.max(100, width), height: Math.max(100, height) };
  }

  public getCachedPhotoUrl(fileId: string): string | null {
    return this.urlCache.get(fileId) || null;
  }

  public cachePhotoUrl(fileId: string, url: string): void {
    this.urlCache.set(fileId, url);
  }
}

export const appPhotosManager = new AppPhotosManager();
export default appPhotosManager;
