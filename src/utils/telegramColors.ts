// Official Telegram Author Colors in Telegram Web K (7 primary colors)
export const TELEGRAM_AUTHOR_COLORS = [
  '#e17076', // red
  '#faa774', // orange
  '#a695e7', // violet
  '#7bc862', // green
  '#6ec9cb', // cyan
  '#65aadd', // blue
  '#ee7aae', // pink
];

export function getSenderColor(senderId: string): string {
  let hash = 0;
  for (let i = 0; i < senderId.length; i++) {
    hash = senderId.charCodeAt(i) + ((hash << 5) - hash);
  }
  const index = Math.abs(hash) % TELEGRAM_AUTHOR_COLORS.length;
  return TELEGRAM_AUTHOR_COLORS[index];
}

export function formatTelegramDate(timestamp: number, lang: 'ar' | 'en' = 'ar'): string {
  const date = new Date(timestamp);
  const now = new Date();
  
  const isToday =
    date.getDate() === now.getDate() &&
    date.getMonth() === now.getMonth() &&
    date.getFullYear() === now.getFullYear();

  const yesterday = new Date(now);
  yesterday.setDate(now.getDate() - 1);
  const isYesterday =
    date.getDate() === yesterday.getDate() &&
    date.getMonth() === yesterday.getMonth() &&
    date.getFullYear() === yesterday.getFullYear();

  if (isToday) {
    return lang === 'ar' ? 'اليوم' : 'Today';
  }
  if (isYesterday) {
    return lang === 'ar' ? 'أمس' : 'Yesterday';
  }

  return date.toLocaleDateString(lang === 'ar' ? 'ar-SA' : 'en-US', {
    day: 'numeric',
    month: 'long',
    year: date.getFullYear() !== now.getFullYear() ? 'numeric' : undefined,
  });
}
