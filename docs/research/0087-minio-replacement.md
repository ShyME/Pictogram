# MinIO replacement for local/test S3 — research note + decision

> Feeding [issue #87](https://github.com/ShyME/pictogram/issues/87). Date: 2026-09-02. **No application or infra code was changed** by this note.

## Decision (2026-09-02)

**Option 1 — keep pinning `minio/minio:RELEASE.2025-09-07T16-13-09Z` indefinitely.** No code change: the pin already exists in `SharedMinio.java` and the compose files, and MinIO here is a **dev + test-only** S3 stub with no attacker path (see §1). Issue #87 is **closed** — this note is the record. No ADR (pinning doesn't change the local-infra story).

**If the pin ever has to change** — the frozen image breaks, or a future ticket (#84 version-catalog/infra, or a media feature) opens the compose + `SharedMinio` files anyway — **migrate to SeaweedFS** and write an ADR at that point. Reasoning: Apache-2.0, large community, fast security response, no rug-pull risk, and static env-var credentials like MinIO so only the Testcontainers wait strategy changes (see §4b). `pgsty/silo` (the Pigsty MinIO fork) is the lower-effort fallback but is a <1-year-old, bus-factor-1 project. RustFS is a no (see §4c).

## TL;DR of the analysis

MinIO CE has no patch path (repo archived 2026-04-25, source-only since Oct 2025) and a growing advisory stream — but every advisory needs network + credential access an attacker doesn't have against a `localhost` / ephemeral-Testcontainers stub. So "security" here reduces to *scanner noise* and *does a patched artifact exist if we ever need one*, not *are we exploitable*. The real deciding axis is **cost of adoption**, and all migration cost is concentrated in `SharedMinio.java` (container def + wait strategy) — not worth paying now for a dependency that works.

---

## What the media module actually needs from S3

`backend/media/src/main/java/me/imshy/pictogram/media/internal/S3BlobStore.java` uses exactly five S3 operations via AWS SDK v2:

| Operation | Call site |
|---|---|
| `CreateBucket` | `ensureBucket()` — tolerates `BucketAlreadyOwnedByYou` / `BucketAlreadyExists` |
| `HeadBucket` | `bucketExists()` — treats 404/403 as "absent" |
| `PutObject` | `put()` — `contentType(image/jpeg)`, body from bytes |
| `GetObject` | `get()` — `getObjectAsBytes` |
| `DeleteObject` | `remove()` |

No multipart, no presigned URLs, no `ListObjectsV2`, no tagging/versioning/lifecycle/policies. The client (`MediaStorageConfiguration.java`) is built with `.endpointOverride(...)`, `Region.of(...)`, `StaticCredentialsProvider`, `.forcePathStyle(true)`, `UrlConnectionHttpClient`. **Any server that speaks path-style S3 for those five verbs is a drop-in** — `MediaStorageConfiguration.java`, `S3BlobStore.java`, `MediaStorageProperties.java` need **zero changes** for every option below.

## Touch points (what a swap actually costs in this repo)

| File | What references MinIO | Changes on a migration? |
|---|---|---|
| `compose.yaml` | `minio` service; `app` env `PICTOGRAM_MEDIA_ENDPOINT` / access+secret keys | Yes — image, command, ports, env-var names |
| `compose.dev.yaml` | `minio` service (host-based dev) | Yes — same |
| `compose.google.yaml` | **nothing** — OAuth-only overlay | **No** (issue #87 lists this file, but it has no MinIO reference) |
| `backend/test-support/.../SharedMinio.java` | Testcontainers `GenericContainer`, `MINIO_ROOT_*` env, `--command`, `Wait.forHttp("/minio/health/ready")` | Yes — image, env, command, **wait strategy**, maybe class rename |
| `.github/workflows/ci.yml` | 4 lines: `docker pull/save/load` of `minio.tar` | Yes — image name in 4 places |
| `.env.example` | `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` | Cosmetic rename |
| `backend/app/src/main/resources/application.yml` | comment only ("Defaults target a local MinIO") | Cosmetic |
| `.github/dependabot.yml` | `ignore: minio/minio` | Remove if we move off it (per issue AC) |
| `backend/README.md` | prose mention of `SharedMinio` | Cosmetic |

So a migration is **~5 functional files + 4 cosmetic**, and the only non-trivial engineering is the `SharedMinio` container definition + wait strategy.

---

## 1. MinIO CE status (primary sources)

- **Repo archived 2026-04-25, read-only.** The GitHub banner on <https://github.com/minio/minio> states: *"This repository was archived by the owner on Apr 25, 2026. It is now read-only."*
- **Source-only since Oct 2025.** Current README (<https://github.com/minio/minio>): *"The MinIO community edition is now distributed as source code only. We will no longer provide pre-compiled binary releases for the community version."* Users must `go install` or build their own container from the repo `Dockerfile`.
- **Maintainer statement**, Harshavardhana, 2025-10-26, <https://github.com/minio/minio/discussions/21667>: *"MinIO is a source-only distribution and is meant to remain that way for the near future."*
- **Maintenance-mode issue** opened 2025-12-04: <https://github.com/minio/minio/issues/21714> (I could not extract the maintainer reply text from the archived thread via fetch — the issue exists and is titled "Maintenance Mode"; treat the exact wording as unverified).
- **Console removed from CE.** The embedded admin console was cut back to a stub in the CE builds (community reports in discussion #21667; Pigsty's writeup dates the removal to "May 2025", <https://blog.vonng.com/en/db/minio-resurrect/> — the *exact* removal commit/date I did not verify from a MinIO primary source). The README still advertises an "embedded web based object browser" but community threads say recent CE source no longer ships the full console. **Not relevant to this repo** — nothing here uses the console.
- **Commercial pivot:** README directs commercial users to **AIStor** (MinIO Inc.'s enterprise product) and warns that *"commercial/proprietary usage of the AGPLv3 software, including repackaging or reselling ... is done at your own risk."*
- **Last CE release with a published Docker image:** `RELEASE.2025-09-07T16-13-09Z`. Docker Hub tag list (<https://hub.docker.com/r/minio/minio/tags>) shows nothing newer than the 2025-09-07 tag (pushed ~12 months ago). **This is exactly the tag the repo pins** — confirmed.
- **One later CE release exists, source-only, no image:** `RELEASE.2025-10-15T17-29-55Z` (2025-10-16), a security release for **CVE-2025-62506** (<https://github.com/minio/minio/releases>, <https://github.com/advisories/GHSA-jjjj-jwhf-8rgr>). CE users get this fix only by building from source.

### CVEs / advisories against MinIO after the pinned tag

From <https://github.com/minio/minio/security/advisories> (all published *after* `RELEASE.2025-09-07`):

| GHSA | CVE | Published | Severity | Summary |
|---|---|---|---|---|
| GHSA-jjjj-jwhf-8rgr | CVE-2025-62506 | 2025-10-16 | High | Priv-esc via session-policy bypass in service accounts / STS |
| GHSA-5cx5-wh4m-82fh | — | 2026-03-19 | Moderate | JWT algorithm confusion in OIDC auth |
| GHSA-jv87-32hw-hh99 | — | 2026-03-20 | Moderate | LDAP login brute-force via user enumeration |
| GHSA-3rh2-v3gr-35p9 | — | 2026-03-27 | High | SSE metadata injection via replication headers |
| GHSA-h749-fxx7-pwpg | — | 2026-04-07 | High | DoS via unbounded allocation in S3 Select CSV parsing |
| GHSA-9c4q-hq6p-c237 | — | 2026-04-11 | High | Unauthenticated object write via Snowball auto-extract |
| GHSA-hv4r-mvr4-25vw | — | 2026-04-14 | High | Unauthenticated object write via query-string signature bypass |
| GHSA-xh8f-g2qw-gcm7 | — | 2026-04-25 | Moderate | Path traversal via msgpack body in `ReadMultiple` storage-REST endpoint |

(CVE numbers beyond CVE-2025-62506 not all verified individually; GHSA IDs and dates are from the advisories index.)

**Security-patch path for CE users:** build from source at HEAD, or use a community fork. There is **no official patched CE binary or image** for anything after 2025-09-07 (the 2025-10-15 tag is source-only).

**Exposure in *this* repo:** every advisory above requires network access to the MinIO API by an attacker who can present *some* credential or reach an unauthenticated endpoint. In Pictogram the server is (a) a Testcontainers container on a random ephemeral port, torn down per run, or (b) a `compose.dev.yaml` container on `localhost`. No untrusted principals, no internet path, credentials are literals in the repo. Realistic exploit risk ≈ **nil**. The genuine costs are: **(1)** image scanners (Trivy/Grype/Docker Scout/Dependabot) flag the frozen base image with a growing list forever; **(2)** no upgrade path if a future Docker Engine / OCI spec change rejects the 2025 image; **(3)** the Debian base layer goes EOL and accrues OS CVEs that also can't be patched.

---

## 2. Option 1 — pin indefinitely

**What it buys:** zero work now. `RELEASE.2025-09-07T16-13-09Z` is an **immutable digest-addressable tag**, so nothing drifts. CI already caches it by `docker save`. The Dependabot `ignore` for `minio/minio` is already in place (`.github/dependabot.yml`). It stays a genuine drop-in MinIO forever (same console, same `/minio/health/ready`, same env vars). This is the *only* option with literally no engineering, no learning curve, and no blast radius.

**What it costs:**
- Vulnerability scanners will report the image indefinitely, and the count only grows. For a solo project this is noise you learn to ignore, or suppress via a `.trivyignore` / Dependabot config — itself a small maintenance item.
- **Eventual bit-rot risks (all slow, none imminent):** the image's base OS reaches EOL and stops getting even upstream OS patches; a future Docker/containerd/OCI change could refuse an old image manifest; the Docker Hub `minio/minio` repo could theoretically be pulled (MinIO has *not* deleted the historical tags as of this writing — <https://hub.docker.com/r/minio/minio/tags> still lists them). Mitigate the last one cheaply by `docker pull` + `docker save` the tarball into the repo's CI cache / a release asset so the project owns a copy.
- No forward path: the day you *do* need a newer feature or a real fix, you're doing the migration anyway, just later.

**Verdict:** correct default for a test-only dependency. Cheap, honest, reversible. Add a comment in `SharedMinio.java` and `compose.yaml` explaining *why* the tag is frozen (so nobody "helpfully" bumps it to a tag that doesn't exist), and keep issue #87 open as the tracking item.

---

## 3. Option 2 — community fork: Pigsty's `pgsty/silo` (was `pgsty/minio`)

**What it is:** a **hard fork with patches**, not a repackage. Source: <https://github.com/pgsty/silo> (README: *"S3-Compatible Object Storage. A MinIO fork maintained by PGSTY"*). Renamed from `pgsty/minio` to `pgsty/silo` with the **2026-08-06 release** (<https://github.com/pgsty/silo/releases/tag/RELEASE.2026-08-06T00-00-00Z> — *"the first release published under the Silo name"*). Based on upstream MinIO `RELEASE.2025-12-03T12-00-00Z` (per <https://silo.pigsty.io/> / search of the project docs; the release page itself does not restate the base tag).

**Maintainer / trust signals:**
- One maintainer: **Ruohang Feng (Vonng)**, author of Pigsty (the "batteries-included PostgreSQL distribution"). Background writeup: <https://blog.vonng.com/en/db/minio-resurrect/> — ran a 25 PB MinIO deployment at TanTan; MinIO is a supported Pigsty module for PG backups, so he has a real dependency on it.
- ~2.7k GitHub stars (<https://github.com/pgsty/silo>).
- **No commercial SLA** — the maintainer explicitly says *"please don't treat this as a commercial SLA"* (<https://blog.vonng.com/en/db/minio-resurrect/>). Bus factor = 1.
- AGPL-3.0-or-later, DCO sign-off, no CLA.

**What the fork changes vs. upstream CE:**
- **Restores the full web console** (upstream cut it to a stub).
- Ships **prebuilt multi-arch binaries + images** (amd64 + arm64) — the thing upstream stopped doing.
- Backports security fixes: CVE-2025-62506 early on; the 2026-08-06 release notes cite CVE-2026-34986 (`go-jose`) and CVE-2026-39883 (OpenTelemetry) dependency patches and *"govulncheck reports no vulnerability reachable from the server code."*
- Keeps S3 protocol, env vars, metrics, on-disk format unchanged (CI compatibility audit).
- Release cadence: *"every one to two months, at most a quarter apart"* (project manifest, per <https://blog.vonng.com/en/db/minio-resurrect/>).

**Docker image:** `docker.io/pgsty/silo:RELEASE.2026-08-06T00-00-00Z` (199 MB standard, 128 MB `-distroless`), amd64+arm64 (<https://github.com/pgsty/silo/releases/tag/RELEASE.2026-08-06T00-00-00Z>). The old `docker.io/pgsty/minio` tags also still publish (up to `RELEASE.2026-08-04`).

**Adoption cost for this repo:** **lowest of any non-pin option.** It *is* MinIO — same `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD`, same `server /data` command, same `/minio/health/ready` endpoint. Change = swap the image string in `SharedMinio.java`, `compose.yaml`, `compose.dev.yaml`, `ci.yml`. **The `SharedMinio` wait strategy and property registration are unchanged.** Could keep the class name.

**Security posture:** better than a frozen upstream (someone is actually shipping fixes), but the "who ships a CVE fix and how fast" answer is *"one person, when he gets to it, no guarantee"*. For a test-only dependency that's fine; you would not want bus-factor-1 on a production edge.

**Risk:** the fork is <1 year old. If the maintainer loses interest, you're back to option 1 but from a less-canonical base. Provenance/signing: images are built in public CI on GitHub; I did not verify cosign/SLSA attestations.

---

## 4. Option 3 — migrate to another S3 server

### 4a. Garage (Deuxfleurs)

| Axis | Finding | Source |
|---|---|---|
| License / governance | AGPLv3; maintained by **Deuxfleurs**, a French non-profit self-hosting collective; in production since 2020 | <https://git.deuxfleurs.fr/Deuxfleurs/garage>, <https://garagehq.deuxfleurs.fr/> |
| Maturity / cadence | v2.0.0 released **2025-06-15**; current **v2.3.0**. Stated cadence: *"a major version about once a year"* | <https://garagehq.deuxfleurs.fr/blog/2025-06-garage-v2/>, <https://garagehq.deuxfleurs.fr/documentation/quick-start/> |
| S3 compat | Supports **CreateBucket, HeadBucket, PutObject, GetObject, HeadObject, DeleteObject, ListObjectsV2, multipart, presigned URLs**; **both path-style and vhost-style**. Permissions are per-access-key-per-bucket, *not* AWS IAM/ACL | <https://garagehq.deuxfleurs.fr/documentation/reference-manual/s3-compatibility/> |
| Docker image | `dxflrs/garage` — **~19.7 MB compressed** (amd64), 18.9 MB (arm64); scratch-based static Rust binary; quick-start pins `dxflrs/garage:v2.3.0` | <https://hub.docker.com/r/dxflrs/garage/tags>, <https://garagehq.deuxfleurs.fr/documentation/quick-start/> |
| Health check | `GET /health` on the **admin API port** (default **3903**), no auth, `200` when quorum is up | <https://garagehq.deuxfleurs.fr/documentation/reference-manual/admin-api/> |
| Security track record | No CVEs / RUSTSEC advisories found for Garage itself (searched; none surfaced). Small, focused codebase | (negative result — could not find any) |
| Auth model | **No `MINIO_ROOT_USER` equivalent.** S3 keys are created after startup via `garage key create` (CLI) or the admin HTTP API (`/v2/CreateKey` with a bearer admin token). `garage server --single-node --default-bucket` skips cluster-layout assignment | <https://garagehq.deuxfleurs.fr/documentation/quick-start/>, <https://garagehq.deuxfleurs.fr/documentation/reference-manual/admin-api/> |

**Adoption cost for this repo: highest of the three candidates.** Garage has no "static root credentials via env" mode. `SharedMinio` currently just sets two env vars and lets the app create the bucket. With Garage the container start sequence becomes: start → wait for `/health` on 3903 → call admin API (or `execInContainer`) to create an access key with a *known* ID/secret (Garage generates random `GK...` IDs, so you'd create then read them back and feed them into `registerTo`) → grant the key `createBucket` permission → then let the app run. That's real Testcontainers plumbing and a meaningful rewrite of `SharedMinio.java`. The app-side S3 client is still unchanged (path-style works). But the friction is exactly in the file that's the hard part of any migration.

**Upside:** tiny image (fastest CI pull/cache, ~1/3 the size of anything else), clean unauthenticated health endpoint, no company-rug-pull risk (non-profit, AGPL, 5+ years), no CVE history.

### 4b. SeaweedFS

| Axis | Finding | Source |
|---|---|---|
| License / governance | Apache-2.0; community-driven, `chrislusf` + contributors; enterprise edition exists separately | <https://github.com/seaweedfs/seaweedfs> |
| Maturity / cadence | Very active (~15k commits), ~34k stars; frequent releases, currently in the **v4.3x** line (2026) | <https://github.com/seaweedfs/seaweedfs> |
| S3 compat | S3 gateway supports PutObject/GetObject/HeadObject/DeleteObject/ListObjectsV2/CreateBucket/HeadBucket; **path-style is the native addressing mode** (virtual-host style needs extra DNS/config). Multipart supported | <https://github.com/seaweedfs/seaweedfs>, wiki "Amazon S3 API" |
| Docker image | `chrislusf/seaweedfs` (single binary, `weed`); amd64 + arm64. Compressed size ~30 MB (Docker Hub — **not verified precisely here**) | <https://github.com/seaweedfs/seaweedfs> |
| Health check | S3 port historically returns 404 on `/healthz` (parsed as a bucket name) — see <https://github.com/seaweedfs/seaweedfs/issues/8243>. Master UI on `:9333`. **Testcontainers would likely use a TCP-port or log-message wait rather than an HTTP health check** |
| Security track record | Actively audited: 2026 batch of advisories — path traversal / cross-bucket (CVE-2026-54917, CVSS 7.8; CVE-2026-55874), improper authz (CVE-2026-55873), unauth SSRF (CVE-2026-73080) — **all fixed promptly** (v4.34 etc.) | <https://github.com/advisories/GHSA-w62w-66v9-vvgv>, <https://advisories.gitlab.com/golang/github.com/seaweedfs/seaweedfs/> |
| Auth model | S3 identities/keys via `-s3.config` JSON or env; single-node: `docker run -p 8333:8333 -e AWS_ACCESS_KEY_ID=... -e AWS_SECRET_ACCESS_KEY=... -e S3_BUCKET=... chrislusf/seaweedfs server -s3` | <https://github.com/seaweedfs/seaweedfs> README quick-start |

**Adoption cost for this repo: moderate — closer to a drop-in than Garage.** One container, `weed server -s3`, static credentials via env (`AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY`), app still creates its own bucket, path-style works unchanged. The one wrinkle is the **wait strategy**: no reliable unauthenticated S3 health endpoint, so `SharedMinio` would switch from `Wait.forHttp("/minio/health/ready")` to `Wait.forListeningPort()` or `Wait.forLogMessage(".*Start Seaweed S3 API.*", 1)`. That's a small, well-trodden change.

**On the CVE list:** it looks alarming, but read it the right way — SeaweedFS gets *looked at* by security researchers precisely because it's widely deployed, and the maintainer ships fixes within days. That is the *opposite* of MinIO CE's situation (fixes exist) and RustFS's (fixes exist but the bugs are amateur-hour). For a test-only dependency none of these are reachable anyway.

### 4c. RustFS

| Axis | Finding | Source |
|---|---|---|
| License / governance | Apache-2.0; **RustFS Inc.** ("Sponsored OSS"), commercial contact `hello@rustfs.com` | <https://github.com/rustfs/rustfs>, <https://hub.docker.com/r/rustfs/rustfs/tags> |
| Maturity | **Pre-1.0: `1.0.0-rc.5`** as of 2026. "MinIO on-disk compatibility" is a preview feature | <https://github.com/rustfs/rustfs> |
| S3 compat | Positions as a MinIO API drop-in; core ops (Put/Get/Head/Delete/List/CreateBucket) present; compatibility matrix in docs. Path-style: not explicitly confirmed from a primary doc page, but it mirrors MinIO's `:9000` API | <https://github.com/rustfs/rustfs>, <https://docs.rustfs.com/> |
| Docker image | `rustfs/rustfs` — **~141 MB compressed** (amd64), 137 MB (arm64); ports 9000/9001; creds `RUSTFS_ACCESS_KEY` / `RUSTFS_SECRET_KEY`; console creds default `rustfsadmin`/`rustfsadmin` | <https://hub.docker.com/r/rustfs/rustfs/tags> |
| Health check | `/health` and `/health/ready` on `:9000`, unauthenticated — **but documented as buggy**: returns 200 when the server can't serve (<https://github.com/rustfs/rustfs/issues/2658>), and 403 when routed through S3 auth in some setups (<https://github.com/rustfs/rustfs/issues/1844>) | |
| Security track record | **Poor for its age.** CVE-2025-68926 — *hardcoded gRPC auth token / "skeleton key"*, unauthenticated full admin takeover, all versions < `1.0.0-alpha.77` (<https://securityonline.info/cve-2025-68926-critical-hardcoded-credential-flaw-exposes-rustfs-storage-clusters/>). Plus CVE-2026-27822 (console stored XSS), CVE-2026-22782 (HMAC secret logged), CVE-2026-27607 (presigned POST policy bypass), CVE-2026-73288 (object-lock priv-esc) | GitHub Advisory DB / cvedetails.com |

**Adoption cost for this repo: low-to-moderate** (env-var creds like MinIO, same ports), but the **health endpoint is unreliable** so the wait strategy needs care, and **the security/maturity signal is bad** — a hardcoded skeleton-key auth bypass is the kind of defect that says the codebase hasn't had adult supervision. Even for test-only use that's a reason to not adopt it as a long-term dependency. **Not recommended.**

---

## 5. Comparison across the security + cost axes

| | **Opt 1: Pin `RELEASE.2025-09-07`** | **Opt 2: `pgsty/silo` fork** | **Opt 3a: Garage** | **Opt 3b: SeaweedFS** | **Opt 3c: RustFS** |
|---|---|---|---|---|---|
| **Security — is there a patch path?** | ❌ none (source build only) | ✅ backported, bus-factor 1, no SLA | ✅ upstream, small clean codebase | ✅ upstream, fast response | ⚠️ upstream, but weak code hygiene |
| **Security — CVE noise in scanners** | grows forever, unfixable | tracks a maintained line | ~none found | many-but-patched | many, some critical |
| **Security — exposure in THIS repo** | none (test/localhost only) | none | none | none | none |
| **Engineering effort now** | **zero** | swap image string (4 files) | rewrite `SharedMinio` (key provisioning) + compose | swap image + change wait strategy | swap image + fragile wait strategy |
| **`SharedMinio` wait strategy** | unchanged | unchanged (`/minio/health/ready`) | new: `/health` on :3903 | new: port/log wait | new: buggy `/health/ready` |
| **App S3 client (`MediaStorageConfiguration`)** | unchanged | unchanged | unchanged | unchanged | unchanged |
| **`forcePathStyle=true` + `endpointOverride`** | ✅ works | ✅ works | ✅ works | ✅ works (native) | ✅ works (assumed) |
| **Static-cred (`ROOT_USER`) startup model** | ✅ | ✅ | ❌ post-start key creation | ✅ | ✅ |
| **CI image size (compressed)** | ~55 MB (MinIO, unverified) | 199 MB / 128 MB distroless | **~20 MB** | ~30 MB (unverified) | ~141 MB |
| **arm64 (Apple Silicon dev)** | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Ongoing maintenance burden** | ignore scanner noise | track a young 1-person fork | track ~annual releases | track frequent releases | track a churning pre-1.0 project |
| **Learning curve** | none | none | **moderate** (key/bucket model, admin API) | low | low |
| **Blast radius if it goes wrong** | none | low (revert to Opt 1) | low (test-only) | low (test-only) | low (test-only) |
| **Project rug-pull risk** | already happened | fork could be abandoned | very low (non-profit, 5 yr) | low (huge community) | medium (VC-ish "Sponsored OSS") |
| **ADR needed?** | no (comment + issue) | yes | yes | yes | yes |

---

## 6. Reasoning for the recommendation

**Security is not the deciding axis here, because there is no attacker.** The honest security question for a `localhost`/Testcontainers S3 stub is *"if we ever need a patched artifact, does one exist?"* — and *"will scanners nag us?"*. That reframing kills the urgency behind Option 3.

**Cost of adoption is the deciding axis, and it points at "do nothing yet".** Option 1 is zero work and fully reversible. Every migration's real cost is concentrated in `SharedMinio.java` (container definition + wait strategy) and the compose files, and none of the alternatives is so much better than a frozen MinIO that it's worth paying that now, in a solo learning project, for a dependency that already works.

**If forced to move**, the ranking is:
1. **`pgsty/silo`** — near-zero effort (it *is* MinIO: same env, same command, same health endpoint, `SharedMinio` barely changes), someone is shipping security fixes. Risk: a <1-year-old fork with bus factor 1.
2. **SeaweedFS** — moderate effort (env-var creds, path-style native; only the wait strategy changes), Apache-2.0, enormous community, fast security response, no rug-pull risk. Best choice if the goal is "a dependency I never have to think about again."
3. **Garage** — cleanest project and tiniest image, but the post-startup key-provisioning model makes it the most `SharedMinio` rework, and this repo gets no benefit from its geo-distribution focus.
4. **RustFS** — no. Pre-1.0, a hardcoded skeleton-key CVE, buggy health endpoint. Wrong signal for a long-term dependency even in test.

**Chosen path (see the Decision section at the top):** stay on the pin (Option 1); close #87 with this note as the record; if/when #84 (version catalog + infra bumps) or a future media feature forces the compose/`SharedMinio` files open anyway, fold in a **SeaweedFS** migration then and write the ADR at that point. That sequences the work to when the touch points are already being edited, minimising total cost.

---

## 7. Things I could not verify from a primary source

- The exact MinIO commit/date that removed the CE console (community threads say "May 2025"; no MinIO-owned primary source pinned down).
- The maintainer reply text in <https://github.com/minio/minio/issues/21714> (archived thread; fetch returned only metadata).
- CVE numbers for the post-2025-09 MinIO advisories other than CVE-2025-62506 (GHSA IDs and dates are confirmed from the advisories index; NIST CVE mapping not individually checked).
- Precise compressed image sizes for `minio/minio:RELEASE.2025-09-07...` and `chrislusf/seaweedfs` (Docker Hub did not render the size in the fetch; figures above are approximate).
- Whether `pgsty/silo` images carry cosign/SLSA provenance attestations.
- RustFS path-style addressing — inferred from its MinIO-API-drop-in positioning, not confirmed on a primary doc page.
- Garage `dxflrs/garage` semver tags on Docker Hub — the tag list rendered as commit hashes; the Garage quick-start doc references `dxflrs/garage:v2.3.0`, so semver tags are believed to exist.
