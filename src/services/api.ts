/**
 * Backend API Client & MTProto Bridge Service Layer
 * 
 * Provides typed client methods for interacting with Telegram MTProto 2.0,
 * RPC invocations, SQLite auto-replies/automation, multi-tier cache,
 * and background synchronizations while preserving full TypeScript safety.
 */

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
    const res = await fetch(url, {
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
// Unified API Surface Export
// ============================================================================

export const api = {
  request,
  mtproto: mtprotoApi,
  automation: automationApi,
  cache: cacheApi,
};

export default api;
