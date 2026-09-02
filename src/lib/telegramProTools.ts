import {
  GhostModeSettings,
  MultiAccount,
  ScheduledMessage,
  AutoResponderRule,
  AutoForwardRule,
  TelegramUser,
} from "../types";
import { DEMO_USER } from "../data/mockTelegramData";

const GHOST_SETTINGS_KEY = "tg_pro_ghost_settings";
const MULTI_ACCOUNTS_KEY = "tg_pro_multi_accounts";
const SCHEDULED_MSGS_KEY = "tg_pro_scheduled_messages";
const AUTO_RESPONDER_KEY = "tg_pro_auto_responder_rules";
const AUTO_FORWARD_KEY = "tg_pro_auto_forward_rules";
const LOCKED_CHATS_PIN_KEY = "tg_pro_locked_pin";
const LOCKED_CHAT_IDS_KEY = "tg_pro_locked_chat_ids";

// --- Ghost Mode Storage & Helpers ---
export function getGhostModeSettings(): GhostModeSettings {
  const stored = localStorage.getItem(GHOST_SETTINGS_KEY);
  if (stored) {
    try {
      return JSON.parse(stored);
    } catch {}
  }
  return {
    enabled: false,
    hideRead: true,
    hideTyping: true,
    hideOnline: true,
  };
}

export function saveGhostModeSettings(settings: GhostModeSettings) {
  localStorage.setItem(GHOST_SETTINGS_KEY, JSON.stringify(settings));
}

// --- Multi-Account Storage & Helpers ---
export function getStoredAccounts(): MultiAccount[] {
  const stored = localStorage.getItem(MULTI_ACCOUNTS_KEY);
  if (stored) {
    try {
      return JSON.parse(stored);
    } catch {}
  }
  // Default with initial active user
  return [
    {
      id: "acc_primary",
      user: DEMO_USER,
      sessionString: localStorage.getItem("tg_mtproto_session_string") || "demo_session",
      isActive: true,
      addedAt: Date.now(),
    },
  ];
}

export function saveStoredAccounts(accounts: MultiAccount[]) {
  localStorage.setItem(MULTI_ACCOUNTS_KEY, JSON.stringify(accounts));
}

export function addAccount(user: TelegramUser, sessionString: string): MultiAccount[] {
  const accounts = getStoredAccounts().map((a) => ({ ...a, isActive: false }));
  const newAccount: MultiAccount = {
    id: `acc_${Date.now()}`,
    user,
    sessionString,
    isActive: true,
    addedAt: Date.now(),
  };
  const updated = [...accounts, newAccount];
  saveStoredAccounts(updated);
  localStorage.setItem("tg_mtproto_session_string", sessionString);
  localStorage.setItem("tg_mtproto_user_profile", JSON.stringify(user));
  return updated;
}

export function switchActiveAccount(accountId: string): MultiAccount | null {
  const accounts = getStoredAccounts();
  let selected: MultiAccount | null = null;
  const updated = accounts.map((a) => {
    if (a.id === accountId) {
      selected = { ...a, isActive: true };
      return selected;
    }
    return { ...a, isActive: false };
  });

  if (selected) {
    saveStoredAccounts(updated);
    localStorage.setItem("tg_mtproto_session_string", (selected as MultiAccount).sessionString);
    localStorage.setItem("tg_mtproto_user_profile", JSON.stringify((selected as MultiAccount).user));
  }
  return selected;
}

export function removeAccount(accountId: string): MultiAccount[] {
  const accounts = getStoredAccounts().filter((a) => a.id !== accountId);
  if (accounts.length > 0 && !accounts.some((a) => a.isActive)) {
    accounts[0].isActive = true;
    localStorage.setItem("tg_mtproto_session_string", accounts[0].sessionString);
    localStorage.setItem("tg_mtproto_user_profile", JSON.stringify(accounts[0].user));
  }
  saveStoredAccounts(accounts);
  return accounts;
}

// --- Scheduled Messages ---
export function getScheduledMessages(): ScheduledMessage[] {
  const stored = localStorage.getItem(SCHEDULED_MSGS_KEY);
  if (stored) {
    try {
      return JSON.parse(stored);
    } catch {}
  }
  return [];
}

export function saveScheduledMessages(msgs: ScheduledMessage[]) {
  localStorage.setItem(SCHEDULED_MSGS_KEY, JSON.stringify(msgs));
}

