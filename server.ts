import express from 'express';
import path from 'path';
import cors from 'cors';
import cookieParser from 'cookie-parser';
import multer from 'multer';
import { createServer as createViteServer } from 'vite';
import { TelegramService, TELEGRAM_API_ID, TELEGRAM_API_HASH } from './server/telegramService.js';

const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 25 * 1024 * 1024 }, // 25 MB
});

async function startServer() {
  const app = express();
  const PORT = 3000;

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
        sessionToken: token,
        isLoggedIn: auth.isLoggedIn,
        user: auth.user,
      });
    } catch (err: any) {
      res.json({
        apiId: TELEGRAM_API_ID,
        apiHashConfigured: true,
        sessionToken: token,
        isLoggedIn: false,
        error: err.message,
      });
    }
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
      res.json({ success: true, result });
    } catch (err: any) {
      console.error('Error sending message:', err);
      res.status(500).json({ error: err.message || 'فشل إرسال الرسالة' });
    }
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

  app.listen(PORT, '0.0.0.0', () => {
    console.log(`Telegram Web Server running on port ${PORT}`);
  });
}

startServer();
