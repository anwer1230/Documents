import { MemberJoinRequestItem } from '../../types';

export const INITIAL_MEMBER_REQUESTS: MemberJoinRequestItem[] = [];

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
