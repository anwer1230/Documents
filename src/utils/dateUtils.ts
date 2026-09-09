/**
 * Telegram Date & Time Parsing Utilities
 *
 * MTProto (GramJS) uses UNIX timestamps in SECONDS (e.g., 1725484800).
 * Standard JavaScript Date expects MILLISECONDS.
 * Mandatory Rule: Any seconds-based timestamp (< 1e11) must be multiplied by 1000: new Date(sec * 1000).
 */

export function parseTelegramDate(dateVal: any): Date {
  if (dateVal === null || dateVal === undefined || dateVal === '') {
    return new Date();
  }
  if (dateVal instanceof Date) {
    return isNaN(dateVal.getTime()) ? new Date() : dateVal;
  }
  if (typeof dateVal === 'object') {
    if (typeof dateVal.rawDate === 'number' && dateVal.rawDate > 0) {
      return parseTelegramDate(dateVal.rawDate);
    }
    if (typeof dateVal.epoch === 'number' && dateVal.epoch > 0) {
      return parseTelegramDate(dateVal.epoch);
    }
    if (dateVal.date !== undefined && dateVal.date !== null && dateVal.date !== '') {
      return parseTelegramDate(dateVal.date);
    }
    if (dateVal.timestamp !== undefined && dateVal.timestamp !== null && dateVal.timestamp !== '') {
      return parseTelegramDate(dateVal.timestamp);
    }
  }
  if (typeof dateVal === 'number') {
    if (isNaN(dateVal) || dateVal <= 0) return new Date();
    // If < 1e11, it's UNIX seconds (e.g. 1725484800) -> multiply by 1000
    return dateVal < 1e11 ? new Date(dateVal * 1000) : new Date(dateVal);
  }
  if (typeof dateVal === 'string') {
    const trimmed = dateVal.trim();
    if (/^\d+$/.test(trimmed)) {
      const num = Number(trimmed);
      if (!isNaN(num) && num > 0) {
        return num < 1e11 ? new Date(num * 1000) : new Date(num);
      }
    }
    const parsed = Date.parse(trimmed);
    if (!isNaN(parsed)) {
      return new Date(parsed);
    }

    // Handle "10:30 AM" or "10:30 ص" or "22:15"
    const timeMatch = trimmed.match(/(\d{1,2}):(\d{2})(?:\s*(AM|PM|ص|م))?/i);
    if (timeMatch) {
      let hours = parseInt(timeMatch[1], 10);
      const minutes = parseInt(timeMatch[2], 10);
      const modifier = (timeMatch[3] || '').toUpperCase();
      if ((modifier === 'PM' || modifier === 'م') && hours < 12) hours += 12;
      if ((modifier === 'AM' || modifier === 'ص') && hours === 12) hours = 0;
      const d = new Date();
      d.setHours(hours, minutes, 0, 0);
      return d;
    }
  }
  return new Date();
}

/**
 * Returns normalized epoch in milliseconds. Returns 0 if invalid.
 */
export function getTelegramEpoch(dateVal: any): number {
  if (dateVal === null || dateVal === undefined || dateVal === '') {
    return 0;
  }
  if (typeof dateVal === 'number') {
    if (isNaN(dateVal) || dateVal <= 0) return 0;
    return dateVal < 1e11 ? dateVal * 1000 : dateVal;
  }
  if (typeof dateVal === 'object') {
    if (dateVal instanceof Date) {
      const t = dateVal.getTime();
      return isNaN(t) ? 0 : t;
    }
    if (typeof dateVal.rawDate === 'number' && dateVal.rawDate > 0) {
      return dateVal.rawDate < 1e11 ? dateVal.rawDate * 1000 : dateVal.rawDate;
    }
    if (typeof dateVal.epoch === 'number' && dateVal.epoch > 0) {
      return dateVal.epoch < 1e11 ? dateVal.epoch * 1000 : dateVal.epoch;
    }
    if (dateVal.date !== undefined && dateVal.date !== null && dateVal.date !== '') {
      const parsed = getTelegramEpoch(dateVal.date);
      if (parsed > 0) {
        // If date has a separate timestamp with hours/minutes, combine them
        if (dateVal.timestamp && typeof dateVal.timestamp === 'string') {
          const timeMatch = dateVal.timestamp.trim().match(/(\d{1,2}):(\d{2})(?:\s*(AM|PM|ص|م))?/i);
          if (timeMatch) {
            const d = new Date(parsed);
            let hours = parseInt(timeMatch[1], 10);
            const minutes = parseInt(timeMatch[2], 10);
            const modifier = (timeMatch[3] || '').toUpperCase();
            if ((modifier === 'PM' || modifier === 'م') && hours < 12) hours += 12;
            if ((modifier === 'AM' || modifier === 'ص') && hours === 12) hours = 0;
            d.setHours(hours, minutes, 0, 0);
            return d.getTime();
          }
        }
        return parsed;
      }
    }
    if (dateVal.timestamp !== undefined && dateVal.timestamp !== null && dateVal.timestamp !== '') {
      const parsed = getTelegramEpoch(dateVal.timestamp);
      if (parsed > 0) return parsed;
    }
  }
  if (typeof dateVal === 'string') {
    const trimmed = dateVal.trim();
    if (/^\d+$/.test(trimmed)) {
      const num = Number(trimmed);
      if (!isNaN(num) && num > 0) {
        return num < 1e11 ? num * 1000 : num;
      }
    }
    const parsed = Date.parse(trimmed);
    if (!isNaN(parsed)) return parsed;

    const timeMatch = trimmed.match(/(\d{1,2}):(\d{2})(?:\s*(AM|PM|ص|م))?/i);
    if (timeMatch) {
      let hours = parseInt(timeMatch[1], 10);
      const minutes = parseInt(timeMatch[2], 10);
      const modifier = (timeMatch[3] || '').toUpperCase();
      if ((modifier === 'PM' || modifier === 'م') && hours < 12) hours += 12;
      if ((modifier === 'AM' || modifier === 'ص') && hours === 12) hours = 0;
      const d = new Date();
      d.setHours(hours, minutes, 0, 0);
      return d.getTime();
    }
  }
  return 0;
}

