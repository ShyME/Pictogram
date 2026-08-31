import createClient from "openapi-fetch";
import type { paths } from "./schema";

/**
 * The typed API client, generated from the backend's committed `openapi.json` (regenerate
 * with `pnpm generate:api`). Bound to the page's own origin: Vite proxies `/api` to the
 * backend in dev, and the backend serves the SPA in every other environment, so every call
 * is effectively same-origin. Access-token headers and silent refresh are the auth
 * slice's job — it registers a middleware via `installApiAuth()` at app startup.
 *
 * `fetch` is a thin indirection to the live global so the middleware's retry, the auth
 * slice's raw refresh call, and this client all dispatch to the same place — a test can
 * swap one `globalThis.fetch`.
 */
export const api = createClient<paths>({
  baseUrl: window.location.origin,
  fetch: (request) => globalThis.fetch(request),
});
