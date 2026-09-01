// Mirrors the backend `Caption` rule (post module): optional, trimmed, at most 2200 code
// points. Checked here so the composer can show a live counter and block an over-long
// caption before the request; the backend stays the authority.

export const CAPTION_MAX_LENGTH = 2200;

/** Length in code points, so an emoji counts as one — the same unit the backend counts in. */
export function captionLength(caption: string): number {
  return [...caption.trim()].length;
}

export function isCaptionWithinLimit(caption: string): boolean {
  return captionLength(caption) <= CAPTION_MAX_LENGTH;
}
