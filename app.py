import os
import json
import time
import logging
import asyncio
import threading
import re
import uuid
from threading import Lock
from datetime import datetime
from functools import wraps
from flask import Flask, session, request, jsonify, render_template, send_from_directory
from flask_socketio import SocketIO, emit, join_room, leave_room
from telethon import events, TelegramClient, errors
from telethon.tl.functions.messages import ImportChatInviteRequest
from telethon.tl.types import InputPeerUser, InputPeerChat, InputPeerChannel

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

app = Flask(__name__)
app.secret_key = os.environ.get("SESSION_SECRET", "telegram_secret_2024")
app.config['PERMANENT_SESSION_LIFETIME'] = 3600 * 24 * 30
app.config['MAX_CONTENT_LENGTH'] = 50 * 1024 * 1024

ADMIN_USERNAME = "admin"
ADMIN_PASSWORD = "772997043anwer"

socketio = SocketIO(
    app,
    cors_allowed_origins="*",
    async_mode='threading',
    ping_timeout=60,
    ping_interval=25,
    logger=False,
    engineio_logger=False,
    allow_upgrades=False,
    transports=['polling']
)

SESSIONS_DIR = "sessions"
UPLOADS_DIR = "static/uploads"
os.makedirs(SESSIONS_DIR, exist_ok=True)
os.makedirs(UPLOADS_DIR, exist_ok=True)

API_ID = 22043994
API_HASH = '56f64582b363d367280db96586b97801'
DATA_FILE = "academic_knowledge.json"

# ========== دوال استخراج الروابط والكلمات ==========
def parse_entities(raw_text):
    entities = []
    found_raw = set()
    def add(val):
        k = val.lower().lstrip('@')
        if k not in found_raw and len(k) >= 4:
            found_raw.add(k)
            entities.append(val)
    for m in re.findall(r'https?://t\.me/\+([A-Za-z0-9_-]+)', raw_text):
        add(f"+{m}")
    for m in re.findall(r'https?://t\.me/joinchat/([A-Za-z0-9_-]+)', raw_text):
        add(m)
    for m in re.findall(r'https?://t\.me/([A-Za-z][A-Za-z0-9_]{3,})', raw_text):
        add(m)
    for m in re.findall(r'(?<![/\w@])t\.me/\+?([A-Za-z0-9_-]{4,})', raw_text):
        add(m)
    for m in re.findall(r'@([A-Za-z0-9_]{5,})', raw_text):
        add(m)
    for m in re.findall(r'(?<!\d)(-100\d{9,})(?!\d)', raw_text):
        add(m)
    if not entities:
        for part in re.split(r'[\n,،\s|؛؛/\\]+', raw_text):
            p = part.strip().lstrip('@')
            if p and len(p) >= 5 and not re.search(r'[أ-ي]', p) and re.match(r'^[A-Za-z0-9_+-]+$', p):
                add(p)
    return entities

def parse_keywords(raw_text):
    seen = set()
    kws = []
    for kw in re.split(r'[\n,،|؛؛]+', raw_text):
        kw = kw.strip()
        if kw and kw.lower() not in seen:
            seen.add(kw.lower())
            kws.append(kw)
    return kws

PREDEFINED_USERS = {
    "user_1": {"id": "user_1", "name": "المستخدم الأول", "icon": "fas fa-user", "color": "#5865f2"},
    "user_2": {"id": "user_2", "name": "المستخدم الثاني", "icon": "fas fa-user-tie", "color": "#3ba55c"},
    "user_3": {"id": "user_3", "name": "المستخدم الثالث", "icon": "fas fa-user-graduate", "color": "#faa81a"},
    "user_4": {"id": "user_4", "name": "المستخدم الرابع", "icon": "fas fa-user-cog", "color": "#ed4245"},
    "user_5": {"id": "user_5", "name": "المستخدم الخامس", "icon": "fas fa-user-astronaut", "color": "#6f42c1"},
}

USERS = {}
USERS_LOCK = Lock()

def save_settings(user_id, settings):
    try:
        path = os.path.join(SESSIONS_DIR, f"{user_id}.json")
        with open(path, "w", encoding="utf-8") as f:
            json.dump(settings, f, ensure_ascii=False, indent=2)
        return True
    except Exception as e:
        logger.error(f"Save settings error: {e}")
        return False

def load_settings(user_id):
    try:
        path = os.path.join(SESSIONS_DIR, f"{user_id}.json")
        if os.path.exists(path):
            with open(path, "r", encoding="utf-8") as f:
                return json.load(f)
    except Exception as e:
        logger.error(f"Load settings error: {e}")
    return {}

# ========== البوت التعليمي ==========
class LearningBot:
    def __init__(self, user_id=None):
        self.user_id = user_id
        self.client = None
        self.is_monitoring = False
        self.reply_in_groups = False
        self.knowledge = self.load_knowledge()
        self.unknown_requests = []
        self.conversations = {}

    def load_knowledge(self):
        if os.path.exists(DATA_FILE):
            try:
                with open(DATA_FILE, "r", encoding="utf-8") as f:
                    return json.load(f)
            except:
                pass
        return {
            "حل واجب": {
                "description": "حل الواجبات والمسائل الدراسية",
                "questions": ["وش المادة؟", "كم سؤال؟", "متى تحتاجه؟"],
                "intent_keywords": ["حل", "واجب", "مسألة", "سؤال", "تمارين"]
            },
            "بحث": {
                "description": "إعداد البحوث الأكاديمية",
                "questions": ["وش موضوع البحث؟", "كم صفحة؟", "تريد مراجع؟"],
                "intent_keywords": ["بحث", "تقرير", "مشروع", "دراسة"]
            },
            "تلخيص": {
                "description": "تلخيص الكتب والمحاضرات",
                "questions": ["وش المحتوى؟", "كم صفحة؟", "ملخص مفصل ولا مختصر؟"],
                "intent_keywords": ["تلخيص", "ملخص", "اختصار"]
            },
            "ترجمة": {
                "description": "ترجمة النصوص",
                "questions": ["اللغة المصدر؟", "كم كلمة؟", "أكاديمية ولا عادية؟"],
                "intent_keywords": ["ترجمة", "ترجم", "نقل"]
            }
        }

    def save_knowledge(self):
        with open(DATA_FILE, "w", encoding="utf-8") as f:
            json.dump(self.knowledge, f, ensure_ascii=False, indent=4)

    async def start_with_client(self, client):
        self.client = client
        self.register_handlers()
        logger.info(f"LearningBot started for {self.user_id}")

    def register_handlers(self):
        @self.client.on(events.NewMessage)
        async def handler(event):
            if not self.is_monitoring:
                return
            await self.handle_message(event)

    def detect_service(self, text):
        text_low = text.lower()
        best_match = None
        best_score = 0
        for service, data in self.knowledge.items():
            for kw in data.get("intent_keywords", []):
                if kw in text_low:
                    score = len(kw)
                    if score > best_score:
                        best_score = score
                        best_match = service
        return best_match

    async def handle_message(self, event):
        try:
            text = event.message.text
            if not text or len(text) < 5:
                return
            sender = await event.get_sender()
            sender_id = sender.id
            sender_name = getattr(sender, 'first_name', '') or getattr(sender, 'username', '') or 'عزيزي'
            is_group = event.is_group
            chat_id = event.chat_id
            if is_group:
                conv_key = f"group_{chat_id}_{sender_id}"
            else:
                conv_key = f"private_{sender_id}"
            if conv_key in self.conversations:
                await self.handle_active_conversation(event, sender, conv_key, text, sender_name)
                return
            if is_group and not self.reply_in_groups:
                return
            service = self.detect_service(text)
            if service:
                await self.start_service_conversation(event, sender, conv_key, service, sender_name)
            else:
                if any(kw in text.lower() for kw in ["محتاج", "ابي", "اريد", "مساعدة", "يساعد"]):
                    self.unknown_requests.append({
                        "raw_text": text,
                        "suggested_name": text[:50],
                        "time": datetime.now().strftime("%Y-%m-%d %H:%M"),
                        "chat_id": chat_id,
                        "sender_name": sender_name
                    })
                    socketio.emit('new_unknown', self.unknown_requests[-1], to=self.user_id)
        except Exception as e:
            logger.error(f"LearningBot error: {e}")

    async def start_service_conversation(self, event, sender, conv_key, service, sender_name):
        intro_msg = self.get_intro_message(service, sender_name)
        await event.reply(intro_msg)
        data = self.knowledge[service]
        self.conversations[conv_key] = {
            "service": service,
            "step": -1,
            "details": {},
            "questions": data["questions"],
            "sender_name": sender_name,
            "service_type": None
        }
        socketio.emit('log_update', {"message": f"Bot: started {service} with {sender_name}"}, to=self.user_id)

    def get_intro_message(self, service, sender_name):
        if service == "حل واجب":
            return f"ابشر وش عندك اخوي بساعدك فية. واجب ولا اختبار؟"
        elif service == "بحث":
            return f"هلا اخوي وش بحثك ابشر بسوية لك"
        elif service == "تلخيص":
            return f"اهلا وش المحتوى اللي تبي تلخيصه؟"
        elif service == "ترجمة":
            return f"مرحبًا وش النص اللي تبي ترجمته؟"
        else:
            return f"ابشر وش تحتاج؟ خبرني التفاصيل"

    async def handle_active_conversation(self, event, sender, conv_key, text, sender_name):
        conv = self.conversations[conv_key]
        step = conv["step"]
        service = conv["service"]
        questions = conv["questions"]
        if step == -1:
            text_low = text.lower()
            if "واجب" in text_low:
                service_type = "واجب"
                await event.reply("ابشر ارسل واجبك وابشر ما يهمك")
            elif "اختبار" in text_low or "كويز" in text_low:
                service_type = "اختبار"
                await event.reply("متى اختبارك؟")
            elif "بحث" in text_low or "مشروع" in text_low:
                service_type = "بحث"
                await event.reply("ابشر وش بحثك؟ ارسل العنوان والتفاصيل")
            elif "تلخيص" in text_low:
                service_type = "تلخيص"
                await event.reply("ارسل النص اللي تبي تلخيصه")
            else:
                service_type = "خدمة"
                await event.reply("شنو التفاصيل بالضبط؟")
            conv["service_type"] = service_type
            conv["step"] = 0
            return
        if step < len(questions):
            conv["details"][f"q{step+1}"] = text
            conv["step"] = step + 1
            if step + 1 < len(questions):
                next_q = questions[step + 1]
                await event.reply(next_q)
            else:
                details_text = "\n".join([f"- {q}: {a}" for q, a in zip(questions, conv["details"].values())])
                final_msg = f"ممتاز {sender_name} ✅\n\nتم استلام طلبك بخصوص {conv['service_type']}.\nالتفاصيل:\n{details_text}\n\nراح أتواصل معاك قريباً عشان نكمل. إذا عندك إضافة خبرني 😊"
                await event.reply(final_msg)
                del self.conversations[conv_key]
                socketio.emit('log_update', {"message": f"Bot: completed {service} from {sender_name}"}, to=self.user_id)

    async def handle_alert(self, alert, sender_id, sender_name, chat_title, keyword):
        if not self.is_monitoring:
            return
        try:
            msg = f"ابشر وش تحتاج؟ {keyword} عندي خدمة تساعدك"
            await self.client.send_message(sender_id, msg)
            socketio.emit('log_update', {"message": f"Bot: contacted {sender_name} for {keyword}"}, to=self.user_id)
        except Exception as e:
            logger.error(f"Alert error: {e}")

    def get_unknown_requests(self):
        return self.unknown_requests

    def clear_unknown(self):
        self.unknown_requests = []

    def add_service(self, name, desc, questions=None, keywords=None):
        if name and desc:
            self.knowledge[name] = {
                "description": desc,
                "questions": questions or ["شنو التفاصيل؟", "متى تحتاجه؟"],
                "intent_keywords": keywords or [name]
            }
            self.save_knowledge()
            return True
        return False

    def delete_service(self, name):
        if name in self.knowledge:
            del self.knowledge[name]
            self.save_knowledge()
            return True
        return False

    def get_services(self):
        return self.knowledge

    def toggle_reply_in_groups(self):
        self.reply_in_groups = not self.reply_in_groups
        return self.reply_in_groups

