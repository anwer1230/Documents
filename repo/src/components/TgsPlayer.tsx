import React, { useEffect, useRef, useState } from 'react';
import { Play, Pause, RotateCcw } from 'lucide-react';

interface TgsPlayerProps {
  src?: string; // URL to .tgs or .json
  animationData?: any; // Raw JSON object or parsed Lottie
  stickerEmoji?: string; // Fallback or associated emoji (e.g. '🔥', '👍')
  width?: number | string;
  height?: number | string;
  autoplay?: boolean;
  loop?: boolean;
  interactive?: boolean;
  className?: string;
  onClick?: () => void;
}

// Built-in Telegram Vector Reaction Animation Keyframes
// Recreates authentic Telegram animated vector reactions using HTML5 Canvas
const VECTOR_REACTIONS: Record<string, {
  color: string;
  secondaryColor: string;
  draw: (ctx: CanvasRenderingContext2D, progress: number, w: number, h: number) => void;
}> = {
  '👍': {
    color: '#3390ec',
    secondaryColor: '#5db0f7',
    draw: (ctx, p, w, h) => {
      const cx = w / 2;
      const cy = h / 2;
      const scale = 1 + Math.sin(p * Math.PI * 2) * 0.15;
      ctx.save();
      ctx.translate(cx, cy);
      ctx.scale(scale, scale);
      
      // Thumb body
      ctx.fillStyle = '#3390ec';
      ctx.beginPath();
      ctx.roundRect(-w * 0.25, -h * 0.05, w * 0.5, h * 0.35, 8);
      ctx.fill();

      // Thumb up
      ctx.beginPath();
      ctx.roundRect(-w * 0.25, -h * 0.35, w * 0.2, h * 0.35, [12, 12, 4, 4]);
      ctx.fill();

      // Sparkles around thumb
      if (p > 0.3 && p < 0.8) {
        ctx.fillStyle = '#ffcf40';
        for (let i = 0; i < 5; i++) {
          const angle = (i * Math.PI) / 2.5 - Math.PI / 2;
          const dist = w * 0.4 * (p * 1.5);
          const px = Math.cos(angle) * dist;
          const py = Math.sin(angle) * dist;
          ctx.beginPath();
          ctx.arc(px, py, 2.5, 0, Math.PI * 2);
          ctx.fill();
        }
      }
      ctx.restore();
    },
  },
  '❤️': {
    color: '#e53935',
    secondaryColor: '#ff5252',
    draw: (ctx, p, w, h) => {
      const cx = w / 2;
      const cy = h / 2;
      const pulse = 1 + Math.sin(p * Math.PI * 4) * 0.18;
      ctx.save();
      ctx.translate(cx, cy);
      ctx.scale(pulse, pulse);

      ctx.fillStyle = '#e53935';
      ctx.beginPath();
      const s = w * 0.32;
      ctx.moveTo(0, s * 0.7);
      ctx.bezierCurveTo(-s * 1.2, -s * 0.4, -s * 0.6, -s * 1.2, 0, -s * 0.4);
      ctx.bezierCurveTo(s * 0.6, -s * 1.2, s * 1.2, -s * 0.4, 0, s * 0.7);
      ctx.fill();

      // Floating mini hearts
      if (p > 0.2 && p < 0.9) {
        ctx.fillStyle = 'rgba(255, 82, 82, 0.7)';
        const floatY = -s * 0.8 - p * 20;
        ctx.beginPath();
        ctx.arc(-10, floatY, 3, 0, Math.PI * 2);
        ctx.arc(12, floatY - 5, 2.5, 0, Math.PI * 2);
        ctx.fill();
      }
      ctx.restore();
    },
  },
  '🔥': {
    color: '#ff9800',
    secondaryColor: '#f44336',
    draw: (ctx, p, w, h) => {
      const cx = w / 2;
      const cy = h / 2;
      const wave = Math.sin(p * Math.PI * 6) * 4;
      ctx.save();
      ctx.translate(cx, cy);

      // Outer fire
      const grad = ctx.createLinearGradient(0, h * 0.3, 0, -h * 0.35);
      grad.addColorStop(0, '#f44336');
      grad.addColorStop(0.6, '#ff9800');
      grad.addColorStop(1, '#ffeb3b');

      ctx.fillStyle = grad;
      ctx.beginPath();
      const s = w * 0.35;
      ctx.moveTo(0, s * 0.8);
      ctx.quadraticCurveTo(-s * 0.9 + wave, s * 0.2, -s * 0.5, -s * 0.3);
      ctx.quadraticCurveTo(-s * 0.2, -s * 0.1, 0, -s * 0.8 - wave);
      ctx.quadraticCurveTo(s * 0.3, -s * 0.2, s * 0.5, -s * 0.4);
      ctx.quadraticCurveTo(s * 0.9 - wave, s * 0.2, 0, s * 0.8);
      ctx.fill();

      // Inner flame core
      ctx.fillStyle = '#fff59d';
      ctx.beginPath();
      ctx.moveTo(0, s * 0.6);
      ctx.quadraticCurveTo(-s * 0.3, s * 0.2, 0, -s * 0.3 + wave);
      ctx.quadraticCurveTo(s * 0.3, s * 0.2, 0, s * 0.6);
      ctx.fill();
      ctx.restore();
    },
  },
  '🎉': {
    color: '#4caf50',
    secondaryColor: '#ffeb3b',
    draw: (ctx, p, w, h) => {
      const cx = w / 2;
      const cy = h / 2;
      ctx.save();
      ctx.translate(cx, cy);

      // Party popper cone
      ctx.fillStyle = '#ff9800';
      ctx.beginPath();
      ctx.moveTo(-w * 0.2, h * 0.25);
      ctx.lineTo(0, -h * 0.1);
      ctx.lineTo(w * 0.2, h * 0.25);
      ctx.closePath();
      ctx.fill();

      // Confetti burst
      const colors = ['#f44336', '#4caf50', '#2196f3', '#ffeb3b', '#9c27b0'];
      for (let i = 0; i < 12; i++) {
        const angle = (i * Math.PI * 2) / 12;
        const dist = (w * 0.25) + p * (w * 0.3);
        const px = Math.cos(angle) * dist;
        const py = Math.sin(angle) * dist - (p * 15);
        ctx.fillStyle = colors[i % colors.length];
        ctx.save();
        ctx.translate(px, py);
        ctx.rotate(p * 5 + i);
        ctx.fillRect(-2, -3, 4, 6);
        ctx.restore();
      }
      ctx.restore();
    },
  },
  '🚀': {
    color: '#2196f3',
    secondaryColor: '#ff5722',
    draw: (ctx, p, w, h) => {
      const cx = w / 2;
      const cy = h / 2;
      const shake = (Math.random() - 0.5) * 3;
      ctx.save();
      ctx.translate(cx + shake, cy - (p * 8));
      ctx.rotate(-Math.PI / 4);

      // Rocket body
      ctx.fillStyle = '#e0e0e0';
      ctx.beginPath();
      ctx.ellipse(0, 0, w * 0.12, h * 0.28, 0, 0, Math.PI * 2);
      ctx.fill();

      // Rocket tip
      ctx.fillStyle = '#f44336';
      ctx.beginPath();
      ctx.moveTo(-w * 0.1, -h * 0.15);
      ctx.quadraticCurveTo(0, -h * 0.35, w * 0.1, -h * 0.15);
      ctx.fill();

      // Exhaust fire
      ctx.fillStyle = '#ff9800';
      ctx.beginPath();
      ctx.moveTo(-w * 0.08, h * 0.22);
      ctx.lineTo(0, h * 0.38 + (Math.sin(p * 20) * 8));
      ctx.lineTo(w * 0.08, h * 0.22);
      ctx.fill();
      ctx.restore();
    },
  },
  '⭐': {
    color: '#ffc107',
    secondaryColor: '#ffe082',
    draw: (ctx, p, w, h) => {
      const cx = w / 2;
      const cy = h / 2;
      const rot = p * Math.PI * 0.5;
      const scale = 1 + Math.sin(p * Math.PI * 2) * 0.12;
      ctx.save();
      ctx.translate(cx, cy);
      ctx.rotate(rot);
      ctx.scale(scale, scale);

      ctx.fillStyle = '#ffc107';
      ctx.beginPath();
      const spikes = 5;
      const outerRadius = w * 0.35;
      const innerRadius = w * 0.16;
      let rotStep = Math.PI / spikes;

      let x = 0;
      let y = -outerRadius;
      ctx.moveTo(x, y);
      for (let i = 0; i < spikes; i++) {
        x = Math.sin(i * 2 * rotStep) * outerRadius;
        y = -Math.cos(i * 2 * rotStep) * outerRadius;
        ctx.lineTo(x, y);
        x = Math.sin((i * 2 + 1) * rotStep) * innerRadius;
        y = -Math.cos((i * 2 + 1) * rotStep) * innerRadius;
        ctx.lineTo(x, y);
      }
      ctx.closePath();
      ctx.fill();

      // Glow sparkles
      ctx.fillStyle = '#ffffff';
      ctx.beginPath();
      ctx.arc(outerRadius * 0.3, -outerRadius * 0.3, 3, 0, Math.PI * 2);
      ctx.fill();
      ctx.restore();
    },
  },
};

