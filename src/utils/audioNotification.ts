/**
 * Web Audio API Synthesizer for Telegram's Signature Notification Chimes & Ringtone
 */

class TelegramAudioEngine {
  private ctx: AudioContext | null = null;
  private volume: number = 80; // 0 to 100 percentage
  private isMuted: boolean = false;

  public setVolume(vol: number) {
    this.volume = Math.max(0, Math.min(100, typeof vol === 'number' ? vol : 80));
  }

  public getVolume(): number {
    return this.volume;
  }

  public setMuted(muted: boolean) {
    this.isMuted = Boolean(muted);
  }

  public getIsMuted(): boolean {
    return this.isMuted;
  }

  public toggleMute(): boolean {
    this.isMuted = !this.isMuted;
    return this.isMuted;
  }

  private getVolumeFactor(): number {
    if (this.isMuted || this.volume <= 0) return 0;
    return this.volume / 100;
  }

  private getContext(): AudioContext | null {
    if (typeof window === 'undefined') return null;
    if (!this.ctx) {
      const AudioCtx = window.AudioContext || (window as any).webkitAudioContext;
      if (AudioCtx) {
        this.ctx = new AudioCtx();
      }
    }
    if (this.ctx && this.ctx.state === 'suspended') {
      this.ctx.resume().catch(() => {});
    }
    return this.ctx;
  }

  /**
   * Signature Telegram Message Chime (Clean, crystal two-tone frequency chime)
   */
  public playMessageChime(isSilent: boolean = false) {
    const factor = this.getVolumeFactor();
    if (isSilent || factor <= 0.001) return;
    try {
      const ctx = this.getContext();
      if (!ctx) return;

      const now = ctx.currentTime;
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();

      osc.type = 'sine';
      osc.frequency.setValueAtTime(880, now); // A5
      osc.frequency.exponentialRampToValueAtTime(1318.51, now + 0.08); // E6

      gain.gain.setValueAtTime(0, now);
      gain.gain.linearRampToValueAtTime(0.28 * factor, now + 0.02);
      gain.gain.exponentialRampToValueAtTime(0.001, now + 0.35);

      osc.connect(gain);
      gain.connect(ctx.destination);

      osc.start(now);
      osc.stop(now + 0.38);
    } catch {}
  }

  /**
   * Telegram Channel Broadcast Post Sound (Deeper bell resonance)
   */
  public playChannelPostSound(isSilent: boolean = false) {
    const factor = this.getVolumeFactor();
    if (isSilent || factor <= 0.001) return;
    try {
      const ctx = this.getContext();
      if (!ctx) return;

      const now = ctx.currentTime;
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();

      osc.type = 'sine';
      osc.frequency.setValueAtTime(659.25, now); // E5
      osc.frequency.exponentialRampToValueAtTime(987.77, now + 0.07); // B5

      gain.gain.setValueAtTime(0, now);
      gain.gain.linearRampToValueAtTime(0.22 * factor, now + 0.02);
      gain.gain.exponentialRampToValueAtTime(0.001, now + 0.45);

      osc.connect(gain);
      gain.connect(ctx.destination);

      osc.start(now);
      osc.stop(now + 0.48);
    } catch {}
  }

  /**
   * Telegram Sent Message Pop sound (Subtle tactile click)
   */
  public playSentPop() {
    const factor = this.getVolumeFactor();
    if (factor <= 0.001) return;
    try {
      const ctx = this.getContext();
      if (!ctx) return;

      const now = ctx.currentTime;
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();

      osc.type = 'sine';
      osc.frequency.setValueAtTime(520, now);
      osc.frequency.exponentialRampToValueAtTime(260, now + 0.04);

      gain.gain.setValueAtTime(0.18 * factor, now);
      gain.gain.exponentialRampToValueAtTime(0.001, now + 0.05);

      osc.connect(gain);
      gain.connect(ctx.destination);

      osc.start(now);
      osc.stop(now + 0.06);
    } catch {}
  }

  /**
   * Telegram Reaction Pop sound
   */
  public playReactionSound() {
    const factor = this.getVolumeFactor();
    if (factor <= 0.001) return;
    try {
      const ctx = this.getContext();
      if (!ctx) return;

      const now = ctx.currentTime;
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();

      osc.type = 'triangle';
      osc.frequency.setValueAtTime(587.33, now); // D5
      osc.frequency.exponentialRampToValueAtTime(1174.66, now + 0.06); // D6

      gain.gain.setValueAtTime(0.15 * factor, now);
      gain.gain.exponentialRampToValueAtTime(0.001, now + 0.18);

      osc.connect(gain);
      gain.connect(ctx.destination);

      osc.start(now);
      osc.stop(now + 0.2);
    } catch {}
  }
}

export const telegramAudio = new TelegramAudioEngine();
