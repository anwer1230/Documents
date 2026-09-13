/**
 * Telegram Vector Lottie animations for .tgs stickers & 3D Vector Reactions
 */

// Vector Star animation data (3D Telegram Star)
export const STAR_LOTTIE = {
  v: "5.7.4",
  fr: 60,
  ip: 0,
  op: 120,
  w: 200,
  h: 200,
  nm: "Telegram 3D Star",
  ddd: 0,
  assets: [],
  layers: [
    {
      ddd: 0,
      ind: 1,
      ty: 4,
      nm: "Star Glow",
      sr: 1,
      ks: {
        o: {
          k: [
            { t: 0, s: [40], h: 0 },
            { t: 60, s: [100], h: 0 },
            { t: 120, s: [40], h: 0 }
          ]
        },
        r: {
          k: [
            { t: 0, s: [0], h: 0 },
            { t: 120, s: [360], h: 0 }
          ]
        },
        p: { k: [100, 100, 0] },
        a: { k: [0, 0, 0] },
        s: {
          k: [
            { t: 0, s: [80, 80, 100], h: 0 },
            { t: 60, s: [115, 115, 100], h: 0 },
            { t: 120, s: [80, 80, 100], h: 0 }
          ]
        }
      },
      ao: 0,
      shapes: [
        {
          ty: "gr",
          it: [
            {
              ty: "sr",
              sy: 1,
              d: 1,
              pt: { k: 5 },
              p: { k: [0, 0] },
              r: { k: 0 },
              ir: { k: 32 },
              is: { k: 0 },
              or: { k: 70 },
              os: { k: 0 },
              nm: "Star Shape"
            },
            {
              ty: "fl",
              c: { k: [1, 0.78, 0.1, 1] },
              o: { k: 100 },
              r: 1,
              nm: "Fill"
            },
            {
              ty: "tr",
              p: { k: [0, 0] },
              a: { k: [0, 0] },
              s: { k: [100, 100] },
              r: { k: 0 },
              o: { k: 100 }
            }
          ]
        }
      ]
    },
    {
      ddd: 0,
      ind: 2,
      ty: 4,
      nm: "Star Core",
      sr: 1,
      ks: {
        o: { k: 100 },
        r: {
          k: [
            { t: 0, s: [0], h: 0 },
            { t: 30, s: [-8], h: 0 },
            { t: 90, s: [8], h: 0 },
            { t: 120, s: [0], h: 0 }
          ]
        },
        p: { k: [100, 100, 0] },
        a: { k: [0, 0, 0] },
        s: {
          k: [
            { t: 0, s: [90, 90, 100], h: 0 },
            { t: 60, s: [105, 105, 100], h: 0 },
            { t: 120, s: [90, 90, 100], h: 0 }
          ]
        }
      },
      ao: 0,
      shapes: [
        {
          ty: "gr",
          it: [
            {
              ty: "sr",
              sy: 1,
              d: 1,
              pt: { k: 5 },
              p: { k: [0, 0] },
              r: { k: 0 },
              ir: { k: 25 },
              is: { k: 0 },
              or: { k: 55 },
              os: { k: 0 },
              nm: "Inner Star"
            },
            {
              ty: "fl",
              c: { k: [1, 0.92, 0.35, 1] },
              o: { k: 100 },
              r: 1,
              nm: "Core Fill"
            },
            {
              ty: "tr",
              p: { k: [0, 0] },
              a: { k: [0, 0] },
              s: { k: [100, 100] },
              r: { k: 0 },
              o: { k: 100 }
            }
          ]
        }
      ]
    }
  ]
};

