import { api } from './client';
import type { components } from './schema';
import { throwIfSessionExpired } from './session';

// The wire shape of a profile, reduced to what every screen needs to render an author or
// an account row. One place normalises the nullable `ProfileView` fields (ADR-0005: the
// client composes cards from batched `?ids=` reads, so this mapping is not per-feature).
export type Account = {
  userId: string;
  username: string;
  displayName: string | null;
};

export function toAccount(view: components['schemas']['ProfileView']): Account {
  return {
    userId: view.userId ?? '',
    username: view.username ?? '',
    displayName: view.displayName ?? null,
  };
}

export async function fetchAccounts(ids: string[]): Promise<Map<string, Account>> {
  if (ids.length === 0) return new Map();

  const { data, response } = await api.GET('/api/profiles', {
    params: { query: { ids } },
  });
  throwIfSessionExpired(response);
  if (!data) throw new Error(`Profile batch request failed: ${response.status}`);

  return new Map(data.map((view) => [view.userId ?? '', toAccount(view)]));
}
