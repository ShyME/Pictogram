# Code formatting and static analysis: autoformat both stacks, gate formatting, keep hint-linters advisory

- **Status:** Accepted

Both the Java backend and the TypeScript frontend get an autoformatter and a
modernisation linter. Formatting is enforced — CI and the local check command both fail on
drift. The hint-linters (the "you could use `getLast()` here" class) start **advisory** and
graduate to gating per-tool once their existing backlog is cleared.

## Backend

**Formatter: Spotless + palantir-java-format.** Spotless is the Gradle wrapper; the
underlying formatter is palantir-java-format, chosen over google-java-format because it uses
a 4-space indent — matching the existing `.editorconfig` — and handles current Java syntax
well, so the one-time reformat is the smallest possible diff. Configuration lives in a new
`pictogram.code-quality-conventions` convention plugin in `buildSrc`, applied to every
module. Spotless also formats the `buildSrc` `*.kts` files with ktfmt. Steps beyond the base
formatter: remove unused imports, order imports, format annotations, trim trailing
whitespace, end with newline.

**Modernisation linter: gradle-modernizer-plugin.** Bytecode-based, flags use of outdated
APIs where a JDK equivalent exists. Runs as part of `check`, target Java 25, including test
classes. Gating (`failOnViolations = true`) from adoption: the backlog was a single
`Optional.get()` in `post`, fixed in the adopting change, so there was nothing to clear over
follow-ups.

The `getLast()`-style hints that motivated this are IntelliJ *inspections*; no plain linter
reproduces them. **OpenRewrite** with a curated recipe list would (its `SequencedCollection`
recipe performs exactly that rewrite), and was considered and rejected: it runs its own
full parse/type-attribution pass — effectively a second compile — and needs a maintained
recipe list. On a 10k-line solo repo where the IDE already surfaces those hints on every
file open, that machinery outweighs the payoff. OpenRewrite earns its keep on large
migrations (framework or Java version bumps); revisit it when there is one. Modernizer is
the lightweight stand-in until then — narrower (it will not do the `getLast()` transform,
since that is a *new* API, not a deprecated one), but near-zero build cost.

## Frontend

**Formatter: Prettier** + `eslint-config-prettier` (disables the ESLint stylistic rules that
would fight it) + `prettier-plugin-organize-imports` (sorts imports, drops unused). Biome was
considered — one fast tool for format and lint — but it cannot enforce the
`eslint-plugin-boundaries` slice rules the repo already depends on, so ESLint stays and the
"one tool" benefit evaporates. Config: `printWidth 100`, `semi: true`, `singleQuote: true`,
`trailingComma: "all"`. Exposed as `pnpm format` / `pnpm format:check`, the latter a CI step.

**Modernisation linter: stricter typescript-eslint + curated unicorn.** Move
typescript-eslint from `recommended` to `strict-type-checked` + `stylistic-type-checked`
(requires enabling type-aware linting via `projectService`), and add `eslint-plugin-unicorn`
`recommended` minus `prevent-abbreviations`, `no-null`, `no-array-reduce`, and
`no-nested-ternary`. New rules land at `warn` so CI stays green; promote to `error` once the
backlog is cleared.

