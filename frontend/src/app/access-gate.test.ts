import { afterEach, expect, test, vi } from "vitest";
import { jsonResponse, problemResponse, stubFetch } from "../test/mock-fetch";
import { requireAnonymous, requireOnboarded, requireOnboardedOrAnon } from "./access-gate";

function profileEndpoint(status: number, body: unknown = {}) {
  const slug = status === 404 ? "profile-not-found" : "unauthorized";
  stubFetch(() => (status === 200 ? jsonResponse(body) : problemResponse(slug, status)));
}

/** The gates throw `redirect(...)`; pull the `Location` out of the thrown Response. */
async function redirectFrom(gate: Promise<unknown>): Promise<string | null> {
  try {
    await gate;
    throw new Error("expected the gate to redirect");
  } catch (thrown) {
    if (thrown instanceof Response) return thrown.headers.get("Location");
    throw thrown;
  }
}

afterEach(() => {
  vi.unstubAllGlobals();
});

test("requireOnboarded hands an onboarded visitor their profile", async () => {
  profileEndpoint(200, { userId: "u-1", username: "ada" });

  await expect(requireOnboarded()).resolves.toEqual({
    userId: "u-1",
    username: "ada",
    displayName: null,
    bio: null,
  });
});

test("requireOnboarded sends a visitor with no session to /login", async () => {
  profileEndpoint(401);

  expect(await redirectFrom(requireOnboarded())).toBe("/login");
});

test("requireOnboarded sends a signed-in visitor with no profile to /onboarding", async () => {
  profileEndpoint(404);

  expect(await redirectFrom(requireOnboarded())).toBe("/onboarding");
});

test("requireAnonymous lets a visitor with no session through", async () => {
  profileEndpoint(401);

  await expect(requireAnonymous()).resolves.toBeNull();
});

test("requireAnonymous bounces an onboarded visitor to the feed", async () => {
  profileEndpoint(200, { userId: "u-1", username: "ada" });

  expect(await redirectFrom(requireAnonymous())).toBe("/");
});

test("requireAnonymous bounces a half-onboarded visitor to /onboarding", async () => {
  profileEndpoint(404);

  expect(await redirectFrom(requireAnonymous())).toBe("/onboarding");
});

test("requireOnboardedOrAnon lets a half-onboarded visitor through", async () => {
  profileEndpoint(404);

  await expect(requireOnboardedOrAnon()).resolves.toBeNull();
});

test("requireOnboardedOrAnon requires a session", async () => {
  profileEndpoint(401);

  expect(await redirectFrom(requireOnboardedOrAnon())).toBe("/login");
});

test("requireOnboardedOrAnon is skipped once a profile exists", async () => {
  profileEndpoint(200, { userId: "u-1", username: "ada" });

  expect(await redirectFrom(requireOnboardedOrAnon())).toBe("/");
});
