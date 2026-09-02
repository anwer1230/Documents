import { TelegramClient, Api } from "telegram";
import { StringSession } from "telegram/sessions/index.js";
import type { TelegramDialog, TelegramMessage, TelegramUser } from "../src/types.js";

const DEFAULT_API_ID = 22043994;
const DEFAULT_API_HASH = "56f64582b363d367280db96586b97801";

export function getTelegramCredentials() {
  const apiIdStr = process.env.TELEGRAM_API_ID;
  const apiId = apiIdStr ? parseInt(apiIdStr, 10) : DEFAULT_API_ID;
  const apiHash = process.env.TELEGRAM_API_HASH || DEFAULT_API_HASH;
  return { apiId, apiHash };
}

// In-memory client cache to reuse active MTProto connections
const clientCache = new Map<string, { client: TelegramClient; lastActive: number }>();

// Cleanup stale clients after 30 minutes of inactivity
setInterval(() => {
  const now = Date.now();
  for (const [key, item] of clientCache.entries()) {
    if (now - item.lastActive > 30 * 60 * 1000) {
      item.client.disconnect().catch(() => {});
      clientCache.delete(key);
    }
  }
}, 5 * 60 * 1000);

export function purgeClientCache(sessionString: string) {
  if (!sessionString) return;
  const cached = clientCache.get(sessionString);
  if (cached) {
    cached.client.disconnect().catch(() => {});
    clientCache.delete(sessionString);
  }
}

export function isAuthError(error: any): boolean {
  if (!error) return false;
  const msg = String(error.message || error.errorMessage || error || "");
  const code = error.code || error.statusCode;
  return (
    code === 401 ||
    msg.includes("AUTH_KEY_UNREGISTERED") ||
    msg.includes("AUTH_KEY_INVALID") ||
    msg.includes("SESSION_REVOKED") ||
    msg.includes("SESSION_EXPIRED") ||
    msg.includes("USER_DEACTIVATED") ||
    msg.includes("SESSION_PASSWORD_NEEDED")
  );
}

export async function getOrCreateClient(sessionString = ""): Promise<{ client: TelegramClient; session: StringSession }> {
  const { apiId, apiHash } = getTelegramCredentials();
  const session = new StringSession(sessionString);
  const cacheKey = sessionString || "temp_" + Math.random().toString(36).substring(2);

  if (sessionString && clientCache.has(cacheKey)) {
    const cached = clientCache.get(cacheKey)!;
    cached.lastActive = Date.now();
    try {
      if (!cached.client.connected) {
        await cached.client.connect();
      }
      return { client: cached.client, session };
    } catch (err: any) {
      if (isAuthError(err)) {
        purgeClientCache(cacheKey);
      }
      throw err;
    }
  }

  const client = new TelegramClient(session, apiId, apiHash, {
    connectionRetries: 2,
    useWSS: false,
    autoReconnect: false,
  });
  (client as any).setLogLevel('none');
  client.onError = async () => {};

  try {
    await client.connect();

    if (sessionString) {
      clientCache.set(cacheKey, { client, lastActive: Date.now() });
    }

    return { client, session };
  } catch (err: any) {
    if (isAuthError(err) && sessionString) {
      purgeClientCache(cacheKey);
    }
    throw err;
  }
}

export async function sendTelegramCode(phoneNumber: string) {
  const { apiId, apiHash } = getTelegramCredentials();
  const session = new StringSession("");
  const client = new TelegramClient(session, apiId, apiHash, {
    connectionRetries: 2,
    autoReconnect: false,
  });
  (client as any).setLogLevel('none');
  client.onError = async () => {};

  await client.connect();

  try {
    const result = await client.sendCode(
      {
        apiId,
        apiHash,
      },
      phoneNumber
    );

    const sessionString = session.save();
    return {
      phoneCodeHash: result.phoneCodeHash,
      isCodeViaApp: result.isCodeViaApp,
      sessionString,
    };
  } catch (error: any) {
    await client.disconnect().catch(() => {});
    throw error;
  }
}