learning_bots = {}
LEARNING_LOCK = Lock()

def get_learning_bot(user_id):
    with LEARNING_LOCK:
        if user_id not in learning_bots:
            learning_bots[user_id] = LearningBot(user_id)
        return learning_bots[user_id]

# ========== UserData و TelegramClientManager ==========
class UserData:
    def __init__(self, user_id):
        self.user_id = user_id
        self.client_manager = None
        self.settings = {}
        self.stats = {"sent": 0, "errors": 0, "alerts": 0, "replies": 0}
        self.connected = False
        self.authenticated = False
        self.awaiting_code = False
        self.awaiting_password = False
        self.phone_code_hash = None
        self.monitoring_active = False
        self.is_running = False
        self.thread = None
        self.phone_number = None
        self.auto_replies = []
        self.telegram_name = None
        self.sent_batches = []
        self.pending_auto_code = None
        self.last_seen = None
        self.blocked = False
        self.disabled = False
        self.alerts = []
        self.scheduled_active = False
        self.scheduled_interval = 0
        self.scheduled_groups = []
        self.scheduled_message = ""
        self.scheduled_image = None

    def to_dict(self):
        slot = self.user_id.split('__', 1)[1] if '__' in self.user_id else self.user_id
        return {
            "user_id": self.user_id,
            "name": PREDEFINED_USERS.get(slot, {}).get("name", slot),
            "phone": self.phone_number,
            "authenticated": self.authenticated,
            "last_seen": self.last_seen.isoformat() if self.last_seen else None,
            "blocked": self.blocked,
            "disabled": self.disabled,
            "groups": self.settings.get("groups", []),
            "watch_words": self.settings.get("watch_words", []),
            "auto_replies": self.auto_replies,
            "alerts_count": len(self.alerts),
            "monitoring_active": self.monitoring_active,
            "scheduled_active": self.scheduled_active,
        }

