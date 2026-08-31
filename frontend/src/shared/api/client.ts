import createClient from "openapi-fetch";
import type { paths } from "./schema";

/**
 * The typed API client, generated from the backend's committed `openapi.json` (regenerate
 * with `pnpm generate:api`). Bound to the page's own origin: Vite proxies `/api` to the
 * backend in dev, and the backend serves the SPA in every other environment, so every call
 * is effectively same-origin. Access-token headers are the auth slice's job (#10) and
 * belong in a middleware added there, not here.
 */
export const api = createClient<paths>({ baseUrl: window.location.origin });
