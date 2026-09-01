export const CAPTION_MAX_LENGTH = 2200;

export function captionLength(caption: string): number {
  return [...caption.trim()].length;
}

export function isCaptionWithinLimit(caption: string): boolean {
  return captionLength(caption) <= CAPTION_MAX_LENGTH;
}
