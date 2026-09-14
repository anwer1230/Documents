/**
 * AndroidNotificationShade.tsx
 *
 * Replicates the Android System Notification Shade & Quick Settings Dropdown
 * Exactly matching the layout, grouping, typography, and badges in the user's screenshot.
 *
 * Implements:
 * 1. Quick Settings top bar: Wi-Fi, Bluetooth, Airplane, Flashlight, Hotspot, Mobile Data + Time (4:15), Date, 51% Battery.
 * 2. Conversations Header ("المحادثات").
 * 3. Group 1: Plus Messenger Card ("Plus • بيان احمد • 279313 رسالة جديدة من 70 محادثة • الآن").
 * 4. Group 2: Telegram Official Card ("تيليجرام • امل • 301 رسالة جديدة من 19 محادثة • 6 دقائق").
 * 5. Stacked sub-items with circular avatars, unread badge counters ("5", "2"), chat titles, timestamps & snippets.
 * 6. Interactive deep-linking to activate the specific chat thread and send real OS push notifications outside the app.
 */

import React, { useState } from 'react';
import {
  Wifi,
  Bluetooth,
  Plane,
  Flashlight,
  Radio,
  ArrowUpDown,
  ChevronDown,
  ChevronUp,
  X,
  Bell,
  Send,
  CheckCheck,
  Smartphone,
  Sparkles,
  ExternalLink,
  BatteryCharging,
  Sliders,
} from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { useTelegram } from '../../context/TelegramContext';
import { backgroundNotificationDaemon } from '../../services/BackgroundNotificationDaemon';

interface AndroidNotificationShadeProps {
  isOpen: boolean;
  onClose: () => void;
}

