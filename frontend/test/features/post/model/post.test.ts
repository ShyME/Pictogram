import { expect, test } from "vitest";
import { originalUrl, thumbnailUrl, toPost } from "@features/post/model/post";

test("toPost fills the ids and treats a missing caption as null", () => {
  expect(
    toPost({ postId: "p-1", authorId: "u-1", mediaId: "m-1", publishedAt: "2026-09-01T12:00:00Z" }),
  ).toEqual({
    postId: "p-1",
    authorId: "u-1",
    mediaId: "m-1",
    caption: null,
    publishedAt: "2026-09-01T12:00:00Z",
  });

  expect(toPost({ postId: "p-1", caption: "hi" }).caption).toBe("hi");
});

test("the media URLs point at the rendition endpoints", () => {
  expect(thumbnailUrl("m-1")).toBe("/api/media/m-1/thumbnail");
  expect(originalUrl("m-1")).toBe("/api/media/m-1/original");
});
