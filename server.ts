import http from 'http';
import express from 'express';
import path from 'path';
import cors from 'cors';
import cookieParser from 'cookie-parser';
import multer from 'multer';
import { WebSocketServer, WebSocket } from 'ws';
import webpush from 'web-push';
import { createServer as createViteServer } from 'vite';
import {
  TelegramService,
  TELEGRAM_API_ID,
  TELEGRAM_API_HASH,
  VAPID_PUBLIC_KEY,
  VAPID_PRIVATE_KEY,
  VAPID_SUBJECT,
} from './server/telegramService.js';

// Configure Web Push with permanent fixed VAPID keys
try {
  webpush.setVapidDetails(
    VAPID_SUBJECT,
    VAPID_PUBLIC_KEY,
    VAPID_PRIVATE_KEY
  );
  console.log('[WebPush] VAPID details configured successfully');
} catch (err) {
  console.warn('[WebPush] VAPID configuration warning:', err);
}

const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 25 * 1024 * 1024 }, // 25 MB
});

async function startServer() {
  const app = express();
  const server = http.createServer(app);
  const wss = new WebSocketServer({ server });
  const PORT = 3000;

  // Active WebSocket clients mapped by sessionToken
  const activeWsClients = new Map<string, Set<WebSocket>>();

  // Helper to broadcast to a specific user's session clients
  const broadcastToSession = (token: string, message: any) => {
    const clients = activeWsClients.get(token);
    if (clients) {
      const data = JSON.stringify(message);
      for (const client of clients) {
        if (client.readyState === WebSocket.OPEN) {
          try {
            client.send(data);
          } catch (e) {
            console.error('[WebSocket] Send error:', e);
          }
        }
      }
    }
  };

  // Helper to broadcast to all connected WebSocket clients
  const broadcastAll = (message: any) => {
    const data = JSON.stringify(message);
    for (const clients of activeWsClients.values()) {
      for (const client of clients) {
        if (client.readyState === WebSocket.OPEN) {
          try {
            client.send(data);
          } catch (e) {
            console.error('[WebSocket] BroadcastAll error:', e);
          }
        }
      }
    }
  };

  // Push Subscriptions storage mapped by sessionToken
  const pushSubscriptions = new Map<string, any>();

  // Send Web Push notification
  const sendWebPush = async (token: string, payload: { title: string; body: string; icon?: string; url?: string }) => {
    const sub = pushSubscriptions.get(token);
    if (!sub) return false;
    try {
      await webpush.sendNotification(
        sub,
        JSON.stringify({
          title: payload.title,
          body: payload.body,
          icon: payload.icon || 'https://telegram.org/img/t_logo.png',
          badge: 'https://telegram.org/img/t_logo.png',
          url: payload.url || '/',
        })
      );
      return true;
    } catch (err: any) {
      console.warn('[WebPush] Error sending push notification:', err?.message || err);
      if (err?.statusCode === 404 || err?.statusCode === 410) {
        pushSubscriptions.delete(token);
      }
      return false;
    }
  };

  // Register real-time MTProto updates callback to push via WebSocket & WebPush
  TelegramService.setOnUpdateCallback((sessionToken: string, update: any) => {
    if (!sessionToken) return;

    if (update.type === 'new_message' && update.message) {
      broadcastToSession(sessionToken, {
        type: 'new_message',
        peerId: update.peerId,
        message: update.message,
      });

      // Also trigger Web Push notification if not outgoing
      if (!update.message.isOut) {
        sendWebPush(sessionToken, {
          title: update.message.senderName || 'Telegram Web',
          body: update.message.text || 'رسالة جديدة في تيليجرام',
          url: '/',
        }).catch(() => {});
      }
    } else if (update.type === 'message_read') {
      broadcastToSession(sessionToken, {
        type: 'message_read',
        peerId: update.peerId,
        messageId: update.messageId,
      });
    } else if (update.type === 'message_edited') {
      broadcastToSession(sessionToken, {
        type: 'message_edited',
        peerId: update.peerId,
        messageId: update.messageId,
        text: update.text,
        editDate: update.editDate,
      });
    } else if (update.type === 'messages_deleted') {
      broadcastToSession(sessionToken, {
        type: 'messages_deleted',
        peerId: update.peerId,
        messageIds: update.messageIds,
      });
    } else if (update.type === 'user_status') {
      broadcastToSession(sessionToken, {
        type: 'user_status',
        userId: update.userId,
        isOnline: update.isOnline,
      });
    } else if (update.type === 'typing_status') {
      broadcastToSession(sessionToken, {
        type: 'typing_status',
        peerId: update.peerId,
        action: update.action || 'typing',
        userName: update.userName,
      });
    }
  });

  wss.on('connection', (ws: WebSocket, req) => {
    let token = 'guest';
    try {
      const url = new URL(req.url || '', `http://${req.headers.host || 'localhost'}`);
      token = url.searchParams.get('token') || 'guest';
    } catch {}

    if (!activeWsClients.has(token)) {
      activeWsClients.set(token, new Set());
    }
    activeWsClients.get(token)!.add(ws);

    // Boot continuous MTProto client listener for this session token if authenticated
    if (token && token !== 'guest' && !token.startsWith('demo_')) {
      TelegramService.getOrCreateClient(token).catch((err) => {
        console.warn('[WS] Could not boot MTProto client for session:', err?.message || err);
      });
    }

    // Initial connection confirmation
    ws.send(JSON.stringify({ type: 'connected', time: Date.now() }));

    // Real-time bidirectional message handling via WebSocket
    ws.on('message', async (raw) => {
      try {
        const data = JSON.parse(raw.toString());
        
        if (data.type === 'ping') {
          ws.send(JSON.stringify({ type: 'pong', time: Date.now() }));
        } else if (data.type === 'send_message') {
          const { peerId, text, replyTo, media } = data;
          if (peerId && (text || media)) {
            const msgObj = {
              id: 'msg_' + Date.now(),
              chatId: peerId,
              senderId: 'me',
              senderName: 'أنا',
              text: text || (media?.type ? `[${media.type}]` : ''),
              timestamp: Date.now(),
              isOut: true,
              status: 'sent',
              replyTo,
              media,
            };

            // Broadcast to all client windows for this session
            broadcastToSession(token, {
              type: 'new_message',
              peerId,
              message: msgObj,
            });

            // Send via MTProto if logged in
            TelegramService.sendMessage(token, peerId, text || '[وسائط]', replyTo ? Number(replyTo.id) : undefined)
              .catch((err) => console.warn('[WS] MTProto send warning:', err.message));
          }
        } else if (data.type === 'mark_read') {
          const { peerId, messageId } = data;
          broadcastToSession(token, {
            type: 'message_read',
            peerId,
            messageId,
          });
          if (peerId) {
            TelegramService.markAsRead(token, peerId).catch(() => {});
          }
        } else if (data.type === 'edit_message') {
          const { peerId, messageId, text } = data;
          broadcastToSession(token, {
            type: 'message_edited',
            peerId,
            messageId: String(messageId),
            text,
            editDate: Date.now(),
          });
          if (peerId && messageId && text) {
            TelegramService.editMessage(token, peerId, Number(messageId), text).catch(() => {});
          }
        } else if (data.type === 'delete_message') {
          const { peerId, messageId, messageIds } = data;
          const targetIds = messageIds || (messageId ? [messageId] : []);
          broadcastToSession(token, {
            type: 'messages_deleted',
            peerId,
            messageIds: targetIds.map(String),
          });
          if (peerId && targetIds.length > 0) {
            const numericIds = targetIds.map((id: any) => Number(id)).filter((id: number) => !isNaN(id));
            if (numericIds.length > 0) {
              TelegramService.deleteMessages(token, peerId, numericIds).catch(() => {});
            }
          }
        } else if (data.type === 'typing_status') {
          const { peerId, action, userName } = data;
          broadcastToSession(token, {
            type: 'typing_status',
            peerId,
            action,
            userName,
          });
        }
      } catch (err) {
        console.warn('[WebSocket] Invalid JSON message received:', err);
      }
    });

    ws.on('close', () => {
      activeWsClients.get(token)?.delete(ws);
      if (activeWsClients.get(token)?.size === 0) {
        activeWsClients.delete(token);
      }
    });
  });

  app.use(cors({ origin: true, credentials: true }));
  app.use(express.json());
  app.use(cookieParser());

  // Session Token Middleware (Header -> Body -> Query -> Cookie)
  app.use((req, res, next) => {
    let token =
      (req.headers['x-session-token'] as string) ||
      (req.body && typeof req.body === 'object' && (req.body.sessionToken as string)) ||
      (req.query && typeof req.query.sessionToken === 'string' && req.query.sessionToken) ||
      req.cookies?.tg_session_id;

    if (!token) {
      token = 'user_session_' + Math.random().toString(36).substring(2, 12);
      res.cookie('tg_session_id', token, {
        maxAge: 365 * 24 * 60 * 60 * 1000,
        httpOnly: false,
        sameSite: 'none',
        secure: true,
        path: '/',
      });
    }
    (req as any).sessionToken = token;
    next();
  });

  // Telegram Health and Connection Status Endpoint
  app.get('/api/telegram/status', async (req, res) => {
    const token = (req as any).sessionToken;
    try {
      const auth = await TelegramService.getMe(token);
      res.json({
        apiId: TELEGRAM_API_ID,
        apiHashConfigured: !!TELEGRAM_API_HASH,
        vapidPublicKey: VAPID_PUBLIC_KEY,
        vapidSubject: VAPID_SUBJECT,
        sessionToken: token,
        isLoggedIn: auth.isLoggedIn,
        user: auth.user,
      });
    } catch (err: any) {
      res.json({
        apiId: TELEGRAM_API_ID,
        apiHashConfigured: true,
        vapidPublicKey: VAPID_PUBLIC_KEY,
        vapidSubject: VAPID_SUBJECT,
        sessionToken: token,
        isLoggedIn: false,
        error: err.message,
      });
    }
  });

  // System Configuration Endpoint (Fixed permanent variables)
  app.get('/api/system/config', (_req, res) => {
    res.json({
      TELEGRAM_API_ID,
      TELEGRAM_API_HASH_CONFIGURED: !!TELEGRAM_API_HASH,
      VAPID_PUBLIC_KEY,
      VAPID_SUBJECT,
      isPermanent: true,
    });
  });

  // Multi-Account Management Endpoints (Up to 6 accounts)
  app.get('/api/telegram/accounts', async (req, res) => {
    try {
      const data = TelegramService.getAccounts();
      res.json(data);
    } catch (err: any) {
      res.status(500).json({ error: err.message });
    }
  });

  app.post('/api/telegram/accounts/new', async (req, res) => {
    try {
      const slot = TelegramService.createAccountSlot();
      res.json(slot);
    } catch (err: any) {
      res.status(400).json({ error: err.message });
    }
  });

  app.post('/api/telegram/accounts/switch', async (req, res) => {
    const { accountId, sessionToken } = req.body;
    try {
      const target = accountId || sessionToken;
      const account = TelegramService.setActiveAccount(target);
      if (account) {
        res.cookie('tg_session_id', account.sessionToken, {
          maxAge: 365 * 24 * 60 * 60 * 1000,
          httpOnly: false,
          sameSite: 'none',
          secure: true,
          path: '/',
        });
        res.json({ success: true, account, sessionToken: account.sessionToken });
      } else {
        res.status(404).json({ error: 'الحساب غير موجود' });
      }
    } catch (err: any) {
      res.status(500).json({ error: err.message });
    }
  });

  app.delete('/api/telegram/accounts/:id', async (req, res) => {
    const { id } = req.params;
    try {
      await TelegramService.removeAccount(id);
      const remaining = TelegramService.getAccounts();
      res.json({ success: true, ...remaining });
    } catch (err: any) {
      res.status(500).json({ error: err.message });
    }
  });

  // Real MTProto Send Verification Code
  app.post('/api/telegram/send-code', async (req, res) => {
    const token = (req.body && req.body.sessionToken) || (req as any).sessionToken;
    const { phoneNumber } = req.body;
    if (!phoneNumber) {
      return res.status(400).json({ error: 'رقم الهاتف مطلوب' });
    }

    try {
      const result = await TelegramService.sendCode(token, phoneNumber);
      res.json({ ...result, sessionToken: token });
    } catch (err: any) {
      console.error('Error in send-code:', err);
      res.status(500).json({
        error: err.message || 'فشل إرسال كود التحقق عبر تيليجرام',
        details: err.errorMessage || err.toString(),
      });
    }
  });

  // Real MTProto Complete Sign In
  app.post('/api/telegram/sign-in', async (req, res) => {
    const token = (req.body && req.body.sessionToken) || (req as any).sessionToken;
    const { phoneCode, phoneCodeHash, phoneNumber } = req.body;
    if (!phoneCode) {
      return res.status(400).json({ error: 'كود التحقق مطلوب' });
    }

    try {
      const result = await TelegramService.signIn(token, phoneCode, phoneCodeHash, phoneNumber);
      res.json({ ...result, sessionToken: result.sessionToken || token });
    } catch (err: any) {
      console.error('Error in sign-in:', err);
      res.status(500).json({
        error: err.message || 'فشل تسجيل الدخول',
        details: err.errorMessage || err.toString(),
      });
    }
  });

  // Real MTProto 2FA Password
  app.post('/api/telegram/sign-in-password', async (req, res) => {
    const token = (req.body && req.body.sessionToken) || (req as any).sessionToken;
    const { password } = req.body;
    if (!password) {
      return res.status(400).json({ error: 'كلمة المرور مطلوبة' });
    }

    try {
      const result = await TelegramService.signInWithPassword(token, password);
      res.json({ ...result, sessionToken: result.sessionToken || token });
    } catch (err: any) {
      console.error('Error in 2FA sign-in:', err);
      res.status(500).json({
        error: err.message || 'كلمة المرور غير صحيحة',
        details: err.errorMessage || err.toString(),
      });
    }
  });

  // Bot Token Login
  app.post('/api/telegram/bot-login', async (req, res) => {
    const token = (req.body && req.body.sessionToken) || (req as any).sessionToken;
    const { botToken } = req.body;
    if (!botToken) {
      return res.status(400).json({ error: 'رمز البوت (Bot Token) مطلوب' });
    }

    try {
      const result = await TelegramService.botLogin(token, botToken);
      res.json({ ...result, sessionToken: token });
    } catch (err: any) {
      console.error('Error in bot-login:', err);
      res.status(500).json({
        error: err.message || 'فشل تسجيل الدخول برمز البوت',
      });
    }
  });

  // Fetch Dialogs / Chats
  app.get('/api/telegram/dialogs', async (req, res) => {
    const token = (req as any).sessionToken;
    try {
      const dialogs = await TelegramService.getDialogs(token, 50);
      res.json({ dialogs });
    } catch (err: any) {
      console.error('Error fetching dialogs:', err);
      res.status(500).json({ error: err.message || 'فشل جلب المحادثات' });
    }
  });

  // Fetch Messages for Chat
  app.get('/api/telegram/messages', async (req, res) => {
    const token = (req as any).sessionToken;
    const { peerId, limit } = req.query;
    if (!peerId) {
      return res.status(400).json({ error: 'peerId مطلوب' });
    }

    try {
      const messages = await TelegramService.getMessages(token, peerId as string, limit ? Number(limit) : 50);
      res.json({ messages });
    } catch (err: any) {
      console.error('Error fetching messages:', err);
      res.status(500).json({ error: err.message || 'فشل جلب الرسائل' });
    }
  });

  // Send Message (supports text and media files)
  app.post('/api/telegram/send-message', async (req, res) => {
    const token = (req as any).sessionToken;
    const { peerId, text, replyTo, media } = req.body;

    if (!peerId || (!text && !media)) {
      return res.status(400).json({ error: 'peerId والنص أو الوسائط مطلوبان' });
    }

    try {
      const result = await TelegramService.sendMessage(token, peerId, text || '', replyTo, media);
      
      // Broadcast new message via WebSocket to all connected clients
      broadcastToSession(token, {
        type: 'new_message',
        peerId,
        message: {
          id: result?.id?.toString() || 'msg_' + Date.now(),
          chatId: peerId,
          senderId: 'me',
          senderName: 'أنا',
          text: text || (media?.type ? `[${media.type}]` : ''),
          timestamp: Date.now(),
          isOut: true,
          status: 'sent',
          replyTo,
          media,
        },
      });

      res.json({ success: true, result });
    } catch (err: any) {
      console.error('Error sending message:', err);
      res.status(500).json({ error: err.message || 'فشل إرسال الرسالة عبر تيليجرام' });
    }
  });

  // Send / Toggle Reaction via MTProto
  app.post('/api/telegram/send-reaction', async (req, res) => {
    const token = (req as any).sessionToken;
    const { peerId, messageId, emoji } = req.body;
    if (!peerId || !messageId) {
      return res.status(400).json({ error: 'peerId and messageId are required' });
    }
    try {
      const result = await TelegramService.sendReaction(token, peerId, messageId, emoji);
      broadcastToSession(token, {
        type: 'message_reaction',
        peerId,
        messageId: String(messageId),
        emoji,
        userReacted: !!emoji,
      });
      res.json({ success: true, result });
    } catch (err: any) {
      console.error('Error sending reaction:', err);
      res.status(500).json({ error: err.message || 'فشل إرسال التفاعل' });
    }
  });

  // Get Peer Stories
  app.get('/api/telegram/stories', async (req, res) => {
    const token = (req as any).sessionToken;
    try {
      const peerStories = await TelegramService.getAllStories(token);
      res.json({ success: true, peerStories });
    } catch (err: any) {
      console.error('Error fetching stories:', err);
      res.status(500).json({ error: err.message || 'فشل جلب القصص' });
    }
  });

  // Read Stories
  app.post('/api/telegram/stories/read', async (req, res) => {
    const token = (req as any).sessionToken;
    const { peerId, maxId } = req.body;
    if (!peerId) {
      return res.status(400).json({ error: 'peerId is required' });
    }
    try {
      const result = await TelegramService.readStories(token, peerId, maxId);
      res.json(result);
    } catch (err: any) {
      console.error('Error reading stories:', err);
      res.status(500).json({ error: err.message });
    }
  });

  // Send Story Reaction
  app.post('/api/telegram/stories/react', async (req, res) => {
    const token = (req as any).sessionToken;
    const { peerId, storyId, emoji } = req.body;
    if (!peerId || !storyId || !emoji) {
      return res.status(400).json({ error: 'peerId, storyId, and emoji are required' });
    }
    try {
      const result = await TelegramService.sendStoryReaction(token, peerId, storyId, emoji);
      res.json(result);
    } catch (err: any) {
      console.error('Error reacting to story:', err);
      res.status(500).json({ error: err.message });
    }
  });

  // Mark Chat / Message as Read
  app.post('/api/telegram/mark-read', async (req, res) => {
    const token = (req as any).sessionToken;
    const { peerId, messageId } = req.body;
    if (!peerId) {
      return res.status(400).json({ error: 'peerId مطلوب' });
    }
    try {
      await TelegramService.markAsRead(token, peerId);
    } catch {}

    // Broadcast read receipt via WebSocket immediately
    broadcastToSession(token, {
      type: 'message_read',
      peerId,
      messageId,
    });

    res.json({ success: true });
  });

  // Set Typing Status via MTProto
  app.post('/api/telegram/set-typing', async (req, res) => {
    const token = (req as any).sessionToken;
    const { peerId, action, userName } = req.body;
    if (!peerId) {
      return res.status(400).json({ error: 'peerId مطلوب' });
    }

    // Broadcast typing status to WebSocket clients
    broadcastToSession(token, {
      type: 'typing_status',
      peerId,
      action: action || 'typing',
      userName,
    });

    try {
      const result = await TelegramService.setTyping(token, peerId, action || 'typing');
      res.json({ success: true, result });
    } catch (err: any) {
      console.error('Error setting typing:', err);
      res.status(500).json({ error: err.message });
    }
  });

  // Send Media / File Upload
  app.post('/api/telegram/upload', upload.single('file'), async (req, res) => {
    const file = req.file;
    if (!file) {
      return res.status(400).json({ error: 'الملف مطلوب' });
    }
    // Return base64 data URL for preview and sending
    const base64 = `data:${file.mimetype};base64,${file.buffer.toString('base64')}`;
    res.json({
      success: true,
      fileName: file.originalname,
      fileSize: (file.size / 1024).toFixed(1) + ' KB',
      mimeType: file.mimetype,
      url: base64,
    });
  });

  // Global Search for Channels, Groups, and Bots (Telegram Web K mechanism)
  app.get('/api/telegram/search-global', async (req, res) => {
    const token = (req as any).sessionToken;
    const query = (req.query.q as string) || '';
    try {
      const results = await TelegramService.searchGlobal(token, query);
      res.json({ success: true, results });
    } catch (err: any) {
      console.error('Error in search-global:', err);
      res.status(500).json({ error: err.message });
    }
  });

  // Join Channel or Supergroup (Telegram Web K mechanism)
  app.post('/api/telegram/join-channel', async (req, res) => {
    const token = (req as any).sessionToken;
    const { channel } = req.body;
    if (!channel) {
      return res.status(400).json({ error: 'المعرف الخاص بالقناة مطلوب' });
    }
    try {
      const result = await TelegramService.joinChannel(token, channel);
      // Broadcast update to session via WebSocket
      broadcastToSession(token, {
        type: 'channel_joined',
        channel: result.channel,
      });
      res.json(result);
    } catch (err: any) {
      console.error('Error joining channel:', err);
      res.status(500).json({ error: err.message });
    }
  });

  // Leave Channel or Supergroup
  app.post('/api/telegram/leave-channel', async (req, res) => {
    const token = (req as any).sessionToken;
    const { channel } = req.body;
    if (!channel) {
      return res.status(400).json({ error: 'المعرف الخاص بالقناة مطلوب' });
    }
    try {
      const result = await TelegramService.leaveChannel(token, channel);
      broadcastToSession(token, {
        type: 'channel_left',
        channelId: channel,
      });
      res.json(result);
    } catch (err: any) {
      console.error('Error leaving channel:', err);
      res.status(500).json({ error: err.message });
    }
  });

  // Bot Callback Query Answer (Telegram Web K mechanism)
  app.post('/api/telegram/bot-callback', async (req, res) => {
    const token = (req as any).sessionToken;
    const { peerId, messageId, data, game } = req.body;
    if (!peerId) {
      return res.status(400).json({ error: 'peerId مطلوب' });
    }
    try {
      const result = await TelegramService.getBotCallbackAnswer(
        token,
        peerId,
        messageId ? Number(messageId) : 0,
        data,
        game
      );
      res.json(result);
    } catch (err: any) {
      console.error('Error in bot-callback:', err);
      res.status(500).json({ error: err.message });
    }
  });

  // Bot Info & Commands (Telegram Web K mechanism)
  app.get('/api/telegram/bot-info', async (req, res) => {
    const token = (req as any).sessionToken;
    const botPeer = (req.query.bot as string) || '';
    if (!botPeer) {
      return res.status(400).json({ error: 'معرف البوت مطلوب' });
    }
    try {
      const result = await TelegramService.getBotInfo(token, botPeer);
      res.json({ success: true, botInfo: result });
    } catch (err: any) {
      console.error('Error getting bot info:', err);
      res.status(500).json({ error: err.message });
    }
  });

  // Inline Bot Query Execution (Telegram Web K mechanism: @bot query)
  app.get('/api/telegram/inline-query', async (req, res) => {
    const token = (req as any).sessionToken;
    const bot = (req.query.bot as string) || '';
    const query = (req.query.q as string) || '';
    if (!bot) {
      return res.status(400).json({ error: 'اسم البوت مطلوب' });
    }
    try {
      const results = await TelegramService.getInlineBotResults(token, bot, query);
      res.json({ success: true, results });
    } catch (err: any) {
      console.error('Error in inline-query:', err);
      res.status(500).json({ error: err.message });
    }
  });

  // Logout
  app.post('/api/telegram/logout', async (req, res) => {
    const token = (req as any).sessionToken;
    try {
      await TelegramService.logout(token);
      res.clearCookie('tg_session_id');
      res.json({ success: true });
    } catch (err: any) {
      res.status(500).json({ error: err.message });
    }
  });

  // Web Push Subscription Endpoints
  app.post('/api/push/subscribe', (req, res) => {
    const token = (req as any).sessionToken;
    if (req.body && req.body.endpoint) {
      pushSubscriptions.set(token, req.body);
      console.log(`[WebPush] Subscription registered for token: ${token}`);
      res.json({ success: true, message: 'Push subscription registered successfully' });
    } else {
      res.status(400).json({ error: 'Invalid subscription object' });
    }
  });

  app.post('/api/push/unsubscribe', (req, res) => {
    const token = (req as any).sessionToken;
    pushSubscriptions.delete(token);
    res.json({ success: true, message: 'Push subscription removed' });
  });

  // Trigger Instant Test Push Notification to verify VAPID & Service Worker
  app.post('/api/push/test', async (req, res) => {
    const token = (req as any).sessionToken;
    const { title, body } = req.body;
    const hasSub = pushSubscriptions.has(token);

    if (!hasSub) {
      return res.status(400).json({
        error: 'لا يوجد اشتراك إشعارات مسجل لهذا المتصفح. يرجى تفعيل الإشعارات أولاً.',
      });
    }

    const sent = await sendWebPush(token, {
      title: title || 'Telegram Web Push 🔔',
      body: body || 'إشعار تجريبي ناجح من تيليجرام ويب عبر مفاتيح VAPID وخدمة Service Worker!',
      url: '/',
    });

    if (sent) {
      res.json({ success: true, message: 'تم إرسال إشعار الدفع بنجاح' });
    } else {
      res.status(500).json({ error: 'فشل إرسال إشعار الدفع عبر خادم VAPID' });
    }
  });

  app.get('/api/push/status', (req, res) => {
    const token = (req as any).sessionToken;
    res.json({
      hasSubscription: pushSubscriptions.has(token),
      vapidPublicKey: VAPID_PUBLIC_KEY,
      vapidSubject: VAPID_SUBJECT,
    });
  });

  // Vite Middleware Setup
  if (process.env.NODE_ENV !== 'production') {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa',
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.join(process.cwd(), 'dist');
    app.use(express.static(distPath));
    app.get('*', (req, res) => {
      res.sendFile(path.join(distPath, 'index.html'));
    });
  }

  server.listen(PORT, '0.0.0.0', () => {
    console.log(`Telegram Web Server running on port ${PORT}`);
    
    // Auto-bootstrap continuous MTProto updates engine for all saved accounts
    TelegramService.initAllSavedSessions().catch((err) => {
      console.warn('[Server] Auto-session bootstrap warning:', err?.message || err);
    });
  });
}

startServer();
