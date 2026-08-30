# Media

Holds uploaded images as bytes. Every upload is re-encoded to one canonical square format,
so the rest of the system never deals with orientation, format, or dimensions.

## Language

**Media**:
One uploaded image belonging to a user, referenced by a `MediaId`. Stored as bytes in object
storage. Always square, always the one canonical format.
_Avoid_: Photo, picture, image, file, attachment, asset, upload

**Original**:
The full-size rendition of a media, produced by re-encoding and cropping the upload to the
fixed square size.
_Avoid_: Full, source, raw

**Thumbnail**:
The small square rendition of a media, used in grids and feed previews.
_Avoid_: Preview, small, mini

**Orphan**:
A media that no post references. Orphans older than a grace period are deleted by a
collection job — this covers both "uploaded but never posted" and "the post was deleted".
_Avoid_: Dangling, unused, stale
