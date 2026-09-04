# Frontend design system: shadcn/ui components on Tailwind v4 tokens, one theme, screenshot-tested

- **Status:** Accepted
- **Relates to:** ADR-0005 (client-side composition — the SPA owns all presentation),
  ADR-0007 (`retries: 0`, the Playwright layer), ADR-0011 (CSP on the app chains)

The frontend is styled with ad-hoc Tailwind utility classes chosen per component:
`src/index.css` is `@import 'tailwindcss';` and nothing else — no tokens, no shared
components. `PostDetailDialog` hand-rolls a `role="dialog"`; `HeartGlyph` hand-draws an
SVG. The next phase adds comments and a responsive pass and wants a consistent, deliberate
look. This records the approach so every feature ticket after it inherits the same
foundation.

## Decisions

1. **Design tokens in `src/index.css` via Tailwind v4 `@theme`.** Semantic names —
   `--color-surface`, `--color-text`, `--color-text-muted`, `--color-accent`,
   `--color-border`, a radii set, a shadow set, one type scale. Components reference tokens,
   never raw palette values. **One theme (light) for v1.** Because every colour is a
   semantic token, a dark theme is a later second `@theme` block, not a sweep of every
   component.

2. **Components: shadcn/ui.** Not an npm dependency — its CLI copies component source (built
   on Radix primitives + Tailwind) into `src/shared/ui/`, which the repo then owns, styles,
   and unit-tests. This resolves the tension between "don't hand-roll accessible dialogs,
   menus and toasts" and "keep runtime UI dependencies at zero and keep the craft story":
   the component code is in-tree and testable, but the parts that are subtly wrong when
   hand-rolled — focus traps, menu keyboard semantics, toast live-regions — come from Radix.
   `PostDetailDialog` re-homes onto the shadcn `Dialog`. shadcn's `cn()` helper and
   `components.json` land in the frontend.

