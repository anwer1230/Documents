import React, { useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { TgsPlayer } from './TgsPlayer';
import { getVectorAnimationForEmoji } from '../utils/tgsAnimations';

export interface ReactionBurstItem {
  id: string;
  emoji: string;
  x: number;
  y: number;
}

interface AnimatedReactionBurstProps {
  bursts: ReactionBurstItem[];
  onRemoveBurst: (id: string) => void;
}

export const AnimatedReactionBurst: React.FC<AnimatedReactionBurstProps> = ({
  bursts,
  onRemoveBurst,
}) => {
  return (
    <div className="fixed inset-0 pointer-events-none z-50 overflow-hidden">
      <AnimatePresence>
        {bursts.map((burst) => {
          const lottieData = getVectorAnimationForEmoji(burst.emoji);
          return (
            <motion.div
              key={burst.id}
              initial={{ scale: 0.2, opacity: 0, x: burst.x - 48, y: burst.y - 48 }}
              animate={{
                scale: [0.2, 1.4, 1.1, 1.2],
                opacity: [0, 1, 1, 0],
                y: burst.y - 120,
              }}
              exit={{ opacity: 0, scale: 0.5 }}
              transition={{ duration: 1.4, ease: 'easeOut' }}
              onAnimationComplete={() => onRemoveBurst(burst.id)}
              className="absolute flex flex-col items-center justify-center drop-shadow-2xl"
            >
              {/* 3D Vector Lottie Animation */}
              <div className="relative">
                <TgsPlayer
                  animationData={lottieData}
                  width={96}
                  height={96}
                  loop={false}
                  autoplay={true}
                  fallbackEmoji={burst.emoji}
                />
                
                {/* Orbital 3D Micro-Sparkles */}
                {[...Array(6)].map((_, i) => {
                  const angle = (i * 60 * Math.PI) / 180;
                  const distance = 40;
                  return (
                    <motion.div
                      key={i}
                      initial={{ scale: 0, opacity: 1, x: 0, y: 0 }}
                      animate={{
                        x: Math.cos(angle) * distance,
                        y: Math.sin(angle) * distance,
                        scale: [0, 1.2, 0],
                        opacity: [1, 1, 0],
                      }}
                      transition={{ duration: 0.8, delay: 0.1 * i, ease: 'easeOut' }}
                      className="absolute top-1/2 left-1/2 w-2 h-2 rounded-full bg-amber-300 shadow-[0_0_8px_#fde047]"
                    />
                  );
                })}
              </div>
            </motion.div>
          );
        })}
      </AnimatePresence>
    </div>
  );
};
