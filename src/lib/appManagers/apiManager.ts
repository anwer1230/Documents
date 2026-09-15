/**
 * Telegram Web K API Manager (apiManager.ts)
 * Based on morethanwords/tweb src/lib/appManagers/apiManager.ts
 * Manages communication with Telegram Data Centers & server API proxies.
 */

import { csrfFetch } from '../../services/csrfFetch';
import { sessionStorage } from '../storages/sessionStorage';

export interface InvokeOptions {
  dcId?: number;
  noAuth?: boolean;
  timeout?: number;
}

export class ApiManager {
  private baseApiUrl = '/api/telegram';

  public async invoke<T = any>(method: string, params: Record<string, any> = {}, options: InvokeOptions = {}): Promise<T> {
    const sessionToken = sessionStorage.get<string>('session_token') || '';
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    };
    if (sessionToken && !options.noAuth) {
      headers['x-telegram-session'] = sessionToken;
      headers['Authorization'] = `Bearer ${sessionToken}`;
    }

    // Map method names to standard backend endpoints if applicable
    let endpoint = `${this.baseApiUrl}/${method.replace('.', '/')}`;
    if (method === 'messages.getHistory') endpoint = `${this.baseApiUrl}/messages/history`;
    else if (method === 'messages.sendMessage') endpoint = `${this.baseApiUrl}/messages/send`;
    else if (method === 'messages.getDialogs') endpoint = `${this.baseApiUrl}/chats`;
    else if (method === 'updates.getDifference') endpoint = `${this.baseApiUrl}/updates/difference`;
    else if (method === 'updates.getState') endpoint = `${this.baseApiUrl}/updates/state`;
    else if (method === 'users.getFullUser') endpoint = `${this.baseApiUrl}/users/get`;

    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), options.timeout || 30000);

    try {
      const res = await csrfFetch(endpoint, {
        method: 'POST',
        headers,
        body: JSON.stringify(params),
        signal: controller.signal,
      });

      clearTimeout(timeout);
      if (!res.ok) {
        const errJson = await res.json().catch(() => ({}));
        throw new Error(errJson.error || `MTProto RPC Error ${res.status}: ${res.statusText}`);
      }
      return await res.json();
    } catch (err: any) {
      clearTimeout(timeout);
      throw err;
    }
  }

  public setSessionToken(token: string): void {
    sessionStorage.set('session_token', token);
  }

  public getSessionToken(): string | null {
    return sessionStorage.get<string>('session_token');
  }
}

export const apiManager = new ApiManager();
export default apiManager;
