import { api } from './client';
import { orAnonymous } from './publicRead';
import type { components } from './schema';

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

  const views = await orAnonymous<components['schemas']['ProfileView'][]>(
    await api.GET('/api/profiles', { params: { query: { ids } } }),
    { path: '/api/profiles', query: { ids }, label: 'Profile batch request' },
  );
  return new Map(views.map((view) => [view.userId ?? '', toAccount(view)]));
}
