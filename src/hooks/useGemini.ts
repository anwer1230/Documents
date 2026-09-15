import { useState, useCallback, useEffect, useRef } from 'react';
import {
  geminiApi,
  GeminiGenerateOptions,
  GeminiGenerateResponse,
  GeminiSummarizeOptions,
  GeminiSummarizeResponse,
  GeminiStatusResponse,
} from '../services/api';

export interface UseGeminiOptions {
  /**
   * Default Gemini model to use if not overridden in calls.
   * Defaults to 'gemini-3.8-flash'.
   */
  defaultModel?: string;
  /** Optional persistent system prompt/persona for generation calls */
  systemInstruction?: string;
  /** Whether to automatically query server for Gemini configuration status on mount */
  autoCheckStatus?: boolean;
  /** Callback triggered on successful AI text generation or summarization */
  onSuccess?: (text: string) => void;
  /** Callback triggered when a Gemini API call encounters an error */
  onError?: (error: Error) => void;
}

export interface UseGeminiReturn {
  /** True if any Gemini request is currently in-flight */
  loading: boolean;
  /** Error message from the most recent failed request, or null */
  error: string | null;
  /** Text output of the most recent generation or summary */
  data: string | null;
  /** Full response metadata object from the last text generation request */
  lastResponse: GeminiGenerateResponse | null;
  /** Summary response metadata from the last chat summarization request */
  lastSummary: GeminiSummarizeResponse | null;
  /** Boolean indicating whether GEMINI_API_KEY is configured on the backend, or null if unverified */
  isConfigured: boolean | null;
  /** List of supported Gemini models retrieved from the backend status check */
  availableModels: string[];

  /**
   * Generates text or reasoning from a prompt using Gemini (defaults to 'gemini-3.8-flash').
   */
  generateText: (
    prompt: string,
    options?: Partial<Omit<GeminiGenerateOptions, 'prompt'>>
  ) => Promise<string>;

  /**
   * Summarizes a Telegram conversation thread (up to the last 100 messages).
   */
  summarizeChat: (options: GeminiSummarizeOptions) => Promise<GeminiSummarizeResponse>;

  /**
   * Translates text into the target language using Gemini.
   */
  translateText: (
    text: string,
    targetLanguage?: string,
    sourceLanguage?: string
  ) => Promise<string>;

  /**
   * Suggests quick contextual smart replies for a chat message.
   */
  suggestReplies: (
    lastMessage: string,
    chatContext?: string,
    language?: 'ar' | 'en'
  ) => Promise<string[]>;

  /**
   * Checks the server-side configuration status and model availability of Gemini.
   */
  checkStatus: () => Promise<GeminiStatusResponse>;

  /**
   * Resets local loading, error, and data states.
   */
  reset: () => void;
}

/**
 * Custom React hook for seamless interactions with Gemini AI via the backend API service layer.
 */
