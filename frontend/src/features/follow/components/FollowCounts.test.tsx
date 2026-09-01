import { afterEach, expect, test, vi } from "vitest";
import { screen } from "@testing-library/react";
import { renderWithProviders } from "../../../test/render";
import { jsonResponse, stubFetch } from "../../../test/mock-fetch";
import { FollowCounts } from "./FollowCounts";

afterEach(() => {
  vi.unstubAllGlobals();
});

test("renders the follower and following counts from the relationship query", async () => {
  stubFetch(() =>
    jsonResponse({ followerCount: 42, followingCount: 7, followedByViewer: false }),
  );
  renderWithProviders(<FollowCounts userId="u-1" />);

  const followers = await screen.findByText("42");
  expect(followers).toBeInTheDocument();
  expect(screen.getByText("7")).toBeInTheDocument();
  expect(screen.getByText("followers")).toBeInTheDocument();
  expect(screen.getByText("following")).toBeInTheDocument();
});

test("shows zeros before the counts have loaded", () => {
  stubFetch(() => new Promise(() => {}));
  renderWithProviders(<FollowCounts userId="u-1" />);

  expect(screen.getAllByText("0")).toHaveLength(2);
});
