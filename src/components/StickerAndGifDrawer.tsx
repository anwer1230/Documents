import React, { useState } from 'react';
import { Smile, Sticker, Film, Search, X, Sparkles } from 'lucide-react';
import { TgsPlayer } from './TgsPlayer';
import {
  STAR_LOTTIE,
  HEART_LOTTIE,
  FIRE_LOTTIE,
  THUMBS_UP_LOTTIE,
  PARTY_LOTTIE,
} from '../utils/tgsAnimations';

interface StickerAndGifDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  onSelectEmoji: (emoji: string) => void;
  onSelectSticker: (sticker: {
    id: string;
    emoji: string;
    name: string;
    img: string;
    isTgs?: boolean;
    lottieData?: any;
  }) => void;
  onSelectGif: (gif: { id: string; title: string; url: string }) => void;
  isDark: boolean;
  lang: 'ar' | 'en';
}

const EMOJI_CATEGORIES = [
  {
    name: 'الرموز والمشاعر',
    nameEn: 'Smileys & Emotion',
    emojis: [
      '😀', '😃', '😄', '😁', '😆', '😅', '😂', '🤣', '🥹', '😊',
      '😇', '🙂', '🙃', '😉', '😌', '😍', '🥰', '😘', '😗', '😙',
      '😚', '😋', '😛', '😝', '😜', '🤪', '🤨', '🧐', '🤓', '😎',
      '🥸', '🤩', '🥳', '😏', '😒', '😞', '😔', '😟', '😕', '🙁',
      '☹️', '😣', '😖', '😫', '😩', '🥺', '😢', '😭', '😮‍💨', '😤',
      '😠', '😡', '🤬', '🤯', '😳', '🥵', '🥶', '😱', '😨', '😰'
    ]
  },
  {
    name: 'الإيماءات واليدين',
    nameEn: 'Hand Gestures',
    emojis: [
      '👍', '👎', '👌', '🤌', '🤏', '✌️', '🤞', '🫰', '🤟', '🤘',
      '🤙', '👈', '👉', '👆', '🖕', '👇', '☝️', '👋', '🤚', '🖐️',
      '✋', '🖖', '👏', '🙌', '👐', '🤲', '🤝', '🙏', '✍️', '💪'
    ]
  },
  {
    name: 'القلوب والتأثيرات',
    nameEn: 'Hearts & Vibes',
    emojis: [
      '❤️', '🧡', '💛', '💚', '💙', '💜', '🖤', '🤍', '🤎', '💔',
      '❤️‍🔥', '❤️‍🩹', '❣️', '💕', '💞', '💓', '💗', '💖', '💘', '💝',
      '🔥', '✨', '⚡', '💥', '💯', '💢', '💫', '🌟', '⭐', '🎉'
    ]
  },
  {
    name: 'الحيوانات والطبيعة',
    nameEn: 'Animals & Nature',
    emojis: [
      '🐶', '🐱', '🐭', '🐹', '🐰', '🦊', '🐻', '🐼', '🐻‍❄️', '🐨',
      '🐯', '🦁', '🐮', '🐷', '🐸', '🐵', '🐔', '🐧', '🐦', '🦆',
      '🦅', '🦉', '🦇', '🐺', '🐗', '🐴', '🦄', '🐝', '🐛', '🦋'
    ]
  },
  {
    name: 'الطعام والشراب',
    nameEn: 'Food & Drinks',
    emojis: [
      '🍏', '🍎', '🍐', '🍊', '🍋', '🍌', '🍉', '🍇', '🍓', '🫐',
      '🍒', '🍑', '🥭', '🍍', '🥥', '🥝', '🍅', '🥑', '🍔', '🍕',
      '🍟', '🌭', '🍿', '🍣', '🍦', '🍩', '🍪', '🎂', '☕', '🧋'
    ]
  }
];

