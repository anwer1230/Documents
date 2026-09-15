/**
 * Telegram Web K Debug Configuration (debug.ts)
 * Based on morethanwords/tweb src/config/debug.ts
 */

export const DEBUG_CONFIG = {
  enabled: typeof window !== 'undefined' && (window.location.search.includes('debug=1') || localStorage.getItem('tweb_debug') === 'true'),
  logUpdates: false,
  logNetwork: false,
  logPerformance: false,
};

export class DebugLogger {
  public static log(prefix: string, ...args: any[]): void {
    if (DEBUG_CONFIG.enabled) {
      console.log(`%c[tweb:${prefix}]`, 'color: #2481cc; font-weight: bold;', ...args);
    }
  }

  public static warn(prefix: string, ...args: any[]): void {
    console.warn(`[tweb:${prefix}]`, ...args);
  }

  public static error(prefix: string, ...args: any[]): void {
    console.error(`[tweb:${prefix}]`, ...args);
  }

  public static time(label: string): void {
    if (DEBUG_CONFIG.enabled && DEBUG_CONFIG.logPerformance) {
      console.time(`[tweb:perf] ${label}`);
    }
  }

  public static timeEnd(label: string): void {
    if (DEBUG_CONFIG.enabled && DEBUG_CONFIG.logPerformance) {
      console.timeEnd(`[tweb:perf] ${label}`);
    }
  }
}

export default DEBUG_CONFIG;
