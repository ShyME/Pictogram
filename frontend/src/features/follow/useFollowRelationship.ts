import { createBatchAside } from '@shared';
import type { QueryClient } from '@tanstack/react-query';
import type { FollowRelationship } from './follow';
import {
  type FollowRelationshipById,
  fetchFollowRelationship,
  fetchFollowRelationships,
} from './followApi';
import { followRelationshipKey } from './queryKeys';

const cache = createBatchAside<FollowRelationship>({
  key: followRelationshipKey,
  readOne: (userId) => fetchFollowRelationship(userId),
  readBatch: async (userIds) => {
    const batch = await fetchFollowRelationships(userIds);
    return batch.map(({ userId, ...relationship }) => [userId, relationship]);
  },
});

export function useFollowRelationship(userId: string) {
  return cache.useValue(userId);
}

export function seedFollowRelationships(
  client: QueryClient,
  records: FollowRelationshipById[],
): void {
  cache.seed(
    client,
    records.map(({ userId, ...relationship }) => [userId, relationship]),
  );
}
