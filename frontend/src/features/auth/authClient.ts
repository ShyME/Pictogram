import { api } from '@shared';
import type { Middleware } from 'openapi-fetch';
import { refreshAccessToken } from './authApi';
import { getAccessToken } from './session';

const preFlight = new WeakMap<Request, Request>();

export const authMiddleware: Middleware = {
  onRequest({ request }) {
    const token = getAccessToken();
    if (token) request.headers.set('Authorization', `Bearer ${token}`);
    preFlight.set(request, request.clone());
    return request;
  },

  async onResponse({ request, response }) {
    const original = preFlight.get(request);
    preFlight.delete(request);

    if (!original || response.status !== 401) return response;

    const token = await refreshAccessToken();
    if (!token) return response;

    original.headers.set('Authorization', `Bearer ${token}`);
    return fetch(original);
  },
};

let isInstalled = false;

export function installApiAuth(): void {
  if (isInstalled) return;
  api.use(authMiddleware);
  isInstalled = true;
}
