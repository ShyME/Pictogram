# Contributing

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

## Blame across the formatting cutover

Each one-time mechanical reformat is a single commit listed in
[`.git-blame-ignore-revs`](./.git-blame-ignore-revs). Point git at it once so `git blame`
and IDE annotations skip those commits:

```bash
git config blame.ignoreRevsFile .git-blame-ignore-revs
```

GitHub applies the file automatically.
