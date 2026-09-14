/**
 * Backend API Client & MTProto Bridge Service Layer
 * 
 * Provides typed client methods for interacting with Telegram MTProto 2.0,
 * RPC invocations, SQLite auto-replies/automation, multi-tier cache,
 * and background synchronizations while preserving full TypeScript safety.
 */

import { csrfFetch } from './csrfFetch.js';

export interface ApiResponse<T = any> {
  success: boolean;
  data?: T;
  error?: string;
  message?: string;
  [key: string]: any;
}

export interface RequestOptions extends RequestInit {
  timeout?: number;
  params?: Record<string, string | number | boolean | undefined>;
}

export class ApiError extends Error {
  status: number;
  data: any;

  constructor(message: string, status: number = 500, data: any = null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.data = data;
  }
}

export async function request<T = any>(endpoint: string, options: RequestOptions = {}): Promise<T> {
  const { timeout = 30000, params, headers = {}, ...fetchOptions } = options;

  let url = endpoint;
  if (params) {
    const searchParams = new URLSearchParams();
    Object.entries(params).forEach(([key, val]) => {
      if (val !== undefined && val !== null) {
        searchParams.append(key, String(val));
      }
    });
    const qs = searchParams.toString();
    if (qs) {
      url += (url.includes('?') ? '&' : '?') + qs;
    }
  }

  const controller = new AbortController();
  const id = setTimeout(() => controller.abort(), timeout);

  try {
    const res = await csrfFetch(url, {
      ...fetchOptions,
      headers: {
        'Content-Type': 'application/json',
        ...headers,
      },
      signal: controller.signal,
    });

    clearTimeout(id);

    const contentType = res.headers.get('content-type') || '';
    let parsed: any;
    if (contentType.includes('application/json')) {
      parsed = await res.json();
    } else {
      parsed = await res.text();
    }

    if (!res.ok) {
      const errMsg = parsed?.error || parsed?.message || `HTTP ${res.status}: ${res.statusText}`;
      throw new ApiError(errMsg, res.status, parsed);
    }

    return parsed as T;
  } catch (err: any) {
    clearTimeout(id);
    if (err.name === 'AbortError') {
      throw new ApiError(`Request timeout after ${timeout}ms: ${endpoint}`, 408);
    }
    throw err;
  }
}

// ============================================================================
// MTProto RPC Registry Client Subsystem
// ============================================================================

export const mtprotoApi = {
  /**
   * Invokes an official Telegram MTProto RPC method on the backend
   */
  invoke: async <T = any>(
    method: string,
    params: Record<string, any> = {},
    sessionString?: string
  ): Promise<ApiResponse<T>> => {
    return request<ApiResponse<T>>('/api/telegram/mtproto/invoke', {
      method: 'POST',
      body: JSON.stringify({ method, params, sessionString }),
    });
  },

  /**
   * Sends a message via MTProto RPC
   */
  sendMessage: async (peer: string, message: string, replyToMsgId?: string | number) => {
    return mtprotoApi.invoke('messages.sendMessage', { peer, message, replyToMsgId });
  },

  /**
   * Sends an emoji reaction via MTProto RPC
   */
  sendReaction: async (peer: string, msgId: number | string, reaction: string) => {
    return mtprotoApi.invoke('messages.sendReaction', { peer, msgId: Number(msgId), reaction });
  },

  /**
   * Pins or unpins a chat dialog
   */
  toggleDialogPin: async (peer: string, pinned: boolean) => {
    return mtprotoApi.invoke('messages.toggleDialogPin', { peer, pinned });
  },

  /**
   * Fetches full Telegram user profile information
   */
  getFullUser: async (id: string) => {
    return mtprotoApi.invoke('users.getFullUser', { id });
  },

  /**
   * Gets privacy setting via MTProto RPC (account.getPrivacy)
   */
  getPrivacy: async (key: string) => {
    return mtprotoApi.invoke('account.getPrivacy', { key });
  },

  /**
   * Sets privacy setting via MTProto RPC (account.setPrivacy)
   */
  setPrivacy: async (key: string, rule: any) => {
    return mtprotoApi.invoke('account.setPrivacy', { key, rule });
  },

  /**
   * Gets Two-Step Verification password status via MTProto RPC (account.getPassword)
   */
  getPassword: async () => {
    return mtprotoApi.invoke('account.getPassword', {});
  },
};

