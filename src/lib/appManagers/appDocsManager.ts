/**
 * Telegram Web K Documents Manager (appDocsManager.ts)
 * Based on morethanwords/tweb src/lib/appManagers/appDocsManager.ts
 */

export interface DocumentInfo {
  id: string;
  fileName: string;
  fileSize: number;
  mimeType: string;
  url?: string;
  duration?: number;
  isVoice?: boolean;
  isAudio?: boolean;
}

export class AppDocsManager {
  public formatFileSize(bytes: number): string {
    if (!bytes || bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }

  public getFileExtension(fileName: string): string {
    return fileName.slice(((fileName.lastIndexOf('.') - 1) >>> 0) + 2).toLowerCase();
  }

  public isVoiceMessage(mimeType: string): boolean {
    return mimeType.includes('audio/ogg') || mimeType.includes('opus');
  }

  public isAudio(mimeType: string): boolean {
    return mimeType.startsWith('audio/');
  }

  public isVideo(mimeType: string): boolean {
    return mimeType.startsWith('video/');
  }
}

export const appDocsManager = new AppDocsManager();
export default appDocsManager;
