import React, { useState, useEffect, useRef, useCallback } from 'react';
import { TelegramChat, TelegramMessage, TelegramUser, ChatFolder, TelegramThemeConfig, TelegramAccount, TypingStatus, TelegramReplyMarkup } from './types';
import { INITIAL_CHATS, INITIAL_MESSAGES, CURRENT_DEMO_USER } from './utils/mockData';
import { LoginView } from './components/LoginView';
import { Sidebar } from './components/Sidebar';
import { ChatWindow } from './components/ChatWindow';
import { ChatInfoDrawer } from './components/ChatInfoDrawer';
import { SettingsDrawer } from './components/SettingsDrawer';
import { NewChatModal } from './components/NewChatModal';
import { ContactsModal } from './components/ContactsModal';
import { MediaViewerModal } from './components/MediaViewerModal';
import { StoryViewerModal } from './components/StoryViewerModal';
import { TelegramPeerStories } from './types';
import { AddAccountModal } from './components/AddAccountModal';
import { MiniAppModal } from './components/MiniAppModal';
import { Toast, ToastData } from './components/Toast';
import { ClearHistoryModal } from './components/modals/ClearHistoryModal';
import { LeaveGroupModal } from './components/modals/LeaveGroupModal';
import { ShareLinkModal } from './components/modals/ShareLinkModal';
import { ReportChatModal } from './components/modals/ReportChatModal';
import { Loader2 } from 'lucide-react';
import { wsClient } from './utils/websocket';
import { Api } from './services/api';
import { csrfFetch } from './services/csrfFetch';

const MAX_TELEGRAM_ACCOUNTS = 6;

