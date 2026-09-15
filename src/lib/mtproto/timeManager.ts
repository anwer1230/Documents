/**
 * Telegram Web K Time & Message ID Manager (timeManager.ts)
 * Based on morethanwords/tweb src/lib/mtproto/timeManager.ts
 */

import { serverTimeManager } from './serverTimeManager';

export class TimeManager {
  private lastMsgId = BigInt(0);

  /**
   * Generates a unique, strictly increasing 64-bit MTProto message ID.
   * Lower 32 bits contain fractional seconds, ensuring (msgId % 4 === 0) for client messages.
   */
  public generateMessageId(): string {
    const serverMs = serverTimeManager.getServerTimeMs();
    const unixSeconds = BigInt(Math.floor(serverMs / 1000));
    const msFraction = BigInt(serverMs % 1000);

    // Approximate fractional nanoseconds in lower 32 bits, aligned to 4
    let msgId = (unixSeconds << BigInt(32)) | ((msFraction * BigInt(4294967)) & BigInt(~3));

    if (msgId <= this.lastMsgId) {
      msgId = this.lastMsgId + BigInt(4);
    }
    this.lastMsgId = msgId;

    return msgId.toString();
  }

  public reset(): void {
    this.lastMsgId = BigInt(0);
  }
}

export const timeManager = new TimeManager();
export default timeManager;
