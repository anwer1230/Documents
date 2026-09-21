export type TelemetryEventType =
  | 'network_online'
  | 'network_offline'
  | 'latency_ping'
  | 'sync_start'
  | 'sync_success'
  | 'sync_error'
  | 'connection_state';

export type TelemetryCategory = 'network' | 'latency' | 'sync';

export interface TelemetryEvent {
  id: string;
  timestamp: string; // ISO 8601
  type: TelemetryEventType;
  category: TelemetryCategory;
  reason?: string;
  durationMs?: number;
  serverDurationMs?: number;
  details?: Record<string, any>;
}

export const MAX_TELEMETRY_LOGS = 50;