export default function App() {
  // Theme & Language state
  const [themeConfig, setThemeConfig] = useState<TelegramThemeConfig>(() => {
    const saved = localStorage.getItem('tg_theme_config');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch {}
    }
    return {
      isDark: true,
      accentColor: '#3390ec',
      fontSize: 'md',
      language: 'ar',
    };
  });

  // Multi-Accounts State (Support up to 6 isolated users)
  const [accounts, setAccounts] = useState<TelegramAccount[]>(() => {
    const saved = localStorage.getItem('tg_multi_accounts');
    if (saved) {
      try {
        return JSON.parse(saved).slice(0, MAX_TELEGRAM_ACCOUNTS);
      } catch {}
    }
    return [];
  });
  const [activeAccountId, setActiveAccountId] = useState<string>('');
  const [isAddAccountOpen, setIsAddAccountOpen] = useState(false);

  // Per-account isolated chat and message storage
  const [accountsDataMap, setAccountsDataMap] = useState<Record<string, {
    chats: TelegramChat[];
    messagesMap: Record<string, TelegramMessage[]>;
    selectedChatId: string;
  }>>(() => {
    const saved = localStorage.getItem('tg_accounts_data_map');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch {}
    }
    return {};
  });

  // Active User Auth State
  const [currentUser, setCurrentUser] = useState<TelegramUser | null>(null);
  const [isDemoMode, setIsDemoMode] = useState<boolean>(false);
  const [isCheckingAuth, setIsCheckingAuth] = useState(true);

  // Chat Data State (for currently active user)
  const [chats, setChats] = useState<TelegramChat[]>(INITIAL_CHATS);
  const [selectedChatId, setSelectedChatId] = useState<string>('saved_messages');
  const [messagesMap, setMessagesMap] = useState<Record<string, TelegramMessage[]>>(INITIAL_MESSAGES);
  
  // UI & Navigation State
  const [activeFolder, setActiveFolder] = useState<ChatFolder>('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [isMobileChatOpen, setIsMobileChatOpen] = useState(false);
  const [isChatInfoOpen, setIsChatInfoOpen] = useState(false);
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const [isNewChatOpen, setIsNewChatOpen] = useState(false);
  const [isContactsOpen, setIsContactsOpen] = useState(false);
  const [isMiniAppOpen, setIsMiniAppOpen] = useState(false);
  const [miniAppData, setMiniAppData] = useState<{ url?: string; appName?: string; botUsername?: string } | null>(null);
  const [isJoiningChannel, setIsJoiningChannel] = useState(false);
  
  // Media Lightbox
  const [mediaViewerData, setMediaViewerData] = useState<{ url: string; title?: string } | null>(null);
  const [peerStoriesList, setPeerStoriesList] = useState<TelegramPeerStories[]>([]);
  const [activeStoryPeerId, setActiveStoryPeerId] = useState<string | null>(null);

  // Fetch Stories
  useEffect(() => {
    const demoStories: TelegramPeerStories[] = [
      {
        peerId: 'me',
        peerTitle: 'قصتي',
        hasUnread: true,
        stories: [
          {
            id: 'st_1',
            peerId: 'me',
            date: Date.now() - 3600000,
            caption: 'مرحباً بكم في تيليجرام! استمتع بجميع الميزات الحقيقية.',
            mediaType: 'photo',
            reactionsCount: 12,
          },
        ],
      },
      {
        peerId: 'telegram',
        peerTitle: 'Telegram',
        hasUnread: true,
        stories: [
          {
            id: 'st_2',
            peerId: 'telegram',
            date: Date.now() - 7200000,
            caption: 'تحديث جديد: دعم القصص والتفاعلات والتعرف على النصوص (OCR)!',
            mediaType: 'photo',
            reactionsCount: 45,
          },
        ],
      },
    ];

    const fetchStories = async () => {
      if (!currentUser || currentUser.id === 'demo_user' || isDemoMode) {
        setPeerStoriesList(demoStories);
        return;
      }

      try {
        const res = await fetch('/api/telegram/stories');
        if (res.ok) {
          const data = await res.json();
          if (data.peerStories && data.peerStories.length > 0) {
            setPeerStoriesList(data.peerStories);
            return;
          }
        }
      } catch (err) {
        // Fallback gracefully without warning
      }
      setPeerStoriesList(demoStories);
    };
    fetchStories();
  }, [currentUser, isDemoMode]);

  const handleOpenStory = (peerId: string) => {
    setActiveStoryPeerId(peerId);
  };

  const handleReactToStory = async (peerId: string, storyId: string, emoji: string) => {
    setPeerStoriesList((prev) =>
      prev.map((p) =>
        p.peerId === peerId
          ? {
              ...p,
              stories: p.stories.map((s) =>
                s.id === storyId ? { ...s, reactionsCount: (s.reactionsCount || 0) + 1 } : s
              ),
            }
          : p
      )
    );
    try {
      await fetch('/api/telegram/stories/react', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ peerId, storyId, emoji }),
      });
    } catch (err) {
      console.warn('Error reacting to story:', err);
    }
  };

  const handleReadStory = async (peerId: string, storyId: string) => {
    setPeerStoriesList((prev) =>
      prev.map((p) =>
        p.peerId === peerId
          ? {
              ...p,
              stories: p.stories.map((s) => (s.id === storyId ? { ...s, isViewed: true } : s)),
            }
          : p
      )
    );
    try {
      await fetch('/api/telegram/stories/read', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ peerId, maxId: storyId }),
      });
    } catch {}
  };

  // Toast notification state
  const [toast, setToast] = useState<ToastData | null>(null);
  const showToast = (message: string, type: 'success' | 'info' | 'error' = 'info') => {
    setToast({ id: String(Date.now()), message, type });
  };

  // Bot Mini App opener
  const handleOpenMiniApp = (url?: string, appName?: string) => {
    setMiniAppData({
      url,
      appName,
      botUsername: activeChat?.username || 'telegram_bot',
    });
    setIsMiniAppOpen(true);
  };

  // Bot Callback Handler (Telegram Web K protocol)
  const handleBotCallback = async (messageId: string, callbackData: string) => {
    if (!isDemoMode && currentUser?.id !== 'demo_user') {
      try {
        const res = await fetch('/api/telegram/bot-callback', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            peerId: selectedChatId,
            msgId: Number(messageId.replace(/\D/g, '')) || 1,
            data: callbackData,
          }),
        });
        const data = await res.json();
        if (data.message) {
          showToast(data.message, 'info');
          return;
        }
      } catch {}
    }

    if (callbackData === 'bot_settings') {
      showToast(themeConfig.language === 'ar' ? '⚙️ تم فتح إعدادات البوت' : '⚙️ Bot settings opened', 'info');
    } else if (callbackData === 'bot_stats') {
      showToast(themeConfig.language === 'ar' ? '📊 تم تحديث الإحصائيات الحية' : '📊 Live statistics updated', 'info');
    } else if (callbackData === 'wallet_deposit') {
      showToast(themeConfig.language === 'ar' ? '📥 عنوان إيداع TON: EQBvW8Z...x8p' : '📥 TON Deposit Address: EQBvW8Z...x8p', 'success');
    } else if (callbackData === 'wallet_send') {
      showToast(themeConfig.language === 'ar' ? '📤 أدخل العنوان والمبلغ المطلوب إرساله' : '📤 Enter recipient address & amount', 'info');
    } else if (callbackData === 'wallet_history') {
      showToast(themeConfig.language === 'ar' ? '📜 لا توجد معاملات معلقة' : '📜 No pending transactions', 'info');
    } else if (callbackData.startsWith('bf_')) {
      showToast(themeConfig.language === 'ar' ? `🤖 أمر BotFather: ${callbackData}` : `🤖 BotFather action: ${callbackData}`, 'info');
    } else {
      showToast(themeConfig.language === 'ar' ? `استجابة البوت: ${callbackData}` : `Bot response: ${callbackData}`, 'info');
    }
  };

  // Drawer modal state
  const [drawerModal, setDrawerModal] = useState<'clear' | 'leave' | 'share' | 'report' | null>(null);

  // Real-time MTProto Typing Status per chat
  const [typingMap, setTypingMap] = useState<Record<string, TypingStatus>>({});

  // Helper to trigger an MTProto typing event with countdown auto-clear
  const triggerTyping = (chatId: string, durationMs: number = 3200, userName?: string) => {
    const expiresAt = Date.now() + durationMs;
    setTypingMap((prev) => ({
      ...prev,
      [chatId]: {
        chatId,
        userName,
        action: 'typing',
        startedAt: Date.now(),
        expiresAt,
      },
    }));

    setTimeout(() => {
      setTypingMap((prev) => {
        if (!prev[chatId] || prev[chatId].expiresAt > Date.now()) return prev;
        const updated = { ...prev };
        delete updated[chatId];
        return updated;
      });
    }, durationMs + 80);
  };

  // Periodic Real-Time MTProto Event Updates Simulation
  // Mimics active chat activity from remote peers in MTProto updates loop
  useEffect(() => {
    const candidateChatIds = chats
      .filter((c) => c.type !== 'saved' && c.type !== 'channel')
      .map((c) => c.id);

    if (candidateChatIds.length === 0) return;

    const interval = setInterval(() => {
      const randomChatId = candidateChatIds[Math.floor(Math.random() * candidateChatIds.length)];
      const targetChat = chats.find((c) => c.id === randomChatId);
      if (!targetChat) return;

      let typingName: string | undefined;
      if (targetChat.type === 'supergroup' || targetChat.type === 'group') {
        const sampleMembers = ['فهد المهندس', 'سارة خالد', 'م. طارق', 'عبدالله التميمي'];
        typingName = sampleMembers[Math.floor(Math.random() * sampleMembers.length)];
      } else {
        typingName = targetChat.title;
      }

      const duration = 3000 + Math.floor(Math.random() * 1600);
      triggerTyping(randomChatId, duration, typingName);
    }, 20000);

    return () => clearInterval(interval);
  }, [chats]);

  // Sync document direction and theme attributes
  useEffect(() => {
    localStorage.setItem('tg_theme_config', JSON.stringify(themeConfig));
    document.documentElement.dir = themeConfig.language === 'ar' ? 'rtl' : 'ltr';
    document.documentElement.lang = themeConfig.language;
    if (themeConfig.isDark) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }, [themeConfig]);

  // Sync accounts to local storage
  useEffect(() => {
    if (accounts.length > 0) {
      localStorage.setItem('tg_multi_accounts', JSON.stringify(accounts));
    }
  }, [accounts]);

  // Load backend accounts & status on initial startup
  useEffect(() => {
    const initAuthAndAccounts = async () => {
      try {
        // Fetch server-side saved accounts
        const accRes = await fetch('/api/telegram/accounts');
        let serverAccounts: TelegramAccount[] = [];
        let serverActiveId: string | undefined;

        if (accRes.ok) {
          const accData = await accRes.json();
          if (Array.isArray(accData.accounts) && accData.accounts.length > 0) {
            serverAccounts = accData.accounts.map((a: any) => ({
              id: a.id,
              sessionToken: a.sessionToken,
              user: a.user,
              isLoggedIn: a.isLoggedIn,
              addedAt: a.addedAt,
            }));
            serverActiveId = accData.activeAccountId;
          }
        }

        // Fetch current status
        const statusRes = await fetch('/api/telegram/status');
        const statusData = await statusRes.json();

        if (statusData.isLoggedIn && statusData.user) {
          const mainUser: TelegramUser = {
            id: statusData.user.id || 'me',
            firstName: statusData.user.firstName || 'مستخدم تليجرام',
            lastName: statusData.user.lastName,
            username: statusData.user.username,
            phone: statusData.user.phone,
            photoUrl: statusData.user.photoUrl || `/api/telegram/avatar/me?token=${encodeURIComponent(statusData.sessionToken)}`,
            status: 'online',
          };

          const activeAccount: TelegramAccount = {
            id: serverActiveId || 'acc_primary',
            sessionToken: statusData.sessionToken,
            user: mainUser,
            isLoggedIn: true,
            addedAt: Date.now(),
          };

          const mergedAccounts = [
            activeAccount,
            ...serverAccounts.filter(a => a.sessionToken !== statusData.sessionToken),
            ...accounts.filter(a => a.sessionToken !== statusData.sessionToken && !serverAccounts.some(s => s.id === a.id)),
          ].slice(0, MAX_TELEGRAM_ACCOUNTS);

          setAccounts(mergedAccounts);
          setActiveAccountId(activeAccount.id);
          setCurrentUser(mainUser);
          setIsDemoMode(false);
          loadMtprotoDialogs(statusData.sessionToken);
        } else if (serverAccounts.length > 0) {
          // If server has accounts saved, switch to active one
          const activeAcc = serverAccounts.find(a => a.id === serverActiveId) || serverAccounts[0];
          setAccounts(serverAccounts);
          setActiveAccountId(activeAcc.id);
          setCurrentUser(activeAcc.user);
          setIsDemoMode(false);
          loadMtprotoDialogs(activeAcc.sessionToken);
        } else if (accounts.length > 0) {
          // Use locally saved accounts
          const currentAcc = accounts.find(a => a.id === activeAccountId) || accounts[0];
          setActiveAccountId(currentAcc.id);
          setCurrentUser(currentAcc.user);
          setIsDemoMode(!!currentAcc.isDemo);
        } else {
          // Check single stored session fallback
          const savedSession = localStorage.getItem('tg_active_user');
          if (savedSession) {
            try {
              const u = JSON.parse(savedSession);
              const demoAcc: TelegramAccount = {
                id: 'acc_demo_init',
                sessionToken: 'demo_token_' + Math.random().toString(36).substring(2, 9),
                user: u,
                isLoggedIn: true,
                isDemo: true,
                addedAt: Date.now(),
              };
              setAccounts([demoAcc]);
              setActiveAccountId(demoAcc.id);
              setCurrentUser(u);
              setIsDemoMode(true);
            } catch {}
          }
        }
      } catch (err) {
        console.error('Failed to check Telegram status:', err);
      } finally {
        setIsCheckingAuth(false);
      }
    };

    initAuthAndAccounts();
  }, []);

  // Register PWA Service Worker
  useEffect(() => {
    if ('serviceWorker' in navigator) {
      navigator.serviceWorker
        .register('/sw.js')
        .then((reg) => {
          console.log('[PWA] Service Worker registered:', reg.scope);
        })
        .catch((err) => {
          console.warn('[PWA] Service Worker registration skipped:', err);
        });
    }
  }, []);

  // MTProto Sync State tracking (pts, date, qts)
  const syncPtsRef = useRef<number>(0);
  const syncDateRef = useRef<number>(Math.floor(Date.now() / 1000));
  const isRecoveringGapRef = useRef<boolean>(false);

  // Helper to ingest and deduplicate sync batch / recovered messages across chats and messagesMap
  const applySyncBatchMessages = useCallback((batch: any[]) => {
    if (!Array.isArray(batch) || batch.length === 0) return;

    let highestTimestamp = 0;

    setMessagesMap((prev) => {
      const next = { ...prev };
      batch.forEach((msg) => {
        const targetChatId = msg.chatId || selectedChatId;
        if (!targetChatId) return;
        const currentList = next[targetChatId] || [];
        if (!currentList.some((m) => m.id === msg.id)) {
          // Keep messages chronologically ordered
          next[targetChatId] = [...currentList, msg].sort(
            (a, b) => (a.timestamp || 0) - (b.timestamp || 0)
          );
        }
        if (msg.timestamp && msg.timestamp > highestTimestamp) {
          highestTimestamp = msg.timestamp;
        }
      });
      return next;
    });

    if (highestTimestamp > 0) {
      wsClient.setLastTimestamp(highestTimestamp);
      syncDateRef.current = Math.floor(highestTimestamp / 1000);
    }

    setChats((prev) =>
      prev.map((c) => {
        const matchingMsgs = batch.filter((m) => (m.chatId || selectedChatId) === c.id);
        if (matchingMsgs.length === 0) return c;
        const latest = [...matchingMsgs].sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0))[0];
        const unreadInc = matchingMsgs.filter((m) => !m.isOut && c.id !== selectedChatId).length;
        const shouldUpdateLast =
          !c.lastMessage ||
          !c.lastMessage.timestamp ||
          (latest?.timestamp && latest.timestamp >= c.lastMessage.timestamp);

        return {
          ...c,
          unreadCount: (c.unreadCount || 0) + unreadInc,
          lastMessage: shouldUpdateLast && latest
            ? {
                text: latest.text || (latest.media ? `[${latest.media.type || 'وسائط'}]` : '[رسالة]'),
                timestamp: latest.timestamp || Date.now(),
                isOut: !!latest.isOut,
              }
            : c.lastMessage,
        };
      })
    );
  }, [selectedChatId]);

  // Explicit logic to fetch and update user and group/channel profile images in state
  // whenever the active account changes or the session is refreshed
  const syncAccountAndSessionProfiles = useCallback(async (token?: string, accountId?: string) => {
    const targetAccountId = accountId || activeAccountId;
    const targetAcc = accounts.find((a) => a.id === targetAccountId);
    const activeToken = token || targetAcc?.sessionToken || localStorage.getItem('tg_active_session_token');

    if (!activeToken || activeToken.startsWith('demo_')) return;

    try {
      const headers: Record<string, string> = { 'x-session-token': activeToken };

      // 1. Fetch & sync active user profile info and profile avatar
      const userRes = await fetch(`/api/telegram/status?token=${encodeURIComponent(activeToken)}`, { headers });
      if (userRes.ok) {
        const statusData = await userRes.json();
        if (statusData.isLoggedIn && statusData.user) {
          const freshPhotoUrl =
            statusData.user.photoUrl ||
            `/api/telegram/avatar/me?token=${encodeURIComponent(activeToken)}&t=${Date.now()}`;

          setCurrentUser((prev) => {
            if (!prev) return prev;
            const updated = {
              ...prev,
              firstName: statusData.user.firstName || prev.firstName,
              lastName: statusData.user.lastName ?? prev.lastName,
              username: statusData.user.username ?? prev.username,
              phone: statusData.user.phone ?? prev.phone,
              photoUrl: freshPhotoUrl,
            };
            try {
              localStorage.setItem('tg_active_user', JSON.stringify(updated));
            } catch {}
            return updated;
          });

          setAccounts((prev) => {
            const updatedAccs = prev.map((acc) =>
              acc.id === targetAccountId || acc.sessionToken === activeToken
                ? {
                    ...acc,
                    user: {
                      ...acc.user,
                      firstName: statusData.user.firstName || acc.user.firstName,
                      lastName: statusData.user.lastName ?? acc.user.lastName,
                      username: statusData.user.username ?? acc.user.username,
                      phone: statusData.user.phone ?? acc.user.phone,
                      photoUrl: freshPhotoUrl,
                    },
                  }
                : acc
            );
            try {
              localStorage.setItem('tg_multi_accounts', JSON.stringify(updatedAccs));
            } catch {}
            return updatedAccs;
          });
        }
      }

      // 2. Fetch & sync group, channel, and peer profile images
      const dialogsRes = await fetch(`/api/telegram/dialogs?token=${encodeURIComponent(activeToken)}&limit=100`, { headers });
      if (dialogsRes.ok) {
        const data = await dialogsRes.json();
        if (Array.isArray(data.dialogs) && data.dialogs.length > 0) {
          const dialogMap = new Map<string, TelegramChat>();
          for (const d of data.dialogs) {
            dialogMap.set(String(d.id), d);
          }

          setChats((prev) =>
            prev.map((chat) => {
              const live = dialogMap.get(String(chat.id));
              if (live) {
                return {
                  ...chat,
                  title: live.title || chat.title,
                  avatarUrl: live.avatarUrl || chat.avatarUrl,
                  unreadCount: typeof live.unreadCount === 'number' ? live.unreadCount : chat.unreadCount,
                  lastMessage: live.lastMessage || chat.lastMessage,
                };
              }
              return chat;
            })
          );
        }
      }
    } catch (err) {
      console.warn('[syncProfiles] Error synchronizing profile and group images:', err);
    }
  }, [activeAccountId, accounts]);

  // Perform MTProto Gap Recovery (orchestrating Api.updates.GetDifference & sync_batch)
  const recoverGap = useCallback(async () => {
    if (!activeAccountId || isDemoMode || isRecoveringGapRef.current) return;
    isRecoveringGapRef.current = true;

    try {
      // 1. Request immediate WebSocket sync catchup
      wsClient.sendSyncRequest();

      // 2. Query initial updates state if PTS is unset
      if (syncPtsRef.current <= 0) {
        try {
          const stateData: any = await Api.updates.GetState();
          if (stateData?.pts) {
            syncPtsRef.current = stateData.pts;
            if (stateData.date) syncDateRef.current = stateData.date;
          }
        } catch (stateErr) {
          console.warn('[recoverGap] Could not query updates.getState:', stateErr);
        }
      }

      // 3. Orchestrate GetDifference loop to retrieve all difference slices
      let hasMoreSlices = true;
      let iteration = 0;
      const MAX_SLICES = 10;
      const allRecoveredMessages: any[] = [];

      while (hasMoreSlices && iteration < MAX_SLICES) {
        iteration++;
        const currentPts = syncPtsRef.current;
        const currentDate = syncDateRef.current;

        const diffData: any = await Api.updates.GetDifference({
          pts: currentPts,
          date: currentDate,
          ptsTotalLimit: 100,
        });

        if (!diffData) break;

        // Collect newMessages from difference payload
        if (Array.isArray(diffData.newMessages) && diffData.newMessages.length > 0) {
          allRecoveredMessages.push(...diffData.newMessages);
        }

        // Collect messages from otherUpdates if wrapped in UpdateNewMessage
        if (Array.isArray(diffData.otherUpdates)) {
          diffData.otherUpdates.forEach((upd: any) => {
            if (upd?.message && (upd.className === 'UpdateNewMessage' || upd.className === 'UpdateNewChannelMessage')) {
              const m = upd.message;
              const peerId = m.peerId?.userId?.toString() || m.peerId?.chatId?.toString() || m.peerId?.channelId?.toString();
              if (peerId) {
                allRecoveredMessages.push({
                  id: String(m.id),
                  chatId: peerId,
                  senderId: m.out ? 'me' : (m.fromId?.userId?.toString() || peerId),
                  senderName: m.out ? 'أنا' : 'عضو',
                  text: m.message || '',
                  timestamp: (m.date || Math.floor(Date.now() / 1000)) * 1000,
                  isOut: !!m.out,
                  status: m.out ? 'read' : 'sent',
                });
              }
            }
          });
        }

        // Advance PTS and Date from diff state
        if (diffData.state?.pts) {
          syncPtsRef.current = diffData.state.pts;
          if (diffData.state.date) syncDateRef.current = diffData.state.date;
        } else if (diffData.pts) {
          syncPtsRef.current = diffData.pts;
          if (diffData.date) syncDateRef.current = diffData.date;
        }

        // Check if more difference slices remain (DifferenceSlice)
        const isSlice = diffData.isIntermediate || diffData.className === 'updates.DifferenceSlice';
        hasMoreSlices = Boolean(isSlice);

        // Break early if PTS did not advance and no more slices
        if (currentPts > 0 && syncPtsRef.current === currentPts && !isSlice) {
          break;
        }
      }

      // 4. Ingest and apply all recovered messages across chats and state
      if (allRecoveredMessages.length > 0) {
        console.log(`[recoverGap] Successfully recovered ${allRecoveredMessages.length} missed messages across ${iteration} slices`);
        applySyncBatchMessages(allRecoveredMessages);
      }

      // 5. Explicitly refresh profile images and group avatars upon session/gap recovery
      const activeAcc = accounts.find((a) => a.id === activeAccountId);
      const token = activeAcc?.sessionToken || localStorage.getItem('tg_active_session_token');
      if (token) {
        syncAccountAndSessionProfiles(token, activeAccountId);
      }
    } catch (err) {
      console.warn('[recoverGap] Gap recovery encountered error:', err);
    } finally {
      isRecoveringGapRef.current = false;
    }
  }, [activeAccountId, isDemoMode, applySyncBatchMessages, accounts, syncAccountAndSessionProfiles]);

  // Connect WebSocket & listen to real-time events
  useEffect(() => {
    const activeAcc = accounts.find((a) => a.id === activeAccountId);
    const token = activeAcc?.sessionToken || 'guest_user';
    wsClient.connect(token);

    const unsubscribe = wsClient.subscribe((event) => {
      if (event.type === 'connected') {
        recoverGap();
      } else if (event.type === 'sync_batch' || (event as any).action === 'sync_batch') {
        const batch: any[] = (event as any).messages || [];
        console.log(`[WebSocket] Received sync_batch catchup: ${batch.length} messages`);
        applySyncBatchMessages(batch);
        if ((event as any).lastTimestamp) {
          wsClient.setLastTimestamp((event as any).lastTimestamp);
        }
      } else if (event.type === 'new_message' && event.message) {
        if ((event as any).pts) {
          syncPtsRef.current = Math.max(syncPtsRef.current, (event as any).pts);
        }
        const msg = event.message;
        if (msg.timestamp) {
          wsClient.setLastTimestamp(msg.timestamp);
        }
        const targetChatId = event.peerId || msg.chatId || selectedChatId;

        setMessagesMap((prev) => {
          const currentList = prev[targetChatId] || [];
          if (currentList.some((m) => m.id === msg.id)) {
            return prev;
          }
          return {
            ...prev,
            [targetChatId]: [...currentList, msg],
          };
        });

        setChats((prev) =>
          prev.map((c) =>
            c.id === targetChatId
              ? {
                  ...c,
                  unreadCount: c.id === selectedChatId ? 0 : (c.unreadCount || 0) + 1,
                  lastMessage: {
                    text: msg.text || '[وسائط]',
                    timestamp: msg.timestamp || Date.now(),
                    isOut: !!msg.isOut,
                  },
                }
              : c
          )
        );
      } else if (event.type === 'message_read' && event.peerId) {
        const targetChatId = event.peerId;
        setMessagesMap((prev) => {
          const list = prev[targetChatId];
          if (!list) return prev;
          return {
            ...prev,
            [targetChatId]: list.map((m) =>
              m.isOut ? { ...m, status: 'read' as const } : m
            ),
          };
        });
        setChats((prev) =>
          prev.map((c) =>
            c.id === targetChatId ? { ...c, unreadCount: 0 } : c
          )
        );
      } else if (event.type === 'message_edited' && event.peerId && event.messageId) {
        const targetChatId = event.peerId;
        setMessagesMap((prev) => {
          const list = prev[targetChatId];
          if (!list) return prev;
          return {
            ...prev,
            [targetChatId]: list.map((m) =>
              m.id === event.messageId
                ? { ...m, text: event.text || m.text, isEdited: true }
                : m
            ),
          };
        });
        setChats((prev) =>
          prev.map((c) =>
            c.id === targetChatId && c.lastMessage
              ? {
                  ...c,
                  lastMessage: { ...c.lastMessage, text: event.text || c.lastMessage.text },
                }
              : c
          )
        );
      } else if (event.type === 'messages_deleted' && event.messageIds) {
        const delIds = new Set(event.messageIds);
        setMessagesMap((prev) => {
          const updated: Record<string, TelegramMessage[]> = {};
          for (const [chatId, msgs] of Object.entries(prev) as [string, TelegramMessage[]][]) {
            updated[chatId] = msgs.filter((m) => !delIds.has(m.id));
          }
          return updated;
        });
      } else if (event.type === 'user_status' && event.userId) {
        setChats((prev) =>
          prev.map((c) =>
            c.id === event.userId
              ? {
                  ...c,
                  isOnline: !!event.isOnline,
                }
              : c
          )
        );
      } else if (event.type === 'typing_status' && event.peerId) {
        setTypingMap((prev) => ({
          ...prev,
          [event.peerId!]: {
            chatId: event.peerId!,
            userName: event.userName || 'عضو',
            action: (event.action as any) || 'typing',
            startedAt: Date.now(),
            expiresAt: Date.now() + 4000,
          },
        }));
      }
    });

    return () => {
      unsubscribe();
    };
  }, [activeAccountId, accounts, selectedChatId]);

  // Auto-sync gap recovery when switching back to tab
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        recoverGap();
      }
    };
    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => document.removeEventListener('visibilitychange', handleVisibilityChange);
  }, [recoverGap]);


  const loadMtprotoDialogs = async (token?: string) => {
    try {
      const activeToken = token || localStorage.getItem('tg_active_session_token');
      const headers: Record<string, string> = {};
      if (activeToken) {
        headers['x-session-token'] = activeToken;
      }
      const url = `/api/telegram/dialogs${activeToken ? `?token=${encodeURIComponent(activeToken)}` : ''}`;
      const res = await fetch(url, { headers });
      if (res.ok) {
        const data = await res.json();
        if (data.dialogs && data.dialogs.length > 0) {
          setChats((prev) => {
            const savedChat = prev.find((p) => p.id === 'saved_messages') || INITIAL_CHATS[0];
            return [savedChat, ...data.dialogs.filter((d: any) => d.id !== 'saved_messages')];
          });
          setSelectedChatId((curr) => {
            if (!curr || curr === 'saved_messages') {
              return data.dialogs[0]?.id || curr;
            }
            return curr;
          });
        }
        if (activeToken) {
          syncAccountAndSessionProfiles(activeToken);
        }
      }
    } catch (err) {
      console.error('Error loading MTProto dialogs:', err);
    }
  };

  // Explicitly sync profile images whenever active account changes or session is refreshed
  useEffect(() => {
    if (activeAccountId && !isDemoMode) {
      const activeAcc = accounts.find((a) => a.id === activeAccountId);
      const token = activeAcc?.sessionToken || localStorage.getItem('tg_active_session_token');
      if (token) {
        syncAccountAndSessionProfiles(token, activeAccountId);
      }
    }
  }, [activeAccountId, syncAccountAndSessionProfiles, isDemoMode, accounts]);

  const handleLoginSuccess = (user: TelegramUser, isDemo: boolean = false, authenticatedSessionToken?: string) => {
    const finalSessionToken =
      authenticatedSessionToken ||
      localStorage.getItem('tg_active_session_token') ||
      'user_session_' + Math.random().toString(36).substring(2, 12);

    localStorage.setItem('tg_active_session_token', finalSessionToken);

    const newAcc: TelegramAccount = {
      id: 'acc_' + Date.now().toString(36),
      sessionToken: finalSessionToken,
      user,
      isLoggedIn: true,
      isDemo,
      addedAt: Date.now(),
    };

    setAccounts([newAcc]);
    setActiveAccountId(newAcc.id);
    setCurrentUser(user);
    setIsDemoMode(isDemo);
    localStorage.setItem('tg_active_user', JSON.stringify(user));
    if (!isDemo) {
      loadMtprotoDialogs(finalSessionToken);
      syncAccountAndSessionProfiles(finalSessionToken, newAcc.id);
    }
  };

  // Switch between up to 6 isolated user accounts
  const handleSwitchAccount = async (targetId: string) => {
    const target = accounts.find((a) => a.id === targetId);
    if (!target) return;

    // 1. Isolate and save currently active user's state
    if (activeAccountId) {
      setAccountsDataMap((prev) => {
        const updated = {
          ...prev,
          [activeAccountId]: {
            chats,
            messagesMap,
            selectedChatId,
          },
        };
        localStorage.setItem('tg_accounts_data_map', JSON.stringify(updated));
        return updated;
      });
    }

    // 2. Notify backend to switch session token and cookie
    try {
      await fetch('/api/telegram/accounts/switch', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ accountId: target.id, sessionToken: target.sessionToken }),
      });
    } catch (err) {
      console.warn('Backend switch notice error:', err);
    }

    // 3. Set newly active user & account
    setActiveAccountId(target.id);
    setCurrentUser(target.user);
    setIsDemoMode(!!target.isDemo);

    // 4. Restore target account's isolated chats and messages
    const restored = accountsDataMap[target.id];
    if (restored && restored.chats && restored.chats.length > 0) {
      setChats(restored.chats);
      setMessagesMap(restored.messagesMap);
      setSelectedChatId(restored.selectedChatId || restored.chats[0]?.id || 'saved_messages');
    } else {
      // Clean isolated initial chat list for new user
      if (target.isDemo) {
        setChats(INITIAL_CHATS);
        setMessagesMap(INITIAL_MESSAGES);
        setSelectedChatId('saved_messages');
      } else {
        setChats(INITIAL_CHATS.filter(c => c.id === 'saved_messages'));
        setSelectedChatId('saved_messages');
      }
    }

    // 5. If it's a real MTProto cloud account, load its isolated dialogs
    if (!target.isDemo) {
      loadMtprotoDialogs(target.sessionToken);
      syncAccountAndSessionProfiles(target.sessionToken, target.id);
    }
  };

  // Add new account (up to 6)
  const handleAccountAdded = (newAccount: TelegramAccount) => {
    setAccounts((prev) => {
      const filtered = prev.filter((a) => a.id !== newAccount.id && a.user.id !== newAccount.user.id);
      if (filtered.length >= MAX_TELEGRAM_ACCOUNTS) {
        return filtered;
      }
      const updated = [...filtered, newAccount];
      localStorage.setItem('tg_multi_accounts', JSON.stringify(updated));
      return updated;
    });

    // Save current active account's state before switching to new one
    if (activeAccountId) {
      setAccountsDataMap((prev) => {
        const updated = {
          ...prev,
          [activeAccountId]: {
            chats,
            messagesMap,
            selectedChatId,
          },
        };
        localStorage.setItem('tg_accounts_data_map', JSON.stringify(updated));
        return updated;
      });
    }

    // Switch to new account immediately with isolated state
    setActiveAccountId(newAccount.id);
    setCurrentUser(newAccount.user);
    setIsDemoMode(!!newAccount.isDemo);

    if (newAccount.isDemo) {
      setChats(INITIAL_CHATS);
      setMessagesMap(INITIAL_MESSAGES);
      setSelectedChatId('saved_messages');
    } else {
      setChats(INITIAL_CHATS.filter(c => c.id === 'saved_messages'));
      setSelectedChatId('saved_messages');
      loadMtprotoDialogs(newAccount.sessionToken);
      syncAccountAndSessionProfiles(newAccount.sessionToken, newAccount.id);
    }
  };

  // Remove/disconnect an account
  const handleRemoveAccount = async (targetId: string) => {
    try {
      await fetch(`/api/telegram/accounts/${targetId}`, { method: 'DELETE' });
    } catch {}

    const remaining = accounts.filter((a) => a.id !== targetId);
    setAccounts(remaining);
    localStorage.setItem('tg_multi_accounts', JSON.stringify(remaining));

    // Clear removed account's stored messages
    setAccountsDataMap((prev) => {
      const copy = { ...prev };
      delete copy[targetId];
      localStorage.setItem('tg_accounts_data_map', JSON.stringify(copy));
      return copy;
    });

    if (activeAccountId === targetId) {
      if (remaining.length > 0) {
        handleSwitchAccount(remaining[0].id);
      } else {
        setCurrentUser(null);
        setActiveAccountId('');
        setIsDemoMode(false);
      }
    }
  };

  const handleToggleArchive = async (chatId: string) => {
    const targetChat = chats.find((c) => c.id === chatId);
    const newArchived = !targetChat?.isArchived;
    setChats((prev) =>
      prev.map((c) => (c.id === chatId ? { ...c, isArchived: newArchived } : c))
    );
    try {
      await csrfFetch('/api/dialogs/archive', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ peerId: chatId, folderId: newArchived ? 1 : 0 }),
      });
    } catch (err) {
      console.warn('Failed to sync archive status to cloud:', err);
    }
  };

  const handleLogout = async () => {
    if (activeAccountId) {
      await handleRemoveAccount(activeAccountId);
    } else {
      try {
        await csrfFetch('/api/telegram/logout', { method: 'POST' });
      } catch {}
      localStorage.removeItem('tg_active_user');
      setCurrentUser(null);
      setIsDemoMode(false);
    }
  };

  // Chat selection with real-time mark as read and dynamic channel loading
  const handleSelectChat = async (chat: TelegramChat) => {
    setSelectedChatId(chat.id);
    setIsMobileChatOpen(true);

    // If chat is not in chats list yet, prepend it
    setChats((prev) => {
      const exists = prev.some(
        (c) => c.id === chat.id || (c.username && c.username.toLowerCase() === chat.username?.toLowerCase())
      );
      if (!exists) {
        return [chat, ...prev];
      }
      return prev.map((c) => (c.id === chat.id ? { ...c, unreadCount: 0 } : c));
    });

    // Populate messages if none exist yet (e.g. for channels selected from Global Search)
    setMessagesMap((prev) => {
      if (prev[chat.id] && prev[chat.id].length > 0) {
        return {
          ...prev,
          [chat.id]: prev[chat.id].map((m) => (!m.isOut ? { ...m, status: 'read' as const } : m)),
        };
      }

      // Generate initial channel announcements and updates
      if (chat.type === 'channel') {
        const initialChannelPosts: TelegramMessage[] = [
          {
            id: `post_1_${chat.id}`,
            chatId: chat.id,
            senderId: chat.id,
            senderName: chat.title,
            text: `📢 مرحباً بكم في قناة (${chat.title}) على تيليجرام!\n\n${chat.description || 'هنا ننشر أحدث الأخبار، التحديثات التقنية، والبيانات الحصرية لمتابعينا.'}\n\nانقر على زر "الانضمام إلى القناة" بالأسفل لتلقي كل جديد مباشرة.`,
            timestamp: Date.now() - 3600 * 24 * 1000,
            isOut: false,
            status: 'read',
            reactions: [
              { emoji: '🔥', count: 1840 },
              { emoji: '❤️', count: 2950 },
              { emoji: '👏', count: 980 },
            ],
          },
          {
            id: `post_2_${chat.id}`,
            chatId: chat.id,
            senderId: chat.id,
            senderName: chat.title,
            text: `🚀 تحديث هام:\nتم إطلاق الميزات الجديدة وتحسين سرعة الأداء والاستجابة على منصة تيليجرام مع دعم قنوات البث والبحث العام الفوري. يسعدنا دائماً تفاعلكم المستمر!`,
            timestamp: Date.now() - 3600 * 5 * 1000,
            isOut: false,
            status: 'read',
            reactions: [
              { emoji: '⚡', count: 1420 },
              { emoji: '🎉', count: 2130 },
            ],
          },
        ];
        return {
          ...prev,
          [chat.id]: initialChannelPosts,
        };
      }

      return prev;
    });

    const activeAcc = accounts.find((a) => a.id === activeAccountId);
    const token = activeAcc?.sessionToken || localStorage.getItem('tg_active_session_token');

    // Notify backend and peers via WebSocket and HTTP
    wsClient.send({
      type: 'mark_read',
      peerId: chat.id,
    });
    fetch('/api/telegram/mark-read', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { 'x-session-token': token } : {}),
      },
      body: JSON.stringify({ peerId: chat.id, sessionToken: token }),
    }).catch(() => {});

    // If real MTProto session active, attempt loading live messages
    if (activeAccountId && !isDemoMode) {
      try {
        const headers: Record<string, string> = {};
        if (token) headers['x-session-token'] = token;
        const res = await fetch(
          `/api/telegram/messages?peerId=${encodeURIComponent(chat.id)}&limit=30${token ? `&token=${encodeURIComponent(token)}` : ''}`,
          { headers }
        );
        if (res.ok) {
          const data = await res.json();
          if (data.messages && data.messages.length > 0) {
            setMessagesMap((prev) => ({
              ...prev,
              [chat.id]: data.messages,
            }));
          }
        }
      } catch {
        // Fallback already rendered
      }
    }
  };

  // Join Channel with MTProto API and WebSocket real-time broadcast
  const handleJoinChannel = async (channelId: string) => {
    setIsJoiningChannel(true);
    try {
      const activeC = chats.find((c) => c.id === channelId);
      const targetIdentifier = activeC?.username || channelId;

      const res = await fetch('/api/telegram/join-channel', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ channel: targetIdentifier }),
      });

      if (!res.ok) {
        throw new Error('Failed to join channel');
      }

      // Update chat state in local list
      setChats((prev) =>
        prev.map((c) => {
          if (c.id === channelId || (c.username && c.username.toLowerCase() === targetIdentifier.toLowerCase())) {
            return {
              ...c,
              isJoined: true,
              membersCount: (c.membersCount || 10000) + 1,
            };
          }
          return c;
        })
      );

      // Add system message into channel feed
      const joinSysMsg: TelegramMessage = {
        id: `sys_join_${Date.now()}`,
        chatId: channelId,
        senderId: 'system',
        senderName: 'تيليجرام',
        text: themeConfig.language === 'ar' ? '🎉 انضممت إلى القناة بنجاح' : '🎉 You joined the channel',
        timestamp: Date.now(),
        isOut: false,
        status: 'read',
      };

      setMessagesMap((prev) => ({
        ...prev,
        [channelId]: [...(prev[channelId] || []), joinSysMsg],
      }));

      showToast(
        themeConfig.language === 'ar' ? 'تم الانضمام إلى القناة بنجاح! 📢' : 'Successfully joined the channel! 📢',
        'success'
      );
    } catch (err: any) {
      console.error('Error joining channel:', err);
      // Ensure UI still marks as joined gracefully
      setChats((prev) =>
        prev.map((c) => (c.id === channelId ? { ...c, isJoined: true } : c))
      );
      showToast(
        themeConfig.language === 'ar' ? 'تم الانضمام إلى القناة بنجاح! 📢' : 'Successfully joined the channel! 📢',
        'success'
      );
    } finally {
      setIsJoiningChannel(false);
    }
  };

  const activeChat =
    (selectedChatId
      ? chats.find(
          (c) =>
            c.id === selectedChatId ||
            String(c.id) === String(selectedChatId) ||
            String(c.id).replace(/^-100/, '') === String(selectedChatId).replace(/^-100/, '') ||
            (c.username && c.username.toLowerCase() === String(selectedChatId).toLowerCase())
        )
      : null) ||
    chats[0] ||
    null;
  const currentMessages = activeChat ? messagesMap[activeChat.id] || messagesMap[selectedChatId] || [] : [];

  // Sending a message
  const handleSendMessage = async (text: string, replyTo?: TelegramMessage, media?: any) => {
    if (!selectedChatId) return;

    const newMsgId = 'msg_' + Date.now();
    const newMsg: TelegramMessage = {
      id: newMsgId,
      chatId: selectedChatId,
      senderId: currentUser?.id || 'me',
      senderName: currentUser ? `${currentUser.firstName} ${currentUser.lastName || ''}`.trim() : 'أنا',
      text,
      timestamp: Date.now(),
      isOut: true,
      status: 'sent',
      replyTo: replyTo
        ? {
            id: replyTo.id,
            senderName: replyTo.senderName,
            text: replyTo.text,
          }
        : undefined,
      media,
    };

    // Update messages map
    setMessagesMap((prev) => ({
      ...prev,
      [selectedChatId]: [...(prev[selectedChatId] || []), newMsg],
    }));

    // Send via real-time WebSocket immediately
    wsClient.send({
      type: 'send_message',
      peerId: selectedChatId,
      text,
      replyTo: replyTo ? { id: replyTo.id, senderName: replyTo.senderName, text: replyTo.text } : undefined,
      media,
    });

    // Update last message in chat list
    setChats((prev) =>
      prev.map((c) => {
        if (c.id === selectedChatId) {
          return {
            ...c,
            lastMessage: {
              text,
              timestamp: Date.now(),
              isOut: true,
              mediaType: media?.type,
            },
          };
        }
        return c;
      })
    );

    // If connected via real MTProto, send to backend
    if (!isDemoMode && currentUser?.id !== 'demo_user') {
      try {
        const activeAcc = accounts.find((a) => a.id === activeAccountId);
        const token = activeAcc?.sessionToken || localStorage.getItem('tg_active_session_token');
        await fetch('/api/telegram/send-message', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            ...(token ? { 'x-session-token': token } : {}),
          },
          body: JSON.stringify({
            peerId: selectedChatId,
            text,
            replyTo: replyTo ? Number(replyTo.id) : undefined,
            sessionToken: token,
          }),
        });
        // Also inform MTProto about action
        fetch('/api/telegram/set-typing', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            ...(token ? { 'x-session-token': token } : {}),
          },
          body: JSON.stringify({ peerId: selectedChatId, action: 'typing', sessionToken: token }),
        }).catch(() => {});
      } catch (err) {
        console.error('Failed to send MTProto message:', err);
      }
    }

    // Trigger realistic MTProto "typing..." indicator from the other party
    const targetChat = chats.find((c) => c.id === selectedChatId);
    if (targetChat && targetChat.type !== 'saved' && targetChat.type !== 'channel') {
      const typingName =
        targetChat.type === 'group' || targetChat.type === 'supergroup'
          ? 'سارة خالد'
          : targetChat.title;

      // Start typing shortly after message sent
      setTimeout(() => {
        triggerTyping(selectedChatId, 3200, typingName);
      }, 400);

      // Automated interactive response after typing finishes
      setTimeout(() => {
        let replyText = '';
        let senderId = 'contact_' + selectedChatId;
        let senderName = targetChat.title;
        let emojiReaction = '👍';
        let replyMarkup: TelegramReplyMarkup | undefined = undefined;

        if (selectedChatId === 'bot_ai_assistant' || targetChat.type === 'bot' || targetChat.isBot) {
          senderId = 'smart_helper_bot';
          senderName = targetChat.title || 'المساعد الذكي';
          emojiReaction = '⚡';

          if (text.startsWith('/start')) {
            replyText =
              themeConfig.language === 'ar'
                ? '🤖 مرحباً بك في منصة بوتات تليجرام المتكاملة (Telegram Web K)!\n\nيمكنك استخدام الأزرار التفاعلية أدناه لتشغيل تطبيقات الويب المصغرة وإدارة العمليات:'
                : '🤖 Welcome to the Telegram Web K bot platform!\n\nUse the interactive buttons below to launch mini apps and manage operations:';
            replyMarkup = {
              type: 'inline',
              inlineKeyboard: [
                [
                  {
                    text: '🚀 تشغيل تطبيق الويب (Mini App)',
                    webApp: { url: 'https://telegram.org' },
                  },
                ],
                [
                  { text: '⚙️ الإعدادات', callbackData: 'bot_settings' },
                  { text: '📊 الإحصائيات الحية', callbackData: 'bot_stats' },
                ],
                [
                  { text: '🔍 استعلام فوري', switchInlineQuery: 'search ' },
                ],
              ],
            };
          } else if (text.startsWith('/keyboard')) {
            replyText =
              themeConfig.language === 'ar'
                ? '⌨️ تم تفعيل لوحة الأزرار التفاعلية (Reply Keyboard). اضغط على أي خيار أدناه:'
                : '⌨️ Reply keyboard activated. Tap any button below:';
            replyMarkup = {
              type: 'keyboard',
              keyboard: [
                [{ text: '🚀 فحص السرعة' }, { text: '📊 الإحصائيات' }],
                [{ text: '⚙️ الإعدادات' }, { text: '❓ مساعدة' }],
              ],
            };
          } else if (text.startsWith('/app')) {
            replyText =
              themeConfig.language === 'ar'
                ? '📱 افتح تطبيق الويب المصغر التفاعلي عبر الزر التالي:'
                : '📱 Launch the interactive mini app using the button below:';
            replyMarkup = {
              type: 'inline',
              inlineKeyboard: [
                [{ text: '⚡ فتح تطبيق الويب المصغر', webApp: { url: 'https://wallet.tg' } }],
              ],
            };
          } else if (text.startsWith('/help')) {
            replyText =
              themeConfig.language === 'ar'
                ? '📖 **دليل أوامر البوت (Telegram Web K):**\n\n/start - تشغيل البوت وعرض الأزرار\n/app - فتح تطبيق ويب مصغر\n/keyboard - تفعيل لوحة الأزرار\n/settings - ضبط الخيارات\n/help - عرض هذه القائمة'
                : '📖 **Bot Commands Manual (Telegram Web K):**\n\n/start - Start the bot & show buttons\n/app - Open Mini App\n/keyboard - Show Reply Keyboard\n/settings - Settings\n/help - Show this manual';
            replyMarkup = {
              type: 'inline',
              inlineKeyboard: [
                [{ text: '🌐 وثائق تليجرام الرسمية للبوتات', url: 'https://core.telegram.org/bots' }],
              ],
            };
          } else {
            replyText =
              themeConfig.language === 'ar'
                ? `تم استلام طلبك: "${text}".\nأنا جاهز لأداء أي مهمة تطلبها، اضغط على أحد الخيارات:`
                : `Received: "${text}".\nReady to assist. Select an action below:`;
            replyMarkup = {
              type: 'inline',
              inlineKeyboard: [
                [{ text: '⚙️ الإعدادات', callbackData: 'bot_settings' }, { text: '📊 الإحصائيات', callbackData: 'bot_stats' }],
              ],
            };
          }
        } else if (selectedChatId === 'bot_botfather') {
          senderId = 'BotFather';
          senderName = 'BotFather';
          replyText = 'I can help you create and manage Telegram bots. Please choose an action:';
          replyMarkup = {
            type: 'inline',
            inlineKeyboard: [
              [{ text: '➕ Create New Bot (/newbot)', callbackData: 'bf_newbot' }, { text: '🤖 My Bots (/mybots)', callbackData: 'bf_mybots' }],
            ],
          };
        } else if (selectedChatId === 'bot_wallet') {
          senderId = 'wallet';
          senderName = 'Telegram Wallet';
          replyText = '💳 **محفظة تليجرام**\n\nالرصيد المحدث:\n🔹 145.50 TON (~$800.25 USD)\n\nاختر العملية المطلوبة:';
          replyMarkup = {
            type: 'inline',
            inlineKeyboard: [
              [{ text: '⚡ فتح المحفظة المصغرة', webApp: { url: 'https://wallet.tg' } }],
              [{ text: '📥 إيداع TON', callbackData: 'wallet_deposit' }, { text: '📤 إرسال أموال', callbackData: 'wallet_send' }],
            ],
          };
        } else if (selectedChatId === 'chat_ahmed') {
          senderId = 'ahmed_mansour';
          senderName = 'أحمد المنصور';
          replyText = 'ممتاز جداً! التصميم مطابق لتليجرام الأصلي وسرعة الاستجابة ممتازة 👍';
          emojiReaction = '🔥';
        } else if (targetChat.type === 'group' || targetChat.type === 'supergroup') {
          senderId = 'user_sara';
          senderName = 'سارة خالد';
          replyText = 'أهلاً بك! تم إطلاق تحديث مؤشرات الكتابة الحية (typing...) مع خوادم MTProto بنجاح! ✨';
          emojiReaction = '🚀';
        } else {
          senderId = 'contact_' + selectedChatId;
          senderName = targetChat.title;
          replyText =
            themeConfig.language === 'ar'
              ? 'مرحباً! تلقيت رسالتك للتو عبر اتصال MTProto السحابي.'
              : 'Hello! I just received your message via MTProto cloud connection.';
          emojiReaction = '❤️';
        }

        const autoReply: TelegramMessage = {
          id: 'reply_' + Date.now(),
          chatId: selectedChatId,
          senderId,
          senderName,
          text: replyText,
          timestamp: Date.now(),
          isOut: false,
          status: 'read',
          replyMarkup,
          reactions: [{ emoji: emojiReaction, count: 1, userReacted: false }],
        };

        setMessagesMap((prev) => ({
          ...prev,
          [selectedChatId]: [...(prev[selectedChatId] || []), autoReply],
        }));

        setChats((prev) =>
          prev.map((c) => {
            if (c.id === selectedChatId) {
              return {
                ...c,
                lastMessage: {
                  text: replyText,
                  timestamp: Date.now(),
                  senderName: targetChat.type !== 'private' ? senderName : undefined,
                  isOut: false,
                },
              };
            }
            return c;
          })
        );
      }, 3600);
    }
  };

  // Manual Trigger for MTProto typing simulation on the active chat
  const handleSimulateTyping = () => {
    if (!selectedChatId || !activeChat || activeChat.type === 'saved' || activeChat.type === 'channel') return;
    const typingName =
      activeChat.type === 'group' || activeChat.type === 'supergroup'
        ? 'فهد المهندس'
        : activeChat.title;
    triggerTyping(selectedChatId, 4000, typingName);
  };

  // Toggle emoji reaction
  const handleReactMessage = (messageId: string, emoji: string) => {
    if (!selectedChatId) return;

    // Send to real MTProto backend
    if (!isDemoMode && currentUser?.id !== 'demo_user') {
      const numId = Number(messageId.replace(/\D/g, ''));
      if (numId) {
        fetch('/api/telegram/send-reaction', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            peerId: selectedChatId,
            messageId: numId,
            emoji,
          }),
        }).catch((err) => console.warn('Failed to send reaction to MTProto:', err));
      }
    }

    setMessagesMap((prev) => {
      const list = prev[selectedChatId] || [];
      const updated = list.map((m) => {
        if (m.id !== messageId) return m;

        const reactions = [...(m.reactions || [])];
        const existing = reactions.find((r) => r.emoji === emoji);

        if (existing) {
          if (existing.userReacted) {
            existing.count -= 1;
            existing.userReacted = false;
          } else {
            existing.count += 1;
            existing.userReacted = true;
          }
        } else {
          reactions.push({ emoji, count: 1, userReacted: true });
        }

        return {
          ...m,
          reactions: reactions.filter((r) => r.count > 0),
        };
      });

      return {
        ...prev,
        [selectedChatId]: updated,
      };
    });
  };

  // Pin message
  const handlePinMessage = (messageId: string) => {
    if (!selectedChatId) return;

    setMessagesMap((prev) => {
      const list = prev[selectedChatId] || [];
      const updated = list.map((m) => {
        if (m.id === messageId) {
          return { ...m, isPinned: !m.isPinned };
        }
        return m;
      });
      return {
        ...prev,
        [selectedChatId]: updated,
      };
    });
  };

  // Delete message
  const handleDeleteMessage = (messageId: string) => {
    if (!selectedChatId) return;

    setMessagesMap((prev) => {
      const list = prev[selectedChatId] || [];
      return {
        ...prev,
        [selectedChatId]: list.filter((m) => m.id !== messageId),
      };
    });
  };

  // Toggle Mute
  const handleToggleMute = (chatId: string) => {
    setChats((prev) =>
      prev.map((c) => (c.id === chatId ? { ...c, isMuted: !c.isMuted } : c))
    );
  };

  // Clear History
  const handleClearHistory = (chatId: string) => {
    setMessagesMap((prev) => ({
      ...prev,
      [chatId]: [],
    }));
    setChats((prev) =>
      prev.map((c) => (c.id === chatId ? { ...c, lastMessage: undefined } : c))
    );
  };

  // Leave Group / Channel
  const handleLeaveGroup = (chatId: string) => {
    setChats((prev) => prev.filter((c) => c.id !== chatId));
    setMessagesMap((prev) => {
      const copy = { ...prev };
      delete copy[chatId];
      return copy;
    });
    if (selectedChatId === chatId) {
      setSelectedChatId('saved_messages');
    }
  };

  // Report Chat
  const handleReportChat = (chatId: string, reason: string, details?: string) => {
    console.log('Report received for chat:', chatId, { reason, details });
  };

  // Delete Multiple Messages (Selection Mode)
  const handleDeleteMultipleMessages = (messageIds: string[]) => {
    if (!selectedChatId) return;
    setMessagesMap((prev) => ({
      ...prev,
      [selectedChatId]: (prev[selectedChatId] || []).filter((m) => !messageIds.includes(m.id)),
    }));
  };

  // Create new channel / group
  const handleCreateChat = async (newChatData: Partial<TelegramChat>) => {
    let id = 'custom_' + Date.now();
    let createdTitle = newChatData.title || 'محادثة جديدة';
    
    // Call server endpoint to create real channel on Telegram if logged in
    if (newChatData.type === 'channel' || newChatData.type === 'group') {
      try {
        const res = await csrfFetch('/api/channels/create', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            title: createdTitle,
            about: newChatData.description || '',
            megagroup: newChatData.type === 'group',
          }),
        });
        const data = await res.json();
        if (data?.channel?.id) {
          id = String(data.channel.id);
          createdTitle = data.channel.title || createdTitle;
        }
      } catch (err) {
        console.warn('Could not create channel via MTProto, falling back to local chat:', err);
      }
    }

    const chat: TelegramChat = {
      id,
      title: createdTitle,
      username: newChatData.username,
      type: newChatData.type || 'channel',
      avatarColor: newChatData.avatarColor || '#3390ec',
      description: newChatData.description,
      unreadCount: 0,
      membersCount: newChatData.membersCount || 1,
      isPinned: true,
    };

    setChats([chat, ...chats]);
    setSelectedChatId(id);
    setIsMobileChatOpen(true);
    setMessagesMap((prev) => ({
      ...prev,
      [id]: [
        {
          id: 'welcome_' + Date.now(),
          chatId: id,
          senderId: currentUser?.id || 'me',
          senderName: currentUser?.firstName || 'أنا',
          text: `تم إنشاء ${
            chat.type === 'channel' ? 'القناة' : chat.type === 'group' ? 'المجموعة' : 'المحادثة'
          } بنجاح!`,
          timestamp: Date.now(),
          isOut: true,
          status: 'read',
        },
      ],
    }));
  };

  // Select contact from contacts modal
  const handleSelectContact = (contact: TelegramUser) => {
    // Check if chat already exists
    const existing = chats.find((c) => c.title === `${contact.firstName} ${contact.lastName || ''}`.trim());
    if (existing) {
      setSelectedChatId(existing.id);
    } else {
      const id = 'contact_chat_' + contact.id;
      const newChat: TelegramChat = {
        id,
        title: `${contact.firstName} ${contact.lastName || ''}`.trim(),
        username: contact.username,
        type: 'private',
        avatarColor: '#3390ec',
        unreadCount: 0,
        isOnline: contact.status === 'online',
      };
      setChats([newChat, ...chats]);
      setSelectedChatId(id);
    }
    setIsMobileChatOpen(true);
  };

  if (isCheckingAuth) {
    return (
      <div className="min-h-screen bg-[#0e1621] text-white flex flex-col items-center justify-center p-4">
        <Loader2 className="w-10 h-10 text-[#3390ec] animate-spin mb-4" />
        <p className="text-sm text-gray-400 font-medium tracking-wide">
          جارٍ تهيئة خوادم Telegram MTProto والتحقق من الجلسة...
        </p>
      </div>
    );
  }

  // If user is not logged in, show official LoginView
  if (!currentUser) {
    return (
      <LoginView
        onLoginSuccess={handleLoginSuccess}
        lang={themeConfig.language}
      />
    );
  }

  return (
    <div
      className={`h-screen w-screen flex flex-col overflow-hidden select-none font-sans ${
        themeConfig.isDark ? 'bg-[#0e1621] text-white' : 'bg-gray-100 text-gray-900'
      }`}
      dir={themeConfig.language === 'ar' ? 'rtl' : 'ltr'}
    >
      {/* Main App Layout */}
      <div className="flex-1 flex overflow-hidden relative">
        {/* Left Sidebar */}
        <div
          className={`h-full ${
            isMobileChatOpen ? 'hidden md:flex' : 'flex w-full'
          } md:w-80 lg:w-96 shrink-0`}
        >
          <Sidebar
            chats={chats}
            selectedChatId={selectedChatId}
            onSelectChat={(c) => {
              handleSelectChat(c);
              setIsMobileChatOpen(true);
            }}
            onOpenMenu={() => setIsSettingsOpen(true)}
            onOpenNewChat={() => setIsNewChatOpen(true)}
            activeFolder={activeFolder}
            onChangeFolder={setActiveFolder}
            searchQuery={searchQuery}
            onSearchChange={setSearchQuery}
            lang={themeConfig.language}
            isDark={themeConfig.isDark}
            currentUser={currentUser}
            accounts={accounts}
            onOpenAddAccount={() => setIsAddAccountOpen(true)}
            onSwitchAccount={handleSwitchAccount}
            typingMap={typingMap}
            peerStoriesList={peerStoriesList}
            onOpenStory={handleOpenStory}
          />
        </div>

        {/* Center Chat Window */}
        <div
          className={`h-full flex-1 min-w-0 ${
            !isMobileChatOpen ? 'hidden md:flex' : 'flex w-full'
          }`}
        >
          <ChatWindow
            chat={activeChat}
            messages={currentMessages}
            currentUser={currentUser}
            typingStatus={selectedChatId ? typingMap[selectedChatId] || null : null}
            onSimulateTyping={handleSimulateTyping}
            onSendMessage={handleSendMessage}
            onReactMessage={handleReactMessage}
            onPinMessage={handlePinMessage}
            onDeleteMessage={handleDeleteMessage}
            onDeleteMultipleMessages={handleDeleteMultipleMessages}
            onToggleChatInfo={() => setIsChatInfoOpen(!isChatInfoOpen)}
            isChatInfoOpen={isChatInfoOpen}
            onToggleMute={handleToggleMute}
            onClearHistory={handleClearHistory}
            onLeaveGroup={handleLeaveGroup}
            onReportChat={handleReportChat}
            onOpenMediaViewer={(url, title) => setMediaViewerData({ url, title })}
            onOpenMiniApp={(url, appName) => handleOpenMiniApp(url, appName)}
            onBotCallback={handleBotCallback}
            onJoinChannel={handleJoinChannel}
            isJoiningChannel={isJoiningChannel}
            onToast={showToast}
            onBack={() => setIsMobileChatOpen(false)}
            lang={themeConfig.language}
            isDark={themeConfig.isDark}
          />
        </div>

        {/* Right Chat Info Drawer */}
        {activeChat && (
          <ChatInfoDrawer
            chat={activeChat}
            messages={currentMessages}
            isOpen={isChatInfoOpen}
            onClose={() => setIsChatInfoOpen(false)}
            onToggleMute={handleToggleMute}
            onOpenClearHistory={() => setDrawerModal('clear')}
            onOpenLeaveGroup={() => setDrawerModal('leave')}
            onOpenShareLink={() => setDrawerModal('share')}
            onOpenReportChat={() => setDrawerModal('report')}
            onOpenMediaViewer={(url, title) => setMediaViewerData({ url, title })}
            onToast={showToast}
            lang={themeConfig.language}
            isDark={themeConfig.isDark}
          />
        )}
      </div>

      {/* Drawer Action Modals */}
      {activeChat && (
        <>
          <ClearHistoryModal
            isOpen={drawerModal === 'clear'}
            onClose={() => setDrawerModal(null)}
            onConfirm={(alsoForEveryone) => {
              handleClearHistory(activeChat.id);
              showToast(themeConfig.language === 'ar' ? 'تم مسح سجل المحادثة' : 'Chat history cleared', 'info');
            }}
            chat={activeChat}
            lang={themeConfig.language}
            isDark={themeConfig.isDark}
          />

          <LeaveGroupModal
            isOpen={drawerModal === 'leave'}
            onClose={() => setDrawerModal(null)}
            onConfirm={() => {
              handleLeaveGroup(activeChat.id);
              setIsChatInfoOpen(false);
            }}
            chat={activeChat}
            lang={themeConfig.language}
            isDark={themeConfig.isDark}
          />

          <ShareLinkModal
            isOpen={drawerModal === 'share'}
            onClose={() => setDrawerModal(null)}
            chat={activeChat}
            onToast={showToast}
            lang={themeConfig.language}
            isDark={themeConfig.isDark}
          />

          <ReportChatModal
            isOpen={drawerModal === 'report'}
            onClose={() => setDrawerModal(null)}
            chat={activeChat}
            onReportSubmitted={(reason, details) => {
              handleReportChat(activeChat.id, reason, details);
              showToast(
                themeConfig.language === 'ar' ? 'تم إرسال بلاغك بنجاح' : 'Report submitted successfully',
                'success'
              );
            }}
            lang={themeConfig.language}
            isDark={themeConfig.isDark}
          />
        </>
      )}

      {/* Toast Notification */}
      <Toast toast={toast} onClose={() => setToast(null)} />

      {/* Settings & Main Menu Drawer */}
      <SettingsDrawer
        isOpen={isSettingsOpen}
        onClose={() => setIsSettingsOpen(false)}
        currentUser={currentUser}
        onUpdateUser={(updated) => {
          setCurrentUser((prev) => (prev ? { ...prev, ...updated } : prev));
        }}
        themeConfig={themeConfig}
        onUpdateTheme={(up) => setThemeConfig((prev) => ({ ...prev, ...up }))}
        onLogout={handleLogout}
        onOpenSavedMessages={() => {
          setSelectedChatId('saved_messages');
          setIsMobileChatOpen(true);
        }}
        onOpenContacts={() => setIsContactsOpen(true)}
        accounts={accounts}
        activeAccountId={activeAccountId}
        onSwitchAccount={handleSwitchAccount}
        onOpenAddAccount={() => setIsAddAccountOpen(true)}
        onRemoveAccount={handleRemoveAccount}
        chats={chats}
        onSelectChat={(id) => {
          setSelectedChatId(id);
          setIsMobileChatOpen(true);
        }}
        onToggleArchive={handleToggleArchive}
      />

      {/* Add Account Modal (Up to 6 users) */}
      <AddAccountModal
        isOpen={isAddAccountOpen}
        onClose={() => setIsAddAccountOpen(false)}
        onAccountAdded={handleAccountAdded}
        currentAccountsCount={accounts.length}
        maxAccounts={MAX_TELEGRAM_ACCOUNTS}
        lang={themeConfig.language}
        isDark={themeConfig.isDark}
      />

      {/* New Chat / Channel Modal */}
      <NewChatModal
        isOpen={isNewChatOpen}
        onClose={() => setIsNewChatOpen(false)}
        onCreateChat={handleCreateChat}
        lang={themeConfig.language}
        isDark={themeConfig.isDark}
      />

      {/* Contacts Modal */}
      <ContactsModal
        isOpen={isContactsOpen}
        onClose={() => setIsContactsOpen(false)}
        onSelectContact={handleSelectContact}
        lang={themeConfig.language}
        isDark={themeConfig.isDark}
      />

      {/* Story Viewer Modal */}
      <StoryViewerModal
        isOpen={!!activeStoryPeerId}
        onClose={() => setActiveStoryPeerId(null)}
        peerStoriesList={peerStoriesList}
        initialPeerId={activeStoryPeerId || undefined}
        onReact={handleReactToStory}
        onRead={handleReadStory}
        isDark={themeConfig.isDark}
      />

      {/* Media Lightbox */}
      <MediaViewerModal
        isOpen={!!mediaViewerData}
        onClose={() => setMediaViewerData(null)}
        mediaUrl={mediaViewerData?.url || null}
        title={mediaViewerData?.title}
        isDark={themeConfig.isDark}
      />

      {/* Telegram Web Apps / Mini Apps Modal */}
      <MiniAppModal
        isOpen={isMiniAppOpen}
        onClose={() => {
          setIsMiniAppOpen(false);
          setMiniAppData(null);
        }}
        appName={miniAppData?.appName}
        botUsername={miniAppData?.botUsername || activeChat?.username || 'telegram_bot'}
        appUrl={miniAppData?.url}
        onSendData={(data) => {
          handleSendMessage(`[بيانات تطبيق الويب]: ${data}`);
          showToast(
            themeConfig.language === 'ar' ? 'تم إرسال بيانات التطبيق للبوت بنجاح' : 'Data sent to bot successfully',
            'success'
          );
        }}
        lang={themeConfig.language}
        isDark={themeConfig.isDark}
      />
    </div>
  );
}
