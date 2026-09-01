import type { Middleware } from "openapi-fetch";
import { api } from "@shared";
import { getAccessToken } from "./session";
import { refreshAccessToken } from "./auth-api";

const preFlight = new WeakMap<Request, Request>();

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