// --- Auto Responder Rules ---
export function getAutoResponderRules(): AutoResponderRule[] {
  const stored = localStorage.getItem(AUTO_RESPONDER_KEY);
  if (stored) {
    try {
      return JSON.parse(stored);
    } catch {}
  }
  return [
    {
      id: "rule_1",
      name: "رد تلقائي عام (وضع الانشغال)",
      triggerKeyword: "*",
      responseText: "مرحباً! أنا غير متاح حالياً عبر تيليجرام، سأقوم بالرد عليك في أقرب وقت. 🕊️",
      enabled: false,
      onlyPrivate: true,
    },
  ];
}

export function saveAutoResponderRules(rules: AutoResponderRule[]) {
  localStorage.setItem(AUTO_RESPONDER_KEY, JSON.stringify(rules));
}

// --- Auto Forward Rules ---
export function getAutoForwardRules(): AutoForwardRule[] {
  const stored = localStorage.getItem(AUTO_FORWARD_KEY);
  if (stored) {
    try {
      return JSON.parse(stored);
    } catch {}
  }
  return [];
}

export function saveAutoForwardRules(rules: AutoForwardRule[]) {
  localStorage.setItem(AUTO_FORWARD_KEY, JSON.stringify(rules));
}

// --- Locked Chats & PIN Security ---
export function getLockedPin(): string | null {
  return localStorage.getItem(LOCKED_CHATS_PIN_KEY);
}

export function setLockedPin(pin: string) {
  localStorage.setItem(LOCKED_CHATS_PIN_KEY, pin);
}

export function getLockedChatIds(): string[] {
  const stored = localStorage.getItem(LOCKED_CHAT_IDS_KEY);
  if (stored) {
    try {
      return JSON.parse(stored);
    } catch {}
  }
  return [];
}

export function toggleLockChat(chatId: string): boolean {
  const locked = getLockedChatIds();
  let isNowLocked = false;
  let updated: string[];
  if (locked.includes(chatId)) {
    updated = locked.filter((id) => id !== chatId);
    isNowLocked = false;
  } else {
    updated = [...locked, chatId];
    isNowLocked = true;
  }
  localStorage.setItem(LOCKED_CHAT_IDS_KEY, JSON.stringify(updated));
  return isNowLocked;
}

// --- Instant Translation Engine ---
export async function translateMessageText(text: string, targetLang = "ar"): Promise<string> {
  // Common Telegram Arabic translations & dictionary for fast instant translation
  const sampleTranslations: Record<string, string> = {
    "Hello": "مرحباً",
    "How are you?": "كيف حالك؟",
    "Welcome to our Telegram channel": "أهلاً بكم في قناتنا على تيليجرام",
    "Check out the latest updates": "اطّلع على آخر التحديثات والأخبار",
    "Good morning": "صباح الخير",
    "Good evening": "مساء الخير",
    "Thank you": "شكراً جزيلاً",
    "See you soon": "أراك قريباً",
  };

  if (sampleTranslations[text.trim()]) {
    return sampleTranslations[text.trim()];
  }

  // If text is already Arabic, provide English translation simulation or vice-versa
  const isArabic = /[\u0600-\u06FF]/.test(text);
  if (isArabic) {
    return `[English]: ${text} (Translated)`;
  } else {
    return `[ترجمة بالعربية]: ${text}`;
  }
}

// --- Voice to Text Transcriber ---
export async function transcribeVoiceAudio(durationSeconds = 10): Promise<string> {
  await new Promise((res) => setTimeout(res, 800));
  const transcripts = [
    "السلام عليكم ورحمة الله، تم استلام الملف وسأقوم بمراجعته والرد عليك قريباً إن شاء الله.",
    "أهلاً وسهلاً، هل يمكننا عقد الاجتماع اليوم في تمام الساعة الخامسة؟",
    "شكراً جزيلاً على المتابعة، تم إتمام العمل وتأكيد الانضمام للقناة بنجاح.",
    "مرحباً، أرسلت لك الرابط الجديد للمجموعة، برجاء التحقق منه.",
  ];
  return transcripts[Math.floor(Math.random() * transcripts.length)];
}

// ==========================================
// 1. SEND & MONITORING STORAGE & HELPERS
// ==========================================
const SEND_MONITOR_KEY = "tg_pro_send_monitor_config";
const SEND_MONITOR_LOGS_KEY = "tg_pro_send_monitor_logs";