const TELEGRAM_STICKER_SETS = [
  {
    name: 'Telegram Duck',
    stickers: [
      { id: 'duck_hello', emoji: '👋', name: 'Duck Hello', img: 'https://images.unsplash.com/photo-1555861496-0666c8981751?w=240&auto=format&fit=crop&q=80' },
      { id: 'duck_love', emoji: '❤️', name: 'Duck Heart', img: 'https://images.unsplash.com/photo-1557971370-e7298ee473fb?w=240&auto=format&fit=crop&q=80' },
      { id: 'duck_cool', emoji: '😎', name: 'Duck Shades', img: 'https://images.unsplash.com/photo-1563889362352-b0492c224f61?w=240&auto=format&fit=crop&q=80' },
      { id: 'duck_surprised', emoji: '😮', name: 'Duck Wow', img: 'https://images.unsplash.com/photo-1582845512747-e42001c95638?w=240&auto=format&fit=crop&q=80' },
    ]
  },
  {
    name: 'Smart Mascot',
    stickers: [
      { id: 'cat_wink', emoji: '😉', name: 'Cat Wink', img: 'https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=240&auto=format&fit=crop&q=80' },
      { id: 'dog_celebrate', emoji: '🥳', name: 'Dog Celebrate', img: 'https://images.unsplash.com/photo-1543466835-00a7907e9de1?w=240&auto=format&fit=crop&q=80' },
      { id: 'fox_ponder', emoji: '🤔', name: 'Fox Think', img: 'https://images.unsplash.com/photo-1474511320723-9a56873867b5?w=240&auto=format&fit=crop&q=80' },
      { id: 'rabbit_jump', emoji: '🐰', name: 'Joyful Rabbit', img: 'https://images.unsplash.com/photo-1585110396000-c9ffd4e4b308?w=240&auto=format&fit=crop&q=80' },
    ]
  }
];

const ANIMATED_TGS_STICKERS = [
  {
    id: 'tgs_star',
    emoji: '⭐️',
    name: 'Telegram 3D Star',
    isTgs: true,
    lottieData: STAR_LOTTIE,
    img: '',
  },
  {
    id: 'tgs_heart',
    emoji: '❤️',
    name: 'Animated Heart',
    isTgs: true,
    lottieData: HEART_LOTTIE,
    img: '',
  },
  {
    id: 'tgs_fire',
    emoji: '🔥',
    name: 'Blazing Fire',
    isTgs: true,
    lottieData: FIRE_LOTTIE,
    img: '',
  },
  {
    id: 'tgs_thumbs',
    emoji: '👍',
    name: 'Vector Thumbs',
    isTgs: true,
    lottieData: THUMBS_UP_LOTTIE,
    img: '',
  },
  {
    id: 'tgs_party',
    emoji: '🎉',
    name: 'Party Burst',
    isTgs: true,
    lottieData: PARTY_LOTTIE,
    img: '',
  },
];

const CURATED_GIFS = [
  { id: 'gif_cheer', title: 'Celebrate 🎉', url: 'https://media.giphy.com/media/26u4cqiYI30juCOGY/giphy.gif' },
  { id: 'gif_applause', title: 'Applause 👏', url: 'https://media.giphy.com/media/l3q2XhfQ8oCkm1Ts4/giphy.gif' },
  { id: 'gif_thumbs', title: 'Thumbs Up 👍', url: 'https://media.giphy.com/media/111ebonMs90YLu/giphy.gif' },
  { id: 'gif_dance', title: 'Dancing Cat 🐱', url: 'https://media.giphy.com/media/JIX9t2j0ZTN9S/giphy.gif' },
  { id: 'gif_mindblown', title: 'Mind Blown 🤯', url: 'https://media.giphy.com/media/xT0xeJpnrWC4XWblEk/giphy.gif' },
  { id: 'gif_yes', title: 'Yes! 🚀', url: 'https://media.giphy.com/media/nXxOjZrbnbRxS/giphy.gif' }
];

