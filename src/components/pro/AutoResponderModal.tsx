import React, { useState } from "react";
import { X, Bot, Plus, Trash2, Check, Sparkles, MessageSquare } from "lucide-react";
import { AutoResponderRule } from "../../types";
import { getAutoResponderRules, saveAutoResponderRules } from "../../lib/telegramProTools";

interface AutoResponderModalProps {
  isOpen: boolean;
  onClose: () => void;
  onRulesChange: (rules: AutoResponderRule[]) => void;
}

export const AutoResponderModal: React.FC<AutoResponderModalProps> = ({
  isOpen,
  onClose,
  onRulesChange,
}) => {
  const [rules, setRules] = useState<AutoResponderRule[]>(() => getAutoResponderRules());
  const [name, setName] = useState("");
  const [keyword, setKeyword] = useState("");
  const [response, setResponse] = useState("");
  const [showAdd, setShowAdd] = useState(false);

  if (!isOpen) return null;

  const handleToggleRule = (id: string) => {
    const updated = rules.map((r) => (r.id === id ? { ...r, enabled: !r.enabled } : r));
    setRules(updated);
    saveAutoResponderRules(updated);
    onRulesChange(updated);
  };

  const handleRemoveRule = (id: string) => {
    const updated = rules.filter((r) => r.id !== id);
    setRules(updated);
    saveAutoResponderRules(updated);
    onRulesChange(updated);
  };

  const handleAddRule = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !response.trim()) return;

    const newRule: AutoResponderRule = {
      id: `rule_${Date.now()}`,
      name: name.trim(),
      triggerKeyword: keyword.trim() || "*",
      responseText: response.trim(),
      enabled: true,
      onlyPrivate: true,
    };

    const updated = [...rules, newRule];
    setRules(updated);
    saveAutoResponderRules(updated);
    onRulesChange(updated);

    setName("");
    setKeyword("");
    setResponse("");
    setShowAdd(false);
  };

  return (
    <div
      id="auto-responder-modal-overlay"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-fade-in"
      dir="rtl"
    >
      <div
        id="auto-responder-modal"
        className="w-full max-w-md bg-white dark:bg-slate-900 rounded-3xl shadow-2xl border border-slate-200 dark:border-slate-800 overflow-hidden flex flex-col p-6 select-none max-h-[90vh]"
      >
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-slate-100 dark:border-slate-800">
          <div className="flex items-center gap-2.5 text-emerald-500 font-bold text-base">
            <Bot className="w-5 h-5" />
            <div>
              <h3 className="text-slate-800 dark:text-white">الرد التلقائي الذكي (Auto-Responder)</h3>
              <p className="text-[11px] text-slate-400 font-normal">ردود آلية على الرسائل والمحادثات الخاصة</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full flex items-center justify-center text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* List of rules */}
        <div className="mt-4 space-y-2.5 overflow-y-auto flex-1 max-h-56 p-1">
          {rules.length === 0 ? (
            <div className="text-center py-6 text-slate-400 text-xs">
              لا توجد قواعد رد تلقائي حالياً
            </div>
          ) : (
            rules.map((rule) => (
              <div
                key={rule.id}
                className="p-3 rounded-2xl bg-slate-50 dark:bg-slate-800/40 border border-slate-200/80 dark:border-slate-800 flex items-center justify-between"
              >
                <div className="min-w-0 flex-1 pr-1">
                  <div className="flex items-center gap-2 font-bold text-xs text-slate-800 dark:text-white">
                    <span>{rule.name}</span>
                    <span className="text-[10px] px-1.5 py-0.5 rounded-full bg-emerald-100 dark:bg-emerald-950 text-emerald-600 dark:text-emerald-400 font-semibold">
                      الكلمة: {rule.triggerKeyword === "*" ? "جميع الرسائل" : rule.triggerKeyword}
                    </span>
                  </div>
                  <p className="text-[11px] text-slate-600 dark:text-slate-300 truncate mt-1">
                    "{rule.responseText}"
                  </p>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={() => handleToggleRule(rule.id)}
                    className={`w-9 h-5 rounded-full transition-colors relative flex items-center p-0.5 shrink-0 ${
                      rule.enabled ? "bg-emerald-500" : "bg-slate-300 dark:bg-slate-700"
                    }`}
                  >
                    <span
                      className={`w-4 h-4 rounded-full bg-white shadow-sm transform transition-transform ${
                        rule.enabled ? "-translate-x-4" : "translate-x-0"
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
              <label className="font-semibold text-slate-600 dark:text-slate-400 block mb-1">اسم القاعدة:</label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="مثال: رد خارج أوقات العمل"
                className="w-full p-2 bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-700 text-xs focus:outline-none"
                required
              />
            </div>
            <div>
              <label className="font-semibold text-slate-600 dark:text-slate-400 block mb-1">الكلمة المفتاحية (اترك فارغاً لجميع الرسائل):</label>
              <input
                type="text"
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                placeholder="مثال: مرحباً أو *"
                className="w-full p-2 bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-700 text-xs focus:outline-none"
              />
            </div>
            <div>
              <label className="font-semibold text-slate-600 dark:text-slate-400 block mb-1">نص الرد التلقائي:</label>
              <textarea
                value={response}
                onChange={(e) => setResponse(e.target.value)}
                placeholder="اكتب رسالة الرد الآلي هنا..."
                className="w-full p-2 bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-700 text-xs focus:outline-none resize-none h-16"
                required
              />
            </div>
            <div className="flex gap-2 pt-1">
              <button
                type="submit"
                className="flex-1 py-2 bg-emerald-500 hover:bg-emerald-600 text-white font-bold rounded-xl"
              >
                إضافة القاعدة
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
            className="mt-4 py-2.5 bg-emerald-500 hover:bg-emerald-600 active:scale-98 text-white font-bold text-xs rounded-2xl shadow-md transition-all flex items-center justify-center gap-2"
          >
            <Plus className="w-4 h-4" />
            <span>إضافة قاعدة رد تلقائي جديدة</span>
          </button>
        )}
      </div>
    </div>
  );
};
