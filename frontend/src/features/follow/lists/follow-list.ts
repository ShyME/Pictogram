import type { FollowListMode } from "../follow-api";

export type { FollowListMode };

export type FollowListTarget = {
  userId: string;
  username: string;
};

/**
 * What the `/u/:username/{followers,following}` loader resolves: the account whose list is
 * being shown, plus the signed-in viewer's own id (so their own row hides its follow
 * button). The endpoints are authenticated, so the loader redirects a signed-out visitor to
 * `/login` before this is ever produced.
 */
export type FollowListData =
  | { status: "found"; target: FollowListTarget; viewerId: string }
  | { status: "not-found"; username: string };