class TelegramClientManager:
    def __init__(self, user_id):
        self.user_id = user_id
        self.client = None
        self.loop = None
        self.thread = None
        self.stop_flag = threading.Event()
        self.is_ready = threading.Event()
        self.event_handlers_registered = False
        self.scheduled_thread = None
        self.scheduled_stop = threading.Event()
        self.keep_alive = True
        self.learning_bot = None

    def start_client_thread(self):
        if self.thread and self.thread.is_alive():
            if self.is_ready.is_set() and self.client and self.client.is_connected():
                return True
            else:
                logger.warning(f"Client thread for {self.user_id} alive but not ready, stopping it")
                self.stop()
                self.thread.join(timeout=3)
                self.thread = None
        self.stop_flag.clear()
        self.is_ready.clear()
        self.keep_alive = True
        self.thread = threading.Thread(target=self._run_client_loop, daemon=True)
        self.thread.start()
        ready = self.is_ready.wait(timeout=30)
        if not ready:
            logger.error(f"Client thread for {self.user_id} did not become ready within 30 seconds")
        return ready

    def _run_client_loop(self):
        try:
            self.loop = asyncio.new_event_loop()
            asyncio.set_event_loop(self.loop)
            session_file = os.path.join(SESSIONS_DIR, f"{self.user_id}_session")
            if API_ID and API_HASH:
                self.client = TelegramClient(session_file, int(API_ID), API_HASH, loop=self.loop)
            else:
                logger.error("API_ID or API_HASH not set")
                self.is_ready.set()
                return
            self.loop.run_until_complete(self._client_main())
        except Exception as e:
            logger.error(f"Client thread error for {self.user_id}: {e}")
            self.is_ready.set()
        finally:
            if self.loop and not self.loop.is_closed():
                self.loop.close()

    async def _client_main(self):
        try:
            await self.client.connect()
            self.is_ready.set()
            logger.info(f"Client for {self.user_id} connected")
            await self._register_event_handlers()
            if await self.client.is_user_authorized():
                with USERS_LOCK:
                    ud = USERS.get(self.user_id)
                    if ud:
                        ud.authenticated = True
                        ud.connected = True
                        if ud.settings.get('monitoring_active'):
                            ud.monitoring_active = True
                            ud.is_running = True
                        if ud.settings.get('scheduled_active'):
                            ud.scheduled_active = True
                            ud.scheduled_interval = ud.settings.get('scheduled_interval', 0)
                            ud.scheduled_groups = ud.settings.get('scheduled_groups', [])
                            ud.scheduled_message = ud.settings.get('scheduled_message', '')
                            ud.scheduled_image = ud.settings.get('scheduled_image')
                            if ud.scheduled_active and ud.scheduled_interval > 0 and ud.scheduled_groups:
                                self.start_scheduled(
                                    ud.scheduled_groups,
                                    ud.scheduled_message,
                                    ud.scheduled_image,
                                    ud.scheduled_interval
                                )
                try:
                    me = await self.client.get_me()
                    if me:
                        tg_name = (getattr(me, 'first_name', '') or '') + (' ' + (getattr(me, 'last_name', '') or '')).rstrip()
                        tg_name = tg_name.strip()
                        with USERS_LOCK:
                            ud2 = USERS.get(self.user_id)
                            if ud2 and tg_name:
                                ud2.telegram_name = tg_name
                        socketio.emit('telegram_name_update', {'name': tg_name}, to=self.user_id)
                except Exception as me_err:
                    logger.warning(f"Could not get 'me' for {self.user_id}: {me_err}")
                bot = get_learning_bot(self.user_id)
                if bot.is_monitoring:
                    await bot.start_with_client(self.client)
                    self.learning_bot = bot
                logger.info(f"✅ {self.user_id} auto-authorized")
            else:
                logger.info(f"Client for {self.user_id} not authorized yet")
            last_ping = time.time()
            while not self.stop_flag.is_set() and self.keep_alive:
                await asyncio.sleep(5)
                try:
                    if time.time() - last_ping > 30:
                        last_ping = time.time()
                        if self.client and self.client.is_connected():
                            await self.client.get_me()
                        else:
                            logger.warning(f"Client {self.user_id} disconnected, reconnecting...")
                            await self.client.connect()
                except Exception as e:
                    logger.error(f"Keep-alive error for {self.user_id}: {e}")
                    try:
                        await self.client.connect()
                    except:
                        pass
        except Exception as e:
            logger.error(f"Client main error for {self.user_id}: {e}")
        finally:
            if self.client:
                try:
                    await self.client.disconnect()
                except:
                    pass

    async def _handle_session_revoked(self):
        logger.info(f"🔴 Session revoked for {self.user_id}")
        with USERS_LOCK:
            ud = USERS.get(self.user_id)
            if ud:
                ud.authenticated = False
                ud.connected = False
                ud.awaiting_code = False
                ud.awaiting_password = False
                ud.monitoring_active = False
                ud.is_running = False
                ud.scheduled_active = False
        for suffix in ['_session', '_session.session']:
            path = os.path.join(SESSIONS_DIR, f"{self.user_id}{suffix}")
            if os.path.exists(path):
                try:
                    os.remove(path)
                except:
                    pass
        settings = load_settings(self.user_id)
        settings.pop('phone', None)
        save_settings(self.user_id, settings)
        socketio.emit('session_revoked', {"message": "⚠️ تم إلغاء الجلسة من تيليجرام - يرجى تسجيل الدخول مجدداً"}, to=self.user_id)
        socketio.emit('log_update', {"message": "🔴 الجلسة أُلغيت من تيليجرام - تم قطع الاتصال تلقائياً"}, to=self.user_id)
        self.stop_flag.set()

    async def _start_code_listener(self):
        try:
            from telethon.tl.types import UpdateServiceNotification
            code_found = asyncio.Event()
            CODE_PATTERN = re.compile(r'\b(\d{5,6})\b')
            def _emit_code(code):
                code_found.set()
                socketio.emit('auto_code', {'code': code}, to=self.user_id)
                socketio.emit('log_update', {'message': f'📩 تم استلام كود التحقق ({code}) تلقائياً'}, to=self.user_id)
            @self.client.on(events.Raw(UpdateServiceNotification))
            async def service_notif_handler(update):
                if code_found.is_set():
                    return
                text = getattr(update, 'message', '') or ''
                match = CODE_PATTERN.search(text)
                if match:
                    _emit_code(match.group(1))
            @self.client.on(events.NewMessage(from_users=777000))
            async def telegram_svc_handler(event):
                if code_found.is_set():
                    return
                text = event.message.message or ''
                match = CODE_PATTERN.search(text)
                if match:
                    _emit_code(match.group(1))
            await asyncio.wait_for(code_found.wait(), timeout=120)
        except asyncio.TimeoutError:
            pass
        except Exception as e:
            logger.error(f"Code listener error for {self.user_id}: {e}")

    async def _register_event_handlers(self):
        if self.event_handlers_registered:
            return
        try:
            @self.client.on(events.NewMessage)
            async def handler(event):
                await self._handle_message(event)
            self.event_handlers_registered = True
            logger.info(f"✅ Event handlers registered for {self.user_id}")
        except Exception as e:
            logger.error(f"Register handlers error for {self.user_id}: {e}")

    async def _handle_message(self, event):
        try:
            if not event.message.text:
                return
            chat = await event.get_chat()
            chat_title = getattr(chat, 'title', None) or getattr(chat, 'first_name', 'مستخدم')
            chat_username = getattr(chat, 'username', None)
            chat_id = getattr(chat, 'id', None)
            if chat_username:
                group_link = f"https://t.me/{chat_username}"
            elif chat_id:
                group_link = f"https://t.me/c/{str(chat_id).lstrip('-100')}"
            else:
                group_link = None
            with USERS_LOCK:
                ud = USERS.get(self.user_id)
                if not ud:
                    return
                monitoring = ud.monitoring_active
                auto_replies = list(ud.auto_replies or [])
                current_settings = dict(ud.settings)
            msg_text = event.message.text
            msg_lower = msg_text.lower()
            msg_date = event.message.date
            if msg_date:
                try:
                    msg_time_str = msg_date.astimezone().strftime('%Y-%m-%d %H:%M:%S')
                except:
                    msg_time_str = msg_date.strftime('%Y-%m-%d %H:%M:%S')
            else:
                msg_time_str = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
            # المراقبة
            if monitoring:
                watch_words = current_settings.get('watch_words', [])
                if not watch_words:
                    fresh = load_settings(self.user_id)
                    watch_words = fresh.get('watch_words', [])
                msg_normalized = ' '.join(msg_text.split()).lower()
                for kw in watch_words:
                    kw_clean = ' '.join(kw.split()).lower() if kw else ''
                    if kw_clean and (kw_clean in msg_normalized or kw_clean in msg_lower):
                        sender = await event.get_sender()
                        sender_id = getattr(sender, 'id', None)
                        sender_first = getattr(sender, 'first_name', '') or ''
                        sender_last = getattr(sender, 'last_name', '') or ''
                        sender_username = getattr(sender, 'username', None)
                        sender_name = (f"{sender_first} {sender_last}".strip() or sender_username or str(sender_id) or 'غير معروف')
                        if sender_username:
                            sender_link = f"https://t.me/{sender_username}"
                        elif sender_id:
                            sender_link = f"tg://user?id={sender_id}"
                        else:
                            sender_link = None
                        alert = {
                            "keyword": kw,
                            "group": chat_title,
                            "group_link": group_link,
                            "group_username": chat_username,
                            "group_id": chat_id,
                            "message": msg_text[:500],
                            "full_message": msg_text,
                            "sender": sender_name,
                            "sender_id": sender_id,
                            "sender_username": sender_username,
                            "sender_link": sender_link,
                            "timestamp": datetime.now().strftime('%H:%M:%S'),
                            "message_time": msg_time_str,
                            "message_id": event.message.id
                        }
                        with USERS_LOCK:
                            ud2 = USERS.get(self.user_id)
                            if ud2:
                                ud2.stats['alerts'] = ud2.stats.get('alerts', 0) + 1
                                ud2.alerts.insert(0, alert)
                                if len(ud2.alerts) > 100:
                                    ud2.alerts.pop()
                                socketio.emit('stats_update', dict(ud2.stats), to=self.user_id)
                        socketio.emit('new_alert', alert, to=self.user_id)
                        socketio.emit('log_update', {"message": f"🚨 تنبيه: '{kw}' في [{chat_title}] من [{sender_name}]"}, to=self.user_id)
                        if self.learning_bot and self.learning_bot.is_monitoring:
                            await self.learning_bot.handle_alert(alert, sender_id, sender_name, chat_title, kw)
                        try:
                            sender_ref = f"@{sender_username}" if sender_username else sender_name
                            group_ref = f"@{chat_username}" if chat_username else chat_title
                            notif = (f"🚨 تنبيه كلمة: {kw}\n📍 المجموعة: {group_ref}\n👤 المرسل: {sender_ref}\n⏰ الوقت: {msg_time_str}\n💬 الرسالة:\n{msg_text[:300]}")
                            await self.client.send_message('me', notif[:4000])
                        except:
                            pass
            # الرد التلقائي
            fresh_rules = load_settings(self.user_id)
            live_auto_replies = fresh_rules.get('auto_replies', [])
            if not live_auto_replies:
                live_auto_replies = auto_replies
            for rule in live_auto_replies:
                kw = (rule.get('keyword', '') or '').strip()
                reply_text = (rule.get('reply', '') or '').strip()
                if not kw or not reply_text:
                    continue
                kw_clean = ' '.join(kw.split()).lower()
                msg_norm = ' '.join(msg_text.split()).lower()
                if kw_clean in msg_norm or kw_clean in msg_lower:
                    try:
                        MAX_TG = 4096
                        if len(reply_text) <= MAX_TG:
                            await event.message.reply(reply_text)
                        else:
                            await event.message.reply(reply_text[:MAX_TG])
                            for chunk_start in range(MAX_TG, len(reply_text), MAX_TG):
                                await asyncio.sleep(0.5)
                                await self.client.send_message(await event.get_chat(), reply_text[chunk_start:chunk_start+MAX_TG])
                        with USERS_LOCK:
                            ud2 = USERS.get(self.user_id)
                            if ud2:
                                ud2.stats['replies'] = ud2.stats.get('replies', 0) + 1
                                socketio.emit('stats_update', dict(ud2.stats), to=self.user_id)
                        socketio.emit('auto_reply_event', {"chat": chat_title, "original_msg": msg_text[:300], "reply_msg": reply_text[:300], "keyword": kw, "timestamp": datetime.now().strftime('%H:%M:%S')}, to=self.user_id)
                        socketio.emit('log_update', {"message": f"🤖 رد تلقائي على رسالة في [{chat_title}] | كلمة: '{kw[:30]}'"}, to=self.user_id)
                        break
                    except Exception as e:
                        logger.error(f"Auto-reply send error: {e}")
        except Exception as e:
            logger.error(f"Handle message error: {e}")

    def run_coroutine(self, coro, timeout=30):
        if not self.loop or self.loop.is_closed():
            raise Exception("Event loop not initialized or closed")
        future = asyncio.run_coroutine_threadsafe(coro, self.loop)
        return future.result(timeout=timeout)

    def stop(self):
        self.keep_alive = False
        self.stop_flag.set()
        self.scheduled_stop.set()

    def start_scheduled(self, groups, message, image_path, interval_minutes):
        with USERS_LOCK:
            ud = USERS.get(self.user_id)
            if ud:
                ud.scheduled_active = True
                ud.scheduled_interval = interval_minutes
                ud.scheduled_groups = groups
                ud.scheduled_message = message
                ud.scheduled_image = image_path
                settings = load_settings(self.user_id)
                settings['scheduled_active'] = True
                settings['scheduled_interval'] = interval_minutes
                settings['scheduled_groups'] = groups
                settings['scheduled_message'] = message
                settings['scheduled_image'] = image_path
                save_settings(self.user_id, settings)
        self.scheduled_stop.clear()
        self.scheduled_thread = threading.Thread(target=self._scheduled_worker, args=(groups, message, image_path, interval_minutes), daemon=True)
        self.scheduled_thread.start()

    def stop_scheduled(self):
        with USERS_LOCK:
            ud = USERS.get(self.user_id)
            if ud:
                ud.scheduled_active = False
                settings = load_settings(self.user_id)
                settings['scheduled_active'] = False
                save_settings(self.user_id, settings)
        self.scheduled_stop.set()

    def _scheduled_worker(self, groups, message, image_path, interval_minutes):
        socketio.emit('log_update', {"message": f"📅 بدأ الإرسال المجدول كل {interval_minutes} دقيقة"}, to=self.user_id)
        # إرسال أول دفعة فوراً
        try:
            self.run_coroutine(self._send_to_groups(groups, message, image_path))
        except Exception as e:
            logger.error(f"First scheduled send error: {e}")
            socketio.emit('log_update', {"message": f"⚠️ خطأ في الإرسال الأول: {str(e)[:150]}"}, to=self.user_id)
        while not self.scheduled_stop.is_set():
            for _ in range(interval_minutes * 60):
                if self.scheduled_stop.is_set():
                    break
                time.sleep(1)
            if self.scheduled_stop.is_set():
                break
            try:
                self.run_coroutine(self._send_to_groups(groups, message, image_path))
            except Exception as e:
                error_msg = f"⚠️ خطأ في الإرسال المجدول: {str(e)[:150]}"
                logger.error(f"Scheduled send error: {e}")
                socketio.emit('log_update', {"message": error_msg}, to=self.user_id)
        socketio.emit('log_update', {"message": "⏹ تم إيقاف الإرسال المجدول"}, to=self.user_id)

    async def _send_to_groups(self, groups, message, image_path):
        from telethon import functions
        sent = 0
        errors = 0
        total = len(groups)
        batch_id = str(uuid.uuid4())
        has_media = bool(image_path and os.path.exists(image_path))
        batch_entries = []
        socketio.emit('log_update', {"message": f"📤 بدء الإرسال إلى {total} مجموعة..."}, to=self.user_id)
        for i, group in enumerate(groups):
            try:
                entity_str = group.strip()
                chat = None
                if entity_str.startswith('+') and len(entity_str) > 8:
                    try:
                        result = await self.client(functions.messages.ImportChatInviteRequest(hash=entity_str[1:]))
                        chat = result.chats[0] if hasattr(result, 'chats') and result.chats else None
                    except Exception as je:
                        if 'Already' in str(je) or 'USER_ALREADY' in str(je):
                            async for dialog in self.client.iter_dialogs():
                                if hasattr(dialog.entity, 'username'):
                                    chat = dialog.entity
                                    break
                        else:
                            raise je
                elif entity_str.lstrip('-').isdigit():
                    chat = await self.client.get_entity(int(entity_str))
                else:
                    username = entity_str.lstrip('@')
                    chat = await self.client.get_entity(f"@{username}")
                if chat is None:
                    raise Exception("لم يتم العثور على المجموعة")
                sent_msg = None
                if has_media:
                    sent_msg = await self.client.send_file(chat, image_path, caption=message or "")
                elif message:
                    sent_msg = await self.client.send_message(chat, message)
                else:
                    raise Exception("لا يوجد محتوى للإرسال")
                sent += 1
                chat_name = getattr(chat, 'title', None) or getattr(chat, 'username', entity_str)
                chat_username = getattr(chat, 'username', None)
                chat_id = getattr(chat, 'id', None)
                msg_id = sent_msg.id if sent_msg else None
                if chat_id and msg_id:
                    batch_entries.append({
                        "chat_id": chat_id,
                        "msg_id": msg_id,
                        "chat_title": chat_name,
                        "chat_username": chat_username,
                        "entity_str": entity_str
                    })
                socketio.emit('log_update', {"message": f"✅ [{i+1}/{total}] أُرسل إلى {chat_name}"}, to=self.user_id)
                with USERS_LOCK:
                    ud = USERS.get(self.user_id)
                    if ud:
                        ud.stats['sent'] = ud.stats.get('sent', 0) + 1
                        socketio.emit('stats_update', dict(ud.stats), to=self.user_id)
                await asyncio.sleep(2)
            except Exception as e:
                errors += 1
                socketio.emit('log_update', {"message": f"❌ [{i+1}/{total}] {group}: {str(e)[:80]}"}, to=self.user_id)
                with USERS_LOCK:
                    ud = USERS.get(self.user_id)
                    if ud:
                        ud.stats['errors'] = ud.stats.get('errors', 0) + 1
                        socketio.emit('stats_update', dict(ud.stats), to=self.user_id)
                await asyncio.sleep(1)
        if batch_entries:
            batch_record = {
                "id": batch_id,
                "text": message or "",
                "has_media": has_media,
                "sent_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                "sent_count": sent,
                "entries": batch_entries
            }
            with USERS_LOCK:
                ud = USERS.get(self.user_id)
                if ud:
                    ud.sent_batches.append(batch_record)
            socketio.emit('batch_saved', batch_record, to=self.user_id)
        socketio.emit('log_update', {"message": f"📊 اكتمل الإرسال: ✅ {sent} ناجح  ❌ {errors} فاشل  من أصل {total}"}, to=self.user_id)
        socketio.emit('send_complete', {"sent": sent, "errors": errors, "total": total}, to=self.user_id)

    async def _edit_batch_messages(self, batch_id, new_text):
        with USERS_LOCK:
            ud = USERS.get(self.user_id)
            if not ud:
                return {"ok": False, "msg": "المستخدم غير موجود"}
            batch = next((b for b in ud.sent_batches if b["id"] == batch_id), None)
        if not batch:
            return {"ok": False, "msg": "الدُّفعة غير موجودة"}
        ok_count = 0
        fail_count = 0
        for entry in batch["entries"]:
            try:
                chat_id = entry["chat_id"]
                msg_id = entry["msg_id"]
                await self.client.edit_message(chat_id, msg_id, new_text)
                ok_count += 1
                socketio.emit('log_update', {"message": f"✏️ تم تعديل الرسالة في {entry['chat_title']}"}, to=self.user_id)
                await asyncio.sleep(0.5)
            except Exception as e:
                fail_count += 1
                socketio.emit('log_update', {"message": f"❌ فشل التعديل في {entry.get('chat_title','?')}: {str(e)[:60]}"}, to=self.user_id)
        with USERS_LOCK:
            ud = USERS.get(self.user_id)
            if ud:
                for b in ud.sent_batches:
                    if b["id"] == batch_id:
                        b["text"] = new_text
                        b["edited_at"] = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                        break
        socketio.emit('batch_edited', {"batch_id": batch_id, "new_text": new_text, "ok": ok_count, "fail": fail_count}, to=self.user_id)
        return {"ok": True, "edited": ok_count, "failed": fail_count}

    async def _delete_batch_messages(self, batch_id):
        with USERS_LOCK:
            ud = USERS.get(self.user_id)
            if not ud:
                return {"ok": False, "msg": "المستخدم غير موجود"}
            batch = next((b for b in ud.sent_batches if b["id"] == batch_id), None)
        if not batch:
            return {"ok": False, "msg": "الدُّفعة غير موجودة"}
        ok_count = 0
        fail_count = 0
        for entry in batch["entries"]:
            try:
                chat_id = entry["chat_id"]
                msg_id = entry["msg_id"]
                await self.client.delete_messages(chat_id, [msg_id])
                ok_count += 1
                socketio.emit('log_update', {"message": f"🗑️ تم حذف الرسالة من {entry['chat_title']}"}, to=self.user_id)
                await asyncio.sleep(0.5)
            except Exception as e:
                fail_count += 1
                socketio.emit('log_update', {"message": f"❌ فشل الحذف من {entry.get('chat_title','?')}: {str(e)[:60]}"}, to=self.user_id)
        with USERS_LOCK:
            ud = USERS.get(self.user_id)
            if ud:
                ud.sent_batches = [b for b in ud.sent_batches if b["id"] != batch_id]
        socketio.emit('batch_deleted', {"batch_id": batch_id, "ok": ok_count, "fail": fail_count}, to=self.user_id)
        return {"ok": True, "deleted": ok_count, "failed": fail_count}

    async def _search_messages_async(self, query, search_type, exclude_chats):
        import re
        results = []
        try:
            async for dialog in self.client.iter_dialogs():
                chat_id = dialog.entity.id
                chat_username = getattr(dialog.entity, 'username', None)
                chat_title = dialog.name
                skip = False
                for ex in exclude_chats:
                    ex_clean = ex.strip().lstrip('@')
                    if str(chat_id) == ex_clean or (chat_username and chat_username == ex_clean):
                        skip = True
                        break
                if skip:
                    continue
                limit = 500
                async for msg in self.client.iter_messages(dialog.entity, limit=limit):
                    if not msg.text:
                        continue
                    msg_text = msg.text
                    if search_type == 'text':
                        if query and query.lower() not in msg_text.lower():
                            continue
                        results.append({
                            "message_id": msg.id,
                            "chat_title": chat_title,
                            "chat_link": f"https://t.me/{chat_username}" if chat_username else f"https://t.me/c/{chat_id}",
                            "sender": msg.sender_id,
                            "message_text": msg_text[:1000],
                            "date": msg.date.isoformat() if msg.date else datetime.now().isoformat(),
                            "link": None
                        })
                    elif search_type == 'telegram_links':
                        urls = re.findall(r'https?://(?:t\.me|telegram\.me)/[^\s<>]+', msg_text)
                        if not urls:
                            continue
                        for url in urls:
                            results.append({
                                "message_id": msg.id,
                                "chat_title": chat_title,
                                "chat_link": f"https://t.me/{chat_username}" if chat_username else f"https://t.me/c/{chat_id}",
                                "sender": msg.sender_id,
                                "message_text": msg_text[:500],
                                "date": msg.date.isoformat() if msg.date else datetime.now().isoformat(),
                                "link": url
                            })
                    elif search_type == 'all_links':
                        urls = re.findall(r'https?://[^\s<>]+', msg_text)
                        if not urls:
                            continue
                        for url in urls:
                            results.append({
                                "message_id": msg.id,
                                "chat_title": chat_title,
                                "chat_link": f"https://t.me/{chat_username}" if chat_username else f"https://t.me/c/{chat_id}",
                                "sender": msg.sender_id,
                                "message_text": msg_text[:500],
                                "date": msg.date.isoformat() if msg.date else datetime.now().isoformat(),
                                "link": url
                            })
                    if len(results) >= 1000:
                        return results
        except Exception as e:
            logger.error(f"Search error: {e}")
            raise
        return results

    def search_messages(self, query, search_type, exclude_chats):
        if not self.loop:
            raise Exception("Event loop not initialized")
        future = asyncio.run_coroutine_threadsafe(self._search_messages_async(query, search_type, exclude_chats), self.loop)
        return future.result(timeout=120)

