import { afterEach, beforeEach, expect, test, vi } from "vitest";
import { api } from "@shared";
import { jsonResponse, pathOf, stubFetch } from "@test-support/mock-fetch";
import { getAccessToken, setAccessToken } from "@features/auth/session";
import { authMiddleware } from "@features/auth/auth-client";

beforeEach(() => {
  api.use(authMiddleware);
  setAccessToken(null);
});

afterEach(() => {
  api.eject(authMiddleware);
  setAccessToken(null);
  vi.unstubAllGlobals();
});

test("attaches the current access token as a bearer header", async () => {
  setAccessToken("token-abc");
  const calls = stubFetch(() => jsonResponse({ username: "ada" }));

  await api.GET("/api/profiles/me");

  expect(calls[0].headers.get("Authorization")).toBe("Bearer token-abc");
});

test("on 401 it refreshes once and retries the request with the new token", async () => {
  setAccessToken("stale");
  const calls = stubFetch((request, hits) => {
    if (pathOf(request) === "/api/auth/refresh") {
      return jsonResponse({ accessToken: "fresh", expiresInSeconds: 900 });
    }
    return hits === 0 ? jsonResponse({}, 401) : jsonResponse({ username: "ada" });
  });

  const { data, response } = await api.GET("/api/profiles/me");

  expect(response.status).toBe(200);
  expect(data).toEqual({ username: "ada" });
  expect(calls.filter((c) => pathOf(c) === "/api/auth/refresh")).toHaveLength(1);
  expect(calls.at(-1)?.headers.get("Authorization")).toBe("Bearer fresh");
  expect(getAccessToken()).toBe("fresh");
});

test("when the refresh fails it returns the 401 and drops the token", async () => {
  setAccessToken("stale");
  const calls = stubFetch(() => jsonResponse({}, 401));

  const { response } = await api.GET("/api/profiles/me");

  expect(response.status).toBe(401);
  expect(getAccessToken()).toBeNull();
  expect(calls.filter((c) => pathOf(c) === "/api/profiles/me")).toHaveLength(1);
});

test("concurrent 401s share a single refresh round-trip", async () => {
  setAccessToken("stale");
  const calls = stubFetch((request, hits) => {
    if (pathOf(request) === "/api/auth/refresh") {
      return jsonResponse({ accessToken: "fresh", expiresInSeconds: 900 });
    }
    if (hits === 0) return jsonResponse({}, 401);
    return jsonResponse(pathOf(request) === "/api/feed" ? { items: [] } : { username: "ada" });
  });

  await Promise.all([api.GET("/api/profiles/me"), api.GET("/api/feed")]);

  expect(calls.filter((c) => pathOf(c) === "/api/auth/refresh")).toHaveLength(1);
});