export const DEFAULT_MONITOR_KEYWORDS: string[] = [
  "اريد مساعدة",
  "ابي مساعدة",
  "من يسوي تكليف",
  "من يحل",
  "عندي بحث",
  "معي واجب",
  "عندي اسايمنت",
  "من يسوي اسايمنت",
  "ابي سكليف",
  "ابي عذر",
  "من يسوي سكليف",
  "ابي شخص مضمون",
  "ابي مختص",
  "هيليب",
  "من يستطيع",
  "تعرفون احد",
  "تعرفون شخص",
  "من يساعدني",
  "من يعرف مختص",
  "مين يعرف يحل واجب",
  "من يحل واجبات الجامعه",
  "أحتاج مساعدتكم",
  "ابي احد يسوي بحث",
  "من يعرف احد كويس",
  "مطلوب",
  "عاجل",
  "استفسار",
];

export const KNOWN_PROTECTION_BOTS: string[] = [
  "missrose_bot",
  "rose_bot",
  "shieldy_bot",
  "groupguardbot",
  "guardbot",
  "combot",
  "combatbot",
  "tgprotectionbot",
  "cas_ban_bot",
  "spamban_bot",
  "joinhiderbot",
  "antispambot",
  "channelguardbot",
  "modr8_bot",
  "federationbot",
  "safegroupbot",
  "cleaner_bot",
  "shieldguardbot",
  "shield_bot",
  "anti_arabic_script_bot",
  "protectronbot",
  "grouphelpbot",
  "grouphelp_bot",
];

/**
 * Clean & Sanitize message text to prevent bot-detection/bans
 */
export function sanitizeMessage(text: string, mode: 'clean' | 'full' = 'clean'): string {
  if (!text) return text;
  let sanitized = text;

  // 1. Remove Telegram links (t.me, telegram.me, etc.)
  sanitized = sanitized.replace(/(https?:\/\/)?(www\.)?(t\.me|telegram\.me|telegram\.dog)\/[a-zA-Z0-9_+/]+/gi, "");
  
  // 2. Remove WhatsApp links (wa.me, chat.whatsapp.com, etc.)
  sanitized = sanitized.replace(/(https?:\/\/)?(www\.)?(wa\.me|chat\.whatsapp\.com|api\.whatsapp\.com)\/[a-zA-Z0-9_+/-]+/gi, "");

  // 3. Remove generic URLs
  sanitized = sanitized.replace(/https?:\/\/[^\s]+/gi, "");

  // 4. Remove Phone numbers (Saudi, Egyptian, international formats)
  sanitized = sanitized.replace(/(?:\+?966|05|00966)[0-9]{8}/g, "");
  sanitized = sanitized.replace(/(?:\+?20|01|0020)[0-9]{9}/g, "");
  sanitized = sanitized.replace(/\+?[0-9]{10,14}/g, "");

  // 5. Remove blatant advertising triggers
  if (mode === 'full') {
    const promoKeywords = ["للتواصل", "واتساب", "خصم", "عرض خاص", "سعر مغري", "تخفيضات", "اشترك الآن", "رابط القناة"];
    promoKeywords.forEach(kw => {
      sanitized = sanitized.split(kw).join("");
    });
  }

  // Remove excessive whitespace & clean up
  return sanitized.replace(/\s+/g, " ").trim();
}

/**
 * Detect whether a chat has known protection bots or restrictive rules
 */
export function isGroupProtected(chatParticipantsOrBots: string[] = []): boolean {
  if (!chatParticipantsOrBots || chatParticipantsOrBots.length === 0) return false;
  return chatParticipantsOrBots.some((username) => {
    const lower = username.toLowerCase().replace("@", "");
    return KNOWN_PROTECTION_BOTS.some((bot) => lower.includes(bot) || bot.includes(lower));
  });
}

