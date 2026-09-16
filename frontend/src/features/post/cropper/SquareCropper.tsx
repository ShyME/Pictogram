import {
  type ChangeEvent,
  type PointerEvent as ReactPointerEvent,
  type Ref,
  type SyntheticEvent,
  useCallback,
  useEffect,
  useImperativeHandle,
  useLayoutEffect,
  useRef,
  useState,
} from 'react';
import {
  type Crop,
  type ImageSize,
  MAX_ZOOM,
  OUTPUT_SIZE,
  frameBox,
  initialCrop,
  panCrop,
  zoomCrop,
  zoomCropAt,
  zoomLevel,
} from './crop';

// Wheel-zoom rates: per pixel of (normalised) `deltaY`, how much the zoom factor changes.
// Ctrl+wheel is how browsers report a trackpad pinch, and pinch gestures report smaller
// deltas than a mouse wheel notch for the same felt gesture, so it gets a steeper rate.
const WHEEL_ZOOM_RATE = 0.002;
const PINCH_ZOOM_RATE = 0.006;

// `deltaY` is only in pixels when `deltaMode` is DOM_DELTA_PIXEL (0). Firefox's default
// mouse-wheel deltas are DOM_DELTA_LINE (1), single-digit values that would make zoom
// nearly imperceptible if treated as pixels — scale them up to a pixel-ish magnitude.
// DOM_DELTA_PAGE (2) is rare enough (e.g. some accessibility tools) not to special-case.
function pixelDeltaY(event: WheelEvent): number {
  const LINE_HEIGHT_PX = 16;
  return event.deltaMode === 1 ? event.deltaY * LINE_HEIGHT_PX : event.deltaY;
}

export type CropperHandle = {
  getCroppedBlob: () => Promise<Blob>;
};

export function SquareCropper({ src, ref }: { src: string; ref?: Ref<CropperHandle> }) {
  const imgRef = useRef<HTMLImageElement>(null);
  const frameRef = useRef<HTMLDivElement>(null);
  const dragFrom = useRef<{ x: number; y: number } | null>(null);

  const [image, setImage] = useState<ImageSize | null>(null);
  const [crop, setCrop] = useState<Crop | null>(null);
  const [frameSize, setFrameSize] = useState(0);

  useLayoutEffect(() => {
    const frame = frameRef.current;
    if (!frame) return;
    const measure = () => {
      setFrameSize(frame.clientWidth);
    };
    measure();
    if (typeof ResizeObserver === 'undefined') return;
    const observer = new ResizeObserver(measure);
    observer.observe(frame);
    return () => {
      observer.disconnect();
    };
  }, []);

  // Registered as a native, non-passive listener rather than React's `onWheel`: React
  // marks its own wheel listener passive by default, which silently drops
  // `preventDefault` and lets the page scroll underneath the frame while cropping.
  //
  // Updates `crop` functionally, from whatever it is when the update applies rather than
  // the value closed over when the listener was attached, so a burst of wheel events fired
  // faster than React re-renders still compounds instead of only the last one landing.
  useEffect(() => {
    const frame = frameRef.current;
    if (!frame || !image) return;
    function onWheel(event: WheelEvent) {
      event.preventDefault();
      if (!frame || !image) return;
      const rect = frame.getBoundingClientRect();
      if (rect.width === 0 || rect.height === 0) return;
      const anchor = {
        x: (event.clientX - rect.left) / rect.width,
        y: (event.clientY - rect.top) / rect.height,
      };
      const rate = event.ctrlKey ? PINCH_ZOOM_RATE : WHEEL_ZOOM_RATE;
      const factor = Math.max(0.1, 1 - pixelDeltaY(event) * rate);
      setCrop((previousCrop) =>
        previousCrop ? zoomCropAt(previousCrop, factor, anchor, image) : previousCrop,
      );
    }
    frame.addEventListener('wheel', onWheel, { passive: false });
    return () => {
      frame.removeEventListener('wheel', onWheel);
    };
  }, [image]);

  useImperativeHandle(
    ref,
    () => ({
      async getCroppedBlob() {
        const img = imgRef.current;
        if (!img || !crop) throw new Error('The crop is not ready yet.');

        const canvas = document.createElement('canvas');
        canvas.width = OUTPUT_SIZE;
        canvas.height = OUTPUT_SIZE;
        const context = canvas.getContext('2d');
        if (!context) throw new Error('Canvas 2D is unavailable.');
        context.drawImage(
          img,
          crop.x,
          crop.y,
          crop.size,
          crop.size,
          0,
          0,
          OUTPUT_SIZE,
          OUTPUT_SIZE,
        );

        return await new Promise<Blob>((resolve, reject) => {
          canvas.toBlob(
            (blob) => {
              if (blob) resolve(blob);
              else reject(new Error('Could not read the crop.'));
            },
            'image/jpeg',
            0.9,
          );
        });
      },
    }),
    [crop],
  );

  const onImageLoad = useCallback((event: SyntheticEvent<HTMLImageElement>) => {
    const el = event.currentTarget;
    const size = { width: el.naturalWidth, height: el.naturalHeight };
    setImage(size);
    setCrop(initialCrop(size));
  }, []);

  function beginDrag(event: ReactPointerEvent) {
    event.currentTarget.setPointerCapture(event.pointerId);
    dragFrom.current = { x: event.clientX, y: event.clientY };
  }

  function onDrag(event: ReactPointerEvent) {
    const from = dragFrom.current;
    if (!from || !image || !crop || frameSize === 0) return;
    setCrop(panCrop(crop, event.clientX - from.x, event.clientY - from.y, frameSize, image));
    dragFrom.current = { x: event.clientX, y: event.clientY };
  }

  function endDrag(event: ReactPointerEvent) {
    event.currentTarget.releasePointerCapture(event.pointerId);
    dragFrom.current = null;
  }

  function onZoom(event: ChangeEvent<HTMLInputElement>) {
    if (!image || !crop) return;
    const target = Number(event.target.value);
    setCrop(zoomCrop(crop, target / zoomLevel(crop, image), image));
  }

  const framed = image && crop && frameSize > 0 ? frameBox(image, crop, frameSize) : null;
  const currentZoom = image && crop ? zoomLevel(crop, image) : 1;

  return (
    <div className="flex flex-col gap-3">
      <div
        ref={frameRef}
        onPointerDown={beginDrag}
        onPointerMove={onDrag}
        onPointerUp={endDrag}
        onPointerCancel={endDrag}
        className="relative aspect-square w-full touch-none overflow-hidden rounded-xl bg-neutral-900 select-none"
      >
        <img
          ref={imgRef}
          src={src}
          alt="Position your photo in the square frame"
          onLoad={onImageLoad}
          draggable={false}
          className={
            framed ? 'absolute max-w-none cursor-grab' : 'absolute inset-0 size-full object-cover'
          }
          style={framed ?? undefined}
        />
      </div>

      <label className="flex items-center gap-3 text-sm text-neutral-500">
        <span className="sr-only">Zoom</span>
        <span aria-hidden>−</span>
        <input
          type="range"
          min={1}
          max={MAX_ZOOM}
          step={0.01}
          value={currentZoom}
          onChange={onZoom}
          aria-label="Zoom"
          disabled={!crop}
          className="flex-1 accent-neutral-900"
        />
        <span aria-hidden>+</span>
      </label>
    </div>
  );
}
