import {
  type ChangeEvent,
  type PointerEvent as ReactPointerEvent,
  type Ref,
  type SyntheticEvent,
  useCallback,
  useImperativeHandle,
  useLayoutEffect,
  useRef,
  useState,
} from "react";
import {
  type Crop,
  type ImageSize,
  MAX_ZOOM,
  OUTPUT_SIZE,
  initialCrop,
  panCrop,
  zoomCrop,
  zoomLevel,
} from "../model/crop";

export type CropperHandle = {
  /** Render the framed square to a downscaled JPEG blob, ready to upload. */
  getCroppedBlob: () => Promise<Blob>;
};

/**
 * A square frame the author pans and zooms over their photo — the output is always 1:1
 * (the media pipeline crops to square server-side regardless, but framing it here keeps
 * what they see as what they get). `getCroppedBlob` draws the framed region to a 1080²
 * canvas, which also downscales an oversized photo before upload.
 */
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
    const measure = () => setFrameSize(frame.clientWidth);
    measure();
    if (typeof ResizeObserver === "undefined") return;
    const observer = new ResizeObserver(measure);
    observer.observe(frame);
    return () => observer.disconnect();
  }, []);

  useImperativeHandle(
    ref,
    () => ({
      async getCroppedBlob() {
        const img = imgRef.current;
        if (!img || !crop) throw new Error("The crop is not ready yet.");

        const canvas = document.createElement("canvas");
        canvas.width = OUTPUT_SIZE;
        canvas.height = OUTPUT_SIZE;
        const context = canvas.getContext("2d");
        if (!context) throw new Error("Canvas 2D is unavailable.");
        context.drawImage(img, crop.x, crop.y, crop.size, crop.size, 0, 0, OUTPUT_SIZE, OUTPUT_SIZE);

        return await new Promise<Blob>((resolve, reject) => {
          canvas.toBlob(
            (blob) => (blob ? resolve(blob) : reject(new Error("Could not read the crop."))),
            "image/jpeg",
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

  const scale = crop && frameSize ? frameSize / crop.size : 1;
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
          style={
            image && crop
              ? {
                  position: "absolute",
                  width: image.width * scale,
                  height: image.height * scale,
                  left: -crop.x * scale,
                  top: -crop.y * scale,
                  maxWidth: "none",
                  cursor: "grab",
                }
              : { position: "absolute", inset: 0, width: "100%", height: "100%", objectFit: "cover" }
          }
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
