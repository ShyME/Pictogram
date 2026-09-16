export const OUTPUT_SIZE = 1080;

export const MAX_ZOOM = 5;

export type Crop = { x: number; y: number; size: number };

export type ImageSize = { width: number; height: number };

export function initialCrop({ width, height }: ImageSize): Crop {
  const size = Math.min(width, height);
  return { x: (width - size) / 2, y: (height - size) / 2, size };
}

export function clampCrop(crop: Crop, image: ImageSize): Crop {
  const maxSize = Math.min(image.width, image.height);
  const size = clamp(crop.size, maxSize / MAX_ZOOM, maxSize);
  return {
    size,
    x: clamp(crop.x, 0, image.width - size),
    y: clamp(crop.y, 0, image.height - size),
  };
}

export function zoomCrop(crop: Crop, factor: number, image: ImageSize): Crop {
  return zoomCropAt(crop, factor, { x: 0.5, y: 0.5 }, image);
}

export type Anchor = { x: number; y: number };

// Zooms the crop window so the image point under `anchor` (a fraction of the crop's own
// width/height, e.g. the cursor position within the frame) stays under it after zooming —
// `zoomCrop`'s centre-anchored zoom is the special case where anchor is (0.5, 0.5).
export function zoomCropAt(crop: Crop, factor: number, anchor: Anchor, image: ImageSize): Crop {
  const anchorX = crop.x + anchor.x * crop.size;
  const anchorY = crop.y + anchor.y * crop.size;
  const size = crop.size / factor;
  return clampCrop({ x: anchorX - anchor.x * size, y: anchorY - anchor.y * size, size }, image);
}

export function panCrop(
  crop: Crop,
  screenDx: number,
  screenDy: number,
  viewportSize: number,
  image: ImageSize,
): Crop {
  const perScreenPixel = crop.size / viewportSize;
  return clampCrop(
    {
      x: crop.x - screenDx * perScreenPixel,
      y: crop.y - screenDy * perScreenPixel,
      size: crop.size,
    },
    image,
  );
}

export function zoomLevel(crop: Crop, image: ImageSize): number {
  return Math.min(image.width, image.height) / crop.size;
}

export type FrameBox = { width: number; height: number; left: number; top: number };

export function frameBox(image: ImageSize, crop: Crop, frameSize: number): FrameBox {
  const scale = frameSize / crop.size;
  return {
    width: image.width * scale,
    height: image.height * scale,
    left: -crop.x * scale,
    top: -crop.y * scale,
  };
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max);
}
