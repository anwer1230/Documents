/**
 * Telegram Official MyMessagesBackend (Batch Manager for Mass Edit & Revoke)
 * Replicates MyMessagesActivity, editMessage, and deleteMessages
 */

import { ConnectionsManager } from '../tgnet/ConnectionsManager';
import { NotificationCenter } from './NotificationCenter';
import { MessagesStorage } from './MessagesStorage';

export interface BatchOperationResult {
  total: number;
  succeeded: number;
  failed: number;
}

export class MyMessagesBackend {
  private static instance: MyMessagesBackend;

  public static getInstance(): MyMessagesBackend {
    if (!MyMessagesBackend.instance) {
      MyMessagesBackend.instance = new MyMessagesBackend();
    }
    return MyMessagesBackend.instance;
  }

  /**
   * Batch edits a set of sent messages across chats
   */
  public async editBatch(
    items: Array<{ messageId: number | string; chatId: number | string }>,
    newText: string,
    onProgress?: (done: number, total: number) => void
  ): Promise<BatchOperationResult> {
    let succeeded = 0;
    let failed = 0;

    for (let i = 0; i < items.length; i++) {
      const item = items[i];
      try {
        const resp = await ConnectionsManager.getInstance().sendRequest('editMessage', {
          peer: item.chatId,
          id: item.messageId,
          message: newText,
        });

        if (resp && resp.success) {
          succeeded++;
        } else {
          failed++;
        }
      } catch {
        failed++;
      }

      if (onProgress) {
        onProgress(i + 1, items.length);
      }
      await new Promise((r) => setTimeout(r, 400));
    }

    NotificationCenter.getInstance().postNotificationName(NotificationCenter.updateInterfaces);
    return { total: items.length, succeeded, failed };
  }

  /**
   * Batch deletes messages permanently for everyone (revoke = true)
   */
  public async deleteBatch(
    items: Array<{ messageId: number | string; chatId: number | string }>,
    revokeForAll: boolean = true,
    onProgress?: (done: number, total: number) => void
  ): Promise<BatchOperationResult> {
    let succeeded = 0;
    let failed = 0;

    // Group by chatId for optimal multi-id payload
    const grouped = new Map<string | number, Array<number | string>>();
    for (const item of items) {
      const list = grouped.get(item.chatId) || [];
      list.push(item.messageId);
      grouped.set(item.chatId, list);
    }

    let processed = 0;
    for (const [chatId, msgIds] of grouped.entries()) {
      try {
        const resp = await ConnectionsManager.getInstance().sendRequest('deleteMessages', {
          peer: chatId,
          ids: msgIds,
          revoke: revokeForAll,
        });

        if (resp && resp.success) {
          succeeded += msgIds.length;
          await MessagesStorage.getInstance().markMessagesAsDeleted(chatId, msgIds);
        } else {
          failed += msgIds.length;
        }
      } catch {
        failed += msgIds.length;
      }

      processed += msgIds.length;
      if (onProgress) {
        onProgress(processed, items.length);
      }
      await new Promise((r) => setTimeout(r, 300));
    }

    NotificationCenter.getInstance().postNotificationName(NotificationCenter.dialogsNeedReload);
    return { total: items.length, succeeded, failed };
  }
}