export async function signInTelegram(params: {
  phoneNumber: string;
  phoneCodeHash: string;
  phoneCode: string;
  sessionString: string;
}) {
  const { apiId, apiHash } = getTelegramCredentials();
  const session = new StringSession(params.sessionString);
  const client = new TelegramClient(session, apiId, apiHash, {
    connectionRetries: 2,
    autoReconnect: false,
  });
  (client as any).setLogLevel('none');
  client.onError = async () => {};

  await client.connect();

  try {
    const user = await client.invoke(
      new Api.auth.SignIn({
        phoneNumber: params.phoneNumber,
        phoneCodeHash: params.phoneCodeHash,
        phoneCode: params.phoneCode,
      })
    );

    const newSessionString = session.save();
    const me = await client.getMe();

    const formattedUser: TelegramUser = {
      id: me.id.toString(),
      firstName: (me as any).firstName || "Telegram User",
      lastName: (me as any).lastName || "",
      username: (me as any).username || "",
      phone: (me as any).phone || params.phoneNumber,
      isPremium: (me as any).premium || false,
      status: "online",
    };

    clientCache.set(newSessionString, { client, lastActive: Date.now() });

    return {
      sessionString: newSessionString,
      user: formattedUser,
    };
  } catch (error: any) {
    if (error.errorMessage === "SESSION_PASSWORD_NEEDED") {
      const interimSession = session.save();
      return {
        requires2FA: true,
        sessionString: interimSession,
      };
    }
    await client.disconnect().catch(() => {});
    throw error;
  }
}

export async function check2FAPassword(params: { password: string; sessionString: string }) {
  const { client, session } = await getOrCreateClient(params.sessionString);

  const me = await (client as any).signInWithPassword(
    {
      apiId: getTelegramCredentials().apiId,
      apiHash: getTelegramCredentials().apiHash,
    },
    {
      password: async () => params.password,
    }
  );

  const finalSessionString = session.save();

  const formattedUser: TelegramUser = {
    id: me.id.toString(),
    firstName: (me as any).firstName || "Telegram User",
    lastName: (me as any).lastName || "",
    username: (me as any).username || "",
    phone: (me as any).phone || "",
    isPremium: (me as any).premium || false,
    status: "online",
  };

  clientCache.set(finalSessionString, { client, lastActive: Date.now() });

  return {
    sessionString: finalSessionString,
    user: formattedUser,
  };
}

export async function verifyTelegramSession(sessionString: string) {
  if (!sessionString) {
    throw new Error("No session provided");
  }

  const { client } = await getOrCreateClient(sessionString);
  const me = await client.getMe();

  if (!me) {
    throw new Error("Invalid session");
  }

  const formattedUser: TelegramUser = {
    id: me.id.toString(),
    firstName: (me as any).firstName || "Telegram User",
    lastName: (me as any).lastName || "",
    username: (me as any).username || "",
    phone: (me as any).phone || "",
    isPremium: (me as any).premium || false,
    status: "online",
  };

  return formattedUser;
}

