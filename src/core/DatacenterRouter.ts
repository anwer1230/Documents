/**
 * DatacenterRouter.ts - Multi-Datacenter Routing & Failover Architecture
 * Telegram uses 5 production Datacenters (DCs) worldwide.
 * - DC1: Miami, USA (Production)
 * - DC2: Amsterdam, Europe (Default Primary)
 * - DC3: Miami, USA (Backup & Auth)
 * - DC4: Amsterdam, Europe (Media & CDN)
 * - DC5: Singapore, Asia (Asia-Pacific Primary)
 * 
 * Features:
 * - Automatic migration on PHONE_MIGRATE_X, FILE_MIGRATE_X, USER_MIGRATE_X
 * - Ping & latency-based nearest DC resolution
 * - Dedicated Media DC routing (DC4) for media downloads
 */

export interface TelegramDatacenter {
  id: number;
  name: string;
  ip: string;
  port: number;
  location: string;
  isMediaDefault?: boolean;
}

export const OFFICIAL_TELEGRAM_DCS: Record<number, TelegramDatacenter> = {
  1: { id: 1, name: 'DC1 (Miami)', ip: '149.154.175.50', port: 443, location: 'Miami, FL, USA' },
  2: { id: 2, name: 'DC2 (Amsterdam)', ip: '149.154.167.50', port: 443, location: 'Amsterdam, NL' },
  3: { id: 3, name: 'DC3 (Miami 2)', ip: '149.154.175.100', port: 443, location: 'Miami, FL, USA' },
  4: { id: 4, name: 'DC4 (Amsterdam Media)', ip: '149.154.167.91', port: 443, location: 'Amsterdam, NL', isMediaDefault: true },
  5: { id: 5, name: 'DC5 (Singapore)', ip: '91.108.56.165', port: 443, location: 'Singapore, SG' },
};

export class DatacenterRouter {
  private static instance: DatacenterRouter;
  private currentDcId: number = 2; // Default to DC2 Amsterdam
  private mediaDcId: number = 4;   // Default media CDN to DC4
  private dcLatencies: Map<number, number> = new Map();

  private constructor() {}

  public static getInstance(): DatacenterRouter {
    if (!DatacenterRouter.instance) {
      DatacenterRouter.instance = new DatacenterRouter();
    }
    return DatacenterRouter.instance;
  }

  public getCurrentDc(): TelegramDatacenter {
    return OFFICIAL_TELEGRAM_DCS[this.currentDcId] || OFFICIAL_TELEGRAM_DCS[2];
  }

  public getMediaDc(): TelegramDatacenter {
    return OFFICIAL_TELEGRAM_DCS[this.mediaDcId] || OFFICIAL_TELEGRAM_DCS[4];
  }

  public setDc(dcId: number): void {
    if (OFFICIAL_TELEGRAM_DCS[dcId]) {
      this.currentDcId = dcId;
    }
  }

  /**
   * Handle MTProto DC Migration errors
   * Error formats: "PHONE_MIGRATE_X", "FILE_MIGRATE_X", "USER_MIGRATE_X", "NETWORK_MIGRATE_X"
   */
  public handleMigrationError(errorMessage: string): number | null {
    const match = errorMessage.match(/(?:PHONE|FILE|USER|NETWORK)_MIGRATE_(\d+)/);
    if (match && match[1]) {
      const targetDc = parseInt(match[1], 10);
      if (OFFICIAL_TELEGRAM_DCS[targetDc]) {
        if (errorMessage.includes('FILE_MIGRATE')) {
          this.mediaDcId = targetDc;
        } else {
          this.currentDcId = targetDc;
        }
        return targetDc;
      }
    }
    return null;
  }

  /**
   * Measure latency across Telegram datacenters for optimal routing
   */
  public async measureNearestDc(): Promise<number> {
    const testPromises = Object.values(OFFICIAL_TELEGRAM_DCS).map(async (dc) => {
      const start = performance.now();
      try {
        // Fast probe via fetch or WebSocket ping
        const url = `https://${dc.ip}:${dc.port}/api`;
        await fetch(url, { method: 'HEAD', mode: 'no-cors' }).catch(() => {});
      } catch (_) {}
      const duration = performance.now() - start;
      this.dcLatencies.set(dc.id, duration);
      return { dcId: dc.id, latency: duration };
    });

    try {
      const results = await Promise.race([
        Promise.all(testPromises),
        new Promise<any[]>(res => setTimeout(() => res([]), 2500))
      ]);
      if (results.length > 0) {
        results.sort((a, b) => a.latency - b.latency);
        this.currentDcId = results[0].dcId;
      }
    } catch (_) {}

    return this.currentDcId;
  }
}

export const datacenterRouter = DatacenterRouter.getInstance();