def get_or_create_user(user_id):
    with USERS_LOCK:
        if user_id not in USERS:
            ud = UserData(user_id)
            ud.settings = load_settings(user_id)
            ud.auto_replies = ud.settings.get('auto_replies', [])
            if ud.settings.get('phone'):
                ud.phone_number = ud.settings['phone']
            ud.blocked = ud.settings.get('blocked', False)
            ud.disabled = ud.settings.get('disabled', False)
            ud.alerts = ud.settings.get('alerts', [])
            ud.monitoring_active = ud.settings.get('monitoring_active', False)
            ud.is_running = ud.monitoring_active
            ud.scheduled_active = ud.settings.get('scheduled_active', False)
            ud.scheduled_interval = ud.settings.get('scheduled_interval', 0)
            ud.scheduled_groups = ud.settings.get('scheduled_groups', [])
            ud.scheduled_message = ud.settings.get('scheduled_message', '')
            ud.scheduled_image = ud.settings.get('scheduled_image')
            if ud.settings.get('last_seen'):
                try:
                    ud.last_seen = datetime.fromisoformat(ud.settings['last_seen'])
                except:
                    pass
            USERS[user_id] = ud
        return USERS[user_id]

VALID_SLOTS = list(PREDEFINED_USERS.keys())

def get_visitor_id():
    vid = session.get('visitor_id')
    if not vid:
        vid = str(uuid.uuid4()).replace('-', '')
        session['visitor_id'] = vid
    return vid

