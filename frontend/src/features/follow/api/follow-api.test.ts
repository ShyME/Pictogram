import { afterEach, expect, test, vi } from "vitest";
import { jsonResponse, pathOf, problemResponse, stubFetch } from "../../../test/mock-fetch";
import { fetchFollowRelationship, followUser, unfollowUser } from "./follow-api";

afterEach(() => {
  vi.unstubAllGlobals();
});

test("fetchFollowRelationship maps the counts and viewer flag", async () => {
  stubFetch(() =>
    jsonResponse({ followerCount: 3, followingCount: 1, followedByViewer: true }),
  );

  await expect(fetchFollowRelationship("u-1")).resolves.toEqual({
    followerCount: 3,
    followingCount: 1,
    followedByViewer: true,
  });
});

test("fetchFollowRelationship retries anonymously when a stale token makes the public read 401", async () => {
  stubFetch((request, hits) => {
    if (new URL(request.url).pathname === "/api/auth/refresh") {
      return problemResponse("unauthorized", 401);
    }
    return hits === 0
      ? problemResponse("unauthorized", 401)
      : jsonResponse({ followerCount: 2, followingCount: 0, followedByViewer: false });
  });

  await expect(fetchFollowRelationship("u-1")).resolves.toEqual({
    followerCount: 2,
    followingCount: 0,
    followedByViewer: false,
  });
});

test("followUser PUTs the relationship resource", async () => {
  const calls = stubFetch(() => new Response(null, { status: 204 }));

  await expect(followUser("u-9")).resolves.toBeUndefined();

  expect(calls[0].method).toBe("PUT");
  expect(pathOf(calls[0])).toBe("/api/follows/u-9");
});

test("unfollowUser DELETEs the relationship resource", async () => {
  const calls = stubFetch(() => new Response(null, { status: 204 }));

  await expect(unfollowUser("u-9")).resolves.toBeUndefined();

  expect(calls[0].method).toBe("DELETE");
  expect(pathOf(calls[0])).toBe("/api/follows/u-9");
});

test("followUser throws when the write fails", async () => {
  stubFetch((request) => {
    if (new URL(request.url).pathname === "/api/auth/refresh") return problemResponse("unauthorized", 401);
    return problemResponse("unauthorized", 401);
  });

  await expect(followUser("u-9")).rejects.toThrow(/Following failed/);
});