export function getSendMonitorConfig(): import("../types").SendMonitorConfig {
  const stored = localStorage.getItem(SEND_MONITOR_KEY);
  if (stored) {
    try {
      const parsed = JSON.parse(stored);
      // Ensure new user keywords and settings are populated
      return {
        id: parsed.id || "send_mon_default",
        enabled: Boolean(parsed.enabled),
        targetChatIds: parsed.targetChatIds || [],
        keywords: Array.isArray(parsed.keywords) && parsed.keywords.length > 0 
          ? Array.from(new Set([...parsed.keywords, ...DEFAULT_MONITOR_KEYWORDS])) 
          : DEFAULT_MONITOR_KEYWORDS,
        replyMessage: parsed.replyMessage || "أهلاً بك! تواصل معي بخصوص طلبك وسأقوم بمساعدتك فوراً بكل احترافية وضمان.",
        actionType: parsed.actionType || "reply",
        broadcastIntervalMinutes: parsed.broadcastIntervalMinutes ?? 0,
        antiFloodDelaySeconds: parsed.antiFloodDelaySeconds ?? 15,
        sendType: parsed.sendType || "auto_interval",
        repeatIntervalMinutes: parsed.repeatIntervalMinutes ?? 60,
        scheduledTime: parsed.scheduledTime || "",
        protectedGroupAction: parsed.protectedGroupAction || "salam",
        smartSalamWaitMinutes: parsed.smartSalamWaitMinutes ?? 5,
        smartSalamRequiredMessages: parsed.smartSalamRequiredMessages ?? 3,
      };
    } catch {}
  }
  return {
    id: "send_mon_default",
    enabled: false,
    targetChatIds: [],
    keywords: DEFAULT_MONITOR_KEYWORDS,
    replyMessage: "أهلاً بك! تواصل معي بخصوص طلبك وسأقوم بمساعدتك فوراً بكل احترافية وضمان.",
    actionType: "reply",
    broadcastIntervalMinutes: 0,
    antiFloodDelaySeconds: 15,
    sendType: "auto_interval",
    repeatIntervalMinutes: 60,
    scheduledTime: "",
    protectedGroupAction: "salam",
    smartSalamWaitMinutes: 5,
    smartSalamRequiredMessages: 3,
  };
}

export function saveSendMonitorConfig(config: import("../types").SendMonitorConfig) {
  localStorage.setItem(SEND_MONITOR_KEY, JSON.stringify(config));
}

export function getSendMonitorLogs(): import("../types").SendMonitorLog[] {
  const stored = localStorage.getItem(SEND_MONITOR_LOGS_KEY);
  if (stored) {
    try {
      return JSON.parse(stored);
    } catch {}
  }
  return [];
}

export function saveSendMonitorLogs(logs: import("../types").SendMonitorLog[]) {
  localStorage.setItem(SEND_MONITOR_LOGS_KEY, JSON.stringify(logs.slice(-100)));
}

export function addSendMonitorLog(log: Omit<import("../types").SendMonitorLog, "id" | "timestamp">) {
  const current = getSendMonitorLogs();
  const newLog: import("../types").SendMonitorLog = {
    ...log,
    id: `log_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`,
    timestamp: Date.now(),
  };
  saveSendMonitorLogs([newLog, ...current]);
  return newLog;
}


// ==========================================
// 2. MY MESSAGES (TEMPLATES) STORAGE & HELPERS
// ==========================================
const MY_MESSAGES_KEY = "tg_pro_my_messages_templates";

export function getSavedTemplates(): import("../types").SavedMessageTemplate[] {
  const stored = localStorage.getItem(MY_MESSAGES_KEY);
  if (stored) {
    try {
      return JSON.parse(stored);
    } catch {}
  }
  return [
    {
      id: "tpl_1",
      title: "رسالة ترحيبية بالقناة",
      category: "general",
      content: "أهلاً ومرحباً بكم جميعاً في قناتنا الرسمية! 🌟\nيسعدنا انضمامكم، ترقبوا كل جديد ومفيد يومياً.",
      isPinned: true,
      tags: ["ترحيب", "عام"],
      createdAt: Date.now() - 86400000 * 5,
    },
    {
      id: "tpl_2",
      title: "نموذج نشر إعلاني احترافي",
      category: "marketing",
      content: "📢 **إعلان هام وخدمات حصرية!**\n\nنقدم لكم أفضل العروض بأعلى جودة وأفضل الأسعار.\n🔹 مميزاتنا:\n• دعم متواصل 24/7\n• تسليم فوري ومضمون\n\nللتواصل والاستفسار: @telegram",
      isPinned: true,
      tags: ["تسويق", "إعلانات"],
      createdAt: Date.now() - 86400000 * 3,
    },
    {
      id: "tpl_3",
      title: "رد سريع: قيد المتابعة",
      category: "quick_reply",
      content: "مرحباً بك! تم استلام رسالتك وهي قيد المتابعة الآن وسنوافيك بالرد الشافي خلال دقائق بإذن الله.",
      isPinned: false,
      tags: ["خدمة عملاء", "سريع"],
      createdAt: Date.now() - 86400000 * 2,
    },
    {
      id: "tpl_4",
      title: "تنبيه موعد المحاضرة الأكاديمية",
      category: "academic",
      content: "🎓 **تذكير أكاديمي:**\nنود تذكير الطلاب الأعزاء بأن موعد المحاضرة القادمة سيكون في تمام الساعة 10:00 صباحاً.\nبرجاء مراجعة الملفات والمقررات قبل البدء.",
      isPinned: false,
      tags: ["دراسة", "محاضرات"],
      createdAt: Date.now() - 86400000,
    },
  ];
}

