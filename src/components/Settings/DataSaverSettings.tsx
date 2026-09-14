import React from 'react';
import { useSettingsStore } from '../../stores/settingsStore';
import { WifiOff, DownloadCloud, Image as ImageIcon } from 'lucide-react';

export const DataSaverSettings: React.FC = () => {
  const { settings, updateSettings } = useSettingsStore();

  return (
    <div className="space-y-3">
      {/* وضع توفير البيانات الرئيسي */}
      <div className="p-3.5 bg-black/10 hover:bg-black/15 dark:bg-white/5 dark:hover:bg-white/10 rounded-xl transition-colors">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-full bg-emerald-500/20 text-emerald-400 flex items-center justify-center">
              <WifiOff className="w-4 h-4" />
            </div>
            <div>
              <div className="font-semibold text-sm">وضع توفير البيانات ✅</div>
              <div className="text-xs text-gray-400">يقلل استهلاك البيانات عن طريق إيقاف التنزيل التلقائي</div>
            </div>
          </div>

          <button
            type="button"
            onClick={() => {
              const next = !settings.dataSaver;
              updateSettings({
                dataSaver: next,
                // إذا تم تفعيل توفير البيانات، يُعطّل التحميل التلقائي تلقائياً
                ...(next ? { autoDownloadMedia: false } : {}),
              });
            }}
            className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${
              settings.dataSaver ? 'bg-emerald-500' : 'bg-gray-400/50'
            }`}
            role="switch"
            aria-checked={settings.dataSaver}
          >
            <span
              className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out ${
                settings.dataSaver ? 'translate-x-5' : 'translate-x-0'
              }`}
            />
          </button>
        </div>
      </div>

      {/* تحميل تلقائي للوسائط والصور */}
      <div className={`p-3.5 bg-black/10 hover:bg-black/15 dark:bg-white/5 dark:hover:bg-white/10 rounded-xl transition-colors ${
        settings.dataSaver ? 'opacity-50' : ''
      }`}>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-full bg-sky-500/20 text-sky-400 flex items-center justify-center">
              <DownloadCloud className="w-4 h-4" />
            </div>
            <div>
              <div className="font-semibold text-sm">تحميل تلقائي للوسائط والصور</div>
              <div className="text-xs text-gray-400">
                {settings.dataSaver
                  ? 'معطل إجبارياً بسبب تفعيل وضع توفير البيانات'
                  : 'تنزيل الصور والمقاطع القصيرة فور وصولها'}
              </div>
            </div>
          </div>

          <button
            type="button"
            disabled={settings.dataSaver}
            onClick={() => updateSettings({ autoDownloadMedia: !settings.autoDownloadMedia })}
            className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${
              settings.autoDownloadMedia && !settings.dataSaver ? 'bg-[#2481cc]' : 'bg-gray-400/50'
            }`}
            role="switch"
            aria-checked={settings.autoDownloadMedia && !settings.dataSaver}
          >
            <span
              className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out ${
                settings.autoDownloadMedia && !settings.dataSaver ? 'translate-x-5' : 'translate-x-0'
              }`}
            />
          </button>
        </div>
      </div>

      {/* عرض الصور المصغرة مسبقاً (Thumbnails) */}
      <div className="p-3.5 bg-black/10 hover:bg-black/15 dark:bg-white/5 dark:hover:bg-white/10 rounded-xl transition-colors">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-full bg-purple-500/20 text-purple-400 flex items-center justify-center">
              <ImageIcon className="w-4 h-4" />
            </div>
            <div>
              <div className="font-semibold text-sm">عرض الصور المصغرة مسبقاً (Thumbnails)</div>
              <div className="text-xs text-gray-400">معاينة مضغوطة منخفضة الحجم قبل فتح الوسائط الأصلية</div>
            </div>
          </div>

          <button
            type="button"
            onClick={() => updateSettings({ preloadThumbnails: !settings.preloadThumbnails })}
            className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${
              settings.preloadThumbnails ? 'bg-[#2481cc]' : 'bg-gray-400/50'
            }`}
            role="switch"
            aria-checked={settings.preloadThumbnails}
          >
            <span
              className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out ${
                settings.preloadThumbnails ? 'translate-x-5' : 'translate-x-0'
              }`}
            />
          </button>
        </div>
      </div>
    </div>
  );
};
