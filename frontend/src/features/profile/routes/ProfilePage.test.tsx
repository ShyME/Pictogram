import type { ReactNode } from "react";
import { expect, test, vi } from "vitest";
import { screen } from "@testing-library/react";
import { renderWithProviders } from "../../../test/render";
import { ProfilePage } from "./ProfilePage";
import type { ProfilePageData } from "./profile-loader";

let loaderData: ProfilePageData;

vi.mock("react-router", () => ({
  useLoaderData: () => loaderData,
  Link: ({ to, children }: { to: string; children: ReactNode }) => <a href={to}>{children}</a>,
}));

const found = (over: Partial<Extract<ProfilePageData, { status: "found" }>> = {}): ProfilePageData => ({
  status: "found",
  profile: { userId: "u-1", username: "ada_lovelace", displayName: "Ada Lovelace", bio: "Countess of Lovelace" },
  isOwnProfile: false,
  ...over,
});

test("shows the display name, handle, bio and follower/following slots", () => {
  loaderData = found();
  renderWithProviders(<ProfilePage />);

  expect(screen.getByRole("heading", { name: "Ada Lovelace" })).toBeInTheDocument();
  expect(screen.getByText("@ada_lovelace")).toBeInTheDocument();
  expect(screen.getByText("Countess of Lovelace")).toBeInTheDocument();
  expect(screen.getByText("followers")).toBeInTheDocument();
  expect(screen.getByText("following")).toBeInTheDocument();
});

test("falls back to the handle as the heading when there is no display name", () => {
  loaderData = found({
    profile: { userId: "u-1", username: "ada_lovelace", displayName: null, bio: null },
  });
  renderWithProviders(<ProfilePage />);

  expect(screen.getByRole("heading", { name: "@ada_lovelace" })).toBeInTheDocument();
});

test("another user's profile shows a follow slot, not an edit slot", () => {
  loaderData = found({ isOwnProfile: false });
  renderWithProviders(<ProfilePage />);

  expect(screen.getByRole("button", { name: /follow/i })).toBeInTheDocument();
  expect(screen.queryByRole("link", { name: /edit profile/i })).not.toBeInTheDocument();
});

test("the viewer's own profile shows an edit link to the settings page, not a follow slot", () => {
  loaderData = found({ isOwnProfile: true });
  renderWithProviders(<ProfilePage />);

  expect(screen.getByRole("link", { name: /edit profile/i })).toHaveAttribute(
    "href",
    "/settings/profile",
  );
  expect(screen.queryByRole("button", { name: /follow/i })).not.toBeInTheDocument();
});

test("an unknown username renders a clear not-found page", () => {
  loaderData = { status: "not-found", username: "ghost_user" };
  renderWithProviders(<ProfilePage />);

  expect(screen.getByRole("heading", { name: /doesn.t exist/i })).toBeInTheDocument();
  expect(screen.getByText("@ghost_user")).toBeInTheDocument();
});
