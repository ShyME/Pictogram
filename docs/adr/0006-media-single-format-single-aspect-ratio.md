# Media: one format, one aspect ratio, backend-authoritative transcode

Every uploaded image is re-encoded **server-side** to JPEG at a fixed square size, plus a
square thumbnail. The client may crop (to let the user choose what lands in the square) and
may downscale a very large photo before upload to save bandwidth, but the backend is the
authority: it validates the bytes, re-encodes, resizes, and generates the thumbnail.

The backend must re-encode regardless because client bytes are never trusted, so a
client-side transcode would be duplicated work we can't rely on. Fixing the format and the
1:1 ratio removes orientation and layout logic everywhere, makes the profile grid trivial,
and lets us treat content type and dimensions as constants (not stored on `Media`).
Re-encoding also strips EXIF/GPS metadata for free.

## Consequences

- Users cannot get back the exact file they uploaded, and cannot post non-square images.
- Multi-image posts, portrait/landscape, and original-quality downloads are out of scope
  and would each reopen this decision.
