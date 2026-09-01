// The crop the user positions is a square window over the source image, in the image's own
// pixels: `size` is the window's side, `(x, y)` its top-left. The media pipeline re-encodes
// server-side to a canonical square, so this window is purely how the author frames the shot
// — but rendering it client-side before upload keeps what they see as what they get, and
// downscales an oversized photo on the way out.

export const OUTPUT_SIZE = 1080;

/** How far past "fills the frame" the user may zoom in — a guard against a 1px crop. */
export const MAX_ZOOM = 5;

export type Crop = { x: number; y: number; size: number };

export type ImageSize = { width: number; height: number };

/** The largest centred square that fits the image — the most zoomed-out ("cover") framing. */
export function initialCrop({ width, height }: ImageSize): Crop {
  const size = Math.min(width, height);
  return { x: (width - size) / 2, y: (height - size) / 2, size };
}

/** Clamp a window back inside the image and within the zoom range. */
export function clampCrop(crop: Crop, image: ImageSize): Crop {
  const maxSize = Math.min(image.width, image.height);
  const size = clamp(crop.size, maxSize / MAX_ZOOM, maxSize);
  return {
    size,
    x: clamp(crop.x, 0, image.width - size),
    y: clamp(crop.y, 0, image.height - size),
  };
}

/** Zoom around the window's centre by `factor` (>1 zooms in), then re-clamp. */
export function zoomCrop(crop: Crop, factor: number, image: ImageSize): Crop {
  const centreX = crop.x + crop.size / 2;
  const centreY = crop.y + crop.size / 2;
  const size = crop.size / factor;
  return clampCrop({ x: centreX - size / 2, y: centreY - size / 2, size }, image);
}

/**
 * Shift the window by a drag measured in *screen* pixels over a viewport `viewportSize`
 * pixels wide, converting to image pixels, then re-clamp.
 */
export function panCrop(
  crop: Crop,
  screenDx: number,
  screenDy: number,
  viewportSize: number,
  image: ImageSize,
): Crop {
  const perScreenPixel = crop.size / viewportSize;
  return clampCrop(
    { x: crop.x - screenDx * perScreenPixel, y: crop.y - screenDy * perScreenPixel, size: crop.size },
    image,
  );
}

/** The current zoom multiplier over the cover framing (1 = fully zoomed out). */
export function zoomLevel(crop: Crop, image: ImageSize): number {
  return Math.min(image.width, image.height) / crop.size;
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max);
}
