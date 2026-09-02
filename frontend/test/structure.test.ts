import { existsSync, globSync } from 'node:fs';
import path from 'node:path';
import { expect, test } from 'vitest';

test('no test files live inside src/', () => {
  const stray = globSync('src/**/*.test.{ts,tsx}');
  expect(stray).toEqual([]);
});

test("every test file's directory mirrors a real src/ directory", () => {
  const testFiles = globSync('test/**/*.test.{ts,tsx}');
  const misplaced = testFiles.filter((file) => {
    const mirroredSrcDir = path.dirname(file).replace(/^test\//, 'src/');
    return !existsSync(mirroredSrcDir);
  });
  expect(misplaced).toEqual([]);
});
