import React, { useState } from 'react';
import { X, UserPlus, Search, Phone, User } from 'lucide-react';
import { TelegramUser } from '../types';

interface ContactsModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSelectContact: (contact: TelegramUser) => void;
  lang: 'ar' | 'en';
  isDark: boolean;
}

const INITIAL_CONTACTS: TelegramUser[] = [
  {
    id: 'contact_1',
    firstName: 'أحمد',
    lastName: 'المنصور',
    phone: '+966 54 987 6543',
    username: 'ahmed_mansour',
    status: 'online',
  },
  {
    id: 'contact_2',
    firstName: 'خالد',
    lastName: 'عبدالله',
    phone: '+966 55 112 2334',
    username: 'khalid_abdullah',
    status: 'recently',
  },
  {
    id: 'contact_3',
    firstName: 'سارة',
    lastName: 'الغامدي',
    phone: '+966 50 334 5566',
    username: 'sara_alghamdi',
    status: 'online',
  },
  {
    id: 'contact_4',
    firstName: 'عبدالرحمن',
    lastName: 'العتيبي',
    phone: '+966 56 778 8990',
    username: 'abdulrahman_o',
    status: 'recently',
  },
];

export const ContactsModal: React.FC<ContactsModalProps> = ({
  isOpen,
  onClose,
  onSelectContact,
  lang,
  isDark,
}) => {
  const isAr = lang === 'ar';
  const [contacts, setContacts] = useState<TelegramUser[]>(INITIAL_CONTACTS);
  const [search, setSearch] = useState('');
  const [showAddForm, setShowAddForm] = useState(false);

  // New Contact fields
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [phone, setPhone] = useState('');

  if (!isOpen) return null;

  const filtered = contacts.filter((c) => {
    const q = search.toLowerCase();
    return (
      c.firstName.toLowerCase().includes(q) ||
      c.lastName?.toLowerCase().includes(q) ||
      c.phone?.includes(q)
    );
  });

  const handleAddContact = (e: React.FormEvent) => {
    e.preventDefault();
    if (!firstName.trim() || !phone.trim()) return;

    const newC: TelegramUser = {
      id: 'contact_' + Date.now(),
      firstName: firstName.trim(),
      lastName: lastName.trim() || undefined,
      phone: phone.trim(),
      status: 'online',
    };

    setContacts([newC, ...contacts]);
    setFirstName('');
    setLastName('');
    setPhone('');
    setShowAddForm(false);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 select-none">
      <div onClick={onClose} className="fixed inset-0 bg-black/60 backdrop-blur-xs" />

      <div
        className={`relative w-full max-w-md rounded-2xl p-6 shadow-2xl z-10 border max-h-[85vh] flex flex-col ${
          isDark ? 'bg-[#17212b] border-[#232e3c] text-white' : 'bg-white border-gray-200 text-gray-900'
        }`}
      >
        <div className="flex items-center justify-between mb-4">
          <h3 className="font-bold text-base">{isAr ? 'جهات الاتصال' : 'Contacts'}</h3>
          <button onClick={onClose} className="p-1 rounded-full text-gray-400 hover:text-white">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Search / Add Header */}
        <div className="flex items-center gap-2 mb-3">
          <div className="relative flex-1">
            <input
              type="text"
              placeholder={isAr ? 'بحث في الأسماء أو الأرقام...' : 'Search contacts...'}
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className={`w-full py-2 ps-8 pe-3 text-xs rounded-xl border focus:outline-none focus:border-[#3390ec] ${
                isDark ? 'bg-[#242f3d] border-[#2f3f50] text-white' : 'bg-gray-100 border-gray-200'
              }`}
            />
            <Search className="w-3.5 h-3.5 text-gray-400 absolute start-2.5 top-2.5" />
          </div>

          <button
            onClick={() => setShowAddForm(!showAddForm)}
            className="p-2 rounded-xl bg-[#3390ec] hover:bg-[#2b7ec9] text-white transition"
            title={isAr ? 'إضافة جهة اتصال' : 'Add Contact'}
          >
            <UserPlus className="w-4 h-4" />
          </button>
        </div>

        {/* Add Contact Accordion */}
        {showAddForm && (
          <form
            onSubmit={handleAddContact}
            className={`p-3 rounded-xl border mb-3 space-y-2 text-xs ${
              isDark ? 'bg-[#242f3d]/70 border-[#2f3f50]' : 'bg-gray-50 border-gray-200'
            }`}
          >
            <h4 className="font-bold text-xs">{isAr ? 'إضافة جهة اتصال جديدة' : 'Add New Contact'}</h4>
            <div className="grid grid-cols-2 gap-2">
              <input
                type="text"
                placeholder={isAr ? 'الاسم الأول' : 'First Name'}
                value={firstName}
                onChange={(e) => setFirstName(e.target.value)}
                className="p-2 rounded-lg bg-black/10 border border-gray-500/20 text-xs"
                required
              />
              <input
                type="text"
                placeholder={isAr ? 'اسم العائلة' : 'Last Name'}
                value={lastName}
                onChange={(e) => setLastName(e.target.value)}
                className="p-2 rounded-lg bg-black/10 border border-gray-500/20 text-xs"
              />
            </div>
            <input
              type="tel"
              dir="ltr"
              placeholder="+966 50 123 4567"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              className="w-full p-2 rounded-lg bg-black/10 border border-gray-500/20 text-xs font-mono"
              required
            />
            <div className="flex justify-end gap-2 pt-1">
              <button
                type="button"
                onClick={() => setShowAddForm(false)}
                className="px-3 py-1 rounded-lg text-gray-400 hover:text-white"
              >
                {isAr ? 'إلغاء' : 'Cancel'}
              </button>
              <button
                type="submit"
                className="px-4 py-1.5 rounded-lg bg-[#3390ec] text-white font-bold"
              >
                {isAr ? 'حفظ' : 'Save'}
              </button>
            </div>
          </form>
        )}

        {/* Contact List */}
        <div className="flex-1 overflow-y-auto space-y-1 divide-y divide-gray-700/10 pe-1">
          {filtered.length === 0 ? (
            <p className="text-center py-8 text-xs text-gray-400">
              {isAr ? 'لا توجد جهات اتصال مطابقة' : 'No contacts found'}
            </p>
          ) : (
            filtered.map((c) => (
              <button
                key={c.id}
                onClick={() => {
                  onSelectContact(c);
                  onClose();
                }}
                className={`w-full flex items-center gap-3 p-2.5 rounded-xl transition text-start ${
                  isDark ? 'hover:bg-[#232e3c]' : 'hover:bg-gray-100'
                }`}
              >
                <div className="w-10 h-10 rounded-full bg-[#3390ec] flex items-center justify-center text-white font-bold text-sm shrink-0">
                  {c.firstName.slice(0, 2)}
                </div>
                <div className="min-w-0 flex-1">
                  <span className="font-bold text-xs block truncate">
                    {c.firstName} {c.lastName || ''}
                  </span>
                  <span className="text-[11px] text-gray-400 font-mono block truncate" dir="ltr">
                    {c.phone}
                  </span>
                </div>
                {c.status === 'online' && (
                  <span className="text-[10px] text-emerald-400 font-semibold shrink-0">
                    {isAr ? 'متصل' : 'online'}
                  </span>
                )}
              </button>
            ))
          )}
        </div>
      </div>
    </div>
  );
};
