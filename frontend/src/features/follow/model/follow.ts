import type { components } from "@shared";

/**
 * A user's follow standing plus the viewer's relationship to them. `followedByViewer` is
 * always `false` for a signed-out visitor — the counts are public, the relationship isn't.
 */
export type FollowRelationship = {
  followerCount: number;
  followingCount: number;
  followedByViewer: boolean;
};

// Every field is optional in the generated schema (no `required` marker), but the endpoint
// always returns all three; the counts default to 0 and the flag to false defensively.
export function toFollowRelationship(
  view: components["schemas"]["FollowRelationship"],
): FollowRelationship {
  return {
    followerCount: view.followerCount ?? 0,
    followingCount: view.followingCount ?? 0,
    followedByViewer: view.followedByViewer ?? false,
  };
}
