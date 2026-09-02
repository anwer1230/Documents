"""مراقب الانضمام الفوري للروابط العامة.

هذا النظام يعمل لكل حساب Telegram على حدة:
* يقرأ الروابط من رسائل المجموعات الواردة فقط.
* يتخطى روابط الدعوة والقنوات والروابط الخاصة دون محاولة انضمام.
* ينضم إلى المجموعات العامة بترتيب آمن.
* يحفظ الروابط المؤجلة عند بلوغ حد Telegram ويعيد المحاولة بعد انتهاء الانتظار.
"""

from __future__ import annotations

import json
import os
import re
import threading
import time
import asyncio
from typing import Any, Awaitable, Callable, Dict, List, Optional

from telethon.errors import FloodWaitError, UserAlreadyParticipantError
from telethon.tl.functions.channels import JoinChannelRequest


_STATE_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "data", "instant_join_state.json")
_STATE_LOCK = threading.RLock()

_PUBLIC_LINK_RE = re.compile(
    r"(?:https?://)?(?:www\.)?(?:t\.me|telegram\.me)/([A-Za-z0-9_+/\-]+)",
    re.IGNORECASE,
)
_SKIPPED_PATHS = {"joinchat", "c", "s", "addlist", "proxy", "iv"}


def _now() -> float:
    return time.time()


def _safe_int(value: Any, default: int, minimum: int, maximum: int) -> int:
    try:
        return max(minimum, min(maximum, int(value)))
    except (TypeError, ValueError):
        return default


def _load_all_state() -> Dict[str, Any]:
    with _STATE_LOCK:
        try:
            with open(_STATE_FILE, "r", encoding="utf-8") as handle:
                value = json.load(handle)
            return value if isinstance(value, dict) else {}
        except (FileNotFoundError, json.JSONDecodeError, OSError):
            return {}


def _write_all_state(value: Dict[str, Any]) -> None:
    os.makedirs(os.path.dirname(_STATE_FILE), exist_ok=True)
    temp_file = f"{_STATE_FILE}.tmp"
    with open(temp_file, "w", encoding="utf-8") as handle:
        json.dump(value, handle, ensure_ascii=False, indent=2)
    os.replace(temp_file, _STATE_FILE)


def _extract_message_links(message: Any) -> List[str]:
    """استخرج روابط t.me من النص ومن كيانات TextUrl."""
    candidates: List[str] = []
    text = getattr(message, "text", None) or getattr(message, "message", None) or ""
    candidates.extend(match.group(1) for match in _PUBLIC_LINK_RE.finditer(text))

    for entity in getattr(message, "entities", None) or []:
        entity_url = getattr(entity, "url", None)
        if entity_url:
            candidates.extend(match.group(1) for match in _PUBLIC_LINK_RE.finditer(entity_url))

    result: List[str] = []
    seen = set()
    for raw in candidates:
        parts = raw.strip("/").split("/")
        if not parts:
            continue
        # +hash و joinchat و c/<id> روابط دعوة/خاصة، وليست روابط مجموعة عامة.
        if raw.startswith("+") or parts[0].lower() in _SKIPPED_PATHS:
            continue
        username = parts[0].split("?", 1)[0].split("#", 1)[0].strip()
        if not re.fullmatch(r"[A-Za-z0-9_]{5,32}", username):
            continue
        canonical = f"https://t.me/{username}"
        if canonical.lower() not in seen:
            seen.add(canonical.lower())
            result.append(canonical)
    return result


