import { frameBox, initialCrop, zoomCropAt } from '@features/post/cropper/crop';
import { SquareCropper, type CropperHandle } from '@features/post/cropper/SquareCropper';
import { fireEvent, render, screen } from '@testing-library/react';
import { createRef } from 'react';
import { afterEach, expect, test } from 'vitest';

const image = { width: 1600, height: 1200 };
const FRAME_SIZE = 300;

// jsdom lays out nothing: stub the frame's measured width (read via `clientWidth` in a
// `useLayoutEffect`) and the loaded image's natural size so the crop math has real
// numbers to work with, then hand back the elements the wheel handler reads geometry from.
function renderLoadedCropper() {
  Object.defineProperty(HTMLElement.prototype, 'clientWidth', {
    configurable: true,
    value: FRAME_SIZE,
  });
  const ref = createRef<CropperHandle>();
  render(<SquareCropper ref={ref} src="blob:preview" />);

  const img = screen.getByRole('img');
  Object.defineProperties(img, {
    naturalWidth: { configurable: true, value: image.width },
    naturalHeight: { configurable: true, value: image.height },
  });
  fireEvent.load(img);

  const frame = img.parentElement;
  if (!frame) throw new Error('expected the cropper frame element');
  frame.getBoundingClientRect = () => ({
    left: 0,
    top: 0,
    right: FRAME_SIZE,
    bottom: FRAME_SIZE,
    width: FRAME_SIZE,
    height: FRAME_SIZE,
    x: 0,
    y: 0,
    toJSON() {
      return { left: 0, top: 0, right: FRAME_SIZE, bottom: FRAME_SIZE, width: FRAME_SIZE };
    },
  });

  return { img, frame };
}

function pixels(value: string): number {
  return Number(value.replace('px', ''));
}

function expectFramedAs(img: HTMLElement, factor: number, anchor: { x: number; y: number }) {
  const crop = zoomCropAt(initialCrop(image), factor, anchor, image);
  const box = frameBox(image, crop, FRAME_SIZE);
  expect(pixels(img.style.width)).toBeCloseTo(box.width);
  expect(pixels(img.style.height)).toBeCloseTo(box.height);
  expect(pixels(img.style.left)).toBeCloseTo(box.left);
  expect(pixels(img.style.top)).toBeCloseTo(box.top);
}

afterEach(() => {
  Reflect.deleteProperty(HTMLElement.prototype, 'clientWidth');
});

test('shows the photo in a frame with a zoom control', () => {
  render(<SquareCropper src="blob:preview" />);

  expect(screen.getByRole('img')).toHaveAttribute('src', 'blob:preview');
  expect(screen.getByRole('slider', { name: /zoom/i })).toBeInTheDocument();
});

test('getCroppedBlob rejects before the image has loaded', async () => {
  const ref = createRef<CropperHandle>();
  render(<SquareCropper ref={ref} src="blob:preview" />);

  await expect(ref.current?.getCroppedBlob()).rejects.toThrow(/not ready/i);
});

test('scrolling up over the frame zooms in, anchored to the cursor rather than the centre', () => {
  const { img, frame } = renderLoadedCropper();

  fireEvent.wheel(frame, { clientX: 0, clientY: 150, deltaY: -100 });

  expectFramedAs(img, 1.2, { x: 0, y: 0.5 });
});

test('scrolling down zooms out', () => {
  const { img, frame } = renderLoadedCropper();

  fireEvent.wheel(frame, { clientX: 150, clientY: 150, deltaY: 50 });

  expectFramedAs(img, 0.9, { x: 0.5, y: 0.5 });
});

test('ctrl+wheel — how browsers report a trackpad pinch — zooms at a steeper rate', () => {
  const { img, frame } = renderLoadedCropper();

  fireEvent.wheel(frame, { clientX: 150, clientY: 150, deltaY: -100, ctrlKey: true });

  expectFramedAs(img, 1.6, { x: 0.5, y: 0.5 });
});

test('a line-mode delta (Firefox’s default for a mouse wheel) is scaled up, not treated as pixels', () => {
  const { img, frame } = renderLoadedCropper();

  // DOM_DELTA_LINE: browsers report a couple of lines per notch, not ~100px.
  fireEvent.wheel(frame, { clientX: 150, clientY: 150, deltaY: -3, deltaMode: 1 });

  // -3 lines * 16px/line = -48px, so factor = 1 - (-48 * 0.002) = 1.096.
  expectFramedAs(img, 1.096, { x: 0.5, y: 0.5 });
});

test('wheeling over the frame prevents the page from scrolling', () => {
  const { frame } = renderLoadedCropper();

  const wasNotCancelled = fireEvent.wheel(frame, { clientX: 150, clientY: 150, deltaY: -100 });

  expect(wasNotCancelled).toBe(false);
});