// Vector Heart Animation (3D Pulse)
export const HEART_LOTTIE = {
  v: "5.7.4",
  fr: 60,
  ip: 0,
  op: 90,
  w: 200,
  h: 200,
  nm: "Telegram 3D Heart",
  ddd: 0,
  assets: [],
  layers: [
    {
      ddd: 0,
      ind: 1,
      ty: 4,
      nm: "Heart Shape",
      sr: 1,
      ks: {
        o: { k: 100 },
        r: { k: 0 },
        p: { k: [100, 100, 0] },
        a: { k: [0, 0, 0] },
        s: {
          k: [
            { t: 0, s: [85, 85, 100], h: 0 },
            { t: 25, s: [115, 115, 100], h: 0 },
            { t: 45, s: [95, 95, 100], h: 0 },
            { t: 65, s: [120, 120, 100], h: 0 },
            { t: 90, s: [85, 85, 100], h: 0 }
          ]
        }
      },
      ao: 0,
      shapes: [
        {
          ty: "gr",
          it: [
            {
              ty: "rc",
              d: 1,
              s: { k: [70, 70] },
              p: { k: [0, 0] },
              r: { k: 35 },
              nm: "Round Base"
            },
            {
              ty: "fl",
              c: { k: [0.95, 0.15, 0.28, 1] },
              o: { k: 100 },
              r: 1,
              nm: "Heart Red"
            },
            {
              ty: "tr",
              p: { k: [0, 0] },
              a: { k: [0, 0] },
              s: { k: [100, 100] },
              r: { k: 45 },
              o: { k: 100 }
            }
          ]
        }
      ]
    }
  ]
};

// Vector Flame / Fire Animation
export const FIRE_LOTTIE = {
  v: "5.7.4",
  fr: 60,
  ip: 0,
  op: 80,
  w: 200,
  h: 200,
  nm: "Telegram 3D Fire",
  ddd: 0,
  assets: [],
  layers: [
    {
      ddd: 0,
      ind: 1,
      ty: 4,
      nm: "Flame",
      sr: 1,
      ks: {
        o: { k: 100 },
        r: {
          k: [
            { t: 0, s: [-4], h: 0 },
            { t: 40, s: [6], h: 0 },
            { t: 80, s: [-4], h: 0 }
          ]
        },
        p: { k: [100, 105, 0] },
        a: { k: [0, 0, 0] },
        s: {
          k: [
            { t: 0, s: [90, 95, 100], h: 0 },
            { t: 40, s: [105, 115, 100], h: 0 },
            { t: 80, s: [90, 95, 100], h: 0 }
          ]
        }
      },
      ao: 0,
      shapes: [
        {
          ty: "gr",
          it: [
            {
              ty: "el",
              d: 1,
              p: { k: [0, 0] },
              s: { k: [70, 90] },
              nm: "Flame Core"
            },
            {
              ty: "fl",
              c: { k: [1, 0.42, 0.05, 1] },
              o: { k: 100 },
              r: 1,
              nm: "Fire Fill"
            },
            {
              ty: "tr",
              p: { k: [0, 0] },
              a: { k: [0, 0] },
              s: { k: [100, 100] },
              r: { k: 0 },
              o: { k: 100 }
            }
          ]
        }
      ]
    }
  ]
};

// Vector Thumbs Up / Like
export const THUMBS_LOTTIE = {
  v: "5.7.4",
  fr: 60,
  ip: 0,
  op: 80,
  w: 200,
  h: 200,
  nm: "Telegram 3D Thumbs Up",
  ddd: 0,
  assets: [],
  layers: [
    {
      ddd: 0,
      ind: 1,
      ty: 4,
      nm: "Thumb",
      sr: 1,
      ks: {
        o: { k: 100 },
        r: {
          k: [
            { t: 0, s: [0], h: 0 },
            { t: 20, s: [-18], h: 0 },
            { t: 50, s: [10], h: 0 },
            { t: 80, s: [0], h: 0 }
          ]
        },
        p: { k: [100, 100, 0] },
        a: { k: [0, 0, 0] },
        s: {
          k: [
            { t: 0, s: [90, 90, 100], h: 0 },
            { t: 40, s: [115, 115, 100], h: 0 },
            { t: 80, s: [90, 90, 100], h: 0 }
          ]
        }
      },
      ao: 0,
      shapes: [
        {
          ty: "gr",
          it: [
            {
              ty: "rc",
              d: 1,
              s: { k: [55, 65] },
              p: { k: [0, 10] },
              r: { k: 12 },
              nm: "Hand Base"
            },
            {
              ty: "rc",
              d: 1,
              s: { k: [26, 45] },
              p: { k: [-15, -25] },
              r: { k: 10 },
              nm: "Thumb Up"
            },
            {
              ty: "fl",
              c: { k: [0.2, 0.56, 0.92, 1] },
              o: { k: 100 },
              r: 1,
              nm: "Hand Fill"
            },
            {
              ty: "tr",
              p: { k: [0, 0] },
              a: { k: [0, 0] },
              s: { k: [100, 100] },
              r: { k: 0 },
              o: { k: 100 }
            }
          ]
        }
      ]
    }
  ]
};

