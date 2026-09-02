export type { FollowListMode } from '../followApi';

export type FollowListTarget = {
  userId: string;
  username: string;
};

export type FollowListData =
  | { status: 'found'; target: FollowListTarget; viewerId: string }
  | { status: 'not-found'; username: string };
