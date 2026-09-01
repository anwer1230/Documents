import React, { useState } from 'react';
import {
  ArrowLeft,
  ArrowRight,
  UserPlus,
  Check,
  X,
  CheckCheck,
  Shield,
  Clock,
  Search,
} from 'lucide-react';
import { useTelegram } from '../../context/TelegramContext';
import { memberRequestsController } from '../../core/messenger/MemberRequestsController';
import { MemberJoinRequestItem } from '../../types';

export const MemberRequestsView: React.FC<{ onBack: () => void }> = ({ onBack }) => {
  const { settings, showToast } = useTelegram();
  const isArabic = settings.language === 'ar';
  const BackIcon = isArabic ? ArrowRight : ArrowLeft;

  const [requests, setRequests] = useState<MemberJoinRequestItem[]>(
    memberRequestsController.getPendingRequests()
  );

  const handleApprove = (id: string, name: string) => {
    memberRequestsController.approveRequest(id);
    setRequests(memberRequestsController.getPendingRequests());
    showToast(isArabic ? `تمت الموافقة على انضمام ${name}` : `Approved ${name}'s join request`, '✅');
  };

  const handleDecline = (id: string, name: string) => {
    memberRequestsController.declineRequest(id);
    setRequests(memberRequestsController.getPendingRequests());
    showToast(isArabic ? `تم رفض طلب ${name}` : `Declined ${name}'s join request`, '❌');
  };

  const handleApproveAll = () => {
    const count = memberRequestsController.approveAll();
    setRequests([]);
    showToast(isArabic ? `تمت الموافقة على جميع الطلبات (${count})` : `Approved all ${count} requests`, '🎉');
  };

  return (
    <div className="flex flex-col h-full bg-[#0e1621] text-white">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3.5 bg-[#2481cc] text-white shrink-0 shadow-md">
        <div className="flex items-center gap-3">
          <button
            onClick={onBack}
            className="p-1.5 rounded-full hover:bg-white/15 transition-colors"
          >
            <BackIcon className="w-5 h-5" />
          </button>
          <div className="flex items-center gap-2">
            <UserPlus className="w-5 h-5" />
            <span className="font-bold text-base">
              {isArabic ? 'طلبات الانضمام المعلقة' : 'Pending Member Join Requests'}
            </span>
          </div>
        </div>

        {requests.length > 0 && (
          <button
            onClick={handleApproveAll}
            className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-500 active:bg-emerald-700 text-white rounded-lg text-xs font-bold flex items-center gap-1.5 transition-colors shadow-sm"
          >
            <CheckCheck className="w-3.5 h-3.5" />
            <span>{isArabic ? 'قبول الكل' : 'Accept All'}</span>
          </button>
        )}
      </div>

      <div className="flex-1 overflow-y-auto p-4 space-y-3">
        {requests.length > 0 ? (
          requests.map((req) => (
            <div
              key={req.id}
              className="bg-[#17212b] p-3.5 rounded-2xl border border-white/10 flex items-center justify-between gap-3 shadow-md hover:border-[#5288c1]/40 transition-all"
            >
              <div className="flex items-center gap-3 min-w-0">
                <img
                  src={req.userAvatar}
                  alt=""
                  className="w-11 h-11 rounded-full object-cover border border-white/10 shrink-0"
                />
                <div className="min-w-0">
                  <div className="text-xs font-bold text-white truncate flex items-center gap-1.5">
                    <span>{req.userName}</span>
                    <span className="px-2 py-0.5 bg-[#5288c1]/20 text-[#5288c1] rounded-full text-[10px] font-normal truncate">
                      {req.chatTitle}
                    </span>
                  </div>
                  {req.userBio && (
                    <div className="text-[11px] text-gray-400 truncate mt-0.5">{req.userBio}</div>
                  )}
                  <div className="text-[10px] text-gray-500 flex items-center gap-1 mt-1 font-mono">
                    <Clock className="w-3 h-3" />
                    <span>{req.requestedAt}</span>
                  </div>
                </div>
              </div>

              {/* Actions */}
              <div className="flex items-center gap-2 shrink-0">
                <button
                  onClick={() => handleDecline(req.id, req.userName)}
                  className="p-2 rounded-xl bg-rose-500/15 hover:bg-rose-500/25 active:bg-rose-500/35 text-rose-400 border border-rose-500/30 transition-colors"
                  title={isArabic ? 'رفض الطلب' : 'Decline'}
                >
                  <X className="w-4 h-4" />
                </button>
                <button
                  onClick={() => handleApprove(req.id, req.userName)}
                  className="p-2 rounded-xl bg-emerald-500/15 hover:bg-emerald-500/25 active:bg-emerald-500/35 text-emerald-400 border border-emerald-500/30 transition-colors"
                  title={isArabic ? 'قبول الانضمام' : 'Approve'}
                >
                  <Check className="w-4 h-4" />
                </button>
              </div>
            </div>
          ))
        ) : (
          <div className="text-center py-16 text-gray-400 space-y-3">
            <div className="w-14 h-14 mx-auto rounded-full bg-emerald-500/15 border border-emerald-500/30 flex items-center justify-center text-emerald-400">
              <CheckCheck className="w-7 h-7" />
            </div>
            <p className="text-sm font-bold text-white">
              {isArabic ? 'لا توجد طلبات انضمام معلقة حالياً' : 'No pending join requests at the moment'}
            </p>
            <p className="text-xs text-gray-500">
              {isArabic ? 'ستظهر هنا طلبات الانضمام للقنوات والمجموعات الخاصة' : 'New join requests for private channels will appear here'}
            </p>
          </div>
        )}
      </div>
    </div>
  );
};
