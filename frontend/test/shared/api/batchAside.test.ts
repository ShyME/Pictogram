import { createBatchAside, createQueryClient } from '@shared';
import { expect, test, vi } from 'vitest';

type Count = { n: number };

const countKey = (id: string) => ['count', id] as const;

function countAside(readBatch: (ids: string[]) => Promise<Iterable<readonly [string, Count]>>) {
  return createBatchAside<Count>({
    key: countKey,
    readOne: (id) => Promise.resolve({ n: id.length }),
    readBatch,
  });
}

test('seed writes each entry under its per-id key', () => {
  const client = createQueryClient();
  const aside = countAside(() => Promise.resolve([]));

  aside.seed(client, [
    ['a', { n: 1 }],
    ['bb', { n: 2 }],
  ]);

  expect(client.getQueryData(countKey('a'))).toEqual({ n: 1 });
  expect(client.getQueryData(countKey('bb'))).toEqual({ n: 2 });
});

test('prime fetches one batch and seeds every id key', async () => {
  const client = createQueryClient();
  const readBatch = vi.fn(() =>
    Promise.resolve<Iterable<readonly [string, Count]>>([
      ['a', { n: 5 }],
      ['b', { n: 6 }],
    ]),
  );

  await countAside(readBatch).prime(client, ['a', 'b']);

  expect(readBatch).toHaveBeenCalledExactlyOnceWith(['a', 'b']);
  expect(client.getQueryData(countKey('a'))).toEqual({ n: 5 });
  expect(client.getQueryData(countKey('b'))).toEqual({ n: 6 });
});

test('prime makes no batch call for an empty id set', async () => {
  const client = createQueryClient();
  const readBatch = vi.fn(() => Promise.resolve<Iterable<readonly [string, Count]>>([]));

  await countAside(readBatch).prime(client, []);

  expect(readBatch).not.toHaveBeenCalled();
});