def get_current_slot():
    slot = session.get('user_slot', 'user_1')
    if slot not in VALID_SLOTS:
        slot = 'user_1'
        session['user_slot'] = slot
    return slot

def get_current_user_id():
    vid = get_visitor_id()
    slot = get_current_slot()
    return f"{vid}__{slot}"

def get_slot_from_uid(uid):
    if '__' in uid:
        return uid.split('__', 1)[1]
    return uid if uid in VALID_SLOTS else 'user_1'

def update_last_seen(user_id):
    with USERS_LOCK:
        ud = USERS.get(user_id)
        if ud:
            ud.last_seen = datetime.now()
            settings = load_settings(user_id)
            settings['last_seen'] = ud.last_seen.isoformat()
            save_settings(user_id, settings)

def ensure_client_running(uid):
    ud = get_or_create_user(uid)
    if ud.client_manager is None:
        ud.client_manager = TelegramClientManager(uid)
        logger.info(f"Created new client manager for {uid}")
    if not ud.client_manager.start_client_thread():
        logger.error(f"Failed to start client for {uid}")
        return False
    try:
        time.sleep(1)
        is_auth = ud.client_manager.run_coroutine(ud.client_manager.client.is_user_authorized(), timeout=10)
        with USERS_LOCK:
            if is_auth != ud.authenticated:
                ud.authenticated = is_auth
                ud.connected = is_auth
                if is_auth:
                    try:
                        me = ud.client_manager.run_coroutine(ud.client_manager.client.get_me(), timeout=10)
                        if me:
                            tg_name = (getattr(me, 'first_name', '') or '') + (' ' + (getattr(me, 'last_name', '') or '')).rstrip()
                            tg_name = tg_name.strip()
                            ud.telegram_name = tg_name
                            settings = load_settings(uid)
                            settings['telegram_name'] = tg_name
                            save_settings(uid, settings)
                            socketio.emit('telegram_name_update', {'name': tg_name}, to=uid)
                    except Exception as me_err:
                        logger.warning(f"get_me error for {uid}: {me_err}")
        return is_auth
    except Exception as e:
        logger.error(f"Error checking auth for {uid}: {e}")
        return False

@app.after_request
def add_no_cache(response):
    response.headers['Cache-Control'] = 'no-store, no-cache, must-revalidate, max-age=0'
    response.headers['Pragma'] = 'no-cache'
    response.headers['Expires'] = '0'
    return response

@app.route("/manifest.json")
def manifest():
    return send_from_directory('static', 'manifest.json', mimetype='application/manifest+json')

@app.route("/")
def index():
    uid = get_current_user_id()
    slot = get_current_slot()
    get_or_create_user(uid)
    settings = load_settings(uid)
    settings['api_configured'] = bool(API_ID and API_HASH)
    return render_template('index.html',
                           settings=settings,
                           predefined_users=PREDEFINED_USERS,
                           current_user_id=slot,
                           current_user_slot=slot)

# ========== مسارات API الأساسية ==========
@app.route("/api/get_login_status")
def api_get_login_status():
    uid = get_current_user_id()
    ud = get_or_create_user(uid)
    if ud.phone_number and not ud.authenticated:
        ensure_client_running(uid)
        ud = get_or_create_user(uid)
    return jsonify({
        "logged_in": ud.authenticated,
        "connected": ud.connected,
        "awaiting_code": ud.awaiting_code,
        "awaiting_password": ud.awaiting_password,
        "is_running": ud.is_running,
        "monitoring_active": ud.monitoring_active,
        "scheduled_active": ud.scheduled_active,
        "phone": ud.phone_number or "",
        "telegram_name": ud.telegram_name or ""
    })

@app.route("/api/get_stats")
def api_get_stats():
    uid = get_current_user_id()
    ud = get_or_create_user(uid)
    stats = {
        "sent": ud.stats.get("sent", 0),
        "errors": ud.stats.get("errors", 0),
        "alerts": ud.stats.get("alerts", 0),
        "replies": ud.stats.get("replies", 0),
    }
    return jsonify({"success": True, **stats})

@app.route("/api/parse_input", methods=["POST"])
def api_parse_input():
    data = request.json or {}
    raw = data.get('text', '')
    mode = data.get('mode', 'groups')
    if not raw.strip():
        return jsonify({"success": False, "items": [], "count": 0})
    if mode == 'keywords':
        result = parse_keywords(raw)
    else:
        result = parse_entities(raw)
    return jsonify({"success": True, "items": result, "count": len(result)})

@app.route("/api/get_settings")
def api_get_settings():
    uid = get_current_user_id()
    settings = load_settings(uid)
    return jsonify({"success": True, "settings": settings})

@app.route("/api/get_auto_replies")
def api_get_auto_replies():
    uid = get_current_user_id()
    settings = load_settings(uid)
    return jsonify({"success": True, "auto_replies": settings.get('auto_replies', [])})

@app.route("/api/switch_user", methods=["POST"])
def api_switch_user():
    data = request.json or {}
    new_slot = data.get('user_id')
    if not new_slot or new_slot not in VALID_SLOTS:
        return jsonify({"success": False, "message": "مستخدم غير صالح"})
    session['user_slot'] = new_slot
    session.permanent = True
    new_uid = get_current_user_id()
    ud = get_or_create_user(new_uid)
    if ud.phone_number:
        ensure_client_running(new_uid)
        ud = get_or_create_user(new_uid)
    settings = load_settings(new_uid)
    tg_name = ud.telegram_name or settings.get('telegram_name') or ''
    return jsonify({
        "success": True,
        "message": f"✅ تم التبديل إلى {tg_name or PREDEFINED_USERS[new_slot]['name']}",
        "settings": settings,
        "logged_in": ud.authenticated,
        "awaiting_code": ud.awaiting_code,
        "awaiting_password": ud.awaiting_password,
        "is_running": ud.is_running,
        "telegram_name": tg_name
    })

@app.route("/api/save_login", methods=["POST"])
def api_save_login():
    uid = get_current_user_id()
    data = request.json or {}
    phone = data.get('phone', '').strip()
    if not phone:
        return jsonify({"success": False, "message": "أدخل رقم الهاتف"})
    if not API_ID or not API_HASH:
        return jsonify({"success": False, "message": "⚠️ TELEGRAM_API_ID و TELEGRAM_API_HASH غير محددة"})
    try:
        from telethon.errors import FloodWaitError
        ud = get_or_create_user(uid)
        socketio.emit('log_update', {"message": "🔄 جارٍ إعداد الاتصال..."}, to=uid)
        if not ud.client_manager:
            ud.client_manager = TelegramClientManager(uid)
        if not ud.client_manager.start_client_thread():
            return jsonify({"success": False, "message": "❌ فشل في تشغيل العميل"})
        is_auth = ud.client_manager.run_coroutine(ud.client_manager.client.is_user_authorized())
        if is_auth:
            with USERS_LOCK:
                ud.authenticated = True
                ud.connected = True
                ud.phone_number = phone
            settings = load_settings(uid)
            settings['phone'] = phone
            save_settings(uid, settings)
            try:
                me = ud.client_manager.run_coroutine(ud.client_manager.client.get_me())
                if me:
                    tg_name = (getattr(me, 'first_name', '') or '') + (' ' + (getattr(me, 'last_name', '') or '')).rstrip()
                    tg_name = tg_name.strip()
                    with USERS_LOCK:
                        ud.telegram_name = tg_name
                    settings['telegram_name'] = tg_name
                    save_settings(uid, settings)
                    socketio.emit('telegram_name_update', {'name': tg_name}, to=uid)
            except Exception as me_err:
                logger.warning(f"get_me error: {me_err}")
            socketio.emit('log_update', {"message": "✅ تم الدخول تلقائياً (جلسة محفوظة)"}, to=uid)
            return jsonify({"success": True, "message": "✅ أنت مسجل دخول بالفعل", "status": "already_authorized"})
        socketio.emit('log_update', {"message": f"📱 إرسال كود إلى {phone}..."}, to=uid)
        try:
            sent = ud.client_manager.run_coroutine(ud.client_manager.client.send_code_request(phone))
        except FloodWaitError as e:
            return jsonify({"success": False, "message": f"⏳ انتظر {e.seconds} ثانية"})
        with USERS_LOCK:
            ud.awaiting_code = True
            ud.phone_code_hash = sent.phone_code_hash
            ud.phone_number = phone
            ud.connected = True
        settings = load_settings(uid)
        settings['phone'] = phone
        save_settings(uid, settings)
        socketio.emit('log_update', {"message": "📱 تم إرسال كود التحقق"}, to=uid)
        try:
            asyncio.run_coroutine_threadsafe(ud.client_manager._start_code_listener(), ud.client_manager.loop)
        except Exception as cl_err:
            logger.warning(f"Code listener start error: {cl_err}")
        return jsonify({"success": True, "message": "📱 تم إرسال كود التحقق إلى هاتفك", "status": "code_sent"})
    except Exception as e:
        logger.error(f"Login error: {e}")
        return jsonify({"success": False, "message": f"❌ خطأ: {str(e)}"})

