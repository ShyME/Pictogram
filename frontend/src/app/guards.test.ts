import { afterEach, expect, test, vi } from "vitest";
import { jsonResponse, problemResponse, stubFetch } from "../test/mock-fetch";
import { loginLoader, newPostLoader, onboardingLoader, rootLoader } from "./guards";

function profileEndpoint(status: number, body: unknown = {}) {
  const slug = status === 404 ? "profile-not-found" : "unauthorized";
  stubFetch(() => (status === 200 ? jsonResponse(body) : problemResponse(slug, status)));
}

function redirectTarget(result: unknown): string | null {
  return result instanceof Response ? result.headers.get("Location") : null;
}

afterEach(() => {
  vi.unstubAllGlobals();
});

test("rootLoader: 401 -> /login, 404 -> /onboarding, 200 -> the profile", async () => {
  profileEndpoint(401);
  expect(redirectTarget(await rootLoader())).toBe("/login");

  profileEndpoint(404);
  expect(redirectTarget(await rootLoader())).toBe("/onboarding");

  profileEndpoint(200, { userId: "u-1", username: "ada" });
  expect(await rootLoader()).toEqual({
    profile: { userId: "u-1", username: "ada", displayName: null, bio: null },
  });
});

test("loginLoader bounces an onboarded user to the feed and a half-onboarded one to onboarding", async () => {
  profileEndpoint(200, { userId: "u-1", username: "ada" });
  expect(redirectTarget(await loginLoader())).toBe("/");

  profileEndpoint(404);
  expect(redirectTarget(await loginLoader())).toBe("/onboarding");

  profileEndpoint(401);
  expect(await loginLoader()).toBeNull();
});

test("onboardingLoader requires a session and is skipped once a profile exists", async () => {
  profileEndpoint(401);
  expect(redirectTarget(await onboardingLoader())).toBe("/login");

  profileEndpoint(200, { userId: "u-1", username: "ada" });
  expect(redirectTarget(await onboardingLoader())).toBe("/");

  profileEndpoint(404);
  expect(await onboardingLoader()).toBeNull();
});

test("newPostLoader: 401 -> /login, 404 -> /onboarding, 200 -> the profile", async () => {
  profileEndpoint(401);
  expect(redirectTarget(await newPostLoader())).toBe("/login");

  profileEndpoint(404);
  expect(redirectTarget(await newPostLoader())).toBe("/onboarding");

  profileEndpoint(200, { userId: "u-1", username: "ada" });
  expect(await newPostLoader()).toEqual({
    profile: { userId: "u-1", username: "ada", displayName: null, bio: null },
  });
});
