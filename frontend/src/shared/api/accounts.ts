import { api } from './client';
import type { components } from './schema';
import { throwIfSessionExpired } from './session';

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
