import { BehaviorSubject, Subject, type Observable } from 'rxjs';

export class SessionExpiredError extends Error {
  constructor() {
    super('The Pictogram session has expired.');
    this.name = 'SessionExpiredError';
  }
}

export function throwIfSessionExpired(response: Response): void {
  if (response.status === 401) throw new SessionExpiredError();
}

// features/auth owns the token itself (get/set/clear); this is the reactive read side a
// feature that can't import features/auth (boundaries/dependencies) still needs — chat's
// connection is torn down the instant sign-out or a failed refresh clears the token.
const accessToken$ = new BehaviorSubject<string | null>(null);

export function publishAccessToken(token: string | null): void {
  accessToken$.next(token);
}

export function accessTokenChanges(): Observable<string | null> {
  return accessToken$.asObservable();
}

// The reverse direction of accessToken$: chat's connection can tell its token expired
// (a dedicated WebSocket close code, #192) but can't call features/auth's refresh itself
// (feature-to-feature imports are a boundary violation) — it asks here instead, and
// features/auth is the one subscriber that acts on the request.
const refreshRequests$ = new Subject<void>();

export function requestAccessTokenRefresh(): void {
  refreshRequests$.next();
}

export function accessTokenRefreshRequests(): Observable<void> {
  return refreshRequests$.asObservable();
}