@app.route("/api/submit_android_code", methods=["POST"])
def api_submit_android_code():
    data = request.json or {}
    code = data.get('code', '').strip()
    uid = get_current_user_id()
    if not code:
        return jsonify({"success": False, "message": "الكود مطلوب"})
    if not re.match(r'^\d{5,6}$', code):
        return jsonify({"success": False, "message": "صيغة الكود غير صحيحة"})
    ud = get_or_create_user(uid)
    with USERS_LOCK:
        ud.pending_auto_code = code
    socketio.emit('log_update', {"message": f"📲 تم استلام كود التحقق: {code}"}, to=uid)
    if ud.awaiting_code:
        def auto_verify():
            with app.app_context():
                try:
                    result = perform_auto_verification(uid, code)
                    if result.get('success'):
                        socketio.emit('log_update', {"message": "✅ تم التحقق التلقائي بنجاح!"}, to=uid)
                except Exception as e:
                    logger.error(f"Auto verification error: {e}")
        threading.Thread(target=auto_verify, daemon=True).start()
        return jsonify({"success": True, "message": "تم استلام الكود، جارٍ التحقق التلقائي..."})
    return jsonify({"success": True, "message": "تم استلام الكود وحفظه"})

def perform_auto_verification(uid, code):
    try:
        from telethon.errors import SessionPasswordNeededError
        ud = get_or_create_user(uid)
        if not ud.client_manager or not ud.awaiting_code:
            return {"success": False, "message": "لا يوجد طلب كود نشط"}
        user = ud.client_manager.run_coroutine(
            ud.client_manager.client.sign_in(ud.phone_number, code, phone_code_hash=ud.phone_code_hash)
        )
        with USERS_LOCK:
            ud.authenticated = True
            ud.connected = True
            ud.awaiting_code = False
            ud.pending_auto_code = None
        ud.client_manager.run_coroutine(ud.client_manager._register_event_handlers())
        try:
            me = ud.client_manager.run_coroutine(ud.client_manager.client.get_me())
            if me:
                tg_name = (getattr(me, 'first_name', '') or '') + (' ' + (getattr(me, 'last_name', '') or '')).rstrip()
                tg_name = tg_name.strip()
                with USERS_LOCK:
                    ud.telegram_name = tg_name
                settings = load_settings(uid)
                settings['telegram_name'] = tg_name
                save_settings(uid, settings)
                socketio.emit('telegram_name_update', {'name': tg_name}, to=uid)
        except Exception as me_err:
            logger.warning(f"get_me error: {me_err}")
        socketio.emit('log_update', {"message": "✅ تم تسجيل الدخول بنجاح"}, to=uid)
        return {"success": True, "message": "تم التحقق"}
    except Exception as e:
        err_name = type(e).__name__
        if 'SessionPasswordNeeded' in err_name:
            with USERS_LOCK:
                ud = USERS.get(uid)
                if ud:
                    ud.awaiting_code = False
                    ud.awaiting_password = True
            socketio.emit('log_update', {"message": "🔒 مطلوب كلمة مرور التحقق بخطوتين"}, to=uid)
            return {"success": False, "message": "مطلوب كلمة مرور"}
        elif 'PhoneCodeInvalid' in err_name:
            return {"success": False, "message": "الكود غير صحيح"}
        return {"success": False, "message": str(e)}

@app.route("/api/verify_code", methods=["POST"])
def api_verify_code():
    uid = get_current_user_id()
    data = request.json or {}
    code = data.get('code', '').strip()
    if not code:
        ud = get_or_create_user(uid)
        with USERS_LOCK:
            if ud.pending_auto_code:
                code = ud.pending_auto_code
            else:
                return jsonify({"success": False, "message": "أدخل كود التحقق"})
    try:
        from telethon.errors import SessionPasswordNeededError
        ud = get_or_create_user(uid)
        if not ud.client_manager or not ud.awaiting_code:
            return jsonify({"success": False, "message": "❌ لا يوجد طلب كود نشط"})
        user = ud.client_manager.run_coroutine(
            ud.client_manager.client.sign_in(ud.phone_number, code, phone_code_hash=ud.phone_code_hash)
        )
        with USERS_LOCK:
            ud.authenticated = True
            ud.connected = True
            ud.awaiting_code = False
            ud.pending_auto_code = None
        ud.client_manager.run_coroutine(ud.client_manager._register_event_handlers())
        try:
            me = ud.client_manager.run_coroutine(ud.client_manager.client.get_me())
            if me:
                tg_name = (getattr(me, 'first_name', '') or '') + (' ' + (getattr(me, 'last_name', '') or '')).rstrip()
                tg_name = tg_name.strip()
                with USERS_LOCK:
                    ud.telegram_name = tg_name
                settings = load_settings(uid)
                settings['telegram_name'] = tg_name
                save_settings(uid, settings)
                socketio.emit('telegram_name_update', {'name': tg_name}, to=uid)
        except Exception as me_err:
            logger.warning(f"get_me error: {me_err}")
        socketio.emit('log_update', {"message": "✅ تم تسجيل الدخول بنجاح"}, to=uid)
        return jsonify({"success": True, "message": "✅ تم تسجيل الدخول بنجاح", "status": "success"})
    except Exception as e:
        err_name = type(e).__name__
        if 'SessionPasswordNeeded' in err_name:
            with USERS_LOCK:
                ud = USERS.get(uid)
                if ud:
                    ud.awaiting_code = False
                    ud.awaiting_password = True
            return jsonify({"success": True, "message": "🔒 أدخل كلمة مرور التحقق بخطوتين", "status": "password_required"})
        elif 'PhoneCodeInvalid' in err_name:
            return jsonify({"success": False, "message": "❌ كود غير صحيح"})
        elif 'PhoneCodeExpired' in err_name:
            return jsonify({"success": False, "message": "❌ انتهت صلاحية الكود"})
        return jsonify({"success": False, "message": f"❌ {str(e)}", "status": "error"})

@app.route("/api/verify_password", methods=["POST"])
def api_verify_password():
    uid = get_current_user_id()
    data = request.json or {}
    password = data.get('password', '')
    if not password:
        return jsonify({"success": False, "message": "أدخل كلمة المرور"})
    try:
        from telethon.errors import PasswordHashInvalidError
        ud = get_or_create_user(uid)
        if not ud.client_manager:
            return jsonify({"success": False, "message": "❌ العميل غير متصل"})
        ud.client_manager.run_coroutine(ud.client_manager.client.sign_in(password=password))
        with USERS_LOCK:
            ud.authenticated = True
            ud.connected = True
            ud.awaiting_password = False
        ud.client_manager.run_coroutine(ud.client_manager._register_event_handlers())
        try:
            me = ud.client_manager.run_coroutine(ud.client_manager.client.get_me())
            if me:
                tg_name = (getattr(me, 'first_name', '') or '') + (' ' + (getattr(me, 'last_name', '') or '')).rstrip()
                tg_name = tg_name.strip()
                with USERS_LOCK:
                    ud.telegram_name = tg_name
                settings = load_settings(uid)
                settings['telegram_name'] = tg_name
                save_settings(uid, settings)
                socketio.emit('telegram_name_update', {'name': tg_name}, to=uid)
        except Exception as me_err:
            logger.warning(f"get_me error: {me_err}")
        socketio.emit('log_update', {"message": "✅ تم التحقق من كلمة المرور"}, to=uid)
        return jsonify({"success": True, "message": "✅ تم تسجيل الدخول بنجاح"})
    except Exception as e:
        err_name = type(e).__name__
        if 'PasswordHashInvalid' in err_name:
            return jsonify({"success": False, "message": "❌ كلمة المرور غير صحيحة"})
        return jsonify({"success": False, "message": f"❌ {str(e)}"})

@app.route("/api/reset_login", methods=["POST"])
def api_reset_login():
    uid = get_current_user_id()
    try:
        ud = get_or_create_user(uid)
        if ud.client_manager:
            ud.client_manager.stop()
            ud.client_manager = None
        session_file = os.path.join(SESSIONS_DIR, f"{uid}_session.session")
        if os.path.exists(session_file):
            os.remove(session_file)
        with USERS_LOCK:
            ud.authenticated = False
            ud.connected = False
            ud.awaiting_code = False
            ud.awaiting_password = False
            ud.phone_number = None
            ud.monitoring_active = False
            ud.is_running = False
            ud.scheduled_active = False
        settings = load_settings(uid)
        settings.pop('phone', None)
        save_settings(uid, settings)
        socketio.emit('log_update', {"message": "🔄 تم إعادة تعيين الجلسة - قم بتسجيل الدخول مجدداً"}, to=uid)
        return jsonify({"success": True, "message": "تم إعادة تعيين الجلسة بنجاح"})
    except Exception as e:
        return jsonify({"success": False, "message": str(e)})

# ========== مسارات البث الجديدة (حفظ الإعدادات، إرسال فوري، إرسال مجدول، إيقاف) ==========
def extract_group_entities(text):
    return parse_entities(text)

@app.route("/api/upload_image", methods=["POST"])
def api_upload_image():
    if 'image' not in request.files:
        return jsonify({"success": False, "message": "لا توجد صورة"})
    file = request.files['image']
    if file.filename == '':
        return jsonify({"success": False, "message": "لم يتم اختيار ملف"})
    ext = file.filename.rsplit('.', 1)[-1].lower()
    if ext not in ['jpg', 'jpeg', 'png', 'gif', 'webp']:
        return jsonify({"success": False, "message": "نوع الملف غير مدعوم"})
    filename = f"{uuid.uuid4().hex}.{ext}"
    filepath = os.path.join(UPLOADS_DIR, filename)
    file.save(filepath)
    relative_path = f"/static/uploads/{filename}"
    return jsonify({"success": True, "path": relative_path, "filename": filename})

@app.route("/api/save_broadcast_settings", methods=["POST"])
def api_save_broadcast_settings():
    uid = get_current_user_id()
    data = request.json or {}
    groups_text = data.get('groups_text', '')
    message = data.get('message', '')
    image_url = data.get('image_url', '')
    extracted_groups = extract_group_entities(groups_text) if groups_text else []
    settings = load_settings(uid)
    settings['broadcast_groups_text'] = groups_text
    settings['broadcast_groups'] = extracted_groups
    settings['broadcast_message'] = message
    settings['broadcast_image'] = image_url
    save_settings(uid, settings)
    ud = get_or_create_user(uid)
    ud.settings = settings
    ud.scheduled_groups = extracted_groups
    ud.scheduled_message = message
    ud.scheduled_image = image_url if image_url else None
    return jsonify({
        "success": True,
        "extracted_groups": extracted_groups,
        "count": len(extracted_groups),
        "message": f"تم حفظ الإعدادات واستخراج {len(extracted_groups)} مجموعة"
    })