export async function fetchTelegramDialogs(sessionString: string, limit = 40): Promise<TelegramDialog[]> {
  const { client } = await getOrCreateClient(sessionString);
  const dialogs = await client.getDialogs({ limit });

  return dialogs.map((d: any) => {
    const entity = d.entity || {};
    const id = d.id?.toString() || (entity.id ? entity.id.toString() : Math.random().toString());
    const isChannel = d.isChannel || entity.broadcast || false;
    const isGroup = d.isGroup || entity.megagroup || (entity.className === "Chat");
    const isUser = d.isUser || (entity.className === "User");
    const isBot = isUser && (entity.bot || false);

    let type: 'user' | 'group' | 'channel' | 'bot' | 'saved' = 'user';
    if (d.name === "Saved Messages" || entity.self) type = 'saved';
    else if (isBot) type = 'bot';
    else if (isChannel) type = 'channel';
    else if (isGroup) type = 'group';

    let mediaType: string | undefined;
    if (d.message?.media) {
      const mClass = d.message.media.className || "";
      if (mClass.includes("Photo")) mediaType = "photo";
      else if (mClass.includes("Document")) mediaType = "document";
      else if (mClass.includes("Voice")) mediaType = "voice";
      else if (mClass.includes("Video")) mediaType = "video";
      else mediaType = "media";
    }

    return {
      id,
      title: d.title || d.name || "Chat",
      name: d.name || d.title || "Chat",
      type,
      username: entity.username || undefined,
      unreadCount: d.unreadCount || 0,
      unreadMentionsCount: d.unreadMentionsCount || 0,
      pinned: d.pinned || false,
      archived: d.archived || false,
      isVerified: entity.verified || false,
      isOnline: entity.status?.className === "UserStatusOnline",
      memberCount: entity.participantsCount || undefined,
      photoUrl: `/api/telegram/photo/${id}`,
      lastMessage: d.message ? {
        id: d.message.id,
        text: d.message.message || (mediaType ? `[${mediaType}]` : ""),
        date: d.message.date || Math.floor(Date.now() / 1000),
        senderName: d.message.sender?.firstName || "",
        out: d.message.out || false,
        mediaType,
      } : undefined,
    };
  });
}

export async function fetchTelegramMessages(sessionString: string, chatId: string, limit = 50, offsetId = 0): Promise<TelegramMessage[]> {
  const { client } = await getOrCreateClient(sessionString);
  
  // Resolve peer
  let peer: any = chatId;
  try {
    if (!isNaN(Number(chatId))) {
      peer = parseInt(chatId, 10);
    }
  } catch {
    peer = chatId;
  }

  const messages = await client.getMessages(peer, {
    limit,
    offsetId,
  });

  return messages.map((m: any) => {
    let mediaObj: any = undefined;
    if (m.media) {
      const mClass = m.media.className || "";
      let mediaType: 'photo' | 'document' | 'voice' | 'video' | 'audio' | 'sticker' | 'webpage' = 'document';
      
      if (mClass.includes("Photo")) {
        mediaType = 'photo';
        mediaObj = {
          type: 'photo',
          url: `/api/telegram/media/${chatId}/${m.id}`,
          width: 800,
          height: 600,
        };
      } else if (mClass.includes("Document")) {
        const doc = m.media.document;
        const mime = doc?.mimeType || "";
        const isVoice = mime.includes("audio/ogg") || mime.includes("opus");
        const isAudio = mime.startsWith("audio/");
        const isVideo = mime.startsWith("video/");
        
        if (isVoice) {
          mediaType = 'voice';
          mediaObj = {
            type: 'voice',
            url: `/api/telegram/media/${chatId}/${m.id}`,
            mimeType: mime,
            duration: 15,
          };
        } else if (isVideo) {
          mediaType = 'video';
          mediaObj = {
            type: 'video',
            url: `/api/telegram/media/${chatId}/${m.id}`,
            mimeType: mime,
          };
        } else if (isAudio) {
          mediaType = 'audio';
          mediaObj = {
            type: 'audio',
            url: `/api/telegram/media/${chatId}/${m.id}`,
            mimeType: mime,
          };
        } else {
          mediaType = 'document';
          mediaObj = {
            type: 'document',
            url: `/api/telegram/media/${chatId}/${m.id}`,
            fileName: doc?.attributes?.find((a: any) => a.fileName)?.fileName || "file",
            fileSize: doc?.size ? Number(doc.size) : undefined,
            mimeType: mime,
          };
        }
      }
    }

    let reactions: any[] = [];
    if (m.reactions?.results) {
      reactions = m.reactions.results.map((r: any) => ({
        emoticon: r.reaction?.emoticon || "👍",
        count: r.count || 1,
        chosen: r.chosen || false,
      }));
    }

    return {
      id: m.id,
      chatId,
      senderId: m.senderId?.toString() || "",
      senderName: m.sender ? (m.sender.firstName || m.sender.title || "User") : (m.out ? "Me" : "Sender"),
      text: m.message || "",
      date: m.date || Math.floor(Date.now() / 1000),
      out: m.out || false,
      pinned: m.pinned || false,
      views: m.views || undefined,
      forwards: m.forwards || undefined,
      replyToMsgId: m.replyTo?.replyToMsgId || undefined,
      media: mediaObj,
      reactions,
    };
  });
}

