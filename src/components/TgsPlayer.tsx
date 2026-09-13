import React, { useEffect, useRef, useState } from 'react';
import lottie, { AnimationItem } from 'lottie-web';
import { inflate } from 'pako';

interface TgsPlayerProps {
  src?: string;
  animationData?: any;
  width?: number | string;
  height?: number | string;
  autoplay?: boolean;
  loop?: boolean;
  className?: string;
  onClick?: () => void;
  onComplete?: () => void;
  fallbackEmoji?: string;
}

/**
 * TgsPlayer: Renders Telegram .tgs vector animations & Lottie JSON.
 * Automatically decompresses gzip-compressed .tgs files into Lottie JSON
 * and renders at 60 FPS vector quality.
 */
export const TgsPlayer: React.FC<TgsPlayerProps> = ({
  src,
  animationData,
  width = 128,
  height = 128,
  autoplay = true,
  loop = true,
  className = '',
  onClick,
  onComplete,
  fallbackEmoji = '✨',
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const animItemRef = useRef<AnimationItem | null>(null);
  const [hasError, setHasError] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let isCancelled = false;

    const loadAndPlay = async () => {
      setIsLoading(true);
      setHasError(false);

      if (animItemRef.current) {
        animItemRef.current.destroy();
        animItemRef.current = null;
      }

      if (!containerRef.current) return;

      try {
        let finalData = animationData;

        if (!finalData && src) {
          const isTgs = src.endsWith('.tgs') || src.includes('.tgs?') || src.startsWith('data:application/x-tgsticker');

          const response = await fetch(src);
          if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);

          if (isTgs) {
            const arrayBuffer = await response.arrayBuffer();
            const uint8Array = new Uint8Array(arrayBuffer);
            try {
              const decompressed = inflate(uint8Array);
              const decompressedText = new TextDecoder('utf-8').decode(decompressed);
              finalData = JSON.parse(decompressedText);
            } catch (decompError) {
              // Try text fallback in case server already unzipped
              const text = new TextDecoder('utf-8').decode(uint8Array);
              finalData = JSON.parse(text);
            }
          } else {
            finalData = await response.json();
          }
        }

        if (isCancelled || !containerRef.current || !finalData) return;

        animItemRef.current = lottie.loadAnimation({
          container: containerRef.current,
          renderer: 'svg',
          loop,
          autoplay,
          animationData: finalData,
        });

        if (onComplete) {
          animItemRef.current.addEventListener('complete', onComplete);
        }

        setIsLoading(false);
      } catch (err) {
        if (!isCancelled) {
          console.warn('[TgsPlayer] Could not render TGS vector animation:', err);
          setHasError(true);
          setIsLoading(false);
        }
      }
    };

    loadAndPlay();

    return () => {
      isCancelled = true;
      if (animItemRef.current) {
        animItemRef.current.destroy();
        animItemRef.current = null;
      }
    };
  }, [src, animationData, loop, autoplay]);

  const styleDimension = (dim: number | string) =>
    typeof dim === 'number' ? `${dim}px` : dim;

  if (hasError) {
    return (
      <div
        onClick={onClick}
        className={`flex items-center justify-center select-none text-2xl cursor-pointer ${className}`}
        style={{ width: styleDimension(width), height: styleDimension(height) }}
      >
        {fallbackEmoji}
      </div>
    );
  }

  return (
    <div
      ref={containerRef}
      onClick={onClick}
      className={`relative select-none overflow-hidden ${className}`}
      style={{
        width: styleDimension(width),
        height: styleDimension(height),
      }}
    >
      {isLoading && (
        <div className="absolute inset-0 flex items-center justify-center opacity-40 animate-pulse text-lg">
          {fallbackEmoji}
        </div>
      )}
    </div>
  );
};
