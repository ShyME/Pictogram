import { afterEach, expect, test, vi } from "vitest";
import { jsonResponse, pathOf, problemResponse, stubFetch } from "../../../test/mock-fetch";
import {
  fetchFollowListPage,
  fetchFollowRelationship,
  followUser,
  unfollowUser,
} from "./follow-api";

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

test("fetchFollowListPage composes the follow list with one profile batch, keeping order", async () => {
  const calls = stubFetch((request) => {
    const url = new URL(request.url);
    if (url.pathname === "/api/follows/u-1/followers") {
      return jsonResponse({ items: ["u-9", "u-3"], nextCursor: "CURSOR" });
    }
    if (url.pathname === "/api/profiles") {
      // The batch endpoint may return them in any order.
      return jsonResponse([
        { userId: "u-3", username: "carol", displayName: "Carol" },
        { userId: "u-9", username: "ada", displayName: null },
      ]);
    }
    throw new Error(`unexpected ${url.pathname}`);
  });

  await expect(fetchFollowListPage("followers", "u-1", undefined)).resolves.toEqual({
    accounts: [
      { userId: "u-9", username: "ada", displayName: null },
      { userId: "u-3", username: "carol", displayName: "Carol" },
    ],
    nextCursor: "CURSOR",
  });

  expect(calls.map(pathOf)).toEqual(["/api/follows/u-1/followers", "/api/profiles"]);
  expect(new URL(calls[1].url).searchParams.getAll("ids")).toEqual(["u-9", "u-3"]);
});

test("fetchFollowListPage skips a listed id the profile batch doesn't return", async () => {
  stubFetch((request) => {
    const url = new URL(request.url);
    if (url.pathname === "/api/follows/u-1/following") {
      return jsonResponse({ items: ["u-9", "u-gone"], nextCursor: null });
    }
    return jsonResponse([{ userId: "u-9", username: "ada", displayName: "Ada" }]);
  });

  const page = await fetchFollowListPage("following", "u-1");
  expect(page.accounts).toEqual([{ userId: "u-9", username: "ada", displayName: "Ada" }]);
  expect(page.nextCursor).toBeNull();
});

test("fetchFollowListPage makes no profile call for an empty page", async () => {
  const calls = stubFetch(() => jsonResponse({ items: [], nextCursor: null }));

  const page = await fetchFollowListPage("followers", "u-1");
  expect(page.accounts).toEqual([]);
  expect(calls.map(pathOf)).toEqual(["/api/follows/u-1/followers"]);
});

test("followUser throws when the write fails", async () => {
  stubFetch((request) => {
    if (new URL(request.url).pathname === "/api/auth/refresh") return problemResponse("unauthorized", 401);
    return problemResponse("unauthorized", 401);
  });

  await expect(followUser("u-9")).rejects.toThrow(/Following failed/);
});
