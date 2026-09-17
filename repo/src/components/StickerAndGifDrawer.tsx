import React, { useState } from 'react';
import { Smile, Image as ImageIcon, Film, Search, X } from 'lucide-react';

interface StickerItem {
  id: string;
  emoji: string;
  name: string;
  img: string;
}

interface GifItem {
  id: string;
  url: string;
  title: string;
}

interface StickerAndGifDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  onSelectEmoji: (emoji: string) => void;
  onSelectSticker: (sticker: StickerItem) => void;
  onSelectGif: (gif: GifItem) => void;
  isDark?: boolean;
  lang?: 'ar' | 'en';
}

export const StickerAndGifDrawer: React.FC<StickerAndGifDrawerProps> = ({
  isOpen,
  onClose,
  onSelectEmoji,
  onSelectSticker,
  onSelectGif,
  isDark = true,
  lang = 'ar',
}) => {
  const [activeTab, setActiveTab] = useState<'emoji' | 'stickers' | 'gifs'>('emoji');
  const [search, setSearch] = useState('');
  const isAr = lang === 'ar';

  if (!isOpen) return null;

  const popularEmojis = [
    '👍', '❤️', '🔥', '🎉', '🚀', '⭐', '👏', '😍', '😂', '🥳', '😎', '🙏',
    '✨', '💯', '🤔', '🙌', '💪', '🤩', '💡', '👌', '⚡', '😇', '🤝', '🎯',
    '💬', '☕', '🌟', '🏆', '🎈', '🥰', '🤗', '💖', '💐', '🌹', '🌈', '💎',
  ];

  const stickers: StickerItem[] = [
    {
      id: 'st_duck_1',
      emoji: '🦆',
      name: 'Happy Duck',
      img: 'https://images.unsplash.com/photo-1555852095-64e7428df0fa?w=200&auto=format&fit=crop&q=80',
    },
    {
      id: 'st_dog_1',
      emoji: '🐶',
      name: 'Friendly Dog',
      img: 'https://images.unsplash.com/photo-1543466835-00a7907e9de1?w=200&auto=format&fit=crop&q=80',
    },
    {
      id: 'st_cat_1',
      emoji: '🐱',
      name: 'Cool Cat',
      img: 'https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=200&auto=format&fit=crop&q=80',
    },
    {
      id: 'st_fox_1',
      emoji: '🦊',
      name: 'Smart Fox',
      img: 'https://images.unsplash.com/photo-1516934024742-b461fba47600?w=200&auto=format&fit=crop&q=80',
    },
    {
      id: 'st_rabbit_1',
      emoji: '🐰',
      name: 'Cheerful Bunny',
      img: 'https://images.unsplash.com/photo-1585110396000-c9ffd4e4b308?w=200&auto=format&fit=crop&q=80',
    },
    {
      id: 'st_bear_1',
      emoji: '🐻',
      name: 'Strong Bear',
      img: 'https://images.unsplash.com/photo-1530595467537-0b5996c41f2d?w=200&auto=format&fit=crop&q=80',
    },
  ];

  const gifs: GifItem[] = [
    {
      id: 'gif_party',
      url: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300&auto=format&fit=crop&q=80',
      title: 'Party & Celebration 🎉',
    },
    {
      id: 'gif_thumbs',
      url: 'https://images.unsplash.com/photo-1584447141267-3c72b223cb60?w=300&auto=format&fit=crop&q=80',
      title: 'Thumbs Up 👍',
    },
    {
      id: 'gif_rocket',
      url: 'https://images.unsplash.com/photo-1517976487541-112df8b1a8d0?w=300&auto=format&fit=crop&q=80',
      title: 'Rocket Launch 🚀',
    },
    {
      id: 'gif_fire',
      url: 'https://images.unsplash.com/photo-1496337589254-7e19d01cec44?w=300&auto=format&fit=crop&q=80',
      title: 'Awesome Fire 🔥',
    },
  ];

  return (
    <div
      className={`absolute bottom-full mb-2 left-2 right-2 sm:left-auto sm:right-4 sm:w-80 h-72 rounded-2xl shadow-2xl border flex flex-col overflow-hidden z-40 backdrop-blur-md transition-all animate-fade-in ${
        isDark ? 'bg-[#17212b]/95 border-[#242f3d] text-white' : 'bg-white/95 border-gray-200 text-gray-900'
      }`}
    >
      {/* Top Tabs Header */}
      <div className="flex items-center justify-between p-2 border-b border-gray-700/30">
        <div className="flex items-center gap-1 bg-black/20 p-1 rounded-xl">
          <button
            onClick={() => setActiveTab('emoji')}
            className={`px-3 py-1 rounded-lg text-xs font-bold flex items-center gap-1.5 transition ${
              activeTab === 'emoji'
                ? 'bg-[#3390ec] text-white'
                : 'text-gray-400 hover:text-white'
            }`}
          >
            <Smile className="w-3.5 h-3.5" />
            <span>{isAr ? 'الرموز' : 'Emoji'}</span>
          </button>
          <button
            onClick={() => setActiveTab('stickers')}
            className={`px-3 py-1 rounded-lg text-xs font-bold flex items-center gap-1.5 transition ${
              activeTab === 'stickers'
                ? 'bg-[#3390ec] text-white'
                : 'text-gray-400 hover:text-white'
            }`}
          >
            <ImageIcon className="w-3.5 h-3.5" />
            <span>{isAr ? 'الملصقات' : 'Stickers'}</span>
          </button>
          <button
            onClick={() => setActiveTab('gifs')}
            className={`px-3 py-1 rounded-lg text-xs font-bold flex items-center gap-1.5 transition ${
              activeTab === 'gifs'
                ? 'bg-[#3390ec] text-white'
                : 'text-gray-400 hover:text-white'
            }`}
          >
            <Film className="w-3.5 h-3.5" />
            <span>{isAr ? 'متحركة' : 'GIFs'}</span>
          </button>
        </div>

        <button
          onClick={onClose}
          className="p-1 rounded-lg hover:bg-white/10 text-gray-400 hover:text-white transition"
        >
          <X className="w-4 h-4" />
        </button>
      </div>

      {/* Content Area */}
      <div className="flex-1 overflow-y-auto p-3">
        {activeTab === 'emoji' && (
          <div className="grid grid-cols-6 gap-2">
            {popularEmojis.map((emoji, i) => (
              <button
                key={i}
                onClick={() => onSelectEmoji(emoji)}
                className="w-10 h-10 rounded-xl hover:bg-white/10 active:scale-125 flex items-center justify-center text-xl transition select-none"
              >
                {emoji}
              </button>
            ))}
          </div>
        )}

        {activeTab === 'stickers' && (
          <div className="grid grid-cols-3 gap-2.5">
            {stickers.map((st) => (
              <button
                key={st.id}
                onClick={() => {
                  onSelectSticker(st);
                  onClose();
                }}
                className="group p-2 rounded-xl hover:bg-white/10 transition flex flex-col items-center gap-1 text-center"
              >
                <img
                  src={st.img}
                  alt={st.name}
                  className="w-14 h-14 object-cover rounded-lg group-hover:scale-110 transition"
                />
                <span className="text-[10px] text-gray-400 truncate max-w-full">
                  {st.emoji} {st.name}
                </span>
              </button>
            ))}
          </div>
        )}

        {activeTab === 'gifs' && (
          <div className="grid grid-cols-2 gap-2">
            {gifs.map((g) => (
              <button
                key={g.id}
                onClick={() => {
                  onSelectGif(g);
                  onClose();
                }}
                className="relative rounded-xl overflow-hidden aspect-video group hover:opacity-90 transition border border-gray-700/50"
              >
                <img src={g.url} alt={g.title} className="w-full h-full object-cover group-hover:scale-105 transition" />
                <span className="absolute bottom-1 inset-x-1 text-[9px] font-bold text-white bg-black/60 px-1 py-0.5 rounded truncate">
                  {g.title}
                </span>
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