// ============================================================================
// Privacy & Two-Step Verification (2FA) Cloud Sync API
// ============================================================================

export interface PrivacyResponse {
  success: boolean;
  key: string;
  option: 'everybody' | 'contacts' | 'nobody';
  rules?: any[];
  [key: string]: any;
}

export interface PasswordResponse {
  success: boolean;
  result?: {
    hasPassword: boolean;
    hasRecovery?: boolean;
    hint?: string;
    loginEmailPattern?: string;
    emailUnconfirmedPattern?: string;
    pendingResetDate?: number;
  };
  hasPassword?: boolean;
  hint?: string;
  [key: string]: any;
}

export const privacyApi = {
  /**
   * Fetches privacy rules from Telegram Cloud (Api.account.GetPrivacy)
   */
  getPrivacy: async (key: 'phoneNumber' | 'statusTimestamp' | 'forwards' | string) => {
    return request<PrivacyResponse>(`/api/telegram/privacy?key=${encodeURIComponent(key)}`);
  },

  /**
   * Sets privacy rules on Telegram Cloud (Api.account.SetPrivacy)
   */
  setPrivacy: async (
    key: 'phoneNumber' | 'statusTimestamp' | 'forwards' | string,
    rule: 'everybody' | 'contacts' | 'nobody' | any
  ) => {
    return request<PrivacyResponse>('/api/telegram/privacy', {
      method: 'POST',
      body: JSON.stringify({ key, rule }),
    });
  },

  /**
   * Checks Two-Step Verification (2FA) status and password hint (Api.account.GetPassword)
   */
  getPassword: async () => {
    return request<PasswordResponse>('/api/telegram/2fa/password');
  },
};

// ============================================================================
// Automation & SQLite Auto-Replies API
// ============================================================================

export const automationApi = {
  getRules: async () => {
    return request<{ enabled: boolean; rules: any[] }>('/api/telegram/auto-replies');
  },

  addRule: async (rule: { keyword: string; replyText: string; matchType?: string; scope?: string }) => {
    return request<any>('/api/telegram/auto-replies/add', {
      method: 'POST',
      body: JSON.stringify(rule),
    });
  },

  updateRule: async (id: string, updates: any) => {
    return request<any>('/api/telegram/auto-replies/update', {
      method: 'POST',
      body: JSON.stringify({ id, ...updates }),
    });
  },

  deleteRule: async (id: string) => {
    return request<any>('/api/telegram/auto-replies/delete', {
      method: 'POST',
      body: JSON.stringify({ id }),
    });
  },

  toggleRule: async (id: string) => {
    return request<any>('/api/telegram/auto-replies/toggle', {
      method: 'POST',
      body: JSON.stringify({ id }),
    });
  },

  toggleAll: async (enabled?: boolean) => {
    return request<any>('/api/telegram/auto-replies/toggle-all', {
      method: 'POST',
      body: JSON.stringify({ enabled }),
    });
  },

  getMonitorKeywords: async () => {
    return request<{ keywords: string[] }>('/api/telegram/monitor-keywords');
  },

  setMonitorKeywords: async (keywords: string[]) => {
    return request<{ keywords: string[] }>('/api/telegram/monitor-keywords', {
      method: 'POST',
      body: JSON.stringify({ keywords }),
    });
  },
};

// ============================================================================
// Multi-Tier Cache & Telemetry API
// ============================================================================

export const cacheApi = {
  getStats: async () => {
    return request<any>('/api/telegram/cache/stats');
  },

  clear: async () => {
    return request<any>('/api/telegram/cache/clear', { method: 'POST' });
  },
};

// ============================================================================
// GramJS / MTProto Api Namespace Compatibility Layer
// ============================================================================

