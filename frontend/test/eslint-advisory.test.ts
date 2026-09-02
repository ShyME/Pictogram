import { ESLint, type Linter } from 'eslint';
import unicorn from 'eslint-plugin-unicorn';
import tseslint from 'typescript-eslint';
import { expect, test } from 'vitest';

// ADR-0010 / #68: the stricter type-aware typescript-eslint sets and unicorn's `recommended`
// ship advisory — every rule they add on top of `typescript-eslint`'s `recommended` must
// resolve to `warn` (1) or `off` (0), never `error` (2), so `pnpm lint` stays green and CI
// with it. #75 clears the backlog and promotes them; until then this guards the wrapper in
// eslint.config.js from being dropped early.
const baseline = new Set(
  tseslint.configs.recommended.flatMap((config) => Object.keys(config.rules ?? {})),
);

const introducedRules = [
  ...tseslint.configs.strictTypeChecked,
  ...tseslint.configs.stylisticTypeChecked,
  unicorn.configs.recommended,
]
  .flatMap((config) => Object.keys(config.rules ?? {}))
  .filter((name) => !baseline.has(name));

test('no rule introduced by the strict/unicorn sets is an error', async () => {
  const config = (await new ESLint().calculateConfigForFile('src/main.tsx')) as Linter.Config;

  const errors = introducedRules.filter((name) => {
    const entry = config.rules?.[name];
    const severity = Array.isArray(entry) ? entry[0] : entry;
    return severity === 2 || severity === 'error';
  });

  expect(errors).toEqual([]);
});
