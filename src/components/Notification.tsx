/**
 * Reusable Notification Component with Swipe-to-Dismiss Gestures
 * 
 * Supports smooth touch and drag swipe gestures (left / right) to dismiss,
 * configurable thresholds, accessible controls, and integrated dismissal tracking.
 */

import React, { useState, useEffect, useRef, useCallback } from 'react';
import { motion, AnimatePresence, PanInfo } from 'motion/react';
import {
  CheckCircle2,
  AlertCircle,
  AlertTriangle,
  Info,
  X,
  MessageSquare,
  ArrowRight,
  ArrowLeft,
  Trash2,
} from 'lucide-react';
import { useNotificationDismiss } from '../hooks/useNotificationDismiss';

export type NotificationType = 'info' | 'success' | 'warning' | 'error' | 'message';

export interface NotificationAction {
  label: string;
  onClick: (e: React.MouseEvent) => void;
  variant?: 'primary' | 'secondary' | 'danger';
}

export interface NotificationProps {
  /** Unique identifier for the notification */
  id?: string;
  /** Primary headline or sender title */
  title?: string;
  /** Notification body text or React node */
  message: React.ReactNode;
  /** Visual type variant */
  type?: NotificationType;
  /** Optional custom icon */
  icon?: React.ReactNode;
  /** Optional avatar image URL */
  avatar?: string;
  /** Optional formatted timestamp */
  timestamp?: string | number | Date;
  /** Callback fired when the notification is dismissed */
  onDismiss?: (id?: string, direction?: 'left' | 'right' | 'up' | 'button') => void;
  /** Optional click handler for the entire notification card */
  onClick?: () => void;
  /** Interactive action buttons */
  actions?: NotificationAction[];
  /** Auto-close timeout in milliseconds (0 or undefined to disable) */
  autoCloseDuration?: number;
  /** Distance in pixels to trigger a swipe dismissal (default: 80) */
  swipeThreshold?: number;
  /** If true, integrates with useNotificationDismiss to automatically hide after 3 dismissals */
  enforceSessionLimit?: boolean;
  /** Whether the notification is rendered in Arabic / RTL context */
  isArabic?: boolean;
  /** Custom additional CSS classes */
  className?: string;
}

const TYPE_CONFIGS: Record<
  NotificationType,
  {
    icon: React.ReactNode;
    badgeBg: string;
    badgeText: string;
    border: string;
    accent: string;
  }
> = {
  info: {
    icon: <Info className="w-4 h-4" />,
    badgeBg: 'bg-sky-500/15 text-sky-400',
    badgeText: 'text-sky-300',
    border: 'border-sky-500/20',
    accent: '#0284c7',
  },
  success: {
    icon: <CheckCircle2 className="w-4 h-4" />,
    badgeBg: 'bg-emerald-500/15 text-emerald-400',
    badgeText: 'text-emerald-300',
    border: 'border-emerald-500/20',
    accent: '#10b981',
  },
  warning: {
    icon: <AlertTriangle className="w-4 h-4" />,
    badgeBg: 'bg-amber-500/15 text-amber-400',
    badgeText: 'text-amber-300',
    border: 'border-amber-500/20',
    accent: '#f59e0b',
  },
  error: {
    icon: <AlertCircle className="w-4 h-4" />,
    badgeBg: 'bg-rose-500/15 text-rose-400',
    badgeText: 'text-rose-300',
    border: 'border-rose-500/20',
    accent: '#f43f5e',
  },
  message: {
    icon: <MessageSquare className="w-4 h-4" />,
    badgeBg: 'bg-indigo-500/15 text-indigo-400',
    badgeText: 'text-indigo-300',
    border: 'border-indigo-500/20',
    accent: '#6366f1',
  },
};

