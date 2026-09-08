// services/ttsService.ts

export class TTSService {
  private synth: SpeechSynthesis | null = null;
  private currentUtterance: SpeechSynthesisUtterance | null = null;

  constructor() {
    if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
      this.synth = window.speechSynthesis;
    }
  }

  // نطق رسالة جديدة
  speakMessage(text: string, language: string = 'ar-SA') {
    if (!text) return;
    if (typeof window === 'undefined' || !('speechSynthesis' in window)) return;

    if (!this.synth) {
      this.synth = window.speechSynthesis;
    }

    // إيقاف أي نطق سابق
    this.stop();

    try {
      // إزالة الروابط أو الرموز المزعجة عند النطق
      const cleanText = text
        .replace(/https?:\/\/\S+/g, '')
        .trim();

      if (!cleanText) return;

      const utterance = new SpeechSynthesisUtterance(cleanText);
      utterance.lang = language;
      utterance.rate = 0.95; // سرعة ملائمة وقريبة للطبيعية
      utterance.pitch = 1.0;
      utterance.volume = 1.0;

      // اختيار أفضل صوت لغوي متوفر
      const voices = this.synth.getVoices();
      const langPrefix = language.split('-')[0].toLowerCase();
      const matchingVoice = voices.find((v) => v.lang.toLowerCase().startsWith(langPrefix));
      if (matchingVoice) {
        utterance.voice = matchingVoice;
      }

      this.currentUtterance = utterance;
      this.synth.speak(utterance);
    } catch (error) {
      console.warn('[TTSService] Speech synthesis warning:', error);
    }
  }

  // إيقاف النطق
  stop() {
    if (this.synth && this.synth.speaking) {
      this.synth.cancel();
    }
    this.currentUtterance = null;
  }

  // التحقق من دعم TTS
  isSupported(): boolean {
    return typeof window !== 'undefined' && 'speechSynthesis' in window;
  }
}

export const ttsService = new TTSService();
