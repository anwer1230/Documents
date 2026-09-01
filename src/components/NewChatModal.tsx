import React, { useState } from "react";
import { X, UserPlus, Users, Megaphone, Search, Check } from "lucide-react";

const getAvatarColor = (name: string) => {
  const colors = [
    'from-red-500 to-orange-500',
    'from-blue-500 to-cyan-500',
    'from-emerald-500 to-teal-500',
    'from-purple-500 to-indigo-500',
    'from-pink-500 to-rose-500',
    'from-amber-500 to-yellow-500',
  ];
  let hash = 0;
  for (let i = 0; i < (name || '').length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash);
  return colors[Math.abs(hash) % colors.length];
};

interface NewChatModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreateChat: (title: string, type: "user" | "group" | "channel") => void;
}

const SAMPLE_CONTACTS = [
  { id: "c1", name: "محمد العلي", username: "mohammed_ali", phone: "+966 55 111 2222" },
  { id: "c2", name: "سارة الأحمد", username: "sara_tech", phone: "+966 50 333 4444" },
  { id: "c3", name: "عمر الفاروق", username: "omar_dev", phone: "+966 54 555 6666" },
  { id: "c4", name: "ريم الصالح", username: "reem_designer", phone: "+966 56 777 8888" },
  { id: "c5", name: "خالد بن الوليد", username: "khalid_w", phone: "+966 59 999 0000" },
];

export const NewChatModal: React.FC<NewChatModalProps> = ({
  isOpen,
  onClose,
  onCreateChat,
}) => {
  const [activeMode, setActiveMode] = useState<"direct" | "group" | "channel">("direct");
  const [nameInput, setNameInput] = useState("");
  const [searchContact, setSearchContact] = useState("");

  if (!isOpen) return null;

  const filteredContacts = SAMPLE_CONTACTS.filter((c) =>
    c.name.toLowerCase().includes(searchContact.toLowerCase()) ||
    c.username.toLowerCase().includes(searchContact.toLowerCase())
  );

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!nameInput.trim()) return;
    onCreateChat(nameInput.trim(), activeMode === "direct" ? "user" : activeMode);
    setNameInput("");
    onClose();
  };

  const handleSelectContact = (contactName: string) => {
    onCreateChat(contactName, "user");
    onClose();
  };

  return (
    <div
      id="telegram-new-chat-modal-overlay"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs"
      dir="rtl"
    >
      <div
        id="telegram-new-chat-modal-container"
        className="w-full max-w-md bg-white dark:bg-slate-900 rounded-2xl shadow-2xl border border-slate-200 dark:border-slate-800 overflow-hidden flex flex-col max-h-[85vh]"
      >
        {/* Header */}
        <div className="p-4 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between">
          <h3 className="font-bold text-base text-slate-800 dark:text-white">
            إنشاء محادثة جديدة
          </h3>
          <button
            id="new-chat-modal-close-btn"
            onClick={onClose}
            className="w-8 h-8 rounded-full flex items-center justify-center text-slate-500 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Type Selector */}
        <div className="p-3 border-b border-slate-100 dark:border-slate-800 flex gap-2">
          <button
            onClick={() => setActiveMode("direct")}
            className={`flex-1 py-2 px-3 rounded-xl text-xs font-semibold flex items-center justify-center gap-1.5 transition-all ${
              activeMode === "direct"
                ? "bg-sky-500 text-white shadow-sm"
                : "bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300"
            }`}
          >
            <UserPlus className="w-3.5 h-3.5" />
            <span>محادثة خاصة</span>
          </button>

          <button
            onClick={() => setActiveMode("group")}
            className={`flex-1 py-2 px-3 rounded-xl text-xs font-semibold flex items-center justify-center gap-1.5 transition-all ${
              activeMode === "group"
                ? "bg-sky-500 text-white shadow-sm"
                : "bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300"
            }`}
          >
            <Users className="w-3.5 h-3.5" />
            <span>مجموعة جديدة</span>
          </button>

          <button
            onClick={() => setActiveMode("channel")}
            className={`flex-1 py-2 px-3 rounded-xl text-xs font-semibold flex items-center justify-center gap-1.5 transition-all ${
              activeMode === "channel"
                ? "bg-sky-500 text-white shadow-sm"
                : "bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300"
            }`}
          >
            <Megaphone className="w-3.5 h-3.5" />
            <span>قناة جديدة</span>
          </button>
        </div>

        {/* Form or Contact Picker */}
        <div className="p-4 overflow-y-auto space-y-4">
          {activeMode !== "direct" ? (
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 mb-1.5">
                  {activeMode === "group" ? "اسم المجموعة" : "اسم القناة"}
                </label>
                <input
                  type="text"
                  required
                  autoFocus
                  value={nameInput}
                  onChange={(e) => setNameInput(e.target.value)}
                  placeholder={activeMode === "group" ? "مثال: فريق العمل" : "مثال: قناة الأخبار"}
                  className="w-full px-3.5 py-2.5 bg-slate-50 dark:bg-slate-800/80 border border-slate-200 dark:border-slate-700 rounded-xl text-slate-800 dark:text-white text-xs focus:outline-none focus:ring-2 focus:ring-sky-500"
                />
              </div>

              <button
                type="submit"
                className="w-full py-2.5 bg-sky-500 hover:bg-sky-600 text-white text-xs font-bold rounded-xl transition-colors shadow-sm"
              >
                {activeMode === "group" ? "إنشاء المجموعة" : "إنشاء القناة"}
              </button>
            </form>
          ) : (
            <div>
              {/* Search Contacts */}
              <div className="relative mb-3">
                <Search className="w-4 h-4 absolute right-3 top-1/2 -translate-y-1/2 text-slate-400" />
                <input
                  type="text"
                  value={searchContact}
                  onChange={(e) => setSearchContact(e.target.value)}
                  placeholder="بحث في جهات الاتصال..."
                  className="w-full pr-9 pl-4 py-2 bg-slate-100 dark:bg-slate-800 rounded-xl text-xs text-slate-800 dark:text-slate-200 focus:outline-none"
                />
              </div>

              {/* Contacts List */}
              <div className="space-y-1">
                {filteredContacts.map((contact) => (
                  <div
                    key={contact.id}
                    onClick={() => handleSelectContact(contact.name)}
                    className="p-2 rounded-xl flex items-center gap-3 hover:bg-slate-100 dark:hover:bg-slate-800 cursor-pointer transition-colors"
                  >
                    <div
                      className={`w-10 h-10 rounded-full bg-gradient-to-tr ${getAvatarColor(
                        contact.name
                      )} flex items-center justify-center text-white text-xs font-bold shrink-0`}
                    >
                      {contact.name.substring(0, 2).toUpperCase()}
                    </div>
                    <div className="min-w-0 flex-1">
                      <h4 className="font-semibold text-xs text-slate-800 dark:text-white truncate">
                        {contact.name}
                      </h4>
                      <p className="text-[11px] text-slate-400 truncate" dir="ltr">
                        @{contact.username}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