export const StickerAndGifDrawer: React.FC<StickerAndGifDrawerProps> = ({
  isOpen,
  onClose,
  onSelectEmoji,
  onSelectSticker,
  onSelectGif,
  isDark,
  lang,
}) => {
  const isAr = lang === 'ar';
  const [activeTab, setActiveTab] = useState<'emoji' | 'stickers' | 'gifs'>('emoji');
  const [searchQuery, setSearchQuery] = useState('');

  if (!isOpen) return null;

  return (
    <div
      className={`absolute bottom-full mb-3 left-2 right-2 sm:left-auto sm:right-6 sm:w-96 rounded-2xl shadow-2xl border flex flex-col z-50 overflow-hidden transition-all animate-in fade-in slide-in-from-bottom-2 duration-150 ${
        isDark ? 'bg-[#1e2c3a] border-gray-700/70 text-white' : 'bg-white border-gray-200 text-gray-900'
      }`}
      style={{ maxHeight: '380px', height: '380px' }}
      onClick={(e) => e.stopPropagation()}
    >
      {/* Header Tabs */}
      <div className={`flex items-center justify-between px-3 py-2 border-b shrink-0 ${isDark ? 'border-gray-700/60 bg-[#17212b]' : 'border-gray-100 bg-gray-50'}`}>
        <div className="flex items-center gap-1">
          <button
            type="button"
            onClick={() => setActiveTab('emoji')}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-medium transition-colors ${
              activeTab === 'emoji'
                ? 'bg-[#3390ec] text-white shadow-sm'
                : isDark
                ? 'text-gray-400 hover:text-white hover:bg-white/5'
                : 'text-gray-600 hover:text-gray-900 hover:bg-gray-200'
            }`}
          >
            <Smile className="w-3.5 h-3.5" />
            <span>{isAr ? 'رموز' : 'Emoji'}</span>
          </button>

          <button
            type="button"
            onClick={() => setActiveTab('stickers')}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-medium transition-colors ${
              activeTab === 'stickers'
                ? 'bg-[#3390ec] text-white shadow-sm'
                : isDark
                ? 'text-gray-400 hover:text-white hover:bg-white/5'
                : 'text-gray-600 hover:text-gray-900 hover:bg-gray-200'
            }`}
          >
            <Sticker className="w-3.5 h-3.5" />
            <span>{isAr ? 'ملصقات' : 'Stickers'}</span>
          </button>

          <button
            type="button"
            onClick={() => setActiveTab('gifs')}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-medium transition-colors ${
              activeTab === 'gifs'
                ? 'bg-[#3390ec] text-white shadow-sm'
                : isDark
                ? 'text-gray-400 hover:text-white hover:bg-white/5'
                : 'text-gray-600 hover:text-gray-900 hover:bg-gray-200'
            }`}
          >
            <Film className="w-3.5 h-3.5" />
            <span>GIFs</span>
          </button>
        </div>

        <button
          type="button"
          onClick={onClose}
          className="p-1 rounded-full text-gray-400 hover:text-gray-200 hover:bg-white/10 transition-colors"
        >
          <X className="w-4 h-4" />
        </button>
      </div>

      {/* Search Bar */}
      <div className={`p-2 border-b shrink-0 ${isDark ? 'border-gray-800 bg-[#1e2c3a]' : 'border-gray-100 bg-white'}`}>
        <div className={`flex items-center gap-2 px-2.5 py-1.5 rounded-lg text-xs ${isDark ? 'bg-[#242f3d] text-white' : 'bg-gray-100 text-gray-800'}`}>
          <Search className="w-3.5 h-3.5 text-gray-400 shrink-0" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder={
              activeTab === 'emoji'
                ? isAr ? 'بحث في الرموز التعبيرية...' : 'Search emojis...'
                : activeTab === 'stickers'
                ? isAr ? 'بحث في حزم الملصقات...' : 'Search stickers...'
                : isAr ? 'بحث في صور GIF...' : 'Search GIFs...'
            }
            className="w-full bg-transparent border-none outline-none placeholder-gray-400 text-xs"
          />
          {searchQuery && (
            <button type="button" onClick={() => setSearchQuery('')} className="text-gray-400 hover:text-white">
              <X className="w-3 h-3" />
            </button>
          )}
        </div>
      </div>

      {/* Tab Content */}
      <div className="flex-1 overflow-y-auto p-3 custom-scrollbar">
        {/* EMOJIS TAB */}
        {activeTab === 'emoji' && (
          <div className="space-y-4">
            {EMOJI_CATEGORIES.map((cat, idx) => {
              const filteredEmojis = searchQuery
                ? cat.emojis.filter((e) => e.includes(searchQuery))
                : cat.emojis;

              if (filteredEmojis.length === 0) return null;

              return (
                <div key={idx} className="space-y-1.5">
                  <div className="text-[11px] font-semibold text-gray-400 uppercase tracking-wider">
                    {isAr ? cat.name : cat.nameEn}
                  </div>
                  <div className="grid grid-cols-8 gap-1">
                    {filteredEmojis.map((emoji, eIdx) => (
                      <button
                        key={eIdx}
                        type="button"
                        onClick={() => onSelectEmoji(emoji)}
                        className="w-9 h-9 flex items-center justify-center text-xl rounded-lg hover:bg-black/10 dark:hover:bg-white/10 hover:scale-125 transition-transform"
                      >
                        {emoji}
                      </button>
                    ))}
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {/* STICKERS TAB */}
        {activeTab === 'stickers' && (
          <div className="space-y-4">
            {/* Animated Telegram TGS Stickers Pack */}
            <div className="space-y-2 p-2 rounded-2xl bg-amber-500/5 border border-amber-500/20">
              <div className="text-xs font-bold text-amber-400 flex items-center justify-between">
                <span className="flex items-center gap-1.5">
                  <Sparkles className="w-3.5 h-3.5" />
                  <span>{isAr ? 'ملصقات متحركة ثلاثية الأبعاد (TGS)' : '3D Animated Stickers (.TGS)'}</span>
                </span>
                <span className="text-[10px] px-2 py-0.5 rounded-full bg-amber-400/20 text-amber-300 font-semibold">
                  RLottie WASM
                </span>
              </div>
              <div className="grid grid-cols-5 gap-2">
                {ANIMATED_TGS_STICKERS.map((stk) => (
                  <button
                    key={stk.id}
                    type="button"
                    onClick={() => onSelectSticker(stk)}
                    className="group p-1.5 rounded-xl hover:bg-amber-400/10 dark:hover:bg-white/10 flex flex-col items-center justify-center gap-1 transition-all active:scale-95 border border-transparent hover:border-amber-400/30"
                    title={stk.name}
                  >
                    <div className="w-12 h-12 flex items-center justify-center group-hover:scale-110 transition-transform">
                      <TgsPlayer animationData={stk.lottieData} width={48} height={48} loop={true} />
                    </div>
                    <span className="text-[10px] text-gray-400 truncate max-w-full font-medium">
                      {stk.name.split(' ')[0]}
                    </span>
                  </button>
                ))}
              </div>
            </div>

            {TELEGRAM_STICKER_SETS.map((set, sIdx) => (
              <div key={sIdx} className="space-y-2">
                <div className="text-xs font-semibold text-gray-400 flex items-center justify-between">
                  <span>{set.name}</span>
                  <span className="text-[10px] text-[#3390ec] font-normal">{isAr ? 'حزمة رسمية' : 'Official Pack'}</span>
                </div>
                <div className="grid grid-cols-4 gap-2">
                  {set.stickers.map((stk) => (
                    <button
                      key={stk.id}
                      type="button"
                      onClick={() => onSelectSticker(stk)}
                      className="group p-1.5 rounded-xl hover:bg-black/10 dark:hover:bg-white/10 flex flex-col items-center gap-1 transition-all active:scale-95"
                    >
                      <img
                        src={stk.img}
                        alt={stk.name}
                        className="w-16 h-16 object-cover rounded-lg shadow-sm group-hover:scale-105 transition-transform"
                      />
                      <span className="text-[10px] text-gray-400 truncate max-w-full">{stk.emoji} {stk.name}</span>
                    </button>
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}

        {/* GIFS TAB */}
        {activeTab === 'gifs' && (
          <div className="grid grid-cols-2 gap-2">
            {CURATED_GIFS.map((gif) => (
              <button
                key={gif.id}
                type="button"
                onClick={() => onSelectGif(gif)}
                className="group relative rounded-xl overflow-hidden aspect-video bg-black/20 hover:opacity-90 transition-all active:scale-95"
              >
                <img
                  src={gif.url}
                  alt={gif.title}
                  className="w-full h-full object-cover group-hover:scale-105 transition-transform"
                  loading="lazy"
                />
                <div className="absolute inset-x-0 bottom-0 p-1.5 bg-gradient-to-t from-black/80 to-transparent text-[10px] text-white font-medium truncate">
                  {gif.title}
                </div>
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
