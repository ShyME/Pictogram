/**
 * The current Pictogram access token, held only in memory: the refresh token is an
 * httpOnly cookie the script can't see (ADR-0004), so it — not this — is the durable
 * source of truth. A reload starts with no token and the first authenticated call's 401
 * triggers a silent refresh (see `auth-client`).
 */
let accessToken: string | null = null;

export function getAccessToken(): string | null {
  return accessToken;
}

export function setAccessToken(token: string | null): void {
  accessToken = token;
}

export function clearAccessToken(): void {
  accessToken = null;
}
