import { ChatCacheUsageInfo } from '../../types';

export const INITIAL_CHAT_CACHE_USAGE: ChatCacheUsageInfo[] = [
  {
    chatId: 'chat_telegram_news',
    chatTitle: 'Telegram News & Releases',
    chatAvatar: 'https://images.unsplash.com/photo-1614680376593-902f749f7ffc?w=100&h=100&fit=crop&crop=faces',
    photosBytes: 14.8 * 1024 * 1024,
    videosBytes: 48.2 * 1024 * 1024,
    audioBytes: 3.1 * 1024 * 1024,
    documentsBytes: 18.5 * 1024 * 1024,
    totalBytes: 84.6 * 1024 * 1024,
    keepMediaMode: '1_month',
  },
  {
    chatId: 'chat_durov',
    chatTitle: 'Pavel Durov',
    chatAvatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100&h=100&fit=crop&crop=faces',
    photosBytes: 5.2 * 1024 * 1024,
    videosBytes: 12.0 * 1024 * 1024,
    audioBytes: 8.4 * 1024 * 1024,
    documentsBytes: 2.1 * 1024 * 1024,
    totalBytes: 27.7 * 1024 * 1024,
    keepMediaMode: 'forever',
  },
  {
    chatId: 'chat_crypto',
    chatTitle: 'Crypto Alpha Traders',
    chatAvatar: 'https://images.unsplash.com/photo-1622979135225-d2ba269bc1df?w=100&h=100&fit=crop&crop=faces',
    photosBytes: 22.4 * 1024 * 1024,
    videosBytes: 74.6 * 1024 * 1024,
    audioBytes: 1.2 * 1024 * 1024,
    documentsBytes: 35.8 * 1024 * 1024,
    totalBytes: 134.0 * 1024 * 1024,
    keepMediaMode: '1_week',
  },
];

class CacheByChatsController {
  private cacheStore: ChatCacheUsageInfo[] = [...INITIAL_CHAT_CACHE_USAGE];

  public getCacheUsageList(): ChatCacheUsageInfo[] {
    return this.cacheStore;
  }

  public getChatCache(chatId: string): ChatCacheUsageInfo | null {
    return this.cacheStore.find((c) => c.chatId === chatId) || null;
  }

  public clearChatMediaType(chatId: string, mediaType: 'all' | 'photos' | 'videos' | 'audio' | 'documents'): number {
    const item = this.cacheStore.find((c) => c.chatId === chatId);
    if (!item) return 0;

    let clearedBytes = 0;
    if (mediaType === 'all') {
      clearedBytes = item.totalBytes;
      item.photosBytes = 0;
      item.videosBytes = 0;
      item.audioBytes = 0;
      item.documentsBytes = 0;
      item.totalBytes = 0;
    } else if (mediaType === 'photos') {
      clearedBytes = item.photosBytes;
      item.photosBytes = 0;
      item.totalBytes -= clearedBytes;
    } else if (mediaType === 'videos') {
      clearedBytes = item.videosBytes;
      item.videosBytes = 0;
      item.totalBytes -= clearedBytes;
    } else if (mediaType === 'audio') {
      clearedBytes = item.audioBytes;
      item.audioBytes = 0;
      item.totalBytes -= clearedBytes;
    } else if (mediaType === 'documents') {
      clearedBytes = item.documentsBytes;
      item.documentsBytes = 0;
      item.totalBytes -= clearedBytes;
    }

    return clearedBytes;
  }

  public updateKeepMedia(chatId: string, mode: ChatCacheUsageInfo['keepMediaMode']): void {
    const item = this.cacheStore.find((c) => c.chatId === chatId);
    if (item) {
      item.keepMediaMode = mode;
    }
  }

  public getTotalCacheSize(): number {
    return this.cacheStore.reduce((acc, curr) => acc + curr.totalBytes, 0);
  }
}

export const cacheByChatsController = new CacheByChatsController();
