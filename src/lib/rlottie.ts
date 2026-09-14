/**
 * Telegram Web K Lottie Animation Player Helper (rlottie.ts)
 * Based on morethanwords/tweb src/lib/rlottie.ts
 */

export interface RLottieOptions {
  container: HTMLElement;
  animationData?: any;
  path?: string;
  loop?: boolean;
  autoplay?: boolean;
}

export class RLottiePlayer {
  private container: HTMLElement;
  private isPlaying = false;
  private animFrameId: number | null = null;

  constructor(options: RLottieOptions) {
    this.container = options.container;
    if (options.autoplay !== false) {
      this.play();
    }
  }

  public play(): void {
    this.isPlaying = true;
  }

  public pause(): void {
    this.isPlaying = false;
    if (this.animFrameId) {
      cancelAnimationFrame(this.animFrameId);
      this.animFrameId = null;
    }
  }

  public stop(): void {
    this.pause();
  }

  public destroy(): void {
    this.stop();
    this.container.innerHTML = '';
  }
}

export default RLottiePlayer;