export const AndroidNotificationShade: React.FC<AndroidNotificationShadeProps> = ({
  isOpen,
  onClose,
}) => {
  const { chats, setActiveChatId, showToast, settings, currentUser } = useTelegram();
  const isArabic = settings.language === 'ar';

  // Quick settings toggles
  const [wifiActive, setWifiActive] = useState(true);
  const [bluetoothActive, setBluetoothActive] = useState(false);
  const [airplaneActive, setAirplaneActive] = useState(false);
  const [flashlightActive, setFlashlightActive] = useState(false);
  const [hotspotActive, setHotspotActive] = useState(false);
  const [mobileDataActive, setMobileDataActive] = useState(false);

  // Group expand/collapse states
  const [plusGroupExpanded, setPlusGroupExpanded] = useState(true);
  const [tgGroupExpanded, setTgGroupExpanded] = useState(true);

  // Notifications dismissed list
  const [dismissedItems, setDismissedItems] = useState<Set<string>>(new Set());

  if (!isOpen) return null;

  const handleOpenChat = (chatId: string, chatTitle: string) => {
    setActiveChatId(chatId);
    showToast(
      isArabic ? `تم الانتقال إلى المحادثة: ${chatTitle}` : `Opened chat: ${chatTitle}`,
      '💬'
    );
    onClose();
  };

  const handleSendRealSystemPush = async () => {
    try {
      const perm = await backgroundNotificationDaemon.requestNotificationPermission();
      if (perm !== 'granted') {
        showToast(
          isArabic
            ? 'يرجى السماح بإذن الإشعارات من إعدادات المتصفح/الجهاز'
            : 'Please grant notification permission in browser/OS settings',
          '⚠️'
        );
        return;
      }

      await backgroundNotificationDaemon.sendSystemNotification({
        title: 'دكتوراه الفلسفة في ( القيادة التربوية)',
        body: 'Man... حتى رسالة مانقدر نسخها',
        chatTitle: 'دكتوراه الفلسفة في ( القياد...',
        senderName: 'Man',
        accountName: currentUser.name || 'بيان احمد',
        unreadCount: 5,
        unreadChatsCount: 3,
        isPlusStyle: true,
        avatar: 'https://images.unsplash.com/photo-1541339907198-e08756dedf3f?w=150&auto=format&fit=crop&q=80',
      });

      showToast(
        isArabic
          ? 'تم إرسال إشعار حقيقي إلى واجهة الجوال والنظام!'
          : 'Real system notification pushed to mobile device!',
        '🔔'
      );
    } catch {
      showToast(isArabic ? 'فشل إرسال الإشعار للنظام' : 'Failed to send system push', '❌');
    }
  };

  const handleClearAll = () => {
    setDismissedItems(new Set(['plus_1', 'plus_2', 'tg_1', 'tg_2', 'tg_3', 'tg_4']));
    showToast(isArabic ? 'تم مسح جميع الإشعارات' : 'All notifications cleared', '🧹');
  };

  return (
    <AnimatePresence>
      <div
        id="android-notification-shade-overlay"
        className="fixed inset-0 z-[10000] flex flex-col justify-start items-center bg-black/85 backdrop-blur-xl overflow-y-auto select-none"
        dir="rtl"
      >
        {/* Top Handle Bar */}
        <div className="w-full max-w-lg mx-auto flex flex-col items-center">
          <div className="w-12 h-1.5 bg-gray-500/50 rounded-full mt-2 mb-1 cursor-pointer hover:bg-gray-400" onClick={onClose} />
        </div>

        {/* Main Shade Container */}
        <motion.div
          initial={{ y: -50, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          exit={{ y: -50, opacity: 0 }}
          transition={{ type: 'spring', damping: 25, stiffness: 300 }}
          className="w-full max-w-lg mx-auto pb-12 px-2.5 sm:px-4 flex-1 flex flex-col font-sans"
        >
          {/* 1. Android Status Bar (Matching Screenshot: 4:15, Monday 31 August, 51% Battery, WiFi, etc.) */}
          <div className="flex items-center justify-between py-2.5 px-2 text-white/90 text-xs font-medium">
            {/* Right in RTL: Date & Time */}
            <div className="flex items-center gap-2">
              <span className="text-sm font-bold text-white tracking-tight">4:15</span>
              <span className="text-gray-300 text-[11.5px]">الاثنين، 31 أغسطس</span>
            </div>

            {/* Left in RTL: Icons & Battery */}
            <div className="flex items-center gap-2 text-gray-200">
              <span className="text-[11px] font-semibold text-white">51%</span>
              <BatteryCharging size={16} className="text-white fill-white/20" />
              <ArrowUpDown size={13} className="text-gray-300" />
              <Wifi size={14} className="text-white" />
            </div>
          </div>

          {/* 2. Quick Settings Tiles Grid (Screenshot Row 1) */}
          <div className="grid grid-cols-6 gap-2 my-2 px-1">
            {/* Tile 1: Wi-Fi (Active Blue) */}
            <button
              onClick={() => setWifiActive(!wifiActive)}
              className={`h-14 rounded-2xl flex flex-col items-center justify-center transition-all ${
                wifiActive ? 'bg-[#1a73e8] text-white shadow-md shadow-blue-900/40' : 'bg-[#2b3038] text-gray-400'
              }`}
            >
              <Wifi size={20} />
            </button>

            {/* Tile 2: Bluetooth */}
            <button
              onClick={() => setBluetoothActive(!bluetoothActive)}
              className={`h-14 rounded-2xl flex flex-col items-center justify-center transition-all ${
                bluetoothActive ? 'bg-[#1a73e8] text-white shadow-md' : 'bg-[#2b3038] text-gray-200'
              }`}
            >
              <Bluetooth size={20} />
            </button>

            {/* Tile 3: Airplane */}
            <button
              onClick={() => setAirplaneActive(!airplaneActive)}
              className={`h-14 rounded-2xl flex flex-col items-center justify-center transition-all ${
                airplaneActive ? 'bg-[#1a73e8] text-white' : 'bg-[#2b3038] text-gray-200'
              }`}
            >
              <Plane size={20} />
            </button>

            {/* Tile 4: Flashlight */}
            <button
              onClick={() => setFlashlightActive(!flashlightActive)}
              className={`h-14 rounded-2xl flex flex-col items-center justify-center transition-all ${
                flashlightActive ? 'bg-[#1a73e8] text-white' : 'bg-[#2b3038] text-gray-200'
              }`}
            >
              <Flashlight size={20} />
            </button>

            {/* Tile 5: Hotspot */}
            <button
              onClick={() => setHotspotActive(!hotspotActive)}
              className={`h-14 rounded-2xl flex flex-col items-center justify-center transition-all ${
                hotspotActive ? 'bg-[#1a73e8] text-white' : 'bg-[#2b3038] text-gray-200'
              }`}
            >
              <Radio size={20} />
            </button>

            {/* Tile 6: Mobile Data */}
            <button
              onClick={() => setMobileDataActive(!mobileDataActive)}
              className={`h-14 rounded-2xl flex flex-col items-center justify-center transition-all ${
                mobileDataActive ? 'bg-[#1a73e8] text-white' : 'bg-[#2b3038] text-gray-200'
              }`}
            >
              <ArrowUpDown size={20} />
            </button>
          </div>

          {/* Pull Bar Indicator */}
          <div className="flex justify-center my-1.5">
            <div className="w-10 h-1 bg-gray-600 rounded-full" />
          </div>

          {/* Section Title: المحادثات */}
          <div className="flex items-center justify-between px-2 pt-1 pb-2">
            <span className="text-sm font-bold text-white tracking-wide">المحادثات</span>
            <div className="flex items-center gap-2">
              <button
                onClick={handleSendRealSystemPush}
                className="flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-cyan-500/20 hover:bg-cyan-500/30 text-cyan-300 text-xs font-medium border border-cyan-500/30 transition-colors"
                title="إرسال إشعار فوري حقيقي للنظام"
              >
                <Smartphone size={13} />
                <span>إرسال إشعار للهاتف</span>
              </button>
              <button
                onClick={handleClearAll}
                className="text-xs text-gray-400 hover:text-white px-1.5 py-0.5 rounded transition-colors"
              >
                مسح الكل
              </button>
            </div>
          </div>

          {/* ========================================================================= */}
          {/* CARD 1: Plus Messenger Grouped Notification (Matching Screenshot 1st card) */}
          {/* ========================================================================= */}
          <div className="bg-white rounded-2xl overflow-hidden shadow-2xl mb-3 text-[#1f1f1f] border border-gray-100">
            {/* Card Header: Plus • بيان احمد • 279313 رسالة جديدة من 70 محادثة • الآن */}
            <div
              onClick={() => setPlusGroupExpanded(!plusGroupExpanded)}
              className="flex items-center justify-between px-4 py-3 cursor-pointer hover:bg-gray-50/80 transition-colors border-b border-gray-100"
            >
              <div className="flex items-center gap-2.5 min-w-0 flex-1">
                {/* Plus Logo Icon (Cyan Circle with Plus) */}
                <div className="w-6 h-6 rounded-full bg-emerald-600/10 border border-emerald-600/30 flex items-center justify-center text-emerald-600 font-bold shrink-0">
                  <span className="text-sm leading-none font-sans font-black">+</span>
                </div>

                <div className="flex items-center gap-1 text-[13px] text-gray-800 font-medium truncate">
                  <span className="font-bold text-gray-900">Plus</span>
                  <span className="text-gray-400">•</span>
                  <span className="font-semibold">{currentUser.name || 'بيان احمد'}</span>
                  <span className="text-gray-400">•</span>
                  <span className="text-gray-600 text-xs truncate">279313 رسالة جديدة من 70 محادثة</span>
                  <span className="text-gray-400">•</span>
                  <span className="text-gray-500 text-xs shrink-0">الآن</span>
                </div>
              </div>

              <div className="shrink-0 text-gray-400 mr-2">
                {plusGroupExpanded ? <ChevronUp size={18} /> : <ChevronDown size={18} />}
              </div>
            </div>

            {/* Sub-items in Plus Group */}
            {plusGroupExpanded && (
              <div className="divide-y divide-gray-100">
                {!dismissedItems.has('plus_1') && (
                  <div
                    onClick={() => handleOpenChat('chat_group_imam', 'جامعة الامام محمد بن سعود')}
                    className="p-3.5 px-4 hover:bg-gray-50 cursor-pointer transition-colors flex items-start gap-3"
                  >
                    <div className="w-10 h-10 rounded-full bg-[#1b3a6b] flex items-center justify-center text-white font-bold shrink-0 text-xs shadow-sm overflow-hidden">
                      <img
                        src="https://images.unsplash.com/photo-1541339907198-e08756dedf3f?w=150&auto=format&fit=crop&q=80"
                        alt="جامعة الامام"
                        className="w-full h-full object-cover"
                        referrerPolicy="no-referrer"
                      />
                    </div>
                    <div className="min-w-0 flex-1">
                      <div className="text-[13.5px] font-bold text-gray-900 truncate">
                        جامعة الامام محمد بن سعود :M: احد عنده قروب فقة العباد...
                      </div>
                      <div className="text-xs text-gray-700 font-medium mt-0.5 truncate dir-rtl">
                        التوفر المتق.طع 🇾🇪🇰🇼🇷🇺🇹🇭 ➖➖➖➖➖➖➖(WhatsApp)
                      </div>
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>

          {/* ========================================================================= */}
          {/* CARD 2: Telegram Official Grouped Notifications (Matching Screenshot 2nd card) */}
          {/* ========================================================================= */}
          <div className="bg-white rounded-2xl overflow-hidden shadow-2xl text-[#1f1f1f] border border-gray-100">
            {/* Card Header: تيليجرام • امل • 301 رسالة جديدة من 19 محادثة • 6 دقائق */}
            <div
              onClick={() => setTgGroupExpanded(!tgGroupExpanded)}
              className="flex items-center justify-between px-4 py-3 cursor-pointer hover:bg-gray-50/80 transition-colors border-b border-gray-100"
            >
              <div className="flex items-center gap-2.5 min-w-0 flex-1">
                {/* Telegram Paper Plane Icon */}
                <div className="w-6 h-6 rounded-full bg-[#2481cc]/15 flex items-center justify-center text-[#2481cc] shrink-0">
                  <Send size={13} className="rotate-45" />
                </div>

                <div className="flex items-center gap-1 text-[13px] text-gray-800 font-medium truncate">
                  <span className="font-bold text-gray-900">تيليجرام</span>
                  <span className="text-gray-400">•</span>
                  <span className="font-semibold">امل</span>
                  <span className="text-gray-400">•</span>
                  <span className="text-gray-600 text-xs truncate">301 رسالة جديدة من 19 محادثة</span>
                  <span className="text-gray-400">•</span>
                  <span className="text-gray-500 text-xs shrink-0">6 دقائق</span>
                </div>
              </div>

              <div className="shrink-0 text-gray-400 mr-2">
                {tgGroupExpanded ? <ChevronUp size={18} /> : <ChevronDown size={18} />}
              </div>
            </div>

            {/* Stacked sub-notifications (Matching items from Screenshot 2) */}
            {tgGroupExpanded && (
              <div className="divide-y divide-gray-100">
                {/* Item 1: دكتوراه الفلسفة في ( القياد... (Badge 5) */}
                {!dismissedItems.has('tg_1') && (
                  <div
                    onClick={() => handleOpenChat('chat_phd_education', 'دكتوراه الفلسفة في ( القيادة التربوية)')}
                    className="p-3.5 px-4 hover:bg-gray-50 cursor-pointer transition-colors flex items-center justify-between gap-3"
                  >
                    <div className="flex items-center gap-3 min-w-0 flex-1">
                      <div className="relative w-11 h-11 rounded-full overflow-hidden shrink-0 border border-gray-200 shadow-sm bg-blue-50">
                        <img
                          src="https://images.unsplash.com/photo-1523050854058-8df90110c9f1?w=150&auto=format&fit=crop&q=80"
                          alt="دكتوراه الفلسفة"
                          className="w-full h-full object-cover"
                          referrerPolicy="no-referrer"
                        />
                      </div>
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-1.5">
                          <span className="text-[13.5px] font-bold text-gray-900 truncate">
                            دكتوراه الفلسفة في ( القياد...
                          </span>
                          <span className="text-gray-400 text-xs">•</span>
                          <span className="text-gray-500 text-[11px] shrink-0">23دقيقة</span>
                        </div>
                        <div className="text-xs text-gray-700 mt-0.5 truncate font-medium">
                          Man... حتى رسالة مانقدر نسخها
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center gap-2 shrink-0">
                      <span className="w-5 h-5 rounded-full bg-[#1a73e8] text-white text-[11px] font-bold flex items-center justify-center shadow-sm">
                        5
                      </span>
                      <ChevronDown size={16} className="text-gray-400" />
                    </div>
                  </div>
                )}

                {/* Item 2: قناة تعلم البرمجة كورسات برمجة بايثون */}
                {!dismissedItems.has('tg_2') && (
                  <div
                    onClick={() => handleOpenChat('chat_python_soc', 'قناة تعلم البرمجة كورسات برمجة بايثون')}
                    className="p-3.5 px-4 hover:bg-gray-50 cursor-pointer transition-colors flex items-center justify-between gap-3"
                  >
                    <div className="flex items-center gap-3 min-w-0 flex-1">
                      <div className="w-11 h-11 rounded-full bg-indigo-950 flex items-center justify-center text-white shrink-0 overflow-hidden shadow-sm">
                        <img
                          src="https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=150&auto=format&fit=crop&q=80"
                          alt="برمجة"
                          className="w-full h-full object-cover"
                          referrerPolicy="no-referrer"
                        />
                      </div>
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-1.5">
                          <span className="text-[13.5px] font-bold text-gray-900 truncate">
                            قناة تعلم البرمجة كورسات برمجة بايثون جاف ذكاء اص...
                          </span>
                          <span className="text-gray-400 text-xs">•</span>
                          <span className="text-gray-500 text-[11px] shrink-0">6 دقائق</span>
                        </div>
                        <div className="text-xs text-gray-700 mt-0.5 truncate font-medium">
                          7- مركز العمليات الأمنية (SOC)...
                        </div>
                      </div>
                    </div>

                    <ChevronDown size={16} className="text-gray-400 shrink-0" />
                  </div>
                )}

                {/* Item 3: توصيل مشاوير (مكة-جدة-...) (Badge 2) */}
                {!dismissedItems.has('tg_3') && (
                  <div
                    onClick={() => handleOpenChat('chat_rides_makkah', 'توصيل مشاوير (مكة-جدة-...)')}
                    className="p-3.5 px-4 hover:bg-gray-50 cursor-pointer transition-colors flex items-center justify-between gap-3"
                  >
                    <div className="flex items-center gap-3 min-w-0 flex-1">
                      <div className="relative w-11 h-11 rounded-full overflow-hidden shrink-0 border border-amber-300 shadow-sm bg-black">
                        <img
                          src="https://images.unsplash.com/photo-1549317661-bd32c8ce0db2?w=150&auto=format&fit=crop&q=80"
                          alt="توصيل مشاوير"
                          className="w-full h-full object-cover"
                          referrerPolicy="no-referrer"
                        />
                        <div className="absolute bottom-0 right-0 w-3.5 h-3.5 rounded-full bg-cyan-500 text-white flex items-center justify-center text-[9px] font-bold">
                          +
                        </div>
                      </div>
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-1.5">
                          <span className="text-[13.5px] font-bold text-gray-900 truncate">
                            توصيل مشاوير (مكة-جدة-...
                          </span>
                          <span className="text-gray-400 text-xs">•</span>
                          <span className="text-gray-500 text-[11px] shrink-0">1دقيقة</span>
                        </div>
                        <div className="text-xs text-gray-700 mt-0.5 truncate font-medium">
                          حماية المجموعة: ارحب يا صديق ⭐...
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center gap-2 shrink-0">
                      <span className="w-5 h-5 rounded-full bg-[#1a73e8] text-white text-[11px] font-bold flex items-center justify-center shadow-sm">
                        2
                      </span>
                      <ChevronDown size={16} className="text-gray-400" />
                    </div>
                  </div>
                )}

                {/* Item 4: دكتوراه الفلسفة في ( القيادة التربوية) */}
                {!dismissedItems.has('tg_4') && (
                  <div
                    onClick={() => handleOpenChat('chat_phd_education', 'دكتوراه الفلسفة في ( القيادة التربوية)')}
                    className="p-3.5 px-4 hover:bg-gray-50 cursor-pointer transition-colors flex items-center justify-between gap-3"
                  >
                    <div className="flex items-center gap-3 min-w-0 flex-1">
                      <div className="relative w-11 h-11 rounded-full overflow-hidden shrink-0 border border-gray-200 shadow-sm bg-blue-50">
                        <img
                          src="https://images.unsplash.com/photo-1523050854058-8df90110c9f1?w=150&auto=format&fit=crop&q=80"
                          alt="دكتوراه الفلسفة"
                          className="w-full h-full object-cover"
                          referrerPolicy="no-referrer"
                        />
                        <div className="absolute bottom-0 right-0 w-3.5 h-3.5 rounded-full bg-cyan-500 text-white flex items-center justify-center text-[9px] font-bold">
                          +
                        </div>
                      </div>
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-1.5">
                          <span className="text-[13.5px] font-bold text-gray-900 truncate">
                            دكتوراه الفلسفة في ( القيادة التربوية)
                          </span>
                          <span className="text-gray-400 text-xs">•</span>
                          <span className="text-gray-500 text-[11px] shrink-0">23دقيقة</span>
                        </div>
                        <div className="text-xs text-gray-700 mt-0.5 truncate font-medium">
                          Man... حتى رسالة مانقدر نسخها
                        </div>
                      </div>
                    </div>

                    <ChevronDown size={16} className="text-gray-400 shrink-0" />
                  </div>
                )}
              </div>
            )}
          </div>

          {/* Bottom Footer Actions */}
          <div className="mt-4 flex items-center justify-between px-2 text-xs text-gray-400">
            <button
              onClick={onClose}
              className="px-4 py-2 rounded-xl bg-white/10 hover:bg-white/20 text-white font-medium transition-colors"
            >
              إغلاق واجهة الإشعارات
            </button>
            <div className="flex items-center gap-1.5 text-[11px] text-gray-400">
              <span>Android System UI 14</span>
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
            </div>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  );
};
