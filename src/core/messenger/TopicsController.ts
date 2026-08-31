/**
 * TopicsController.ts - org.telegram.messenger.TopicsController
 */

export class TopicsController {
  private currentAccount: number;

  constructor(accountNum: number = 0) {
    this.currentAccount = accountNum;
  }

  public getTopics(_chatId: string | number): any[] {
    return [];
  }
}

export const topicsController = new TopicsController(0);
