import React, { useState, useEffect, useRef, useMemo } from 'react';
import { io, Socket } from 'socket.io-client';
import { 
  Send, 
  Play, 
  Square, 
  RotateCcw, 
  Save, 
  RefreshCw, 
  Upload, 
  Trash2, 
  Edit3, 
  Search, 
  Radio, 
  CheckCircle2, 
  AlertCircle, 
  Clock, 
  Sliders, 
  BarChart3, 
  Terminal, 
  History, 
  LogOut, 
  User as UserIcon,
  X,
  FileText,
  Image as ImageIcon
} from 'lucide-react';

export interface LogEntry {
  id: string;
  level: 'INFO' | 'WARNING' | 'ERROR';
  msg: string;
  time: string;
}

export interface SentBatch {
  id: string;
  text: string;
  has_media?: boolean;
  media?: any[];
  target_chats?: string[];
  sent_count: number;
  failed_count?: number;
  sent_at: string;
  edited_at?: string;
  status?: string;
}

interface SendMonitorComponentProps {
  onBack?: () => void;
}

const DEFAULT_WATCH_WORDS = [
  'اريد مساعدة',
  'ابي مساعدة',
  'من يسوي تكليف',
  'من يحل',
  'عندي بحث',
  'معي واجب',
  'عندي اسايمنت',
  'من يسوي اسايمنت',
  'ابي سكليف',
  'ابي عذر',
  'من يسوي سكليف',
  'ابي شخص مضمون',
  'ابي مختص',
  'هيليب',
  'من يستطيع',
  'تعرفون احد',
  'تعرفون شخص',
  'من يساعدني',
  'من يعرف مختص',
  'مين يعرف يحل واجب',
  'من يحل واجبات الجامعه',
  'أحتاج مساعدتكم',
  'ابي احد يسوي بحث',
  'مين يعرف مختص',
  'من يعرف احد كويس'
].join('\n');

