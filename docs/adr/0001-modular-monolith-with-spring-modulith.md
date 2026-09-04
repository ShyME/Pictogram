# Modular monolith with Spring Modulith

- **Status:** Accepted
- **Relates to:** ADR-0007 (all work is test-first), ADR-0014 (`chat` is the one context
  that departs from this)

Pictogram is a portfolio project for practising DDD and TDD, and we want strong module
isolation now plus a credible path to microservices later without paying for distribution
today. We are building a **single Spring Boot deployable** with one Gradle subproject per
bounded context (`identity`, `profile`, `media`, `post`, `social`) plus an open
`shared-kernel` holding only ID value types. `social` covers the follow graph, the feed
and likes as three `internal/` sub-domains behind one module boundary (#128 merged what
were three finer-grained contexts). Boundaries are enforced by
**Spring Modulith** — a `verify()` test fails the build on any illegal cross-module
dependency. Persistence is one PostgreSQL instance with **a schema per module and no
foreign keys across schemas**, so a module's schema can move to its own database unchanged.

## Consequences

- Contexts reference each other's data **by ID only**; cross-module reads go through a
  module's published interface or a domain event, never a JOIN.
- Every context is designed to be split out later; that split is explicitly **not** work
  for v1.
- All design and implementation is test-first (see ADR-0007).
- **`chat` is the one exception.** It is not a Modulith module and never enters this
  deployable — it ships as a separate service from day one instead of being split out
  later (ADR-0014).