export function useGemini(options: UseGeminiOptions = {}): UseGeminiReturn {
  const {
    defaultModel = 'gemini-3.8-flash',
    systemInstruction,
    autoCheckStatus = false,
    onSuccess,
    onError,
  } = options;

  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [data, setData] = useState<string | null>(null);
  const [lastResponse, setLastResponse] = useState<GeminiGenerateResponse | null>(null);
  const [lastSummary, setLastSummary] = useState<GeminiSummarizeResponse | null>(null);
  const [isConfigured, setIsConfigured] = useState<boolean | null>(null);
  const [availableModels, setAvailableModels] = useState<string[]>([
    'gemini-3.8-flash',
    'gemini-3.1-flash-lite',
    'gemini-3.1-pro-preview',
  ]);

  const isMountedRef = useRef<boolean>(true);

  useEffect(() => {
    isMountedRef.current = true;
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  const reset = useCallback(() => {
    setLoading(false);
    setError(null);
    setData(null);
    setLastResponse(null);
    setLastSummary(null);
  }, []);

  const checkStatus = useCallback(async (): Promise<GeminiStatusResponse> => {
    try {
      const status = await geminiApi.getStatus();
      if (isMountedRef.current) {
        setIsConfigured(Boolean(status.configured || status.hasGeminiApiKey));
        if (Array.isArray(status.availableModels) && status.availableModels.length > 0) {
          setAvailableModels(status.availableModels);
        }
      }
      return status;
    } catch (err: any) {
      if (isMountedRef.current) {
        setIsConfigured(false);
      }
      throw err;
    }
  }, []);

  useEffect(() => {
    if (autoCheckStatus) {
      checkStatus().catch((err) => {
        console.warn('[useGemini] Automatic status check failed:', err?.message || err);
      });
    }
  }, [autoCheckStatus, checkStatus]);

  const generateText = useCallback(
    async (
      prompt: string,
      callOptions?: Partial<Omit<GeminiGenerateOptions, 'prompt'>>
    ): Promise<string> => {
      setLoading(true);
      setError(null);

      try {
        const payload: GeminiGenerateOptions = {
          prompt,
          model: callOptions?.model || defaultModel,
          systemInstruction: callOptions?.systemInstruction || systemInstruction,
          temperature: callOptions?.temperature,
          maxOutputTokens: callOptions?.maxOutputTokens,
        };

        const res = await geminiApi.generateContent(payload);

        if (!isMountedRef.current) return res.text;

        setData(res.text);
        setLastResponse(res);
        setLoading(false);

        if (onSuccess) {
          onSuccess(res.text);
        }

        return res.text;
      } catch (err: any) {
        const errorMsg = err?.message || 'Failed to generate content with Gemini AI';
        if (isMountedRef.current) {
          setError(errorMsg);
          setLoading(false);
        }
        if (onError) {
          onError(err instanceof Error ? err : new Error(errorMsg));
        }
        throw err;
      }
    },
    [defaultModel, systemInstruction, onSuccess, onError]
  );

  const summarizeChat = useCallback(
    async (summarizeOptions: GeminiSummarizeOptions): Promise<GeminiSummarizeResponse> => {
      setLoading(true);
      setError(null);

      try {
        const res = await geminiApi.summarizeChat(summarizeOptions);

        if (!isMountedRef.current) return res;

        setData(res.summary);
        setLastSummary(res);
        setLoading(false);

        if (onSuccess) {
          onSuccess(res.summary);
        }

        return res;
      } catch (err: any) {
        const errorMsg = err?.message || 'Failed to summarize chat conversation';
        if (isMountedRef.current) {
          setError(errorMsg);
          setLoading(false);
        }
        if (onError) {
          onError(err instanceof Error ? err : new Error(errorMsg));
        }
        throw err;
      }
    },
    [onSuccess, onError]
  );

  const translateText = useCallback(
    async (
      text: string,
      targetLanguage: string = 'ar',
      sourceLanguage?: string
    ): Promise<string> => {
      setLoading(true);
      setError(null);

      try {
        const translation = await geminiApi.translateText(text, targetLanguage, sourceLanguage);

        if (!isMountedRef.current) return translation;

        setData(translation);
        setLoading(false);

        if (onSuccess) {
          onSuccess(translation);
        }

        return translation;
      } catch (err: any) {
        const errorMsg = err?.message || 'Failed to translate text with Gemini AI';
        if (isMountedRef.current) {
          setError(errorMsg);
          setLoading(false);
        }
        if (onError) {
          onError(err instanceof Error ? err : new Error(errorMsg));
        }
        throw err;
      }
    },
    [onSuccess, onError]
  );

  const suggestReplies = useCallback(
    async (
      lastMessage: string,
      chatContext?: string,
      language: 'ar' | 'en' = 'ar'
    ): Promise<string[]> => {
      setLoading(true);
      setError(null);

      try {
        const replies = await geminiApi.suggestSmartReplies(lastMessage, chatContext, language);

        if (isMountedRef.current) {
          setLoading(false);
        }

        return replies;
      } catch (err: any) {
        const errorMsg = err?.message || 'Failed to suggest smart replies';
        if (isMountedRef.current) {
          setError(errorMsg);
          setLoading(false);
        }
        if (onError) {
          onError(err instanceof Error ? err : new Error(errorMsg));
        }
        throw err;
      }
    },
    [onError]
  );

  return {
    loading,
    error,
    data,
    lastResponse,
    lastSummary,
    isConfigured,
    availableModels,
    generateText,
    summarizeChat,
    translateText,
    suggestReplies,
    checkStatus,
    reset,
  };
}

export default useGemini;
