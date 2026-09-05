export class SessionExpiredError extends Error {
  constructor() {
    super('The Pictogram session has expired.');
    this.name = 'SessionExpiredError';
  }
}

export function throwIfSessionExpired(response: Response): void {
  if (response.status === 401) throw new SessionExpiredError();
}
