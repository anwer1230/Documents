/**
 * StoriesController.ts - org.telegram.messenger.StoriesController
 */

export class StoriesController {
  private static instances = new Map<number, StoriesController>();
  private currentAccount: number;

  public static getInstance(accountNum: number = 0): StoriesController {
    if (!StoriesController.instances.has(accountNum)) {
      StoriesController.instances.set(accountNum, new StoriesController(accountNum));
    }
    return StoriesController.instances.get(accountNum)!;
  }

  constructor(accountNum: number = 0) {
    this.currentAccount = accountNum;
  }

  public getStories(): any[] {
    return [];
  }
}

export const storiesController = StoriesController.getInstance(0);
