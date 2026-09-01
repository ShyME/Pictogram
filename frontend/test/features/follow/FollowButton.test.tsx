import { afterEach, expect, test, vi } from "vitest";
import { fireEvent, screen, waitFor } from "@testing-library/react";
import { renderWithProviders } from "@test-support/render";
import { jsonResponse, pathOf, stubFetch } from "@test-support/mock-fetch";
import { FollowButton } from "@features/follow/FollowButton";

afterEach(() => {
  vi.unstubAllGlobals();
});

const relationship = (followedByViewer: boolean, followerCount = followedByViewer ? 1 : 0) =>
  jsonResponse({ followerCount, followingCount: 0, followedByViewer });

async function findEnabledButton(name: string) {
  const button = await screen.findByRole("button", { name });
  await waitFor(() => expect(button).toBeEnabled());
  return button;
}

test("shows Follow when the viewer is not following the user", async () => {
  stubFetch(() => relationship(false));
  renderWithProviders(<FollowButton userId="u-1" />);

  expect(await screen.findByRole("button", { name: "Follow" })).toHaveAttribute(
    "aria-pressed",
    "false",
  );
});

test("shows Following when the viewer already follows the user", async () => {
  stubFetch(() => relationship(true));
  renderWithProviders(<FollowButton userId="u-1" />);

  expect(await screen.findByRole("button", { name: "Following" })).toHaveAttribute(
    "aria-pressed",
    "true",
  );
});

test("clicking Follow PUTs the follow and flips the label once the count reloads", async () => {
  const calls = stubFetch((request) => {
    if (request.method === "PUT") return new Response(null, { status: 204 });
    const followed = calls.some((c) => c.method === "PUT");
    return relationship(followed);
  });
  renderWithProviders(<FollowButton userId="u-1" />);

  fireEvent.click(await findEnabledButton("Follow"));

  expect(await screen.findByRole("button", { name: "Following" })).toBeInTheDocument();
  const put = calls.find((c) => c.method === "PUT")!;
  expect(pathOf(put)).toBe("/api/follows/u-1");
});

test("clicking Following DELETEs the follow", async () => {
  const calls = stubFetch((request) => {
    if (request.method === "DELETE") return new Response(null, { status: 204 });
    const unfollowed = calls.some((c) => c.method === "DELETE");
    return relationship(!unfollowed);
  });
  renderWithProviders(<FollowButton userId="u-1" />);

  fireEvent.click(await findEnabledButton("Following"));

  expect(await screen.findByRole("button", { name: "Follow" })).toBeInTheDocument();
  expect(calls.some((c) => c.method === "DELETE" && pathOf(c) === "/api/follows/u-1")).toBe(true);
});

test("surfaces an error when the follow write fails", async () => {
  stubFetch((request) => {
    if (request.method === "PUT") return new Response(null, { status: 500 });
    return relationship(false);
  });
  renderWithProviders(<FollowButton userId="u-1" />);

  fireEvent.click(await findEnabledButton("Follow"));

  await waitFor(() => expect(screen.getByRole("alert")).toHaveTextContent(/didn.t work/i));
});
