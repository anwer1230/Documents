/**
 * Telegram Web K App Managers Utilities (utils.ts)
 * Based on morethanwords/tweb src/lib/appManagers/utils.ts
 */

export type EventListenerCallback<T = any> = (payload: T) => void;

export class EventEmitter {
  private listeners: Map<string, Set<EventListenerCallback>> = new Map();

  public on<T = any>(event: string, callback: EventListenerCallback<T>): () => void {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, new Set());
    }
    this.listeners.get(event)!.add(callback);
    return () => this.off(event, callback);
  }

  public off(event: string, callback: EventListenerCallback): void {
    const set = this.listeners.get(event);
    if (set) {
      set.delete(callback);
      if (set.size === 0) this.listeners.delete(event);
    }
  }

  public emit<T = any>(event: string, payload: T): void {
    const set = this.listeners.get(event);
    if (set) {
      set.forEach((cb) => {
        try {
          cb(payload);
        } catch (err) {
          console.error(`[EventEmitter] Error in listener for ${event}:`, err);
        }
      });
    }
  }
}

/**
 * Peer type determination according to MTProto / tweb conventions
 */
export function getPeerType(peerId: string | number): 'user' | 'chat' | 'channel' {
  const str = String(peerId);
  if (str.startsWith('-100')) return 'channel';
  if (str.startsWith('-')) return 'chat';
  return 'user';
}

/**
 * Convert arbitrary peer ID to unified channel ID (-100...) or group ID
 */
export function normalizePeerId(peerId: string | number): string {
  return String(peerId);
}

/**
 * Debounce helper
 */
export function debounce<T extends (...args: any[]) => any>(fn: T, wait: number): (...args: Parameters<T>) => void {
  let timeout: any;
  return function (...args: Parameters<T>) {
    clearTimeout(timeout);
    timeout = setTimeout(() => fn(...args), wait);
  };
}

/**
 * Safe integer comparison
 */
export function compareDates(a: number, b: number): number {
  return a - b;
}
