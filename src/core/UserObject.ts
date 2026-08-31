/**
 * Official Telegram UserObject Implementation (MTProto 2.0 & Android Architecture)
 */

export interface TelegramUser {
  id: string | number;
  first_name?: string;
  last_name?: string;
  username?: string;
  phone?: string;
  photo?: any;
  status?: any;
  bot?: boolean;
  verified?: boolean;
  premium?: boolean;
  deleted?: boolean;
  mutual_contact?: boolean;
  self?: boolean;
  contact?: boolean;
  access_hash?: string;
}

export class UserObject {
  public static isDeleted(user?: TelegramUser | null): boolean {
    return !user || Boolean(user.deleted);
  }

  public static isContact(user?: TelegramUser | null): boolean {
    return Boolean(user && (user.contact || user.mutual_contact));
  }

  public static isUserSelf(user?: TelegramUser | null): boolean {
    return Boolean(user && user.self);
  }

  public static isBot(user?: TelegramUser | null): boolean {
    return Boolean(user && user.bot);
  }

  public static getFirstName(user?: TelegramUser | null): string {
    if (!user) return '';
    if (user.deleted) return 'Deleted Account';
    return user.first_name || user.username || '';
  }

  public static getUserName(user?: TelegramUser | null): string {
    if (!user) return '';
    if (user.deleted) return 'Deleted Account';
    const first = (user.first_name || '').trim();
    const last = (user.last_name || '').trim();
    if (first && last) return `${first} ${last}`;
    if (first) return first;
    if (last) return last;
    if (user.username) return `@${user.username}`;
    return user.phone || 'Telegram User';
  }

  public static getPublicUsername(user?: TelegramUser | null): string | null {
    if (!user || !user.username) return null;
    return `@${user.username.replace(/^@/, '')}`;
  }
}
