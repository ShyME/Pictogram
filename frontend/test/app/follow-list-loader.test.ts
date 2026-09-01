import type { LoaderFunctionArgs } from "react-router";
import { afterEach, expect, test, vi } from "vitest";
import { jsonResponse, problemResponse, stubFetch } from "@test-support/mock-fetch";
import { followListLoader } from "@app/follow-list-loader";

afterEach(() => {
  vi.unstubAllGlobals();
});

const load = (username: string) =>
  followListLoader({ params: { username } } as unknown as LoaderFunctionArgs);

function route(handlers: { me: () => Response; profile: () => Response }) {
  stubFetch((request) => {
    const { pathname } = new URL(request.url);
    if (pathname === "/api/profiles/me") return handlers.me();
    return handlers.profile();
  });
}

test("resolves the target profile and the viewer's own id", async () => {
  route({
    me: () => jsonResponse({ userId: "viewer-1", username: "me", displayName: "Me", bio: null }),
    profile: () => jsonResponse({ userId: "u-2", username: "grace", displayName: "Grace", bio: null }),
  });

  await expect(load("grace")).resolves.toEqual({
    status: "found",
    target: { userId: "u-2", username: "grace" },
    viewerId: "viewer-1",
  });
});

test("sends a signed-out visitor who followed a count link to /login", async () => {
  route({
    me: () => problemResponse("unauthorized", 401),
    profile: () => jsonResponse({ userId: "u-2", username: "grace" }),
  });

  const result = await load("grace");
  expect(result).toBeInstanceOf(Response);
  expect((result as Response).headers.get("Location")).toBe("/login");
});

test("sends a session-without-profile visitor to /onboarding", async () => {
  route({
    me: () => problemResponse("profile-not-found", 404),
    profile: () => jsonResponse({ userId: "u-2", username: "grace" }),
  });

  expect(((await load("grace")) as Response).headers.get("Location")).toBe("/onboarding");
});

test("echoes the username back when no such account exists", async () => {
  route({
    me: () => jsonResponse({ userId: "viewer-1", username: "me", bio: null }),
    profile: () => problemResponse("profile-not-found", 404),
  });

  await expect(load("ghost_user")).resolves.toEqual({ status: "not-found", username: "ghost_user" });
});
