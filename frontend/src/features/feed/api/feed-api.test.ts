import { afterEach, expect, test, vi } from "vitest";
import { jsonResponse, stubFetch } from "../../../test/mock-fetch";
import { SessionExpiredError, fetchFeed } from "./feed-api";

afterEach(() => {
  vi.unstubAllGlobals();
});

test("returns an empty list for an empty page", async () => {
  stubFetch(() => jsonResponse({ items: [], nextCursor: null }));

  await expect(fetchFeed()).resolves.toEqual([]);
});

test("maps feed cards to items", async () => {
  stubFetch(() => jsonResponse({ items: [{ postId: "p-1", author: "ada" }] }));

  await expect(fetchFeed()).resolves.toEqual([{ postId: "p-1", author: "ada" }]);
});

test("throws SessionExpiredError on 401 (silent refresh already failed)", async () => {
  stubFetch(() => jsonResponse({}, 401));

  await expect(fetchFeed()).rejects.toBeInstanceOf(SessionExpiredError);
});

test("throws a generic error on an unexpected status", async () => {
  stubFetch(() => new Response(null, { status: 500 }));

  await expect(fetchFeed()).rejects.toThrow(/500/);
});
