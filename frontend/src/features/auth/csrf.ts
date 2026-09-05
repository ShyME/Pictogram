export const CSRF_HEADER = 'X-XSRF-TOKEN';

const XSRF_COOKIE = /(?:^|;\s*)XSRF-TOKEN=([^;]+)/;

export function readCsrfToken(): string | null {
  const match = XSRF_COOKIE.exec(document.cookie);
  return match ? decodeURIComponent(match[1]) : null;
}
