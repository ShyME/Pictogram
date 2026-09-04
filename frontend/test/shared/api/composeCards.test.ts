import { composeCards, primeBestEffort } from '@shared';
import { expect, test, vi } from 'vitest';

test('returns the hydrated cards after priming resolves', async () => {
  const prime = vi.fn(() => [Promise.resolve()]);

  const cards = await composeCards(['p-1', 'p-2'], {
    hydrate: (ids) => Promise.resolve(ids.map((id) => ({ postId: id }))),
    idOf: (card) => card.postId,
    prime,
  });

  expect(cards).toEqual([{ postId: 'p-1' }, { postId: 'p-2' }]);
  expect(prime).toHaveBeenCalledExactlyOnceWith(['p-1', 'p-2']);
});

test('still returns the cards when a priming task rejects', async () => {
  const cards = await composeCards(['p-1'], {
    hydrate: (ids) => Promise.resolve(ids.map((id) => ({ postId: id }))),
    idOf: (card) => card.postId,
    prime: () => [Promise.reject(new Error('likes batch down')), Promise.resolve()],
  });

  expect(cards).toEqual([{ postId: 'p-1' }]);
});

test('propagates a hydration failure — the page cannot render without its cards', async () => {
  await expect(
    composeCards(['p-1'], {
      hydrate: () => Promise.reject(new Error('author batch down')),
      idOf: (card: { postId: string }) => card.postId,
      prime: () => [],
    }),
  ).rejects.toThrow('author batch down');
});

test('primeBestEffort swallows rejections and skips absent tasks', async () => {
  const failing = Promise.reject(new Error('down'));

  await expect(primeBestEffort([failing, undefined, Promise.resolve()])).resolves.toBeUndefined();
});
