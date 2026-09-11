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

// Configure Web Push VAPID credentials permanently
try {
  webpush.setVapidDetails(VAPID_SUBJECT, VAPID_PUBLIC_KEY, VAPID_PRIVATE_KEY);
  console.log('[WebPush] VAPID details configured successfully');
} catch (err) {
  console.warn('[WebPush] Error setting VAPID details:', err);
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
  // Active Web Push subscriptions mapped by sessionToken
  const pushSubscriptions = new Map<string, any>();

  const broadcastToSession = (token: string, data: any) => {
    const clients = activeWsClients.get(token);
    if (clients && clients.size > 0) {
      const payload = JSON.stringify(data);
      for (const client of clients) {
        if (client.readyState === WebSocket.OPEN) {
          client.send(payload);
        }
      }
    }
  };

  const sendPushToToken = async (
    token: string,
    notification: { title: string; body: string; url?: string }
  ) => {
    const sub = pushSubscriptions.get(token);
    if (!sub) return;
    try {
      await webpush.sendNotification(
        sub,
        JSON.stringify({
          title: notification.title,
          body: notification.body,
          icon: 'https://telegram.org/img/t_logo.png',
          badge: 'https://telegram.org/img/t_logo.png',
          url: notification.url || '/',
        })
      );
    } catch (err: any) {
      console.warn('[WebPush] Failed sending push notification:', err?.message || err);
      if (err?.statusCode === 410 || err?.statusCode === 404) {
        pushSubscriptions.delete(token);
      }
    }
  };

  // Wire MTProto live message handler to WebSocket & Push notifications
  TelegramService.setOnNewMessageHandler((token, message, peerId) => {
    broadcastToSession(token, {
      type: 'new_message',
      peerId,
      message,
    });
    sendPushToToken(token, {
      title: message.senderName || 'Telegram Web',
      body: message.text || 'رسالة جديدة',
      url: '/',
    });
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

    ws.on('message', async (raw) => {
      try {
        const data = JSON.parse(raw.toString());
        if (data.type === 'ping') {
          ws.send(JSON.stringify({ type: 'pong', time: Date.now() }));
        } else if (data.type === 'send_message') {
          const { peerId, text, replyTo } = data;
          if (peerId && text) {
            const localMsg = {
              id: 'ws_' + Date.now(),
              chatId: peerId,
              senderId: 'me',
              senderName: 'أنا',
              text,
              timestamp: Date.now(),
              isOut: true,
              status: 'sent',
            };
            broadcastToSession(token, {
              type: 'new_message',
              peerId,
              message: localMsg,
            });

            // Mark read after short interval
            setTimeout(() => {
              broadcastToSession(token, {
                type: 'message_read',
                peerId,
                messageId: localMsg.id,
              });
            }, 1200);

            // Forward to MTProto
            try {
              await TelegramService.sendMessage(token, peerId, text, replyTo);
            } catch {}
          }
        } else if (data.type === 'mark_read') {
          const { peerId } = data;
          if (peerId) {
            broadcastToSession(token, {
              type: 'message_read',
              peerId,
              time: Date.now(),
            });
          }
        } else if (data.type === 'typing') {
          const { peerId, action } = data;
          if (peerId) {
            broadcastToSession(token, {
              type: 'typing_status',
              peerId,
              action: action || 'typing',
              time: Date.now(),
            });
            try {
              await TelegramService.setTyping(token, peerId, action || 'typing');
            } catch {}
          }
        }
      } catch (err) {
        console.warn('[WebSocket] Error handling message:', err);
      }
    });

    ws.on('close', () => {
      activeWsClients.get(token)?.delete(ws);
      if (activeWsClients.get(token)?.size === 0) {
        activeWsClients.delete(token);
      }
    });

    ws.send(JSON.stringify({ type: 'connected', time: Date.now() }));
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
    const { peerId, text, replyTo } = req.body;
    if (!peerId || !text) {
      return res.status(400).json({ error: 'peerId والنص مطلوبان' });
    }

    try {
      const result = await TelegramService.sendMessage(token, peerId, text, replyTo);
      const newMsg = {
        id: result?.id?.toString() || 'msg_' + Date.now(),
        chatId: peerId,
        senderId: 'me',
        senderName: 'أنا',
        text,
        timestamp: Date.now(),
        isOut: true,
        status: 'sent',
      };

      // Broadcast immediately to connected WebSocket sessions
      broadcastToSession(token, {
        type: 'new_message',
        peerId,
        message: newMsg,
      });

      // Broadcast read status after 1.2s
      setTimeout(() => {
        broadcastToSession(token, {
          type: 'message_read',
          peerId,
          messageId: newMsg.id,
        });
      }, 1200);

      res.json({ success: true, result });
    } catch (err: any) {
      console.error('Error sending message:', err);
      res.status(500).json({ error: err.message || 'فشل إرسال الرسالة' });
    }
  });

  // Mark Read in Real-Time
  app.post('/api/telegram/mark-read', async (req, res) => {
    const token = (req as any).sessionToken;
    const { peerId } = req.body;
    if (!peerId) {
      return res.status(400).json({ error: 'peerId مطلوب' });
    }

    broadcastToSession(token, {
      type: 'message_read',
      peerId,
      time: Date.now(),
    });

    res.json({ success: true });
  });

  // Set Typing Status via MTProto
  app.post('/api/telegram/set-typing', async (req, res) => {
    const token = (req as any).sessionToken;
    const { peerId, action } = req.body;
    if (!peerId) {
      return res.status(400).json({ error: 'peerId مطلوب' });
    }

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
    if (!req.body || !req.body.endpoint) {
      return res.status(400).json({ error: 'بيانات الاشتراك غير صحيحة' });
    }
    pushSubscriptions.set(token, req.body);
    console.log(`[WebPush] Push subscription stored for session ${token.slice(0, 10)}...`);
    res.json({ success: true, message: 'تم تسجيل اشتراك إشعارات الويب الدفعية بنجاح' });
  });

  // Test Web Push with VAPID
  app.post('/api/push/send-test', async (req, res) => {
    const token = (req as any).sessionToken;
    const sub = pushSubscriptions.get(token);
    if (!sub) {
      return res.status(400).json({
        error: 'لم يتم العثور على اشتراك إشعارات دفعية لهذا المتصفح. يرجى تفعيل الإشعارات أولاً.',
      });
    }

    try {
      const payload = JSON.stringify({
        title: 'Telegram Web',
        body: '🔔 تم تفعيل إشعارات Web Push بنجاح عبر مفاتيح VAPID الثابتة!',
        icon: 'https://telegram.org/img/t_logo.png',
        badge: 'https://telegram.org/img/t_logo.png',
        url: '/',
      });

      await webpush.sendNotification(sub, payload);
      res.json({ success: true, message: 'تم إرسال إشعار Web Push التجريبي بنجاح عبر VAPID!' });
    } catch (err: any) {
      console.error('[WebPush] Error sending test notification:', err);
      res.status(500).json({ error: err.message || 'فشل إرسال الإشعار' });
    }
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
