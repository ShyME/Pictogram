import { api } from '@shared';
import type { Middleware } from 'openapi-fetch';
import { refreshAccessToken } from './authApi';
import { CSRF_HEADER, readCsrfToken } from './csrf';
import { getAccessToken } from './session';

const preFlight = new WeakMap<Request, Request>();

export const authMiddleware: Middleware = {
  onRequest({ request }) {
    const token = getAccessToken();
    if (token) request.headers.set('Authorization', `Bearer ${token}`);
    if (requiresCsrfToken(request)) {
      const csrfToken = readCsrfToken();
      if (csrfToken) request.headers.set(CSRF_HEADER, csrfToken);
    }
    preFlight.set(request, request.clone());
    return request;
  },

  async onResponse({ request, response }) {
    const original = preFlight.get(request);
    preFlight.delete(request);
    if (!original) return response;

    // A cold session has no XSRF-TOKEN cookie; the rejected call seeds one, so the retry
    // carries it. One retry only — a persistent 403 falls through to the caller (sign-out
    // still clears the local token; ADR-0011).
    if (response.status === 403 && requiresCsrfToken(original)) {
      const csrfToken = readCsrfToken();
      if (!csrfToken) return response;
      original.headers.set(CSRF_HEADER, csrfToken);
      return fetch(original);
    }

    if (response.status !== 401) return response;

    const token = await refreshAccessToken();
    if (!token) return response;

    original.headers.set('Authorization', `Bearer ${token}`);
    return fetch(original);
  },
};

// Only the /api/auth/** chain runs a CsrfFilter (ADR-0011); the bearer-only /api/** chain does not.
function requiresCsrfToken(request: Request): boolean {
  if (request.method === 'GET' || request.method === 'HEAD') return false;
  return new URL(request.url).pathname.startsWith('/api/auth/');
}

let isInstalled = false;

export function installApiAuth(): void {
  if (isInstalled) return;
  api.use(authMiddleware);
  isInstalled = true;
}