// Vector Rocket Animation
export const ROCKET_LOTTIE = {
  v: "5.7.4",
  fr: 60,
  ip: 0,
  op: 100,
  w: 200,
  h: 200,
  nm: "Telegram 3D Rocket",
  ddd: 0,
  assets: [],
  layers: [
    {
      ddd: 0,
      ind: 1,
      ty: 4,
      nm: "Rocket Body",
      sr: 1,
      ks: {
        o: { k: 100 },
        r: {
          k: [
            { t: 0, s: [45], h: 0 },
            { t: 50, s: [42], h: 0 },
            { t: 100, s: [45], h: 0 }
          ]
        },
        p: {
          k: [
            { t: 0, s: [95, 105, 0], h: 0 },
            { t: 50, s: [105, 95, 0], h: 0 },
            { t: 100, s: [95, 105, 0], h: 0 }
          ]
        },
        a: { k: [0, 0, 0] },
        s: { k: [100, 100, 100] }
      },
      ao: 0,
      shapes: [
        {
          ty: "gr",
          it: [
            {
              ty: "el",
              d: 1,
              p: { k: [0, 0] },
              s: { k: [40, 80] },
              nm: "Rocket Fuselage"
            },
            {
              ty: "fl",
              c: { k: [0.95, 0.25, 0.25, 1] },
              o: { k: 100 },
              r: 1,
              nm: "Rocket Red"
            },
            {
              ty: "tr",
              p: { k: [0, 0] },
              a: { k: [0, 0] },
              s: { k: [100, 100] },
              r: { k: 0 },
              o: { k: 100 }
            }
          ]
        }
      ]
    }
  ]
};