@app.route("/api/send_now", methods=["POST"])
def api_send_now():
    uid = get_current_user_id()
    ud = get_or_create_user(uid)
    if not ensure_client_running(uid):
        return jsonify({"success": False, "message": "العميل غير جاهز، حاول مرة أخرى"})
    settings = load_settings(uid)
    groups = settings.get('broadcast_groups', [])
    message = settings.get('broadcast_message', '')
    image_path = settings.get('broadcast_image', '')
    if not groups:
        return jsonify({"success": False, "message": "لا توجد مجموعات محددة. قم بحفظ الإعدادات أولاً."})
    if not message and not image_path:
        return jsonify({"success": False, "message": "لا يوجد نص أو صورة للإرسال"})
    # تحويل المسار إلى مطلق
    abs_image_path = None
    if image_path and image_path.startswith('/'):
        abs_image_path = os.path.join(app.root_path, image_path.lstrip('/'))
        if not os.path.exists(abs_image_path):
            abs_image_path = None
    elif image_path and os.path.exists(image_path):
        abs_image_path = image_path
    def send_async():
        with app.app_context():
            try:
                ud.client_manager.run_coroutine(ud.client_manager._send_to_groups(groups, message, abs_image_path))
                socketio.emit('log_update', {"message": "✅ تم الإرسال الفوري بنجاح"}, to=uid)
            except Exception as e:
                logger.error(f"Send now error: {e}")
                socketio.emit('log_update', {"message": f"❌ فشل الإرسال الفوري: {str(e)}"}, to=uid)
    threading.Thread(target=send_async, daemon=True).start()
    return jsonify({
        "success": True,
        "message": f"بدء الإرسال الفوري إلى {len(groups)} مجموعة",
        "groups_count": len(groups)
    })

@app.route("/api/start_scheduled", methods=["POST"])
def api_start_scheduled():
    uid = get_current_user_id()
    data = request.json or {}
    interval = data.get('interval', 30)
    ud = get_or_create_user(uid)
    settings = load_settings(uid)
    groups = settings.get('broadcast_groups', [])
    message = settings.get('broadcast_message', '')
    image_path = settings.get('broadcast_image', '')
    if not groups:
        return jsonify({"success": False, "message": "لا توجد مجموعات محددة. قم بحفظ الإعدادات أولاً."})
    if interval < 1:
        return jsonify({"success": False, "message": "الفاصل الزمني يجب أن يكون أكبر من 0"})
    if not ensure_client_running(uid):
        return jsonify({"success": False, "message": "العميل غير جاهز"})
    if ud.client_manager.scheduled_thread and ud.client_manager.scheduled_thread.is_alive():
        ud.client_manager.stop_scheduled()
        time.sleep(1)
    abs_image_path = None
    if image_path and image_path.startswith('/'):
        abs_image_path = os.path.join(app.root_path, image_path.lstrip('/'))
        if not os.path.exists(abs_image_path):
            abs_image_path = None
    elif image_path and os.path.exists(image_path):
        abs_image_path = image_path
    ud.client_manager.start_scheduled(groups, message, abs_image_path, interval)
    return jsonify({
        "success": True,
        "message": f"تم بدء الإرسال المجدول كل {interval} دقيقة (سيتم الإرسال فوراً ثم كل {interval} دقيقة)",
        "interval": interval
    })

@app.route("/api/stop_scheduled", methods=["POST"])
def api_stop_scheduled():
    uid = get_current_user_id()
    ud = get_or_create_user(uid)
    if ud.client_manager:
        ud.client_manager.stop_scheduled()
        return jsonify({"success": True, "message": "تم إيقاف الإرسال المجدول"})
    else:
        return jsonify({"success": False, "message": "لا توجد جدولة نشطة"})

# ========== مسارات إضافية للبوت التعليمي والإدارة وغيرها ==========
@app.route("/api/save_auto_replies", methods=["POST"])
def api_save_auto_replies():
    uid = get_current_user_id()
    data = request.json or {}
    auto_replies = data.get('auto_replies', [])
    settings = load_settings(uid)
    settings['auto_replies'] = auto_replies
    save_settings(uid, settings)
    ud = get_or_create_user(uid)
    ud.auto_replies = auto_replies
    return jsonify({"success": True, "message": "تم حفظ الردود التلقائية"})

@app.route("/api/save_settings", methods=["POST"])
def api_save_settings():
    uid = get_current_user_id()
    data = request.json or {}
    settings = load_settings(uid)
    if 'message' in data:
        settings['message'] = data['message']
    if 'groups' in data:
        settings['groups'] = data['groups']
    if 'watch_words' in data:
        settings['watch_words'] = data['watch_words']
    if 'send_type' in data:
        settings['send_type'] = data['send_type']
    if 'interval' in data:
        settings['interval'] = data['interval']
    save_settings(uid, settings)
    ud = get_or_create_user(uid)
    ud.settings = settings
    return jsonify({"success": True, "message": "تم حفظ الإعدادات"})

@app.route("/api/start_monitoring", methods=["POST"])
def api_start_monitoring():
    uid = get_current_user_id()
    ud = get_or_create_user(uid)
    if not ensure_client_running(uid):
        return jsonify({"success": False, "message": "العميل غير جاهز"})
    ud.monitoring_active = True
    ud.is_running = True
    settings = load_settings(uid)
    settings['monitoring_active'] = True
    save_settings(uid, settings)
    bot = get_learning_bot(uid)
    bot.is_monitoring = True
    if ud.client_manager and ud.client_manager.client:
        try:
            ud.client_manager.run_coroutine(bot.start_with_client(ud.client_manager.client))
        except Exception as e:
            logger.error(f"Start bot error: {e}")
    socketio.emit('monitoring_status', {'is_running': True}, to=uid)
    return jsonify({"success": True, "message": "تم تشغيل المراقبة"})

@app.route("/api/stop_monitoring", methods=["POST"])
def api_stop_monitoring():
    uid = get_current_user_id()
    ud = get_or_create_user(uid)
    ud.monitoring_active = False
    ud.is_running = False
    settings = load_settings(uid)
    settings['monitoring_active'] = False
    save_settings(uid, settings)
    bot = get_learning_bot(uid)
    bot.is_monitoring = False
    socketio.emit('monitoring_status', {'is_running': False}, to=uid)
    return jsonify({"success": True, "message": "تم إيقاف المراقبة"})

@app.route("/api/reset_stats", methods=["POST"])
def api_reset_stats():
    uid = get_current_user_id()
    ud = get_or_create_user(uid)
    ud.stats = {"sent": 0, "errors": 0, "alerts": 0, "replies": 0}
    socketio.emit('stats_update', dict(ud.stats), to=uid)
    return jsonify({"success": True, "message": "تم إعادة تعيين الإحصائيات"})

@app.route("/api/join_group", methods=["POST"])
def api_join_group():
    uid = get_current_user_id()
    data = request.json or {}
    link = data.get('link', '')
    if not link:
        return jsonify({"success": False, "message": "الرابط مطلوب"})
    if not ensure_client_running(uid):
        return jsonify({"success": False, "message": "العميل غير جاهز"})
    ud = get_or_create_user(uid)
    try:
        if link.startswith('+'):
            result = ud.client_manager.run_coroutine(ud.client_manager.client(ImportChatInviteRequest(link[1:])))
            return jsonify({"success": True, "message": "تم الانضمام بنجاح"})
        elif link.startswith('@') or link.startswith('https://t.me/'):
            entity = ud.client_manager.run_coroutine(ud.client_manager.client.get_entity(link))
            if entity:
                return jsonify({"success": True, "message": f"تم العثور على المجموعة: {entity.title}"})
            else:
                return jsonify({"success": False, "message": "لم يتم العثور على المجموعة"})
        else:
            return jsonify({"success": False, "message": "صيغة رابط غير مدعومة"})
    except Exception as e:
        return jsonify({"success": False, "message": str(e)})

@app.route("/api/parse_join_links", methods=["POST"])
def api_parse_join_links():
    data = request.json or {}
    raw = data.get('raw', '')
    if not raw:
        return jsonify({"success": False, "links": [], "count": 0})
    links = parse_entities(raw)
    labeled = [{"label": l, "link": l} for l in links]
    return jsonify({"success": True, "links": labeled, "count": len(labeled)})

@app.route("/api/bulk_join", methods=["POST"])
def api_bulk_join():
    uid = get_current_user_id()
    data = request.json or {}
    links = data.get('links', [])
    if not links:
        return jsonify({"success": False, "message": "لا توجد روابط"})
    if not ensure_client_running(uid):
        return jsonify({"success": False, "message": "العميل غير جاهز"})
    ud = get_or_create_user(uid)
    results = []
    for item in links:
        link = item.get('link')
        try:
            if link.startswith('+'):
                ud.client_manager.run_coroutine(ud.client_manager.client(ImportChatInviteRequest(link[1:])))
                results.append({"link": link, "status": "success"})
            else:
                results.append({"link": link, "status": "skipped"})
        except Exception as e:
            results.append({"link": link, "status": "error", "error": str(e)})
    return jsonify({"success": True, "message": f"تمت معالجة {len(results)} رابط", "results": results})

@app.route("/api/sent_batches", methods=["GET"])
def api_sent_batches():
    uid = get_current_user_id()
    ud = get_or_create_user(uid)
    return jsonify({"success": True, "batches": ud.sent_batches})

@app.route("/api/edit_batch", methods=["POST"])
def api_edit_batch():
    uid = get_current_user_id()
    data = request.json or {}
    batch_id = data.get('batch_id')
    new_text = data.get('new_text')
    if not batch_id or new_text is None:
        return jsonify({"success": False, "message": "بيانات ناقصة"})
    ud = get_or_create_user(uid)
    if not ud.client_manager:
        return jsonify({"success": False, "message": "العميل غير متصل"})
    try:
        result = ud.client_manager.run_coroutine(ud.client_manager._edit_batch_messages(batch_id, new_text))
        if result.get('ok'):
            return jsonify({"success": True, "message": f"تم تعديل {result['edited']} رسالة"})
        else:
            return jsonify({"success": False, "message": result.get('msg', 'خطأ')})
    except Exception as e:
        return jsonify({"success": False, "message": str(e)})

