export const CAPTION_MAX_LENGTH = 2200;

export function captionLength(caption: string): number {
  // Count Unicode code points, not UTF-16 units, so an emoji counts as one
  // (caption.test.ts). The backend enforces the same limit the same way.
  // eslint-disable-next-line @typescript-eslint/no-misused-spread
  return [...caption.trim()].length;
}

export function isCaptionWithinLimit(caption: string): boolean {
  return captionLength(caption) <= CAPTION_MAX_LENGTH;
}
