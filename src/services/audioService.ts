// services/audioService.ts

class AudioService {
  private audioContext: AudioContext | null = null;
  private currentSound: string = 'classic';

  constructor() {
    // Lazy initialized on first user interaction if needed
  }

  private getContext(): AudioContext | null {
    if (typeof window === 'undefined') return null;
    if (!this.audioContext) {
      const AudioCtx = window.AudioContext || (window as any).webkitAudioContext;
      if (AudioCtx) {
        this.audioContext = new AudioCtx();
      }
    }
    if (this.audioContext && this.audioContext.state === 'suspended') {
      this.audioContext.resume().catch(() => {});
    }
    return this.audioContext;
  }

  // تحميل الصوت من ملف أو التوليد النغمي برمجياً
  async loadSound(type: string): Promise<AudioBuffer | null> {
    const ctx = this.getContext();
    if (!ctx) return null;

    try {
      const response = await fetch(`/sounds/${type}.mp3`);
      if (response.ok) {
        const arrayBuffer = await response.arrayBuffer();
        return await ctx.decodeAudioData(arrayBuffer);
      }
    } catch (_) {
      // Fallback to Web Audio synthesis
    }
    return null;
  }

  // تشغيل صوت الإشعار
  async playNotification(soundType: string = this.currentSound) {
    if (soundType === 'silent') return;

    const ctx = this.getContext();
    if (!ctx) return;

    try {
      const buffer = await this.loadSound(soundType);
      if (buffer) {
        const source = ctx.createBufferSource();
        source.buffer = buffer;
        source.connect(ctx.destination);
        source.start(0);
        return;
      }
    } catch (error) {
      console.warn('[AudioService] Failed to play buffer, falling back to synthesized tone:', error);
    }

    // توليد النغمات برمجياً بالكامل عبر Web Audio API
    this.playSynthesizedTone(soundType);
  }

  // نغمات Web Audio مصممة برمجياً لمطابقة تليجرام وبدائلها
  private playSynthesizedTone(type: string) {
    const ctx = this.getContext();
    if (!ctx) return;

    const now = ctx.currentTime;

    switch (type) {
      case 'beep': {
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.type = 'sine';
        osc.frequency.setValueAtTime(900, now);
        gain.gain.setValueAtTime(0.25, now);
        gain.gain.exponentialRampToValueAtTime(0.001, now + 0.15);
        osc.start(now);
        osc.stop(now + 0.15);
        break;
      }

      case 'chime': {
        // نغمة جرس نقي بطبقتين
        [1046.5, 1318.5, 1567.98].forEach((freq, idx) => {
          const osc = ctx.createOscillator();
          const gain = ctx.createGain();
          osc.connect(gain);
          gain.connect(ctx.destination);
          osc.type = 'sine';
          osc.frequency.setValueAtTime(freq, now + idx * 0.04);
          gain.gain.setValueAtTime(0.18, now + idx * 0.04);
          gain.gain.exponentialRampToValueAtTime(0.001, now + 0.45);
          osc.start(now + idx * 0.04);
          osc.stop(now + 0.45);
        });
        break;
      }

      case 'bubble': {
        this.playBubblePop();
        break;
      }

      case 'classic':
      default: {
        // كلاسيك تليجرام المميز (نغمتان متتاليتان لطيفة)
        const notes = [
          { freq: 659.25, time: 0, duration: 0.18 }, // E5
          { freq: 880.0, time: 0.12, duration: 0.35 }, // A5
        ];

        notes.forEach((n) => {
          const osc = ctx.createOscillator();
          const gain = ctx.createGain();
          osc.connect(gain);
          gain.connect(ctx.destination);
          osc.type = 'sine';
          osc.frequency.setValueAtTime(n.freq, now + n.time);
          gain.gain.setValueAtTime(0.25, now + n.time);
          gain.gain.exponentialRampToValueAtTime(0.001, now + n.time + n.duration);
          osc.start(now + n.time);
          osc.stop(now + n.time + n.duration);
        });
        break;
      }
    }
  }

  // توليد صوت بوب سريع برمجياً (عند الإرسال أو فقاعة)
  playBubblePop() {
    const ctx = this.getContext();
    if (!ctx) return;

    const osc = ctx.createOscillator();
    const gainNode = ctx.createGain();

    osc.connect(gainNode);
    gainNode.connect(ctx.destination);

    osc.frequency.setValueAtTime(700, ctx.currentTime);
    osc.frequency.exponentialRampToValueAtTime(1100, ctx.currentTime + 0.08);
    osc.type = 'sine';

    gainNode.gain.setValueAtTime(0.3, ctx.currentTime);
    gainNode.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.1);

    osc.start(ctx.currentTime);
    osc.stop(ctx.currentTime + 0.1);
  }

  // صوت نقر خفيف وسريع للأزرار
  playClick() {
    const ctx = this.getContext();
    if (!ctx) return;

    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.connect(gain);
    gain.connect(ctx.destination);

    osc.frequency.setValueAtTime(1200, ctx.currentTime);
    osc.type = 'triangle';

    gain.gain.setValueAtTime(0.12, ctx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.04);

    osc.start(ctx.currentTime);
    osc.stop(ctx.currentTime + 0.04);
  }

  // تغيير الصوت الحالي
  setSoundType(type: string) {
    this.currentSound = type;
  }

  getCurrentSound(): string {
    return this.currentSound;
  }
}

export const audioService = new AudioService();
