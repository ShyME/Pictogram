import {
  MAX_ZOOM,
  clampCrop,
  frameBox,
  initialCrop,
  panCrop,
  zoomCrop,
  zoomCropAt,
  zoomLevel,
} from '@features/post/cropper/crop';
import { expect, test } from 'vitest';

const landscape = { width: 1600, height: 1200 };
const portrait = { width: 900, height: 1500 };

test('initialCrop is the largest centred square that fits', () => {
  expect(initialCrop(landscape)).toEqual({ x: 200, y: 0, size: 1200 });
  expect(initialCrop(portrait)).toEqual({ x: 0, y: 300, size: 900 });
  expect(zoomLevel(initialCrop(landscape), landscape)).toBe(1);
});

test('clampCrop keeps the window inside the image', () => {
  const clamped = clampCrop({ x: -500, y: 999, size: 1200 }, landscape);
  expect(clamped.x).toBe(0);
  expect(clamped.y).toBe(0);
});

test('clampCrop refuses to zoom past the frame or past MAX_ZOOM', () => {
  const tooBig = clampCrop({ x: 0, y: 0, size: 99_999 }, landscape);
  expect(tooBig.size).toBe(1200);

  const tooSmall = clampCrop({ x: 0, y: 0, size: 1 }, landscape);
  expect(tooSmall.size).toBe(1200 / MAX_ZOOM);
});

test('zoomCrop zooms around the window centre', () => {
  const start = initialCrop(landscape);
  const zoomed = zoomCrop(start, 2, landscape);

  expect(zoomed.size).toBe(600);
  expect(zoomed.x + zoomed.size / 2).toBe(800);
  expect(zoomed.y + zoomed.size / 2).toBe(600);
  expect(zoomLevel(zoomed, landscape)).toBe(2);
});

test('frameBox scales the image to the frame and offsets it by the crop origin', () => {
  const crop = { x: 200, y: 100, size: 600 };
  const frameSize = 300;

  expect(frameBox(landscape, crop, frameSize)).toEqual({
    width: 800,
    height: 600,
    left: -100,
    top: -50,
  });
});

test('zoomCropAt zooms around an arbitrary anchor, keeping the anchored image point fixed', () => {
  const start = initialCrop(landscape);

  const topLeft = zoomCropAt(start, 2, { x: 0, y: 0 }, landscape);
  expect(topLeft).toEqual({ x: 200, y: 0, size: 600 });

  const bottomRight = zoomCropAt(start, 2, { x: 1, y: 1 }, landscape);
  expect(bottomRight).toEqual({ x: 800, y: 600, size: 600 });
  expect(zoomLevel(bottomRight, landscape)).toBe(2);
});

test('zoomCropAt at the frame centre matches zoomCrop', () => {
  const start = initialCrop(landscape);
  expect(zoomCropAt(start, 2, { x: 0.5, y: 0.5 }, landscape)).toEqual(
    zoomCrop(start, 2, landscape),
  );
});

test('zoomCropAt re-clamps when zooming out would push the window off the image', () => {
  const crop = { x: 1000, y: 0, size: 400 };
  expect(zoomCropAt(crop, 0.25, { x: 1, y: 0 }, landscape)).toEqual({ x: 0, y: 0, size: 1200 });
});

test('panCrop converts a screen drag into image pixels and re-clamps', () => {
  const start = zoomCrop(initialCrop(landscape), 2, landscape);
  const panned = panCrop(start, 30, 0, 300, landscape);
  expect(panned.x).toBe(440);

  expect(panCrop(start, 100_000, 0, 300, landscape).x).toBe(0);
  expect(panCrop(start, -100_000, 0, 300, landscape).x).toBe(landscape.width - 600);
});
