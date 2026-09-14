/**
 * Telegram Web K Configuration State (state.ts)
 * Based on morethanwords/tweb src/config/state.ts
 */

export interface DCConfig {
  id: number;
  ip: string;
  port: number;
  isTest?: boolean;
}

export const TELEGRAM_CONFIG_STATE = {
  version: '2.0.0-k',
  build: 'official-tweb-k',
  dcs: [
    { id: 1, ip: '149.154.175.50', port: 443 },
    { id: 2, ip: '149.154.167.50', port: 443 },
    { id: 3, ip: '149.154.175.100', port: 443 },
    { id: 4, ip: '149.154.167.91', port: 443 },
    { id: 5, ip: '91.108.56.165', port: 443 },
  ] as DCConfig[],
  defaultDcId: 2,
  webSocketTimeout: 20000,
  networkMaxRetries: 8,
  minPtsBatchSize: 20,
  maxPtsBatchSize: 100,
};

export default TELEGRAM_CONFIG_STATE;
