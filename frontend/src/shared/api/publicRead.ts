type QueryValue = string | number | readonly (string | number)[];

// The one place a "signed-out visitors may call this GET" read is resolved. The typed
// bearer call is passed in already-awaited: its data settles the read. A 401 means the
// auth middleware tried to refresh and found no session, so the same URL is re-fetched
// bare and the backend serves its public projection. Any other status is a real failure.
export async function orAnonymous<T>(
  bearer: { data?: T | null; response: Response },
  publicRead: { path: string; query?: Record<string, QueryValue | undefined>; label: string },
): Promise<T> {
  if (bearer.data != null) return bearer.data;
  if (bearer.response.status !== 401) {
    throw new Error(`${publicRead.label} failed: ${bearer.response.status}`);
  }

  const anonymous = await fetch(anonymousUrl(publicRead.path, publicRead.query), {
    headers: { Accept: 'application/json' },
  });
  if (!anonymous.ok) throw new Error(`${publicRead.label} failed: ${anonymous.status}`);
  return (await anonymous.json()) as T;
}

function anonymousUrl(path: string, query?: Record<string, QueryValue | undefined>): URL {
  const url = new URL(path, location.origin);
  const entries = Object.entries(query ?? {});
  for (const [name, value] of entries) {
    if (value == null) continue;
    const items = Array.isArray(value) ? value : [value];
    for (const item of items) url.searchParams.append(name, String(item));
  }
  return url;
}
