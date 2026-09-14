/**
 * Telegram Web K Server Time Manager (serverTimeManager.ts)
 * Based on morethanwords/tweb src/lib/mtproto/serverTimeManager.ts
 */

export class ServerTimeManager {
  private timeOffset = 0; // serverTime - localTime (in seconds)

  public setServerTime(serverTimeSeconds: number): void {
    const localSeconds = Math.floor(Date.now() / 1000);
    this.timeOffset = serverTimeSeconds - localSeconds;
  }

  public getServerTimeSeconds(): number {
    return Math.floor(Date.now() / 1000) + this.timeOffset;
  }

  public getServerTimeMs(): number {
    return Date.now() + this.timeOffset * 1000;
  }

  public getTimeOffset(): number {
    return this.timeOffset;
  }
}

export const serverTimeManager = new ServerTimeManager();
export default serverTimeManager;
