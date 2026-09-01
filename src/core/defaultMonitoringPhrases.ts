/**
 * defaultMonitoringPhrases.ts
 *
 * Permanent, immutable core list of full Arabic monitoring phrases for Live Monitoring (المراقبة اللحظية).
 * Defined as full phrases (not isolated tokens) for exact semantic matching on incoming Telegram messages.
 */

export const PERMANENT_MONITORING_PHRASES: string[] = [
  'اريد مساعدة',
  'ابي مساعدة',
  'من يسوي تكليف',
  'من يحل',
  'عندي بحث',
  'معي واجب',
  'عندي اسايمنت',
  'من يسوي اسايمنت',
  'ابي سكليف',
  'ابي عذر',
  'من يسوي سكليف',
  'ابي شخص مضمون',
  'ابي مختص',
  'هيليب',
  'من يستطيع',
  'تعرفون احد',
  'تعرفون شخص',
  'من يساعدني',
  'من يعرف مختص',
  'مين يعرف يحل واجب',
  'من يحل واجبات الجامعه',
  'أحتاج مساعدتكم',
  'ابي احد يسوي بحث',
  'مين يعرف مختص',
  'من يعرف احد كويس'
];

/**
 * Deduplicated string of default phrases joined by newlines for UI textareas
 */
export const DEFAULT_MONITORING_TEXT: string = Array.from(new Set(PERMANENT_MONITORING_PHRASES)).join('\n');