/**
 * Formats time in 'ar-EG' locale using the user's browser timezone
 * Example output: "10:30 ص" or "02:15 م"
 */
export function formatTelegramTime(dateVal: any, timeZone?: string): string {
  const d = parseTelegramDate(dateVal);
  const tz = timeZone || (typeof Intl !== 'undefined' ? Intl.DateTimeFormat().resolvedOptions().timeZone : undefined);
  try {
    return d.toLocaleTimeString('ar-EG', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: true,
      timeZone: tz,
    });
  } catch (_) {
    return d.toLocaleTimeString('ar-EG', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: true,
    });
  }
}

/**
 * Formats date divider in 'ar-EG' (e.g. 'اليوم', 'أمس', '4 سبتمبر 2026')
 */
export function formatTelegramDateDivider(dateVal: any, timeZone?: string): string {
  const d = parseTelegramDate(dateVal);
  const tz = timeZone || (typeof Intl !== 'undefined' ? Intl.DateTimeFormat().resolvedOptions().timeZone : undefined);
  const now = new Date();

  const isToday =
    d.getDate() === now.getDate() &&
    d.getMonth() === now.getMonth() &&
    d.getFullYear() === now.getFullYear();
  if (isToday) return 'اليوم';

  const yesterday = new Date(now);
  yesterday.setDate(now.getDate() - 1);
  const isYesterday =
    d.getDate() === yesterday.getDate() &&
    d.getMonth() === yesterday.getMonth() &&
    d.getFullYear() === yesterday.getFullYear();
  if (isYesterday) return 'أمس';

  try {
    return d.toLocaleDateString('ar-EG', {
      day: 'numeric',
      month: 'long',
      year: d.getFullYear() !== now.getFullYear() ? 'numeric' : undefined,
      timeZone: tz,
    });
  } catch (_) {
    return d.toLocaleDateString('ar-EG', {
      day: 'numeric',
      month: 'long',
      year: d.getFullYear() !== now.getFullYear() ? 'numeric' : undefined,
    });
  }
}

/**
 * Formats chat list item timestamp (e.g. "10:30 ص", "أمس", "4 سبت", "2024/09/04")
 */
export function formatChatListTime(dateVal: any, timeZone?: string): string {
  if (!dateVal) return '';
  const d = parseTelegramDate(dateVal);
  const tz = timeZone || (typeof Intl !== 'undefined' ? Intl.DateTimeFormat().resolvedOptions().timeZone : undefined);
  const now = new Date();

  const isToday =
    d.getDate() === now.getDate() &&
    d.getMonth() === now.getMonth() &&
    d.getFullYear() === now.getFullYear();
  if (isToday) {
    return formatTelegramTime(d, tz);
  }

  const yesterday = new Date(now);
  yesterday.setDate(now.getDate() - 1);
  const isYesterday =
    d.getDate() === yesterday.getDate() &&
    d.getMonth() === yesterday.getMonth() &&
    d.getFullYear() === yesterday.getFullYear();
  if (isYesterday) return 'أمس';

  const diffDays = (now.getTime() - d.getTime()) / (1000 * 3600 * 24);
  if (diffDays < 7 && diffDays > 0) {
    try {
      return d.toLocaleDateString('ar-EG', { weekday: 'short', timeZone: tz });
    } catch (_) {
      return d.toLocaleDateString('ar-EG', { weekday: 'short' });
    }
  }

  try {
    return d.toLocaleDateString('ar-EG', {
      day: 'numeric',
      month: 'short',
      year: d.getFullYear() !== now.getFullYear() ? 'numeric' : undefined,
      timeZone: tz,
    });
  } catch (_) {
    return d.toLocaleDateString('ar-EG', {
      day: 'numeric',
      month: 'short',
      year: d.getFullYear() !== now.getFullYear() ? 'numeric' : undefined,
    });
  }
}
