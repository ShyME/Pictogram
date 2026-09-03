import { api, type components } from '@shared';
import { CSRF_HEADER, readCsrfToken } from './csrf';
import { clearAccessToken, setAccessToken } from './session';

const REFRESH_ENDPOINT = '/api/auth/refresh';

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
    response = await postRefresh();
    // A cold browser session has no XSRF-TOKEN cookie yet; the rejected call seeds one, so the
    // retry carries it. Every later refresh finds the cookie and succeeds on the first call.
    if (response.status === 403) response = await postRefresh();
  } catch {
    clearAccessToken();
    return null;
  }

  if (!response.ok) {
    clearAccessToken();
    return null;
  }

  const body = (await response.json()) as components['schemas']['AccessTokenResponse'];
  setAccessToken(body.accessToken);
  return body.accessToken;
}

function postRefresh(): Promise<Response> {
  const headers = new Headers({ Accept: 'application/json' });
  const csrfToken = readCsrfToken();
  if (csrfToken) headers.set(CSRF_HEADER, csrfToken);
  return fetch(REFRESH_ENDPOINT, { method: 'POST', headers });
}

export async function signOut(): Promise<void> {
  await api.POST('/api/auth/logout').catch(() => {
    // A failed logout call still clears the local token below.
  });
  clearAccessToken();
}
