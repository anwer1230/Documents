/**
 * Backend API Client & Gemini Service Layer
 * 
 * Encapsulates all backend HTTP API interactions including Gemini AI requests,
 * Telegram MTProto bridge endpoints, chat management, authentication,
 * and system telemetry while keeping API credentials strictly server-side.
 */

// ============================================================================
// Types & Interfaces
// ============================================================================

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

export interface GeminiGenerateOptions {
  /** The primary prompt text to send to the Gemini model */
  prompt: string;
  /** Optional system instruction or role persona */
  systemInstruction?: string;
  /**
   * Model selection. Defaults to 'gemini-3.8-flash'.
   * Standard valid models: 'gemini-3.8-flash', 'gemini-3.1-flash-lite', 'gemini-3.1-pro-preview'
   */
  model?: string;
  /** Sampling temperature (0.0 to 2.0) */
  temperature?: number;
  /** Maximum number of generated tokens */
  maxOutputTokens?: number;
}

export interface GeminiGenerateResponse {
  success: boolean;
  text: string;
  model: string;
  timestamp: string;
  error?: string;
  message?: string;
}

export interface GeminiStatusResponse {
  success: boolean;
  configured: boolean;
  hasGeminiApiKey: boolean;
  defaultModel: string;
  availableModels: string[];
}

export interface GeminiSummarizeMessage {
  id?: string | number;
  senderName?: string;
  text?: string;
  timestamp?: string;
  out?: boolean;
  isOutgoing?: boolean;
  media?: { type?: string };
}

export interface GeminiSummarizeOptions {
  chatId: string;
  chatTitle?: string;
  messages?: GeminiSummarizeMessage[];
  language?: 'ar' | 'en' | string;
  sessionString?: string;
  phone?: string;
}

export interface GeminiSummarizeResponse {
  success: boolean;
  summary: string;
  messageCount: number;
  model: string;
  chatTitle: string;
  timestamp: string;
  error?: string;
  message?: string;
}

export interface TelegramAuthSendCodeResponse {
  success: boolean;
  phoneCodeHash?: string;
  isCodeSent?: boolean;
  nextType?: string;
  timeout?: number;
  error?: string;
  message?: string;
}

export interface TelegramAuthVerifyResponse {
  success: boolean;
  user?: {
    id: string | number;
    firstName?: string;
    lastName?: string;
    username?: string;
    phone?: string;
  };
  sessionString?: string;
  requiresPassword?: boolean;
  error?: string;
  message?: string;
}

export interface TelegramMessagePayload {
  chatId: string | number;
  text: string;
  replyToMsgId?: string | number;
  sessionString?: string;
  phone?: string;
}

// ============================================================================
// Core API Request Helper
// ============================================================================

class ApiError extends Error {
  status: number;
  data: any;

  constructor(message: string, status: number = 500, data: any = null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.data = data;
  }
}

async function request<T = any>(endpoint: string, options: RequestOptions = {}): Promise<T> {
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
  const timeoutId = setTimeout(() => controller.abort(), timeout);

  try {
    const isFormData = fetchOptions.body instanceof FormData;
    const defaultHeaders: Record<string, string> = isFormData
      ? {}
      : {
          'Content-Type': 'application/json',
          Accept: 'application/json',
        };

    const response = await fetch(url, {
      ...fetchOptions,
      headers: {
        ...defaultHeaders,
        ...(headers as Record<string, string>),
      },
      signal: controller.signal,
    });

    clearTimeout(timeoutId);

    const contentType = response.headers.get('content-type') || '';
    let responseData: any;
    if (contentType.includes('application/json')) {
      responseData = await response.json();
    } else {
      responseData = await response.text();
    }

    if (!response.ok) {
      const errorMessage =
        (responseData && (responseData.message || responseData.error)) ||
        `HTTP Request failed with status ${response.status}: ${response.statusText}`;
      throw new ApiError(errorMessage, response.status, responseData);
    }

    return responseData as T;
  } catch (err: any) {
    clearTimeout(timeoutId);
    if (err.name === 'AbortError') {
      throw new ApiError(`Request timed out after ${timeout}ms`, 408);
    }
    if (err instanceof ApiError) {
      throw err;
    }
    throw new ApiError(err?.message || 'Network request failed', 0);
  }
}

// ============================================================================
// Gemini AI API Module
// ============================================================================

