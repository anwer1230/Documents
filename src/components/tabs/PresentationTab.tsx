import React, { useState } from 'react';
import {
  FileCode,
  Download,
  Sparkles,
  Layers,
  Type,
  Palette,
  FileText,
  Upload,
  CheckCircle2,
  AlertCircle,
  LayoutTemplate,
  Sliders,
  Image as ImageIcon
} from 'lucide-react';

interface PresentationTabProps {
  onGenerate?: (data: any) => Promise<void>;
}

export const PresentationTab: React.FC<PresentationTabProps> = ({ onGenerate }) => {
  const [activeSubTab, setActiveSubTab] = useState<'text' | 'file' | 'html'>('text');
  const [title, setTitle] = useState('');
  const [pType, setPType] = useState('academic');
  const [lang, setLang] = useState('ar');
  const [slidesCount, setSlidesCount] = useState(10);
  const [theme, setTheme] = useState('modern_gradient');
  const [textContent, setTextContent] = useState('');
  const [htmlContent, setHtmlContent] = useState('');
  const [showCoverSettings, setShowCoverSettings] = useState(false);
  const [coverTitle, setCoverTitle] = useState('');
  const [coverSubtitle, setCoverSubtitle] = useState('');
  const [coverAuthor, setCoverAuthor] = useState('');
  const [coverOrg, setCoverOrg] = useState('');
  const [coverDate, setCoverDate] = useState('');
  const [incTables, setIncTables] = useState(true);
  const [incCharts, setIncCharts] = useState(true);
  const [incIcons, setIncIcons] = useState(true);
  const [loading, setLoading] = useState(false);
  const [generatedSuccess, setGeneratedSuccess] = useState(false);

  const handleGenerate = async () => {
    setLoading(true);
    setGeneratedSuccess(false);
    try {
      if (onGenerate) {
        await onGenerate({
          title,
          pType,
          lang,
          slidesCount,
          theme,
          content: activeSubTab === 'text' ? textContent : activeSubTab === 'html' ? htmlContent : '',
          cover: showCoverSettings ? { coverTitle, coverSubtitle, coverAuthor, coverOrg, coverDate } : null,
          incTables,
          incCharts,
          incIcons,
        });
      } else {
        // Direct download simulation / mock API call
        await new Promise((resolve) => setTimeout(resolve, 1500));
      }
      setGeneratedSuccess(true);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-4 text-right" dir="rtl">
      {/* Header Banner */}
      <div className="rounded-xl p-4 text-white bg-gradient-to-r from-purple-700 via-indigo-600 to-blue-600 shadow-md">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg bg-white/20 flex items-center justify-center text-xl shadow-inner">
              <i className="fas fa-file-powerpoint text-white" />
            </div>
            <div>
              <h3 className="font-bold text-base">منشئ العروض التقديمية الاحترافية (PowerPoint)</h3>
              <p className="text-xs text-purple-100 opacity-90">
                إنشاء عروض تقديمية متكاملة وتنسيق شرائح احترافية مدعومة بالذكاء الاصطناعي
              </p>
            </div>
          </div>
          <span className="text-xs font-semibold px-2.5 py-1 rounded-full bg-white/20 border border-white/30 backdrop-blur-sm">
            PowerPoint + Word PPTX
          </span>
        </div>
      </div>

      {/* Sub-tabs: Text / File / HTML */}
      <div className="flex border-b border-zinc-700/60 gap-2 pb-1">
        <button
          onClick={() => setActiveSubTab('text')}
          className={`flex items-center gap-2 px-4 py-2 text-xs font-bold rounded-t-lg transition-colors ${
            activeSubTab === 'text'
              ? 'bg-purple-600 text-white border-b-2 border-purple-400'
              : 'bg-zinc-800/60 text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800'
          }`}
        >
          <i className="fas fa-keyboard text-xs" />
          <span>من نص</span>
        </button>
        <button
          onClick={() => setActiveSubTab('file')}
          className={`flex items-center gap-2 px-4 py-2 text-xs font-bold rounded-t-lg transition-colors ${
            activeSubTab === 'file'
              ? 'bg-purple-600 text-white border-b-2 border-purple-400'
              : 'bg-zinc-800/60 text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800'
          }`}
        >
          <i className="fas fa-file-upload text-xs" />
          <span>من ملف (Word/PDF)</span>
        </button>
        <button
          onClick={() => setActiveSubTab('html')}
          className={`flex items-center gap-2 px-4 py-2 text-xs font-bold rounded-t-lg transition-colors ${
            activeSubTab === 'html'
              ? 'bg-purple-600 text-white border-b-2 border-purple-400'
              : 'bg-zinc-800/60 text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800'
          }`}
        >
          <i className="fas fa-code text-xs" />
          <span>من كود HTML</span>
        </button>
      </div>

      {/* Configuration Grid */}
      <div className="grid grid-cols-1 md:grid-cols-12 gap-3">
        {/* Title */}
        <div className="md:col-span-8">
          <label className="block text-xs font-bold text-zinc-300 mb-1">
            عنوان العرض التقديمي (اختياري):
          </label>
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="اتركه فارغاً ليُستخرج تلقائياً من محتوى النص"
            className="w-full bg-zinc-900 border border-zinc-700 rounded-lg px-3 py-2 text-xs text-zinc-100 placeholder-zinc-500 focus:outline-none focus:border-purple-500"
          />
        </div>

        {/* Presentation Type */}
        <div className="md:col-span-4">
          <label className="block text-xs font-bold text-zinc-300 mb-1">
            نوع العرض:
          </label>
          <select
            value={pType}
            onChange={(e) => setPType(e.target.value)}
            className="w-full bg-zinc-900 border border-zinc-700 rounded-lg px-3 py-2 text-xs text-zinc-100 focus:outline-none focus:border-purple-500"
          >
            <option value="academic">📊 أكاديمي / بحثي</option>
            <option value="business">💼 تجاري / تسويقي</option>
            <option value="educational">🎓 تعليمي / تدريبي</option>
            <option value="report">📰 إخباري / تقرير دوري</option>
            <option value="tech">💻 تقني / تكنولوجي</option>
          </select>
        </div>

        {/* Language */}
        <div className="md:col-span-4">
          <label className="block text-xs font-bold text-zinc-300 mb-1">
            لغة العرض:
          </label>
          <select
            value={lang}
            onChange={(e) => setLang(e.target.value)}
            className="w-full bg-zinc-900 border border-zinc-700 rounded-lg px-3 py-2 text-xs text-zinc-100 focus:outline-none focus:border-purple-500"
          >
            <option value="ar">العربية (RTL)</option>
            <option value="en">الإنجليزية (LTR)</option>
            <option value="both">مزدوج (عربي / إنجليزي)</option>
          </select>
        </div>

        {/* Slides Count */}
        <div className="md:col-span-4">
          <label className="block text-xs font-bold text-zinc-300 mb-1">
            عدد الشرائح المقترح:
          </label>
          <div className="flex items-center gap-2">
            <input
              type="range"
              min="5"
              max="30"
              value={slidesCount}
              onChange={(e) => setSlidesCount(Number(e.target.value))}
              className="w-full accent-purple-500 cursor-pointer"
            />
            <span className="text-xs font-bold bg-purple-900/60 border border-purple-500/30 text-purple-200 px-2.5 py-1 rounded-md min-w-[50px] text-center">
              {slidesCount} شرائح
            </span>
          </div>
        </div>

        {/* Theme Selection */}
        <div className="md:col-span-4">
          <label className="block text-xs font-bold text-zinc-300 mb-1">
            نمط التصميم والألوان:
          </label>
          <select
            value={theme}
            onChange={(e) => setTheme(e.target.value)}
            className="w-full bg-zinc-900 border border-zinc-700 rounded-lg px-3 py-2 text-xs text-zinc-100 focus:outline-none focus:border-purple-500"
          >
            <option value="modern_gradient">✨ عصري متدرج (Modern Gradient)</option>
            <option value="corporate_classic">🏛️ كلاسيكي رسمي (Corporate Classic)</option>
            <option value="dark_luxury">🌙 داكن فاخر (Dark Luxury)</option>
            <option value="creative_vibrant">🎨 إبداعي حيوي (Creative Vibrant)</option>
            <option value="academic_clean">📖 أكاديمي ناصع (Academic Clean)</option>
          </select>
        </div>
      </div>

      {/* Input Area by Sub-Tab */}
      {activeSubTab === 'text' && (
        <div>
          <label className="block text-xs font-bold text-zinc-300 mb-1">
            محتوى النص أو مسودة العرض:
          </label>
          <textarea
            rows={7}
            value={textContent}
            onChange={(e) => setTextContent(e.target.value)}
            placeholder="الصق نص المحتوى، المقال، البحث، أو خطة الدرس هنا..."
            className="w-full bg-zinc-900 border border-zinc-700 rounded-lg p-3 text-xs text-zinc-100 placeholder-zinc-500 focus:outline-none focus:border-purple-500 leading-relaxed font-sans"
          />
        </div>
      )}

      {activeSubTab === 'file' && (
        <div className="p-6 border-2 border-dashed border-zinc-700 rounded-xl bg-zinc-900/50 text-center hover:border-purple-500 transition-colors cursor-pointer">
          <Upload className="w-8 h-8 text-purple-400 mx-auto mb-2" />
          <p className="text-xs font-bold text-zinc-200 mb-1">اسحب وأفلت ملف Word (.docx) أو PDF هنا</p>
          <p className="text-[11px] text-zinc-400 mb-3">أو انقر لاختيار الملف من جهازك</p>
          <label className="inline-block bg-purple-600 hover:bg-purple-500 text-white text-xs font-bold px-4 py-2 rounded-lg cursor-pointer transition-colors shadow">
            اختيار ملف
            <input
              type="file"
              accept=".docx,.doc,.pdf,.txt"
              className="hidden"
              onChange={(e) => {
                const f = e.target.files?.[0];
                if (f) setTitle(f.name.replace(/\.[^/.]+$/, ''));
              }}
            />
          </label>
        </div>
      )}

      {activeSubTab === 'html' && (
        <div>
          <label className="block text-xs font-bold text-zinc-300 mb-1">
            كود HTML المصدري:
          </label>
          <textarea
            rows={7}
            value={htmlContent}
            onChange={(e) => setHtmlContent(e.target.value)}
            placeholder="<div class='slide'><h2>عنوان الشريحة</h2><p>النقاط...</p></div>"
            className="w-full bg-zinc-900 border border-zinc-700 rounded-lg p-3 text-xs text-purple-200 placeholder-zinc-600 focus:outline-none focus:border-purple-500 font-mono text-left"
            dir="ltr"
          />
        </div>
      )}

      {/* Cover Page Settings (Collapsible) */}
      <div className="border border-zinc-700/80 rounded-xl overflow-hidden bg-zinc-900/60">
        <button
          type="button"
          onClick={() => setShowCoverSettings(!showCoverSettings)}
          className="w-full p-3 text-xs font-bold text-zinc-200 flex items-center justify-between bg-zinc-800/60 hover:bg-zinc-800 transition-colors"
        >
          <span className="flex items-center gap-2">
            <i className="fas fa-image text-purple-400" />
            <span>إعدادات صفحة الغلاف (اختياري)</span>
          </span>
          <i className={`fas fa-chevron-${showCoverSettings ? 'up' : 'down'} text-xs text-zinc-400`} />
        </button>

        {showCoverSettings && (
          <div className="p-3 grid grid-cols-1 md:grid-cols-2 gap-2.5 border-t border-zinc-700/60 bg-zinc-900/40">
            <div>
              <label className="block text-[11px] font-semibold text-zinc-400 mb-1">عنوان الغلاف الرئيسي:</label>
              <input
                type="text"
                value={coverTitle}
                onChange={(e) => setCoverTitle(e.target.value)}
                placeholder="عنوان الغلاف..."
                className="w-full bg-zinc-950 border border-zinc-800 rounded px-2.5 py-1.5 text-xs text-zinc-100 focus:outline-none focus:border-purple-500"
              />
            </div>
            <div>
              <label className="block text-[11px] font-semibold text-zinc-400 mb-1">العنوان الفرعي:</label>
              <input
                type="text"
                value={coverSubtitle}
                onChange={(e) => setCoverSubtitle(e.target.value)}
                placeholder="العنوان الفرعي للغلاف..."
                className="w-full bg-zinc-950 border border-zinc-800 rounded px-2.5 py-1.5 text-xs text-zinc-100 focus:outline-none focus:border-purple-500"
              />
            </div>
            <div>
              <label className="block text-[11px] font-semibold text-zinc-400 mb-1">اسم الباحث / المقدّم:</label>
              <input
                type="text"
                value={coverAuthor}
                onChange={(e) => setCoverAuthor(e.target.value)}
                placeholder="د. / أ. ..."
                className="w-full bg-zinc-950 border border-zinc-800 rounded px-2.5 py-1.5 text-xs text-zinc-100 focus:outline-none focus:border-purple-500"
              />
            </div>
            <div>
              <label className="block text-[11px] font-semibold text-zinc-400 mb-1">الجهة / الجامعة:</label>
              <input
                type="text"
                value={coverOrg}
                onChange={(e) => setCoverOrg(e.target.value)}
                placeholder="كلية ... / قسم ..."
                className="w-full bg-zinc-950 border border-zinc-800 rounded px-2.5 py-1.5 text-xs text-zinc-100 focus:outline-none focus:border-purple-500"
              />
            </div>
          </div>
        )}
      </div>

      {/* Options: Tables / Charts / Icons */}
      <div className="flex flex-wrap items-center gap-4 bg-zinc-900/60 p-3 rounded-xl border border-zinc-800">
        <span className="text-xs font-bold text-zinc-300">عناصر التخصيص:</span>
        <label className="flex items-center gap-1.5 text-xs text-zinc-200 cursor-pointer">
          <input
            type="checkbox"
            checked={incTables}
            onChange={(e) => setIncTables(e.target.checked)}
            className="rounded text-purple-600 focus:ring-0"
          />
          <span>تضمين الجداول التلخيصية</span>
        </label>
        <label className="flex items-center gap-1.5 text-xs text-zinc-200 cursor-pointer">
          <input
            type="checkbox"
            checked={incCharts}
            onChange={(e) => setIncCharts(e.target.checked)}
            className="rounded text-purple-600 focus:ring-0"
          />
          <span>الرسوم البيانية والمخططات</span>
        </label>
        <label className="flex items-center gap-1.5 text-xs text-zinc-200 cursor-pointer">
          <input
            type="checkbox"
            checked={incIcons}
            onChange={(e) => setIncIcons(e.target.checked)}
            className="rounded text-purple-600 focus:ring-0"
          />
          <span>أيقونات ورسوم توضيحية</span>
        </label>
      </div>

      {/* Action Button & Status */}
      <div className="pt-2 flex items-center justify-between gap-3">
        {generatedSuccess ? (
          <div className="flex items-center gap-2 text-emerald-400 text-xs font-bold bg-emerald-950/40 border border-emerald-500/30 px-3 py-2 rounded-lg">
            <CheckCircle2 className="w-4 h-4" />
            <span>تم تجهيز ملف العرض التقديمي بنجاح (جاهز للتحميل)!</span>
          </div>
        ) : (
          <span className="text-xs text-zinc-500">العرض يُنتج بصيغة .pptx و .docx متوافقة بالكامل</span>
        )}

        <button
          onClick={handleGenerate}
          disabled={loading || (activeSubTab === 'text' && !textContent.trim())}
          className="flex items-center gap-2 bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-500 hover:to-indigo-500 text-white font-bold px-6 py-2.5 rounded-xl shadow-lg transition-all disabled:opacity-50 disabled:cursor-not-allowed text-xs"
        >
          {loading ? (
            <>
              <i className="fas fa-spinner fa-spin text-xs" />
              <span>جاري صياغة وإنشاء الشرائح...</span>
            </>
          ) : (
            <>
              <i className="fas fa-magic text-xs" />
              <span>إنشاء العرض التقديمي (PowerPoint)</span>
            </>
          )}
        </button>
      </div>
    </div>
  );
};