export async function sendTelegramMessage(params: {
  sessionString: string;
  chatId: string;
  message: string;
  replyToMsgId?: number;
}) {
  const { client } = await getOrCreateClient(params.sessionString);
  
  let peer: any = params.chatId;
  try {
    if (!isNaN(Number(params.chatId))) {
      peer = parseInt(params.chatId, 10);
    }
  } catch {
    peer = params.chatId;
  }

  const result = await client.sendMessage(peer, {
    message: params.message,
    replyTo: params.replyToMsgId,
  });

  return {
    id: result.id,
    chatId: params.chatId,
    senderId: (result as any).senderId?.toString() || "",
    senderName: "Me",
    text: result.message || params.message,
    date: result.date || Math.floor(Date.now() / 1000),
    out: true,
  };
}

export async function deleteTelegramMessages(params: {
  sessionString: string;
  chatId: string;
  messageIds: number[];
  revoke?: boolean;
}) {
  const { client } = await getOrCreateClient(params.sessionString);
  
  let peer: any = params.chatId;
  try {
    if (!isNaN(Number(params.chatId))) {
      peer = parseInt(params.chatId, 10);
    }
  } catch {
    peer = params.chatId;
  }

  await client.deleteMessages(peer, params.messageIds, {
    revoke: params.revoke !== false,
  });

  return { success: true };
}

const photoCache = new Map<string, { buffer: Buffer; expiresAt: number }>();

export async function downloadTelegramPhoto(sessionString: string, peerId: string): Promise<Buffer | null> {
  const cacheKey = `${sessionString.substring(0, 16)}_${peerId}`;
  const cached = photoCache.get(cacheKey);
  if (cached && cached.expiresAt > Date.now()) {
    return cached.buffer;
  }

  try {
    const { client } = await getOrCreateClient(sessionString);
    let peer: any = peerId;
    if (peerId === "me" || peerId === "self") {
      peer = "me";
    } else if (!isNaN(Number(peerId))) {
      peer = parseInt(peerId, 10);
    }
    const buffer = await client.downloadProfilePhoto(peer);
    if (buffer && Buffer.isBuffer(buffer) && buffer.length > 0) {
      // Cache for 10 minutes
      photoCache.set(cacheKey, { buffer, expiresAt: Date.now() + 10 * 60 * 1000 });
      return buffer as Buffer;
    }
    return null;
  } catch {
    return null;
  }
}

export async function downloadTelegramMedia(sessionString: string, chatId: string, messageId: number): Promise<Buffer | null> {
  try {
    const { client } = await getOrCreateClient(sessionString);
    let peer: any = chatId;
    if (!isNaN(Number(chatId))) {
      peer = parseInt(chatId, 10);
    }
    const messages = await client.getMessages(peer, { ids: messageId });
    if (messages && messages[0] && messages[0].media) {
      const buffer = await client.downloadMedia(messages[0].media, {});
      return buffer as Buffer;
    }
    return null;
  } catch {
    return null;
  }
}

