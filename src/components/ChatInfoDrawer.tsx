import React, { useState } from 'react';
import {
  X,
  Bell,
  BellOff,
  Image,
  FileText,
  Music,
  Link,
  Users,
  Bookmark,
  Trash2,
  LogOut,
  Shield,
  Copy,
  Check,
  Share2,
  Flag,
  Search,
  Crown,
  ShieldCheck,
  MoreVertical,
  ExternalLink,
} from 'lucide-react';
import { TelegramChat, TelegramMessage } from '../types';

interface ChatInfoDrawerProps {
  chat: TelegramChat;
  messages: TelegramMessage[];
  isOpen: boolean;
  onClose: () => void;
  onToggleMute: (chatId: string) => void;
  onOpenClearHistory: () => void;
  onOpenLeaveGroup: () => void;
  onOpenShareLink: () => void;
  onOpenReportChat: () => void;
  onOpenMediaViewer: (url: string, title?: string) => void;
  onSelectMember?: (member: { id: string; name: string }) => void;
  onToast: (msg: string, type?: 'success' | 'info' | 'error') => void;
  lang: 'ar' | 'en';
  isDark: boolean;
}

export const ChatInfoDrawer: React.FC<ChatInfoDrawerProps> = ({
  chat,
  messages,
  isOpen,
  onClose,
  onToggleMute,
  onOpenClearHistory,
  onOpenLeaveGroup,
  onOpenShareLink,
  onOpenReportChat,
  onOpenMediaViewer,
  onSelectMember,
  onToast,
  lang,
  isDark,
}) => {
  const isAr = lang === 'ar';
  const [activeTab, setActiveTab] = useState<'media' | 'files' | 'members' | 'links'>('media');
  const [memberSearch, setMemberSearch] = useState('');
  const [isCopiedLink, setIsCopiedLink] = useState(false);

  if (!isOpen) return null;

  // Filter media, files, and links from chat messages
  const mediaMessages = messages.filter((m) => m.media?.type === 'photo' || m.media?.type === 'video');
  const fileMessages = messages.filter((m) => m.media?.type === 'document');
  const linkMessages = messages.filter(
    (m) => m.text && (m.text.includes('http://') || m.text.includes('https://') || m.text.includes('t.me/'))
  );

  const isGroupOrChannel = chat.type === 'group' || chat.type === 'supergroup' || chat.type === 'channel';

  const inviteLink =
    chat.inviteLink ||
    (chat.username ? `https://t.me/${chat.username}` : `https://t.me/+join_${chat.id}`);

  const handleCopyLink = () => {
    try {
      navigator.clipboard.writeText(inviteLink);
      setIsCopiedLink(true);
      onToast(isAr ? 'تم نسخ الرابط إلى الحافظة' : 'Link copied to clipboard', 'success');
      setTimeout(() => setIsCopiedLink(false), 2200);
    } catch {
      onToast(inviteLink, 'info');
    }
  };

  // Filter members
  const membersList = chat.members || [
    { id: 'm_1', name: 'خالد عبدالله', role: 'owner' as const, isOnline: true, avatarColor: '#e17076' },
    { id: 'm_2', name: 'سارة خالد', role: 'admin' as const, isOnline: true, avatarColor: '#65aadd' },
    { id: 'm_3', name: 'فهد المهندس', role: 'admin' as const, isOnline: false, avatarColor: '#7bc862' },
    { id: 'm_4', name: 'عمر القحطاني', role: 'member' as const, isOnline: true, avatarColor: '#faa774' },
    { id: 'm_5', name: 'نورة السالم', role: 'member' as const, isOnline: false, avatarColor: '#ee7aae' },
  ];

  const displayedMembers = memberSearch.trim()
    ? membersList.filter((m) => m.name.toLowerCase().includes(memberSearch.toLowerCase()))
    : membersList;

  return (
    <aside
      className={`w-84 border-s flex flex-col h-full z-20 shrink-0 select-none transition-all ${
        isDark ? 'bg-[#17212b] border-[#0e1621] text-white' : 'bg-white border-gray-200 text-gray-900'
      }`}
    >
      {/* Top Header */}
      <div className="h-16 px-4 flex items-center justify-between border-b border-gray-700/20 shrink-0">
        <h3 className="font-bold text-sm">
          {chat.type === 'channel'
            ? isAr
              ? 'معلومات القناة'
              : 'Channel Info'
            : isGroupOrChannel
            ? isAr
              ? 'معلومات المجموعة'
              : 'Group Info'
            : isAr
            ? 'الملف الشخصي'
            : 'User Info'}
        </h3>
        <button
          onClick={onClose}
          className={`p-2 rounded-full transition ${
            isDark ? 'hover:bg-[#232e3c] text-gray-400' : 'hover:bg-gray-100 text-gray-600'
          }`}
          title={isAr ? 'إغلاق' : 'Close'}
        >
          <X className="w-5 h-5" />
        </button>
      </div>

      {/* Main Scroll Content */}
      <div className="flex-1 overflow-y-auto p-4 space-y-5">
        {/* Profile Avatar & Title */}
        <div className="flex flex-col items-center text-center">
          {chat.avatarUrl ? (
            <img
              src={chat.avatarUrl}
              alt={chat.title}
              referrerPolicy="no-referrer"
              className="w-24 h-24 rounded-full object-cover shadow-lg mb-3 ring-2 ring-[#3390ec]/20"
            />
          ) : (
            <div
              className="w-24 h-24 rounded-full flex items-center justify-center text-white font-bold text-3xl shadow-lg mb-3"
              style={{ backgroundColor: chat.avatarColor || '#3390ec' }}
            >
              {chat.type === 'saved' ? <Bookmark className="w-10 h-10 fill-white" /> : chat.title.slice(0, 2)}
            </div>
          )}

          <h2 className="text-base font-bold leading-snug">{chat.title}</h2>
          {chat.username && (
            <p className="text-xs text-[#3390ec] font-mono mt-0.5">@{chat.username}</p>
          )}

          <p className="text-xs text-gray-400 mt-1">
            {chat.type === 'saved'
              ? isAr
                ? 'مساحتك السحابية الخاصة'
                : 'Your cloud storage'
              : chat.type === 'channel'
              ? isAr
                ? `${chat.membersCount?.toLocaleString() || '48,920'} مشترك`
                : `${chat.membersCount?.toLocaleString() || '48,920'} subscribers`
              : chat.type === 'supergroup' || chat.type === 'group'
              ? isAr
                ? `${chat.membersCount?.toLocaleString() || '1,420'} عضو، 3 متصلين الآن`
                : `${chat.membersCount?.toLocaleString() || '1,420'} members, 3 online`
              : chat.isOnline
              ? isAr
                ? 'متصل الآن'
                : 'online'
              : isAr
              ? 'آخر ظهور مؤخراً'
              : 'last seen recently'}
          </p>
        </div>

        {/* Description / Bio */}
        {chat.description && (
          <div
            className={`p-3 rounded-2xl border text-xs leading-relaxed ${
              isDark ? 'bg-[#242f3d]/50 border-[#2f3f50] text-gray-300' : 'bg-gray-50 border-gray-200 text-gray-700'
            }`}
          >
            <span className="font-bold block text-gray-400 mb-1 text-[11px]">
              {isAr ? 'الوصف' : 'Description'}
            </span>
            <p className="break-words">{chat.description}</p>
          </div>
        )}

        {/* Invite Link Card (Official Telegram Web K component) */}
        {isGroupOrChannel && (
          <div
            className={`p-3 rounded-2xl border ${
              isDark ? 'bg-[#242f3d]/50 border-[#2f3f50]' : 'bg-gray-50 border-gray-200'
            }`}
          >
            <div className="flex items-center justify-between mb-1.5">
              <span className="text-[11px] font-bold text-gray-400">
                {isAr ? 'رابط الدعوة للمجموعة' : 'Invite Link'}
              </span>
              <button
                onClick={onOpenShareLink}
                className="text-xs text-[#3390ec] hover:underline flex items-center gap-1 font-semibold"
              >
                <span>{isAr ? 'مشاركة' : 'Share'}</span>
                <Share2 className="w-3 h-3" />
              </button>
            </div>

            <div className="flex items-center justify-between gap-2">
              <span className="font-mono text-xs text-[#3390ec] truncate select-all">
                {inviteLink}
              </span>
              <button
                onClick={handleCopyLink}
                className={`p-1.5 rounded-lg transition shrink-0 ${
                  isCopiedLink
                    ? 'bg-emerald-500 text-white'
                    : isDark
                    ? 'bg-[#2b394a] hover:bg-[#3390ec] text-gray-300 hover:text-white'
                    : 'bg-gray-200 hover:bg-[#3390ec] text-gray-700 hover:text-white'
                }`}
                title={isAr ? 'نسخ الرابط' : 'Copy link'}
              >
                {isCopiedLink ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
              </button>
            </div>
          </div>
        )}

        {/* Notifications Toggle */}
        <div
          className={`flex items-center justify-between p-3 rounded-2xl border ${
            isDark ? 'bg-[#242f3d]/50 border-[#2f3f50]' : 'bg-gray-50 border-gray-200'
          }`}
        >
          <div className="flex items-center gap-2.5 text-xs">
            {chat.isMuted ? (
              <BellOff className="w-4 h-4 text-amber-400" />
            ) : (
              <Bell className="w-4 h-4 text-[#3390ec]" />
            )}
            <div>
              <span className="font-semibold block">{isAr ? 'الإشعارات' : 'Notifications'}</span>
              <span className="text-[10px] text-gray-400">
                {chat.isMuted ? (isAr ? 'صامت' : 'Muted') : (isAr ? 'مفعلة' : 'Enabled')}
              </span>
            </div>
          </div>

          <button
            onClick={() => onToggleMute(chat.id)}
            className={`w-11 h-6 rounded-full transition-colors relative cursor-pointer ${
              chat.isMuted ? 'bg-gray-600' : 'bg-[#3390ec]'
            }`}
          >
            <span
              className={`w-4 h-4 rounded-full bg-white absolute top-1 transition-transform ${
                chat.isMuted ? 'start-1' : 'start-6'
              }`}
            />
          </button>
        </div>

        {/* Media & Members Tabs Selector */}
        <div>
          <div className="flex border-b border-gray-700/20 text-xs">
            <button
              onClick={() => setActiveTab('media')}
              className={`flex-1 py-2 font-bold border-b-2 transition ${
                activeTab === 'media'
                  ? 'border-[#3390ec] text-[#3390ec]'
                  : 'border-transparent text-gray-400 hover:text-white'
              }`}
            >
              {isAr ? 'الوسائط' : 'Media'} ({mediaMessages.length})
            </button>
            <button
              onClick={() => setActiveTab('files')}
              className={`flex-1 py-2 font-bold border-b-2 transition ${
                activeTab === 'files'
                  ? 'border-[#3390ec] text-[#3390ec]'
                  : 'border-transparent text-gray-400 hover:text-white'
              }`}
            >
              {isAr ? 'الملفات' : 'Files'} ({fileMessages.length})
            </button>
            {isGroupOrChannel && (
              <button
                onClick={() => setActiveTab('members')}
                className={`flex-1 py-2 font-bold border-b-2 transition ${
                  activeTab === 'members'
                    ? 'border-[#3390ec] text-[#3390ec]'
                    : 'border-transparent text-gray-400 hover:text-white'
                }`}
              >
                {isAr ? 'الأعضاء' : 'Members'} ({displayedMembers.length})
              </button>
            )}
          </div>

          {/* Tab 1: Photos / Media */}
          {activeTab === 'media' && (
            <div className="pt-3">
              {mediaMessages.length === 0 ? (
                <div className="text-center py-6 text-xs text-gray-500">
                  {isAr ? 'لا توجد وسائط بعد' : 'No media yet'}
                </div>
              ) : (
                <div className="grid grid-cols-3 gap-2">
                  {mediaMessages.map((m) => (
                    <img
                      key={m.id}
                      src={m.media?.url}
                      alt="Media preview"
                      referrerPolicy="no-referrer"
                      onClick={() => onOpenMediaViewer(m.media!.url!, m.media?.title)}
                      className="w-full h-20 object-cover rounded-xl cursor-pointer hover:opacity-85 transition shadow-sm"
                    />
                  ))}
                </div>
              )}
            </div>
          )}

          {/* Tab 2: Files */}
          {activeTab === 'files' && (
            <div className="pt-3 space-y-2">
              {fileMessages.length === 0 ? (
                <div className="text-center py-6 text-xs text-gray-500">
                  {isAr ? 'لا توجد ملفات بعد' : 'No files yet'}
                </div>
              ) : (
                fileMessages.map((m) => (
                  <div
                    key={m.id}
                    className="flex items-center gap-2.5 p-2 rounded-xl bg-black/10 text-xs"
                  >
                    <FileText className="w-4 h-4 text-emerald-400 shrink-0" />
                    <span className="truncate flex-1 font-mono">{m.media?.fileName}</span>
                    <span className="text-[10px] text-gray-400 shrink-0">{m.media?.fileSize}</span>
                  </div>
                ))
              )}
            </div>
          )}

          {/* Tab 3: Members List */}
          {activeTab === 'members' && isGroupOrChannel && (
            <div className="pt-3 space-y-2">
              {/* Search members */}
              <div className="flex items-center gap-2 px-2.5 py-1.5 rounded-xl bg-black/10 text-xs mb-2">
                <Search className="w-3.5 h-3.5 text-gray-400 shrink-0" />
                <input
                  type="text"
                  value={memberSearch}
                  onChange={(e) => setMemberSearch(e.target.value)}
                  placeholder={isAr ? 'بحث في الأعضاء...' : 'Search members...'}
                  className="bg-transparent focus:outline-none w-full text-xs"
                />
              </div>

              {/* Members items */}
              <div className="space-y-1">
                {displayedMembers.map((member) => (
                  <div
                    key={member.id}
                    onClick={() => onSelectMember && onSelectMember(member)}
                    className={`flex items-center justify-between p-2 rounded-xl transition cursor-pointer ${
                      isDark ? 'hover:bg-[#232e3c]' : 'hover:bg-gray-100'
                    }`}
                  >
                    <div className="flex items-center gap-2.5 min-w-0">
                      <div className="relative">
                        <div
                          className="w-8 h-8 rounded-full flex items-center justify-center text-white text-xs font-bold"
                          style={{ backgroundColor: member.avatarColor || '#3390ec' }}
                        >
                          {member.name.slice(0, 2)}
                        </div>
                        {member.isOnline && (
                          <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 border-2 border-[#17212b] absolute bottom-0 end-0" />
                        )}
                      </div>
                      <div className="min-w-0">
                        <p className="text-xs font-semibold truncate">{member.name}</p>
                        <span className="text-[10px] text-gray-400 block">
                          {member.isOnline
                            ? isAr
                              ? 'متصل'
                              : 'online'
                            : isAr
                            ? 'آخر ظهور مؤخراً'
                            : 'offline'}
                        </span>
                      </div>
                    </div>

                    {/* Role Badge */}
                    <div>
                      {member.role === 'owner' && (
                        <span className="inline-flex items-center gap-1 text-[10px] font-bold text-amber-400 bg-amber-400/10 px-2 py-0.5 rounded-full">
                          <Crown className="w-3 h-3" />
                          <span>{isAr ? 'المنشئ' : 'Owner'}</span>
                        </span>
                      )}
                      {member.role === 'admin' && (
                        <span className="inline-flex items-center gap-1 text-[10px] font-bold text-[#3390ec] bg-[#3390ec]/10 px-2 py-0.5 rounded-full">
                          <ShieldCheck className="w-3 h-3" />
                          <span>{isAr ? 'مشرف' : 'Admin'}</span>
                        </span>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Telegram Web K Bottom Actions List */}
        <div className="pt-4 border-t border-gray-700/20 space-y-1">
          {/* Share Link */}
          {isGroupOrChannel && (
            <button
              onClick={onOpenShareLink}
              className={`w-full py-2.5 px-3 rounded-xl text-xs font-semibold flex items-center gap-3 transition ${
                isDark ? 'hover:bg-[#232e3c] text-gray-300' : 'hover:bg-gray-100 text-gray-700'
              }`}
            >
              <Share2 className="w-4 h-4 text-[#3390ec]" />
              <span>{isAr ? 'مشاركة رابط المجموعة' : 'Share Group Link'}</span>
            </button>
          )}

          {/* Clear History */}
          <button
            onClick={onOpenClearHistory}
            className={`w-full py-2.5 px-3 rounded-xl text-xs font-semibold flex items-center gap-3 transition ${
              isDark ? 'hover:bg-[#232e3c] text-gray-300' : 'hover:bg-gray-100 text-gray-700'
            }`}
          >
            <Trash2 className="w-4 h-4 text-gray-400" />
            <span>{isAr ? 'مسح سجل المحادثة' : 'Clear History'}</span>
          </button>

          {/* Report Chat */}
          {chat.type !== 'saved' && (
            <button
              onClick={onOpenReportChat}
              className={`w-full py-2.5 px-3 rounded-xl text-xs font-semibold flex items-center gap-3 transition ${
                isDark ? 'hover:bg-[#232e3c] text-gray-300' : 'hover:bg-gray-100 text-gray-700'
              }`}
            >
              <Flag className="w-4 h-4 text-amber-400" />
              <span>{isAr ? 'الإبلاغ عن المحادثة' : 'Report Chat'}</span>
            </button>
          )}

          {/* Leave Group / Delete and Exit */}
          {chat.type !== 'saved' && (
            <button
              onClick={onOpenLeaveGroup}
              className="w-full py-2.5 px-3 rounded-xl text-xs font-semibold text-red-500 hover:bg-red-500/10 flex items-center gap-3 transition"
            >
              <LogOut className="w-4 h-4 text-red-500" />
              <span>
                {chat.type === 'channel'
                  ? isAr
                    ? 'مغادرة القناة'
                    : 'Leave Channel'
                  : isGroupOrChannel
                  ? isAr
                    ? 'مغادرة المجموعة'
                    : 'Leave Group'
                  : isAr
                  ? 'حذف المحادثة'
                  : 'Delete Chat'}
              </span>
            </button>
          )}
        </div>
      </div>
    </aside>
  );
};
