import { type QueryClient, type QueryKey, useQuery } from '@tanstack/react-query';

const SEEDED_STALE_TIME_MS = 30_000;

type BatchEntries<TValue> = Iterable<readonly [string, TValue]>;

export type BatchAside<TValue> = {
  useValue: (id: string) => ReturnType<typeof useQuery<TValue, Error, TValue, QueryKey>>;
  seed: (client: QueryClient, entries: BatchEntries<TValue>) => void;
  prime: (client: QueryClient, ids: string[]) => Promise<void>;
};

export function createBatchAside<TValue>(config: {
  key: (id: string) => QueryKey;
  readOne: (id: string) => Promise<TValue>;
  readBatch: (ids: string[]) => Promise<BatchEntries<TValue>>;
}): BatchAside<TValue> {
  const seed: BatchAside<TValue>['seed'] = (client, entries) => {
    for (const [id, value] of entries) client.setQueryData<TValue>(config.key(id), value);
  };

  return {
    useValue: (id) =>
      useQuery({
        queryKey: config.key(id),
        queryFn: () => config.readOne(id),
        staleTime: SEEDED_STALE_TIME_MS,
      }),

    seed,

    prime: async (client, ids) => {
      if (ids.length === 0) return;
      seed(client, await config.readBatch(ids));
    },
  };
}