export const geminiApi = {
  /**
   * Check Gemini AI server configuration and available models.
   */
  async getStatus(): Promise<GeminiStatusResponse> {
    return request<GeminiStatusResponse>('/api/gemini/status', {
      method: 'GET',
    });
  },

  /**
   * Generate text or general reasoning with Gemini models through the backend API.
   * Defaults to 'gemini-3.8-flash'.
   */
  async generateContent(options: GeminiGenerateOptions): Promise<GeminiGenerateResponse> {
    const {
      prompt,
      systemInstruction,
      model = 'gemini-3.8-flash',
      temperature,
      maxOutputTokens,
    } = options;

    return request<GeminiGenerateResponse>('/api/gemini/generate', {
      method: 'POST',
      body: JSON.stringify({
        prompt,
        systemInstruction,
        model,
        temperature,
        maxOutputTokens,
      }),
    });
  },

  /**
   * Summarize a Telegram chat thread (last 100 messages) using Gemini AI.
   */
  async summarizeChat(options: GeminiSummarizeOptions): Promise<GeminiSummarizeResponse> {
    const {
      chatId,
      chatTitle,
      messages = [],
      language = 'ar',
      sessionString,
      phone,
    } = options;

    return request<GeminiSummarizeResponse>('/api/telegram/chat/summarize', {
      method: 'POST',
      body: JSON.stringify({
        chatId,
        chatTitle,
        messages,
        language,
        sessionString,
        phone,
      }),
    });
  },

  /**
   * Translate text between languages using Gemini models.
   */
  async translateText(text: string, targetLanguage: string = 'ar', sourceLanguage?: string): Promise<string> {
    const prompt = sourceLanguage
      ? `Translate the following text from ${sourceLanguage} to ${targetLanguage}:\n\n"${text}"\n\nReturn ONLY the translation without quotes or commentary.`
      : `Translate the following text to ${targetLanguage}:\n\n"${text}"\n\nReturn ONLY the translation without quotes or commentary.`;

    const response = await this.generateContent({
      prompt,
      systemInstruction: 'You are an accurate, high-fidelity real-time language translator. Return only the requested translation.',
      model: 'gemini-3.8-flash',
      temperature: 0.2,
    });

    return (response.text || '').trim();
  },

  /**
   * Suggest 3 quick contextual replies for a chat message.
   */
  async suggestSmartReplies(
    lastMessage: string,
    chatContext?: string,
    language: 'ar' | 'en' = 'ar'
  ): Promise<string[]> {
    const isArabic = language === 'ar';
    const prompt = isArabic
      ? `بناءً على الرسالة الأخيرة في محادثة تيليجرام: "${lastMessage}"${chatContext ? `\nسياق المحادثة: "${chatContext}"` : ''}
اقترح 3 ردود سريعة ومناسبة (كل رد جملة قصيرة واحدة).
أرجع الردود بصيغة قائمة مفصولة بأسطر جديدة فقط.`
      : `Based on the latest Telegram message: "${lastMessage}"${chatContext ? `\nContext: "${chatContext}"` : ''}
Suggest 3 concise, natural quick replies (one short sentence each).
Return the replies as a plain line-separated list only.`;

    try {
      const response = await this.generateContent({
        prompt,
        systemInstruction: 'You are a smart chat assistant generating quick, contextual reply suggestions. Return only the 3 suggestions, one per line.',
        model: 'gemini-3.8-flash',
        temperature: 0.4,
      });

      const lines = (response.text || '')
        .split('\n')
        .map((l) => l.replace(/^[-*•\d.)\s]+/, '').trim())
        .filter(Boolean);

      return lines.slice(0, 3);
    } catch {
      return isArabic
        ? ['تمام، شكراً لك!', 'سأراجع الأمر قريباً.', 'حسناً، متفقين.']
        : ['Sounds good, thanks!', 'I will check it soon.', 'Got it, agreed!'];
    }
  },
};

// ============================================================================
// Telegram Authentication Module
// ============================================================================

