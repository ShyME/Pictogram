import type { ReactNode } from "react";
import { afterEach, expect, test, vi } from "vitest";
import { screen } from "@testing-library/react";
import { jsonResponse, pathOf, stubFetch } from "@test-support/mock-fetch";
import { renderWithProviders } from "@test-support/render";
import { FollowListPage } from "@features/follow/lists/FollowListPage";
import type { FollowListData } from "@features/follow/lists/follow-list";

const loaderData: FollowListData = {
  status: "found",
  target: { userId: "u-1", username: "ada" },
  viewerId: "viewer-7",
};

vi.mock("react-router", () => ({
  useLoaderData: () => loaderData,
  Link: ({ to, children }: { to: string; children: ReactNode }) => <a href={to}>{children}</a>,
}));

afterEach(() => {
  vi.unstubAllGlobals();
});

test("seeds every row's relationship so the real FollowButtons never fetch one at a time", async () => {
  const calls = stubFetch((request) => {
    const { pathname } = new URL(request.url);
    if (pathname === "/api/follows/u-1/followers") {
      return jsonResponse({ items: ["u-carol", "u-bob"], nextCursor: null });
    }
    if (pathname === "/api/profiles") {
      return jsonResponse([
        { userId: "u-carol", username: "carol", displayName: "Carol" },
        { userId: "u-bob", username: "bob", displayName: "Bob" },
      ]);
    }
    if (pathname === "/api/follows") {
      return jsonResponse([
        { userId: "u-carol", followerCount: 3, followingCount: 1, followedByViewer: true },
        { userId: "u-bob", followerCount: 0, followingCount: 0, followedByViewer: false },
      ]);
    }
    throw new Error(`unexpected request: ${pathname}`);
  });

  renderWithProviders(<FollowListPage mode="followers" />);

  // Each button renders straight from the seeded cache: Carol is followed, Bob is not.
  expect(await screen.findByRole("button", { name: "Following" })).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Follow" })).toBeInTheDocument();

  const paths = calls.map(pathOf);
  expect(paths.filter((p) => p === "/api/follows/u-1/followers")).toHaveLength(1);
  expect(paths.filter((p) => p === "/api/profiles")).toHaveLength(1);
  expect(paths.filter((p) => p === "/api/follows")).toHaveLength(1);
  expect(paths.filter((p) => p === "/api/follows/u-carol" || p === "/api/follows/u-bob")).toEqual([]);
});
