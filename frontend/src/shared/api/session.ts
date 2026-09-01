export class SessionExpiredError extends Error {
  constructor() {
    super('The Pictogram session has expired.');
    this.name = 'SessionExpiredError';
  }
}

// Route loaders keep their own status unions and turn a 401 into a redirect; every other
// authenticated fetch calls this so a screen can react to a dead session the same way.
export function throwIfSessionExpired(response: Response): void {
  if (response.status === 401) throw new SessionExpiredError();
}