// Telegram Official Mascot Duck Animated Sticker (TGS Vector)
export const DUCK_HELLO_LOTTIE = {
  v: "5.7.4",
  fr: 60,
  ip: 0,
  op: 120,
  w: 250,
  h: 250,
  nm: "Telegram Duck Hello .tgs",
  ddd: 0,
  assets: [],
  layers: [
    {
      ddd: 0,
      ind: 1,
      ty: 4,
      nm: "Duck Head & Body",
      sr: 1,
      ks: {
        o: { k: 100 },
        r: {
          k: [
            { t: 0, s: [-3], h: 0 },
            { t: 30, s: [4], h: 0 },
            { t: 60, s: [-3], h: 0 },
            { t: 90, s: [4], h: 0 },
            { t: 120, s: [-3], h: 0 }
          ]
        },
        p: { k: [125, 135, 0] },
        a: { k: [0, 0, 0] },
        s: {
          k: [
            { t: 0, s: [100, 100, 100], h: 0 },
            { t: 60, s: [105, 102, 100], h: 0 },
            { t: 120, s: [100, 100, 100], h: 0 }
          ]
        }
      },
      ao: 0,
      shapes: [
        {
          ty: "gr",
          it: [
            {
              ty: "el",
              d: 1,
              p: { k: [0, 0] },
              s: { k: [110, 120] },
              nm: "Yellow Body"
            },
            {
              ty: "fl",
              c: { k: [1, 0.84, 0.12, 1] },
              o: { k: 100 },
              r: 1,
              nm: "Duck Yellow"
            },
            {
              ty: "tr",
              p: { k: [0, 0] },
              a: { k: [0, 0] },
              s: { k: [100, 100] },
              r: { k: 0 },
              o: { k: 100 }
            }
          ]
        },
        {
          ty: "gr",
          it: [
            {
              ty: "el",
              d: 1,
              p: { k: [-18, -12] },
              s: { k: [16, 20] },
              nm: "Eye Left"
            },
            {
              ty: "el",
              d: 1,
              p: { k: [18, -12] },
              s: { k: [16, 20] },
              nm: "Eye Right"
            },
            {
              ty: "fl",
              c: { k: [0.1, 0.1, 0.12, 1] },
              o: { k: 100 },
              r: 1,
              nm: "Black Eyes"
            },
            {
              ty: "tr",
              p: { k: [0, 0] },
              a: { k: [0, 0] },
              s: { k: [100, 100] },
              r: { k: 0 },
              o: { k: 100 }
            }
          ]
        },
        {
          ty: "gr",
          it: [
            {
              ty: "el",
              d: 1,
              p: { k: [0, 10] },
              s: { k: [38, 22] },
              nm: "Beak"
            },
            {
              ty: "fl",
              c: { k: [0.98, 0.5, 0.1, 1] },
              o: { k: 100 },
              r: 1,
              nm: "Orange Beak"
            },
            {
              ty: "tr",
              p: { k: [0, 0] },
              a: { k: [0, 0] },
              s: { k: [100, 100] },
              r: { k: 0 },
              o: { k: 100 }
            }
          ]
        }
      ]
    },
    {
      ddd: 0,
      ind: 2,
      ty: 4,
      nm: "Waving Wing",
      sr: 1,
      ks: {
        o: { k: 100 },
        r: {
          k: [
            { t: 0, s: [-30], h: 0 },
            { t: 20, s: [35], h: 0 },
            { t: 40, s: [-30], h: 0 },
            { t: 60, s: [35], h: 0 },
            { t: 80, s: [-30], h: 0 },
            { t: 100, s: [35], h: 0 },
            { t: 120, s: [-30], h: 0 }
          ]
        },
        p: { k: [65, 120, 0] },
        a: { k: [0, 0, 0] },
        s: { k: [100, 100, 100] }
      },
      ao: 0,
      shapes: [
        {
          ty: "gr",
          it: [
            {
              ty: "el",
              d: 1,
              p: { k: [-20, -10] },
              s: { k: [42, 28] },
              nm: "Wing Wing"
            },
            {
              ty: "fl",
              c: { k: [0.95, 0.76, 0.08, 1] },
              o: { k: 100 },
              r: 1,
              nm: "Wing Color"
            },
            {
              ty: "tr",
              p: { k: [0, 0] },
              a: { k: [0, 0] },
              s: { k: [100, 100] },
              r: { k: 0 },
              o: { k: 100 }
            }
          ]
        }
      ]
    }
  ]
};

export const THUMBS_UP_LOTTIE = THUMBS_LOTTIE;
export const PARTY_LOTTIE = ROCKET_LOTTIE;

export const TGS_ANIMATIONS_MAP: Record<string, any> = {
  '⭐': STAR_LOTTIE,
  'star': STAR_LOTTIE,
  '❤️': HEART_LOTTIE,
  'heart': HEART_LOTTIE,
  '🔥': FIRE_LOTTIE,
  'fire': FIRE_LOTTIE,
  '👍': THUMBS_LOTTIE,
  'like': THUMBS_LOTTIE,
  '🚀': ROCKET_LOTTIE,
  'rocket': ROCKET_LOTTIE,
  'duck_hello': DUCK_HELLO_LOTTIE,
  'duck': DUCK_HELLO_LOTTIE,
};

export const getVectorAnimationForEmoji = (emoji: string): any => {
  if (TGS_ANIMATIONS_MAP[emoji]) return TGS_ANIMATIONS_MAP[emoji];
  if (emoji.includes('⭐') || emoji.includes('🌟')) return STAR_LOTTIE;
  if (emoji.includes('❤️') || emoji.includes('💖') || emoji.includes('💕')) return HEART_LOTTIE;
  if (emoji.includes('🔥') || emoji.includes('⚡')) return FIRE_LOTTIE;
  if (emoji.includes('👍') || emoji.includes('👌')) return THUMBS_LOTTIE;
  if (emoji.includes('🚀')) return ROCKET_LOTTIE;
  return STAR_LOTTIE;
};
