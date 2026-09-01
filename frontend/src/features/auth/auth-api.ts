import { api, type components } from "@shared";
import { clearAccessToken, setAccessToken } from "./session";

const REFRESH_ENDPOINT = "/api/auth/refresh";

let inFlight: Promise<string | null> | null = null;

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

export async function signOut(): Promise<void> {
  await api.POST("/api/auth/logout").catch(() => undefined);
  clearAccessToken();
}
