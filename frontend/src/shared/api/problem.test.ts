import { expect, test } from "vitest";
import { problemSlug } from "./problem";

test("extracts the slug from a Pictogram problem type", () => {
  expect(problemSlug({ type: "https://pictogram.dev/problems/username-taken" })).toBe(
    "username-taken",
  );
});

test("returns null when the body is not a Problem Detail", () => {
  expect(problemSlug(null)).toBeNull();
  expect(problemSlug("not an object")).toBeNull();
  expect(problemSlug({})).toBeNull();
  expect(problemSlug({ type: 42 })).toBeNull();
});

test("passes a non-Pictogram type through unchanged", () => {
  expect(problemSlug({ type: "about:blank" })).toBe("about:blank");
});
