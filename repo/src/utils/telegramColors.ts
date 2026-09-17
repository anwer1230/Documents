const TELEGRAM_PALETTE = [
  '#e17076', // red
  '#faa774', // orange
  '#a695e7', // violet
  '#7bc862', // green
  '#6ec9cb', // cyan
  '#65aadd', // blue
  '#ee7aae', // pink
];

export function getSenderColor(idOrName: string | number): string {
  const str = String(idOrName || '0');
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    hash = (hash * 31 + str.charCodeAt(i)) & 0xffffffff;
  }
  const index = Math.abs(hash) % TELEGRAM_PALETTE.length;
  return TELEGRAM_PALETTE[index];
}

export function getAvatarColor(idOrName: string | number): string {
  return getSenderColor(idOrName);
}

export function getInitials(name: string): string {
  if (!name) return 'TG';
  const parts = name.trim().split(/\s+/);
  if (parts.length >= 2) {
    return (parts[0][0] + parts[1][0]).toUpperCase();
  }
  return name.slice(0, 2).toUpperCase();
}

export function formatTelegramDate(timestamp: number, lang: 'ar' | 'en' = 'ar'): string {
  if (!timestamp) return '';
  const date = new Date(timestamp);
  const now = new Date();
  const isToday =
    date.getDate() === now.getDate() &&
    date.getMonth() === now.getMonth() &&
    date.getFullYear() === now.getFullYear();

  if (isToday) {
    return date.toLocaleTimeString(lang === 'ar' ? 'ar-SA' : 'en-US', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    });
  }

  const isThisYear = date.getFullYear() === now.getFullYear();
  if (isThisYear) {
    return date.toLocaleDateString(lang === 'ar' ? 'ar-SA' : 'en-US', {
      month: 'short',
      day: 'numeric',
    });
  }

  return date.toLocaleDateString(lang === 'ar' ? 'ar-SA' : 'en-US', {
    year: 'numeric',
    month: 'numeric',
    day: 'numeric',
  });
}
