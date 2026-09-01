import { afterEach, expect, test, vi } from "vitest";
import { screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router";
import { renderWithProviders } from "@test-support/render";
import { jsonResponse, stubFetch } from "@test-support/mock-fetch";
import { FeedPage } from "@features/feed/FeedPage";

function renderFeed() {
  renderWithProviders(
    <MemoryRouter initialEntries={["/"]}>
      <Routes>
        <Route path="/" element={<FeedPage />} />
        <Route path="/login" element={<p>Login screen</p>} />
      </Routes>
    </MemoryRouter>,
  );
}

afterEach(() => {
  vi.unstubAllGlobals();
});

test("shows a friendly 'find people to follow' empty state", async () => {
  stubFetch(() => jsonResponse({ items: [], nextCursor: null }));
  renderFeed();

  expect(await screen.findByText(/find people to follow/i)).toBeInTheDocument();
});

test("redirects to /login when the session has expired", async () => {
  stubFetch(() => jsonResponse({}, 401));
  renderFeed();

  expect(await screen.findByText("Login screen", undefined, { timeout: 3000 })).toBeInTheDocument();
});

test("surfaces a load failure without crashing", async () => {
  stubFetch(() => new Response(null, { status: 500 }));
  renderFeed();

  expect(
    await screen.findByText(/couldn.t load your feed/i, undefined, { timeout: 3000 }),
  ).toBeInTheDocument();
});
