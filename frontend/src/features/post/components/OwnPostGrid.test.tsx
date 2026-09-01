import { afterEach, expect, test, vi } from "vitest";
import { fireEvent, screen } from "@testing-library/react";
import { renderWithProviders } from "../../../test/render";
import { jsonResponse, stubFetch } from "../../../test/mock-fetch";
import { OwnPostGrid } from "./OwnPostGrid";

afterEach(() => {
  vi.unstubAllGlobals();
});

test("shows an empty state when the author has no posts", async () => {
  stubFetch(() => jsonResponse({ items: [], nextCursor: null }));
  renderWithProviders(<OwnPostGrid authorId="u-1" />);

  expect(await screen.findByText("No posts yet")).toBeInTheDocument();
});

test("renders each post's thumbnail, newest first as the server sends them", async () => {
  stubFetch(() =>
    jsonResponse({
      items: [
        { postId: "p-2", mediaId: "m-2", caption: "later", publishedAt: "t2" },
        { postId: "p-1", mediaId: "m-1", caption: null, publishedAt: "t1" },
      ],
      nextCursor: null,
    }),
  );
  renderWithProviders(<OwnPostGrid authorId="u-1" />);

  const images = await screen.findAllByRole("img");
  expect(images.map((img) => img.getAttribute("src"))).toEqual([
    "/api/media/m-2/thumbnail",
    "/api/media/m-1/thumbnail",
  ]);
});

test("pages on the keyset cursor when 'Load more' is clicked", async () => {
  const calls = stubFetch((_request, hits) =>
    hits === 0
      ? jsonResponse({ items: [{ postId: "p-2", mediaId: "m-2", publishedAt: "t2" }], nextCursor: "CURSOR" })
      : jsonResponse({ items: [{ postId: "p-1", mediaId: "m-1", publishedAt: "t1" }], nextCursor: null }),
  );
  renderWithProviders(<OwnPostGrid authorId="u-1" />);

  fireEvent.click(await screen.findByRole("button", { name: /load more/i }));

  expect(await screen.findByAltText("A post")).toBeInTheDocument();
  expect(screen.getAllByRole("img")).toHaveLength(2);
  expect(new URL(calls[1].url).searchParams.get("cursor")).toBe("CURSOR");
  expect(screen.queryByRole("button", { name: /load more/i })).not.toBeInTheDocument();
});
