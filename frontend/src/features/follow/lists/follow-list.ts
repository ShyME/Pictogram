import type { FollowListMode } from "../follow-api";

export type { FollowListMode };

export type FollowListTarget = {
  userId: string;
  username: string;
};

export type FollowListData =
  | { status: "found"; target: FollowListTarget; viewerId: string }
  | { status: "not-found"; username: string };
