/**
 * AuthTokensHelper.ts - Token Management for MTProto Sessions
 */

import { SecureSessionStorage } from '../../utils/SecureSessionStorage';

export class AuthTokensHelper {
  private static instance: AuthTokensHelper;

  public static getInstance(): AuthTokensHelper {
    if (!AuthTokensHelper.instance) {
      AuthTokensHelper.instance = new AuthTokensHelper();
    }
    return AuthTokensHelper.instance;
  }

  public getSessionToken(accountNum: number = 0): string | null {
    return SecureSessionStorage.getItem<string>(`tg_session_${accountNum}`) || SecureSessionStorage.getItem<string>('tg_session_string');
  }

  public saveSessionToken(token: string, accountNum: number = 0): void {
    SecureSessionStorage.setItem(`tg_session_${accountNum}`, token);
    SecureSessionStorage.setItem('tg_session_string', token);
  }

  public clearSessionToken(accountNum: number = 0): void {
    SecureSessionStorage.removeItem(`tg_session_${accountNum}`);
    if (accountNum === 0) {
      SecureSessionStorage.removeItem('tg_session_string');
    }
  }

  public saveUserBackup(accountOrUser: number | any, userOrAccount?: any): void {
    if (typeof accountOrUser === 'number') {
      SecureSessionStorage.setItem(`tg_user_backup_${accountOrUser}`, userOrAccount);
    } else {
      SecureSessionStorage.setItem(`tg_user_backup_0`, accountOrUser);
    }
  }

  public restoreUserBackup(accountNum: number = 0): any {
    return SecureSessionStorage.getItem(`tg_user_backup_${accountNum}`);
  }

  public hasPersistentSession(accountNum: number = 0): boolean {
    return Boolean(this.getSessionToken(accountNum));
  }

  public protectRealUserSession(_accountNum?: number): void {}

  public registerDeviceWithPushToken(_tokenOrAccount?: string | number): void {}

  public saveFutureAuthToken(accountOrToken: any, tokenOrExpires?: any, _expires?: any): void {
    if (typeof accountOrToken === 'number' && typeof tokenOrExpires === 'string') {
      SecureSessionStorage.setItem(`tg_future_token_${accountOrToken}`, tokenOrExpires);
    } else if (typeof accountOrToken === 'string') {
      SecureSessionStorage.setItem(`tg_future_token_0`, accountOrToken);
    }
  }

  public clearAccountTokens(accountNum: number = 0): void {
    this.clearSessionToken(accountNum);
  }
}

export const authTokensHelper = AuthTokensHelper.getInstance();
