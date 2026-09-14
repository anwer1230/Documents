/**
 * useNotificationDismiss
 * 
 * Tracks notification dismiss counts in localStorage scoped to the current session.
 * Automatically hides notifications after the user has dismissed three notifications
 * in the current session.
 */

import { useState, useEffect, useCallback } from 'react';

const DISMISS_COUNT_KEY = 'notification_dismiss_count';
const SESSION_ID_KEY = 'notification_dismiss_session_id';
const DISMISS_EVENT_NAME = 'notification:dismiss_count_updated';
export const MAX_SESSION_DISMISSALS = 3;

/**
 * Returns or initializes a unique session identifier for the current browser session.
 */
function getOrCreateSessionId(): string {
  if (typeof window === 'undefined') return 'ssr_session';
  try {
    let sessionId = sessionStorage.getItem(SESSION_ID_KEY);
    if (!sessionId) {
      sessionId = `sess_${Date.now()}_${Math.random().toString(36).slice(2, 9)}`;
      sessionStorage.setItem(SESSION_ID_KEY, sessionId);
    }
    return sessionId;
  } catch {
    return 'fallback_session';
  }
}

/**
 * Reads the current session dismiss count from localStorage.
 * If the session has changed, resets the count to 0.
 */
function getStoredDismissCount(): number {
  if (typeof window === 'undefined') return 0;
  try {
    const currentSessionId = getOrCreateSessionId();
    const storedSessionId = localStorage.getItem(SESSION_ID_KEY);

    // If session ID matches, retrieve the count
    if (storedSessionId === currentSessionId) {
      const stored = localStorage.getItem(DISMISS_COUNT_KEY);
      const count = stored ? parseInt(stored, 10) : 0;
      return Number.isNaN(count) ? 0 : count;
    }

    // New session detected: initialize localStorage for this session
    localStorage.setItem(SESSION_ID_KEY, currentSessionId);
    localStorage.setItem(DISMISS_COUNT_KEY, '0');
    return 0;
  } catch {
    return 0;
  }
}

export interface UseNotificationDismissResult {
  /** The current count of notifications dismissed in this session */
  dismissCount: number;
  /** Whether notifications are automatically hidden (dismissCount >= 3) */
  shouldHideNotifications: boolean;
  /** Synonym for shouldHideNotifications (true when dismissCount >= 3) */
  isDismissLimitReached: boolean;
  /** True if dismiss count is below the limit (< 3) */
  canShowNotifications: boolean;
  /** The maximum dismissals allowed before hiding (3) */
  maxDismissLimit: number;
  /** Records a dismissal: increments the count in localStorage and updates state */
  recordDismiss: (notificationId?: string, direction?: 'left' | 'right' | 'up' | 'button') => void;
  /** Resets the dismiss count for the current session back to 0 in localStorage */
  resetDismissCount: () => void;
}

export function useNotificationDismiss(): UseNotificationDismissResult {
  const [dismissCount, setDismissCount] = useState<number>(() => getStoredDismissCount());

  // Keep state in sync with localStorage and other components/tabs
  useEffect(() => {
    const syncFromStorage = () => {
      setDismissCount(getStoredDismissCount());
    };

    window.addEventListener('storage', syncFromStorage);
    window.addEventListener(DISMISS_EVENT_NAME, syncFromStorage);

    return () => {
      window.removeEventListener('storage', syncFromStorage);
      window.removeEventListener(DISMISS_EVENT_NAME, syncFromStorage);
    };
  }, []);

  const recordDismiss = useCallback((_notificationId?: string, _direction?: 'left' | 'right' | 'up' | 'button') => {
    try {
      const currentSessionId = getOrCreateSessionId();
      localStorage.setItem(SESSION_ID_KEY, currentSessionId);

      const current = getStoredDismissCount();
      const nextCount = current + 1;
      localStorage.setItem(DISMISS_COUNT_KEY, nextCount.toString());
      setDismissCount(nextCount);

      // Dispatch event to sync all hook instances within the same page
      window.dispatchEvent(
        new CustomEvent(DISMISS_EVENT_NAME, {
          detail: {
            count: nextCount,
            shouldHide: nextCount >= MAX_SESSION_DISMISSALS,
          },
        })
      );
    } catch (err) {
      console.warn('[useNotificationDismiss] Error updating localStorage:', err);
      setDismissCount((prev) => prev + 1);
    }
  }, []);

  const resetDismissCount = useCallback(() => {
    try {
      const currentSessionId = getOrCreateSessionId();
      localStorage.setItem(SESSION_ID_KEY, currentSessionId);
      localStorage.setItem(DISMISS_COUNT_KEY, '0');
      setDismissCount(0);

      window.dispatchEvent(
        new CustomEvent(DISMISS_EVENT_NAME, {
          detail: { count: 0, shouldHide: false },
        })
      );
    } catch (err) {
      console.warn('[useNotificationDismiss] Error resetting localStorage:', err);
      setDismissCount(0);
    }
  }, []);

  const isDismissLimitReached = dismissCount >= MAX_SESSION_DISMISSALS;
  const shouldHideNotifications = isDismissLimitReached;
  const canShowNotifications = !isDismissLimitReached;

  return {
    dismissCount,
    shouldHideNotifications,
    isDismissLimitReached,
    canShowNotifications,
    maxDismissLimit: MAX_SESSION_DISMISSALS,
    recordDismiss,
    resetDismissCount,
  };
}

export default useNotificationDismiss;
