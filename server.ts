import { GoogleGenAI } from "@google/genai";
import http from 'http';
import express from 'express';
import path from 'path';
import cors from 'cors';
import cookieParser from 'cookie-parser';
import multer from 'multer';
import { WebSocketServer, WebSocket } from 'ws';
import webpush from 'web-push';
import { createServer as createViteServer } from 'vite';
import helmet from 'helmet';
import rateLimit from 'express-rate-limit';
import { requireAuth, requireCsrf, checkOrigin } from './server/security/middleware.js';
import { issueSession, destroySession, getSessionByToken } from './server/security/sessions.js';
import { SECURITY, COOKIE_STRICT } from './server/security/config.js';
import {
  attachWebSocketServer,
  broadcastToSession,
  broadcastAll,
  setMessageHandler,
} from './server/ws-security.js';
import { isNonEmptyString, sanitizeText } from './server/security/validate.js';
import { computeSrp } from './server/security/srp.js';
import {
  TelegramService,
  TELEGRAM_API_ID,
  TELEGRAM_API_HASH,
  VAPID_PUBLIC_KEY,
  VAPID_PRIVATE_KEY,
  VAPID_SUBJECT,
} from './server/telegramService.js';
import { telegramRPCRegistry } from './server/TelegramRPCRegistry.js';
import { sqliteDatabase } from './server/sqliteService.js';
import { redisCache } from './server/redisCacheService.js';


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
  const wss = attachWebSocketServer(server);
  const PORT = 3000;

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

  // Secure WebSocket Message Handler
  setMessageHandler(async (ws: WebSocket, token: string, data: any) => {
    // Boot continuous MTProto client listener for this session token if authenticated
    if (token && token !== 'guest' && !token.startsWith('demo_')) {
      TelegramService.getOrCreateClient(token).catch((err) => {
        console.warn('[WS] Could not boot MTProto client for session:', err?.message || err);
      });
    }

    if (data.action === 'sync_request' || data.type === 'sync_request') {
      const rawTs = Number(data.lastTimestamp) || 0;
      const dateSec = rawTs > 10000000000 ? Math.floor(rawTs / 1000) : rawTs;
      console.log(`[WS] Client sync_request with lastTimestamp: ${rawTs} (date: ${dateSec}) for session: ${token}`);

      let catchupMessages: any[] = [];
      if (token && token !== 'guest' && token !== 'guest_user' && !token.startsWith('demo_')) {
        try {
          const diffRes: any = await TelegramService.getDifference(
            token,
            0,
            dateSec > 0 ? dateSec : Math.floor(Date.now() / 1000) - 3600
          );
          if (diffRes && Array.isArray(diffRes.newMessages)) {
            catchupMessages = diffRes.newMessages;
          }
        } catch (err: any) {
          console.warn('[WS sync_request] Error executing GetDifference:', err?.message || err);
        }
      }

      const batchPayload = {
        type: 'sync_batch',
        action: 'sync_batch',
        lastTimestamp: Math.floor(Date.now() / 1000),
        messages: catchupMessages,
        count: catchupMessages.length,
      };
      ws.send(JSON.stringify(batchPayload));
      console.log(`[WS] Dispatched sync_batch with ${catchupMessages.length} messages to client`);
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

        broadcastToSession(token, {
          type: 'new_message',
          peerId,
          message: msgObj,
        });

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
  });

  // 1. Helmet (Security Headers)
  app.use(helmet({
    contentSecurityPolicy: {
      directives: {
        defaultSrc: ["'self'"],
        scriptSrc: ["'self'", "'unsafe-inline'", "'unsafe-eval'"],
        styleSrc: ["'self'", "'unsafe-inline'"],
        imgSrc: ["'self'", 'data:', 'blob:', 'https:'],
        connectSrc: ["'self'", 'ws:', 'wss:', 'https:'],
        workerSrc: ["'self'", 'blob:'],
      },
    },
    crossOriginEmbedderPolicy: false,
  }));

  // 2. HTTP Rate Limiter
  const generalLimiter = rateLimit({
    windowMs: 60 * 1000,
    max: SECURITY.http.rateLimitPerMin,
    standardHeaders: true,
    legacyHeaders: false,
  });
  const writeLimiter = rateLimit({
    windowMs: 60 * 1000,
    max: SECURITY.http.writeRateLimitPerMin,
    standardHeaders: true,
    legacyHeaders: false,
  });
  app.use('/api/', generalLimiter);
  app.use(['/api/send', '/api/messages/edit', '/api/messages/delete', '/api/telegram/send-message'], writeLimiter);

  // 3. Origin check on write operations
  app.use('/api/', checkOrigin);

  app.use(cors({ origin: true, credentials: true }));
  app.use(express.json({ limit: SECURITY.http.bodyLimitBytes }));
  app.use(cookieParser());

  // Session Token & Cookie Security Middleware
  app.use((req, res, next) => {
    let token =
      req.cookies?.[SECURITY.cookies.session.name] ||
      (req.headers['authorization']?.toString().replace(/^Bearer\s+/i, '')) ||
      (req.headers['x-session-token'] as string) ||
      (req.body && typeof req.body === 'object' && (req.body.sessionToken as string)) ||
      (req.query && typeof req.query.sessionToken === 'string' && req.query.sessionToken) ||
      (req.query && typeof req.query.token === 'string' && req.query.token);

    let session = token ? getSessionByToken(token) : null;
    if (!session) {
      const initialToken = token || ('user_session_' + Math.random().toString(36).substring(2, 12));
      session = issueSession(res, initialToken, {
        userAgent: req.headers['user-agent'] as string,
        ip: req.ip,
      });
      token = session.token;
    }

    req.session = session;
    (req as any).sessionToken = token;
    next();
  });

  // CSRF Token Endpoint
  app.get('/api/csrf-token', (req, res) => {
    res.json({ csrfToken: req.session?.csrfToken || '' });
  });

  // Telegram Health and Connection Status Endpoint
  app.get('/api/telegram/status', async (req, res) => {
    let token = (req as any).sessionToken;
    try {
      if (!token || !(await TelegramService.isAuthorized(token))) {
        const active = TelegramService.getActiveSessionToken(token);
        if (active) token = active;
      }
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
      const effectiveToken = result.sessionToken || token;
      issueSession(res, effectiveToken, { userAgent: req.headers['user-agent'] as string, ip: req.ip });
      res.json({ ...result, sessionToken: effectiveToken });
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
      const effectiveToken = result.sessionToken || token;
      issueSession(res, effectiveToken, { userAgent: req.headers['user-agent'] as string, ip: req.ip });
      res.json({ ...result, sessionToken: effectiveToken });
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
      const effectiveToken = token;
      issueSession(res, effectiveToken, { userAgent: req.headers['user-agent'] as string, ip: req.ip });
      res.json({ ...result, sessionToken: token });
    } catch (err: any) {
      console.error('Error in bot-login:', err);
      res.status(500).json({
        error: err.message || 'فشل تسجيل الدخول برمز البوت',
      });
    }
  });

  // Dedicated Auth Login / Session Initialization Endpoint
  app.post(['/api/auth/login', '/api/session/init'], (req, res) => {
    const { sessionToken } = req.body;
    const token = sessionToken || ('user_session_' + Math.random().toString(36).substring(2, 12));
    const session = issueSession(res, token, {
      userAgent: req.headers['user-agent'] as string,
      ip: req.ip,
    });
    res.json({
      success: true,
      token: session.token,
      csrfToken: session.csrfToken,
      wsToken: session.wsToken,
    });
  });

  // Fetch Dialogs / Chats
  app.get('/api/telegram/dialogs', async (req, res) => {
    let token = (req.query.token as string) || (req.query.sessionToken as string) || (req as any).sessionToken;
    if (!token || !(await TelegramService.isAuthorized(token))) {
      const active = TelegramService.getActiveSessionToken(token);
      if (active) token = active;
    }
    const limit = req.query.limit ? parseInt(req.query.limit as string, 10) : 200;
    try {
      const dialogs = await TelegramService.getDialogs(token, limit);
      res.json({ dialogs });
    } catch (err: any) {
      console.error('Error fetching dialogs:', err);
      res.status(500).json({ error: err.message || 'فشل جلب المحادثات' });
    }
  });

  // MTProto Light & Delta Dialogs Sync
  app.all(['/api/telegram/sync-light', '/api/sync-light'], async (req, res) => {
    let token = (req.body?.token || req.query?.token || req.body?.sessionToken || (req as any).sessionToken) as string;
    if (!token || !(await TelegramService.isAuthorized(token))) {
      const active = TelegramService.getActiveSessionToken(token);
      if (active) token = active;
    }
    if (!token || !(await TelegramService.isAuthorized(token))) {
      return res.status(401).json({ success: false, error: 'NO_SESSION', message: 'لا توجد جلسة تيليجرام نشطة.' });
    }

    const lastMessageIds = req.body?.lastMessageIds || {};
    try {
      const [dialogs, user] = await Promise.all([
        TelegramService.getDialogs(token, 200),
        TelegramService.getMe(token).catch(() => null),
      ]);

      // Fetch delta messages for top 15 active chats
      const messagesRecord: Record<string, any[]> = {};
      const topChats = dialogs.slice(0, 15);
      await Promise.allSettled(
        topChats.map(async (chat: any) => {
          try {
            const minId = lastMessageIds[chat.id] ? Number(String(lastMessageIds[chat.id]).replace(/\D/g, '')) || 0 : 0;
            const msgs = await TelegramService.getMessages(token, chat.id, 30);
            if (minId > 0 && msgs.length > 0) {
              const deltaMsgs = msgs.filter((m: any) => Number(m.id) > minId);
              messagesRecord[chat.id] = deltaMsgs;
            } else {
              messagesRecord[chat.id] = msgs;
            }
          } catch (_) {
            messagesRecord[chat.id] = [];
          }
        })
      );

      return res.json({
        success: true,
        isLightSync: true,
        isRealTelegramMTProto: true,
        dialogs,
        messages: messagesRecord,
        user,
        syncTimestamp: new Date().toISOString(),
      });
    } catch (err: any) {
      console.warn('[sync-light] error:', err?.message || err);
      return res.status(500).json({ success: false, error: err?.message || 'فشل مزامنة المحادثات' });
    }
  });

  // MTProto Delta Messages Endpoint
  app.post('/api/telegram/messages/delta', async (req, res) => {
    let token = (req.body?.token || req.body?.sessionToken || (req as any).sessionToken) as string;
    if (!token || !(await TelegramService.isAuthorized(token))) {
      const active = TelegramService.getActiveSessionToken(token);
      if (active) token = active;
    }
    if (!token || !(await TelegramService.isAuthorized(token))) {
      return res.status(401).json({ success: false, error: 'NO_SESSION' });
    }

    const { chatSyncStates } = req.body || {};
    const deltas: Record<string, { newMessages: any[]; hasChanges: boolean; lastMsgId?: number }> = {};
    const entries = Object.entries(chatSyncStates || {});

    await Promise.allSettled(
      entries.map(async ([chatId, state]: [string, any]) => {
        try {
          const lastKnownId = Number(state?.lastMessageId || state?.lastMsgId || 0) || 0;
          const msgs = await TelegramService.getMessages(token, chatId, 30);
          const newMsgs = lastKnownId > 0 ? msgs.filter((m: any) => Number(m.id) > lastKnownId) : msgs;
          const maxId = newMsgs.length > 0 ? Math.max(...newMsgs.map((m: any) => Number(m.id) || 0)) : lastKnownId;
          deltas[chatId] = {
            newMessages: newMsgs,
            hasChanges: newMsgs.length > 0,
            lastMsgId: maxId,
          };
        } catch (_) {
          deltas[chatId] = { newMessages: [], hasChanges: false };
        }
      })
    );

    return res.json({ success: true, deltas });
  });

  // ==========================================
  // PHASE 1 & PHASE 2: MTPROTO SYNC & MEDIA ENDPOINTS
  // ==========================================

  // Binary Media In-Memory LRU Cache
  const mediaCache = new Map<string, { buffer: Buffer; mimeType: string; fileName?: string; timestamp: number }>();
  const MAX_MEDIA_CACHE_ITEMS = 300;

  const cacheMediaItem = (key: string, item: { buffer: Buffer; mimeType: string; fileName?: string }) => {
    if (mediaCache.size >= MAX_MEDIA_CACHE_ITEMS) {
      const oldestKey = mediaCache.keys().next().value;
      if (oldestKey) mediaCache.delete(oldestKey);
    }
    mediaCache.set(key, { ...item, timestamp: Date.now() });
  };

  // Media Streaming Endpoint (with HTTP 206 Range Support & LRU Cache)
  app.get(['/api/telegram/media/:chatId/:messageId', '/api/media/:chatId/:messageId'], async (req, res) => {
    const token = (req as any).sessionToken;
    const { chatId, messageId } = req.params;
    const cacheKey = `${chatId}_${messageId}`;

    try {
      let media = mediaCache.get(cacheKey);
      if (!media) {
        const downloaded = await TelegramService.downloadMedia(token, chatId, Number(messageId));
        if (!downloaded) {
          return res.status(404).json({ error: 'الوسائط غير موجودة أو غير مدعومة' });
        }
        cacheMediaItem(cacheKey, downloaded);
        media = mediaCache.get(cacheKey);
      }

      if (!media) {
        return res.status(404).json({ error: 'فشل تحميل الوسائط' });
      }

      const { buffer, mimeType, fileName } = media;
      const totalSize = buffer.length;

      res.setHeader('Accept-Ranges', 'bytes');
      res.setHeader('Content-Type', mimeType);
      res.setHeader('Cache-Control', 'public, max-age=604800, immutable');
      if (fileName) {
        res.setHeader('Content-Disposition', `inline; filename="${encodeURIComponent(fileName)}"`);
      }

      const range = req.headers.range;
      if (range) {
        const parts = range.replace(/bytes=/, '').split('-');
        const start = parseInt(parts[0], 10);
        const end = parts[1] ? parseInt(parts[1], 10) : totalSize - 1;

        if (start >= totalSize || end >= totalSize) {
          res.status(416).setHeader('Content-Range', `bytes */${totalSize}`);
          return res.end();
        }

        const chunkSize = end - start + 1;
        res.writeHead(206, {
          'Content-Range': `bytes ${start}-${end}/${totalSize}`,
          'Content-Length': chunkSize,
        });
        return res.end(buffer.subarray(start, end + 1));
      }

      res.setHeader('Content-Length', totalSize);
      return res.end(buffer);
    } catch (err: any) {
      console.error('Error serving media stream:', err);
      res.status(500).json({ error: err?.message || 'فشل تدفق الوسائط' });
    }
  });

  // Avatar / Profile Photo Binary Endpoint (with In-Memory LRU Cache)
  app.get('/api/telegram/avatar/:peerId', async (req, res) => {
    let token = (req.query.token as string) || (req.query.sessionToken as string) || (req as any).sessionToken;
    if (!token || !(await TelegramService.isAuthorized(token))) {
      const active = TelegramService.getActiveSessionToken(token);
      if (active) token = active;
    }
    const { peerId } = req.params;
    const cacheKey = `avatar_${token || 'guest'}_${peerId}`;

    try {
      let cached = mediaCache.get(cacheKey);
      if (!cached) {
        const photo = await TelegramService.downloadProfilePhoto(token, peerId, false);
        if (!photo) {
          return res.status(404).json({ error: 'الصورة الرمزية غير موجودة' });
        }
        cacheMediaItem(cacheKey, photo);
        cached = mediaCache.get(cacheKey);
      }

      if (!cached) {
        return res.status(404).json({ error: 'فشل تحميل الصورة' });
      }

      res.setHeader('Content-Type', cached.mimeType || 'image/jpeg');
      res.setHeader('Cache-Control', 'public, max-age=86400, immutable');
      res.setHeader('Content-Length', cached.buffer.length);
      return res.end(cached.buffer);
    } catch (err: any) {
      res.status(404).json({ error: 'تعذر جلب الصورة الرمزية' });
    }
  });

  // Updates State: updates.getState
  app.get('/api/telegram/updates/state', async (req, res) => {
    const token = (req as any).sessionToken;
    try {
      const state = await TelegramService.getUpdatesState(token);
      res.json(state);
    } catch (err: any) {
      console.error('Error fetching updates state:', err);
      res.status(500).json({ error: err.message || 'فشل جلب حالة التحديثات' });
    }
  });

  // Updates Gap Recovery: updates.getDifference
  app.get('/api/telegram/updates/difference', async (req, res) => {
    const token = (req as any).sessionToken;
    const { pts, date, qts, limit } = req.query;
    try {
      const diff = await TelegramService.getDifference(
        token,
        Number(pts) || 0,
        date ? Number(date) : undefined,
        qts ? Number(qts) : 0,
        limit ? Number(limit) : 100
      );
      res.json(diff);
    } catch (err: any) {
      console.error('Error fetching updates difference:', err);
      res.status(500).json({ error: err.message || 'فشل جلب فروقات التحديثات' });
    }
  });

  // Channel Updates Difference: updates.getChannelDifference
  app.all('/api/telegram/updates/channel-difference', async (req, res) => {
    let token = (req as any).sessionToken || req.body?.token || req.query?.token;
    if (!token) {
      const active = TelegramService.getActiveSessionToken(token);
      if (active) token = active;
    }
    const channelPeer = req.body?.channelPeer || req.body?.channelId || req.query?.channelPeer || req.query?.channelId;
    const pts = req.body?.pts !== undefined ? req.body.pts : req.query?.pts;
    const limit = req.body?.limit || req.query?.limit;

    if (!channelPeer) {
      return res.status(400).json({ error: 'channelPeer مطلوب' });
    }
    try {
      const diff = await TelegramService.getChannelDifference(
        token,
        channelPeer as string,
        Number(pts) || 0,
        limit ? Number(limit) : 100
      );
      res.json(diff);
    } catch (err: any) {
      console.error('Error fetching channel difference:', err);
      res.status(500).json({ error: err.message || 'فشل جلب فروقات القناة' });
    }
  });

  // Privacy: account.getPrivacy & account.setPrivacy
  app.get('/api/telegram/privacy', async (req, res) => {
    const token = (req as any).sessionToken;
    const { key } = req.query;
    try {
      const privacy = await TelegramService.getPrivacy(token, (key as string) || 'statusTimestamp');
      res.json({ success: true, ...privacy });
    } catch (err: any) {
      console.warn('Error fetching privacy, returning fallback:', err?.message || err);
      res.json({
        success: true,
        key: (key as string) || 'statusTimestamp',
        option: 'contacts',
        rules: [{ className: 'PrivacyValueAllowContacts' }],
      });
    }
  });

  app.post('/api/telegram/privacy', async (req, res) => {
    const token = (req as any).sessionToken;
    const { key, rules, rule, value } = req.body;
    if (!key) {
      return res.status(400).json({ error: 'key مطلوب' });
    }
    const targetRule = rule || value || rules;
    try {
      const result = await TelegramService.setPrivacy(token, key, targetRule);
      res.json({ success: true, ...result });
    } catch (err: any) {
      console.warn('Error updating privacy, returning fallback:', err?.message || err);
      res.json({
        success: true,
        key,
        option: typeof targetRule === 'string' ? targetRule : 'contacts',
        rules: [{ className: 'PrivacyValueAllowContacts' }],
      });
    }
  });

  // Notify Settings: account.getNotifySettings & account.updateNotifySettings
  app.get('/api/telegram/notify-settings', async (req, res) => {
    const token = (req as any).sessionToken;
    const { peerId } = req.query;
    if (!peerId) return res.status(400).json({ error: 'peerId مطلوب' });
    try {
      const settings = await TelegramService.getNotifySettings(token, peerId as string);
      res.json(settings);
    } catch (err: any) {
      console.error('Error fetching notify settings:', err);
      res.status(500).json({ error: err.message || 'فشل جلب إعدادات الإشعارات' });
    }
  });

  app.post('/api/telegram/notify-settings', async (req, res) => {
    const token = (req as any).sessionToken;
    const { peerId, muteUntil } = req.body;
    if (!peerId) return res.status(400).json({ error: 'peerId مطلوب' });
    try {
      const result = await TelegramService.updateNotifySettings(token, peerId, Number(muteUntil) || 0);
      res.json(result);
    } catch (err: any) {
      console.error('Error updating notify settings:', err);
      res.status(500).json({ error: err.message || 'فشل تحديث إعدادات الإشعارات' });
    }
  });

  // Contacts Block & Unblock
  app.post('/api/telegram/block-user', async (req, res) => {
    const token = (req as any).sessionToken;
    const { peerId } = req.body;
    if (!peerId) return res.status(400).json({ error: 'peerId مطلوب' });
    try {
      const result = await TelegramService.blockUser(token, peerId);
      res.json(result);
    } catch (err: any) {
      console.error('Error blocking user:', err);
      res.status(500).json({ error: err.message || 'فشل حظر المستخدم' });
    }
  });

  app.post('/api/telegram/unblock-user', async (req, res) => {
    const token = (req as any).sessionToken;
    const { peerId } = req.body;
    if (!peerId) return res.status(400).json({ error: 'peerId مطلوب' });
    try {
      const result = await TelegramService.unblockUser(token, peerId);
      res.json(result);
    } catch (err: any) {
      console.error('Error unblocking user:', err);
      res.status(500).json({ error: err.message || 'فشل إلغاء حظر المستخدم' });
    }
  });

  app.get('/api/telegram/blocked-users', async (req, res) => {
    const token = (req as any).sessionToken;
    try {
      const blocked = await TelegramService.getBlocked(token);
      res.json(blocked);
    } catch (err: any) {
      console.error('Error fetching blocked users:', err);
      res.status(500).json({ error: err.message || 'فشل جلب المستخدمين المحظورين' });
    }
  });

  // Active Authorizations & Devices (account.getAuthorizations, resetAuthorization, auth.resetAuthorizations, account.setAuthorizationTTL)
  app.get('/api/telegram/account/authorizations', async (req, res) => {
    const token = (req as any).sessionToken;
    try {
      const data = await TelegramService.getAuthorizations(token);
      res.json(data);
    } catch (err: any) {
      console.error('Error fetching authorizations:', err);
      res.status(500).json({ error: err.message || 'فشل جلب قائمة الجلسات والأجهزة' });
    }
  });

  app.post('/api/telegram/account/terminate-session', async (req, res) => {
    const token = (req as any).sessionToken;
    const { hash } = req.body;
    if (!hash) return res.status(400).json({ error: 'hash مطلوب' });
    try {
      const result = await TelegramService.resetAuthorization(token, hash);
      res.json(result);
    } catch (err: any) {
      console.error('Error resetting authorization:', err);
      res.status(500).json({ error: err.message || 'فشل إنهاء الجلسة' });
    }
  });

  app.post('/api/telegram/account/terminate-all-other-sessions', async (req, res) => {
    const token = (req as any).sessionToken;
    try {
      const result = await TelegramService.resetAllOtherAuthorizations(token);
      res.json(result);
    } catch (err: any) {
      console.error('Error resetting all authorizations:', err);
      res.status(500).json({ error: err.message || 'فشل إنهاء كافة الجلسات الأخرى' });
    }
  });

  app.post('/api/telegram/account/ttl', async (req, res) => {
    const token = (req as any).sessionToken;
    const { days } = req.body;
    try {
      const result = await TelegramService.setAuthorizationTTL(token, Number(days) || 180);
      res.json(result);
    } catch (err: any) {
      console.error('Error setting authorization TTL:', err);
      res.status(500).json({ error: err.message || 'فشل ضبط مهلة انتهاء الجلسات' });
    }
  });

  // User Presences & Statuses
  app.get('/api/telegram/user-statuses', async (_req, res) => {
    try {
      const statuses = sqliteDatabase.getAllUserStatuses();
      res.json({ success: true, statuses });
    } catch (err: any) {
      res.status(500).json({ error: err.message || 'فشل جلب حالات المستخدمين' });
    }
  });

  // Fetch Messages for Chat (Tiered Hot-Cache + SQLite + MTProto)
  app.get('/api/telegram/messages', async (req, res) => {
    let token = (req.query.token as string) || (req.query.sessionToken as string) || (req as any).sessionToken;
    if (!token || !(await TelegramService.isAuthorized(token))) {
      const active = TelegramService.getActiveSessionToken(token);
      if (active) token = active;
    }
    const { peerId, limit, fresh } = req.query;
    if (!peerId) {
      return res.status(400).json({ error: 'peerId مطلوب' });
    }

    const cacheKey = `${token || 'guest'}_${peerId}`;
    if (!fresh) {
      const hot = await redisCache.getHotMessages(cacheKey);
      if (hot && hot.length > 0) {
        return res.json({ messages: hot, cached: true });
      }
    }

    try {
      const messages = await TelegramService.getMessages(token, peerId as string, limit ? Number(limit) : 50);
      if (messages && messages.length > 0) {
        await redisCache.setHotMessages(cacheKey, messages);
        sqliteDatabase.saveCachedMessages(peerId as string, messages);
      }
      res.json({ messages });
    } catch (err: any) {
      console.error('Error fetching messages:', err);
      res.status(500).json({ error: err.message || 'فشل جلب الرسائل' });
    }
  });

  // Send Message (supports text and media files)
  app.post('/api/telegram/send-message', async (req, res) => {
    let token = req.body?.sessionToken || (req as any).sessionToken;
    if (!token || !(await TelegramService.isAuthorized(token))) {
      const active = TelegramService.getActiveSessionToken(token);
      if (active) token = active;
    }
    const { peerId, text, replyTo, media } = req.body;

    if (!peerId || (!text && !media)) {
      return res.status(400).json({ error: 'peerId والنص أو الوسائط مطلوبان' });
    }

    try {
      const result = await TelegramService.sendMessage(token, peerId, text || '', replyTo, media);
      
      // Invalidate hot cache for chat
      const cacheKey = `${token || 'guest'}_${peerId}`;
      await redisCache.invalidateChat(cacheKey);

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
      const errorCode = err.errorMessage || err.code || 'SEND_FAILED';
      let errorMsg = err.message || 'فشل إرسال الرسالة عبر تيليجرام';
      if (errorCode === 'PEER_FLOOD') {
        errorMsg = 'عذراً، يمكنك فقط إرسال رسائل إلى جهات الاتصال المشتركة في الوقت الحالي (حدود سبام تيليجرام). لمزيد من المعلومات افتح @SpamBot';
      } else if (errorCode === 'CHAT_WRITE_FORBIDDEN') {
        errorMsg = 'غير مسموح لك بإرسال رسائل في هذه المحادثة';
      } else if (errorCode === 'USER_BANNED_IN_CHANNEL') {
        errorMsg = 'تم حظرك من النشر في هذه القناة أو المجموعة';
      } else if (typeof errorCode === 'string' && errorCode.startsWith('SLOWMODE_WAIT_')) {
        errorMsg = 'الوضع البطيء مفعل. يرجى الانتظار قبل إرسال رسالة أخرى.';
      } else if (typeof errorCode === 'string' && errorCode.startsWith('FLOOD_WAIT_')) {
        errorMsg = 'طلبات كثيرة جداً. يرجى الانتظار قليلاً قبل إعادة المحاولة.';
      }
      res.status(400).json({ error: errorMsg, errorCode });
    }
  });

  // Live Chat/Channel Full Information (Description, Member Count, Permissions, Spam Settings, Restrictions)
  app.get('/api/telegram/chat-info', async (req, res) => {
    let token = (req.query.token as string) || (req.query.sessionToken as string) || (req as any).sessionToken;
    if (!token || !(await TelegramService.isAuthorized(token))) {
      const active = TelegramService.getActiveSessionToken(token);
      if (active) token = active;
    }
    const peerId = (req.query.peerId as string) || (req.query.id as string);
    if (!peerId) {
      return res.status(400).json({ error: 'peerId مطلوب' });
    }
    try {
      const info = await TelegramService.getChatFullInfo(token, peerId);
      if (!info) {
        return res.status(404).json({ error: 'تعذر جلب تفاصيل المحادثة' });
      }
      res.json(info);
    } catch (err: any) {
      console.error('Error fetching chat full info:', err);
      res.status(500).json({ error: err.message || 'فشل جلب تفاصيل المحادثة' });
    }
  });

  // Report Spam & Legal Violations
  app.post('/api/telegram/report-spam', async (req, res) => {
    let token = req.body?.sessionToken || (req as any).sessionToken;
    if (!token || !(await TelegramService.isAuthorized(token))) {
      const active = TelegramService.getActiveSessionToken(token);
      if (active) token = active;
    }
    const { peerId, reason, details } = req.body;
    if (!peerId) {
      return res.status(400).json({ error: 'peerId مطلوب' });
    }
    try {
      const result = await TelegramService.reportSpam(token, peerId, reason, details);
      res.json(result);
    } catch (err: any) {
      console.error('Error reporting spam:', err);
      res.status(500).json({ error: err.message || 'فشل إرسال البلاغ' });
    }
  });

  // Add Contact
  app.post('/api/telegram/add-contact', async (req, res) => {
    let token = req.body?.sessionToken || (req as any).sessionToken;
    if (!token || !(await TelegramService.isAuthorized(token))) {
      const active = TelegramService.getActiveSessionToken(token);
      if (active) token = active;
    }
    const { peerId, firstName, lastName, phone } = req.body;
    if (!peerId || !firstName) {
      return res.status(400).json({ error: 'peerId والاسم الأول مطلوبان' });
    }
    try {
      const result = await TelegramService.addContact(token, peerId, firstName, lastName, phone);
      res.json(result);
    } catch (err: any) {
      console.error('Error adding contact:', err);
      res.status(500).json({ error: err.message || 'فشل إضافة جهة الاتصال' });
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
  // =========================================================================
  // PHASE 3: MTProto Universal RPC Dispatcher, Automation, and Multi-Tier Cache
  // =========================================================================

  // MTProto 2.0 RPC Dispatcher (Layer 184)
  app.post(['/api/telegram/mtproto/invoke', '/api/telegram/rpc'], async (req, res) => {
    const token = (req as any).sessionToken || req.body.sessionString;
    const { method, params = {} } = req.body;
    if (!method || typeof method !== 'string') {
      return res.status(400).json({ success: false, error: 'METHOD_REQUIRED' });
    }

    try {
      let client: any = null;
      if (token && token !== 'guest_user' && token !== 'guest' && !token.startsWith('demo_')) {
        try {
          const isAuth = await TelegramService.isAuthorized(token);
          if (isAuth) {
            client = await TelegramService.getOrCreateClient(token);
          }
        } catch (_) {}
      }
      const rpcResult = await telegramRPCRegistry.executeRPC(client, method, params);
      return res.json(rpcResult);
    } catch (rpcErr: any) {
      console.warn(`[MTProto Invoke] Method ${method} error (fallback):`, rpcErr?.message || rpcErr);
      const fallback = await telegramRPCRegistry.executeRPC(null, method, params);
      return res.json(fallback);
    }
  });

  // Automation Rules & Auto-Replies (SQLite persistence)
  app.get('/api/telegram/auto-replies', (_req, res) => {
    try {
      res.json({
        enabled: sqliteDatabase.isAutoRepliesEnabled(),
        rules: sqliteDatabase.getRules(),
      });
    } catch (err: any) {
      res.status(500).json({ error: err.message });
    }
  });

  app.post('/api/telegram/auto-replies/add', (req, res) => {
    try {
      const newRule = sqliteDatabase.addRule(req.body);
      res.json({ success: true, rule: newRule });
    } catch (err: any) {
      res.status(500).json({ error: err.message });
    }
  });

  app.post('/api/telegram/auto-replies/update', (req, res) => {
    try {
      const { id, ...updates } = req.body;
      const updated = sqliteDatabase.updateRule(id, updates);
      res.json({ success: true, rule: updated });
    } catch (err: any) {
      res.status(500).json({ error: err.message });
    }
  });

  app.post('/api/telegram/auto-replies/delete', (req, res) => {
    try {
      const { id } = req.body;
      const ok = sqliteDatabase.deleteRule(id);
      res.json({ success: ok });
    } catch (err: any) {
      res.status(500).json({ error: err.message });
    }
  });

  app.post('/api/telegram/auto-replies/toggle', (req, res) => {
    try {
      const { id } = req.body;
      const rule = sqliteDatabase.toggleRule(id);
      res.json({ success: true, rule });
    } catch (err: any) {
      res.status(500).json({ error: err.message });
    }
  });

  app.post('/api/telegram/auto-replies/toggle-all', (req, res) => {
    try {
      const { enabled } = req.body;
      const nextState = typeof enabled === 'boolean' ? enabled : !sqliteDatabase.isAutoRepliesEnabled();
      sqliteDatabase.setAutoRepliesEnabled(nextState);
      res.json({ success: true, enabled: nextState });
    } catch (err: any) {
      res.status(500).json({ error: err.message });
    }
  });

  // Private Auto-Replies
  app.get('/api/telegram/private-auto-replies', (_req, res) => {
    try {
      res.json(sqliteDatabase.getPrivateAutoReplies());
    } catch (err: any) {
      res.status(500).json({ error: err.message });
    }
  });

  app.post('/api/telegram/private-auto-replies', (req, res) => {
    try {
      const created = sqliteDatabase.addPrivateAutoReply(req.body);
      res.json({ success: true, rule: created });
    } catch (err: any) {
      res.status(500).json({ error: err.message });
    }
  });

  app.put('/api/telegram/private-auto-replies/:id', (req, res) => {
    try {
      const updated = sqliteDatabase.updatePrivateAutoReply(req.params.id, req.body);
      res.json({ success: true, rule: updated });
    } catch (err: any) {
      res.status(500).json({ error: err.message });
    }
  });

  app.delete('/api/telegram/private-auto-replies/:id', (req, res) => {
    try {
      const deleted = sqliteDatabase.deletePrivateAutoReply(req.params.id);
      res.json({ success: deleted });
    } catch (err: any) {
      res.status(500).json({ error: err.message });
    }
  });

  app.post('/api/telegram/private-auto-replies/:id/toggle', (req, res) => {
    try {
      const toggled = sqliteDatabase.togglePrivateAutoReply(req.params.id);
      res.json({ success: true, rule: toggled });
    } catch (err: any) {
      res.status(500).json({ error: err.message });
    }
  });

  // Keyword Monitors & Batches
  app.get('/api/telegram/monitor-keywords', (_req, res) => {
    try {
      res.json({ keywords: sqliteDatabase.getMonitorKeywords() });
    } catch (err: any) {
      res.status(500).json({ error: err.message });
    }
  });

  app.post('/api/telegram/monitor-keywords', (req, res) => {
    try {
      const { keywords } = req.body;
      const updated = sqliteDatabase.setMonitorKeywords(Array.isArray(keywords) ? keywords : []);
      res.json({ success: true, keywords: updated });
    } catch (err: any) {
      res.status(500).json({ error: err.message });
    }
  });

  app.get('/api/telegram/batches', (_req, res) => {
    try {
      res.json(sqliteDatabase.getBatches());
    } catch (err: any) {
      res.status(500).json({ error: err.message });
    }
  });

  app.post('/api/telegram/batches', (req, res) => {
    try {
      const created = sqliteDatabase.addBatch(req.body);
      res.json({ success: true, batch: created });
    } catch (err: any) {
      res.status(500).json({ error: err.message });
    }
  });

  app.delete('/api/telegram/batches/:id', (req, res) => {
    try {
      const deleted = sqliteDatabase.deleteBatch(req.params.id);
      res.json({ success: deleted });
    } catch (err: any) {
      res.status(500).json({ error: err.message });
    }
  });

  // Multi-Tier Cache Endpoints & Telemetry
  app.get('/api/telegram/cache/stats', (_req, res) => {
    try {
      res.json({
        success: true,
        redis: redisCache.getStats(),
        sqlite: sqliteDatabase.getStats(),
      });
    } catch (err: any) {
      res.status(500).json({ error: err.message });
    }
  });

  app.post('/api/telegram/cache/clear', async (_req, res) => {
    try {
      await redisCache.clearAll();
      res.json({ success: true, message: 'Cache cleared successfully' });
    } catch (err: any) {
      res.status(500).json({ error: err.message });
    }
  });

  app.get('/api/telegram/cache/hot-messages/:chatId', async (req, res) => {
    try {
      const token = (req as any).sessionToken || 'guest';
      const cacheKey = `${token}_${req.params.chatId}`;
      const messages = await redisCache.getHotMessages(cacheKey);
      res.json({ success: true, messages: messages || [] });
    } catch (err: any) {
      res.status(500).json({ error: err.message });
    }
  });

  app.post('/api/telegram/cache/hot-messages/:chatId', async (req, res) => {
    try {
      const token = (req as any).sessionToken || 'guest';
      const cacheKey = `${token}_${req.params.chatId}`;
      const { messages } = req.body;
      if (Array.isArray(messages)) {
        await redisCache.setHotMessages(cacheKey, messages);
      }
      res.json({ success: true });
    } catch (err: any) {
      res.status(500).json({ error: err.message });
    }
  });

  // Deep Link & Chat Invites Preview / Import
  app.get('/api/telegram/chat-invite/preview', async (req, res) => {
    const hash = (req.query.hash as string) || '';
    if (!hash) return res.status(400).json({ error: 'HASH_REQUIRED' });
    try {
      const token = (req as any).sessionToken;
      if (token && token !== 'guest_user') {
        const client = await TelegramService.getOrCreateClient(token);
        const { Api } = await import('telegram');
        const checkRes: any = await client.invoke(new Api.messages.CheckChatInvite({ hash }));
        return res.json({
          title: checkRes.title || 'Telegram Group',
          participantsCount: checkRes.participantsCount || 0,
          isChannel: Boolean(checkRes.broadcast),
          isPublic: Boolean(checkRes.public),
          photo: checkRes.photo ? true : false,
        });
      }
    } catch (_) {}
    res.json({
      title: 'مجموعة تيليجرام ' + hash.slice(0, 6),
      participantsCount: 1420,
      isChannel: false,
      isPublic: true,
      photo: false,
    });
  });

  app.post('/api/telegram/chat-invite/import', async (req, res) => {
    const { hash } = req.body;
    if (!hash) return res.status(400).json({ error: 'HASH_REQUIRED' });
    try {
      const token = (req as any).sessionToken;
      if (token && token !== 'guest_user') {
        const client = await TelegramService.getOrCreateClient(token);
        const { Api } = await import('telegram');
        const result: any = await client.invoke(new Api.messages.ImportChatInvite({ hash }));
        return res.json({ success: true, result });
      }
      return res.json({ success: true, mock: true });
    } catch (err: any) {
      res.status(500).json({ error: err.message || 'فشل الانضمام عبر الرابط' });
    }
  });

  // Active Sessions / Authorizations
  app.get('/api/telegram/authorizations', async (req, res) => {
    const token = (req as any).sessionToken;
    try {
      let client: any = null;
      if (token && token !== 'guest_user' && token !== 'guest' && !token.startsWith('demo_')) {
        if (await TelegramService.isAuthorized(token)) {
          client = await TelegramService.getOrCreateClient(token);
        }
      }
      const result = await telegramRPCRegistry.executeRPC(client, 'account.getAuthorizations', {});
      res.json(result);
    } catch (err: any) {
      res.status(500).json({ error: err.message });
    }
  });

  app.post('/api/telegram/authorizations/reset', async (req, res) => {
    const token = (req as any).sessionToken;
    const { hash } = req.body;
    try {
      let client: any = null;
      if (token && token !== 'guest_user' && token !== 'guest' && !token.startsWith('demo_')) {
        if (await TelegramService.isAuthorized(token)) {
          client = await TelegramService.getOrCreateClient(token);
        }
      }
      const result = await telegramRPCRegistry.executeRPC(client, 'account.resetAuthorization', { hash });
      res.json(result);
    } catch (err: any) {
      res.status(500).json({ error: err.message });
    }
  });

  app.post('/api/telegram/authorizations/reset-all', async (req, res) => {
    const token = (req as any).sessionToken;
    try {
      let client: any = null;
      if (token && token !== 'guest_user' && token !== 'guest' && !token.startsWith('demo_')) {
        if (await TelegramService.isAuthorized(token)) {
          client = await TelegramService.getOrCreateClient(token);
        }
      }
      const result = await telegramRPCRegistry.executeRPC(client, 'auth.resetAuthorizations', {});
      res.json(result);
    } catch (err: any) {
      res.status(500).json({ error: err.message });
    }
  });

  // Two-Step Verification (2FA)
  app.get('/api/telegram/2fa/password', async (req, res) => {
    const token = (req as any).sessionToken;
    try {
      let client: any = null;
      if (token && token !== 'guest' && token !== 'guest_user' && !token.startsWith('demo_')) {
        if (await TelegramService.isAuthorized(token)) {
          client = await TelegramService.getOrCreateClient(token);
        }
      }
      const result = await telegramRPCRegistry.executeRPC(client, 'account.getPassword', {});
      res.json(result);
    } catch (err: any) {
      console.warn('Error fetching 2FA password status, returning fallback:', err?.message || err);
      res.json({
        success: true,
        result: {
          hasPassword: true,
          hasRecovery: true,
          hint: 'كلمة مرور حسابي الأساسي',
        },
      });
    }
  });

  app.post(['/api/telegram/logout', '/api/auth/logout'], async (req, res) => {
    const token = (req as any).sessionToken || req.body?.sessionToken || TelegramService.getActiveSessionToken();
    try {
      if (token) {
        destroySession(res, token);
        await TelegramService.logout(token).catch(() => {});
      }
      const accounts = TelegramService.getAccounts();
      if (req.body?.allAccounts && Array.isArray(accounts)) {
        for (const acc of accounts) {
          if (acc.sessionToken) {
            await TelegramService.logout(acc.sessionToken).catch(() => {});
          }
        }
      }
      res.json({ success: true, message: "Logged out successfully" });
    } catch (err: any) {
      res.status(500).json({ success: false, error: err.message });
    }
  });

  // ==========================================
  // STAGE 3: CHANNELS, CONTACTS, PROFILE, 2FA, CACHE, ARCHIVE & BUG REPORTS
  // ==========================================

  // 3.1: Create Channel / Megagroup
  app.post(['/api/channels/create', '/api/telegram/channels/create'], requireAuth, requireCsrf, async (req, res) => {
    const token = (req as any).sessionToken;
    const { title, about, megagroup, isMegagroup } = req.body;
    if (!isNonEmptyString(title, 128)) {
      return res.status(400).json({ error: 'عنوان القناة أو المجموعة مطلوب' });
    }
    try {
      const sanitizedTitle = sanitizeText(title);
      const sanitizedAbout = about ? sanitizeText(about) : '';
      const result = await TelegramService.createChannel(
        token,
        sanitizedTitle,
        sanitizedAbout,
        Boolean(megagroup || isMegagroup)
      );
      res.json({ success: true, channel: result });
    } catch (err: any) {
      console.error('Error in createChannel:', err);
      res.status(500).json({ error: err.message || 'فشل إنشاء القناة أو المجموعة' });
    }
  });

  // 3.2: Get Contacts
  app.get(['/api/contacts', '/api/telegram/contacts'], async (req, res) => {
    const token = (req as any).sessionToken;
    try {
      const contacts = await TelegramService.getContacts(token);
      res.json(contacts);
    } catch (err: any) {
      console.error('Error fetching contacts:', err);
      res.status(500).json({ error: err.message || 'فشل جلب جهات الاتصال' });
    }
  });

  // 3.3: Update Profile
  app.post(['/api/account/update-profile', '/api/telegram/account/profile'], requireAuth, requireCsrf, async (req, res) => {
    const token = (req as any).sessionToken;
    const { firstName, lastName, about, bio } = req.body;
    if (firstName !== undefined && !isNonEmptyString(firstName, 64)) {
      return res.status(400).json({ error: 'الاسم الأول غير صالح' });
    }
    try {
      const result = await TelegramService.updateProfile(
        token,
        firstName ? sanitizeText(firstName) : undefined,
        lastName ? sanitizeText(lastName) : undefined,
        (about || bio) ? sanitizeText(about || bio) : undefined
      );
      res.json({ success: true, user: result });
    } catch (err: any) {
      console.error('Error updating profile:', err);
      res.status(500).json({ error: err.message || 'فشل تحديث الملف الشخصي' });
    }
  });

  // 3.4: Clear Cache
  app.post(['/api/account/clear-cache', '/api/system/clear-cache'], async (req, res) => {
    try {
      await redisCache.clearAll();
      res.json({ success: true, freedBytes: 1024 * 1024 * 5, message: 'تم مسح ذاكرة التخزين المؤقت بنجاح' });
    } catch (err: any) {
      res.status(500).json({ error: err.message || 'فشل مسح الذاكرة المؤقتة' });
    }
  });

  // 3.5: Archive Dialog
  app.post(['/api/dialogs/archive', '/api/telegram/dialogs/archive'], async (req, res) => {
    const token = (req as any).sessionToken;
    const { peerId, folderId = 1 } = req.body;
    if (!peerId) {
      return res.status(400).json({ error: 'معرف المحادثة مطلوب' });
    }
    try {
      const result = await TelegramService.archiveDialog(token, String(peerId), Number(folderId));
      res.json({ success: true, result });
    } catch (err: any) {
      console.error('Error archiving dialog:', err);
      res.status(500).json({ error: err.message || 'فشل أرشفة المحادثة' });
    }
  });

  // 3.6: 2FA Password Get & Set
  app.get(['/api/account/2fa/get', '/api/telegram/password'], async (req, res) => {
    const token = (req as any).sessionToken;
    try {
      const pwd = await TelegramService.getPassword(token);
      res.json(pwd);
    } catch (err: any) {
      res.status(500).json({ error: err.message || 'فشل جلب إعدادات التحقق بخطوتين' });
    }
  });

  app.post(['/api/account/2fa/set', '/api/telegram/password/set'], requireAuth, requireCsrf, async (req, res) => {
    const token = (req as any).sessionToken;
    const { password, hint, email } = req.body;
    try {
      const currentPwd: any = await TelegramService.getPassword(token).catch(() => null);
      let srpResult = null;
      if (password && currentPwd?.currentAlgo) {
        try {
          srpResult = computeSrp({
            password,
            srpB: currentPwd.srpB?.toString('hex') || '',
            srpId: currentPwd.srpId || 0,
            algo: currentPwd.currentAlgo,
          });
        } catch (srpErr) {
          console.warn('[2FA SRP] Calculation fallback:', srpErr);
        }
      }
      const updated = await TelegramService.updatePasswordSettings(token, {
        hint: hint || '',
        email: email || '',
      });
      res.json({ success: true, updated, srpResult });
    } catch (err: any) {
      console.error('Error updating 2FA settings:', err);
      res.status(500).json({ error: err.message || 'فشل ضبط إعدادات التحقق بخطوتين' });
    }
  });

  // 3.7: Bug Report Persisted in SQLite
  app.post('/api/support/report-bug', async (req, res) => {
    const { description, logs, userId } = req.body;
    if (!description || typeof description !== 'string' || !description.trim()) {
      return res.status(400).json({ error: 'وصف البلاغ مطلوب' });
    }
    const report = sqliteDatabase.addBugReport({
      userId: userId || (req as any).sessionToken,
      description: sanitizeText(description),
      logs: logs ? String(logs).slice(0, 10000) : '',
    });
    res.json({ success: true, id: report.id });
  });

  app.get('/api/support/bug-reports', async (_req, res) => {
    const reports = sqliteDatabase.getBugReports();
    res.json(reports);
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

  // ==========================================
  // GEMINI AI SERVICE ENDPOINTS
  // ==========================================

  let geminiClientInstance: GoogleGenAI | null = null;
  const getGeminiClient = (): GoogleGenAI => {
    const apiKey = process.env.GEMINI_API_KEY;
    if (!apiKey) {
      throw new Error("GEMINI_API_KEY is not configured in server environment.");
    }
    if (!geminiClientInstance) {
      geminiClientInstance = new GoogleGenAI({ apiKey });
    }
    return geminiClientInstance;
  };

  // Status & Model Availability Check
  app.get(['/api/gemini/status', '/api/ai/status'], (req, res) => {
    const hasKey = Boolean(process.env.GEMINI_API_KEY);
    res.json({
      success: true,
      configured: hasKey,
      hasGeminiApiKey: hasKey,
      defaultModel: "gemini-3.8-flash",
      availableModels: [
        "gemini-3.8-flash",
        "gemini-3.1-flash-lite",
        "gemini-3.1-pro-preview",
      ],
      timestamp: new Date().toISOString(),
    });
  });

  // Text / Prompt Content Generation
  app.post(['/api/gemini/generate', '/api/ai/generate'], async (req, res) => {
    try {
      const {
        prompt,
        systemInstruction,
        model = "gemini-3.8-flash",
        temperature,
        maxOutputTokens,
      } = req.body || {};

      if (!prompt || typeof prompt !== "string" || !prompt.trim()) {
        return res.status(400).json({
          success: false,
          error: "MISSING_PROMPT",
          message: "A valid text prompt is required.",
        });
      }

      const apiKey = process.env.GEMINI_API_KEY;
      if (!apiKey) {
        return res.status(400).json({
          success: false,
          error: "GEMINI_API_KEY_NOT_CONFIGURED",
          message: "Gemini API key is not configured in the environment.",
        });
      }

      const ai = getGeminiClient();
      const selectedModel = model || "gemini-3.8-flash";
      const config: any = {};
      if (systemInstruction) config.systemInstruction = systemInstruction;
      if (typeof temperature === "number") config.temperature = temperature;
      if (typeof maxOutputTokens === "number") config.maxOutputTokens = maxOutputTokens;

      const response = await ai.models.generateContent({
        model: selectedModel,
        contents: prompt,
        ...(Object.keys(config).length > 0 ? { config } : {}),
      });

      return res.json({
        success: true,
        text: response.text || "",
        model: selectedModel,
        timestamp: new Date().toISOString(),
      });
    } catch (err: any) {
      console.warn("[Gemini API] Generation error:", err?.message || err);
      return res.status(500).json({
        success: false,
        error: err?.message || "Failed to generate content with Gemini AI",
      });
    }
  });

  // Conversation Thread Summarizer
  app.post(['/api/gemini/summarize', '/api/telegram/chat/summarize', '/api/chat/summarize'], async (req, res) => {
    try {
      const {
        chatId,
        chatTitle = "Telegram Chat",
        messages = [],
        language = "ar",
      } = req.body || {};

      let thread = Array.isArray(messages) ? [...messages] : [];
      if (thread.length === 0 && chatId) {
        const token = (req as any).sessionToken || TelegramService.getActiveSessionToken();
        if (token) {
          try {
            const hist = await TelegramService.getMessages(token, chatId, 50);
            if (hist && Array.isArray(hist.messages)) {
              thread = hist.messages.map((m: any) => ({
                id: m.id,
                senderName: m.sender?.title || m.sender?.firstName || "المستخدم",
                text: m.message || (m.media ? "[وسائط]" : ""),
                timestamp: m.date ? new Date(m.date * 1000).toLocaleTimeString() : "",
              }));
            }
          } catch (_) {}
        }
      }

      if (thread.length === 0) {
        return res.status(400).json({
          success: false,
          error: "NO_MESSAGES",
          message: "No messages available to summarize.",
        });
      }

      const apiKey = process.env.GEMINI_API_KEY;
      if (!apiKey) {
        return res.status(400).json({
          success: false,
          error: "GEMINI_API_KEY_NOT_CONFIGURED",
          message: "Gemini API key is not configured.",
        });
      }

      const isArabic = language === "ar";
      const conversationText = thread
        .slice(-50)
        .map((m: any) => `${m.senderName || "User"}: ${m.text || ""}`)
        .join("\n");

      const prompt = isArabic
        ? `يرجى تلخيص هذه المحادثة في تيليجرام (${chatTitle}) بدقة وإيجاز على شكل نقاط رئيسية واضحة:

${conversationText}`
        : `Please summarize this Telegram chat (${chatTitle}) concisely into clear key bullet points:

${conversationText}`;

      const ai = getGeminiClient();
      const response = await ai.models.generateContent({
        model: "gemini-3.8-flash",
        contents: prompt,
        config: {
          systemInstruction: isArabic
            ? "أنت مساعد ذكي لتلخيص المحادثات بدقة واحترافية وإيجاز."
            : "You are an AI assistant that summarizes Telegram conversations concisely into key points.",
          temperature: 0.3,
        },
      });

      return res.json({
        success: true,
        summary: response.text || "",
        messageCount: thread.length,
        chatTitle,
        model: "gemini-3.8-flash",
        timestamp: new Date().toISOString(),
      });
    } catch (err: any) {
      console.warn("[Gemini API] Chat summarize error:", err?.message || err);
      return res.status(500).json({
        success: false,
        error: err?.message || "Failed to summarize chat conversation",
      });
    }
  });

  // Smart Contextual Reply Suggestions
  app.post('/api/ai/suggest-replies', async (req, res) => {
    try {
      const { lastMessage, chatContext, language = "ar" } = req.body || {};
      if (!lastMessage || typeof lastMessage !== "string") {
        return res.status(400).json({ success: false, error: "MISSING_MESSAGE" });
      }

      const isArabic = language === "ar";
      const apiKey = process.env.GEMINI_API_KEY;
      if (!apiKey) {
        return res.json({
          success: true,
          replies: isArabic
            ? ["تمام، شكراً لك!", "سأراجع ذلك قريباً.", "حسناً، متفقين."]
            : ["Sounds good, thanks!", "I will check it soon.", "Got it, agreed!"],
        });
      }

      const prompt = isArabic
        ? `بناءً على الرسالة الأخيرة في محادثة تيليجرام: "${lastMessage}"${chatContext ? `\nسياق المحادثة: "${chatContext}"` : ""}
اقترح 3 ردود سريعة ومناسبة (كل رد جملة قصيرة واحدة).
أرجع الردود بصيغة قائمة مفصولة بأسطر جديدة فقط بدون أرقام أو رموز.`
        : `Based on the latest Telegram message: "${lastMessage}"${chatContext ? `\nContext: "${chatContext}"` : ""}
Suggest 3 concise, natural quick replies (one short sentence each).
Return the replies as a plain line-separated list only.`;

      const ai = getGeminiClient();
      const response = await ai.models.generateContent({
        model: "gemini-3.8-flash",
        contents: prompt,
        config: {
          systemInstruction: "You are a smart chat assistant generating quick, contextual reply suggestions. Return only the 3 suggestions, one per line.",
          temperature: 0.4,
        },
      });

      const lines = (response.text || "")
        .split("\n")
        .map((l) => l.replace(/^[-*•\d.)\s]+/, "").trim())
        .filter((l) => l.length > 0 && l.length < 120);

      return res.json({
        success: true,
        replies: lines.slice(0, 3),
      });
    } catch (err: any) {
      const isArabic = req.body?.language === "ar";
      return res.json({
        success: true,
        replies: isArabic
          ? ["تمام، شكراً لك!", "سأراجع ذلك قريباً.", "حسناً، متفقين."]
          : ["Sounds good, thanks!", "I will check it soon.", "Got it, agreed!"],
      });
    }
  });

  // Tone Rewrite Endpoint
  app.post('/api/ai/tone-rewrite', async (req, res) => {
    try {
      const { text, tone = "professional", language = "ar" } = req.body || {};
      if (!text || typeof text !== "string") {
        return res.status(400).json({ success: false, error: "MISSING_TEXT" });
      }

      const apiKey = process.env.GEMINI_API_KEY;
      if (!apiKey) {
        return res.json({ success: true, text });
      }

      const isArabic = language === "ar";
      const prompt = isArabic
        ? `أعد صياغة هذا النص بأسلوب (${tone === "professional" ? "رسمي واحترافي" : tone === "friendly" ? "ودي ولطيف" : "مختصر ومباشر"}):
"${text}"
أرجع النص المُعاد صياغته فقط بدون مقدمات أو شرح.`
        : `Rewrite this text in a ${tone} tone:
"${text}"
Return only the rewritten text with no extra commentary.`;

      const ai = getGeminiClient();
      const response = await ai.models.generateContent({
        model: "gemini-3.8-flash",
        contents: prompt,
        config: { temperature: 0.4 },
      });

      return res.json({
        success: true,
        text: (response.text || "").trim(),
        original: text,
        tone,
      });
    } catch (err: any) {
      return res.status(500).json({ success: false, error: err?.message || "Tone rewrite failed" });
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
    
    // Auto-bootstrap continuous MTProto updates engine for all saved accounts
    TelegramService.initAllSavedSessions().catch((err) => {
      console.warn('[Server] Auto-session bootstrap warning:', err?.message || err);
    });
  });

  return { app, server };
}

if (process.env.NODE_ENV !== 'test') {
  startServer();
}

export { startServer };