3. **Icons: `lucide-react`** (shadcn's default). A build-time dependency, tree-shaken to the
   icons actually used. `HeartGlyph` and any other bespoke SVGs are replaced.

4. **Typography: one self-hosted webfont.** Inter (Geist is an acceptable alternative),
   `system-ui` fallback stack, 400 and 600 weights, `font-display: swap`, vendored via
   `@fontsource` so it serves from the app's own origin. CSP stays `'self'` — ADR-0011 is
   untouched. (If the font is ever loaded from `fonts.googleapis.com` instead, that ADR's
   `style-src`/`font-src` gain the Google font hosts, and its decision 3 wording is
   updated — but self-hosting is the default precisely to avoid that.)

5. **Aesthetic: content-first minimal.** Neutral chrome, the photo is the only saturated
   element on the page, a single accent colour for primary actions, generous whitespace.
   This is Instagram's own answer and it suits a photo portfolio. One `design`-skill canvas
   pass settles the palette, the type scale, the spacing rhythm, and one screen's look;
   after that, code is the source of truth. Full up-front mockups of every screen are
   overkill for five screens.

6. **Navigation: one `<AppNav>` in `src/shared/ui`.** Full inline nav at the `md` breakpoint
   and above; below it, the nav collapses to the logo plus an avatar `DropdownMenu`. This
   replaces the inline `<header>` in `AppLayout`. The nav gains a chat entry when that epic
   lands — it is not stubbed now.

7. **The component set is lean and grows on demand.** Initial set: Button, Input, Textarea,
   Dialog, DropdownMenu, Avatar, Card, Toast, Spinner, EmptyState. Anything else is added
   with a one-line CLI call when a feature needs it. There is no penalty for deferring, so
   nothing speculative is built.

8. **Visual-regression suite.** A `/ui` showcase route renders every `shared/ui` component
   in its states. Playwright `toHaveScreenshot` snapshots cover that route plus the key
   screens (feed, profile, post detail, login) at a narrow and a wide viewport. "Consistent"
   becomes an enforced property rather than a hope. `retries: 0` still holds (ADR-0007);
   baselines are committed and updated deliberately, and — like the existing e2e — the
   snapshots are generated in the Playwright CI image so font rendering is stable.

9. **One retrofit pass.** After the tokens and the component set land, a single change
   migrates the existing screens (Login, Onboarding, EditProfile, NewPost, Profile, Feed,
   FollowList) onto tokens + `shared/ui`. Done means: no ad-hoc colour or spacing literals
   left, and the screen matches its committed screenshot baseline.

## Why not

- **Hand-roll everything.** Considered — it is the project's minimal-dependency instinct,
  and `PostDetailDialog` proves it is feasible. Rejected because focus management, `Menu`
  keyboard handling and toast accessibility are exactly what goes subtly wrong hand-rolled,
  and shadcn keeps the code in-tree anyway, so "own the code" is not given up.
- **A full styled kit (MUI, Mantine, Chakra).** Each ships its own styling system that
  fights Tailwind, adds real runtime weight, and pushes a Material/opinionated look. The
  repo is already committed to Tailwind v4.
- **Radix Primitives directly, without shadcn.** Viable. shadcn is Radix plus a sensible
  default style and a catalogue to copy from; it is the fallback if the CLI workflow proves
  annoying.
- **Dark mode now.** Doubles the review and screenshot surface of every screen and the
  retrofit before one theme is polished. Deferred — but the semantic tokens mean it is not
  designed out.

## Consequences

- `eslint-plugin-boundaries`: `shared/ui` is part of `@shared`; features import components
  from there, never from each other.
- Every subsequent frontend ticket — comments first — builds on `shared/ui` and adds its
  screens to the screenshot suite. The cross-layer slice rule (ADR-0007) now includes a
  visual snapshot wherever a slice has UI.
- The `/ui` showcase route is test/dev-only or served behind the same build; it is not a
  user-facing page.
- `pnpm` gains `lucide-react` and `@fontsource/*` as dependencies and the shadcn CLI as a
  dev tool. This is the first time the frontend takes on component/icon dependencies; the
  bar was "in-tree, testable, tree-shaken", which shadcn + lucide meet.

## Amendment (#134)

The canvas pass (`/design`) settled the palette, type scale and spacing; #134 built it out.
Deviations from the decisions above, all deliberate:

- **Accent is a restrained indigo** (`oklch(0.52 0.16 264)`), not near-black — the user's
  call from the canvas: "some colour, not too outstanding". Primary actions, links and
  active nav use it; the focus ring stays blue regardless.
- **Token names follow the shadcn/Tailwind convention** (`--color-foreground`,
  `--color-foreground-muted`, `--color-surface`) rather than the ADR's prose spelling
  (`text`, `muted text`) — `text-text-muted` reads badly as a utility. The mapping is 1:1.
- **The type scale and spacing rhythm are Tailwind v4's defaults**, with only `--font-sans`
  (Inter) overridden. Inventing a parallel ramp bought nothing and widened the retrofit.
- **`/ui` is dev/test-only** — mounted behind `import.meta.env.DEV`, so the route and its
  code fold out of the production bundle entirely (the ADR's stricter option).
- **Toast is `@radix-ui/react-toast`** (not `sonner`) — keeps the "own the source" story;
  the live-region behaviour is the Radix part.
- The visual suite runs against a bare `vite` dev server (no backend) as its own Playwright
  config + CI job in the pinned Playwright image; baselines are `*-linux.png`, committed,
  regenerated in that image.
- shadcn component files are renamed to camelCase on add (repo `unicorn/filename-case`).

## Amendment (#135)

`<AppNav>` built per decision 6. Deviations / notes:

- **The breakpoint is matched in JS** (`useSyncExternalStore` over `matchMedia('(min-width:
  48rem)')`), not CSS `hidden md:flex` / `md:hidden`. Only the active branch is ever in the
  DOM, so there is one tab stop and one copy of each action — no `aria-hidden` duplicates.
- The sign-out logic moved from a `SignOutButton` component to a `useSignOut` hook that the
  layouts drive through `AppNav` props.
- **`AppNav` is on every authenticated screen, not just the feed** (the ticket's "used on
  every page"). Two pathless layout routes in `routes.tsx`: `AppLayout` (`rootLoader`,
  redirects) wraps feed / new post / edit profile / follow lists and passes the viewer to
  `/new` + `/settings/profile` via `Outlet` context; `PublicLayout` (`viewerLoader`, never
  redirects) wraps the public profile page and renders `AppNav` in its logged-out state
  (logo + "Log in") for anonymous visitors. Each page's hand-written `<header>` chrome is
  removed. `/login` and `/onboarding` stay chrome-only (mid-auth-flow, no nav).
- The per-screen **visual retrofit** (tokens, Toast, screenshot baselines) remains #136;
  this only moves the nav.

## Amendment (#136)

The one retrofit pass (decision 9) landed. Notes:

- **Screens migrated:** onboarding, edit profile, new post, profile + post grid, feed +
  feed card, and the follower / following lists. All ad-hoc `neutral-*` / `red-*` literals
  are gone; the screens use the semantic token utilities and `shared/ui` primitives
  (`Button`, `Input`, `Textarea`, `Card`, `Avatar`, `Spinner`, `EmptyState`).
  Post detail is deliberately untouched — it is rebuilt in the comments work (#137).
- **Mutation feedback is a `Toast`.** Create-profile, edit-profile and publish-post surface
  success and infrastructure failure through the app-level `Toaster` (`toast(...)` from
  `@shared`); delete-post toasts on success. Field-level and in-context messages (username
  taken, caption too long, a failed delete while its confirm dialog is still open) stay
  inline next to their control — they are answers about the thing in front of you, not
  transient status.
- **The delete confirmation stays a hand-rolled modal** (now on tokens + `Button`), not the
  shared Radix `Dialog`: that dialog's scroll-lock writes an inline `style` on `<body>`,
  which the app CSP (`style-src 'self'`, ADR-0011) blocks. `PostDetailDialog` is hand-rolled
  for the same reason; #137 owns making a Radix dialog work under the CSP and unifying them.
- **The username-rename caution** dropped its amber colour: there is no warning token in the
  one-theme palette, so it renders as `text-foreground-muted` with `role="status"`.
- **Visual regression** now drives the real router against a stubbed backend
  (`visual/appWorld.ts` fulfils every `/api/**` read from a fixed world, clock pinned so the
  relative timestamps are stable). `visual/screens.visual.ts` commits a `*-linux.png`
  baseline per screen at both viewports.

## Amendment (#137)

The comments work rebuilt the post-detail view and forced the dialog question the two
earlier tickets deferred.

- **The Radix `Dialog` primitive is gone; `shared/ui/modal.tsx` replaces it.** Radix
  `Dialog.Content` wraps its children in `react-remove-scroll`, which injects a `<style>`
  element to lock body scroll — blocked by the app CSP `style-src 'self'` (ADR-0011), with
  no clean fix for a statically-served SPA (decision 3 already ruled out a nonce/hash
  pipeline). `Modal` is a hand-rolled, shadcn-shaped compound (`Modal`, `ModalContent`,
  `ModalHeader`, `ModalTitle`, `ModalDescription`, `ModalFooter`) with a focus trap, an
  Escape / backdrop close, and a scroll lock set through the CSSOM property API
  (`document.body.style.overflow = …`) — the same `style-src` carve-out the crop frame
  uses. `PostDetailDialog` and the delete-confirmation (`PostGrid`) are both on it now, so
  the "two hand-rolled modals" note from #136 is resolved. `e2e/comments.spec.ts` opens the
  modal under the real CSP; the `cspViolations` fixture fails the test on any violation.
- **The post-detail view is the shared `Modal`** hosting the image, the injected like
  control, the caption, and the comment thread. The feed card gains an "Open comments"
  affordance; the profile grid thumbnail already opened the detail. Both are wired from
  `routes.tsx` (a feature may not import another feature's component), which passes
  `renderComments` / `renderPostDetail` render props.
- **`relativeTime` moved from `features/feed` to `shared/lib`** so the comment row and the
  feed card can share it without a cross-feature import.
- The `/ui` showcase's `Dialog` section became a `Modal` section; the `dialog-*` visual
  baselines were replaced by `modal-*`, and a `post-detail` screen baseline was added.

## Amendment (#139)

The responsive pass over the finished layouts — done last, once the screens were built and
retrofitted (#136) and the comment view rebuilt (#137). Notes:

- **`visual/overflow.visual.ts`** is an assertion suite, not a screenshot one: for every
  screen, at both the 375 px and 1440 px project, it fails if the document is wider than the
  viewport. The feed card and the post-detail modal are `overflow-hidden`, so text that
  fails to wrap is clipped without growing the document — those two get an extra check that
  the container's own `scrollWidth` doesn't exceed its box. It runs against `stubStress`
  (a `stubApp` overlay in `visual/appWorld.ts` that swaps every user-authored payload for
  worst-case content — a 20-char username, a long display name, an unbreakable token, a long
  bare URL). Encoding this as assertions means a regression fails a test rather than needing
  someone to spot it in a screenshot diff.
- **The hardening was `break-words` / `truncate` + `min-w-0` / `shrink-0`**, nothing
  structural: the feed-card and comment-row headers truncate the name and pin the timestamp;
  the profile heading/bio, the not-found lines, the follow-list heading and the post-detail
  caption wrap long tokens.
- **The post grid adapts its column count**: `grid-cols-3` on a phone, `sm:grid-cols-4`
  above. The feed column was already constrained (`max-w-xl`, centred) and the `Modal`
  already fits a small viewport (`p-4` overlay, `w-full max-w-*`), so those needed nothing.
- Only `profile-desktop-linux.png` moved (the fourth grid column); all other baselines were
  unchanged.
