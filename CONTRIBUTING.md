# Contributing

## Local demo data

`task up` starts with an empty database. Once the stack is up, `task seed` populates it
with a fixed demo graph so you can sign in and land on a full app:

- Five personas — `alice`, `bob`, `carol`, `dave`, `erin` — with four posts each, a
  follow graph (`alice` follows and is followed by everyone), and likes and comments.
- **Sign in as `alice`** (type `alice` on the mock-Google form) for a non-empty feed
  *and* a non-empty profile grid.

A second run against a fully-seeded stack does nothing. `task clean` wipes the database if
you want to start over. `PICTOGRAM_BASE_URL` / `PICTOGRAM_OAUTH_URL` override the default
`task up` topology (`PICTOGRAM_OAUTH_URL` is used for its origin — the issuer's host and port).

## Tests pin behaviour

All work here is test-first (ADR-0007). A test earns its place by pinning a *behaviour* —
something that would break if the production code regressed. Before adding one, check it
against these:

- **Assert the state change or the response shape, not just the status.** `status().isOk()`
  plus one `jsonPath` existence check does not pin much. `PostApiTest` is the bar: after a
  delete it re-reads the grid and asserts the post is gone. A write test should read the
  thing back; a response-body test should assert the fields the client depends on.
- **Assert the negative half.** A happy-path scenario is half a test. Pair it with the
  other user *not* seeing the resource, the second call being a no-op, the forbidden
  actor's target being *unchanged* after the 403.
- **Paging call sites get both boundaries.** For every keyset/cursor read, pin *exactly
  `pageSize` rows ⇒ no `nextCursor`* and *`pageSize + 1` rows ⇒ full page + a cursor*, not
  just one mid-range case.
- **No assertion that restates the code.** If the assertion rebuilds the production
  expression (same formula, same branch conditions), it passes by construction and pins
  nothing. Assert against an independently-known expected value instead.
- **A comment in a test is a constraint, not narration** (`CLAUDE.md`): it names the
  ordering/timing rule the test exists to hold, or it goes.

## Formatting

Code layout is machine-enforced. CI and `task test` fail on any drift, so run `task format`
before you push.

**Frontend — Prettier.** `printWidth 100`, `semi: true`, `singleQuote: true`,
`trailingComma: "all"` (`.prettierrc.json`). `eslint-config-prettier` is wired into the
flat config so ESLint's stylistic rules don't fight Prettier — ESLint keeps only its logic
and slice-boundary rules. `prettier-plugin-organize-imports` sorts imports and drops unused
ones. `pnpm format:check` runs in `task test` and as a step in the frontend CI job.

**Backend — Spotless + palantir-java-format.** Configured in the
`pictogram.code-quality-conventions` convention plugin (`backend/buildSrc`) and applied to
every module; `spotlessCheck` runs as part of `check`, so `./gradlew build` and the PR gate
pick it up with no extra step. palantir-java-format's fixed 4-space indent matches
`.editorconfig`. The `*.gradle.kts` build scripts are formatted too, with ktfmt.

### IntelliJ

Development here is IntelliJ-only, so there is no committed IDE config — set it up once:

- **palantir-java-format**: install the *palantir-java-format* plugin (Settings → Plugins →
  Marketplace), then enable it under Settings → palantir-java-format. It replaces the
  built-in formatter for Java.
- **Prettier**: Settings → Languages & Frameworks → JavaScript → Prettier → *Automatic
  Prettier configuration*, and enable *Run on save*. The bundled plugin picks up
  `frontend/.prettierrc.json`.
- **Format / fix on save**: Settings → Tools → Actions on Save → enable *Reformat code* and
  *Optimize imports*; under JavaScript → Code Quality Tools → ESLint enable *Run eslint
  --fix on save*. This mirrors `spotlessApply` / `pnpm format`.

## Linting

**Frontend — ESLint.** `pnpm lint` (a frontend CI step, and part of `task test`). Beyond the
slice-boundary rules, the flat config runs type-aware `typescript-eslint`
(`strict-type-checked` + `stylistic-type-checked`, via `projectService`) and a curated
`eslint-plugin-unicorn` `recommended`. Every rule those sets add on top of
`typescript-eslint`'s `recommended` is forced to `warn`, so `pnpm lint` exits 0 with
warnings and CI stays green. [#75](https://github.com/ShyME/pictogram/issues/75) tracks
clearing the backlog and promoting them to `error`; don't add new warnings in the meantime
(`pnpm lint --fix` clears most). The `recommended` rules and the boundary rules are `error`
and do gate.

## Design system

The frontend UI is built on semantic design tokens plus a small set of in-tree components
(ADR-0013), light theme only.

- **Tokens** are a Tailwind v4 `@theme` block in `frontend/src/index.css` —
  `--color-surface`, `--color-foreground[-muted|-subtle]`, `--color-accent`, `--color-danger`,
  radii (`--radius-control|card|dialog`), shadows (`--shadow-card|popover|dialog`), and the
  self-hosted Inter stack (`@fontsource`). Components use the semantic utilities
  (`bg-surface`, `text-foreground-muted`, `rounded-card`, `shadow-popover`) and **never** a
  raw `neutral-*` / `indigo-*` palette step, so a dark theme stays a later additive block.
- **Primitives** live in `frontend/src/shared/ui/` (Button, Input, Textarea, Card, Avatar,
  DropdownMenu, Toast, Spinner, EmptyState, AppNav) on Radix + `lucide-react`, re-exported
  from `@shared`. `Modal` is the exception — hand-rolled, not Radix: Radix's `Dialog` locks
  scroll via an injected `<style>` that the CSP (`style-src 'self'`, ADR-0011) blocks. `/ui`
  renders them all — dev/test only, never linked, stripped from the production build.

### Adding a shadcn/ui component

The set grows on demand. `components.json` is configured:

```bash
cd frontend && pnpm dlx shadcn@latest add <component>   # e.g. tooltip, popover, tabs
```

That drops source into `src/shared/ui/`. Then:

1. Rename the file to camelCase (`dropdown-menu.tsx` → `dropdownMenu.tsx`) — the
   `unicorn/filename-case` rule rejects kebab-case.
2. Swap its palette classes for token utilities (`bg-background` → `bg-surface`,
   `text-muted-foreground` → `text-foreground-muted`, …); imports become relative
   (`../lib/cn`, `./sibling`).
3. Re-export it from `src/shared/ui/index.ts`, add it to `src/app/UiShowcase.tsx`, and add
   a unit test under `test/shared/ui/`.

### Visual regression

`pnpm test:visual` (in `frontend/`) runs Playwright `toHaveScreenshot` at ~375 and ~1440 px
against a bare `vite` dev server (no backend). It covers the `/ui` showcase, `/login`, and
every retrofitted screen (`visual/screens.visual.ts`) — the screen tests drive the real
router with each `/api/**` read fulfilled from the fixed world in `visual/appWorld.ts`
(clock pinned so relative timestamps don't drift). Baselines are committed under
`frontend/visual/__screenshots__/**/*-linux.png` and are generated in the pinned Playwright
CI image (`ci.yml` → `visual` job) so font/AA rendering is stable; `retries: 0` holds
(ADR-0007). A local run writes gitignored `*-darwin` snapshots.

The same config also runs `visual/overflow.visual.ts` — an assertion suite (no baselines)
that fails if any screen overflows its viewport horizontally at either width, driven by
`stubStress` (worst-case usernames, display names, tokens and URLs). See ADR-0013
"Amendment (#139)".

After an intentional UI change, regenerate the `*-linux.png` baselines in the same image and
commit them:

```bash
docker run --rm -v "$PWD/frontend:/work" -v /work/node_modules -w /work --ipc=host \
  -e PNPM_STORE_DIR=/tmp/pnpm-store \
  mcr.microsoft.com/playwright:v1.62.1-noble \
  bash -lc 'corepack enable && pnpm install --frozen-lockfile && pnpm test:visual:update'
```

(Or take the PNGs from the `visual` job's `visual-regression` artifact.)

## Blame across the formatting cutover

Each one-time mechanical reformat is a single commit listed in
[`.git-blame-ignore-revs`](./.git-blame-ignore-revs). Point git at it once so `git blame`
and IDE annotations skip those commits:

```bash
git config blame.ignoreRevsFile .git-blame-ignore-revs
```

GitHub applies the file automatically.
