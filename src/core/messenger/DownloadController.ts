/**
 * DownloadController.ts - org.telegram.messenger.DownloadController
 */

export class DownloadController {
  private static instances = new Map<number, DownloadController>();
  private currentAccount: number;

  public static getInstance(accountNum: number = 0): DownloadController {
    if (!DownloadController.instances.has(accountNum)) {
      DownloadController.instances.set(accountNum, new DownloadController(accountNum));
    }
    return DownloadController.instances.get(accountNum)!;
  }

  constructor(accountNum: number = 0) {
    this.currentAccount = accountNum;
  }

  public canDownloadMedia(): boolean {
    return true;
  }
}

export const downloadController = DownloadController.getInstance(0);
