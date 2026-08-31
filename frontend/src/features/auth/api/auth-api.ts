import { api, type components } from "@shared";
import { clearAccessToken, setAccessToken } from "../model/session";

// Kept off the typed client: routing refresh through the same client the auth middleware
// wraps would recurse on its own 401.
const REFRESH_ENDPOINT = "/api/auth/refresh";

// One in-flight refresh at a time: several calls can 401 at once (e.g. every route
// loader on first load), and they should share a single round-trip, not stampede the
// refresh endpoint and rotate the cookie repeatedly.
let inFlight: Promise<string | null> | null = null;

/**
 * Exchanges the refresh cookie for a fresh access token, stores it, and returns it —
 * or `null` when the session is gone (no cookie, or a rotated/stolen token), after
 * clearing any stale token. Called by the API client on a 401; not routed through the
 * typed client itself, which would recurse.
 */
export function refreshAccessToken(): Promise<string | null> {
  inFlight ??= runRefresh().finally(() => {
    inFlight = null;
  });
  return inFlight;
}

async function runRefresh(): Promise<string | null> {
  let response: Response;
  try {
    response = await fetch(REFRESH_ENDPOINT, {
      method: "POST",
      credentials: "same-origin",
      headers: { Accept: "application/json" },
    });
  } catch {
    clearAccessToken();
    return null;
  }

  if (!response.ok) {
    clearAccessToken();
    return null;
  }

  const body = (await response.json()) as components["schemas"]["AccessTokenResponse"];
  setAccessToken(body.accessToken);
  return body.accessToken;
}

/**
 * Ends the session server-side (revoking the refresh-token family) and locally. The
 * local half is what matters to the user, so a failed round-trip is swallowed rather
 * than trapping them on a signed-in-looking screen.
 */
export async function signOut(): Promise<void> {
  try {
    await api.POST("/api/auth/logout");
  } catch {
    // best effort — fall through to clearing the local token
  }
  clearAccessToken();
}
