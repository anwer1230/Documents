import { ContactBirthday } from '../../types';

export const INITIAL_CONTACT_BIRTHDAYS: ContactBirthday[] = [
  {
    userId: 'user_pavel',
    name: 'Pavel Durov',
    avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100&h=100&fit=crop&crop=faces',
    username: 'durov',
    birthDate: '10-10',
    isToday: false,
    daysRemaining: 41,
    age: 41,
  },
  {
    userId: 'user_sarah',
    name: 'Sarah Connor',
    avatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=100&h=100&fit=crop&crop=faces',
    username: 'sarah_c',
    birthDate: new Date().toISOString().slice(5, 10), // Today!
    isToday: true,
    daysRemaining: 0,
    age: 28,
  },
  {
    userId: 'user_ahmed',
    name: 'Ahmed Al-Mansoor',
    avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100&h=100&fit=crop&crop=faces',
    username: 'ahmed_m',
    birthDate: '09-02',
    isToday: false,
    daysRemaining: 3,
    age: 32,
  },
];

export class BirthdayController {
  private static instances = new Map<number, BirthdayController>();
  private currentAccount: number = 0;
  private birthdays: ContactBirthday[] = [...INITIAL_CONTACT_BIRTHDAYS];

  public static getInstance(accountNum: number = 0): BirthdayController {
    if (!BirthdayController.instances.has(accountNum)) {
      const inst = new BirthdayController();
      inst.currentAccount = accountNum;
      BirthdayController.instances.set(accountNum, inst);
    }
    return BirthdayController.instances.get(accountNum)!;
  }

  public getBirthdays(): ContactBirthday[] {
    return this.birthdays;
  }

  public getTodayBirthdays(): ContactBirthday[] {
    return this.birthdays.filter((b) => b.isToday);
  }

  public getUpcomingBirthdays(): ContactBirthday[] {
    return this.birthdays.filter((b) => !b.isToday && b.daysRemaining <= 14);
  }

  public markCelebrated(userId: string): void {
    const item = this.birthdays.find((b) => b.userId === userId);
    if (item) {
      item.hasCelebrated = true;
    }
  }
}

export const birthdayController = new BirthdayController();
