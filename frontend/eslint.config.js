import js from '@eslint/js';
import prettier from 'eslint-config-prettier/flat';
import boundaries from 'eslint-plugin-boundaries';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import unicorn from 'eslint-plugin-unicorn';
import globals from 'globals';
import tseslint from 'typescript-eslint';

// ADR-0010 / #68: the type-aware strict/stylistic sets and unicorn's recommended ship
// advisory — rules they add on top of `recommended` are forced to `warn` so CI stays
// green; #75 promotes them to `error`. Rules already in `recommended` keep their severity.
const recommendedRules = new Set(
  tseslint.configs.recommended.flatMap((config) => Object.keys(config.rules ?? {})),
);

/** @param {import('eslint').Linter.RuleEntry} level */
const toWarn = (level) => {
  const [severity, ...options] = Array.isArray(level) ? level : [level];
  const warned = severity === 'error' || severity === 2 ? 'warn' : severity;
  return options.length > 0 ? [warned, ...options] : warned;
};

/**
 * @param {import('typescript-eslint').ConfigArray} configs
 * @returns {import('typescript-eslint').ConfigArray}
 */
const advisory = (configs) =>
  configs.map((config) => ({
    ...config,
    rules: Object.fromEntries(
      Object.entries(config.rules ?? {})
        .filter(([name]) => !recommendedRules.has(name))
        .map(([name, level]) => [name, toWarn(level)]),
    ),
  }));

export default tseslint.config(
  { ignores: ['dist', 'coverage', 'node_modules', 'playwright-report', 'test-results'] },
  {
    extends: [
      js.configs.recommended,
      ...tseslint.configs.recommended,
      ...advisory(tseslint.configs.strictTypeChecked),
      ...advisory(tseslint.configs.stylisticTypeChecked),
      ...advisory([unicorn.configs.recommended]),
    ],
    files: ['**/*.{ts,tsx}'],
    languageOptions: {
      ecmaVersion: 2022,
      globals: globals.browser,
      parserOptions: {
        projectService: true,
        tsconfigRootDir: import.meta.dirname,
      },
    },
    plugins: {
      'react-hooks': reactHooks,
      'react-refresh': reactRefresh,
      boundaries,
    },
    settings: {
      'boundaries/include': ['src/**/*.{ts,tsx}', 'test/**/*.{ts,tsx}'],
      'boundaries/ignore': ['src/**/*.d.ts'],
      'boundaries/elements': [
        { type: 'app', pattern: ['src/app', 'test/app'], mode: 'folder' },
        { type: 'shared', pattern: ['src/shared', 'test/shared'], mode: 'folder' },
        {
          type: 'feature',
          pattern: ['src/features/*', 'test/features/*'],
          mode: 'folder',
          capture: ['feature'],
        },
        {
          type: 'testkit',
          pattern: ['test/support/*', 'test/*.test.{ts,tsx}'],
          mode: 'file',
        },
        { type: 'entrypoint', pattern: 'src/main.tsx', mode: 'file' },
      ],
      'import/resolver': {
        typescript: { project: ['./tsconfig.app.json', './tsconfig.test.json'] },
      },
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      'react-refresh/only-export-components': ['warn', { allowConstantExport: true }],
      'boundaries/no-unknown': 'error',
      'boundaries/no-unknown-files': 'error',
      'boundaries/element-types': [
        'error',
        {
          default: 'disallow',
          rules: [
            { from: 'testkit', allow: ['app', 'shared', 'feature', 'testkit'] },
            { from: 'entrypoint', allow: ['app', 'shared'] },
            { from: 'app', allow: ['app', 'shared', 'feature', 'testkit'] },
            {
              from: 'feature',
              allow: ['shared', 'testkit', ['feature', { feature: '${from.feature}' }]],
            },
            { from: 'shared', allow: ['shared'] },
          ],
        },
      ],
      // Dropped from unicorn's `recommended` per ADR-0010: abbreviations read fine in this
      // codebase, `null` is deliberate at the API/JSON edge, `reduce` and nested ternaries
      // are allowed style.
      'unicorn/prevent-abbreviations': 'off',
      'unicorn/no-null': 'off',
      'unicorn/no-array-reduce': 'off',
      'no-nested-ternary': 'off',
    },
  },
  {
    files: ['**/*.test.{ts,tsx}', 'test/support/**'],
    languageOptions: { globals: { ...globals.browser, ...globals.node } },
  },
  {
    files: ['vite.config.ts'],
    languageOptions: { globals: globals.node },
    settings: { 'boundaries/include': [] },
  },
  {
    files: ['e2e/**/*.ts', 'playwright.config.ts'],
    languageOptions: { globals: globals.node },
    settings: { 'boundaries/include': [] },
  },
  prettier,
);
