# Media: one format, one aspect ratio, backend-authoritative transcode

- **Status:** Accepted
- **Relates to:** ADR-0009 (the `Media` row lives in Postgres; the bytes live in MinIO)

Every uploaded image is re-encoded **server-side** to JPEG at a fixed square size, plus a
square thumbnail. The client may crop (to let the user choose what lands in the square) and
may downscale a very large photo before upload to save bandwidth, but the backend is the
authority: it validates the bytes, re-encodes, resizes, and generates the thumbnail.

The backend must re-encode regardless because client bytes are never trusted, so a
client-side transcode would be duplicated work we can't rely on. Fixing the format and the
1:1 ratio removes orientation and layout logic everywhere, makes the profile grid trivial,
and lets us treat content type and dimensions as constants (not stored on `Media`).
Re-encoding also strips EXIF/GPS metadata for free; the pipeline reads the EXIF orientation
tag first so the canonical rendition is upright, then drops all metadata.

## Accepted input formats

**JPEG, PNG, and WebP.** These are what a JVM `ImageIO` pipeline decodes (WebP via the
TwelveMonkeys plugin). The canonical output is JPEG at 1080×1080; the thumbnail is
320×320.

**HEIC is not accepted.** There is no pure-JVM HEIC decoder — it needs a native `libheif`
in the image, on CI runners, and in local dev, which is disproportionate for v1. In
practice it rarely bites: iOS Safari converts HEIC to JPEG when a photo is chosen for
upload. Bytes that no codec can decode (a raw `.heic` among them) are rejected with a
`media-undecodable` 400. Adding HEIC later is a self-contained change (a decoder plugin
plus the native lib) that does not reopen this decision.

## Consequences

- Users cannot get back the exact file they uploaded, and cannot post non-square images.
- Multi-image posts, portrait/landscape, and original-quality downloads are out of scope
  and would each reopen this decision.
- The `media` module owns object storage: bytes live in MinIO (S3-compatible), under keys
  derived from the `MediaId` by convention, reached through the S3 API (ADR-0009 keeps the
  row itself in Postgres). The bucket is created on first upload, so no environment needs a
  provisioning step.
