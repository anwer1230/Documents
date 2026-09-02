export interface TelegramStickerItem {
  id: string;
  pack: string;
  name: string;
  emoji: string;
  url: string;
}

export const TELEGRAM_STICKER_PACKS: {
  id: string;
  title: string;
  icon: string;
  stickers: TelegramStickerItem[];
}[] = [
  {
    id: "telegram_ducks",
    title: "بطة تيليجرام (Telegram Duck)",
    icon: "🦆",
    stickers: [
      { id: "duck_hello", pack: "ducks", name: "مرحباً", emoji: "👋", url: "https://images.unsplash.com/photo-1555852095-64e7428df0fa?w=180&auto=format&fit=crop&q=80" },
      { id: "duck_love", pack: "ducks", name: "حب", emoji: "❤️", url: "https://images.unsplash.com/photo-1548767797-d8c844163c4c?w=180&auto=format&fit=crop&q=80" },
      { id: "duck_cool", pack: "ducks", name: "نظارات", emoji: "😎", url: "https://images.unsplash.com/photo-1516467508483-a7212febe31a?w=180&auto=format&fit=crop&q=80" },
      { id: "duck_party", pack: "ducks", name: "احتفال", emoji: "🎉", url: "https://images.unsplash.com/photo-1530103862676-de8c9debad1d?w=180&auto=format&fit=crop&q=80" },
      { id: "duck_work", pack: "ducks", name: "عمل", emoji: "💻", url: "https://images.unsplash.com/photo-1583511655857-d19b40a7a54e?w=180&auto=format&fit=crop&q=80" },
      { id: "duck_sleep", pack: "ducks", name: "نوم", emoji: "😴", url: "https://images.unsplash.com/photo-1541781774459-bb2af2f05b55?w=180&auto=format&fit=crop&q=80" },
    ],
  },
  {
    id: "telegram_premium",
    title: "ملصقات بريميوم (Premium Animated)",
    icon: "⭐",
    stickers: [
      { id: "prem_star", pack: "premium", name: "نجمة ذهبية", emoji: "⭐", url: "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=180&auto=format&fit=crop&q=80" },
      { id: "prem_fire", pack: "premium", name: "شعلة نارية", emoji: "🔥", url: "https://images.unsplash.com/photo-1569429593410-b498b3fb3387?w=180&auto=format&fit=crop&q=80" },
      { id: "prem_rocket", pack: "premium", name: "صاروخ الفضاء", emoji: "🚀", url: "https://images.unsplash.com/photo-1517976487507-5b6513d88bf7?w=180&auto=format&fit=crop&q=80" },
      { id: "prem_diamond", pack: "premium", name: "الماس", emoji: "💎", url: "https://images.unsplash.com/photo-1515377905703-c4788e51af15?w=180&auto=format&fit=crop&q=80" },
      { id: "prem_crown", pack: "premium", name: "تاج", emoji: "👑", url: "https://images.unsplash.com/photo-1579783902614-a3fb3927b675?w=180&auto=format&fit=crop&q=80" },
      { id: "prem_trophy", pack: "premium", name: "كأس", emoji: "🏆", url: "https://images.unsplash.com/photo-1567427017947-545c5f8d16ad?w=180&auto=format&fit=crop&q=80" },
    ],
  },
  {
    id: "telegram_cats",
    title: "قطط مضحكة (Funny Cats)",
    icon: "🐱",
    stickers: [
      { id: "cat_shocked", pack: "cats", name: "مصدوم", emoji: "🙀", url: "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=180&auto=format&fit=crop&q=80" },
      { id: "cat_cute", pack: "cats", name: "لطيف", emoji: "😻", url: "https://images.unsplash.com/photo-1533738363-b7f9aef128ce?w=180&auto=format&fit=crop&q=80" },
      { id: "cat_angry", pack: "cats", name: "غاضب", emoji: "😾", url: "https://images.unsplash.com/photo-1573865526739-10659fec78a5?w=180&auto=format&fit=crop&q=80" },
      { id: "cat_laugh", pack: "cats", name: "ضحك", emoji: "😹", url: "https://images.unsplash.com/photo-1495360010541-f48722b34f7d?w=180&auto=format&fit=crop&q=80" },
    ],
  },
];

export const TELEGRAM_GIFS = [
  { id: "gif_celebrate", title: "احتفال", url: "https://media.giphy.com/media/26u4cqiYI30juCOGY/giphy.gif" },
  { id: "gif_thumbs_up", title: "ممتاز", url: "https://media.giphy.com/media/111ebonMs90YLu/giphy.gif" },
  { id: "gif_mindblown", title: "انبهار", url: "https://media.giphy.com/media/26ufdipQqU2lhNA4g/giphy.gif" },
  { id: "gif_dance", title: "رقص", url: "https://media.giphy.com/media/blSTtZehjAZ8I/giphy.gif" },
  { id: "gif_love", title: "قلوب", url: "https://media.giphy.com/media/l4pTdcifPZLpDjL1e/giphy.gif" },
  { id: "gif_applause", title: "تصفيق", url: "https://media.giphy.com/media/l3q2XhfQ8oCkm1Ts4/giphy.gif" },
];