@app.route("/api/delete_batch", methods=["POST"])
def api_delete_batch():
    uid = get_current_user_id()
    data = request.json or {}
    batch_id = data.get('batch_id')
    if not batch_id:
        return jsonify({"success": False, "message": "معرف الدفعة مطلوب"})
    ud = get_or_create_user(uid)
    if not ud.client_manager:
        return jsonify({"success": False, "message": "العميل غير متصل"})
    try:
        result = ud.client_manager.run_coroutine(ud.client_manager._delete_batch_messages(batch_id))
        if result.get('ok'):
            return jsonify({"success": True, "message": f"تم حذف {result['deleted']} رسالة"})
        else:
            return jsonify({"success": False, "message": result.get('msg', 'خطأ')})
    except Exception as e:
        return jsonify({"success": False, "message": str(e)})

# ========== مسارات البوت التعليمي ==========
@app.route("/api/learning/status", methods=["GET"])
def api_learning_status():
    uid = get_current_user_id()
    bot = get_learning_bot(uid)
    return jsonify({"success": True, "active": bot.is_monitoring, "reply_in_groups": bot.reply_in_groups})

@app.route("/api/learning/toggle", methods=["POST"])
def api_learning_toggle():
    uid = get_current_user_id()
    bot = get_learning_bot(uid)
    bot.is_monitoring = not bot.is_monitoring
    if bot.is_monitoring:
        ud = get_or_create_user(uid)
        if ud.client_manager and ud.client_manager.client:
            try:
                ud.client_manager.run_coroutine(bot.start_with_client(ud.client_manager.client))
            except Exception as e:
                logger.error(f"Start bot error: {e}")
    return jsonify({"success": True, "active": bot.is_monitoring, "message": "تم تبديل حالة البوت"})

@app.route("/api/learning/toggle_public", methods=["POST"])
def api_learning_toggle_public():
    uid = get_current_user_id()
    bot = get_learning_bot(uid)
    bot.reply_in_groups = not bot.reply_in_groups
    return jsonify({"success": True, "reply_in_groups": bot.reply_in_groups, "message": "تم تبديل الرد في المجموعات"})

@app.route("/api/learning/services", methods=["GET"])
def api_learning_services():
    uid = get_current_user_id()
    bot = get_learning_bot(uid)
    return jsonify({"success": True, "services": bot.get_services()})

@app.route("/api/learning/unknown", methods=["GET"])
def api_learning_unknown():
    uid = get_current_user_id()
    bot = get_learning_bot(uid)
    return jsonify({"success": True, "requests": bot.get_unknown_requests()})

@app.route("/api/learning/teach", methods=["POST"])
def api_learning_teach():
    uid = get_current_user_id()
    data = request.json or {}
    service = data.get('service')
    description = data.get('description')
    if not service or not description:
        return jsonify({"success": False, "message": "الخدمة والوصف مطلوبان"})
    bot = get_learning_bot(uid)
    if bot.add_service(service, description):
        return jsonify({"success": True, "message": f"تمت إضافة خدمة {service}"})
    else:
        return jsonify({"success": False, "message": "فشلت الإضافة"})

@app.route("/api/learning/delete", methods=["POST"])
def api_learning_delete():
    uid = get_current_user_id()
    data = request.json or {}
    service = data.get('service')
    if not service:
        return jsonify({"success": False, "message": "اسم الخدمة مطلوب"})
    bot = get_learning_bot(uid)
    if bot.delete_service(service):
        return jsonify({"success": True, "message": f"تم حذف خدمة {service}"})
    else:
        return jsonify({"success": False, "message": "الخدمة غير موجودة"})

@app.route("/api/learning/teach_from_unknown", methods=["POST"])
def api_learning_teach_from_unknown():
    uid = get_current_user_id()
    data = request.json or {}
    index = data.get('index')
    service = data.get('service')
    description = data.get('description')
    if index is None or not service or not description:
        return jsonify({"success": False, "message": "بيانات ناقصة"})
    bot = get_learning_bot(uid)
    unknown = bot.get_unknown_requests()
    if 0 <= index < len(unknown):
        if bot.add_service(service, description):
            bot.unknown_requests.pop(index)
            return jsonify({"success": True, "message": f"تم تعلم خدمة {service}"})
    return jsonify({"success": False, "message": "فشل تعلم الخدمة"})

@app.route("/api/learning/clear_unknown", methods=["POST"])
def api_learning_clear_unknown():
    uid = get_current_user_id()
    bot = get_learning_bot(uid)
    bot.clear_unknown()
    return jsonify({"success": True, "message": "تم مسح الطلبات غير المعروفة"})

# ========== مسارات الإدارة (Admin) ==========
admin_auth = threading.local()
admin_auth.authenticated = False

@app.route("/admin/api/check", methods=["GET"])
def admin_check():
    return jsonify({"authenticated": getattr(admin_auth, 'authenticated', False)})

@app.route("/admin/api/login", methods=["POST"])
def admin_login():
    data = request.json or {}
    if data.get('username') == ADMIN_USERNAME and data.get('password') == ADMIN_PASSWORD:
        admin_auth.authenticated = True
        return jsonify({"success": True})
    return jsonify({"success": False, "message": "بيانات غير صحيحة"})

@app.route("/admin/api/logout", methods=["POST"])
def admin_logout():
    admin_auth.authenticated = False
    return jsonify({"success": True})

def admin_required(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        if not getattr(admin_auth, 'authenticated', False):
            return jsonify({"success": False, "message": "غير مصرح"}), 401
        return f(*args, **kwargs)
    return decorated

@app.route("/admin/api/users", methods=["GET"])
@admin_required
def admin_get_users():
    users_list = []
    with USERS_LOCK:
        for uid, ud in USERS.items():
            users_list.append(ud.to_dict())
    return jsonify({"success": True, "users": users_list})

@app.route("/admin/api/user/<user_id>", methods=["POST"])
@admin_required
def admin_update_user(user_id):
    data = request.json or {}
    action = data.get('action')
    with USERS_LOCK:
        ud = USERS.get(user_id)
        if not ud:
            return jsonify({"success": False, "message": "المستخدم غير موجود"})
        if action == 'block':
            ud.blocked = data.get('blocked', False)
            settings = load_settings(user_id)
            settings['blocked'] = ud.blocked
            save_settings(user_id, settings)
        elif action == 'disable':
            ud.disabled = data.get('disabled', False)
            settings = load_settings(user_id)
            settings['disabled'] = ud.disabled
            save_settings(user_id, settings)
    return jsonify({"success": True})

@app.route("/admin/api/fetch_chats/<user_id>", methods=["GET"])
@admin_required
def admin_fetch_chats(user_id):
    ud = get_or_create_user(user_id)
    if not ensure_client_running(user_id):
        return jsonify({"success": False, "message": "العميل غير جاهز"})
    try:
        chats = ud.client_manager.run_coroutine(ud.client_manager._fetch_dialogs())
        return jsonify({"success": True, "chats": chats})
    except Exception as e:
        return jsonify({"success": False, "message": str(e)})

@app.route("/admin/api/user_alerts/<user_id>", methods=["GET"])
@admin_required
def admin_user_alerts(user_id):
    ud = get_or_create_user(user_id)
    return jsonify({"success": True, "alerts": ud.alerts[:50]})

@app.route("/admin/api/export", methods=["GET"])
@admin_required
def admin_export():
    data = []
    with USERS_LOCK:
        for uid, ud in USERS.items():
            data.append({
                "user_id": uid,
                "settings": ud.settings,
                "stats": ud.stats,
                "alerts": ud.alerts[:100],
                "auto_replies": ud.auto_replies,
                "sent_batches": ud.sent_batches
            })
    return jsonify(data)

@app.route("/admin/api/restart", methods=["POST"])
@admin_required
def admin_restart():
    def restart():
        time.sleep(1)
        os._exit(0)
    threading.Thread(target=restart, daemon=True).start()
    return jsonify({"success": True, "message": "جاري إعادة التشغيل..."})

@app.route("/admin/api/search/<user_id>", methods=["POST"])
@admin_required
def admin_search_messages(user_id):
    data = request.json or {}
    query = data.get('query', '')
    search_type = data.get('search_type', 'text')
    exclude_chats = data.get('exclude_chats', [])
    ud = get_or_create_user(user_id)
    if not ensure_client_running(user_id):
        return jsonify({"success": False, "message": "العميل غير جاهز"})
    def run_search():
        with app.app_context():
            try:
                results = ud.client_manager.search_messages(query, search_type, exclude_chats)
                socketio.emit('admin_search_results', {
                    "user_id": user_id,
                    "results": results,
                    "count": len(results)
                })
            except Exception as e:
                socketio.emit('admin_search_error', {"user_id": user_id, "error": str(e)})
    threading.Thread(target=run_search, daemon=True).start()
    return jsonify({"success": True, "message": "جاري البحث..."})

# إضافة دالة جلب الدردشات إلى TelegramClientManager
async def _fetch_dialogs(self):
    dialogs = []
    async for dialog in self.client.iter_dialogs():
        if dialog.is_group or dialog.is_channel:
            title = dialog.name
            username = getattr(dialog.entity, 'username', None)
            link = f"https://t.me/{username}" if username else f"https://t.me/c/{dialog.entity.id}"
            dialogs.append({"title": title, "link": link, "username": username})
    return dialogs

TelegramClientManager._fetch_dialogs = _fetch_dialogs

# ========== بدء التطبيق ==========
if __name__ == '__main__':
    port = int(os.environ.get('PORT', 5000))
    socketio.run(app, host='0.0.0.0', port=port, debug=False, allow_unsafe_werkzeug=True)