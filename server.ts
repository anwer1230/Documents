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

  // Session Token Middleware
  app.use((req, res, next) => {
    let token = (req.headers['x-session-token'] as string) || req.cookies?.tg_session_id;
    if (!token) {
      token = 'user_session_' + Math.random().toString(36).substring(2, 12);
      res.cookie('tg_session_id', token, { maxAge: 365 * 24 * 60 * 60 * 1000, httpOnly: false });
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
        res.cookie('tg_session_id', account.sessionToken, { maxAge: 365 * 24 * 60 * 60 * 1000, httpOnly: false });
        res.json({ success: true, account });
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
    const token = (req as any).sessionToken;
    const { phoneNumber } = req.body;
    if (!phoneNumber) {
      return res.status(400).json({ error: 'رقم الهاتف مطلوب' });
    }

    try {
      const result = await TelegramService.sendCode(token, phoneNumber);
      res.json(result);
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
    const token = (req as any).sessionToken;
    const { phoneCode, phoneCodeHash } = req.body;
    if (!phoneCode) {
      return res.status(400).json({ error: 'كود التحقق مطلوب' });
    }

    try {
      const result = await TelegramService.signIn(token, phoneCode, phoneCodeHash);
      res.json(result);
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
    const token = (req as any).sessionToken;
    const { password } = req.body;
    if (!password) {
      return res.status(400).json({ error: 'كلمة المرور مطلوبة' });
    }

    try {
      const result = await TelegramService.signInWithPassword(token, password);
      res.json(result);
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
    const token = (req as any).sessionToken;
    const { botToken } = req.body;
    if (!botToken) {
      return res.status(400).json({ error: 'رمز البوت (Bot Token) مطلوب' });
    }

    try {
      const result = await TelegramService.botLogin(token, botToken);
      res.json(result);
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

  // Send Message
  app.post('/api/telegram/send-message', async (req, res) => {
    const token = (req as any).sessionToken;
    const { peerId, text, replyTo, media } = req.body;
    if (!peerId || (!text && !media)) {
      return res.status(400).json({ error: 'peerId والنص مطلوبان' });
    }

    try {
      const result = await TelegramService.sendMessage(token, peerId, text || '[وسائط]', replyTo);

      // Broadcast new message via WebSocket to all connected clients
      broadcastToSession(token, {
        type: 'new_message',
        peerId,
        message: {
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
        },
      });

      res.json({ success: true, result });
    } catch (err: any) {
      console.error('Error sending message:', err);
      // Still broadcast optimistic message
      broadcastToSession(token, {
        type: 'new_message',
        peerId,
        message: {
          id: 'msg_' + Date.now(),
          chatId: peerId,
          senderId: 'me',
          senderName: 'أنا',
          text: text || '',
          timestamp: Date.now(),
          isOut: true,
          status: 'sent',
          replyTo,
          media,
        },
      });
      res.json({ success: true, simulated: true });
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
      res.json({ success: true, simulated: true });
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
  });
}

startServer();
