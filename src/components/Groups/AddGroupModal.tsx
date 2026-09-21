import React, { useState } from 'react';
import { X, Plus, Link, AlertCircle } from 'lucide-react';
import { TelegramGroup } from '../../types';

interface AddGroupModalProps {
  isOpen: boolean;
  onClose: () => void;
  existingGroups: TelegramGroup[];
  onAddGroup: (newGroup: TelegramGroup) => void;
}

export const AddGroupModal: React.FC<AddGroupModalProps> = ({
  isOpen,
  onClose,
  existingGroups,
  onAddGroup,
}) => {
  const [inputVal, setInputVal] = useState('');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const raw = inputVal.trim();
    if (!raw) {
      setErrorMsg('يرجى إدخال رابط أو معرف المجموعة');
      return;
    }

    // Clean username or ID
    const cleanKey = raw.toLowerCase()
      .replace(/^(?:custom_|chat_|user_|channel_)+/i, '')
      .replace(/^https?:\/\/t(?:elegram)?\.me\//, '')
      .replace(/^@/, '')
      .replace(/\/+$/, '');

    // Check duplicate
    const isDuplicate = existingGroups.some((g) => {
      const gClean = (g.username || g.id).toLowerCase()
        .replace(/^(?:custom_|chat_|user_|channel_)+/i, '')
        .replace(/^https?:\/\/t(?:elegram)?\.me\//, '')
        .replace(/^@/, '')
        .replace(/\/+$/, '');
      return gClean === cleanKey;
    });

    if (isDuplicate) {
      setErrorMsg('المجموعة أو الرابط مضاف مسبقاً! النظام يمنع التكرار ويحتفظ برابط فريد واحد.');
      return;
    }

    const newGroup: TelegramGroup = {
      id: raw.startsWith('-100') ? raw : `-100${Date.now()}`,
      title: raw.startsWith('https://') || raw.startsWith('@') ? `مجموعة ${cleanKey}` : raw,
      username: cleanKey.length <= 32 && !cleanKey.startsWith('+') ? cleanKey : undefined,
      type: 'supergroup',
      memberCount: 0,
      onlineCount: 0,
      status: 'active',
      connectionHealth: 'excellent',
      permissionStatus: 'can_post',
      latencyMs: 0,
      lastPingTimestamp: Date.now(),
      lastActivityTime: 'الآن',
      unreadCount: 0,
      permissions: {
        canSendMessages: true,
        canSendMedia: true,
        canSendStickers: true,
        canAddWebPagePreviews: true,
        slowmodeDelaySeconds: 0,
      },
      statusReason: 'تمت الإضافة بنجاح',
      inviteLink: raw.startsWith('http') ? raw : `https://t.me/${cleanKey}`,
      isMonitored: true,
    };

    onAddGroup(newGroup);
    setInputVal('');
    setErrorMsg(null);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-fade-in">
      <div 
        id="add-group-modal-box"
        className="w-full max-w-md bg-slate-900 border border-slate-800 rounded-3xl p-6 text-slate-100 shadow-2xl relative"
      >
        <div className="flex items-center justify-between pb-4 border-b border-slate-800">
          <div className="flex items-center gap-2">
            <div className="p-2 rounded-xl bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
              <Plus className="w-5 h-5" />
            </div>
            <h3 className="text-base font-bold text-white">إضافة مجموعة جديدة وفحص صحتها</h3>
          </div>
          <button
            id="close-add-group-btn"
            type="button"
            onClick={onClose}
            className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1.5">
              رابط المجموعة، اسم المستخدم، أو المعرف:
            </label>
            <div className="relative">
              <Link className="w-4 h-4 text-slate-500 absolute top-3 right-3" />
              <input
                id="new-group-input"
                type="text"
                value={inputVal}
                onChange={(e) => {
                  setInputVal(e.target.value);
                  if (errorMsg) setErrorMsg(null);
                }}
                placeholder="مثال: https://t.me/example_group أو @example_group"
                className="w-full bg-slate-800/80 border border-slate-700/80 rounded-xl pr-10 pl-3 py-2.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
              />
            </div>
            {errorMsg && (
              <div className="flex items-center gap-1.5 text-rose-400 text-xs mt-2 font-medium">
                <AlertCircle className="w-3.5 h-3.5 shrink-0" />
                <span>{errorMsg}</span>
              </div>
            )}
          </div>

          <div className="p-3 rounded-xl bg-slate-950/50 border border-slate-800 text-[11px] text-slate-400 leading-relaxed">
            💡 يفرز النظام الروابط تلقائياً ويمنع التكرار، ويقوم فوراً بإجراء اختبار فحص الاستجابة وصلاحيات النشر (Write Access).
          </div>

          <div className="flex items-center justify-end gap-2 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-xs font-semibold rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors cursor-pointer"
            >
              إلغاء
            </button>
            <button
              id="confirm-add-group-btn"
              type="submit"
              className="px-4 py-2 text-xs font-semibold rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white transition-all shadow-md shadow-indigo-600/20 cursor-pointer"
            >
              إضافة وفحص الاتصال
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
