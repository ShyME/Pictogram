import type { Middleware } from "openapi-fetch";
import { api } from "@shared";
import { getAccessToken } from "./session";
import { refreshAccessToken } from "./auth-api";

// The pre-flight copy of each request, kept so a 401 can be replayed with a refreshed
// token — openapi-fetch spends the original body stream when it calls `fetch(request)`.
const preFlight = new WeakMap<Request, Request>();

/**
 * Puts the access token on every `/api/**` call and, on a 401, refreshes it once and
 * replays the request. A refresh that fails (the session is gone) leaves the 401 to
 * stand — route loaders read that as "send them to /login".
 */
export const authMiddleware: Middleware = {
  onRequest({ request }) {
    const token = getAccessToken();
    if (token) request.headers.set("Authorization", `Bearer ${token}`);
    preFlight.set(request, request.clone());
    return request;
  },

  async onResponse({ request, response }) {
    const original = preFlight.get(request);
    preFlight.delete(request);

    if (response.status !== 401 || !original) return response;

    const token = await refreshAccessToken();
    if (!token) return response;

    original.headers.set("Authorization", `Bearer ${token}`);
    return fetch(original);
  },
};

let installed = false;

export function installApiAuth(): void {
  if (installed) return;
  api.use(authMiddleware);
  installed = true;
}