// Link Parsing & Invite Resolution (Telegram Android & Web standard)
export function parseTelegramLink(input: string): { type: "invite" | "username" | "url"; value: string } {
  const clean = input.trim();

  // Invite hash patterns: t.me/+hash, t.me/joinchat/hash, tg://join?invite=hash
  const invitePlusMatch = clean.match(/(?:https?:\/\/)?(?:www\.)?t\.me\/\+([a-zA-Z0-9_-]+)/i);
  if (invitePlusMatch) return { type: "invite", value: invitePlusMatch[1] };

  const joinChatMatch = clean.match(/(?:https?:\/\/)?(?:www\.)?t\.me\/joinchat\/([a-zA-Z0-9_-]+)/i);
  if (joinChatMatch) return { type: "invite", value: joinChatMatch[1] };

  const tgJoinMatch = clean.match(/tg:\/\/join\?invite=([a-zA-Z0-9_-]+)/i);
  if (tgJoinMatch) return { type: "invite", value: tgJoinMatch[1] };

  // Username patterns: t.me/username, @username, tg://resolve?domain=username
  const usernameMatch = clean.match(/(?:https?:\/\/)?(?:www\.)?t\.me\/([a-zA-Z0-9_]{3,})/i);
  if (usernameMatch && !["joinchat", "share", "addstickers", "setlanguage", "proxy"].includes(usernameMatch[1].toLowerCase())) {
    return { type: "username", value: usernameMatch[1] };
  }

  const tgResolveMatch = clean.match(/tg:\/\/resolve\?domain=([a-zA-Z0-9_]{3,})/i);
  if (tgResolveMatch) return { type: "username", value: tgResolveMatch[1] };

  const atMatch = clean.match(/^@([a-zA-Z0-9_]{3,})$/);
  if (atMatch) return { type: "username", value: atMatch[1] };

  if (/^[a-zA-Z0-9_-]{16,}$/.test(clean)) {
    return { type: "invite", value: clean };
  }

  return { type: "url", value: clean };
}

export async function resolveTelegramLink(sessionString: string, link: string) {
  const parsed = parseTelegramLink(link);
  const { client } = await getOrCreateClient(sessionString);

  if (parsed.type === "invite") {
    try {
      const invite: any = await client.invoke(
        new Api.messages.CheckChatInvite({
          hash: parsed.value,
        })
      );

      const isAlreadyJoined = invite.className === "ChatInviteAlready";
      const chat = invite.chat || invite;

      return {
        type: "invite",
        hash: parsed.value,
        title: chat.title || "Telegram Group / Channel",
        participantsCount: chat.participantsCount || invite.participantsCount || 0,
        isChannel: chat.broadcast || chat.megagroup || false,
        isGroup: chat.megagroup || (!chat.broadcast && chat.participantsCount > 0),
        alreadyJoined: isAlreadyJoined,
        about: chat.about || invite.about || "مجموعة أو قناة تيليجرام عبر رابط دعوة خاص",
        verified: chat.verified || false,
        photoUrl: chat.id ? `/api/telegram/photo/${chat.id}` : undefined,
      };
    } catch (err: any) {
      if (err.errorMessage === "INVITE_HASH_EXPIRED") {
        throw new Error("رابط الدعوة منتهي الصلاحية (Invite link expired)");
      }
      if (err.errorMessage === "INVITE_HASH_INVALID") {
        throw new Error("رابط الدعوة غير صالح (Invalid invite link)");
      }
      throw err;
    }
  }

  if (parsed.type === "username") {
    try {
      const entity: any = await client.getEntity(parsed.value);
      const isChannel = entity.broadcast || false;
      const isGroup = entity.megagroup || entity.className === "Chat";
      const isUser = entity.className === "User";

      return {
        type: "public",
        id: entity.id.toString(),
        username: entity.username || parsed.value,
        title: entity.title || entity.firstName || parsed.value,
        name: entity.firstName || entity.title || parsed.value,
        participantsCount: entity.participantsCount || undefined,
        isChannel,
        isGroup,
        isUser,
        verified: entity.verified || false,
        about: entity.about || "حساب أو قناة عامة على تيليجرام",
        photoUrl: `/api/telegram/photo/${entity.id}`,
      };
    } catch (err: any) {
      if (err.errorMessage === "USERNAME_NOT_OCCUPIED") {
        throw new Error(`اسم المستخدم @${parsed.value} غير موجود`);
      }
      throw err;
    }
  }

  throw new Error("الرابط المدخل ليس رابط تيليجرام صالح");
}

