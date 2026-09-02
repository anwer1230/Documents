import React, { useRef, useState } from "react";
import {
  Image,
  FileText,
  Music,
  BarChart2,
  MapPin,
  User,
  X,
  Send,
  Plus,
  Trash2,
  HelpCircle,
} from "lucide-react";
import { TelegramMedia } from "../types";

interface AttachmentSheetProps {
  isOpen: boolean;
  onClose: () => void;
  onSendMedia: (media: TelegramMedia, caption?: string) => void;
  onSendPoll?: (question: string, options: string[], isAnonymous: boolean, isQuiz: boolean) => void;
}

export const AttachmentSheet: React.FC<AttachmentSheetProps> = ({
  isOpen,
  onClose,
  onSendMedia,
  onSendPoll,
}) => {
  const photoInputRef = useRef<HTMLInputElement>(null);
  const docInputRef = useRef<HTMLInputElement>(null);
  const audioInputRef = useRef<HTMLInputElement>(null);

  // Staged Media Preview state before sending
  const [stagedMedia, setStagedMedia] = useState<{
    file: File;
    previewUrl: string;
    type: "photo" | "video" | "document" | "audio";
    name: string;
    size: number;
  } | null>(null);
  const [mediaCaption, setMediaCaption] = useState("");

  // Poll creation modal state
  const [pollModalOpen, setPollModalOpen] = useState(false);
  const [pollQuestion, setPollQuestion] = useState("");
  const [pollOptions, setPollOptions] = useState<string[]>(["", ""]);
  const [isAnonymous, setIsAnonymous] = useState(true);
  const [isQuiz, setIsQuiz] = useState(false);
  const [correctOptionIndex, setCorrectOptionIndex] = useState<number>(0);

  // Location modal state
  const [locationModalOpen, setLocationModalOpen] = useState(false);
  const [locationName, setLocationName] = useState("الموقع الحالي (الرياض / دبي)");
  const [locationCoords, setLocationCoords] = useState("24.7136° N, 46.6753° E");

  // Contact modal state
  const [contactModalOpen, setContactModalOpen] = useState(false);
  const [contactName, setContactName] = useState("");
  const [contactPhone, setContactPhone] = useState("");

  if (!isOpen && !stagedMedia && !pollModalOpen && !locationModalOpen && !contactModalOpen) {
    return null;
  }

  // Handle Photo & Video Selection
  const handlePhotoSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const isVideo = file.type.startsWith("video/");
    const previewUrl = URL.createObjectURL(file);

    setStagedMedia({
      file,
      previewUrl,
      type: isVideo ? "video" : "photo",
      name: file.name,
      size: file.size,
    });
    setMediaCaption("");
    onClose();
  };

  // Handle Document Selection
  const handleDocSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const previewUrl = URL.createObjectURL(file);
    setStagedMedia({
      file,
      previewUrl,
      type: "document",
      name: file.name,
      size: file.size,
    });
    setMediaCaption("");
    onClose();
  };

  // Handle Audio Selection
  const handleAudioSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const previewUrl = URL.createObjectURL(file);
    setStagedMedia({
      file,
      previewUrl,
      type: "audio",
      name: file.name,
      size: file.size,
    });
    setMediaCaption("");
    onClose();
  };

  // Confirm and Send Staged Media
  const handleConfirmSendMedia = () => {
    if (!stagedMedia) return;

    const mediaObj: TelegramMedia = {
      type: stagedMedia.type,
      url: stagedMedia.previewUrl,
      fileName: stagedMedia.name,
      fileSize: stagedMedia.size,
      mimeType: stagedMedia.file.type,
      caption: mediaCaption.trim() || undefined,
    };

    onSendMedia(mediaObj, mediaCaption.trim() || undefined);
    setStagedMedia(null);
    setMediaCaption("");
  };

  // Submit Poll
  const handleCreatePoll = (e: React.FormEvent) => {
    e.preventDefault();
    if (!pollQuestion.trim()) return;
    const validOptions = pollOptions.filter((o) => o.trim().length > 0);
    if (validOptions.length < 2) return;

    if (onSendPoll) {
      onSendPoll(pollQuestion.trim(), validOptions, isAnonymous, isQuiz);
    } else {
      // Fallback as formatted poll media
      onSendMedia({
        type: "document",
        fileName: `📊 استطلاع: ${pollQuestion.trim()}`,
        caption: `📊 ${pollQuestion.trim()}\n\n` + validOptions.map((opt, i) => `${i + 1}. ${opt} (0%)`).join("\n"),
      });
    }

    setPollModalOpen(false);
    setPollQuestion("");
    setPollOptions(["", ""]);
    onClose();
  };

  // Submit Location
  const handleSendLocation = () => {
    onSendMedia({
      type: "webpage",
      caption: `📍 ${locationName}\nإحداثيات: ${locationCoords}\nhttps://maps.google.com/?q=${encodeURIComponent(locationCoords)}`,
    });
    setLocationModalOpen(false);
    onClose();
  };

  // Submit Contact
  const handleSendContact = () => {
    if (!contactName.trim() || !contactPhone.trim()) return;
    onSendMedia({
      type: "contact",
      fileName: `${contactName}.vcf`,
      caption: `👤 جهة اتصال: ${contactName}\n📞 الهاتف: ${contactPhone}`,
    });
    setContactModalOpen(false);
    setContactName("");
    setContactPhone("");
    onClose();
  };

  return (
    <>
      {/* Hidden File Inputs */}
      <input
        ref={photoInputRef}
        type="file"
        accept="image/*,video/*"
        className="hidden"
        onChange={handlePhotoSelect}
      />
      <input
        ref={docInputRef}
        type="file"
        accept="*/*"
        className="hidden"
        onChange={handleDocSelect}
      />
      <input
        ref={audioInputRef}
        type="file"
        accept="audio/*"
        className="hidden"
        onChange={handleAudioSelect}
      />

      {/* Main DrKLO/Telegram Attachment Action Grid Sheet */}
      {isOpen && (
        <div className="absolute bottom-16 right-4 z-40 bg-white/95 dark:bg-slate-900/95 backdrop-blur-xl border border-slate-200 dark:border-slate-800 rounded-3xl p-4 shadow-2xl animate-fade-in w-72 select-none">
          <div className="flex items-center justify-between pb-3 mb-3 border-b border-slate-100 dark:border-slate-800">
            <span className="text-xs font-bold text-slate-700 dark:text-slate-200">
              إرفاق محتوى ووسائط
            </span>
            <button
              onClick={onClose}
              className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 p-1 rounded-full hover:bg-slate-100 dark:hover:bg-slate-800"
            >
              <X className="w-4 h-4" />
            </button>
          </div>

          <div className="grid grid-cols-3 gap-3 text-center">
            {/* Gallery (Photo / Video) */}
            <button
              onClick={() => photoInputRef.current?.click()}
              className="flex flex-col items-center gap-1.5 p-2 rounded-2xl hover:bg-sky-50 dark:hover:bg-sky-950/50 transition-all group"
            >
              <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-sky-400 to-blue-500 text-white flex items-center justify-center shadow-md shadow-sky-500/20 group-hover:scale-105 transition-transform">
                <Image className="w-6 h-6" />
              </div>
              <span className="text-[11px] font-bold text-slate-700 dark:text-slate-300">
                صورة وفيديو
              </span>
            </button>

            {/* Document / File */}
            <button
              onClick={() => docInputRef.current?.click()}
              className="flex flex-col items-center gap-1.5 p-2 rounded-2xl hover:bg-indigo-50 dark:hover:bg-indigo-950/50 transition-all group"
            >
              <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-indigo-500 to-purple-600 text-white flex items-center justify-center shadow-md shadow-indigo-500/20 group-hover:scale-105 transition-transform">
                <FileText className="w-6 h-6" />
              </div>
              <span className="text-[11px] font-bold text-slate-700 dark:text-slate-300">
                مستند وملف
              </span>
            </button>

            {/* Music / Audio */}
            <button
              onClick={() => audioInputRef.current?.click()}
              className="flex flex-col items-center gap-1.5 p-2 rounded-2xl hover:bg-amber-50 dark:hover:bg-amber-950/50 transition-all group"
            >
              <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-amber-400 to-orange-500 text-white flex items-center justify-center shadow-md shadow-amber-500/20 group-hover:scale-105 transition-transform">
                <Music className="w-6 h-6" />
              </div>
              <span className="text-[11px] font-bold text-slate-700 dark:text-slate-300">
                موسيقى
              </span>
            </button>

            {/* Poll */}
            <button
              onClick={() => {
                onClose();
                setPollModalOpen(true);
              }}
              className="flex flex-col items-center gap-1.5 p-2 rounded-2xl hover:bg-emerald-50 dark:hover:bg-emerald-950/50 transition-all group"
            >
              <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-emerald-400 to-teal-500 text-white flex items-center justify-center shadow-md shadow-emerald-500/20 group-hover:scale-105 transition-transform">
                <BarChart2 className="w-6 h-6" />
              </div>
              <span className="text-[11px] font-bold text-slate-700 dark:text-slate-300">
                استطلاع رأي
              </span>
            </button>

            {/* Location */}
            <button
              onClick={() => {
                onClose();
                setLocationModalOpen(true);
              }}
              className="flex flex-col items-center gap-1.5 p-2 rounded-2xl hover:bg-rose-50 dark:hover:bg-rose-950/50 transition-all group"
            >
              <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-rose-400 to-red-500 text-white flex items-center justify-center shadow-md shadow-rose-500/20 group-hover:scale-105 transition-transform">
                <MapPin className="w-6 h-6" />
              </div>
              <span className="text-[11px] font-bold text-slate-700 dark:text-slate-300">
                الموقع
              </span>
            </button>

            {/* Contact */}
            <button
              onClick={() => {
                onClose();
                setContactModalOpen(true);
              }}
              className="flex flex-col items-center gap-1.5 p-2 rounded-2xl hover:bg-cyan-50 dark:hover:bg-cyan-950/50 transition-all group"
            >
              <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-cyan-400 to-sky-600 text-white flex items-center justify-center shadow-md shadow-cyan-500/20 group-hover:scale-105 transition-transform">
                <User className="w-6 h-6" />
              </div>
              <span className="text-[11px] font-bold text-slate-700 dark:text-slate-300">
                جهة اتصال
              </span>
            </button>
          </div>
        </div>
      )}

      {/* Staged Media Preview & Caption Modal before Sending */}
      {stagedMedia && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md flex items-center justify-center p-4">
          <div
            className="bg-white dark:bg-slate-900 rounded-3xl max-w-lg w-full overflow-hidden shadow-2xl border border-slate-200 dark:border-slate-800 animate-scale-up"
            dir="rtl"
          >
            <div className="p-4 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between">
              <span className="font-bold text-sm text-slate-800 dark:text-slate-100">
                إرسال وسائط ومستندات
              </span>
              <button
                onClick={() => setStagedMedia(null)}
                className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 p-1"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-4 bg-slate-50 dark:bg-slate-950/50 flex flex-col items-center justify-center min-h-60 max-h-96 overflow-hidden">
              {stagedMedia.type === "photo" ? (
                <img
                  src={stagedMedia.previewUrl}
                  alt="Upload preview"
                  className="max-h-72 object-contain rounded-2xl shadow-md"
                />
              ) : stagedMedia.type === "video" ? (
                <video
                  src={stagedMedia.previewUrl}
                  controls
                  className="max-h-72 rounded-2xl shadow-md"
                />
              ) : (
                <div className="flex flex-col items-center gap-3 p-6 text-center">
                  <div className="w-16 h-16 rounded-2xl bg-indigo-500/10 text-indigo-500 flex items-center justify-center">
                    <FileText className="w-8 h-8" />
                  </div>
                  <div>
                    <h4 className="font-bold text-sm text-slate-800 dark:text-slate-100 break-all">
                      {stagedMedia.name}
                    </h4>
                    <span className="text-xs text-slate-400">
                      {(stagedMedia.size / (1024 * 1024)).toFixed(2)} MB
                    </span>
                  </div>
                </div>
              )}
            </div>

            <div className="p-4 space-y-3">
              <input
                type="text"
                value={mediaCaption}
                onChange={(e) => setMediaCaption(e.target.value)}
                placeholder="إضافة شرح أو تعليق (اختياري)..."
                className="w-full bg-slate-100 dark:bg-slate-800 text-xs px-4 py-2.5 rounded-2xl border border-transparent focus:border-sky-500 focus:outline-none text-slate-800 dark:text-slate-100"
                autoFocus
              />

              <div className="flex items-center justify-between pt-2">
                <button
                  type="button"
                  onClick={() => setStagedMedia(null)}
                  className="px-4 py-2 rounded-xl text-xs font-bold text-slate-500 hover:bg-slate-100 dark:hover:bg-slate-800"
                >
                  إلغاء
                </button>
                <button
                  type="button"
                  onClick={handleConfirmSendMedia}
                  className="px-6 py-2.5 bg-sky-500 hover:bg-sky-600 text-white font-bold text-xs rounded-2xl shadow-md shadow-sky-500/20 flex items-center gap-2"
                >
                  <span>إرسال</span>
                  <Send className="w-4 h-4 -rotate-45" />
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Poll Creation Modal */}
      {pollModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md flex items-center justify-center p-4">
          <div
            className="bg-white dark:bg-slate-900 rounded-3xl max-w-md w-full p-5 shadow-2xl border border-slate-200 dark:border-slate-800 animate-scale-up"
            dir="rtl"
          >
            <div className="flex items-center justify-between pb-3 mb-4 border-b border-slate-100 dark:border-slate-800">
              <div className="flex items-center gap-2 text-slate-800 dark:text-slate-100">
                <BarChart2 className="w-5 h-5 text-emerald-500" />
                <h3 className="font-bold text-sm">إنشاء استطلاع رأي جديد</h3>
              </div>
              <button
                onClick={() => setPollModalOpen(false)}
                className="text-slate-400 hover:text-slate-600 p-1"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreatePoll} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 mb-1">
                  سؤال الاستطلاع:
                </label>
                <input
                  type="text"
                  required
                  value={pollQuestion}
                  onChange={(e) => setPollQuestion(e.target.value)}
                  placeholder="اطرح سؤالاً..."
                  className="w-full bg-slate-100 dark:bg-slate-800 px-3.5 py-2 rounded-xl text-xs text-slate-800 dark:text-slate-100 border border-transparent focus:border-emerald-500 focus:outline-none"
                  autoFocus
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 mb-1">
                  خيارات الإجابة:
                </label>
                <div className="space-y-2 max-h-48 overflow-y-auto">
                  {pollOptions.map((opt, idx) => (
                    <div key={idx} className="flex items-center gap-2">
                      <input
                        type="text"
                        value={opt}
                        onChange={(e) => {
                          const copy = [...pollOptions];
                          copy[idx] = e.target.value;
                          setPollOptions(copy);
                        }}
                        placeholder={`خيار ${idx + 1}`}
                        className="flex-1 bg-slate-100 dark:bg-slate-800 px-3 py-1.5 rounded-xl text-xs text-slate-800 dark:text-slate-100 border border-transparent focus:border-emerald-500 focus:outline-none"
                      />
                      {pollOptions.length > 2 && (
                        <button
                          type="button"
                          onClick={() => {
                            setPollOptions(pollOptions.filter((_, i) => i !== idx));
                          }}
                          className="text-red-400 hover:text-red-600 p-1"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      )}
                    </div>
                  ))}
                </div>

                {pollOptions.length < 10 && (
                  <button
                    type="button"
                    onClick={() => setPollOptions([...pollOptions, ""])}
                    className="mt-2 text-xs font-bold text-emerald-600 dark:text-emerald-400 hover:underline flex items-center gap-1"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    <span>إضافة خيار آخر</span>
                  </button>
                )}
              </div>

              {/* Poll Settings */}
              <div className="space-y-2 pt-2 border-t border-slate-100 dark:border-slate-800">
                <label className="flex items-center justify-between text-xs cursor-pointer">
                  <span className="text-slate-700 dark:text-slate-300 font-medium">
                    تصويت مجهول الهوية (Anonymous)
                  </span>
                  <input
                    type="checkbox"
                    checked={isAnonymous}
                    onChange={(e) => setIsAnonymous(e.target.checked)}
                    className="rounded text-emerald-500 focus:ring-emerald-500 w-4 h-4"
                  />
                </label>

                <label className="flex items-center justify-between text-xs cursor-pointer">
                  <span className="text-slate-700 dark:text-slate-300 font-medium">
                    وضع الاختبار (Quiz Mode)
                  </span>
                  <input
                    type="checkbox"
                    checked={isQuiz}
                    onChange={(e) => setIsQuiz(e.target.checked)}
                    className="rounded text-emerald-500 focus:ring-emerald-500 w-4 h-4"
                  />
                </label>
              </div>

              <div className="flex items-center justify-end gap-2 pt-3">
                <button
                  type="button"
                  onClick={() => setPollModalOpen(false)}
                  className="px-4 py-2 rounded-xl text-xs font-bold text-slate-500 hover:bg-slate-100 dark:hover:bg-slate-800"
                >
                  إلغاء
                </button>
                <button
                  type="submit"
                  className="px-5 py-2.5 bg-emerald-500 hover:bg-emerald-600 text-white font-bold text-xs rounded-xl shadow-md shadow-emerald-500/20"
                >
                  نشر الاستطلاع
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Location Modal */}
      {locationModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md flex items-center justify-center p-4">
          <div
            className="bg-white dark:bg-slate-900 rounded-3xl max-w-md w-full p-5 shadow-2xl border border-slate-200 dark:border-slate-800 animate-scale-up"
            dir="rtl"
          >
            <div className="flex items-center justify-between pb-3 mb-4 border-b border-slate-100 dark:border-slate-800">
              <div className="flex items-center gap-2 text-slate-800 dark:text-slate-100">
                <MapPin className="w-5 h-5 text-rose-500" />
                <h3 className="font-bold text-sm">مشاركة الموقع الجغرافي</h3>
              </div>
              <button
                onClick={() => setLocationModalOpen(false)}
                className="text-slate-400 hover:text-slate-600 p-1"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-3">
              <div>
                <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 mb-1">
                  اسم الموقع:
                </label>
                <input
                  type="text"
                  value={locationName}
                  onChange={(e) => setLocationName(e.target.value)}
                  className="w-full bg-slate-100 dark:bg-slate-800 px-3.5 py-2 rounded-xl text-xs text-slate-800 dark:text-slate-100"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 mb-1">
                  الإحداثيات:
                </label>
                <input
                  type="text"
                  value={locationCoords}
                  onChange={(e) => setLocationCoords(e.target.value)}
                  className="w-full bg-slate-100 dark:bg-slate-800 px-3.5 py-2 rounded-xl text-xs text-slate-800 dark:text-slate-100"
                />
              </div>

              <div className="flex items-center justify-end gap-2 pt-3">
                <button
                  type="button"
                  onClick={() => setLocationModalOpen(false)}
                  className="px-4 py-2 rounded-xl text-xs font-bold text-slate-500"
                >
                  إلغاء
                </button>
                <button
                  type="button"
                  onClick={handleSendLocation}
                  className="px-5 py-2.5 bg-rose-500 hover:bg-rose-600 text-white font-bold text-xs rounded-xl shadow-md"
                >
                  مشاركة الموقع
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Contact Modal */}
      {contactModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md flex items-center justify-center p-4">
          <div
            className="bg-white dark:bg-slate-900 rounded-3xl max-w-md w-full p-5 shadow-2xl border border-slate-200 dark:border-slate-800 animate-scale-up"
            dir="rtl"
          >
            <div className="flex items-center justify-between pb-3 mb-4 border-b border-slate-100 dark:border-slate-800">
              <div className="flex items-center gap-2 text-slate-800 dark:text-slate-100">
                <User className="w-5 h-5 text-cyan-500" />
                <h3 className="font-bold text-sm">مشاركة جهة اتصال</h3>
              </div>
              <button
                onClick={() => setContactModalOpen(false)}
                className="text-slate-400 hover:text-slate-600 p-1"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-3">
              <div>
                <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 mb-1">
                  الاسم الكامل:
                </label>
                <input
                  type="text"
                  required
                  value={contactName}
                  onChange={(e) => setContactName(e.target.value)}
                  placeholder="محمد أحمد"
                  className="w-full bg-slate-100 dark:bg-slate-800 px-3.5 py-2 rounded-xl text-xs text-slate-800 dark:text-slate-100"
                  autoFocus
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 mb-1">
                  رقم الهاتف:
                </label>
                <input
                  type="tel"
                  required
                  value={contactPhone}
                  onChange={(e) => setContactPhone(e.target.value)}
                  placeholder="+966 50 123 4567"
                  className="w-full bg-slate-100 dark:bg-slate-800 px-3.5 py-2 rounded-xl text-xs text-slate-800 dark:text-slate-100"
                />
              </div>

              <div className="flex items-center justify-end gap-2 pt-3">
                <button
                  type="button"
                  onClick={() => setContactModalOpen(false)}
                  className="px-4 py-2 rounded-xl text-xs font-bold text-slate-500"
                >
                  إلغاء
                </button>
                <button
                  type="button"
                  onClick={handleSendContact}
                  className="px-5 py-2.5 bg-cyan-500 hover:bg-cyan-600 text-white font-bold text-xs rounded-xl shadow-md"
                >
                  إرسال جهة الاتصال
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  );
};
