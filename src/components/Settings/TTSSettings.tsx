import React from 'react';
import { useSettingsStore } from '../../stores/settingsStore';
import { ttsService } from '../../services/ttsService';
import { Mic, Volume2, Play } from 'lucide-react';

export const TTSSettings: React.FC = () => {
  const { settings, updateSettings } = useSettingsStore();

  const handleTestSpeech = () => {
    ttsService.speakMessage('مرحباً بك في تليجرام، ميزة النطق الآلي تعمل بنجاح!', settings.ttsLanguage || 'ar-SA');
  };

  return (
    <div className="space-y-3">
      <div className="p-3.5 bg-black/10 hover:bg-black/15 dark:bg-white/5 dark:hover:bg-white/10 rounded-xl transition-colors">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-full bg-emerald-500/20 text-emerald-400 flex items-center justify-center">
              <Mic className="w-4 h-4" />
            </div>
            <div>
              <div className="font-semibold text-sm">نطق الرسائل الجديدة (TTS) 🗣️</div>
              <div className="text-xs text-gray-400">قراءة الرسائل الواردة بصوت عربي آلي فور وصولها</div>
            </div>
          </div>

          <button
            type="button"
            onClick={() => updateSettings({ enableTTS: !settings.enableTTS })}
            className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${
              settings.enableTTS ? 'bg-[#2481cc]' : 'bg-gray-400/50'
            }`}
            role="switch"
            aria-checked={settings.enableTTS}
          >
            <span
              className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out ${
                settings.enableTTS ? 'translate-x-5' : 'translate-x-0'
              }`}
            />
          </button>
        </div>

        {/* Test Speech Button & Language indicator */}
        <div className="mt-3 pt-3 border-t border-white/10 flex items-center justify-between">
          <div className="text-xs text-gray-400 flex items-center gap-1.5">
            <Volume2 className="w-3.5 h-3.5 text-[#5288c1]" />
            <span>اللغة المدعومة: العربية (ar-SA)</span>
          </div>

          <button
            type="button"
            onClick={handleTestSpeech}
            className="flex items-center gap-1.5 px-3 py-1 bg-[#2481cc]/20 hover:bg-[#2481cc]/30 text-[#5288c1] rounded-lg text-xs font-semibold transition-colors cursor-pointer"
          >
            <Play className="w-3 h-3 fill-current" />
            <span>تجربة النطق</span>
          </button>
        </div>
      </div>
    </div>
  );
};
