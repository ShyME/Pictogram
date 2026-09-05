import { publishAccessToken } from '@shared';

let accessToken: string | null = null;

export function getAccessToken(): string | null {
  return accessToken;
}

export function setAccessToken(token: string | null): void {
  accessToken = token;
  publishAccessToken(token);
}

export function clearAccessToken(): void {
  accessToken = null;
  publishAccessToken(null);
}
