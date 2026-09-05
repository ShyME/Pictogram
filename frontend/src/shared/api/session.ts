import { BehaviorSubject, type Observable } from 'rxjs';

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