export const authApi = {
  /**
   * Request an authentication code via Telegram MTProto.
   */
  async sendCode(phone: string, forceSms: boolean = false): Promise<TelegramAuthSendCodeResponse> {
    return request<TelegramAuthSendCodeResponse>('/api/telegram/auth/send-code', {
      method: 'POST',
      body: JSON.stringify({ phone, forceSms }),
    });
  },

  /**
   * Verify an authentication code and complete login.
   */
  async verifyCode(
    phone: string,
    phoneCodeHash: string,
    code: string,
    password?: string
  ): Promise<TelegramAuthVerifyResponse> {
    return request<TelegramAuthVerifyResponse>('/api/telegram/auth/verify-code', {
      method: 'POST',
      body: JSON.stringify({ phone, phoneCodeHash, code, password }),
    });
  },

  /**
   * Resend SMS/call authentication code.
   */
  async resendCode(phone: string, phoneCodeHash: string): Promise<ApiResponse> {
    return request<ApiResponse>('/api/telegram/auth/resend-code', {
      method: 'POST',
      body: JSON.stringify({ phone, phoneCodeHash }),
    });
  },

  /**
   * Perform initial client handshake with the backend.
   */
  async handshake(payload: Record<string, any> = {}): Promise<ApiResponse> {
    return request<ApiResponse>('/api/telegram/auth/handshake', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },
};

// ============================================================================
// Telegram Messages & Media Module
// ============================================================================

export const messagesApi = {
  /**
   * Send a text message to a conversation.
   */
  async send(payload: TelegramMessagePayload): Promise<ApiResponse> {
    return request<ApiResponse>('/api/telegram/messages/send', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },

  /**
   * Upload and send media files (photos, audio, documents).
   */
  async sendMedia(formData: FormData): Promise<ApiResponse> {
    return request<ApiResponse>('/api/telegram/messages/send-media', {
      method: 'POST',
      body: formData,
    });
  },

  /**
   * Edit an existing message.
   */
  async edit(
    chatId: string | number,
    messageId: string | number,
    text: string,
    sessionString?: string,
    phone?: string
  ): Promise<ApiResponse> {
    return request<ApiResponse>('/api/telegram/messages/edit', {
      method: 'POST',
      body: JSON.stringify({ chatId, messageId, text, sessionString, phone }),
    });
  },

  /**
   * Delete messages from a chat.
   */
  async delete(
    chatId: string | number,
    messageIds: (string | number)[],
    revoke: boolean = true,
    sessionString?: string,
    phone?: string
  ): Promise<ApiResponse> {
    return request<ApiResponse>('/api/telegram/messages/delete', {
      method: 'POST',
      body: JSON.stringify({ chatId, messageIds, revoke, sessionString, phone }),
    });
  },

  /**
   * Forward messages to another chat.
   */
  async forward(
    fromChatId: string | number,
    toChatId: string | number,
    messageIds: (string | number)[],
    sessionString?: string,
    phone?: string
  ): Promise<ApiResponse> {
    return request<ApiResponse>('/api/telegram/messages/forward', {
      method: 'POST',
      body: JSON.stringify({ fromChatId, toChatId, messageIds, sessionString, phone }),
    });
  },

  /**
   * Send an emoji reaction to a message.
   */
  async react(
    chatId: string | number,
    messageId: string | number,
    reaction: string,
    sessionString?: string,
    phone?: string
  ): Promise<ApiResponse> {
    return request<ApiResponse>('/api/telegram/messages/react', {
      method: 'POST',
      body: JSON.stringify({ chatId, messageId, reaction, sessionString, phone }),
    });
  },
};

// ============================================================================
// Telegram Chats & Dialogs Module
// ============================================================================

export const chatsApi = {
  /**
   * Get full info for a chat (member count, online count, description).
   */
  async getFullInfo(chatId: string | number, sessionString?: string, phone?: string): Promise<ApiResponse> {
    return request<ApiResponse>('/api/telegram/chat/full-info', {
      method: 'POST',
      body: JSON.stringify({ chatId, sessionString, phone }),
    });
  },

  /**
   * Archive or unarchive a chat dialog.
   */
  async archive(chatId: string | number, folderId: number = 1, sessionString?: string): Promise<ApiResponse> {
    return request<ApiResponse>('/api/telegram/dialogs/archive', {
      method: 'POST',
      body: JSON.stringify({ chatId, folderId, sessionString }),
    });
  },

  /**
   * Block a user.
   */
  async blockUser(userId: string | number, sessionString?: string): Promise<ApiResponse> {
    return request<ApiResponse>('/api/telegram/users/block', {
      method: 'POST',
      body: JSON.stringify({ userId, sessionString }),
    });
  },

  /**
   * Search messages, chats, or contacts.
   */
  async search(query: string, peer?: string | number, limit: number = 50): Promise<ApiResponse> {
    return request<ApiResponse>('/api/telegram/search', {
      method: 'POST',
      body: JSON.stringify({ query, peer, limit }),
    });
  },

  /**
   * Join a chat or channel via invite link or username.
   */
  async joinLink(inviteLink: string, sessionString?: string): Promise<ApiResponse> {
    return request<ApiResponse>('/api/telegram/links/join', {
      method: 'POST',
      body: JSON.stringify({ link: inviteLink, sessionString }),
    });
  },
};

// ============================================================================
// System, Cache, & Diagnostics Module
// ============================================================================

export const systemApi = {
  /**
   * Check backend health.
   */
  async getHealth(): Promise<{ status: string; timestamp?: string }> {
    return request('/api/health');
  },

  /**
   * Get environment diagnostics and configuration flags.
   */
  async getEnvInfo(): Promise<ApiResponse> {
    return request('/api/env/info');
  },

  /**
   * Get Redis / Memory Cache statistics.
   */
  async getCacheStats(): Promise<ApiResponse> {
    return request('/api/cache/stats');
  },

  /**
   * Clear hot cache.
   */
  async clearCache(): Promise<ApiResponse> {
    return request('/api/cache/clear', { method: 'POST' });
  },

  /**
   * Get Data Center (DC) connection status.
   */
  async getDcStatus(): Promise<ApiResponse> {
    return request('/api/dc/status');
  },

  /**
   * Switch Telegram Data Center (DC).
   */
  async switchDc(dcId: number): Promise<ApiResponse> {
    return request('/api/dc/switch', {
      method: 'POST',
      body: JSON.stringify({ dcId }),
    });
  },

  /**
   * Check auto-updater status.
   */
  async getUpdateStatus(): Promise<ApiResponse> {
    return request('/api/update/status');
  },

  /**
   * Trigger self-update.
   */
  async triggerUpdate(): Promise<ApiResponse> {
    return request('/api/update/trigger', { method: 'POST' });
  },
};

// ============================================================================
// Unified API Client Export
// ============================================================================

export const api = {
  request,
  gemini: geminiApi,
  auth: authApi,
  messages: messagesApi,
  chats: chatsApi,
  system: systemApi,
};

export default api;