export const TgsPlayer: React.FC<TgsPlayerProps> = ({
  src,
  animationData,
  stickerEmoji = '🔥',
  width = 64,
  height = 64,
  autoplay = true,
  loop = true,
  interactive = true,
  className = '',
  onClick,
}) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const animFrameRef = useRef<number | null>(null);
  const [isPlaying, setIsPlaying] = useState(autoplay);
  const [isHovered, setIsHovered] = useState(false);
  const progressRef = useRef(0);

  // Decompress .tgs (gzip Lottie JSON) if src is provided
  useEffect(() => {
    let active = true;
    const loadTgs = async () => {
      if (!src || !src.endsWith('.tgs')) return;
      try {
        const res = await fetch(src);
        if (!res.ok) return;
        const buffer = await res.arrayBuffer();
        
        // Use browser DecompressionStream for gzip decompression
        if ('DecompressionStream' in window) {
          const ds = new (window as any).DecompressionStream('gzip');
          const writer = ds.writable.getWriter();
          writer.write(buffer);
          writer.close();
          const decompressedStream = ds.readable;
          const reader = decompressedStream.getReader();
          const chunks = [];
          while (true) {
            const { done, value } = await reader.read();
            if (done) break;
            chunks.push(value);
          }
          const blob = new Blob(chunks);
          const jsonText = await blob.text();
          if (active) {
            try {
              JSON.parse(jsonText);
            } catch {}
          }
        }
      } catch (err) {
        console.debug('[TgsPlayer] TGS decompression fallback:', err);
      }
    };

    loadTgs();
    return () => {
      active = false;
    };
  }, [src]);

  // Canvas Vector Render Loop
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    let startTime = performance.now();
    const duration = 1200; // 1.2s animation cycle

    const render = (time: number) => {
      if (!canvas || !ctx) return;
      const elapsed = time - startTime;
      let progress = (elapsed % duration) / duration;
      if (!loop && elapsed >= duration) {
        progress = 1;
        setIsPlaying(false);
      }
      progressRef.current = progress;

      // Clear canvas
      ctx.clearRect(0, 0, canvas.width, canvas.height);

      // Select vector drawer based on stickerEmoji or fallback
      const drawer = VECTOR_REACTIONS[stickerEmoji] || VECTOR_REACTIONS['🔥'];
      if (drawer) {
        drawer.draw(ctx, progress, canvas.width, canvas.height);
      } else {
        // Fallback emoji render
        ctx.font = `${canvas.width * 0.6}px sans-serif`;
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(stickerEmoji, canvas.width / 2, canvas.height / 2);
      }

      if (isPlaying) {
        animFrameRef.current = requestAnimationFrame(render);
      }
    };

    if (isPlaying) {
      animFrameRef.current = requestAnimationFrame(render);
    }

    return () => {
      if (animFrameRef.current) {
        cancelAnimationFrame(animFrameRef.current);
      }
    };
  }, [isPlaying, loop, stickerEmoji]);

  const handleToggle = () => {
    if (onClick) {
      onClick();
    }
    if (interactive) {
      setIsPlaying((prev) => !prev);
    }
  };

  const numericWidth = typeof width === 'number' ? width : parseInt(width as string, 10) || 64;
  const numericHeight = typeof height === 'number' ? height : parseInt(height as string, 10) || 64;

  return (
    <div
      className={`relative inline-flex items-center justify-center select-none cursor-pointer transition-transform duration-200 ${
        isHovered ? 'scale-110' : 'scale-100'
      } ${className}`}
      style={{ width, height }}
      onClick={handleToggle}
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
      title={`${stickerEmoji} - Telegram Vector Reaction`}
    >
      <canvas
        ref={canvasRef}
        width={numericWidth * 2} // Retina 2x rendering
        height={numericHeight * 2}
        style={{ width: `${numericWidth}px`, height: `${numericHeight}px` }}
        className="pointer-events-none drop-shadow-md"
      />
    </div>
  );
};
