import js from "@eslint/js";
import globals from "globals";
import tseslint from "typescript-eslint";
import reactHooks from "eslint-plugin-react-hooks";
import reactRefresh from "eslint-plugin-react-refresh";
import boundaries from "eslint-plugin-boundaries";

// `boundaries/element-types` below keeps feature slices isolated the way the
// backend context map isolates modules: one feature may not import another.
export default tseslint.config(
  { ignores: ["dist", "coverage", "node_modules", "playwright-report", "test-results"] },
  {
    extends: [js.configs.recommended, ...tseslint.configs.recommended],
    files: ["**/*.{ts,tsx}"],
    languageOptions: {
      ecmaVersion: 2022,
      globals: globals.browser,
    },
    plugins: {
      "react-hooks": reactHooks,
      "react-refresh": reactRefresh,
      boundaries,
    },
    settings: {
      // test/ mirrors src/ 1:1 (see test/structure.test.ts), so every element
      // below spans both roots and test files inherit the same boundary rules
      // as the production code they exercise.
      "boundaries/include": ["src/**/*.{ts,tsx}", "test/**/*.{ts,tsx}"],
      "boundaries/ignore": ["src/**/*.d.ts"],
      "boundaries/elements": [
        { type: "app", pattern: ["src/app", "test/app"], mode: "folder" },
        { type: "shared", pattern: ["src/shared", "test/shared"], mode: "folder" },
        {
          type: "feature",
          pattern: ["src/features/*", "test/features/*"],
          mode: "folder",
          capture: ["feature"],
        },
        {
          type: "testkit",
          pattern: ["test/support/*", "test/*.test.{ts,tsx}"],
          mode: "file",
        },
        { type: "entrypoint", pattern: "src/main.tsx", mode: "file" },
      ],
      "import/resolver": {
        typescript: { project: ["./tsconfig.app.json", "./tsconfig.test.json"] },
      },
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      "react-refresh/only-export-components": [
        "warn",
        { allowConstantExport: true },
      ],
      "boundaries/no-unknown": "error",
      "boundaries/no-unknown-files": "error",
      "boundaries/element-types": [
        "error",
        {
          default: "disallow",
          rules: [
            { from: "testkit", allow: ["app", "shared", "feature", "testkit"] },
            { from: "entrypoint", allow: ["app", "shared"] },
            { from: "app", allow: ["app", "shared", "feature", "testkit"] },
            {
              from: "feature",
              allow: [
                "shared",
                "testkit",
                ["feature", { feature: "${from.feature}" }],
              ],
            },
            { from: "shared", allow: ["shared"] },
          ],
        },
      ],
    },
  },
  {
    files: ["**/*.test.{ts,tsx}", "test/support/**"],
    languageOptions: { globals: { ...globals.browser, ...globals.node } },
  },
  {
    files: ["vite.config.ts"],
    languageOptions: { globals: globals.node },
    settings: { "boundaries/include": [] },
  },
  {
    // Playwright blackbox journeys run in Node against the deployed app, not a feature slice.
    files: ["e2e/**/*.ts", "playwright.config.ts"],
    languageOptions: { globals: globals.node },
    settings: { "boundaries/include": [] },
  },
);
