/**
 * Telegram Web K Core MTProto Orchestrator (mtproto.ts)
 * Based on morethanwords/tweb src/lib/mtproto/mtproto.ts
 */

import { Networker } from './networker';
import { appUpdatesManager } from '../appManagers/appUpdatesManager';
import { EventEmitter } from '../appManagers/utils';

export interface MTProtoConfig {
  defaultDcId: number;
  apiId?: number;
  apiHash?: string;
}

export class MTProtoCore extends EventEmitter {
  private networkers: Map<number, Networker> = new Map();
  private primaryDcId: number;

  constructor(config: MTProtoConfig = { defaultDcId: 2 }) {
    super();
    this.primaryDcId = config.defaultDcId;
  }

  public getNetworker(dcId: number = this.primaryDcId): Networker {
    if (!this.networkers.has(dcId)) {
      const net = new Networker({ dcId });

      net.on('update', (update: any) => {
        appUpdatesManager.handleUpdates(update);
        this.emit('update', update);
      });

      net.on('status', (status: string) => {
        this.emit('status', { dcId, status });
      });

      this.networkers.set(dcId, net);
    }
    return this.networkers.get(dcId)!;
  }

  public async start(): Promise<void> {
    const net = this.getNetworker(this.primaryDcId);
    await net.connect();
  }

  public stop(): void {
    this.networkers.forEach((net) => net.disconnect());
  }
}

export const mtprotoCore = new MTProtoCore();
export default mtprotoCore;
