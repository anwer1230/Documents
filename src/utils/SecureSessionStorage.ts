/**
 * SecureSessionStorage.ts - Telegram Secure Session Key-Value Storage
 */

export class SecureSessionStorage {
  public static getItem<T = any>(key: string): T | null {
    try {
      if (typeof window === 'undefined' || !window.localStorage) return null;
      const raw = localStorage.getItem(key);
      if (!raw) return null;
      try {
        return JSON.parse(raw);
      } catch {
        return raw as unknown as T;
      }
    } catch {
      return null;
    }
  }

  public static setItem(key: string, value: any): void {
    try {
      if (typeof window === 'undefined' || !window.localStorage) return;
      const strVal = typeof value === 'string' ? value : JSON.stringify(value);
      localStorage.setItem(key, strVal);
    } catch (_) {}
  }

  public static removeItem(key: string): void {
    try {
      if (typeof window === 'undefined' || !window.localStorage) return;
      localStorage.removeItem(key);
    } catch (_) {}
  }

  public static clear(): void {
    try {
      if (typeof window === 'undefined' || !window.localStorage) return;
      localStorage.clear();
    } catch (_) {}
  }

  /**
   * Complete purge of all GramJS sessions, authentication keys, and MTProto caches
   */
  public static purgeAllSessions(reason: string = 'AUTH_KEY_UNREGISTERED'): void {
    try {
      console.warn(`[SecureSessionStorage] Purging all persistent GramJS sessions (reason: ${reason})`);
      if (typeof window === 'undefined') return;

      const keysToPurge = [
        'tg_session_string',
        'tg_session_0',
        'tg_session_1',
        'tg_session_2',
        'tg_session_string_0',
        'tg_session_string_1',
        'tg_session_string_2',
        'telegram_session',
        'tg_auth_user',
        'tg_user_profile',
        'tg_auth_session_active',
        'tg_multi_accounts_v3',
        'tg_active_account_id_v3',
        'tg_accounts',
        'tg_active_account_id',
        'tg_user',
        'tg_phone',
        'tg_current_user',
        'tg_user_config_0',
        'tg_user_config_1',
        'tg_user_config_2',
        'tg_mtproto_session_0',
        'tg_mtproto_session_1',
        'tg_mtproto_session_2',
        'tg_auth_token',
        'tg_future_token_0',
      ];

      for (const k of keysToPurge) {
        try {
          localStorage.removeItem(k);
        } catch (_) {}
      }

      // Mark explicitly logged out so initial state doesn't auto-revive stale mock data
      localStorage.setItem('tg_explicitly_logged_out', 'true');
      sessionStorage.clear();
    } catch (e) {
      console.warn('[SecureSessionStorage] Session purge notice:', e);
    }
  }

  public static async validateSessionWithServer(options: {
    sessionString: string;
    phone: string;
    accountId?: string;
  }): Promise<{ valid: boolean; revoked?: boolean; user?: any; reason?: string; isOffline?: boolean }> {
    if (!options.sessionString && !options.phone) {
      return { valid: false, revoked: true, reason: 'NO_CREDENTIALS' };
    }
    try {
      const res = await fetch('/api/telegram/session/validate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(options),
      });

      const data = await res.json().catch(() => null);

      if (res.status === 401 || data?.revoked || data?.reason === 'AUTH_KEY_UNREGISTERED') {
        return {
          valid: false,
          revoked: true,
          reason: data?.reason || 'AUTH_KEY_UNREGISTERED',
        };
      }

      if (res.ok && data?.valid) {
        return {
          valid: true,
          revoked: false,
          user: data.user,
        };
      }

      if (res.status === 503 || data?.isOffline) {
        return { valid: false, isOffline: true, reason: 'SERVICE_UNAVAILABLE' };
      }

      return {
        valid: Boolean(data?.valid),
        revoked: Boolean(data?.revoked),
        user: data?.user,
        reason: data?.reason,
      };
    } catch {
      // Offline fallback: don't falsely claim validated, signal offline
      return { valid: false, isOffline: true, reason: 'OFFLINE' };
    }
  }

  public static async restoreFromIndexedDBBackup(keys: string[]): Promise<Record<string, any>> {
    const result: Record<string, any> = {};
    for (const key of keys) {
      const val = SecureSessionStorage.getItem(key);
      if (val !== null) {
        result[key] = val;
      }
    }
    return result;
  }
}
