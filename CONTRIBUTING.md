# Contributing

## Formatting

Code layout is machine-enforced. CI and `task test` fail on any drift, so format before you
push.

```bash
task format
cd frontend && pnpm format
```

**Frontend — Prettier.** `printWidth 100`, `semi: true`, `singleQuote: true`,
`trailingComma: "all"` (`.prettierrc.json`). `eslint-config-prettier` is wired into the
flat config so ESLint's stylistic rules don't fight Prettier — ESLint keeps only its logic
and slice-boundary rules. `prettier-plugin-organize-imports` sorts imports and drops unused
ones. `pnpm format:check` runs in `task test` and as a step in the frontend CI job.

### IntelliJ

Development here is IntelliJ-only, so there is no committed IDE config — set it up once:

- **Prettier**: Settings → Languages & Frameworks → JavaScript → Prettier → *Automatic
  Prettier configuration*, and enable *Run on save*. The bundled plugin picks up
  `frontend/.prettierrc.json`.
- **ESLint fix on save**: Settings → Languages & Frameworks → JavaScript → Code Quality
  Tools → ESLint → *Automatic ESLint configuration*, and enable *Run eslint --fix on save*.

## Blame across the formatting cutover

Each one-time mechanical reformat is a single commit listed in
[`.git-blame-ignore-revs`](./.git-blame-ignore-revs). Point git at it once so `git blame`
and IDE annotations skip those commits:

```bash
git config blame.ignoreRevsFile .git-blame-ignore-revs
```

GitHub applies the file automatically.