export async function joinTelegramChat(params: {
  sessionString: string;
  hash?: string;
  username?: string;
  chatId?: string;
}) {
  const { client } = await getOrCreateClient(params.sessionString);

  if (params.hash) {
    const updates: any = await client.invoke(
      new Api.messages.ImportChatInvite({
        hash: params.hash,
      })
    );

    const chats = updates.chats || [];
    const joinedChat = chats[0];
    return {
      success: true,
      chatId: joinedChat?.id?.toString() || params.hash,
      title: joinedChat?.title || "Joined Chat",
    };
  }

  if (params.username || params.chatId) {
    const peer = params.username || params.chatId;
    const result: any = await client.invoke(
      new Api.channels.JoinChannel({
        channel: peer as any,
      })
    );

    const chats = result.chats || [];
    const joinedChat = chats[0];
    return {
      success: true,
      chatId: joinedChat?.id?.toString() || peer,
      title: joinedChat?.title || params.username || "Joined Channel",
    };
  }

  throw new Error("بيانات الانضمام غير مكتملة");
}

export async function leaveTelegramChat(params: {
  sessionString: string;
  chatId: string;
}) {
  const { client } = await getOrCreateClient(params.sessionString);

  let peer: any = params.chatId;
  if (!isNaN(Number(params.chatId))) {
    peer = parseInt(params.chatId, 10);
  }

  try {
    await client.invoke(
      new Api.channels.LeaveChannel({
        channel: peer,
      })
    );
    return { success: true };
  } catch {
    try {
      await (client as any).deleteDialog(peer);
    } catch {}
    return { success: true };
  }
}

export async function editTelegramMessage(params: {
  sessionString: string;
  chatId: string;
  messageId: number;
  newText: string;
}) {
  const { client } = await getOrCreateClient(params.sessionString);

  let peer: any = params.chatId;
  if (!isNaN(Number(params.chatId))) {
    peer = parseInt(params.chatId, 10);
  }

  const result = await client.editMessage(peer, {
    message: params.messageId,
    text: params.newText,
  });

  return {
    id: params.messageId,
    chatId: params.chatId,
    text: params.newText,
    date: (result as any)?.date || Math.floor(Date.now() / 1000),
  };
}

