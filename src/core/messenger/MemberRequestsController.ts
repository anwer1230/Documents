import { MemberJoinRequestItem } from '../../types';

export const INITIAL_MEMBER_REQUESTS: MemberJoinRequestItem[] = [
  {
    id: 'req_001',
    chatId: 'chat_tech_vip',
    chatTitle: 'Tech Leaders VIP Club',
    userId: 'user_alexander',
    userName: 'Alexander Wright',
    userAvatar: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100&h=100&fit=crop&crop=faces',
    userBio: 'Senior Systems Engineer & Open-Source Contributor',
    requestedAt: '25 minutes ago',
    status: 'pending',
  },
  {
    id: 'req_002',
    chatId: 'chat_tech_vip',
    chatTitle: 'Tech Leaders VIP Club',
    userId: 'user_fatima',
    userName: 'Fatima Al-Zahra',
    userAvatar: 'https://images.unsplash.com/photo-1580489944761-15a19d654956?w=100&h=100&fit=crop&crop=faces',
    userBio: 'Crypto Researcher & Fullstack Developer',
    requestedAt: '1 hour ago',
    status: 'pending',
  },
  {
    id: 'req_003',
    chatId: 'chat_telegram_news',
    chatTitle: 'Telegram Beta Testers',
    userId: 'user_david',
    userName: 'David Miller',
    userAvatar: 'https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=100&h=100&fit=crop&crop=faces',
    userBio: 'Mobile QA Specialist',
    requestedAt: '3 hours ago',
    status: 'pending',
  },
];

export class MemberRequestsController {
  private static instances = new Map<number, MemberRequestsController>();
  private currentAccount: number = 0;
  private requests: MemberJoinRequestItem[] = [...INITIAL_MEMBER_REQUESTS];

  public static getInstance(accountNum: number = 0): MemberRequestsController {
    if (!MemberRequestsController.instances.has(accountNum)) {
      const inst = new MemberRequestsController();
      inst.currentAccount = accountNum;
      MemberRequestsController.instances.set(accountNum, inst);
    }
    return MemberRequestsController.instances.get(accountNum)!;
  }

  public getPendingRequests(chatId?: string): MemberJoinRequestItem[] {
    if (chatId) {
      return this.requests.filter((r) => r.chatId === chatId && r.status === 'pending');
    }
    return this.requests.filter((r) => r.status === 'pending');
  }

  public approveRequest(requestId: string): { success: boolean; request?: MemberJoinRequestItem } {
    const req = this.requests.find((r) => r.id === requestId);
    if (req) {
      req.status = 'approved';
      return { success: true, request: req };
    }
    return { success: false };
  }

  public declineRequest(requestId: string): { success: boolean; request?: MemberJoinRequestItem } {
    const req = this.requests.find((r) => r.id === requestId);
    if (req) {
      req.status = 'declined';
      return { success: true, request: req };
    }
    return { success: false };
  }

  public approveAll(chatId?: string): number {
    const pending = this.getPendingRequests(chatId);
    pending.forEach((r) => (r.status = 'approved'));
    return pending.length;
  }
}

export const memberRequestsController = new MemberRequestsController();
