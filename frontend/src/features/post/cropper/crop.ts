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
  const centreX = crop.x + crop.size / 2;
  const centreY = crop.y + crop.size / 2;
  const size = crop.size / factor;
  return clampCrop({ x: centreX - size / 2, y: centreY - size / 2, size }, image);
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

function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max);
}
