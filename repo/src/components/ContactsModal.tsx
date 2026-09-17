import React, { useState } from 'react';
import { Users, Search, UserPlus, X, Phone, Check } from 'lucide-react';
import { TelegramUser } from '../types';
import { getSenderColor, getInitials } from '../utils/telegramColors';

interface ContactsModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSelectContact: (contact: TelegramUser) => void;
  lang?: 'ar' | 'en';
  isDark?: boolean;
}

export const ContactsModal: React.FC<ContactsModalProps> = ({
  isOpen,
  onClose,
  onSelectContact,
  lang = 'ar',
  isDark = true,
}) => {
  const isAr = lang === 'ar';
  const [search, setSearch] = useState('');
  const [contacts, setContacts] = useState<TelegramUser[]>([
    {
      id: 'contact_pavel',
      firstName: 'Pavel',
      lastName: 'Durov',
      username: 'durov',
      phone: '+1 888 555 0199',
      isVerified: true,
      status: 'online',
    },
    {
      id: 'contact_ahmed',
      firstName: 'أحمد',
      lastName: 'المنصور',
      username: 'ahmed_mansour',
      phone: '+966 55 123 4567',
      status: 'online',
    },
    {
      id: 'contact_sara',
      firstName: 'سارة',
      lastName: 'خالد',
      username: 'sara_k',
      phone: '+971 50 987 6543',
      status: 'recently',
    },
    {
      id: 'contact_fahad',
      firstName: 'فهد',
      lastName: 'المهندس',
      username: 'fahad_tech',
      phone: '+966 54 876 5432',
      status: 'online',
    },
    {
      id: 'contact_noura',
      firstName: 'نورة',
      lastName: 'السعيد',
      username: 'noura_s',
      phone: '+965 99 345 678',
      status: 'recently',
    },
  ]);

  const [isAdding, setIsAdding] = useState(false);
  const [newFirstName, setNewFirstName] = useState('');
  const [newLastName, setNewLastName] = useState('');
  const [newPhone, setNewPhone] = useState('');

  if (!isOpen) return null;

  const filteredContacts = contacts.filter((c) => {
    if (!search.trim()) return true;
    const q = search.toLowerCase();
    const fullName = `${c.firstName} ${c.lastName || ''}`.toLowerCase();
    return fullName.includes(q) || c.username?.toLowerCase().includes(q) || c.phone?.includes(q);
  });

  const handleAddContact = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newFirstName.trim() || !newPhone.trim()) return;

    const added: TelegramUser = {
      id: 'contact_' + Date.now(),
      firstName: newFirstName.trim(),
      lastName: newLastName.trim() || undefined,
      phone: newPhone.trim(),
      status: 'recently',
    };

    setContacts([added, ...contacts]);
    setNewFirstName('');
    setNewLastName('');
    setNewPhone('');
    setIsAdding(false);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in select-none">
      <div
        className={`w-full max-w-md h-[560px] rounded-3xl shadow-2xl p-6 border flex flex-col transition-colors ${
          isDark ? 'bg-[#17212b] border-[#242f3d] text-white' : 'bg-white border-gray-200 text-gray-900'
        }`}
      >
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-gray-700/40">
          <div className="flex items-center gap-2 font-bold text-base">
            <Users className="w-5 h-5 text-[#3390ec]" />
            <span>{isAr ? 'جهات الاتصال' : 'Contacts'}</span>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg hover:bg-white/10 text-gray-400 hover:text-white transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {isAdding ? (
          <form onSubmit={handleAddContact} className="mt-4 space-y-4 flex-1">
            <h4 className="font-bold text-sm text-[#3390ec]">{isAr ? 'إضافة جهة اتصال جديدة' : 'Add New Contact'}</h4>

            <div>
              <label className="block text-xs font-semibold text-gray-400 mb-1">{isAr ? 'الاسم الأول' : 'First Name'} *</label>
              <input
                type="text"
                value={newFirstName}
                onChange={(e) => setNewFirstName(e.target.value)}
                required
                autoFocus
                className={`w-full px-3 py-2 rounded-xl text-xs outline-none border ${
                  isDark ? 'bg-black/30 border-gray-700 text-white' : 'bg-white border-gray-300'
                }`}
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-gray-400 mb-1">{isAr ? 'اسم العائلة' : 'Last Name'}</label>
              <input
                type="text"
                value={newLastName}
                onChange={(e) => setNewLastName(e.target.value)}
                className={`w-full px-3 py-2 rounded-xl text-xs outline-none border ${
                  isDark ? 'bg-black/30 border-gray-700 text-white' : 'bg-white border-gray-300'
                }`}
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-gray-400 mb-1">{isAr ? 'رقم الهاتف' : 'Phone Number'} *</label>
              <input
                type="tel"
                value={newPhone}
                onChange={(e) => setNewPhone(e.target.value)}
                placeholder="+966 50 000 0000"
                dir="ltr"
                required
                className={`w-full px-3 py-2 rounded-xl text-xs outline-none border ${
                  isDark ? 'bg-black/30 border-gray-700 text-white' : 'bg-white border-gray-300'
                }`}
              />
            </div>

            <div className="flex items-center justify-end gap-3 pt-4">
              <button
                type="button"
                onClick={() => setIsAdding(false)}
                className="px-4 py-2 rounded-xl text-xs font-semibold hover:bg-white/10 text-gray-400"
              >
                {isAr ? 'إلغاء' : 'Cancel'}
              </button>
              <button
                type="submit"
                className="px-5 py-2 rounded-xl text-xs font-bold bg-[#3390ec] hover:bg-[#2b7ec9] text-white"
              >
                {isAr ? 'حفظ جهة الاتصال' : 'Save Contact'}
              </button>
            </div>
          </form>
        ) : (
          <>
            {/* Search and Add */}
            <div className="my-3 flex items-center gap-2">
              <div
                className={`flex-1 flex items-center gap-2 px-3 py-2 rounded-xl border ${
                  isDark ? 'bg-[#242f3d] border-transparent' : 'bg-gray-100 border-transparent'
                }`}
              >
                <Search className="w-4 h-4 text-gray-400 shrink-0" />
                <input
                  type="text"
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  placeholder={isAr ? 'بحث في جهات الاتصال...' : 'Search contacts...'}
                  className="w-full bg-transparent text-xs outline-none"
                />
              </div>

              <button
                onClick={() => setIsAdding(true)}
                className="p-2 rounded-xl bg-[#3390ec]/20 hover:bg-[#3390ec] text-[#3390ec] hover:text-white transition"
                title={isAr ? 'إضافة جهة اتصال' : 'Add Contact'}
              >
                <UserPlus className="w-5 h-5" />
              </button>
            </div>

            {/* List */}
            <div className="flex-1 overflow-y-auto space-y-1 pr-1">
              {filteredContacts.map((c) => {
                const fullName = `${c.firstName} ${c.lastName || ''}`.trim();
                return (
                  <div
                    key={c.id}
                    onClick={() => {
                      onSelectContact(c);
                      onClose();
                    }}
                    className={`p-2.5 rounded-2xl flex items-center gap-3 cursor-pointer transition ${
                      isDark ? 'hover:bg-white/10' : 'hover:bg-gray-100'
                    }`}
                  >
                    <div
                      style={{ backgroundColor: getSenderColor(c.id) }}
                      className="w-10 h-10 rounded-full flex items-center justify-center font-bold text-xs text-white shrink-0 shadow"
                    >
                      {getInitials(fullName)}
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-1">
                        <span className="font-semibold text-xs truncate">{fullName}</span>
                        {c.isVerified && <span className="text-[#3390ec] text-[10px]">✓</span>}
                      </div>
                      <span className="text-[11px] text-gray-400 block truncate">
                        {c.phone || (c.username ? `@${c.username}` : '')}
                      </span>
                    </div>
                    <span
                      className={`text-[10px] font-medium shrink-0 ${
                        c.status === 'online' ? 'text-emerald-400' : 'text-gray-500'
                      }`}
                    >
                      {c.status === 'online'
                        ? isAr
                          ? 'متصل'
                          : 'online'
                        : isAr
                        ? 'مؤخراً'
                        : 'recently'}
                    </span>
                  </div>
                );
              })}
            </div>
          </>
        )}
      </div>
    </div>
  );
};