export const Api = {
  account: {
    GetPrivacy: async (params: { key: any } | any) => {
      const rawKey = params?.key !== undefined ? params.key : params;
      const keyStr =
        typeof rawKey === 'object' && rawKey?._
          ? rawKey._.replace('inputPrivacyKey', '').toLowerCase()
          : String(rawKey || '');

      const normalizedKey = keyStr.includes('phone')
        ? 'phoneNumber'
        : keyStr.includes('status') || keyStr.includes('timestamp')
        ? 'statusTimestamp'
        : keyStr.includes('forward')
        ? 'forwards'
        : String(rawKey || 'phoneNumber');

      try {
        const res = await mtprotoApi.invoke('account.getPrivacy', { key: normalizedKey });
        return res;
      } catch {
        return await privacyApi.getPrivacy(normalizedKey);
      }
    },

    SetPrivacy: async (params: { key: any; rules: any } | any) => {
      const rawKey = params?.key;
      const keyStr =
        typeof rawKey === 'object' && rawKey?._
          ? rawKey._.replace('inputPrivacyKey', '').toLowerCase()
          : String(rawKey || '');

      const normalizedKey = keyStr.includes('phone')
        ? 'phoneNumber'
        : keyStr.includes('status') || keyStr.includes('timestamp')
        ? 'statusTimestamp'
        : keyStr.includes('forward')
        ? 'forwards'
        : String(rawKey || 'phoneNumber');

      let ruleOption: 'everybody' | 'contacts' | 'nobody' = 'everybody';
      const rawRules = params?.rules ?? params?.rule;
      if (Array.isArray(rawRules)) {
        const firstRule = rawRules[0];
        const ruleName = typeof firstRule === 'object' ? firstRule?._ || '' : String(firstRule);
        if (ruleName.toLowerCase().includes('disallow') || ruleName.toLowerCase().includes('nobody')) {
          ruleOption = 'nobody';
        } else if (ruleName.toLowerCase().includes('contact')) {
          ruleOption = 'contacts';
        } else {
          ruleOption = 'everybody';
        }
      } else if (typeof rawRules === 'string') {
        ruleOption = rawRules as any;
      }

      try {
        return await mtprotoApi.invoke('account.setPrivacy', { key: normalizedKey, rule: ruleOption });
      } catch {
        return await privacyApi.setPrivacy(normalizedKey, ruleOption);
      }
    },

    GetPassword: async () => {
      try {
        return await mtprotoApi.invoke('account.getPassword', {});
      } catch {
        return await privacyApi.getPassword();
      }
    },
  },

  updates: {
    GetState: async () => {
      try {
        const res = await mtprotoApi.invoke('updates.getState', {});
        return res?.result || res;
      } catch {
        return await request('/api/telegram/updates/state');
      }
    },

    GetDifference: async (params: { pts?: number; date?: number; qts?: number; ptsTotalLimit?: number }) => {
      try {
        const res = await mtprotoApi.invoke('updates.getDifference', params);
        return res?.result || res;
      } catch {
        const query = new URLSearchParams();
        if (params.pts !== undefined) query.set('pts', String(params.pts));
        if (params.date !== undefined) query.set('date', String(params.date));
        if (params.qts !== undefined) query.set('qts', String(params.qts));
        if (params.ptsTotalLimit !== undefined) query.set('limit', String(params.ptsTotalLimit));
        return await request(`/api/telegram/updates/difference?${query.toString()}`);
      }
    },
  },

  InputPrivacyKeyPhoneNumber: () => ({ _: 'inputPrivacyKeyPhoneNumber' }),
  InputPrivacyKeyStatusTimestamp: () => ({ _: 'inputPrivacyKeyStatusTimestamp' }),
  InputPrivacyKeyForwards: () => ({ _: 'inputPrivacyKeyForwards' }),
  InputPrivacyValueAllowAll: () => ({ _: 'inputPrivacyValueAllowAll' }),
  InputPrivacyValueAllowContacts: () => ({ _: 'inputPrivacyValueAllowContacts' }),
  InputPrivacyValueDisallowAll: () => ({ _: 'inputPrivacyValueDisallowAll' }),
};

// ============================================================================
// Unified API Surface Export
// ============================================================================

export const api = {
  request,
  mtproto: mtprotoApi,
  privacy: privacyApi,
  automation: automationApi,
  cache: cacheApi,
  Api,
};

export default api;
