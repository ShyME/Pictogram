import { afterEach, expect, test, vi } from "vitest";
import { SessionExpiredError } from "@shared";
import { jsonResponse, stubFetch } from "@test-support/mock-fetch";
import { fetchFeedPage } from "@features/feed/feed-api";

afterEach(() => {
  vi.unstubAllGlobals();
});

test("returns an empty page for a viewer with nothing to see", async () => {
  stubFetch(() => jsonResponse({ items: [], nextCursor: null }));

  await expect(fetchFeedPage()).resolves.toEqual({ posts: [], nextCursor: null });
});

test("maps feed cards to posts and carries the cursor", async () => {
  stubFetch(() =>
    jsonResponse({
      items: [
        {
          postId: "p-1",
          authorId: "u-1",
          mediaId: "m-1",
          caption: "hi",
          publishedAt: "2026-09-01T10:00:00Z",
        },
      ],
      nextCursor: "CURSOR",
    }),
  );

  await expect(fetchFeedPage()).resolves.toEqual({
    posts: [
      {
        postId: "p-1",
        authorId: "u-1",
        mediaId: "m-1",
        caption: "hi",
        publishedAt: "2026-09-01T10:00:00Z",
      },
    ],
    nextCursor: "CURSOR",
  });
});

test("passes the cursor through as a query parameter", async () => {
  const calls = stubFetch(() => jsonResponse({ items: [], nextCursor: null }));

  await fetchFeedPage("NEXT");

  expect(new URL(calls[0].url).searchParams.get("cursor")).toBe("NEXT");
});

test("throws SessionExpiredError on a 401 the silent refresh could not fix", async () => {
  stubFetch(() => jsonResponse({}, 401));

  await expect(fetchFeedPage()).rejects.toBeInstanceOf(SessionExpiredError);
});

test("throws a generic error on an unexpected status", async () => {
  stubFetch(() => new Response(null, { status: 500 }));

  await expect(fetchFeedPage()).rejects.toThrow(/500/);
});
