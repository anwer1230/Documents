import React from 'react';
import { useSettingsStore } from '../../stores/settingsStore';
import { audioService } from '../../services/audioService';
import { Bell, Volume2, Play, MousePointerClick, Send } from 'lucide-react';

const SOUND_OPTIONS = [
  { value: 'classic', label: 'كلاسيك تليجرام' },
  { value: 'beep', label: 'نغمة بسيطة' },
  { value: 'chime', label: 'جرس نقي 🔥' },
  { value: 'bubble', label: 'بوب سريع 🔥' },
  { value: 'silent', label: 'صامت 🔥' },
];

export const SoundSelector: React.FC = () => {
  const { settings, updateSettings } = useSettingsStore();

  const handleSoundChange = (value: string) => {
    updateSettings({ notificationSound: value as any });
    audioService.setSoundType(value);
    audioService.playNotification(value);
  };

  const previewCurrentSound = () => {
    audioService.playNotification(settings.notificationSound);
  };

  return (
    <div className="space-y-4">
      {/* Sound Style Selector */}
      <div className="p-3.5 bg-black/10 hover:bg-black/15 dark:bg-white/5 dark:hover:bg-white/10 rounded-xl transition-colors">
        <div className="flex items-center justify-between gap-3 mb-2.5">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-full bg-pink-500/20 text-pink-400 flex items-center justify-center">
              <Bell className="w-4 h-4" />
            </div>
            <div>
              <span className="font-semibold text-sm block">نمط الإشعارات (Ringtone)</span>
              <span className="text-xs text-gray-400">اختر النغمة المشغلة عند ورود رسائل جديدة</span>
            </div>
          </div>

          <button
            type="button"
            onClick={previewCurrentSound}
            title="معاينة الصوت"
            className="flex items-center gap-1.5 px-3 py-1 bg-[#2481cc]/20 hover:bg-[#2481cc]/30 text-[#5288c1] rounded-lg text-xs font-semibold transition-colors cursor-pointer"
          >
            <Play className="w-3 h-3 fill-current" />
            <span>معاينة</span>
          </button>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 mt-2">
          {SOUND_OPTIONS.map((opt) => {
            const isSelected = settings.notificationSound === opt.value;
            return (
              <button
                key={opt.value}
                type="button"
                onClick={() => handleSoundChange(opt.value)}
                className={`flex items-center justify-between p-2.5 rounded-lg border text-xs font-medium transition-all ${
                  isSelected
                    ? 'border-[#2481cc] bg-[#2481cc]/15 text-[#5288c1]'
                    : 'border-white/10 hover:bg-white/5 text-gray-300'
                }`}
              >
                <span>{opt.label}</span>
                {isSelected && <span className="w-2 h-2 rounded-full bg-[#2481cc]" />}
              </button>
            );
          })}
        </div>
      </div>

      {/* Interactive Sound Effects */}
      <div className="p-3.5 bg-black/10 hover:bg-black/15 dark:bg-white/5 dark:hover:bg-white/10 rounded-xl space-y-3 transition-colors">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-full bg-sky-500/20 text-sky-400 flex items-center justify-center">
              <Send className="w-4 h-4" />
            </div>
            <div>
              <div className="font-semibold text-sm">صوت إرسال الرسالة</div>
              <div className="text-xs text-gray-400">تشغيل نغمة بوب خفيفة عند إرسال أي رسالة بنجاح</div>
            </div>
          </div>
          <button
            type="button"
            onClick={() => {
              const next = !settings.enableSendSound;
              updateSettings({ enableSendSound: next });
              if (next) audioService.playBubblePop();
            }}
            className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${
              settings.enableSendSound ? 'bg-[#2481cc]' : 'bg-gray-400/50'
            }`}
            role="switch"
            aria-checked={settings.enableSendSound}
          >
            <span
              className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out ${
                settings.enableSendSound ? 'translate-x-5' : 'translate-x-0'
              }`}
            />
          </button>
        </div>

        <div className="flex items-center justify-between pt-2 border-t border-white/10">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-full bg-amber-500/20 text-amber-400 flex items-center justify-center">
              <MousePointerClick className="w-4 h-4" />
            </div>
            <div>
              <div className="font-semibold text-sm">صوت النقر والتفاعل</div>
              <div className="text-xs text-gray-400">تأثير صوتي خفيف عند التبديل والنقر داخل القوائم</div>
            </div>
          </div>
          <button
            type="button"
            onClick={() => {
              const next = !settings.enableClickSound;
              updateSettings({ enableClickSound: next });
              if (next) audioService.playClick();
            }}
            className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${
              settings.enableClickSound ? 'bg-[#2481cc]' : 'bg-gray-400/50'
            }`}
            role="switch"
            aria-checked={settings.enableClickSound}
          >
            <span
              className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out ${
                settings.enableClickSound ? 'translate-x-5' : 'translate-x-0'
              }`}
            />
          </button>
        </div>
      </div>
    </div>
  );
};
