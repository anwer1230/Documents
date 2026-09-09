"""خدمة مستقلة لاستخراج روابط Telegram العامة والانضمام للمجموعات فقط."""

import asyncio
import re
import threading
import time
from collections import deque
from urllib.parse import parse_qs, unquote, urlparse

from telethon import functions
from telethon.errors import FloodWaitError, UserAlreadyParticipantError


class DirectLinkJoinService:
    """مراقب روابط دائم، منفصل عن مراقبة الكلمات والمهام المجدولة."""

    MIN_JOIN_INTERVAL = 60
    MAX_JOINS_PER_HOUR = 15
    MAX_SEEN_LINKS = 1500
    MAX_HISTORY = 100

    _LINK_RE = re.compile(
        r"(?i)(?:(?:https?://)?(?:t\.me|telegram\.me)/[^\s<>()]+"
        r"|tg://resolve\?domain=[^\s<>()]+)"
    )

    def __init__(self, user_id, load_settings, save_settings, send_saved_message,
                 emit, logger):
        self.user_id = user_id
        self._load_settings = load_settings
        self._save_settings = save_settings
        self._send_saved_message = send_saved_message
        self._emit_callback = emit
        self._logger = logger
        self.client = None
        self._queue = None
        self._worker_task = None
        self._queued_links = set()
        self._state_lock = threading.RLock()
        self._load_state()

    def _load_state(self):
        try:
            settings = self._load_settings(self.user_id) or {}
        except Exception:
            settings = {}
        with self._state_lock:
            self._attempts = deque(
                float(value) for value in settings.get("direct_join_attempts", [])
                if isinstance(value, (int, float))
            )
            self._attempts = deque(
                value for value in self._attempts if time.time() - value < 3600
            )
            raw_seen = settings.get("direct_join_seen_links", {})
            self._seen_links = {
                str(key): float(value)
                for key, value in raw_seen.items()
                if isinstance(value, (int, float))
            } if isinstance(raw_seen, dict) else {}
            self._trim_seen_locked()
            self._last_attempt_at = float(
                settings.get("direct_join_last_attempt_at", 0) or 0
            )
            self._history = list(settings.get("direct_join_history", []))[-self.MAX_HISTORY:]
            raw_stats = settings.get("direct_join_stats", {})
            self._stats = {
                "joined": int(raw_stats.get("joined", 0) or 0),
                "already": int(raw_stats.get("already", 0) or 0),
                "skipped": int(raw_stats.get("skipped", 0) or 0),
                "failed": int(raw_stats.get("failed", 0) or 0),
                "rate_limited": int(raw_stats.get("rate_limited", 0) or 0),
            }

    def _trim_seen_locked(self):
        if len(self._seen_links) <= self.MAX_SEEN_LINKS:
            return
        ordered = sorted(self._seen_links.items(), key=lambda item: item[1])
        self._seen_links = dict(ordered[-self.MAX_SEEN_LINKS:])

    def _snapshot_locked(self):
        return {
            "direct_join_enabled": True,
            "direct_join_attempts": list(self._attempts)[-self.MAX_JOINS_PER_HOUR:],
            "direct_join_last_attempt_at": self._last_attempt_at,
            "direct_join_seen_links": dict(self._seen_links),
            "direct_join_history": self._history[-self.MAX_HISTORY:],
            "direct_join_stats": dict(self._stats),
        }

    def _persist(self):
        try:
            with self._state_lock:
                state = self._snapshot_locked()
            settings = self._load_settings(self.user_id) or {}
            settings.update(state)
            self._save_settings(self.user_id, settings, force=True)
        except Exception as exc:
            self._logger.warning("Direct link join state save failed for %s: %s", self.user_id, exc)

    def _emit(self, event_name, payload):
        try:
            self._emit_callback(event_name, payload)
        except Exception:
            pass

    @classmethod
    def extract_links(cls, message):
        if not message:
            return []
        text = getattr(message, "raw_text", None) or getattr(message, "text", None) or ""
        links = cls._LINK_RE.findall(text)
        for entity in getattr(message, "entities", None) or []:
            entity_url = getattr(entity, "url", None)
            if entity_url:
                links.append(entity_url)
        result = []
        seen = set()
        for link in links:
            normalized = cls.normalize_link(link)
            if normalized and normalized not in seen:
                seen.add(normalized)
                result.append(normalized)
        return result

    @staticmethod
    def normalize_link(link):
        value = unquote(str(link or "")).strip()
        value = value.rstrip(".,!?;:)]}>\"'،؛؟")
        if value.startswith("t.me/") or value.startswith("telegram.me/"):
            value = "https://" + value
        if value.startswith("tg://"):
            return value
        parsed = urlparse(value)
        if parsed.scheme not in ("http", "https") or parsed.netloc.lower() not in (
            "t.me", "www.t.me", "telegram.me", "www.telegram.me"
        ):
            return ""
        return f"https://t.me/{parsed.path.lstrip('/')}" + (
            f"?{parsed.query}" if parsed.query else ""
        )

    @staticmethod
    def public_username(link):
        value = str(link or "")
        if value.lower().startswith("tg://"):
            query = parse_qs(urlparse(value).query)
            path = query.get("domain", [""])[0]
        else:
            path = urlparse(value).path.strip("/")
        parts = [part for part in path.split("/") if part]
        if not parts:
            return None
        first = parts[0].lower()
        if first in {"joinchat", "c", "addstickers", "addemoji", "proxy", "s"}:
            if first == "s" and len(parts) > 1:
                first = parts[1].lower()
            else:
                return None
        if first.startswith("+") or first.startswith("-") or not re.fullmatch(
            r"[a-z0-9_]{5,32}", first
        ):
            return None
        return first

    @classmethod
    def classify_link(cls, link):
        return "public" if cls.public_username(link) else "private"

    async def start(self, client):
        self.client = client
        if self._worker_task and not self._worker_task.done():
            return
        self._queue = asyncio.Queue()
        self._worker_task = asyncio.create_task(self._worker())
        self._emit("direct_join_status", self.status())
        self._logger.info("✅ Direct link join service started for %s", self.user_id)

    async def stop(self):
        task = self._worker_task
        self._worker_task = None
        if task and not task.done():
            task.cancel()
            try:
                await task
            except asyncio.CancelledError:
                pass
        self._queue = None
        self._queued_links.clear()

    async def handle_message(self, message, source):
        if not self._queue:
            return
        for link in self.extract_links(message):
            kind = self.classify_link(link)
            with self._state_lock:
                if link in self._seen_links:
                    continue
                self._seen_links[link] = time.time()
                self._trim_seen_locked()
                if kind == "private":
                    self._record_locked(
                        link, "skipped_private",
                        "رابط خاص أو رابط قناة خاصة — تم تركه دون انضمام", source
                    )
                    should_queue = False
                elif link in self._queued_links:
                    should_queue = False
                else:
                    self._queued_links.add(link)
                    should_queue = True
            self._persist()
            if kind == "private":
                self._emit("direct_join_event", {
                    "status": "skipped_private",
                    "link": link,
                    "message": "تم تجاهل رابط خاص أو قناة خاصة",
                    "source": source,
                    "stats": self.status(),
                })
            elif should_queue:
                await self._queue.put((link, source))

    def _record_locked(self, link, status, message, source):
        self._history.append({
            "link": link,
            "status": status,
            "message": message,
            "source": source,
            "timestamp": time.time(),
        })
        self._history = self._history[-self.MAX_HISTORY:]
        if status == "joined":
            self._stats["joined"] += 1
        elif status == "already":
            self._stats["already"] += 1
        elif status == "rate_limited":
            self._stats["rate_limited"] += 1
        elif status.startswith("skipped"):
            self._stats["skipped"] += 1
        else:
            self._stats["failed"] += 1

    def _record(self, link, status, message, source):
        with self._state_lock:
            self._record_locked(link, status, message, source)
        self._persist()
        payload = {
            "status": status,
            "link": link,
            "message": message,
            "source": source,
            "stats": self.status(),
        }
        self._emit("direct_join_event", payload)
        self._emit("direct_join_status", self.status())

    async def _reserve_join_slot(self, link, source):
        while True:
            with self._state_lock:
                now = time.time()
                self._attempts = deque(
                    value for value in self._attempts if now - value < 3600
                )
                if len(self._attempts) >= self.MAX_JOINS_PER_HOUR:
                    message = "تم بلوغ حد 15 رابطاً خلال الساعة"
                    self._record_locked(link, "rate_limited", message, source)
                    persist = True
                    wait = 0
                else:
                    wait = max(0, self.MIN_JOIN_INTERVAL - (now - self._last_attempt_at))
                    if wait <= 0:
                        self._attempts.append(now)
                        self._last_attempt_at = now
                        persist = True
                    else:
                        persist = False
            if persist:
                self._persist()
                if wait == 0:
                    return True
                self._emit("direct_join_event", {
                    "status": "rate_limited",
                    "link": link,
                    "message": "تم إيقاف الرابط بسبب حد 15 رابطاً في الساعة",
                    "source": source,
                    "stats": self.status(),
                })
                return False
            await asyncio.sleep(min(wait, 5))

    async def _worker(self):
        while True:
            link, source = await self._queue.get()
            try:
                username = self.public_username(link)
                if not username:
                    self._record(link, "skipped_private", "رابط خاص — تم تركه دون انضمام", source)
                    continue
                try:
                    entity = await self.client.get_entity(f"@{username}")
                    is_group = bool(getattr(entity, "megagroup", False))
                    is_basic_group = entity.__class__.__name__ == "Chat"
                    is_public_channel = bool(getattr(entity, "broadcast", False)) and not is_group
                    if is_public_channel or not (is_group or is_basic_group):
                        self._record(
                            link, "skipped_channel",
                            "الرابط يعود إلى قناة عامة/كيان غير مجموعة — تم تركه", source
                        )
                        continue
                except Exception as exc:
                    self._record(link, "failed", f"تعذّر فحص المجموعة: {str(exc)[:120]}", source)
                    continue

                if not await self._reserve_join_slot(link, source):
                    continue
                try:
                    if not is_group:
                        self._record(
                            link, "skipped_unsupported",
                            "المجموعة القديمة غير قابلة للانضمام الآلي بأمان — تم تركها", source
                        )
                        continue
                    await self.client(functions.channels.JoinChannelRequest(entity))
                    message = f"✅ تم الانضمام تلقائياً إلى المجموعة العامة @{username}"
                    self._record(link, "joined", message, source)
                    await self._send_saved_message(
                        f"🤖 إشعار الانضمام المباشر\n\n{message}\n"
                        f"🔗 الرابط: {link}\n📍 مصدر الرابط: {source}"
                    )
                except UserAlreadyParticipantError:
                    self._record(link, "already", f"الحساب منضم مسبقاً إلى @{username}", source)
                except FloodWaitError as exc:
                    self._record(link, "failed", f"تيليجرام طلب الانتظار {exc.seconds} ثانية", source)
                except Exception as exc:
                    self._record(link, "failed", f"فشل الانضمام إلى @{username}: {str(exc)[:120]}", source)
            except asyncio.CancelledError:
                raise
            except Exception as exc:
                self._logger.error("Direct link join worker error for %s: %s", self.user_id, exc)
            finally:
                self._queued_links.discard(link)
                self._queue.task_done()

    def status(self):
        with self._state_lock:
            now = time.time()
            attempts = [value for value in self._attempts if now - value < 3600]
            self._attempts = deque(attempts)
            next_in = max(0, int(self.MIN_JOIN_INTERVAL - (now - self._last_attempt_at)))
            return {
                "enabled": True,
                "running": bool(self._worker_task and not self._worker_task.done()),
                "min_interval_seconds": self.MIN_JOIN_INTERVAL,
                "hourly_limit": self.MAX_JOINS_PER_HOUR,
                "attempts_last_hour": len(attempts),
                "remaining_hourly": max(0, self.MAX_JOINS_PER_HOUR - len(attempts)),
                "next_join_in_seconds": next_in,
                "stats": dict(self._stats),
                "history": list(self._history[-20:]),
            }