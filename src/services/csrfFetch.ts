/**
 * src/services/csrfFetch.ts
 * Wrapper around fetch that automatically reads and sends CSRF token.
 */
export function getCookie(name: string): string | null {
  if (typeof document === 'undefined') return null;
  const m = document.cookie.match(new RegExp(`(^|;\\s*)${name}=([^;]*)`));
  return m ? decodeURIComponent(m[2]) : null;
}

export async function csrfFetch(url: string, options: RequestInit = {}): Promise<Response> {
  const method = (options.method || 'GET').toUpperCase();
  const headers = new Headers(options.headers || {});

  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    const csrfToken = getCookie('csrf_token');
    if (csrfToken && !headers.has('x-csrf-token')) {
      headers.set('x-csrf-token', csrfToken);
    }
  }

  return fetch(url, {
    ...options,
    headers,
    credentials: 'include',
  });
}
