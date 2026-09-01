import type { components } from "@shared";

export type FollowRelationship = {
  followerCount: number;
  followingCount: number;
  followedByViewer: boolean;
};

export function toFollowRelationship(
  view: components["schemas"]["FollowRelationship"],
): FollowRelationship {
  return {
    followerCount: view.followerCount ?? 0,
    followingCount: view.followingCount ?? 0,
    followedByViewer: view.followedByViewer ?? false,
  };
}
