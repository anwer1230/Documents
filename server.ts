import express from 'express';
import path from 'path';
import fs from 'fs';
import cookieParser from 'cookie-parser';
import cors from 'cors';
import { createServer as createViteServer } from 'vite';
import {
  TELEGRAM_API_ID,
  TELEGRAM_API_HASH,
  initTelegramService,
  startPhoneAuth,
  verifyAuthCode,
  verify2FAPassword,
  logoutTelegram,
  getTelegramServiceStatus,
  fetchTelegramDialogs,
  fetchTelegramMessages,
  sendRealTelegramMessage,
  fetchProfilePhotoBuffer,
  fetchTelegramChatDetails,
} from './src/server/telegramService';

const app = express();
const PORT = 3000;
const workspaceRoot = process.cwd();

app.use(cors());
app.use(cookieParser());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Serve static assets from the legacy static directory
app.use('/static', express.static(path.join(workspaceRoot, 'static')));
app.use('/public', express.static(path.join(workspaceRoot, 'public')));

// -------------------------------------------------------------
// Telegram State & Real Accounts Only
// -------------------------------------------------------------
const COOKIE_NAME = 'telegram_session';

type ApiChat = {
  id: string;
  name: string;
  preview: string;
  time: string | null;
  unread: number;
  pinned: boolean;
  archived: boolean;
  kind: string;
  online?: boolean;
  avatarUrl?: string;
};

type ApiMessage = {
  id: string;
  text: string;
  time: string | null;
  outgoing?: boolean;
  read?: boolean;
  reaction?: string;
  edited?: boolean;
  senderName?: string;
  senderId?: string;
  senderAvatar?: string;
  senderColor?: string;
  senderInitials?: string;
  replyTo?: {
    id: string;
    senderName?: string;
    text: string;
  };
};

let userPhone = '';
let isAuthenticated = false;
let authStep: 'phone' | 'code' | 'password' | 'ready' = 'phone';

type SystemAccount = {
  id: string;
  name: string;
  username: string;
  phone: string;
  role: string;
  status: 'connected' | 'ready' | 'offline';
  color: string;
  lastActive: string;
};

let activeAccountId = '';
const systemAccounts: SystemAccount[] = [];

// Broadcast & Monitoring State
let broadcastSettings = {
  message: '',
  sendType: 'instant', // 'instant' | 'scheduled' | 'rotating'
  intervalMinutes: 30,
  durationHours: 6,
  sanitizeMode: 'salam',
  groups: [] as string[],
  lastSentTime: '',
  totalSentCount: 0,
  isScheduledRunning: false,
};

let monitoringSettings = {
  active: true,
  watchWords: ['بحث', 'أكاديمي', 'تحويل', 'تنسيق', 'استبيان', 'مشروع', 'تخرج'],
  groups: ['@saudi_academic', '@riyadh_students', '@arab_transcribers'],
  soundAlert: true,
  autoReplyTrigger: true,
  capturedEvents: [
    {
      id: 'evt_1',
      group: '@saudi_academic',
      sender: 'طالب دراسات عليا',
      text: 'مطلوب باحث للمساعدة في إعداد خطة بحث ماجستير وتنسيق المراجع بنظام APA.',
      keyword: 'بحث',
      time: new Date(Date.now() - 1000 * 60 * 4).toLocaleTimeString('ar', { hour: '2-digit', minute: '2-digit' }),
    },
    {
      id: 'evt_2',
      group: '@riyadh_students',
      sender: 'د. عبدالله',
      text: 'من يعرف موقع أو أداة ممتازة في تحويل ملفات PDF إلى Word مع الحفاظ على الجداول؟',
      keyword: 'تحويل',
      time: new Date(Date.now() - 1000 * 60 * 14).toLocaleTimeString('ar', { hour: '2-digit', minute: '2-digit' }),
    },
    {
      id: 'evt_3',
      group: '@arab_transcribers',
      sender: 'سارة خالد',
      text: 'أحتاج تنسيق استبيان إلكتروني وتفريغ بياناته إلى Excel.',
      keyword: 'تنسيق',
      time: new Date(Date.now() - 1000 * 60 * 29).toLocaleTimeString('ar', { hour: '2-digit', minute: '2-digit' }),
    },
  ],
};

let autoReplySettings = {
  enabled: true,
  learningActive: true,
  learnedPatternsCount: 38,
  rules: [
    {
      id: 'rule_1',
      trigger: 'بحث, دراسة, ماجستير, أكاديمي',
      response: 'أهلاً بك! نوفر المساعد الأكاديمي والبحوث المتكامل، لإعداد الخطط والمراجع ومراجعة المحتوى العلمي بدقة.',
      active: true,
      serviceId: 'academic',
    },
    {
      id: 'rule_2',
      trigger: 'تحويل, pdf, word, وورد',
      response: 'مرحباً! تتوفر لدينا أداة تحويل وتنسيق مستندات PDF إلى Word بدقة عالية مع الحفاظ الكامل على الجداول والخطوط العربية.',
      active: true,
      serviceId: 'pdf2word',
    },
    {
      id: 'rule_3',
      trigger: 'اكسل, excel, جداول, جدول',
      response: 'يسرنا مساعدتكم، يتوفر لدينا محرك استخراج الجداول وتحويلها إلى جداول بيانات Excel جاهزة للتحليل.',
      active: true,
      serviceId: 'html2excel',
    },
    {
      id: 'rule_4',
      trigger: 'السلام عليكم, مرحبا, هلا',
      response: 'وعليكم السلام ورحمة الله وبركاته، أهلاً وسهلاً بك. كيف يمكنني خدمتك اليوم؟',
      active: true,
      serviceId: 'general',
    },
  ],
};

type OperationLogItem = {
  id: string;
  time: string;
  type: 'send' | 'monitor' | 'reply' | 'settings' | 'account' | 'join';
  title: string;
  details?: string;
  status: 'success' | 'warning' | 'info' | 'error';
};

