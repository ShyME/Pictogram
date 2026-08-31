import { afterEach, expect, test, vi } from "vitest";
import { jsonResponse, problemResponse, stubFetch } from "../../../test/mock-fetch";
import { editProfileLoader } from "./edit-profile-loader";

afterEach(() => {
  vi.unstubAllGlobals();
});

function redirectTarget(result: unknown): string | null {
  return result instanceof Response ? result.headers.get("Location") : null;
}

test("hands the current profile to the form", async () => {
  stubFetch(() => jsonResponse({ userId: "u-1", username: "ada", displayName: "Ada", bio: "hi" }));

  await expect(editProfileLoader()).resolves.toEqual({
    profile: { userId: "u-1", username: "ada", displayName: "Ada", bio: "hi" },
  });
});

test("sends a visitor with no session to /login", async () => {
  stubFetch(() => problemResponse("unauthorized", 401));

  expect(redirectTarget(await editProfileLoader())).toBe("/login");
});

test("sends a signed-in user with no profile to /onboarding", async () => {
  stubFetch(() => problemResponse("profile-not-found", 404));

  expect(redirectTarget(await editProfileLoader())).toBe("/onboarding");
});
