import React, { useState } from "react";
import { X, RefreshCw, Plus, Trash2, Check, ArrowRightLeft } from "lucide-react";
import { AutoForwardRule, TelegramDialog } from "../../types";
import { getAutoForwardRules, saveAutoForwardRules } from "../../lib/telegramProTools";

interface AutoForwardModalProps {
  isOpen: boolean;
  onClose: () => void;
  dialogs: TelegramDialog[];
  onRulesChange: (rules: AutoForwardRule[]) => void;
}

export const AutoForwardModal: React.FC<AutoForwardModalProps> = ({
  isOpen,
  onClose,
  dialogs,
  onRulesChange,
}) => {
  const [rules, setRules] = useState<AutoForwardRule[]>(() => getAutoForwardRules());
  const [sourceId, setSourceId] = useState(String(dialogs[0]?.id || ""));
  const [targetId, setTargetId] = useState(String(dialogs[1]?.id || dialogs[0]?.id || ""));
  const [removeQuote, setRemoveQuote] = useState(true);
  const [showAdd, setShowAdd] = useState(false);

  if (!isOpen) return null;

  const handleToggleRule = (id: string | number) => {
    const strId = String(id);
    const updated = rules.map((r) => (String(r.id) === strId ? { ...r, enabled: !(r.enabled ?? r.isEnabled) } : r));
    setRules(updated);
    saveAutoForwardRules(updated);
    onRulesChange(updated);
  };

  const handleRemoveRule = (id: string | number) => {
    const strId = String(id);
    const updated = rules.filter((r) => String(r.id) !== strId);
    setRules(updated);
    saveAutoForwardRules(updated);
    onRulesChange(updated);
  };

  const handleAddRule = (e: React.FormEvent) => {
    e.preventDefault();
    if (!sourceId || !targetId || sourceId === targetId) {
      alert("يرجى اختيار قناتين مختلفتين");
      return;
    }

    const source = dialogs.find((d) => String(d.id) === String(sourceId));
    const target = dialogs.find((d) => String(d.id) === String(targetId));

    const newRule: AutoForwardRule = {
      id: `forward_rule_${Date.now()}`,
      sourceChatId: String(sourceId),
      sourceChatTitle: source?.title || source?.name || "Source",
      targetChatId: String(targetId),
      targetChatTitle: target?.title || target?.name || "Target",
      removeQuote,
      enabled: true,
      isEnabled: true,
    };

    const updated = [...rules, newRule];
    setRules(updated);
    saveAutoForwardRules(updated);
    onRulesChange(updated);

    setShowAdd(false);
  };

  return (
    <div
      id="auto-forward-modal-overlay"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-fade-in"
      dir="rtl"
    >
      <div
        id="auto-forward-modal"
        className="w-full max-w-md bg-white dark:bg-slate-900 rounded-3xl shadow-2xl border border-slate-200 dark:border-slate-800 overflow-hidden flex flex-col p-6 select-none max-h-[90vh]"
      >
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-slate-100 dark:border-slate-800">
          <div className="flex items-center gap-2.5 text-sky-500 font-bold text-base">
            <RefreshCw className="w-5 h-5" />
            <div>
              <h3 className="text-slate-800 dark:text-white">نقل وتكرار المنشورات تلقائياً (Auto-Forwarder)</h3>
              <p className="text-[11px] text-slate-400 font-normal">نسخ المنشورات من القنوات المصدر إلى قنواتك</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full flex items-center justify-center text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Rules list */}
        <div className="mt-4 space-y-2.5 overflow-y-auto flex-1 max-h-56 p-1">
          {rules.length === 0 ? (
            <div className="text-center py-6 text-slate-400 text-xs">
              لا توجد مسارات نقل مضافة حالياً
            </div>
          ) : (
            rules.map((rule) => (
              <div
                key={rule.id}
                className="p-3 rounded-2xl bg-slate-50 dark:bg-slate-800/40 border border-slate-200/80 dark:border-slate-800 flex items-center justify-between"
              >
                <div className="min-w-0 flex-1 pr-1 text-xs">
                  <div className="flex items-center gap-1.5 font-bold text-slate-800 dark:text-white truncate">
                    <span className="text-sky-500">{rule.sourceChatTitle}</span>
                    <span>➔</span>
                    <span className="text-emerald-500">{rule.targetChatTitle}</span>
                  </div>
                  <span className="text-[10px] text-slate-400 block mt-0.5">
                    {rule.removeQuote ? "إزالة اقتباس المصدر" : "مع إظهار المصدر"}
                  </span>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={() => handleToggleRule(rule.id)}
                    className={`w-9 h-5 rounded-full transition-colors relative flex items-center p-0.5 shrink-0 ${
                      (rule.enabled ?? rule.isEnabled) ? "bg-sky-500" : "bg-slate-300 dark:bg-slate-700"
                    }`}
                  >
                    <span
                      className={`w-4 h-4 rounded-full bg-white shadow-sm transform transition-transform ${
                        (rule.enabled ?? rule.isEnabled) ? "-translate-x-4" : "translate-x-0"
                      }`}
                    />
                  </button>
                  <button
                    onClick={() => handleRemoveRule(rule.id)}
                    className="w-7 h-7 rounded-full text-slate-400 hover:text-red-500 flex items-center justify-center transition-colors"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            ))
          )}
        </div>

        {/* Add Form */}
        {showAdd ? (
          <form onSubmit={handleAddRule} className="mt-4 p-3.5 bg-slate-50 dark:bg-slate-800/60 rounded-2xl border border-slate-200 dark:border-slate-700 space-y-2.5 text-xs">
            <div>
              <label className="font-semibold text-slate-600 dark:text-slate-400 block mb-1">القناة المصدر (المستلم منها):</label>
              <select
                value={sourceId}
                onChange={(e) => setSourceId(e.target.value)}
                className="w-full p-2 bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-700 text-xs focus:outline-none"
              >
                {dialogs.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.title || d.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="font-semibold text-slate-600 dark:text-slate-400 block mb-1">القناة الهدف (المرسل إليها):</label>
              <select
                value={targetId}
                onChange={(e) => setTargetId(e.target.value)}
                className="w-full p-2 bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-700 text-xs focus:outline-none"
              >
                {dialogs.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.title || d.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="flex items-center justify-between p-2 bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-700">
              <span className="text-[11px] font-semibold text-slate-700 dark:text-slate-300">
                حذف اسم القناة الأصلية (بدون اقتباس)
              </span>
              <input
                type="checkbox"
                checked={removeQuote}
                onChange={(e) => setRemoveQuote(e.target.checked)}
                className="w-4 h-4 text-sky-500 rounded"
              />
            </div>

            <div className="flex gap-2 pt-1">
              <button
                type="submit"
                className="flex-1 py-2 bg-sky-500 hover:bg-sky-600 text-white font-bold rounded-xl"
              >
                تفعيل المسار
              </button>
              <button
                type="button"
                onClick={() => setShowAdd(false)}
                className="px-3 py-2 bg-slate-200 dark:bg-slate-700 text-slate-600 dark:text-slate-300 font-bold rounded-xl"
              >
                إلغاء
              </button>
            </div>
          </form>
        ) : (
          <button
            onClick={() => setShowAdd(true)}
            className="mt-4 py-2.5 bg-sky-500 hover:bg-sky-600 active:scale-98 text-white font-bold text-xs rounded-2xl shadow-md transition-all flex items-center justify-center gap-2"
          >
            <Plus className="w-4 h-4" />
            <span>إضافة مسار نقل آلي جديد</span>
          </button>
        )}
      </div>
    </div>
  );
};