const SERVER_KNOWN_PROTECTED_BOTS = [
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

function sanitizeServerText(text: string, mode: "clean" | "full" = "clean"): string {
  if (!text) return text;
  let sanitized = text;
  sanitized = sanitized.replace(/(https?:\/\/)?(www\.)?(t\.me|telegram\.me|telegram\.dog)\/[a-zA-Z0-9_+/]+/gi, "");
  sanitized = sanitized.replace(/(https?:\/\/)?(www\.)?(wa\.me|chat\.whatsapp\.com|api\.whatsapp\.com)\/[a-zA-Z0-9_+/-]+/gi, "");
  sanitized = sanitized.replace(/https?:\/\/[^\s]+/gi, "");
  sanitized = sanitized.replace(/(?:\+?966|05|00966)[0-9]{8}/g, "");
  sanitized = sanitized.replace(/(?:\+?20|01|0020)[0-9]{9}/g, "");
  sanitized = sanitized.replace(/\+?[0-9]{10,14}/g, "");

  if (mode === "full") {
    const promoKeywords = ["للتواصل", "واتساب", "خصم", "عرض خاص", "سعر مغري", "تخفيضات", "اشترك الآن", "رابط القناة"];
    promoKeywords.forEach((kw) => {
      sanitized = sanitized.split(kw).join("");
    });
  }
  return sanitized.replace(/\s+/g, " ").trim();
}

async function isChatProtectedServer(client: TelegramClient, peer: any): Promise<boolean> {
  try {
    const participants: any = await client.getParticipants(peer, { limit: 50 });
    return participants.some((p: any) => {
      const username = (p.username || "").toLowerCase();
      const firstName = (p.firstName || "").toLowerCase();
      return (
        p.bot &&
        (SERVER_KNOWN_PROTECTED_BOTS.some((b) => username.includes(b) || b.includes(username)) ||
          username.includes("guard") ||
          username.includes("shield") ||
          username.includes("protect") ||
          firstName.includes("guard") ||
          firstName.includes("حماية"))
      );
    });
  } catch {
    return false;
  }
}

export async function broadcastTelegramMessage(params: {
  sessionString: string;
  chatIds: string[];
  message: string;
  delayMs?: number;
  protectedGroupAction?: "salam" | "skip" | "smart" | "always" | "off";
  smartSalamWaitMinutes?: number;
  smartSalamRequiredMessages?: number;
}) {
  const { client } = await getOrCreateClient(params.sessionString);
  const action = params.protectedGroupAction || "salam";
  const delay = params.delayMs || 3000;
  const results: any[] = [];

  let sent = 0;
  let skipped = 0;
  let failed = 0;

  for (const chatId of params.chatIds) {
    let peer: any = chatId;
    if (!isNaN(Number(chatId))) {
      peer = parseInt(chatId, 10);
    }

    try {
      let isProtected = false;
      if (action !== "off" && action !== "always") {
        isProtected = await isChatProtectedServer(client, peer);
      }

      if (isProtected && action === "skip") {
        skipped++;
        results.push({
          chatId,
          status: "skipped",
          message: "تم تخطي المجموعة لأنها محمية ببوت حماية",
        });
        continue;
      }

      let messageToSend = params.message;
      if (action === "always" || (isProtected && action === "smart")) {
        messageToSend = sanitizeServerText(params.message, "clean");
      }

      if (isProtected && action === "salam") {
        // Smart Salam Mode: Send initial greeting
        const initialMsg: any = await client.sendMessage(peer, { message: "السلام عليكم" });
        sent++;
        results.push({
          chatId,
          status: "salam_initiated",
          messageId: initialMsg.id,
          message: "تم إرسال رسالة السلام المبدئية بنجاح، وستتم المتابعة والتعديل الذكي",
        });
      } else {
        const sentMsg: any = await client.sendMessage(peer, { message: messageToSend });
        sent++;
        results.push({
          chatId,
          status: "success",
          messageId: sentMsg.id,
          message: "تم الإرسال بنجاح",
        });
      }

      // Delay between sends
      if (delay > 0) {
        await new Promise((resolve) => setTimeout(resolve, delay));
      }
    } catch (err: any) {
      failed++;
      results.push({
        chatId,
        status: "failed",
        error: err.message || "فشل الإرسال",
      });
    }
  }

  return {
    total: params.chatIds.length,
    sent,
    skipped,
    failed,
    results,
  };
}

export async function scanTelegramChannels(params: {
  sessionString: string;
  channelIds: string[];
  keywords: string[];
  limitPerChannel?: number;
}) {
  const { client } = await getOrCreateClient(params.sessionString);
  const limit = params.limitPerChannel || 15;
  const matches: any[] = [];

  for (const chatId of params.channelIds) {
    let peer: any = chatId;
    if (!isNaN(Number(chatId))) {
      peer = parseInt(chatId, 10);
    }

    try {
      const messages: any = await client.getMessages(peer, { limit });
      for (const m of messages) {
        if (!m.message || m.out) continue;
        const msgText = m.message.toLowerCase();
        const matched = params.keywords.filter((kw) => msgText.includes(kw.toLowerCase()));

        if (matched.length > 0) {
          matches.push({
            id: `match_${chatId}_${m.id}`,
            chatId,
            chatTitle: m.sender?.firstName || m.sender?.title || chatId,
            senderName: m.sender?.firstName || "مستخدم",
            text: m.message,
            matchedKeywords: matched,
            timestamp: m.date || Math.floor(Date.now() / 1000),
            messageId: m.id,
          });
        }
      }
    } catch {
      // Continue next chat on permission or access restriction
    }
  }

  return { matches };
}


