// Defence-in-depth for the cookie-authenticated identity chain (#125): the backend sets a
// non-HttpOnly XSRF-TOKEN cookie and expects it echoed in X-XSRF-TOKEN on every mutating
// /api/auth/** call, on top of the refresh cookie's SameSite=Strict.
export const CSRF_HEADER = 'X-XSRF-TOKEN';

const XSRF_COOKIE = /(?:^|;\s*)XSRF-TOKEN=([^;]+)/;

export function readCsrfToken(): string | null {
  const match = XSRF_COOKIE.exec(document.cookie);
  return match ? decodeURIComponent(match[1]) : null;
}