export function saveSavedTemplates(templates: import("../types").SavedMessageTemplate[]) {
  localStorage.setItem(MY_MESSAGES_KEY, JSON.stringify(templates));
}

// ==========================================
// 3. AUTO JOIN ADVANCED STORAGE & HELPERS
// ==========================================
const AUTO_JOIN_QUEUE_KEY = "tg_pro_auto_join_queue";
const AUTO_JOIN_CONFIG_KEY = "tg_pro_auto_join_config";

export function getAutoJoinConfig(): import("../types").AutoJoinConfig {
  const stored = localStorage.getItem(AUTO_JOIN_CONFIG_KEY);
  if (stored) {
    try {
      return JSON.parse(stored);
    } catch {}
  }
  return {
    delaySeconds: 12,
    randomDelayRange: 4,
    autoStartOnPaste: false,
    maxJoinsPerBatch: 50,
  };
}

export function saveAutoJoinConfig(config: import("../types").AutoJoinConfig) {
  localStorage.setItem(AUTO_JOIN_CONFIG_KEY, JSON.stringify(config));
}

export function getAutoJoinQueue(): import("../types").AutoJoinQueueItem[] {
  const stored = localStorage.getItem(AUTO_JOIN_QUEUE_KEY);
  if (stored) {
    try {
      return JSON.parse(stored);
    } catch {}
  }
  return [];
}

export function saveAutoJoinQueue(queue: import("../types").AutoJoinQueueItem[]) {
  localStorage.setItem(AUTO_JOIN_QUEUE_KEY, JSON.stringify(queue));
}

// ==========================================
// 4. SAVED LINKS (LINK BANK) STORAGE & HELPERS
// ==========================================
const SAVED_LINKS_KEY = "tg_pro_saved_links_bank";

export function getSavedLinks(): import("../types").SavedTelegramLink[] {
  const stored = localStorage.getItem(SAVED_LINKS_KEY);
  if (stored) {
    try {
      return JSON.parse(stored);
    } catch {}
  }
  return [
    {
      id: "link_1",
      title: "قناة التحديثات والأخبار التقنية",
      link: "https://t.me/telegram",
      username: "telegram",
      type: "channel",
      category: "تقنية",
      tags: ["أخبار", "رسمي"],
      isJoined: true,
      savedAt: Date.now() - 86400000 * 10,
    },
    {
      id: "link_2",
      title: "مجموعة المطورين والبرمجة العربية",
      link: "https://t.me/arabic_devs",
      username: "arabic_devs",
      type: "group",
      category: "برمجة",
      tags: ["تطوير", "أكواد"],
      isJoined: false,
      savedAt: Date.now() - 86400000 * 4,
    },
    {
      id: "link_3",
      title: "مكتبة الكتب والمراجع الأكاديمية PDF",
      link: "https://t.me/academic_library_pdf",
      username: "academic_library_pdf",
      type: "channel",
      category: "أكاديمي",
      tags: ["كتب", "مراجع", "جامعة"],
      isJoined: true,
      savedAt: Date.now() - 86400000 * 2,
    },
  ];
}

export function saveSavedLinks(links: import("../types").SavedTelegramLink[]) {
  localStorage.setItem(SAVED_LINKS_KEY, JSON.stringify(links));
}

// ==========================================
// 5. ACADEMIC TOOLS STORAGE & HELPERS
// ==========================================
const ACADEMIC_CONFIG_KEY = "tg_pro_academic_config";
const ACADEMIC_RESOURCES_KEY = "tg_pro_academic_resources";

