import { afterEach, expect, test, vi } from "vitest";
import { jsonResponse, problemResponse, stubFetch } from "../../../test/mock-fetch";
import { fetchMyProfile, submitOnboarding } from "./profile-api";

afterEach(() => {
  vi.unstubAllGlobals();
});

test("fetchMyProfile maps 200 to an onboarded profile", async () => {
  stubFetch(() =>
    jsonResponse({ userId: "u-1", username: "ada", displayName: "Ada", bio: null }),
  );

  await expect(fetchMyProfile()).resolves.toEqual({
    status: "onboarded",
    profile: { userId: "u-1", username: "ada", displayName: "Ada", bio: null },
  });
});

test("fetchMyProfile maps 404 to not-onboarded", async () => {
  stubFetch(() => problemResponse("profile-not-found", 404));

  await expect(fetchMyProfile()).resolves.toEqual({ status: "not-onboarded" });
});

test("fetchMyProfile maps 401 to unauthenticated", async () => {
  stubFetch(() => problemResponse("unauthorized", 401));

  await expect(fetchMyProfile()).resolves.toEqual({ status: "unauthenticated" });
});

test("submitOnboarding returns the created profile on 201", async () => {
  stubFetch(() =>
    jsonResponse({ userId: "u-1", username: "ada_lovelace", displayName: null, bio: null }, 201),
  );

  await expect(submitOnboarding({ username: "ada_lovelace" })).resolves.toEqual({
    status: "created",
    profile: { userId: "u-1", username: "ada_lovelace", displayName: null, bio: null },
  });
});

test("submitOnboarding distinguishes a taken username from a malformed one", async () => {
  stubFetch(() => problemResponse("username-taken", 409));
  await expect(submitOnboarding({ username: "ada" })).resolves.toEqual({
    status: "username-taken",
  });

  stubFetch(() => problemResponse("username-invalid", 400));
  await expect(submitOnboarding({ username: "ada" })).resolves.toEqual({
    status: "username-invalid",
  });
});

test("submitOnboarding surfaces an over-long display name or bio", async () => {
  stubFetch(() => problemResponse("profile-details-invalid", 400));

  await expect(submitOnboarding({ username: "ada", bio: "x" })).resolves.toEqual({
    status: "details-invalid",
  });
});

test("submitOnboarding treats an existing profile as already-onboarded, not an error", async () => {
  stubFetch(() => problemResponse("already-onboarded", 409));

  await expect(submitOnboarding({ username: "ada" })).resolves.toEqual({
    status: "already-onboarded",
  });
});

test("submitOnboarding omits blank optional fields from the request body", async () => {
  let sentBody: unknown;
  stubFetch(async (request) => {
    sentBody = await request.clone().json();
    return jsonResponse({ userId: "u-1", username: "ada_lovelace" }, 201);
  });

  await submitOnboarding({ username: "ada_lovelace", displayName: "  ", bio: "" });

  expect(sentBody).toEqual({ username: "ada_lovelace" });
});