class InstantJoinMonitor:
    """طابور انضمام محافظ ودائم لكل حساب Telegram."""

    DEFAULT_DAILY_LIMIT = 8
    DEFAULT_MIN_INTERVAL = 120
    WINDOW_SECONDS = 24 * 60 * 60
    MAX_PENDING = 100

    def __init__(
        self,
        manager: Any,
        load_settings: Callable[[str], Dict[str, Any]],
        save_settings: Callable[[str, Dict[str, Any]], bool],
        notify: Callable[[str, str, Dict[str, Any]], Awaitable[None]],
        log: Callable[[str], None],
    ):
        self.manager = manager
        self.user_id = str(manager.user_id)
        self._load_settings = load_settings
        self._save_settings = save_settings
        self._notify = notify
        self._log = log
        self._task = None
        self._stop_requested = False
        self._wake_event = None
        self._last_result: Dict[str, Any] = {}

        state = _load_all_state().get(self.user_id, {})
        self._pending: List[Dict[str, Any]] = [
            item for item in state.get("pending", [])
            if isinstance(item, dict) and item.get("url")
        ]
        self._joined_at: List[float] = [
            float(value) for value in state.get("joined_at", [])
            if isinstance(value, (int, float))
        ]
        self._last_result = state.get("last_result", {}) if isinstance(state.get("last_result"), dict) else {}

        settings = self._settings()
        if "instant_join_enabled" not in settings:
            settings["instant_join_enabled"] = True
            self._save_settings(self.user_id, settings)

    def _settings(self) -> Dict[str, Any]:
        try:
            value = self._load_settings(self.user_id)
            return value if isinstance(value, dict) else {}
        except Exception:
            return {}

    def _enabled(self) -> bool:
        return self._settings().get("instant_join_enabled", True) is not False

    def _limits(self) -> tuple[int, int]:
        settings = self._settings()
        daily_limit = _safe_int(
            settings.get("instant_join_daily_limit"),
            self.DEFAULT_DAILY_LIMIT,
            1,
            20,
        )
        min_interval = _safe_int(
            settings.get("instant_join_min_interval"),
            self.DEFAULT_MIN_INTERVAL,
            60,
            3600,
        )
        return daily_limit, min_interval

    def _prune_history(self) -> None:
        cutoff = _now() - self.WINDOW_SECONDS
        self._joined_at = [stamp for stamp in self._joined_at if stamp > cutoff]

    def _persist(self) -> None:
        with _STATE_LOCK:
            all_state = _load_all_state()
            all_state[self.user_id] = {
                "pending": self._pending[-self.MAX_PENDING:],
                "joined_at": self._joined_at[-50:],
                "last_result": self._last_result,
            }
            _write_all_state(all_state)

    def _emit_log(self, message: str) -> None:
        try:
            self._log(message)
        except Exception:
            pass

    def _next_slot(self) -> float:
        self._prune_history()
        daily_limit, min_interval = self._limits()
        now = _now()
        next_slot = now
        if len(self._joined_at) >= daily_limit:
            next_slot = max(next_slot, self._joined_at[0] + self.WINDOW_SECONDS)
        if self._joined_at:
            next_slot = max(next_slot, self._joined_at[-1] + min_interval)
        return next_slot

    def _find_pending(self, url: str) -> Optional[Dict[str, Any]]:
        return next((item for item in self._pending if item.get("url") == url), None)

    def _remove_pending(self, item: Dict[str, Any]) -> None:
        try:
            self._pending.remove(item)
        except ValueError:
            pass
        self._persist()

    def _set_retry(self, item: Dict[str, Any], message: str, delay: int) -> None:
        item["attempts"] = int(item.get("attempts", 0)) + 1
        item["last_error"] = str(message)[:240]
        item["next_attempt_at"] = _now() + max(60, int(delay))
        self._last_result = {
            "url": item.get("url"),
            "status": "deferred",
            "reason": item["last_error"],
            "next_attempt_at": item["next_attempt_at"],
        }
        self._persist()

    async def start(self) -> None:
        if self._task and not self._task.done():
            return
        self._stop_requested = False
        self._wake_event = asyncio.Event()
        self._task = asyncio.create_task(self._worker())
        self._emit_log(f"⚡ الانضمام الفوري مفعّل للحساب {self.user_id} — تتم مراقبة الروابط العامة")

    async def stop(self) -> None:
        self._stop_requested = True
        if self._wake_event:
            self._wake_event.set()
        if self._task and not self._task.done():
            self._task.cancel()
            try:
                await self._task
            except BaseException:
                pass
        self._task = None

    async def handle_message(self, event: Any, chat: Any = None) -> None:
        if not self._enabled() or getattr(getattr(event, "message", None), "out", False):
            return
        if not (getattr(event, "is_group", False) or getattr(event, "is_channel", False)):
            return
        chat = chat or await event.get_chat()
        # event.is_channel يشمل القنوات والمجموعات الخارقة؛ نراقب المجموعات فقط.
        if getattr(chat, "broadcast", False) and not getattr(chat, "megagroup", False):
            return

        message = getattr(event, "message", None)
        for url in _extract_message_links(message):
            if self._find_pending(url):
                continue
            if len(self._pending) >= self.MAX_PENDING:
                self._pending.pop(0)
            self._pending.append({
                "url": url,
                "source_chat": getattr(chat, "title", None) or getattr(chat, "username", None) or str(getattr(chat, "id", "")),
                "source_message_id": getattr(message, "id", None),
                "added_at": _now(),
                "attempts": 0,
                "next_attempt_at": _now(),
            })
            self._persist()
            self._emit_log(f"🔎 رابط عام جديد في {self._pending[-1]['source_chat']}: {url} — تمت إضافته لطابور الفحص")
        if self._wake_event:
            self._wake_event.set()

    async def _wait(self, seconds: float) -> None:
        if not self._wake_event:
            return
        try:
            await asyncio.wait_for(self._wake_event.wait(), timeout=max(0.5, seconds))
            self._wake_event.clear()
        except asyncio.TimeoutError:
            pass

    def _due_item(self) -> Optional[Dict[str, Any]]:
        now = _now()
        due = [item for item in self._pending if float(item.get("next_attempt_at", 0)) <= now]
        return min(due, key=lambda item: float(item.get("added_at", now))) if due else None

    def _next_pending_time(self) -> Optional[float]:
        if not self._pending:
            return None
        return min(float(item.get("next_attempt_at", 0)) for item in self._pending)

    @staticmethod
    def _is_permanent_error(error: Exception) -> bool:
        text = str(error).lower()
        permanent_terms = (
            "channelprivate",
            "channel private",
            "private",
            "invalid",
            "not found",
            "username not occupied",
            "no user",
            "banned",
            "forbidden",
            "too many channels",
            "invite",
        )
        return any(term in text for term in permanent_terms)

    async def _process(self, item: Dict[str, Any]) -> None:
        url = item["url"]
        client = self.manager.client
        try:
            entity = await client.get_entity(url)
            if getattr(entity, "broadcast", False) or not getattr(entity, "megagroup", False):
                self._emit_log(f"⏭️ تم تخطي الرابط لأنه قناة أو ليس مجموعة عامة: {url}")
                self._last_result = {"url": url, "status": "skipped", "reason": "قناة أو رابط خاص"}
                self._remove_pending(item)
                return

            try:
                protected, reason, _bots = await self.manager.get_group_protection_details(entity)
                if protected:
                    self._emit_log(f"🛡️ تم تخطي مجموعة محمية: {url} — {reason or 'حماية مكتشفة'}")
                    self._last_result = {"url": url, "status": "skipped", "reason": reason or "مجموعة محمية"}
                    self._remove_pending(item)
                    return
            except Exception:
                # فشل الفحص الوقائي لا يمنع محاولة المجموعة العامة.
                pass

            await client(JoinChannelRequest(entity))
            joined_at = _now()
            self._joined_at.append(joined_at)
            self._prune_history()
            self._last_result = {"url": url, "status": "joined", "joined_at": joined_at}
            self._remove_pending(item)
            self._emit_log(f"✅ تم الانضمام الفوري إلى المجموعة العامة: {url}")
            await self._notify(
                "✅ تم الانضمام إلى مجموعة عامة",
                f"تم الانضمام تلقائياً إلى المجموعة:\n{url}",
                {"type": "instant_join", "status": "joined", "url": url},
            )
        except UserAlreadyParticipantError:
            self._last_result = {"url": url, "status": "already", "reason": "منضم مسبقاً"}
            self._remove_pending(item)
            self._emit_log(f"📌 منضم مسبقاً، تم حذف الرابط من الطابور: {url}")
        except FloodWaitError as error:
            wait_seconds = max(60, int(getattr(error, "seconds", 3600)))
            self._set_retry(item, f"Telegram طلب الانتظار {wait_seconds} ثانية", wait_seconds)
            self._emit_log(f"⏳ تم تأجيل {url} حتى انتهاء مهلة Telegram ({wait_seconds} ثانية)")
        except Exception as error:
            if self._is_permanent_error(error):
                self._last_result = {"url": url, "status": "skipped", "reason": str(error)[:240]}
                self._remove_pending(item)
                self._emit_log(f"⏭️ تم تخطي رابط غير صالح/خاص: {url} — {str(error)[:120]}")
            else:
                # الأخطاء المؤقتة تعاد بجدول متدرج حتى لا يتكرر الطلب بسرعة.
                delay = min(24 * 3600, 3600 * (2 ** min(int(item.get("attempts", 0)), 4)))
                self._set_retry(item, str(error), delay)
                self._emit_log(f"🔁 تعذر الانضمام الآن، ستتم إعادة المحاولة لاحقاً: {url}")

    async def _worker(self) -> None:
        while not self._stop_requested:
            if not self._enabled() or not self.manager.client:
                await self._wait(30)
                continue

            item = self._due_item()
            if item is None:
                next_time = self._next_pending_time()
                await self._wait(max(1, min(60, (next_time - _now()) if next_time else 60)))
                continue

            next_slot = self._next_slot()
            if next_slot > _now():
                # نؤجل أول رابط فقط؛ بقية الروابط تبقى محفوظة في الطابور.
                item["next_attempt_at"] = max(float(item.get("next_attempt_at", 0)), next_slot)
                self._persist()
                await self._wait(min(60, next_slot - _now()))
                continue

            try:
                await self._process(item)
            except asyncio.CancelledError:
                raise
            except Exception as error:
                self._set_retry(item, str(error), 3600)
                self._emit_log(f"⚠️ توقف فحص الرابط مؤقتاً: {str(error)[:120]}")

    def snapshot(self) -> Dict[str, Any]:
        self._prune_history()
        daily_limit, min_interval = self._limits()
        next_time = self._next_pending_time()
        return {
            "enabled": self._enabled(),
            "running": bool(self._task and not self._task.done()),
            "pending_count": len(self._pending),
            "pending": [
                {
                    "url": item.get("url"),
                    "source_chat": item.get("source_chat"),
                    "attempts": item.get("attempts", 0),
                    "next_attempt_at": item.get("next_attempt_at"),
                    "last_error": item.get("last_error"),
                }
                for item in self._pending[:100]
            ],
            "joined_last_24h": len(self._joined_at),
            "daily_limit": daily_limit,
            "min_interval_seconds": min_interval,
            "next_attempt_at": next_time,
            "last_result": self._last_result,
        }