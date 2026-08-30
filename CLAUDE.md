# Pictogram

## Agent skills

### Issue tracker

Issues and specs are tracked as GitHub issues, using the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

Default canonical triage vocabulary (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`). See `docs/agents/triage-labels.md`.

### Domain docs

Multi-context: `CONTEXT-MAP.md` at the repo root links one `CONTEXT.md` per bounded context under `backend/<module>/`; system-wide ADRs in `docs/adr/`. See `docs/agents/domain.md`.

## Project

Pictogram is an Instagram-like portfolio app built to practise **DDD** and **TDD**: a
modular monolith (React 19 + Spring Boot 4, Java 25) with module boundaries drawn for later
service extraction. All work is test-first. Read `CONTEXT-MAP.md` and `docs/adr/` before
touching module boundaries, integration style, auth, or the feed.
