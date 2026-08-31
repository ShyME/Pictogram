import { expect, test } from "vitest";
import { isUsernameShapeValid } from "./username";

test.each(["ada", "ada_lovelace", "a1_", "abc", "a".repeat(20)])(
  "accepts well-formed username %j",
  (value) => {
    expect(isUsernameShapeValid(value)).toBe(true);
  },
);

test.each(["", "ab", "a".repeat(21), "Ada", "ada lovelace", "adá", "ada-lovelace", "ada!"])(
  "rejects malformed username %j",
  (value) => {
    expect(isUsernameShapeValid(value)).toBe(false);
  },
);
