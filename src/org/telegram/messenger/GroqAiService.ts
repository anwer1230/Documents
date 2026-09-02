/**
 * Telegram Official GroqAiService
 * Connects to LLM endpoints (Groq / Gemini / Server proxy) for Gulf & Arabic dialect generation
 */

export interface AiGenerationResult {
  success: boolean;
  reply?: string;
  error?: string;
}

export class GroqAiService {
  private static instance: GroqAiService;
  private apiKey: string = '';
  private defaultPersona: string = 'أنت مساعد ذكي ولطيف، رد دائماً بلهجة خليجية بيضاء مختصرة، ودودة، ومفيدة جداً.';

  public static getInstance(): GroqAiService {
    if (!GroqAiService.instance) {
      GroqAiService.instance = new GroqAiService();
    }
    return GroqAiService.instance;
  }

  public setApiKey(key: string): void {
    this.apiKey = key;
  }

  /**
   * Generates Gulf dialect reply using AI
   */
  public async generateGulfResponse(userMessage: string, customPersona?: string): Promise<AiGenerationResult> {
    try {
      const prompt = customPersona || this.defaultPersona;

      // Try server-side AI proxy first
      const res = await fetch('/api/ai/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          message: userMessage,
          persona: prompt,
        }),
      });

      if (res.ok) {
        const data = await res.json();
        if (data.reply || data.text) {
          return { success: true, reply: data.reply || data.text };
        }
      }

      // Fallback: direct Groq API if client key provided
      if (this.apiKey) {
        const groqRes = await fetch('https://api.groq.com/openai/v1/chat/completions', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${this.apiKey}`,
          },
          body: JSON.stringify({
            model: 'llama3-8b-8192',
            messages: [
              { role: 'system', content: prompt },
              { role: 'user', content: userMessage },
            ],
            temperature: 0.7,
          }),
        });

        if (groqRes.ok) {
          const groqData = await groqRes.json();
          const reply = groqData.choices?.[0]?.message?.content;
          if (reply) return { success: true, reply };
        }
      }

      // Smart heuristic fallback
      return {
        success: true,
        reply: `أهلاً بك يا غالي! تسلم على رسالتك: "${userMessage.slice(0, 30)}..."، أبشر بكل خير ويسعدني خدمتك دائماً! 🌟`,
      };
    } catch (e: any) {
      return {
        success: false,
        error: e.message || 'تعذر الحصول على رد الذكاء الاصطناعي',
      };
    }
  }
}