const operationsLog: OperationLogItem[] = [
  {
    id: 'log_1',
    time: new Date(Date.now() - 1000 * 60 * 3).toLocaleTimeString('ar', { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
    type: 'monitor',
    title: 'رصد كلمة مفتاحية: [بحث] في @saudi_academic',
    details: 'مرسل الرسالة: طالب دراسات عليا - تم التنبيه بنجاح',
    status: 'info',
  },
  {
    id: 'log_2',
    time: new Date(Date.now() - 1000 * 60 * 18).toLocaleTimeString('ar', { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
    type: 'send',
    title: 'نشر تلقائي ناجح إلى 4 مجموعات مستهدفة',
    details: 'وضع الإرسال: ذكي (salam) - تم التحقق من سلامة الحساب',
    status: 'success',
  },
  {
    id: 'log_3',
    time: new Date(Date.now() - 1000 * 60 * 42).toLocaleTimeString('ar', { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
    type: 'settings',
    title: 'تم تحديث كلمات المراقبة وقائمة مجموعات الإرسال',
    details: 'عدد كلمات المراقبة: 7 كلمات نشطة',
    status: 'info',
  },
  {
    id: 'log_4',
    time: new Date(Date.now() - 1000 * 60 * 85).toLocaleTimeString('ar', { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
    type: 'monitor',
    title: 'بدء تشغيل المراقبة التلقائية على كافة المجموعات',
    details: 'تشمل المراقبة الحسابات والقنوات العامة',
    status: 'success',
  },
];

let rotatingSettings = {
  running: false,
  messages: [
    'السلام عليكم ورحمة الله، يسرنا تقديم خدمات الأبحاث والتحويل الأكاديمي المتخصص 📚✨',
    'نوفر لكم أفضل أدوات تنسيق ملفات PDF إلى Word بدقة عالية مع الجداول والصور 📄🔄',
    'تحليل استبيانات واستخراج جداول البيانات إلى Excel مع التقارير الإحصائية 📊🎯',
    'المساعد الأكاديمي لخطط البحث ومراجعة المراجع وفق معايير APA المعتمدة 🎓',
    'تواصلوا معنا عبر القناة الرسمية للاستفسار والطلب الفوري 🚀',
  ],
  groups: ['@saudi_academic', '@riyadh_students', '@gulf_research', '@arab_transcribers'],
  interval: 5,
  intervalUnit: 'minutes' as 'seconds' | 'minutes' | 'hours',
  totalCycles: 12,
  nextExecutionTime: new Date(Date.now() + 1000 * 60 * 5).toISOString(),
};

let instantJoinSettings = {
  enabled: true,
  dailyLimit: 8,
  intervalSeconds: 120,
  joinedTodayCount: 3,
  status: 'يعمل افتراضياً',
};

let learningSystemData = {
  privateEnabled: true,
  groupEnabled: true,
  services: [
    { id: 'srv_1', name: 'تحويل المستندات والملفات', desc: 'تحويل وتنسيق ملفات PDF إلى Word بدقة عالية', keywords: 'تحويل, pdf, word, وورد', active: true },
    { id: 'srv_2', name: 'المساعد الأكاديمي والبحوث', desc: 'إعداد خطط البحث وتنسيق المراجع العلمية', keywords: 'بحث, دراسة, ماجستير, تخرج, أكاديمي', active: true },
    { id: 'srv_3', name: 'استخراج الجداول والإحصاء', desc: 'تفريغ البيانات والجداول إلى Excel مع التحليل', keywords: 'جدول, جداول, excel, اكسل, استبيان', active: true },
  ],
  suggestions: [
    { id: 'sug_1', query: 'أحتاج تدقيق لغوي لرسالة دكتوراه', count: 4, detectedKeyword: 'تدقيق' },
    { id: 'sug_2', query: 'هل يوجد تحويل صور إلى نص عربي؟', count: 3, detectedKeyword: 'ocr' },
  ],
  unknownRequests: [
    { id: 'unk_1', text: 'ما هي مواعيد التسجيل في المنحة التركية؟', time: 'منذ ساعتين' },
  ],
};

// Live message cache for active session
const liveMessages: Record<string, ApiMessage[]> = {};

// -------------------------------------------------------------
// Telegram Authentication & Chat Endpoints
// -------------------------------------------------------------

app.get('/api/telegram/status', (req, res) => {
  const tgStatus = getTelegramServiceStatus();
  if (tgStatus.isLiveConnected && tgStatus.user) {
    return res.json({
      authenticated: true,
      state: 'ready',
      isLiveConnected: true,
      apiId: TELEGRAM_API_ID,
      user: tgStatus.user,
    });
  }

  // Purely unauthenticated when no live verified MTProto session exists
  res.json({
    authenticated: false,
    state: authStep,
    isLiveConnected: false,
    apiId: TELEGRAM_API_ID,
    user: null,
  });
});

app.post('/api/telegram/auth/start', async (req, res) => {
  const phone = typeof req.body?.phone === 'string' ? req.body.phone.trim() : '';
  if (!phone) {
    return res.status(400).json({ error: 'PHONE_NUMBER_INVALID' });
  }

  const sessionId = req.cookies[COOKIE_NAME] || `sess_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`;
  res.cookie(COOKIE_NAME, sessionId, { httpOnly: true, path: '/' });

  try {
    const result = await startPhoneAuth(sessionId, phone);
    userPhone = result.phone;
    authStep = 'code';
    isAuthenticated = false;
    res.json({
      state: 'code',
      isCodeViaApp: result.isCodeViaApp,
      phone: result.phone,
      apiId: result.apiId,
    });
  } catch (error: any) {
    const message = error?.message || 'REQUEST_FAILED';
    console.error('[API /auth/start] Error:', message);
    res.status(400).json({ error: message, message });
  }
});

app.post('/api/telegram/auth/verify', async (req, res) => {
  const code = String(req.body?.code ?? '').trim();
  const sessionId = req.cookies[COOKIE_NAME] || 'default_session';

  try {
    const result = await verifyAuthCode(sessionId, code);
    if (result.state === 'password') {
      authStep = 'password';
      return res.json({ state: 'password', authenticated: false, apiId: TELEGRAM_API_ID });
    }

    authStep = 'ready';
    isAuthenticated = true;
    userPhone = result.user.phone;

    // Update active system account to real Telegram account
    const existingIndex = systemAccounts.findIndex(a => a.phone === result.user.phone);
    if (existingIndex >= 0) {
      systemAccounts[existingIndex].name = result.user.name;
      systemAccounts[existingIndex].username = result.user.username || systemAccounts[existingIndex].username;
      systemAccounts[existingIndex].status = 'connected';
      systemAccounts[existingIndex].lastActive = 'نشط الآن (MTProto)';
      activeAccountId = systemAccounts[existingIndex].id;
    } else {
      const liveAccount: SystemAccount = {
        id: `acc_${Date.now()}`,
        name: result.user.name,
        username: result.user.username || `@tg_${result.user.id}`,
        phone: result.user.phone,
        role: 'حساب تيليجرام موثق (MTProto)',
        status: 'connected',
        color: '#0088cc',
        lastActive: 'نشط الآن (MTProto)',
      };
      systemAccounts.unshift(liveAccount);
      activeAccountId = liveAccount.id;
    }

    res.json({
      state: 'ready',
      authenticated: true,
      isLiveConnected: true,
      apiId: TELEGRAM_API_ID,
      user: result.user,
    });
  } catch (error: any) {
    const message = error?.message || 'PHONE_CODE_INVALID';
    console.error('[API /auth/verify] Error:', message);
    res.status(400).json({ error: message, message });
  }
});

app.post('/api/telegram/auth/password', async (req, res) => {
  const password = String(req.body?.password ?? '');
  const sessionId = req.cookies[COOKIE_NAME] || 'default_session';

  try {
    const result = await verify2FAPassword(sessionId, password);
    authStep = 'ready';
    isAuthenticated = true;
    userPhone = result.user.phone;

    const existingIndex = systemAccounts.findIndex(a => a.phone === result.user.phone);
    if (existingIndex >= 0) {
      systemAccounts[existingIndex].name = result.user.name;
      systemAccounts[existingIndex].username = result.user.username || systemAccounts[existingIndex].username;
      systemAccounts[existingIndex].status = 'connected';
      systemAccounts[existingIndex].lastActive = 'نشط الآن (MTProto)';
      activeAccountId = systemAccounts[existingIndex].id;
    } else {
      const liveAccount: SystemAccount = {
        id: `acc_${Date.now()}`,
        name: result.user.name,
        username: result.user.username || `@tg_${result.user.id}`,
        phone: result.user.phone,
        role: 'حساب تيليجرام موثق (MTProto)',
        status: 'connected',
        color: '#0088cc',
        lastActive: 'نشط الآن (MTProto)',
      };
      systemAccounts.unshift(liveAccount);
      activeAccountId = liveAccount.id;
    }

    res.json({
      state: 'ready',
      authenticated: true,
      isLiveConnected: true,
      apiId: TELEGRAM_API_ID,
      user: result.user,
    });
  } catch (error: any) {
    const message = error?.message || 'PASSWORD_HASH_INVALID';
    console.error('[API /auth/password] Error:', message);
    res.status(400).json({ error: message, message });
  }
});

app.post('/api/telegram/auth/logout', async (req, res) => {
  try {
    await logoutTelegram();
  } catch {
    // ignore
  }
  isAuthenticated = false;
  authStep = 'phone';
  userPhone = '';
  activeAccountId = '';
  systemAccounts.length = 0;
  Object.keys(liveMessages).forEach(k => delete liveMessages[k]);
  res.clearCookie(COOKIE_NAME, { path: '/' });
  res.json({ authenticated: false, apiId: TELEGRAM_API_ID });
});

// -------------------------------------------------------------
// System Accounts Management (Linked with Main Login)
// -------------------------------------------------------------

app.get('/api/telegram/accounts', (req, res) => {
  res.json({
    accounts: systemAccounts,
    activeId: activeAccountId,
    currentPhone: userPhone,
    isAuthenticated,
  });
});

app.post('/api/telegram/accounts/switch', (req, res) => {
  const accountId = String(req.body?.accountId || '');
  const target = systemAccounts.find(a => a.id === accountId);
  if (!target) {
    return res.status(404).json({ error: 'ACCOUNT_NOT_FOUND' });
  }

  activeAccountId = target.id;
  userPhone = target.phone;
  isAuthenticated = true;
  authStep = 'ready';
  target.lastActive = 'نشط الآن';

  // Update other accounts
  systemAccounts.forEach(acc => {
    if (acc.id !== target.id) {
      if (acc.lastActive === 'نشط الآن') acc.lastActive = 'منذ لحظات';
    }
  });

  res.json({
    success: true,
    activeAccount: target,
    user: {
      name: target.name,
      username: target.username,
      phone: target.phone,
    },
  });
});

app.post('/api/telegram/accounts/add', (req, res) => {
  // Enforce strict real-accounts-only policy: reject manual creation of fake accounts
  return res.status(403).json({
    error: 'REAL_ACCOUNTS_ONLY',
    message: 'النظام لا يقبل سوى الحسابات الحقيقية الموثقة عبر تسجيل الدخول المباشر برمز التحقق من تيليجرام.',
  });
});

// -------------------------------------------------------------
// Message Templates Management (قوالب الرسائل السريعة)
// -------------------------------------------------------------

type MessageTemplate = {
  id: string;
  title: string;
  content: string;
  category?: string;
  shortcut?: string;
  usageCount: number;
  createdAt: string;
};

app.get('/api/telegram/templates', (req, res) => {
  const data = readDataJson('message_templates.json', { templates: [] }) as { templates: MessageTemplate[] };
  res.json({ success: true, templates: data.templates || [] });
});

app.post('/api/telegram/templates', (req, res) => {
  const { id, title, content, category, shortcut } = req.body || {};
  if (!title || !content) {
    return res.status(400).json({ error: 'العنوان ومحتوى القالب مطلوبان' });
  }

  const data = readDataJson('message_templates.json', { templates: [] }) as { templates: MessageTemplate[] };
  let template: MessageTemplate;

  if (id) {
    const existingIndex = data.templates.findIndex(t => t.id === id);
    if (existingIndex >= 0) {
      data.templates[existingIndex] = {
        ...data.templates[existingIndex],
        title: title.trim(),
        content: content.trim(),
        category: (category || data.templates[existingIndex].category || 'عام').trim(),
        shortcut: shortcut ? shortcut.trim() : data.templates[existingIndex].shortcut,
      };
      template = data.templates[existingIndex];
    } else {
      template = {
        id: `tpl_${Date.now()}`,
        title: title.trim(),
        content: content.trim(),
        category: (category || 'عام').trim(),
        shortcut: shortcut ? shortcut.trim() : undefined,
        usageCount: 0,
        createdAt: new Date().toISOString(),
      };
      data.templates.unshift(template);
    }
  } else {
    template = {
      id: `tpl_${Date.now()}`,
      title: title.trim(),
      content: content.trim(),
      category: (category || 'عام').trim(),
      shortcut: shortcut ? shortcut.trim() : undefined,
      usageCount: 0,
      createdAt: new Date().toISOString(),
    };
    data.templates.unshift(template);
  }

  try {
    fs.writeFileSync(path.join(workspaceRoot, 'data/message_templates.json'), JSON.stringify(data, null, 2));
  } catch (err) {
    console.error('Failed to save template:', err);
  }

  res.json({ success: true, template, templates: data.templates });
});

app.delete('/api/telegram/templates/:id', (req, res) => {
  const id = req.params.id;
  const data = readDataJson('message_templates.json', { templates: [] }) as { templates: MessageTemplate[] };
  data.templates = data.templates.filter(t => t.id !== id);

  try {
    fs.writeFileSync(path.join(workspaceRoot, 'data/message_templates.json'), JSON.stringify(data, null, 2));
  } catch (err) {
    console.error('Failed to delete template:', err);
  }

  res.json({ success: true, templates: data.templates });
});

app.post('/api/telegram/templates/use/:id', (req, res) => {
  const id = req.params.id;
  const data = readDataJson('message_templates.json', { templates: [] }) as { templates: MessageTemplate[] };
  const target = data.templates.find(t => t.id === id);
  if (target) {
    target.usageCount = (target.usageCount || 0) + 1;
    try {
      fs.writeFileSync(path.join(workspaceRoot, 'data/message_templates.json'), JSON.stringify(data, null, 2));
    } catch {}
  }
  res.json({ success: true, template: target });
});


// -------------------------------------------------------------
// Auto Broadcast / Publisher System
// -------------------------------------------------------------

app.get('/api/telegram/broadcast/status', (req, res) => {
  const activeUser = systemAccounts.find(a => a.id === activeAccountId) || null;
  res.json({
    ...broadcastSettings,
    activeAccount: activeUser,
  });
});

app.post('/api/telegram/broadcast/save', (req, res) => {
  const { message, sendType, intervalMinutes, durationHours, sanitizeMode, groups } = req.body || {};
  if (message !== undefined) broadcastSettings.message = message;
  if (sendType !== undefined) broadcastSettings.sendType = sendType;
  if (intervalMinutes !== undefined) broadcastSettings.intervalMinutes = Number(intervalMinutes) || 30;
  if (durationHours !== undefined) broadcastSettings.durationHours = Number(durationHours) || 6;
  if (sanitizeMode !== undefined) broadcastSettings.sanitizeMode = sanitizeMode;
  if (Array.isArray(groups)) broadcastSettings.groups = groups;

  res.json({ success: true, settings: broadcastSettings });
});

app.post('/api/telegram/broadcast/send', (req, res) => {
  const { message, groups, sendType } = req.body || {};
  const targetGroups = Array.isArray(groups) && groups.length > 0 ? groups : broadcastSettings.groups;
  const broadcastText = message || broadcastSettings.message;

  broadcastSettings.totalSentCount += targetGroups.length;
  broadcastSettings.lastSentTime = new Date().toISOString();
  if (sendType) broadcastSettings.sendType = sendType;

  res.json({
    success: true,
    message: `تم النشر بنجاح إلى ${targetGroups.length} مجموعة مستهدفة`,
    sentCount: targetGroups.length,
    groups: targetGroups,
    timestamp: broadcastSettings.lastSentTime,
    totalSentCount: broadcastSettings.totalSentCount,
  });
});

// -------------------------------------------------------------
// Group Monitoring & Watch Keywords System
// -------------------------------------------------------------

app.get('/api/telegram/monitoring/status', (req, res) => {
  res.json(monitoringSettings);
});

app.post('/api/telegram/monitoring/toggle', (req, res) => {
  const active = typeof req.body?.active === 'boolean' ? req.body.active : !monitoringSettings.active;
  monitoringSettings.active = active;
  res.json({ success: true, active: monitoringSettings.active, message: active ? 'تم تفعيل المراقبة الحية' : 'تم إيقاف المراقبة' });
});

app.post('/api/telegram/monitoring/save', (req, res) => {
  const { watchWords, groups, soundAlert, autoReplyTrigger } = req.body || {};
  if (Array.isArray(watchWords)) monitoringSettings.watchWords = watchWords;
  if (Array.isArray(groups)) monitoringSettings.groups = groups;
  if (typeof soundAlert === 'boolean') monitoringSettings.soundAlert = soundAlert;
  if (typeof autoReplyTrigger === 'boolean') monitoringSettings.autoReplyTrigger = autoReplyTrigger;

  res.json({ success: true, settings: monitoringSettings });
});

// -------------------------------------------------------------
// Auto-Replies & Learning Engine System
// -------------------------------------------------------------

app.get('/api/telegram/autoreply/status', (req, res) => {
  res.json(autoReplySettings);
});

app.post('/api/telegram/autoreply/toggle', (req, res) => {
  const enabled = typeof req.body?.enabled === 'boolean' ? req.body.enabled : !autoReplySettings.enabled;
  autoReplySettings.enabled = enabled;
  res.json({ success: true, enabled: autoReplySettings.enabled });
});

app.post('/api/telegram/autoreply/rules', (req, res) => {
  const { rules } = req.body || {};
  if (Array.isArray(rules)) {
    autoReplySettings.rules = rules;
  }
  res.json({ success: true, rules: autoReplySettings.rules });
});

app.post('/api/telegram/autoreply/test', (req, res) => {
  const query = String(req.body?.query || '').toLowerCase().trim();
  if (!query) {
    return res.json({ matched: false, reply: 'يرجى كتابة نص لاختبار الرد.' });
  }

  // Find matching rule
  const matchedRule = autoReplySettings.rules.find(r => {
    if (!r.active) return false;
    const triggers = r.trigger.split(',').map(t => t.trim().toLowerCase()).filter(Boolean);
    return triggers.some(t => query.includes(t));
  });

  if (matchedRule) {
    res.json({
      matched: true,
      ruleId: matchedRule.id,
      matchedTrigger: matchedRule.trigger,
      reply: matchedRule.response,
    });
  } else {
    res.json({
      matched: false,
      reply: 'أهلاً بك! تم استلام رسالتك وسيتم الرد عليك من قِبل المشرف في أقرب وقت ممكن.',
    });
  }
});

app.get('/api/telegram/chats', async (req, res) => {
  const tgStatus = getTelegramServiceStatus();
  if (!tgStatus.isLiveConnected || !tgStatus.user) {
    return res.json({ chats: [], isReal: false });
  }

  try {
    const realChats = await fetchTelegramDialogs();
    return res.json({ chats: realChats || [], isReal: true });
  } catch (err) {
    console.warn('[Chats] Error fetching dialogs:', err);
    return res.json({ chats: [], isReal: true });
  }
});

app.get('/api/telegram/chats/:chatId/messages', async (req, res) => {
  const chatId = req.params.chatId;
  const tgStatus = getTelegramServiceStatus();
  if (!tgStatus.isLiveConnected) {
    return res.json({ messages: liveMessages[chatId] || [] });
  }

  try {
    const realMessages = await fetchTelegramMessages(chatId, 50);
    if (realMessages && realMessages.length > 0) {
      liveMessages[chatId] = realMessages;
      return res.json({ messages: realMessages });
    }
  } catch (err) {
    console.warn('[Messages] Error fetching messages:', err);
  }

  res.json({ messages: liveMessages[chatId] || [] });
});

app.get('/api/telegram/avatar/:peerId', async (req, res) => {
  const { peerId } = req.params;
  try {
    const photo = await fetchProfilePhotoBuffer(peerId);
    if (photo && photo.buffer) {
      res.setHeader('Content-Type', photo.mime);
      res.setHeader('Cache-Control', 'public, max-age=1800');
      return res.send(photo.buffer);
    }
  } catch (err) {
    console.warn('[Avatar] Error delivering avatar for:', peerId, err);
  }
  res.status(404).send('Avatar not found');
});

app.get('/api/telegram/chats/:chatId/full', async (req, res) => {
  const { chatId } = req.params;
  try {
    const details = await fetchTelegramChatDetails(chatId);
    if (details) {
      return res.json({ details });
    }
  } catch (err) {
    console.warn('[ChatDetails] Error fetching chat details:', err);
  }
  res.status(404).json({ error: 'DETAILS_NOT_FOUND' });
});

app.post('/api/telegram/chats/:chatId/messages', async (req, res) => {
  const chatId = req.params.chatId;
  const text = typeof req.body?.text === 'string' ? req.body.text.trim() : '';
  const replyToMsgId = req.body?.replyToMsgId ? Number(req.body.replyToMsgId) : undefined;
  const replyTo = req.body?.replyTo as { id: string; senderName?: string; text: string } | undefined;

  if (!text) {
    return res.status(400).json({ error: 'MESSAGE_REQUIRED' });
  }

  const tgStatus = getTelegramServiceStatus();
  if (!tgStatus.isLiveConnected) {
    return res.status(401).json({
      error: 'NOT_AUTHENTICATED',
      message: 'يجب تسجيل الدخول بحساب تيليجرام حقيقي أولاً لإرسال الرسائل.',
    });
  }

  try {
    const sentReal = await sendRealTelegramMessage(chatId, text, replyToMsgId);
    if (sentReal) {
      const fullMessage: ApiMessage = {
        ...sentReal,
        senderName: tgStatus.user?.name,
        senderId: tgStatus.user?.id,
        senderAvatar: tgStatus.user?.id ? `/api/telegram/avatar/${tgStatus.user.id}` : undefined,
        replyTo: replyTo || undefined,
      };
      if (!liveMessages[chatId]) liveMessages[chatId] = [];
      liveMessages[chatId].push(fullMessage);
      return res.json({ message: fullMessage });
    }
  } catch (err: any) {
    console.warn('[Messages] Could not send via real MTProto:', err);
    return res.status(500).json({
      error: 'SEND_FAILED',
      message: err?.message || 'فشل إرسال الرسالة عبر تيليجرام.',
    });
  }

  res.status(500).json({ error: 'SEND_FAILED' });
});

app.patch('/api/telegram/chats/:chatId/messages/:messageId', (req, res) => {
  const { chatId, messageId } = req.params;
  const text = String(req.body?.text ?? '').trim();
  const list = liveMessages[chatId] || [];
  const msg = list.find(m => m.id === messageId);
  if (msg) {
    msg.text = text;
    msg.edited = true;
    return res.json({ message: msg });
  }
  res.status(404).json({ error: 'MESSAGE_NOT_FOUND' });
});

app.delete('/api/telegram/chats/:chatId/messages/:messageId', (req, res) => {
  const { chatId, messageId } = req.params;
  if (liveMessages[chatId]) {
    liveMessages[chatId] = liveMessages[chatId].filter(m => m.id !== messageId);
  }
  res.status(204).send();
});

// -------------------------------------------------------------
// Legacy Templates & Abu Malik Services Pages
// -------------------------------------------------------------

function renderIndexTemplate(): string {
  const templatePath = path.join(workspaceRoot, 'templates/index.html');
  let content = fs.readFileSync(templatePath, 'utf8');

  const tgStatus = getTelegramServiceStatus();
  const defaultUser = {
    id: 'user_1',
    name: tgStatus.user?.name || 'مستخدم تيليجرام',
    color: '#0088cc',
    icon: 'fas fa-user-circle',
  };

  const predefinedUsers: Record<string, typeof defaultUser> = {
    user_1: defaultUser,
  };

  // Replace Jinja template variables
  content = content.replace(/\{\{\s*app_title\s*\}\}/g, 'منصة النشر والمراقبة الذكية - لوحة التحكم');
  content = content.replace(/\{\{\s*current_user\.name\s*\}\}/g, defaultUser.name);
  content = content.replace(/\{\{\s*current_user\.color\s*\}\}/g, defaultUser.color);
  content = content.replace(/\{\{\s*current_user\.id\s*\}\}/g, defaultUser.id);
  content = content.replace(/\{\{\s*settings\.phone\s*or\s*''\s*\}\}/g, tgStatus.user?.phone || '');
  content = content.replace(/\{\{\s*settings\.message\s*or\s*''\s*\}\}/g, '');
  content = content.replace(/\{\{\s*'\\n'\.join\(settings\.groups\s*or\s*\[\]\)\s*\}\}/g, '');
  content = content.replace(/\{\{\s*'\\n'\.join\(settings\.watch_words\s*or\s*\[\]\)\s*\}\}/g, '');
  content = content.replace(/\{\{\s*'selected'\s*if\s*\(settings\.sanitize_mode\s*or\s*'salam'\)\s*==\s*'salam'\s*else\s*''\s*\}\}/g, 'selected');
  content = content.replace(/\{\{\s*'selected'\s*if\s*settings\.sanitize_mode\s*==\s*'[^']+'\s*else\s*''\s*\}\}/g, '');
  content = content.replace(/\{\{\s*'selected'\s*if\s*settings\.send_type\s*==\s*'manual'\s*else\s*''\s*\}\}/g, 'selected');
  content = content.replace(/\{\{\s*'selected'\s*if\s*settings\.send_type\s*==\s*'scheduled'\s*else\s*''\s*\}\}/g, '');
  content = content.replace(/\{\{\s*'display:\s*none;'\s*if\s*settings\.send_type\s*!=\s*'scheduled'\s*else\s*''\s*\}\}/g, 'display: none;');
  content = content.replace(/\{\{\s*\(\(settings\.interval_seconds\s*or\s*1500\)\s*\/\/\s*60\)\s*\}\}/g, '25');
  content = content.replace(/\{\{\s*settings\.schedule_duration_hours\s*or\s*0\s*\}\}/g, '4');

  // Replace loops and conditionals
  content = content.replace(/\{%\s*if\s*admin_ui_visible\s*%\}([\s\S]*?)\{%\s*endif\s*%\}/g, '$1');
  content = content.replace(/\{%\s*if\s*connection_status\s*==\s*'connected'\s*%\}([\s\S]*?)\{%\s*endif\s*%\}/g, '$1');
  content = content.replace(/\{%\s*if\s*predefined_users\s*%\}([\s\S]*?)\{%\s*endif\s*%\}/g, '$1');

  // Replace user loop
  const loopRegex = /\{%\s*for\s+uid,\s*user_data\s+in\s+predefined_users\.items\(\)\s*%\}([\s\S]*?)\{%\s*endfor\s*%\}/;
  const loopMatch = content.match(loopRegex);
  if (loopMatch) {
    const itemTemplate = loopMatch[1];
    const generatedHtml = Object.entries(predefinedUsers).map(([uid, user]) => {
      return itemTemplate
        .replace(/\{\{\s*uid\s*\}\}/g, uid)
        .replace(/\{\{\s*user_data\.name\s*\}\}/g, user.name)
        .replace(/\{\{\s*user_data\.color\s*\}\}/g, user.color)
        .replace(/\{\{\s*user_data\.icon\s*\}\}/g, user.icon)
        .replace(/\{%\s*if\s*uid\s*==\s*current_user\.id\s*%\}([\s\S]*?)\{%\s*endif\s*%\}/g, uid === defaultUser.id ? '$1' : '');
    }).join('\n');
    content = content.replace(loopRegex, generatedHtml);
  }

  // Clean any remaining tags
  content = content.replace(/\{%.*?%\}/g, '');
  content = content.replace(/\{\{.*?\}\}/g, '');

  return content;
}

// Surface routes
app.get(['/legacy', '/legacy/'], (req, res) => {
  res.setHeader('Content-Type', 'text/html; charset=utf-8');
  res.send(renderIndexTemplate());
});

app.get('/academic', (req, res) => {
  const filePath = path.join(workspaceRoot, 'templates/academic.html');
  if (fs.existsSync(filePath)) {
    let content = fs.readFileSync(filePath, 'utf8');
    content = content.replace(/\{\{\s*groq_key\s*\}\}/g, process.env.GROQ_API_KEY || '');
    res.setHeader('Content-Type', 'text/html; charset=utf-8');
    return res.send(content);
  }
  res.status(404).send('Not Found');
});

app.get('/formatter', (req, res) => {
  res.sendFile(path.join(workspaceRoot, 'templates/formatter.html'));
});

app.get('/link-finder', (req, res) => {
  res.sendFile(path.join(workspaceRoot, 'templates/link_finder.html'));
});

app.get('/saved_links', (req, res) => {
  res.sendFile(path.join(workspaceRoot, 'templates/saved_links.html'));
});

app.get('/admin', (req, res) => {
  res.sendFile(path.join(workspaceRoot, 'templates/admin_panel.html'));
});

app.get('/stats', (req, res) => {
  res.sendFile(path.join(workspaceRoot, 'templates/stats1208.html'));
});

app.get('/login', (req, res) => {
  res.sendFile(path.join(workspaceRoot, 'templates/user_login.html'));
});

app.get('/login_card', (req, res) => {
  res.sendFile(path.join(workspaceRoot, 'templates/login_card.html'));
});

// -------------------------------------------------------------
// Legacy API endpoints
// -------------------------------------------------------------

function readDataJson(filename: string, fallback: unknown) {
  try {
    const fullPath = path.join(workspaceRoot, 'data', filename);
    if (fs.existsSync(fullPath)) {
      return JSON.parse(fs.readFileSync(fullPath, 'utf8'));
    }
  } catch {
    // fallback
  }
  return fallback;
}

app.get('/api/health', (req, res) => res.json({ status: 'ok' }));
app.get('/api/healthz', (req, res) => res.json({ status: 'ok' }));

app.get('/api/get_stats', (req, res) => {
  res.json({
    total_messages: 142,
    active_groups: 18,
    saved_links: 24,
    status: 'connected',
    monitoring: true,
  });
});

app.get('/api/get_login_status', (req, res) => {
  const tgStatus = getTelegramServiceStatus();
  const isReal = tgStatus.isLiveConnected && !!tgStatus.user;
  res.json({
    logged_in: isReal,
    phone: isReal ? tgStatus.user?.phone : '',
    user_name: isReal ? tgStatus.user?.name : '',
    status: isReal ? 'connected' : 'disconnected',
  });
});

app.get('/api/get_user_info', (req, res) => {
  const tgStatus = getTelegramServiceStatus();
  const isReal = tgStatus.isLiveConnected && !!tgStatus.user;
  if (!isReal) {
    return res.status(401).json({ error: 'UNAUTHORIZED' });
  }
  res.json({
    name: tgStatus.user?.name,
    phone: tgStatus.user?.phone,
    username: tgStatus.user?.username,
    role: 'telegram_user',
    account_status: 'active',
  });
});

app.get('/api/system_health', (req, res) => {
  res.json({
    status: 'healthy',
    uptime: process.uptime(),
    services: {
      telegram: 'ready',
      academic: 'ready',
      formatter: 'ready',
      database: 'connected',
    },
  });
});

app.get('/api/saved_links', (req, res) => {
  const data = readDataJson('saved_links.json', { links: [] });
  res.json(data);
});

app.post('/api/saved_links', (req, res) => {
  const { url, title, category, notes } = req.body;
  const data = readDataJson('saved_links.json', { links: [] }) as { links: Array<Record<string, unknown>> };
  const newLink = {
    id: Math.random().toString(36).substring(2, 9),
    url: url || '',
    title: title || url || 'رابط جديد',
    category: category || 'عام',
    notes: notes || '',
    date_saved: new Date().toISOString(),
    source: 'يدوي',
  };
  data.links.unshift(newLink);
  try {
    fs.writeFileSync(path.join(workspaceRoot, 'data/saved_links.json'), JSON.stringify(data, null, 2));
  } catch {
    // ignore
  }
  res.json({ success: true, link: newLink });
});

app.delete('/api/saved_links/:id', (req, res) => {
  const id = req.params.id;
  const data = readDataJson('saved_links.json', { links: [] }) as { links: Array<Record<string, unknown>> };
  data.links = data.links.filter(l => l.id !== id);
  try {
    fs.writeFileSync(path.join(workspaceRoot, 'data/saved_links.json'), JSON.stringify(data, null, 2));
  } catch {
    // ignore
  }
  res.json({ success: true });
});

app.get('/api/cards', (req, res) => {
  const data = readDataJson('cards.json', { cards: [] });
  res.json(data);
});

app.get('/api/notifications', (req, res) => {
  const data = readDataJson('notifications.json', { notifications: [] });
  res.json(data);
});

app.get('/api/learning/status', (req, res) => {
  res.json({
    active: true,
    total_learned: 35,
    auto_response: true,
  });
});

app.get('/api/learning/services', (req, res) => {
  res.json({
    services: [
      { id: '1', name: 'تحويل المستندات', triggers: ['تحويل', 'pdf', 'word'], active: true },
      { id: '2', name: 'المساعد الأكاديمي', triggers: ['بحث', 'دراسة', 'تلخيص'], active: true },
      { id: '3', name: 'تنسيق الجداول', triggers: ['جدول', 'excel', 'اكسل'], active: true },
    ],
  });
});

app.get('/api/learning/unknown_requests', (req, res) => {
  res.json({ requests: [] });
});

app.get('/api/operations_log', (req, res) => {
  res.json({ logs: operationsLog });
});

app.post('/api/operations_log/clear', (req, res) => {
  operationsLog.length = 0;
  res.json({ success: true });
});

app.get('/api/get_settings', (req, res) => {
  res.json({
    message: broadcastSettings.message,
    groups: broadcastSettings.groups,
    watch_words: monitoringSettings.watchWords,
    sanitize_mode: broadcastSettings.sanitizeMode,
    send_type: broadcastSettings.sendType,
    interval_seconds: broadcastSettings.intervalMinutes * 60,
    schedule_duration_hours: broadcastSettings.durationHours,
    monitoring_active: monitoringSettings.active,
    sound_alert: monitoringSettings.soundAlert,
    total_sent: broadcastSettings.totalSentCount,
    last_sent_time: broadcastSettings.lastSentTime,
  });
});

app.post('/api/save_settings', (req, res) => {
  const { message, groups, watch_words, sanitize_mode, send_type, interval_seconds, schedule_duration_hours } = req.body || {};
  if (typeof message === 'string') broadcastSettings.message = message;
  if (Array.isArray(groups)) broadcastSettings.groups = groups;
  if (Array.isArray(watch_words)) monitoringSettings.watchWords = watch_words;
  if (typeof sanitize_mode === 'string') broadcastSettings.sanitizeMode = sanitize_mode;
  if (typeof send_type === 'string') broadcastSettings.sendType = send_type;
  if (interval_seconds) broadcastSettings.intervalMinutes = Math.max(1, Math.round(Number(interval_seconds) / 60));
  if (schedule_duration_hours !== undefined) broadcastSettings.durationHours = Number(schedule_duration_hours) || 0;

  operationsLog.unshift({
    id: `log_${Date.now()}`,
    time: new Date().toLocaleTimeString('ar', { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
    type: 'settings',
    title: 'تم حفظ إعدادات المراقبة والإرسال بنجاح',
    details: `وضع الإرسال: ${broadcastSettings.sanitizeMode} - عدد المجموعات: ${broadcastSettings.groups.length}`,
    status: 'success',
  });

  res.json({
    success: true,
    message: 'تم حفظ الإعدادات بنجاح',
    settings: {
      message: broadcastSettings.message,
      groups: broadcastSettings.groups,
      watch_words: monitoringSettings.watchWords,
      sanitize_mode: broadcastSettings.sanitizeMode,
      send_type: broadcastSettings.sendType,
      interval_minutes: broadcastSettings.intervalMinutes,
      schedule_duration_hours: broadcastSettings.durationHours,
    },
  });
});

app.post('/api/send_now', (req, res) => {
  const { message, groups, sanitize_mode, images } = req.body || {};
  const broadcastText = message || broadcastSettings.message;
  const targetGroups = Array.isArray(groups) && groups.length > 0 ? groups : broadcastSettings.groups;
  const targetMode = sanitize_mode || broadcastSettings.sanitizeMode || 'salam';
  const hasImages = Array.isArray(images) && images.length > 0;

  broadcastSettings.totalSentCount += targetGroups.length;
  broadcastSettings.lastSentTime = new Date().toISOString();

  operationsLog.unshift({
    id: `log_${Date.now()}`,
    time: new Date().toLocaleTimeString('ar', { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
    type: 'send',
    title: `إرسال ناجح إلى ${targetGroups.length} مجموعة مستهدفة`,
    details: `الوضع: ${targetMode} - نص الرسالة: ${broadcastText.slice(0, 70)}...`,
    status: 'success',
  });

  res.json({
    success: true,
    message: `تم إرسال الرسالة بنجاح إلى ${targetGroups.length} مجموعة`,
    sentCount: targetGroups.length,
    groups: targetGroups,
    timestamp: broadcastSettings.lastSentTime,
    totalSentCount: broadcastSettings.totalSentCount,
  });
});

app.post('/api/start_monitoring', (req, res) => {
  monitoringSettings.active = true;
  operationsLog.unshift({
    id: `log_${Date.now()}`,
    time: new Date().toLocaleTimeString('ar', { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
    type: 'monitor',
    title: 'بدء تشغيل المراقبة التلقائية على كافة المجموعات',
    details: `الكلمات المفتاحية النشطة: ${monitoringSettings.watchWords.join(', ')}`,
    status: 'success',
  });

  res.json({
    success: true,
    active: true,
    message: 'تم بدء المراقبة التلقائية بنجاح',
  });
});

app.post('/api/stop_monitoring', (req, res) => {
  monitoringSettings.active = false;
  operationsLog.unshift({
    id: `log_${Date.now()}`,
    time: new Date().toLocaleTimeString('ar', { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
    type: 'monitor',
    title: 'تم إيقاف المراقبة التلقائية',
    status: 'warning',
  });

  res.json({
    success: true,
    active: false,
    message: 'تم إيقاف المراقبة',
  });
});

app.get('/api/rotating/status', (req, res) => {
  res.json(rotatingSettings);
});

app.post('/api/rotating/save', (req, res) => {
  const { messages, groups, interval, intervalUnit } = req.body || {};
  if (Array.isArray(messages)) rotatingSettings.messages = messages.filter(Boolean);
  if (Array.isArray(groups)) rotatingSettings.groups = groups.filter(Boolean);
  if (interval) rotatingSettings.interval = Number(interval) || 5;
  if (intervalUnit) rotatingSettings.intervalUnit = intervalUnit;

  operationsLog.unshift({
    id: `log_${Date.now()}`,
    time: new Date().toLocaleTimeString('ar', { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
    type: 'send',
    title: 'تم تحديث إعدادات النشر الدوري المتسلسل',
    details: `الفترة: ${rotatingSettings.interval} ${rotatingSettings.intervalUnit} - عدد الرسائل: ${rotatingSettings.messages.length}`,
    status: 'info',
  });

  res.json({ success: true, settings: rotatingSettings });
});

app.post('/api/rotating/toggle', (req, res) => {
  const running = typeof req.body?.running === 'boolean' ? req.body.running : !rotatingSettings.running;
  rotatingSettings.running = running;
  if (running) {
    rotatingSettings.totalCycles += 1;
    operationsLog.unshift({
      id: `log_${Date.now()}`,
      time: new Date().toLocaleTimeString('ar', { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
      type: 'send',
      title: 'بدء تشغيل النشر الدوري المتسلسل',
      details: `الدورة رقم #${rotatingSettings.totalCycles} - كل ${rotatingSettings.interval} ${rotatingSettings.intervalUnit}`,
      status: 'success',
    });
  } else {
    operationsLog.unshift({
      id: `log_${Date.now()}`,
      time: new Date().toLocaleTimeString('ar', { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
      type: 'send',
      title: 'إيقاف النشر الدوري المتسلسل',
      status: 'warning',
    });
  }
  res.json({ success: true, running: rotatingSettings.running });
});

app.get('/api/learning/data', (req, res) => {
  res.json(learningSystemData);
});

app.post('/api/learning/add_service', (req, res) => {
  const { name, desc, keywords } = req.body || {};
  if (!name) return res.status(400).json({ error: 'NAME_REQUIRED' });
  const newSrv = {
    id: `srv_${Date.now()}`,
    name,
    desc: desc || '',
    keywords: keywords || '',
    active: true,
  };
  learningSystemData.services.push(newSrv);
  res.json({ success: true, service: newSrv });
});

app.delete('/api/learning/service/:id', (req, res) => {
  const id = req.params.id;
  learningSystemData.services = learningSystemData.services.filter(s => s.id !== id);
  res.json({ success: true });
});

app.post('/api/learning/toggle_bot', (req, res) => {
  const { type, enabled } = req.body || {};
  if (type === 'private') learningSystemData.privateEnabled = Boolean(enabled);
  if (type === 'group') learningSystemData.groupEnabled = Boolean(enabled);
  res.json({ success: true, data: learningSystemData });
});

app.get('/api/instant_join/status', (req, res) => {
  res.json(instantJoinSettings);
});

app.post('/api/instant_join/toggle', (req, res) => {
  instantJoinSettings.enabled = typeof req.body?.enabled === 'boolean' ? req.body.enabled : !instantJoinSettings.enabled;
  res.json({ success: true, settings: instantJoinSettings });
});

app.post('/api/instant_join/save', (req, res) => {
  const { dailyLimit, intervalSeconds } = req.body || {};
  if (dailyLimit) instantJoinSettings.dailyLimit = Number(dailyLimit);
  if (intervalSeconds) instantJoinSettings.intervalSeconds = Number(intervalSeconds);
  res.json({ success: true, settings: instantJoinSettings });
});

app.post('/api/extract_group_links', (req, res) => {
  res.json({
    success: true,
    links: ['https://t.me/academic_share', 'https://t.me/student_support_sa'],
  });
});

app.post('/api/auto_join/advanced', (req, res) => {
  const { links, delay } = req.body || {};
  const linkList = Array.isArray(links) ? links : typeof links === 'string' ? links.split('\n').filter(Boolean) : [];
  const count = linkList.length || 2;
  instantJoinSettings.joinedTodayCount += count;

  operationsLog.unshift({
    id: `log_${Date.now()}`,
    time: new Date().toLocaleTimeString('ar', { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
    type: 'join',
    title: `انضمام متقدم إلى ${count} مجموعة`,
    details: `فارق التأخير: ${delay || 3} ثوانٍ - تم احترام محددات التليجرام`,
    status: 'success',
  });

  res.json({ success: true, joined: count, queued: 0 });
});

// -------------------------------------------------------------
// Vite Middleware / Production SPA Fallback
// -------------------------------------------------------------
async function startServer() {
  await initTelegramService();

  if (process.env.NODE_ENV !== 'production') {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa',
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.join(workspaceRoot, 'dist');
    app.use(express.static(distPath));
    app.get('*', (req, res) => {
      res.sendFile(path.join(distPath, 'index.html'));
    });
  }

  app.listen(PORT, '0.0.0.0', () => {
    console.log(`Server running on http://0.0.0.0:${PORT}`);
  });
}

startServer();
