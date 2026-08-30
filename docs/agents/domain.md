# Domain Docs

How the engineering skills should consume this repo's domain documentation when exploring the codebase.

## Before exploring, read these

Pictogram is a **multi-context** repo.

- **`CONTEXT-MAP.md`** at the repo root: the list of bounded contexts and how they relate. Start here.
- **`backend/<module>/CONTEXT.md`**: the glossary for one bounded context. Read each one relevant to the topic, and use its vocabulary.
- **`docs/adr/`**: system-wide architecture decisions. Read the ADRs that touch the area you're about to work in.

If any of these files don't exist yet, **proceed silently**. The `/domain-modeling` skill (reached via `/grill-with-docs` and `/improve-codebase-architecture`) creates them lazily when terms or decisions actually get resolved.

## File structure

```
/
├── CONTEXT-MAP.md                     ← the context list + relationships
├── docs/adr/                          ← system-wide decisions (0001…)
└── backend/
    ├── identity/CONTEXT.md
    ├── profile/CONTEXT.md
    ├── media/CONTEXT.md
    ├── post/CONTEXT.md
    ├── follow/CONTEXT.md
    ├── feed/CONTEXT.md
    └── engagement/CONTEXT.md
```

There are no context-scoped `docs/adr/` directories yet; add `backend/<module>/docs/adr/` only if a decision is genuinely local to one context.

## Use the glossary's vocabulary

When your output names a domain concept (in an issue title, a refactor proposal, a hypothesis, a test name), use the term as defined in that context's `CONTEXT.md`. Don't drift to synonyms the glossary explicitly avoids.

If the concept you need isn't in the glossary yet, that's a signal: either you're inventing language the project doesn't use (reconsider) or there's a real gap (note it for `/domain-modeling`).

## Flag ADR conflicts

If your output contradicts an existing ADR, surface it explicitly rather than silently overriding:

> _Contradicts ADR-0007 (event-sourced orders), but worth reopening because…_
