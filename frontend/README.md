# Pictogram frontend

React 19 + TypeScript SPA, built with Vite. React Router, TanStack Query and
Tailwind v4 are wired in; there are no features yet — just the app shell and one
placeholder route (`/`).

## Commands

```bash
pnpm install          # Node >= 24 (see .nvmrc); pnpm via corepack
pnpm dev              # dev server
pnpm build            # tsc -b && vite build
pnpm lint             # eslint, incl. slice-boundary enforcement
pnpm test             # vitest run
pnpm typecheck        # tsc -b --noEmit
```

## Structure — feature slices

```
src/
  app/               composition root: providers, router, route table
  shared/            cross-cutting building blocks (may import shared only)
  features/<name>/   one vertical slice:
    api/             calls to backend REST resources
    components/      slice-local components
    model/           types and pure logic
    routes/          route components
    index.ts         the slice's public surface
```

Import rules are enforced by `eslint-plugin-boundaries` (`pnpm lint`):

- `app` may import any slice and `shared`.
- A `feature` may import `shared` and its own slice only. Importing another
  feature is a lint error — cross-slice code belongs in `shared` or is composed
  in `app`.
- `shared` may import `shared` only.

Modules are split for possible later extraction, mirroring the backend context
map (`../CONTEXT-MAP.md`).
