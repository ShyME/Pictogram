# No BFF: feed-card composition on the client

A feed card needs data from three contexts — the post and its image (`post`, `media`), the
author's username and display name (`profile`), and the like count plus "did I like it"
(`engagement`). We are **not** building a backend-for-frontend module to stitch this
together. Each module exposes its own REST resources, and the React app assembles the card
from **batched** calls (`GET /api/profiles?ids=…`, `GET /api/engagement/likes?postIds=…`),
cached across screens by TanStack Query.

This keeps modules from reaching across their boundaries to enrich each other's responses,
and avoids a composition layer that would couple to every screen. With fan-out-on-read the
`feed` query already spans `follow` and `post`, so the only extra cross-context work is
per-card enrichment, which the client is well placed to cache.

## Consequences

- Batch (`?ids=`) endpoints on `profile` and `engagement` are a **requirement**, not an
  optimisation — the client must not make N+1 calls.
- If a second screen ever needs the same heavy cross-context fan-in, a thin `bff` module is
  the escape hatch.