export function getAcademicConfig(): import("../types").AcademicExtractConfig {
  const stored = localStorage.getItem(ACADEMIC_CONFIG_KEY);
  if (stored) {
    try {
      return JSON.parse(stored);
    } catch {}
  }
  return {
    sourceChatIds: [],
    fileTypes: ["pdf", "document", "video", "zip"],
    searchQuery: "",
    summarizeWithAI: true,
  };
}

export function saveAcademicConfig(config: import("../types").AcademicExtractConfig) {
  localStorage.setItem(ACADEMIC_CONFIG_KEY, JSON.stringify(config));
}

export function getAcademicResources(): import("../types").AcademicResourceItem[] {
  const stored = localStorage.getItem(ACADEMIC_RESOURCES_KEY);
  if (stored) {
    try {
      return JSON.parse(stored);
    } catch {}
  }
  return [
    {
      id: "acad_1",
      title: "مرجع هياكل البيانات والخوارزميات الشامل",
      fileName: "Data_Structures_Algorithms_Complete.pdf",
      fileType: "pdf",
      fileSize: 14250000,
      chatTitle: "مكتبة علوم الحاسوب",
      chatId: "-1001552345678",
      messageId: 442,
      date: Math.floor(Date.now() / 1000) - 86400 * 2,
      summary: "كتاب شامل يشرح المصفوفات والقوائم المتصلة والأشجار وخوارزميات الترتيب والبحث مع أمثلة عملية.",
    },
    {
      id: "acad_2",
      title: "ملخص الذكاء الاصطناعي وتعلم الآلة",
      fileName: "Machine_Learning_Fundamentals.pdf",
      fileType: "pdf",
      fileSize: 8400000,
      chatTitle: "مكتبة علوم الحاسوب",
      chatId: "-1001552345678",
      messageId: 512,
      date: Math.floor(Date.now() / 1000) - 86400 * 4,
      summary: "شرح أساسيات الشبكات العصبية، نماذج الانحدار، والتصنيف مع تطبيقات برمجية.",
    },
    {
      id: "acad_3",
      title: "تسجيل محاضرة هندسة البرمجيات وقواعد البيانات",
      fileName: "Software_Engineering_Lecture_04.mp4",
      fileType: "video",
      fileSize: 68500000,
      chatTitle: "محاضرات تقنية المعلومات",
      chatId: "-1001998877665",
      messageId: 108,
      date: Math.floor(Date.now() / 1000) - 86400 * 5,
      summary: "تسجيل مرئي لمحاضرة تصميم قواعد البيانات العلائقية ومخططات ER Diagrams.",
    },
  ];
}

export function saveAcademicResources(resources: import("../types").AcademicResourceItem[]) {
  localStorage.setItem(ACADEMIC_RESOURCES_KEY, JSON.stringify(resources));
}

// ==========================================
// 6. LINK MONITOR STORAGE & HELPERS
// ==========================================
const LINK_MONITOR_CONFIG_KEY = "tg_pro_link_monitor_config";
const CAPTURED_LINKS_KEY = "tg_pro_captured_links";

export function getLinkMonitorConfig(): import("../types").LinkMonitorConfig {
  const stored = localStorage.getItem(LINK_MONITOR_CONFIG_KEY);
  if (stored) {
    try {
      return JSON.parse(stored);
    } catch {}
  }
  return {
    enabled: false,
    monitoredChatIds: [],
    autoJoinCapturedTelegramLinks: false,
    autoSaveToLinkBank: true,
    soundAlert: true,
    filterKeywords: [],
  };
}

export function saveLinkMonitorConfig(config: import("../types").LinkMonitorConfig) {
  localStorage.setItem(LINK_MONITOR_CONFIG_KEY, JSON.stringify(config));
}

export function getCapturedLinks(): import("../types").CapturedLinkItem[] {
  const stored = localStorage.getItem(CAPTURED_LINKS_KEY);
  if (stored) {
    try {
      return JSON.parse(stored);
    } catch {}
  }
  return [];
}

export function saveCapturedLinks(links: import("../types").CapturedLinkItem[]) {
  localStorage.setItem(CAPTURED_LINKS_KEY, JSON.stringify(links.slice(-200)));
}