export const SendMonitorComponent: React.FC<SendMonitorComponentProps> = ({ onBack }) => {
  // Socket & Connection State
  const [isConnected, setIsConnected] = useState<boolean>(false);
  const [isRunning, setIsRunning] = useState<boolean>(true); // Monitoring active by default!
  const [userAccountName, setUserAccountName] = useState<string>('متصل (تيليجرام)');
  const [isLoggedIn, setIsLoggedIn] = useState<boolean>(true);

  // Settings State
  const [sendType, setSendType] = useState<'manual' | 'scheduled'>('scheduled');
  const [messageText, setMessageText] = useState<string>('');
  const [groupsList, setGroupsList] = useState<string>('');
  const [watchWords, setWatchWords] = useState<string>(DEFAULT_WATCH_WORDS);
  const [intervalSeconds, setIntervalSeconds] = useState<number>(3600);
  const [scheduleDuration, setScheduleDuration] = useState<number>(0);
  const [sanitizeMode, setSanitizeMode] = useState<'salam' | 'smart' | 'skip' | 'off'>('salam');
  const [smartRequired, setSmartRequired] = useState<number>(3);
  const [images, setImages] = useState<Array<{ name: string; data: string; size?: number }>>([]);

  // Stats State
  const [sentCount, setSentCount] = useState<number>(0);
  const [errorCount, setErrorCount] = useState<number>(0);
  const [nextSendRemaining, setNextSendRemaining] = useState<number>(3600);
  const [scheduleRemaining, setScheduleRemaining] = useState<number>(0);

  // Logs State
  const [logs, setLogs] = useState<LogEntry[]>([
    {
      id: 'init_1',
      level: 'INFO',
      msg: '🟢 خادم الإرسال والمراقبة نشط وجاهز - المراقبة تعمل افتراضياً عبر بروتوكول GramJS',
      time: new Date().toLocaleTimeString('ar-SA')
    }
  ]);
  const [logFilter, setLogFilter] = useState<'all' | 'INFO' | 'WARNING' | 'ERROR'>('all');
  const [logSearch, setLogSearch] = useState<string>('');
  const logContainerRef = useRef<HTMLDivElement>(null);

  // Batches State
  const [batches, setBatches] = useState<SentBatch[]>([]);
  const [isLoadingBatches, setIsLoadingBatches] = useState<boolean>(false);
  const [editingBatch, setEditingBatch] = useState<SentBatch | null>(null);
  const [newBatchText, setNewBatchText] = useState<string>('');
  const [isActionLoading, setIsActionLoading] = useState<boolean>(false);

  const socketRef = useRef<Socket | null>(null);

  // Helper to append log
  const addLog = (level: 'INFO' | 'WARNING' | 'ERROR', msg: string, time?: string) => {
    const timeStr = time || new Date().toLocaleTimeString('ar-SA');
    setLogs((prev) => [
      ...prev,
      {
        id: `log_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`,
        level,
        msg,
        time: timeStr
      }
    ]);
  };

  // 1. Socket.IO Lifecycle
  useEffect(() => {
    const socket = io({
      transports: ['websocket', 'polling'],
      reconnectionAttempts: 10,
      timeout: 10000
    });
    socketRef.current = socket;

    socket.on('connect', () => {
      setIsConnected(true);
      addLog('INFO', '✅ متصل بالخادم في الوقت الفعلي (Socket.IO)');
    });

    socket.on('disconnect', () => {
      setIsConnected(false);
      addLog('WARNING', '⚠️ انقطع الاتصال بالخادم، جاري إعادة المحاولة تلقائياً...');
    });

    socket.on('monitoring_status', (data: { is_running: boolean }) => {
      if (data && typeof data.is_running === 'boolean') {
        setIsRunning(data.is_running);
      }
    });

    socket.on('stats_update', (data: { sent?: number; errors?: number }) => {
      if (data?.sent !== undefined) setSentCount(data.sent);
      if (data?.errors !== undefined) setErrorCount(data.errors);
    });

    socket.on('heartbeat', (data: { status?: string; next_send_remaining?: number; schedule_remaining?: number }) => {
      if (data?.status) {
        setIsRunning(data.status === 'active');
      }
      if (data?.next_send_remaining !== undefined) {
        setNextSendRemaining(data.next_send_remaining);
      }
      if (data?.schedule_remaining !== undefined) {
        setScheduleRemaining(data.schedule_remaining);
      }
    });

    socket.on('log_update', (data: { level?: 'INFO' | 'WARNING' | 'ERROR'; msg?: string; message?: string; time?: string }) => {
      if (data) {
        const lvl = data.level || 'INFO';
        const m = data.msg || data.message || '';
        if (m) addLog(lvl, m, data.time);
      }
    });

    socket.on('live_log', (data: { level?: 'INFO' | 'WARNING' | 'ERROR'; msg?: string; message?: string; time?: string }) => {
      if (data) {
        const lvl = data.level || 'INFO';
        const m = data.msg || data.message || '';
        if (m) addLog(lvl, m, data.time);
      }
    });

    socket.on('batch_saved', (batch: SentBatch) => {
      addLog('INFO', `📦 تم حفظ دفعة إرسال جديدة: [${batch.id}]`);
      loadBatches();
    });

    socket.on('batch_edited', (batch: SentBatch) => {
      addLog('INFO', `✏️ تم تحديث الدفعة [${batch.id}] بنجاح`);
      loadBatches();
    });

    socket.on('batch_deleted', (data: { id: string }) => {
      addLog('WARNING', `🗑️ تم حذف الدفعة [${data?.id}] وسحب رسائلها`);
      loadBatches();
    });

    // Initial Data Fetching
    loadSettings();
    loadBatches();
    checkLoginStatus();

    return () => {
      socket.disconnect();
    };
  }, []);

  // Auto-scroll logs to bottom
  useEffect(() => {
    if (logContainerRef.current) {
      logContainerRef.current.scrollTop = logContainerRef.current.scrollHeight;
    }
  }, [logs]);

  // Load Settings from Server
  const loadSettings = async () => {
    try {
      const res = await fetch('/api/load_backup_settings');
      if (res.ok) {
        const data = await res.json();
        if (data.settings) {
          const s = data.settings;
          if (s.send_type) setSendType(s.send_type);
          if (s.message !== undefined) setMessageText(s.message);
          if (s.groups) {
            setGroupsList(Array.isArray(s.groups) ? s.groups.join('\n') : s.groups);
          }
          if (s.watch_words) {
            setWatchWords(Array.isArray(s.watch_words) ? s.watch_words.join('\n') : s.watch_words);
          }
          if (s.interval_seconds) setIntervalSeconds(s.interval_seconds);
          if (s.schedule_duration !== undefined) setScheduleDuration(s.schedule_duration);
          if (s.sanitize_mode) setSanitizeMode(s.sanitize_mode);
          if (s.smart_required_messages) setSmartRequired(s.smart_required_messages);
        }
      }
    } catch (e) {
      console.warn('Could not load backup settings:', e);
    }
  };

  // Check Login Status
  const checkLoginStatus = async () => {
    try {
      const res = await fetch('/api/get_login_status');
      if (res.ok) {
        const data = await res.json();
        setIsLoggedIn(Boolean(data.logged_in));
        if (data.account_name) {
          setUserAccountName(data.account_name);
        }
        if (typeof data.is_running === 'boolean') {
          setIsRunning(data.is_running);
        }
      }
    } catch (e) {
      console.warn('Could not verify login status:', e);
    }
  };

  // Load Batches
  const loadBatches = async () => {
    setIsLoadingBatches(true);
    try {
      const res = await fetch('/api/sent_batches');
      if (res.ok) {
        const data = await res.json();
        if (Array.isArray(data.batches)) {
          setBatches(data.batches);
        }
      }
    } catch (e) {
      console.warn('Could not fetch sent batches:', e);
    } finally {
      setIsLoadingBatches(false);
    }
  };

  // Handle Save Settings
  const handleSaveSettings = async () => {
    setIsActionLoading(true);
    try {
      const payload = {
        send_type: sendType,
        message: messageText,
        groups: groupsList.split('\n').map((g) => g.trim()).filter(Boolean),
        watch_words: watchWords.split('\n').map((w) => w.trim()).filter(Boolean),
        interval_seconds: Number(intervalSeconds) || 3600,
        schedule_duration: Number(scheduleDuration) || 0,
        sanitize_mode: sanitizeMode,
        smart_required_messages: Number(smartRequired) || 3
      };

      const res = await fetch('/api/save_settings', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      const data = await res.json();
      if (data.success) {
        addLog('INFO', '💾 تم حفظ جميع إعدادات الإرسال والمراقبة بنجاح في الخادم');
      } else {
        addLog('ERROR', `❌ تعذر حفظ الإعدادات: ${data.message || 'خطأ غير معروف'}`);
      }
    } catch (e: any) {
      addLog('ERROR', `❌ خطأ في الاتصال أثناء حفظ الإعدادات: ${e.message}`);
    } finally {
      setIsActionLoading(false);
    }
  };

  // Handle Send Now
  const handleSendNow = async () => {
    const rawGroups = groupsList.split('\n').map((g) => g.trim()).filter(Boolean);
    if (!messageText.trim() && images.length === 0) {
      addLog('WARNING', '⚠️ يرجى إدخال نص الرسالة أو اختيار صورة واحدة على الأقل');
      return;
    }
    if (rawGroups.length === 0) {
      addLog('WARNING', '⚠️ يرجى إدخال روابط أو معرفات المجموعات المستهدفة');
      return;
    }

    setIsActionLoading(true);
    addLog('INFO', `🚀 جاري إرسال الرسالة إلى ${rawGroups.length} مجموعة بنمط (${sanitizeMode})...`);

    try {
      const payload = {
        message: messageText,
        groups: rawGroups,
        images: images,
        send_to_all: false,
        action: sanitizeMode,
        interval_seconds: 2
      };

      const res = await fetch('/api/send_now', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      const data = await res.json();
      if (data.success) {
        addLog('INFO', `✅ ${data.message || 'تم بدء عملية الإرسال الفوري بنجاح'}`);
        loadBatches();
      } else {
        addLog('ERROR', `❌ فشل الإرسال: ${data.message || 'خطأ'}`);
      }
    } catch (e: any) {
      addLog('ERROR', `❌ خطأ أثناء الإرسال: ${e.message}`);
    } finally {
      setIsActionLoading(false);
    }
  };

  // Handle Start Monitoring (Even though it runs by default, allows re-confirming)
  const handleStartMonitoring = async () => {
    try {
      const res = await fetch('/api/start_monitoring', { method: 'POST' });
      const data = await res.json();
      if (data.success) {
        setIsRunning(true);
        addLog('INFO', '🟢 تم تنشيط نظام المراقبة اللحظية للكلمات المفتاحية بنجاح');
      }
    } catch (e: any) {
      addLog('ERROR', `تعذر بدء المراقبة: ${e.message}`);
    }
  };

  // Handle Stop Monitoring
  const handleStopMonitoring = async () => {
    try {
      const res = await fetch('/api/stop_monitoring', { method: 'POST' });
      const data = await res.json();
      if (data.success) {
        setIsRunning(false);
        addLog('WARNING', '⏹ تم إيقاف المراقبة مؤقتاً');
      }
    } catch (e: any) {
      addLog('ERROR', `تعذر إيقاف المراقبة: ${e.message}`);
    }
  };

  // Handle Resume
  const handleResume = async () => {
    try {
      const res = await fetch('/api/resume_scheduled', { method: 'POST' });
      const data = await res.json();
      if (data.success) {
        setIsRunning(true);
        addLog('INFO', '🔄 تم استئناف الإرسال المجدول بنجاح');
      }
    } catch (e: any) {
      addLog('ERROR', `تعذر استئناف الإرسال: ${e.message}`);
    }
  };

  // Handle Fetch All Groups
  const handleFetchAllGroups = async () => {
    setIsActionLoading(true);
    addLog('INFO', '🔍 جاري جلب جميع المجموعات والقنوات من حسابك في تيليجرام...');
    try {
      const res = await fetch('/api/get_all_groups');
      const data = await res.json();
      if (data.success && Array.isArray(data.groups) && data.groups.length > 0) {
        const links = data.groups.map((g: any) => g.link || `@${g.id}`).join('\n');
        setGroupsList(links);
        addLog('INFO', `✅ تم جلب ${data.groups.length} مجموعة بنجاح`);
      } else {
        addLog('WARNING', data.message || 'لم يتم العثور على مجموعات مفتوحة في الحساب');
      }
    } catch (e: any) {
      addLog('ERROR', `❌ تعذر جلب المجموعات: ${e.message}`);
    } finally {
      setIsActionLoading(false);
    }
  };

  // Handle Image Upload
  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;

    const newImgs: Array<{ name: string; data: string; size?: number }> = [];
    Array.from(files).forEach((file) => {
      const reader = new FileReader();
      reader.onload = (ev) => {
        if (ev.target?.result) {
          newImgs.push({
            name: file.name,
            data: ev.target.result as string,
            size: file.size
          });
          if (newImgs.length === files.length) {
            setImages((prev) => [...prev, ...newImgs]);
            addLog('INFO', `🖼 تم إرفاق ${newImgs.length} صورة للإرسال`);
          }
        }
      };
      reader.readAsDataURL(file);
    });
  };

  // Remove Image
  const removeImage = (index: number) => {
    setImages((prev) => prev.filter((_, i) => i !== index));
  };

  // Handle Edit Batch
  const handleEditBatch = async () => {
    if (!editingBatch || !newBatchText.trim()) return;
    setIsActionLoading(true);
    try {
      const res = await fetch('/api/edit_batch', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          batch_id: editingBatch.id,
          new_text: newBatchText.trim()
        })
      });
      const data = await res.json();
      if (data.success) {
        addLog('INFO', `✏️ تم تعديل محتوى الدفعة [${editingBatch.id}] في جميع المجموعات`);
        setEditingBatch(null);
        setNewBatchText('');
        loadBatches();
      } else {
        addLog('ERROR', `❌ تعذر التعديل: ${data.message}`);
      }
    } catch (e: any) {
      addLog('ERROR', `خطأ في تعديل الدفعة: ${e.message}`);
    } finally {
      setIsActionLoading(false);
    }
  };

  // Handle Delete Batch
  const handleDeleteBatch = async (batchId: string) => {
    if (!window.confirm('هل أنت متأكد من حذف وسحب رسائل هذه الدفعة من المجموعات؟')) return;
    try {
      const res = await fetch('/api/delete_batch', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ batch_id: batchId })
      });
      const data = await res.json();
      if (data.success) {
        addLog('INFO', `🗑️ تم حذف الدفعة [${batchId}] وسحب رسائلها بنجاح`);
        loadBatches();
      } else {
        addLog('ERROR', `تعذر الحذف: ${data.message}`);
      }
    } catch (e: any) {
      addLog('ERROR', `خطأ أثناء الحذف: ${e.message}`);
    }
  };

  // Handle Logout
  const handleLogout = async () => {
    if (!window.confirm('هل أنت متأكد من تسجيل الخروج من تيليجرام؟')) return;
    try {
      const res = await fetch('/api/user_logout', { method: 'POST' });
      if (res.ok) {
        setIsLoggedIn(false);
        setUserAccountName('غير متصل');
        addLog('INFO', '👋 تم تسجيل الخروج بنجاح');
      }
    } catch (e: any) {
      addLog('ERROR', `خطأ في تسجيل الخروج: ${e.message}`);
    }
  };

  // Filtered Logs
  const filteredLogs = useMemo(() => {
    return logs.filter((log) => {
      const matchLevel = logFilter === 'all' || log.level === logFilter;
      const matchSearch = !logSearch.trim() || log.msg.toLowerCase().includes(logSearch.toLowerCase());
      return matchLevel && matchSearch;
    });
  }, [logs, logFilter, logSearch]);

  return (
    <div id="sendMonitorRoot" dir="rtl" className="w-full flex flex-col gap-4 text-zinc-100 font-sans select-none">
      {/* 1. Header & Navigation Top Bar */}
      <div className="flex flex-wrap items-center justify-between gap-3 p-4 bg-zinc-900/90 border border-zinc-800 rounded-2xl shadow-lg backdrop-blur-md">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-blue-500/20 border border-blue-500/40 flex items-center justify-center text-blue-400 font-bold shadow-inner">
            <Send className="w-5 h-5" />
          </div>
          <div>
            <h1 className="text-lg sm:text-xl font-black text-white flex items-center gap-2 m-0">
              <span>الإرسال والمراقبة</span>
              <span className="text-xs px-2.5 py-0.5 rounded-full bg-blue-500/20 text-blue-400 border border-blue-500/30 font-bold">
                مركز سرعة إنجاز
              </span>
            </h1>
            <p className="text-xs text-zinc-400 m-0">
              منظومة الأتمتة الموحدة لإرسال الرسائل وجدولتها مع رادار المراقبة التلقائي المباشر
            </p>
          </div>
        </div>

        {/* Status Badges & Controls */}
        <div className="flex flex-wrap items-center gap-2">
          {/* Connection Status Badge */}
          <span
            id="connectionStatus"
            className={`px-3 py-1 text-xs font-bold rounded-lg border flex items-center gap-1.5 transition-all ${
              isConnected
                ? 'bg-emerald-500/15 border-emerald-500/40 text-emerald-400'
                : 'bg-rose-500/15 border-rose-500/40 text-rose-400'
            }`}
          >
            <span className={`w-2 h-2 rounded-full ${isConnected ? 'bg-emerald-400 animate-pulse' : 'bg-rose-400'}`} />
            {isConnected ? 'متصل بالخادم' : 'غير متصل'}
          </span>

          {/* Monitor Status Badge */}
          <span
            id="monitorStatus"
            className={`px-3 py-1 text-xs font-bold rounded-lg border flex items-center gap-1.5 transition-all ${
              isRunning
                ? 'bg-cyan-500/15 border-cyan-500/40 text-cyan-400'
                : 'bg-zinc-800 border-zinc-700 text-zinc-400'
            }`}
          >
            <Radio className={`w-3.5 h-3.5 ${isRunning ? 'animate-spin text-cyan-400' : ''}`} />
            {isRunning ? 'مراقبة: نشطة 🟢' : 'مراقبة: متوقفة ⏹'}
          </span>

          {/* User Display Badge */}
          <span
            id="userDisplay"
            className="px-3 py-1 text-xs font-bold rounded-lg bg-indigo-500/15 border border-indigo-500/30 text-indigo-300 flex items-center gap-1.5"
          >
            <UserIcon className="w-3.5 h-3.5" />
            <span>{userAccountName}</span>
          </span>

          {/* Logout Button */}
          <button
            id="logoutBtn"
            onClick={handleLogout}
            className="px-3 py-1 text-xs font-bold rounded-lg bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 border border-rose-500/30 flex items-center gap-1 transition-colors cursor-pointer"
          >
            <LogOut className="w-3.5 h-3.5" />
            <span>تسجيل خروج</span>
          </button>

          {onBack && (
            <button
              onClick={onBack}
              className="p-1.5 rounded-lg bg-zinc-800 hover:bg-zinc-700 text-zinc-300 border border-zinc-700 transition-colors"
              title="رجوع"
            >
              <X className="w-4 h-4" />
            </button>
          )}
        </div>
      </div>

      {/* 2. Main Workspace (2-Column Grid) */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-4 items-start">
        {/* RIGHT COLUMN: Settings Card + Stats Card (col-lg-5) */}
        <div className="lg:col-span-5 flex flex-col gap-4">
          {/* Settings Card */}
          <div className="bg-zinc-900/90 border border-zinc-800 rounded-2xl p-4 sm:p-5 shadow-xl flex flex-col gap-4">
            <div className="flex items-center justify-between border-b border-zinc-800 pb-3">
              <div className="flex items-center gap-2">
                <Sliders className="w-4 h-4 text-blue-400" />
                <h2 className="text-sm font-black text-white m-0">إعدادات الإرسال والمراقبة</h2>
              </div>
              <span className="text-[11px] text-zinc-400 font-mono">نظام الإنجاز الذكي</span>
            </div>

            <form id="settingsForm" onSubmit={(e) => { e.preventDefault(); handleSaveSettings(); }} className="flex flex-col gap-3.5">
              {/* Send Type Selector */}
              <div>
                <label className="block text-xs font-bold text-zinc-300 mb-1">نوع الإرسال</label>
                <select
                  id="sendType"
                  value={sendType}
                  onChange={(e) => setSendType(e.target.value as 'manual' | 'scheduled')}
                  className="w-full bg-zinc-950 border border-zinc-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-blue-500"
                >
                  <option value="manual">يدوي (فوري الآن)</option>
                  <option value="scheduled">مجدول (دوري متكرر)</option>
                </select>
              </div>

              {/* Message Textarea */}
              <div>
                <label className="block text-xs font-bold text-zinc-300 mb-1">نص الرسالة</label>
                <textarea
                  id="messageText"
                  rows={3}
                  value={messageText}
                  onChange={(e) => setMessageText(e.target.value)}
                  placeholder="اكتب رسالتك هنا..."
                  className="w-full bg-zinc-950 border border-zinc-700 rounded-xl p-3 text-xs text-white placeholder-zinc-500 focus:outline-none focus:border-blue-500 resize-none font-sans"
                />
              </div>

              {/* Target Groups Textarea & Fetch All Button */}
              <div>
                <div className="flex items-center justify-between mb-1">
                  <label className="text-xs font-bold text-zinc-300">قائمة المجموعات (رابط أو معرف بكل سطر)</label>
                  <button
                    type="button"
                    id="fetchAllGroupsBtn"
                    onClick={handleFetchAllGroups}
                    disabled={isActionLoading}
                    className="text-[11px] text-blue-400 hover:text-blue-300 flex items-center gap-1 font-bold bg-blue-500/10 hover:bg-blue-500/20 px-2 py-0.5 rounded-lg border border-blue-500/30 transition-colors"
                  >
                    <RefreshCw className={`w-3 h-3 ${isActionLoading ? 'animate-spin' : ''}`} />
                    <span>جلب كل المجموعات</span>
                  </button>
                </div>
                <textarea
                  id="groupsList"
                  rows={4}
                  value={groupsList}
                  onChange={(e) => setGroupsList(e.target.value)}
                  placeholder="https://t.me/group1&#10;https://t.me/group2&#10;@channel_username"
                  className="w-full bg-zinc-950 border border-zinc-700 rounded-xl p-3 text-xs text-white placeholder-zinc-500 focus:outline-none focus:border-blue-500 font-mono resize-none text-left"
                  dir="ltr"
                />
              </div>

              {/* Watch Words Textarea */}
              <div>
                <label className="block text-xs font-bold text-zinc-300 mb-1">
                  كلمات المراقبة المستهدفة (كل كلمة/جملة في سطر)
                </label>
                <textarea
                  id="watchWords"
                  rows={3}
                  value={watchWords}
                  onChange={(e) => setWatchWords(e.target.value)}
                  placeholder="اريد مساعدة&#10;من يسوي تكليف&#10;من يحل"
                  className="w-full bg-zinc-950 border border-zinc-700 rounded-xl p-3 text-xs text-white placeholder-zinc-500 focus:outline-none focus:border-blue-500 font-mono resize-none"
                />
              </div>

              {/* Two-Column Inputs (Interval Seconds & Schedule Duration) */}
              <div className="grid grid-cols-2 gap-2.5">
                <div>
                  <label className="block text-xs font-bold text-zinc-300 mb-1">الفاصل الزمني (ثوانٍ)</label>
                  <input
                    type="number"
                    id="intervalSeconds"
                    value={intervalSeconds}
                    onChange={(e) => setIntervalSeconds(Number(e.target.value))}
                    min={60}
                    className="w-full bg-zinc-950 border border-zinc-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-blue-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-zinc-300 mb-1">مدة الإرسال (ساعات، 0 = دائم)</label>
                  <input
                    type="number"
                    id="scheduleDuration"
                    value={scheduleDuration}
                    onChange={(e) => setScheduleDuration(Number(e.target.value))}
                    min={0}
                    step={0.5}
                    className="w-full bg-zinc-950 border border-zinc-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-blue-500"
                  />
                </div>
              </div>

              {/* Two-Column Inputs (Sanitize Mode & Smart Required) */}
              <div className="grid grid-cols-2 gap-2.5">
                <div>
                  <label className="block text-xs font-bold text-zinc-300 mb-1">نمط التنقية الذكية</label>
                  <select
                    id="sanitizeMode"
                    value={sanitizeMode}
                    onChange={(e) => setSanitizeMode(e.target.value as any)}
                    className="w-full bg-zinc-950 border border-zinc-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-blue-500"
                  >
                    <option value="salam">السلام عليكم (ذكي ومضمون)</option>
                    <option value="smart">تنقية ذكية</option>
                    <option value="skip">تخطي المحمي</option>
                    <option value="off">إلغاء التنقية</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-bold text-zinc-300 mb-1">الحد الأدنى للرسائل</label>
                  <input
                    type="number"
                    id="smartRequired"
                    value={smartRequired}
                    onChange={(e) => setSmartRequired(Number(e.target.value))}
                    min={1}
                    max={20}
                    className="w-full bg-zinc-950 border border-zinc-700 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-blue-500"
                  />
                </div>
              </div>

              {/* Image Input & Preview */}
              <div>
                <label className="block text-xs font-bold text-zinc-300 mb-1">إرفاق صور (اختياري)</label>
                <div className="flex items-center gap-2">
                  <label className="flex-1 cursor-pointer bg-zinc-950 hover:bg-zinc-800 border border-dashed border-zinc-700 hover:border-zinc-500 rounded-xl p-2.5 flex items-center justify-center gap-2 text-xs text-zinc-400 transition-colors">
                    <Upload className="w-4 h-4 text-blue-400" />
                    <span>اختر صوراً للإرسال</span>
                    <input
                      type="file"
                      id="imageInput"
                      accept="image/*"
                      multiple
                      onChange={handleImageChange}
                      className="hidden"
                    />
                  </label>
                </div>

                {/* Preview Container */}
                <div id="imagePreview" className="flex flex-wrap gap-2 mt-2">
                  {images.map((img, idx) => (
                    <div key={idx} className="relative group w-14 h-14 rounded-lg overflow-hidden border border-zinc-700 bg-zinc-950">
                      <img src={img.data} alt={img.name} className="w-full h-full object-cover" />
                      <button
                        type="button"
                        onClick={() => removeImage(idx)}
                        className="absolute inset-0 bg-black/70 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity text-rose-400"
                        title="حذف الصورة"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  ))}
                </div>
              </div>

              {/* Action Buttons Matrix */}
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-2 pt-2 border-t border-zinc-800">
                {/* Start Monitoring Button: By default active, shows pulse! */}
                <button
                  type="button"
                  id="startMonitoringBtn"
                  onClick={handleStartMonitoring}
                  className={`py-2 px-3 rounded-xl text-xs font-black flex items-center justify-center gap-1.5 transition-all shadow-md ${
                    isRunning
                      ? 'bg-emerald-600/30 border border-emerald-500/50 text-emerald-300 ring-1 ring-emerald-500/30'
                      : 'bg-emerald-600 hover:bg-emerald-500 text-white'
                  }`}
                  title="المراقبة تعمل تلقائياً في الخلفية"
                >
                  <Play className={`w-3.5 h-3.5 ${isRunning ? 'fill-emerald-400 text-emerald-400' : ''}`} />
                  <span>{isRunning ? 'المراقبة: نشطة 🟢' : 'بدء المراقبة'}</span>
                </button>

                {/* Stop Monitoring Button */}
                <button
                  type="button"
                  id="stopMonitoringBtn"
                  onClick={handleStopMonitoring}
                  className="py-2 px-3 rounded-xl bg-zinc-800 hover:bg-zinc-700 text-zinc-300 border border-zinc-700 text-xs font-bold flex items-center justify-center gap-1.5 transition-colors"
                >
                  <Square className="w-3.5 h-3.5 text-rose-400" />
                  <span>إيقاف</span>
                </button>

                {/* Send Now Button */}
                <button
                  type="button"
                  id="sendNowBtn"
                  onClick={handleSendNow}
                  disabled={isActionLoading}
                  className="py-2 px-3 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-xs font-black flex items-center justify-center gap-1.5 transition-all shadow-md disabled:opacity-50"
                >
                  <Send className="w-3.5 h-3.5" />
                  <span>إرسال الآن</span>
                </button>

                {/* Resume Button */}
                <button
                  type="button"
                  id="resumeBtn"
                  onClick={handleResume}
                  className="py-2 px-3 rounded-xl bg-amber-600/20 hover:bg-amber-600/30 text-amber-300 border border-amber-500/30 text-xs font-bold flex items-center justify-center gap-1.5 transition-colors"
                >
                  <RotateCcw className="w-3.5 h-3.5 text-amber-400" />
                  <span>استئناف</span>
                </button>

                {/* Save Settings Button */}
                <button
                  type="submit"
                  id="saveSettingsBtn"
                  disabled={isActionLoading}
                  className="col-span-2 sm:col-span-2 py-2 px-3 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-black flex items-center justify-center gap-1.5 transition-all shadow-md disabled:opacity-50"
                >
                  <Save className="w-3.5 h-3.5" />
                  <span>حفظ الإعدادات</span>
                </button>
              </div>
            </form>
          </div>

          {/* Stats Card */}
          <div className="bg-zinc-900/90 border border-zinc-800 rounded-2xl p-4 sm:p-5 shadow-xl flex flex-col gap-3">
            <div className="flex items-center justify-between border-b border-zinc-800 pb-2.5">
              <div className="flex items-center gap-2">
                <BarChart3 className="w-4 h-4 text-emerald-400" />
                <h3 className="text-xs font-black text-white m-0">الإحصائيات المباشرة</h3>
              </div>
              <span className="text-[10px] text-zinc-500">تحديث فوري</span>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
              {/* Sent Count */}
              <div className="bg-zinc-950 border border-zinc-800 rounded-xl p-3 flex flex-col items-center justify-center">
                <span className="text-[11px] text-zinc-400 mb-1">المرسلة</span>
                <span id="sentCount" className="text-xl font-black text-emerald-400 font-mono">
                  {sentCount}
                </span>
              </div>

              {/* Error Count */}
              <div className="bg-zinc-950 border border-zinc-800 rounded-xl p-3 flex flex-col items-center justify-center">
                <span className="text-[11px] text-zinc-400 mb-1">الأخطاء</span>
                <span id="errorCount" className="text-xl font-black text-rose-400 font-mono">
                  {errorCount}
                </span>
              </div>

              {/* Next Send Remaining */}
              <div className="bg-zinc-950 border border-zinc-800 rounded-xl p-3 flex flex-col items-center justify-center">
                <span className="text-[11px] text-zinc-400 mb-1">الإرسال القادم</span>
                <span id="nextSendRemaining" className="text-sm font-black text-blue-400 font-mono">
                  {Math.floor(nextSendRemaining / 60)}:{(nextSendRemaining % 60).toString().padStart(2, '0')}
                </span>
              </div>

              {/* Schedule Remaining */}
              <div className="bg-zinc-950 border border-zinc-800 rounded-xl p-3 flex flex-col items-center justify-center">
                <span className="text-[11px] text-zinc-400 mb-1">المتبقي للمهمة</span>
                <span id="scheduleRemaining" className="text-sm font-black text-amber-400 font-mono">
                  {scheduleRemaining > 0 ? `${Math.floor(scheduleRemaining / 3600)}س` : 'دائم ∞'}
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* LEFT COLUMN: Live Event Log Card + Sent Batches Card (col-lg-7) */}
        <div className="lg:col-span-7 flex flex-col gap-4">
          {/* Live Event Log Card */}
          <div className="bg-zinc-900/90 border border-zinc-800 rounded-2xl p-4 sm:p-5 shadow-xl flex flex-col gap-3">
            {/* Header with Log Count, Clear Logs & Filters */}
            <div className="flex flex-wrap items-center justify-between gap-2 border-b border-zinc-800 pb-3">
              <div className="flex items-center gap-2">
                <Terminal className="w-4 h-4 text-cyan-400" />
                <h3 className="text-xs font-black text-white m-0">سجل الأحداث المباشر</h3>
                <span id="logCount" className="px-2 py-0.5 rounded-full bg-zinc-800 text-zinc-300 text-[10px] font-mono">
                  {filteredLogs.length}
                </span>
              </div>

              <div className="flex items-center gap-2">
                <button
                  id="clearLogsBtn"
                  onClick={() => setLogs([])}
                  className="px-2.5 py-1 text-[11px] rounded-lg bg-zinc-800 hover:bg-zinc-700 text-zinc-400 hover:text-zinc-200 border border-zinc-700 transition-colors"
                >
                  مسح السجل
                </button>
              </div>
            </div>

            {/* Filter Badges & Search */}
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div className="flex items-center gap-1.5 text-[11px]">
                {(['all', 'INFO', 'WARNING', 'ERROR'] as const).map((filter) => (
                  <button
                    key={filter}
                    data-filter={filter}
                    onClick={() => setLogFilter(filter)}
                    className={`filter-badge px-2.5 py-1 rounded-lg font-bold border transition-all cursor-pointer ${
                      logFilter === filter
                        ? 'bg-blue-600 text-white border-blue-500 shadow-sm'
                        : 'bg-zinc-950 text-zinc-400 border-zinc-800 hover:text-zinc-200'
                    }`}
                  >
                    {filter === 'all' ? 'الكل' : filter}
                  </button>
                ))}
              </div>

              <div className="relative w-full sm:w-44">
                <Search className="w-3.5 h-3.5 text-zinc-500 absolute right-2.5 top-2.5" />
                <input
                  type="text"
                  id="logSearch"
                  value={logSearch}
                  onChange={(e) => setLogSearch(e.target.value)}
                  placeholder="بحث في السجل..."
                  className="w-full bg-zinc-950 border border-zinc-800 rounded-lg pr-8 pl-2 py-1 text-xs text-zinc-200 focus:outline-none focus:border-zinc-600 font-sans"
                />
              </div>
            </div>

            {/* Log Terminal Container */}
            <div
              id="logContainer"
              ref={logContainerRef}
              className="w-full h-80 overflow-y-auto bg-[#0d1117] border border-zinc-800 rounded-xl p-3 flex flex-col gap-1.5 font-mono text-xs select-text"
            >
              {filteredLogs.length === 0 ? (
                <div className="m-auto text-zinc-600 text-center py-8">لا توجد سجلات تطابق الفلتر الحالي</div>
              ) : (
                filteredLogs.map((entry) => (
                  <div key={entry.id} className="log-entry flex items-start gap-2 py-0.5 border-b border-zinc-900/60 leading-relaxed">
                    <span className="log-time text-zinc-500 shrink-0 text-[10px] pt-0.5">[{entry.time}]</span>
                    <span
                      className={`log-level shrink-0 text-[10px] px-1.5 py-0.2 rounded font-bold ${
                        entry.level === 'INFO'
                          ? 'bg-blue-950 text-blue-400 border border-blue-800/40'
                          : entry.level === 'WARNING'
                          ? 'bg-amber-950 text-amber-400 border border-amber-800/40'
                          : 'bg-rose-950 text-rose-400 border border-rose-800/40'
                      }`}
                    >
                      {entry.level}
                    </span>
                    <span className="log-msg text-zinc-200 break-words flex-1 text-right" dir="auto">
                      {entry.msg}
                    </span>
                  </div>
                ))
              )}
            </div>
          </div>

          {/* Sent Batches Card: رسائلي (الدفعات المرسلة) */}
          <div className="bg-zinc-900/90 border border-zinc-800 rounded-2xl p-4 sm:p-5 shadow-xl flex flex-col gap-3">
            <div className="flex items-center justify-between border-b border-zinc-800 pb-3">
              <div className="flex items-center gap-2">
                <History className="w-4 h-4 text-amber-400" />
                <h3 className="text-xs font-black text-white m-0">رسائلي (الدفعات المرسلة)</h3>
                <span className="px-2 py-0.5 rounded-full bg-zinc-800 text-zinc-300 text-[10px] font-mono">
                  {batches.length}
                </span>
              </div>

              <button
                id="refreshBatchesBtn"
                onClick={loadBatches}
                disabled={isLoadingBatches}
                className="px-2.5 py-1 text-[11px] font-bold rounded-lg bg-zinc-800 hover:bg-zinc-700 text-zinc-300 border border-zinc-700 flex items-center gap-1.5 transition-colors cursor-pointer"
              >
                <RefreshCw className={`w-3 h-3 ${isLoadingBatches ? 'animate-spin' : ''}`} />
                <span>تحديث</span>
              </button>
            </div>

            {/* Batch List */}
            <div id="batchList" className="flex flex-col gap-2.5 max-h-72 overflow-y-auto pr-0.5">
              {batches.length === 0 ? (
                <div className="bg-zinc-950 border border-zinc-800 rounded-xl p-8 text-center text-zinc-500 text-xs">
                  لم يتم إرسال دفعات بعد. قم بإرسال رسالة لتظهر هنا مع إمكانية تعديلها أو سحبها.
                </div>
              ) : (
                batches.map((batch) => (
                  <div
                    key={batch.id}
                    className="batch-item bg-zinc-950 border border-zinc-800 hover:border-zinc-700 rounded-xl p-3 flex flex-col sm:flex-row sm:items-center justify-between gap-3 transition-colors"
                  >
                    <div className="flex-1 flex flex-col gap-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-zinc-800 text-zinc-400">
                          {batch.id}
                        </span>
                        <span className="text-[11px] text-zinc-400 flex items-center gap-1">
                          <Clock className="w-3 h-3" />
                          <span>{batch.sent_at}</span>
                        </span>
                        {batch.edited_at && (
                          <span className="text-[10px] text-amber-400 bg-amber-500/10 px-1.5 py-0.2 rounded border border-amber-500/20">
                            معدل: {batch.edited_at}
                          </span>
                        )}
                      </div>

                      <p className="batch-text text-xs text-zinc-200 line-clamp-2 m-0 mt-0.5 leading-relaxed">
                        {batch.text}
                      </p>

                      <div className="flex items-center gap-3 text-[11px] text-zinc-500 mt-1">
                        <span>نجاح: <strong className="text-emerald-400">{batch.sent_count}</strong></span>
                        {Boolean(batch.failed_count) && (
                          <span>فشل: <strong className="text-rose-400">{batch.failed_count}</strong></span>
                        )}
                        {batch.has_media && (
                          <span className="text-blue-400 flex items-center gap-1">
                            <ImageIcon className="w-3 h-3" />
                            <span>مرفق صور</span>
                          </span>
                        )}
                      </div>
                    </div>

                    {/* Batch Actions */}
                    <div className="flex items-center gap-2 shrink-0 self-end sm:self-center">
                      <button
                        onClick={() => {
                          setEditingBatch(batch);
                          setNewBatchText(batch.text);
                        }}
                        className="edit-batch px-2.5 py-1.5 rounded-lg bg-blue-500/10 hover:bg-blue-500/20 text-blue-400 border border-blue-500/30 text-xs font-bold flex items-center gap-1 transition-colors"
                        title="تعديل الرسالة في المجموعات"
                      >
                        <Edit3 className="w-3.5 h-3.5" />
                        <span>تعديل</span>
                      </button>

                      <button
                        onClick={() => handleDeleteBatch(batch.id)}
                        className="delete-batch px-2.5 py-1.5 rounded-lg bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 border border-rose-500/30 text-xs font-bold flex items-center gap-1 transition-colors"
                        title="حذف وسحب الرسائل"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                        <span>حذف وسحب</span>
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Edit Batch Modal */}
      {editingBatch && (
        <div className="fixed inset-0 z-[3000] flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
          <div className="bg-zinc-900 border border-zinc-800 rounded-2xl max-w-lg w-full p-5 shadow-2xl flex flex-col gap-4">
            <div className="flex items-center justify-between border-b border-zinc-800 pb-3">
              <h4 className="text-sm font-black text-white m-0 flex items-center gap-2">
                <Edit3 className="w-4 h-4 text-blue-400" />
                <span>تعديل محتوى الدفعة ({editingBatch.id})</span>
              </h4>
              <button onClick={() => setEditingBatch(null)} className="text-zinc-500 hover:text-zinc-300">
                <X className="w-4 h-4" />
              </button>
            </div>

            <textarea
              rows={5}
              value={newBatchText}
              onChange={(e) => setNewBatchText(e.target.value)}
              className="w-full bg-zinc-950 border border-zinc-700 rounded-xl p-3 text-xs text-white focus:outline-none focus:border-blue-500 resize-none font-sans"
              placeholder="اكتب النص الجديد للدفعة..."
            />

            <div className="flex items-center justify-end gap-2">
              <button
                onClick={() => setEditingBatch(null)}
                className="px-4 py-2 rounded-xl bg-zinc-800 hover:bg-zinc-700 text-zinc-300 text-xs font-bold transition-colors"
              >
                إلغاء
              </button>
              <button
                onClick={handleEditBatch}
                disabled={isActionLoading || !newBatchText.trim()}
                className="px-4 py-2 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-xs font-black transition-colors disabled:opacity-50"
              >
                {isActionLoading ? 'جاري التعديل...' : 'حفظ وتعديل بالتيليجرام'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
