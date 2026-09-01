import type { ReactNode } from "react";
import { beforeEach, expect, test, vi } from "vitest";
import { fireEvent, screen, waitFor } from "@testing-library/react";
import { renderWithProviders } from "../../../test/render";
import { FollowListPage } from "./FollowListPage";
import type { FollowListData } from "../model/follow-list";
import type { Account } from "../model/account";

let loaderData: FollowListData;

vi.mock("react-router", () => ({
  useLoaderData: () => loaderData,
  Link: ({ to, children }: { to: string; children: ReactNode }) => <a href={to}>{children}</a>,
}));

const fetchFollowListPage = vi.fn();
vi.mock("../api/follow-api", () => ({
  fetchFollowListPage: (...args: unknown[]) => fetchFollowListPage(...args),
}));

vi.mock("../components/AccountList", () => ({
  AccountList: ({ accounts, viewerId }: { accounts: Account[]; viewerId: string }) => (
    <ul>
      {accounts.map((a) => (
        <li key={a.userId}>
          {a.username} · viewer {viewerId}
        </li>
      ))}
    </ul>
  ),
}));

const found: FollowListData = {
  status: "found",
  target: { userId: "u-1", username: "ada" },
  viewerId: "viewer-7",
};

const account = (username: string): Account => ({ userId: `id-${username}`, username, displayName: null });

beforeEach(() => {
  loaderData = found;
  fetchFollowListPage.mockReset();
});

test("shows who follows the account, newest first, from the loader's target", async () => {
  fetchFollowListPage.mockResolvedValue({
    accounts: [account("carol"), account("bob")],
    relationships: [],
    nextCursor: null,
  });

  renderWithProviders(<FollowListPage mode="followers" />);

  expect(await screen.findByText("carol · viewer viewer-7")).toBeInTheDocument();
  expect(screen.getByRole("heading", { name: /people who follow @ada/i })).toBeInTheDocument();
  expect(fetchFollowListPage).toHaveBeenCalledWith("followers", "u-1", undefined);
});

test("following mode changes the heading and the endpoint", async () => {
  fetchFollowListPage.mockResolvedValue({
    accounts: [account("bob")],
    relationships: [],
    nextCursor: null,
  });

  renderWithProviders(<FollowListPage mode="following" />);

  expect(await screen.findByText("bob · viewer viewer-7")).toBeInTheDocument();
  expect(screen.getByRole("heading", { name: /accounts @ada follows/i })).toBeInTheDocument();
  expect(fetchFollowListPage).toHaveBeenCalledWith("following", "u-1", undefined);
});

test("an empty followers list gets a friendly empty state", async () => {
  fetchFollowListPage.mockResolvedValue({ accounts: [], relationships: [], nextCursor: null });

  renderWithProviders(<FollowListPage mode="followers" />);

  expect(await screen.findByText("No followers yet")).toBeInTheDocument();
});

test("an empty following list gets its own empty state", async () => {
  fetchFollowListPage.mockResolvedValue({ accounts: [], relationships: [], nextCursor: null });

  renderWithProviders(<FollowListPage mode="following" />);

  expect(await screen.findByText("Not following anyone yet")).toBeInTheDocument();
});

test("Load more pages on the cursor", async () => {
  fetchFollowListPage
    .mockResolvedValueOnce({ accounts: [account("carol")], relationships: [], nextCursor: "CURSOR" })
    .mockResolvedValueOnce({ accounts: [account("bob")], relationships: [], nextCursor: null });

  renderWithProviders(<FollowListPage mode="followers" />);

  await screen.findByText("carol · viewer viewer-7");
  fireEvent.click(screen.getByRole("button", { name: /load more/i }));

  expect(await screen.findByText("bob · viewer viewer-7")).toBeInTheDocument();
  expect(fetchFollowListPage).toHaveBeenLastCalledWith("followers", "u-1", "CURSOR");
  await waitFor(() =>
    expect(screen.queryByRole("button", { name: /load more/i })).not.toBeInTheDocument(),
  );
});

test("an unknown username renders a not-found page", () => {
  loaderData = { status: "not-found", username: "ghost" };

  renderWithProviders(<FollowListPage mode="followers" />);

  expect(screen.getByRole("heading", { name: /doesn.t exist/i })).toBeInTheDocument();
  expect(screen.getByText("@ghost")).toBeInTheDocument();
});
