import { existsSync, globSync } from "node:fs";
import { dirname } from "node:path";
import { expect, test } from "vitest";

// test/ mirrors src/ directory-for-directory (see the frontend section of
// docs/agents/domain.md); these two checks are what keep that guarantee from
// silently rotting as files move.

test("no test files live inside src/", () => {
  const stray = globSync("src/**/*.test.{ts,tsx}");
  expect(stray).toEqual([]);
});

test("every test file's directory mirrors a real src/ directory", () => {
  const testFiles = globSync("test/**/*.test.{ts,tsx}");
  const misplaced = testFiles.filter((file) => {
    const mirroredSrcDir = dirname(file).replace(/^test\//, "src/");
    return !existsSync(mirroredSrcDir);
  });
  expect(misplaced).toEqual([]);
});
