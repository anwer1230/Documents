import React from 'react';
import { useSettingsStore, applyTheme } from '../../stores/settingsStore';
import { Sun, Moon } from 'lucide-react';

export const ThemeToggle: React.FC = () => {
  const { settings, updateSettings } = useSettingsStore();
  const isLight = settings.theme === 'light';

  const toggleTheme = () => {
    const newTheme = isLight ? 'dark' : 'light';
    updateSettings({ theme: newTheme });
  };

  return (
    <div className="flex items-center justify-between p-3.5 bg-black/10 hover:bg-black/15 dark:bg-white/5 dark:hover:bg-white/10 rounded-xl transition-colors">
      <div className="flex items-center gap-3">
        <div className={`w-9 h-9 rounded-full flex items-center justify-center transition-colors ${
          isLight ? 'bg-amber-500/20 text-amber-500' : 'bg-indigo-500/20 text-indigo-400'
        }`}>
          {isLight ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
        </div>
        <div>
          <div className="font-semibold text-sm">
            {isLight ? 'الوضع النهاري (Light Mode)' : 'الوضع الليلي (Dark Mode)'}
          </div>
          <div className="text-xs text-gray-400">
            {isLight ? 'سمة فاتحة مريحة للقراءة' : 'سمة مظلمة لتوفير البطارية وإراحة العين'}
          </div>
        </div>
      </div>

      <button
        type="button"
        onClick={toggleTheme}
        className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${
          !isLight ? 'bg-[#2481cc]' : 'bg-gray-400/50'
        }`}
        role="switch"
        aria-checked={!isLight}
      >
        <span
          className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out ${
            !isLight ? 'translate-x-5' : 'translate-x-0'
          }`}
        />
      </button>
    </div>
  );
};

export { applyTheme };
