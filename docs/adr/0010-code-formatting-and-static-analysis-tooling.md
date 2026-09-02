# Code formatting and static analysis: autoformat both stacks, gate formatting, keep hint-linters advisory

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