**Status (#68, #75): done — every rule is `error`.** #68 landed the sets advisory; #75
cleared the ~230-warning backlog and dropped the `advisory()` wrapper. Backlog fixes were
real code changes, not blanket disables. Five rules keep a non-default config, each because
the codebase already had a deliberate counter-convention:

- `unicorn/filename-case`: `{ cases: { camelCase, pascalCase } }`. Component files stay
  `PascalCase.tsx` (named after their component); every other file is `camelCase`. The
  kebab-case modules were renamed in one pure-rename commit (recorded in
  `.git-blame-ignore-revs`).
- `@typescript-eslint/consistent-type-definitions`: `'type'` — the domain is modelled with
  `type` unions, not interfaces.
- `@typescript-eslint/restrict-template-expressions`: `{ allowNumber: true }` — `` `…:
  ${response.status}` `` in error messages is idiomatic here.
- `unicorn/switch-case-braces`: `'avoid'` — the domain switches are terse `case x: return y`.
- `@typescript-eslint/only-throw-error`: allow `Response` — React Router loaders redirect by
  throwing the `redirect()` response.

**Dependency refresh: ESLint 9 → 10, unicorn 65 → 74, boundaries 5 → 7,
react-hooks 5 → 7.** typescript-eslint stays on 8.x and TypeScript on 5.9 — type-aware
linting does not yet support TypeScript 7. `eslint-plugin-boundaries` 7 rewrote its config
shape: `element-types` → `dependencies`, `no-unknown` → `no-unknown-dependencies`, string
selectors → `{ element: { type } }` objects, `${from.feature}` → the Handlebars
`{{ from.element.captured.feature }}`. The `mode: "file" | "folder"` element option is
deprecated in 7 but still works; migrating `testkit`/`entrypoint` off it means moving them
to `boundaries/files` categories, which ripples through every policy that lists `testkit`
in a `types` array — left as a follow-up.

unicorn 65 → 74 added ~15 rules to `recommended`. Two are adopted (`consistent-boolean-name`,
`prefer-minimal-ternary` — the ~20 hits were fixed). Five are disabled for the same reason
as the pre-existing four above — the codebase already had a deliberate counter-convention:

- `unicorn/name-replacements`: `prevent-abbreviations` by another name (`ref` →
  `reference`, `searchParams` → `searchParameters`).
- `unicorn/consistent-class-member-order`: the e2e page objects list public locators before
  private plumbing on purpose.
- `unicorn/no-top-level-assignment-in-function`: module-scoped mutable state set from a
  function is deliberate — in-flight request guards (`authApi`), the auth-middleware install
  latch (`authClient`), and per-test `loaderData` fixtures.
- `unicorn/prefer-await`: `.finally()`/`.catch()` chaining is used where `await` would break
  the shape (the `inFlight ??= …` singleton).
- `unicorn/no-top-level-side-effects`: `routes.tsx` calls `installApiAuth()` at module load
  — the composition root.

Two files are excluded from linting: the generated `src/shared/api/schema.d.ts`
(regenerated by `pnpm generate:api`; `schema.contract.test.ts` guards it), and Vite's
scaffolded `src/vite-env.d.ts`, which keeps its kebab-case name so `npm create vite` and
the Vite docs stay in sync.

## Enforcement and rollout

- **Wired into the standard command**, not a separate one: Gradle `check` (backend `build`
  already depends on it) and `pnpm` scripts. No new CI job — one added step in the frontend
  job for `pnpm format:check`; the backend needs no workflow change.
- **No git hook.** Q'd and rejected — hooks are bypassable and noisy on a solo repo, and the
  IDE formats on save. `Taskfile.yml` gets a `task format` target (`spotlessApply` +
  `pnpm format`) as the manual convenience.
- **One mechanical reformat commit per stack**, each SHA recorded in a new
  `.git-blame-ignore-revs` (`git config blame.ignoreRevsFile`; GitHub reads it
  automatically). Blame stays useful across the cutover.
- **Shipped as four independent pieces of work** — backend formatter, frontend formatter,
  backend linter, frontend linter — in that rough order (formatters first: pure mechanical,
  low risk, they seed the blame-ignore file).
- **IDE**: development is IntelliJ-only here, so there is no committed editor config. The
  palantir-java-format IntelliJ plugin plus format-on-save gives backend parity with CI;
  IntelliJ's bundled Prettier and ESLint-fix-on-save cover the frontend. Documented in
  CONTRIBUTING.

## Consequences

- The first reformat PR on each stack touches almost every file (~200 Java files / ~10k
  lines on the backend). Reviewers skip the diff; the blame-ignore entry keeps history
  navigable.
- Enabling `projectService` for typescript-eslint makes frontend linting type-aware, so
  `pnpm lint` gets slower. Accepted for the stronger rule set.
- Two concurrent branches can both reformat and collide, but the collision is a normal merge
  conflict in formatted code, not a silent runtime footgun.
- Promoting either linter from advisory to gating is a follow-up decision, made once its
  backlog count is known — small backlog: clear it in the adopting PR and gate immediately;
  large: clear over follow-ups, then flip. Modernizer's backlog was one violation, so it
  gates from adoption.
