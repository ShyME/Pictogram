import { afterEach, expect, test, vi } from "vitest";
import { jsonResponse, pathOf, stubFetch } from "../../../test/mock-fetch";
import { getAccessToken, setAccessToken } from "../model/session";
import { refreshAccessToken, signOut } from "./auth-api";

afterEach(() => {
  setAccessToken(null);
  vi.unstubAllGlobals();
});

test("refreshAccessToken stores and returns the new token", async () => {
  stubFetch(() => jsonResponse({ accessToken: "fresh", expiresInSeconds: 900 }));

  await expect(refreshAccessToken()).resolves.toBe("fresh");
  expect(getAccessToken()).toBe("fresh");
});

test("refreshAccessToken yields null and clears the token when the session is gone", async () => {
  setAccessToken("stale");
  stubFetch(() => jsonResponse({}, 401));

  await expect(refreshAccessToken()).resolves.toBeNull();
  expect(getAccessToken()).toBeNull();
});

test("signOut posts to the logout endpoint and drops the local token", async () => {
  setAccessToken("live");
  const calls = stubFetch(() => new Response(null, { status: 204 }));

  await signOut();

  expect(calls.map(pathOf)).toContain("/api/auth/logout");
  expect(calls[0].method).toBe("POST");
  expect(getAccessToken()).toBeNull();
});

test("signOut still clears the token if the logout call fails", async () => {
  setAccessToken("live");
  stubFetch(() => Promise.reject(new Error("offline")));

  await signOut();

  expect(getAccessToken()).toBeNull();
});
