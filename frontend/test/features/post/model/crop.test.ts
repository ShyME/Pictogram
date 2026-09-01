import { expect, test } from "vitest";
import {
  MAX_ZOOM,
  clampCrop,
  initialCrop,
  panCrop,
  zoomCrop,
  zoomLevel,
} from "@features/post/model/crop";

const landscape = { width: 1600, height: 1200 };
const portrait = { width: 900, height: 1500 };

test("initialCrop is the largest centred square that fits", () => {
  expect(initialCrop(landscape)).toEqual({ x: 200, y: 0, size: 1200 });
  expect(initialCrop(portrait)).toEqual({ x: 0, y: 300, size: 900 });
  expect(zoomLevel(initialCrop(landscape), landscape)).toBe(1);
});

test("clampCrop keeps the window inside the image", () => {
  const clamped = clampCrop({ x: -500, y: 999, size: 1200 }, landscape);
  expect(clamped.x).toBe(0);
  expect(clamped.y).toBe(0); // 1200-tall image, 1200 window: only y=0 fits
});

test("clampCrop refuses to zoom past the frame or past MAX_ZOOM", () => {
  const tooBig = clampCrop({ x: 0, y: 0, size: 99999 }, landscape);
  expect(tooBig.size).toBe(1200);

  const tooSmall = clampCrop({ x: 0, y: 0, size: 1 }, landscape);
  expect(tooSmall.size).toBe(1200 / MAX_ZOOM);
});

test("zoomCrop zooms around the window centre", () => {
  const start = initialCrop(landscape); // centre (800, 600), size 1200
  const zoomed = zoomCrop(start, 2, landscape);

  expect(zoomed.size).toBe(600);
  expect(zoomed.x + zoomed.size / 2).toBe(800);
  expect(zoomed.y + zoomed.size / 2).toBe(600);
  expect(zoomLevel(zoomed, landscape)).toBe(2);
});

test("panCrop converts a screen drag into image pixels and re-clamps", () => {
  const start = zoomCrop(initialCrop(landscape), 2, landscape); // size 600, x 500, y 300
  // drag 30 screen px right over a 300px viewport -> 60 image px; the window moves left
  const panned = panCrop(start, 30, 0, 300, landscape);
  expect(panned.x).toBe(440);

  // a huge drag can't push the window off the image
  expect(panCrop(start, 100000, 0, 300, landscape).x).toBe(0);
  expect(panCrop(start, -100000, 0, 300, landscape).x).toBe(landscape.width - 600);
});
