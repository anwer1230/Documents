import React, { useEffect } from 'react';
import { ThemeToggle } from './ThemeToggle';
import { SoundSelector } from './SoundSelector';
import { DataSaverSettings } from './DataSaverSettings';
import { TTSSettings } from './TTSSettings';
import { ResetSettings } from './ResetSettings';
import { useSettingsStore } from '../../stores/settingsStore';
import { ArrowLeft, ArrowRight, Settings, Palette, Bell, Wifi, Mic, Sparkles } from 'lucide-react';

interface SettingsScreenProps {
  onBack?: () => void;
}

export const SettingsScreen: React.FC<SettingsScreenProps> = ({ onBack }) => {
  const { loadSettings } = useSettingsStore();

  useEffect(() => {
    loadSettings();
  }, [loadSettings]);

  return (
    <div className="flex-1 flex flex-col h-full overflow-hidden bg-[var(--tg-theme-surface,#17212b)] text-[var(--tg-text-color,#ffffff)] font-sans">
      {/* Top Header Bar */}
      <div className="p-4 bg-[var(--tg-header-bg,#17212b)] border-b border-white/10 flex items-center justify-between shadow-xs shrink-0">
        <div className="flex items-center gap-3">
          {onBack && (
            <button
              type="button"
              onClick={onBack}
              className="p-2 rounded-full hover:bg-white/10 active:bg-white/20 transition-colors cursor-pointer text-gray-200"
              title="رجوع"
            >
              <ArrowRight className="w-5 h-5 rtl:block hidden" />
              <ArrowLeft className="w-5 h-5 rtl:hidden block" />
            </button>
          )}
          <div>
            <h1 className="font-bold text-lg flex items-center gap-2">
              <Settings className="w-5 h-5 text-[#5288c1]" />
              <span>إعدادات Telegram Web</span>
            </h1>
            <p className="text-xs text-gray-400">تخصيص الواجهة، النغمات، وتوفير البيانات والنطق الآلي</p>
          </div>
        </div>

        <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-[#2481cc]/20 text-[#5288c1] text-xs font-semibold">
          <Sparkles className="w-3.5 h-3.5" />
          <span>الواجهة الرسمية</span>
        </div>
      </div>

      {/* Scrollable Settings Sections */}
      <div className="flex-1 overflow-y-auto p-4 sm:p-5 space-y-6 scrollbar-thin">
        {/* Section 1: المظهر والسمات */}
        <section className="space-y-2.5">
          <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-[#5288c1]">
            <Palette className="w-4 h-4" />
            <h2>المظهر والسمات</h2>
          </div>
          <ThemeToggle />
        </section>

        {/* Section 2: نغمات الإشعارات والأصوات */}
        <section className="space-y-2.5">
          <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-pink-400">
            <Bell className="w-4 h-4" />
            <h2>الإشعارات والأصوات</h2>
          </div>
          <SoundSelector />
        </section>

        {/* Section 3: توفير البيانات */}
        <section className="space-y-2.5">
          <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-emerald-400">
            <Wifi className="w-4 h-4" />
            <h2>توفير البيانات</h2>
          </div>
          <DataSaverSettings />
        </section>

        {/* Section 4: الصوت والنطق الآلي */}
        <section className="space-y-2.5">
          <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-purple-400">
            <Mic className="w-4 h-4" />
            <h2>الصوت والنطق الآلي (TTS)</h2>
          </div>
          <TTSSettings />
        </section>

        {/* Section 5: استعادة بيانات العرض التجريبي الأصلية */}
        <section className="pt-2 border-t border-white/10">
          <ResetSettings />
        </section>
      </div>
    </div>
  );
};
