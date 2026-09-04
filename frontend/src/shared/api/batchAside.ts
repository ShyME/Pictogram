import { type QueryClient, type QueryKey, useQuery } from '@tanstack/react-query';

// A page load primes this cache from one batch call; the per-key hooks then read from
// cache instead of firing a request each (ADR-0005). A primed entry is held fresh this
// long so the hook's mount-time refetch is skipped.
const SEEDED_STALE_TIME_MS = 30_000;

type BatchEntries<TValue> = Iterable<readonly [string, TValue]>;

export type BatchAside<TValue> = {
  useValue: (id: string) => ReturnType<typeof useQuery<TValue, Error, TValue, QueryKey>>;
  seed: (client: QueryClient, entries: BatchEntries<TValue>) => void;
  prime: (client: QueryClient, ids: string[]) => Promise<void>;
};

// One implementation of the batch-primed per-key query: `likes`, comment counts, and the
// follow relationship all hand-rolled the same seed / prime / per-key `useQuery` with the
// same seeded stale time. `readOne` resolves a single id (a batch-of-one with a zero-value
// fallback, or a dedicated endpoint); `readBatch` resolves a page for priming.
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