export const Notification: React.FC<NotificationProps> = ({
  id,
  title,
  message,
  type = 'info',
  icon,
  avatar,
  timestamp,
  onDismiss,
  onClick,
  actions = [],
  autoCloseDuration,
  swipeThreshold = 80,
  enforceSessionLimit = true,
  isArabic = false,
  className = '',
}) => {
  const [isVisible, setIsVisible] = useState(true);
  const [exitX, setExitX] = useState<number | null>(null);
  const [dragOffset, setDragOffset] = useState<number>(0);
  const cardRef = useRef<HTMLDivElement>(null);

  const { recordDismiss, shouldHideNotifications } = useNotificationDismiss();

  // If session dismiss limit is reached (3 dismissals in current session), hide notifications
  if (enforceSessionLimit && shouldHideNotifications) {
    return null;
  }

  // Handle auto-close timer if specified
  useEffect(() => {
    if (!autoCloseDuration || autoCloseDuration <= 0) return;
    const timer = setTimeout(() => {
      handleDismiss('button');
    }, autoCloseDuration);
    return () => clearTimeout(timer);
  }, [autoCloseDuration]);

  const handleDismiss = useCallback(
    (direction: 'left' | 'right' | 'up' | 'button' = 'button') => {
      // Record dismissal in localStorage tracking hook
      recordDismiss(id, direction);

      // Trigger exit animation
      if (direction === 'left') {
        setExitX(-400);
      } else if (direction === 'right') {
        setExitX(400);
      }
      setIsVisible(false);

      if (onDismiss) {
        onDismiss(id, direction);
      }
    },
    [id, onDismiss, recordDismiss]
  );

  const handleDrag = (_event: MouseEvent | TouchEvent | PointerEvent, info: PanInfo) => {
    setDragOffset(info.offset.x);
  };

  const handleDragEnd = (_event: MouseEvent | TouchEvent | PointerEvent, info: PanInfo) => {
    const offsetX = info.offset.x;
    const velocityX = info.velocity.x;

    // Swipe threshold detection (distance or velocity)
    if (offsetX < -swipeThreshold || velocityX < -400) {
      handleDismiss('left');
    } else if (offsetX > swipeThreshold || velocityX > 400) {
      handleDismiss('right');
    } else {
      setDragOffset(0);
    }
  };

  const typeConfig = TYPE_CONFIGS[type] || TYPE_CONFIGS.info;
  const renderIcon = icon || typeConfig.icon;

  const formattedTimestamp = timestamp
    ? typeof timestamp === 'string'
      ? timestamp
      : timestamp instanceof Date
      ? timestamp.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      : new Date(timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    : undefined;

  return (
    <AnimatePresence>
      {isVisible && (
        <div className="relative w-full max-w-md mx-auto select-none overflow-hidden my-1.5">
          {/* Swipe indicator background reveal */}
          <div
            className={`absolute inset-0 rounded-2xl flex items-center justify-between px-5 transition-opacity duration-150 pointer-events-none ${
              dragOffset !== 0 ? 'opacity-100' : 'opacity-0'
            }`}
            style={{
              backgroundColor:
                dragOffset < 0 ? 'rgba(239, 68, 68, 0.15)' : 'rgba(16, 185, 129, 0.15)',
            }}
          >
            <div
              className={`flex items-center gap-1.5 text-xs font-semibold ${
                dragOffset > 0 ? 'text-emerald-400' : 'text-transparent'
              }`}
            >
              <ArrowLeft className="w-4 h-4" />
              <span>{isArabic ? 'إغلاق' : 'Dismiss'}</span>
            </div>
            <div
              className={`flex items-center gap-1.5 text-xs font-semibold ${
                dragOffset < 0 ? 'text-rose-400' : 'text-transparent'
              }`}
            >
              <span>{isArabic ? 'إغلاق' : 'Dismiss'}</span>
              <Trash2 className="w-4 h-4" />
            </div>
          </div>

          {/* Draggable Swipe Card */}
          <motion.div
            ref={cardRef}
            drag="x"
            dragConstraints={{ left: 0, right: 0 }}
            dragElastic={0.8}
            onDrag={handleDrag}
            onDragEnd={handleDragEnd}
            animate={{ x: 0, opacity: 1, scale: 1 }}
            exit={{
              x: exitX ?? (isArabic ? -400 : 400),
              opacity: 0,
              scale: 0.92,
              transition: { duration: 0.22, ease: 'easeInOut' },
            }}
            whileHover={{ scale: 1.01 }}
            whileTap={{ cursor: 'grabbing' }}
            onClick={onClick}
            role="alert"
            aria-live="polite"
            className={`relative z-10 w-full rounded-2xl bg-neutral-900/90 backdrop-blur-md text-neutral-100 border ${typeConfig.border} shadow-xl shadow-black/40 p-3.5 transition-shadow hover:shadow-2xl cursor-grab active:cursor-grabbing ${className}`}
            style={{ touchAction: 'pan-y' }}
          >
            <div className="flex items-start gap-3">
              {/* Avatar or Type Icon */}
              <div className="relative shrink-0">
                {avatar ? (
                  <img
                    src={avatar}
                    alt={title || 'Notification Avatar'}
                    className="w-10 h-10 rounded-xl object-cover border border-white/10 shadow-sm"
                  />
                ) : (
                  <div
                    className={`w-10 h-10 rounded-xl flex items-center justify-center border border-white/10 shadow-sm ${typeConfig.badgeBg}`}
                  >
                    {renderIcon}
                  </div>
                )}
              </div>

              {/* Text content */}
              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between gap-2 mb-0.5">
                  {title && (
                    <h4 className="text-sm font-semibold text-white truncate tracking-tight">
                      {title}
                    </h4>
                  )}
                  {formattedTimestamp && (
                    <span className="text-[11px] text-neutral-400 font-mono shrink-0">
                      {formattedTimestamp}
                    </span>
                  )}
                </div>

                <div className="text-xs text-neutral-300 leading-relaxed break-words line-clamp-3">
                  {message}
                </div>

                {/* Optional action buttons */}
                {actions.length > 0 && (
                  <div className="mt-2.5 flex items-center flex-wrap gap-1.5 pt-1.5 border-t border-white/10">
                    {actions.map((act, index) => {
                      const variantClasses =
                        act.variant === 'danger'
                          ? 'bg-rose-500/20 hover:bg-rose-500/30 text-rose-300 border border-rose-500/30'
                          : act.variant === 'primary'
                          ? 'bg-indigo-600 hover:bg-indigo-500 text-white shadow-sm'
                          : 'bg-white/10 hover:bg-white/20 text-neutral-200';

                      return (
                        <button
                          key={index}
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation();
                            act.onClick(e);
                          }}
                          className={`px-2.5 py-1 rounded-lg text-xs font-medium transition-colors active:scale-95 cursor-pointer ${variantClasses}`}
                        >
                          {act.label}
                        </button>
                      );
                    })}
                  </div>
                )}
              </div>

              {/* Close / Dismiss 'X' button */}
              <button
                type="button"
                aria-label={isArabic ? 'إغلاق الإشعار' : 'Dismiss notification'}
                onClick={(e) => {
                  e.stopPropagation();
                  handleDismiss('button');
                }}
                className="shrink-0 p-1.5 rounded-lg text-neutral-400 hover:text-white hover:bg-white/10 active:scale-90 transition-all cursor-pointer"
                title={
                  isArabic
                    ? 'إغلاق (اسحب للإخفاء)'
                    : 'Dismiss (swipe to dismiss)'
                }
              >
                <X className="w-4 h-4" />
              </button>
            </div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
};

export default Notification;
