import { useEffect, useId, useRef, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Link, useNavigate } from "react-router";
import { publishPost, uploadPhoto } from "./post-api";
import { SquareCropper, type CropperHandle } from "./cropper/SquareCropper";
import { CAPTION_MAX_LENGTH, captionLength, isCaptionWithinLimit } from "./caption";
import { postsByAuthorKey } from "./query-keys";

/**
 * The post composer: pick a photo, frame it in the square crop, optionally caption it, and
 * publish. Publishing is two calls — upload the framed bytes for a `MediaId`, then publish
 * the post — after which the author's grid query is invalidated and we land on their profile
 * where the new post sits at the top.
 */
export function NewPostPage({
  authorId,
  profileUsername,
}: {
  authorId: string;
  profileUsername: string;
}) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const captionFieldId = useId();
  const cropper = useRef<CropperHandle>(null);

  const [file, setFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [caption, setCaption] = useState("");

  useEffect(() => {
    if (!file) return;
    const url = URL.createObjectURL(file);
    setPreviewUrl(url);
    return () => URL.revokeObjectURL(url);
  }, [file]);

  const publish = useMutation({
    mutationFn: async () => {
      if (!cropper.current) throw new Error("The crop is not ready.");
      const framed = await cropper.current.getCroppedBlob();
      const mediaId = await uploadPhoto(framed);
      return publishPost({ mediaId, caption });
    },
    onSuccess: (outcome) => {
      if (outcome.status === "published") {
        queryClient.invalidateQueries({ queryKey: postsByAuthorKey(authorId) });
        navigate(`/u/${profileUsername}`, { replace: true });
      }
    },
  });

  function onPickFile(event: React.ChangeEvent<HTMLInputElement>) {
    const picked = event.target.files?.[0] ?? null;
    if (picked) setFile(picked);
  }

  const captionCount = captionLength(caption);
  const captionValid = isCaptionWithinLimit(caption);
  const outcome = publish.data;

  return (
    <div className="min-h-dvh bg-neutral-50">
      <header className="flex items-center justify-between border-b border-neutral-200 bg-white px-4 py-3">
        <Link to="/" className="font-semibold tracking-tight text-neutral-900">
          Pictogram
        </Link>
        <Link to={`/u/${profileUsername}`} className="text-sm font-medium text-neutral-500 hover:text-neutral-900">
          Cancel
        </Link>
      </header>

      <main className="mx-auto max-w-md px-4 py-8">
        <h1 className="text-xl font-semibold tracking-tight text-neutral-900">New post</h1>

        {!file ? (
          <label className="mt-6 flex aspect-square w-full cursor-pointer flex-col items-center justify-center gap-2 rounded-xl border-2 border-dashed border-neutral-300 bg-white text-sm text-neutral-500 hover:border-neutral-400">
            <span className="text-base font-medium text-neutral-700">Select a photo</span>
            <span>It&rsquo;ll be cropped to a square.</span>
            <input type="file" accept="image/*" onChange={onPickFile} className="sr-only" />
          </label>
        ) : (
          <form
            onSubmit={(event) => {
              event.preventDefault();
              if (captionValid) publish.mutate();
            }}
            className="mt-6"
          >
            {previewUrl && <SquareCropper ref={cropper} src={previewUrl} />}

            <div className="mt-4">
              <label htmlFor={captionFieldId} className="block text-sm font-medium text-neutral-700">
                Caption <span className="font-normal text-neutral-400">(optional)</span>
              </label>
              <textarea
                id={captionFieldId}
                name="caption"
                value={caption}
                onChange={(event) => setCaption(event.target.value)}
                rows={3}
                className="mt-1 w-full resize-none rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:border-neutral-900 focus:outline-none aria-[invalid=true]:border-red-500"
                aria-invalid={!captionValid}
              />
              <p className={`mt-1 text-right text-xs ${captionValid ? "text-neutral-400" : "text-red-600"}`}>
                {captionCount} / {CAPTION_MAX_LENGTH}
              </p>
            </div>

            {publish.isError && (
              <p role="alert" className="mt-2 text-sm text-red-600">
                Something went wrong publishing your post. Please try again.
              </p>
            )}
            {outcome?.status === "media-unusable" && (
              <p role="alert" className="mt-2 text-sm text-red-600">
                That photo couldn&rsquo;t be used. Pick it again and retry.
              </p>
            )}
            {outcome?.status === "caption-too-long" && (
              <p role="alert" className="mt-2 text-sm text-red-600">
                Your caption is too long. Shorten it and try again.
              </p>
            )}

            <div className="mt-4 flex items-center gap-3">
              <button
                type="submit"
                disabled={!captionValid || publish.isPending}
                className="rounded-lg bg-neutral-900 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-neutral-700 disabled:opacity-50"
              >
                {publish.isPending ? "Sharing…" : "Share"}
              </button>
              <button
                type="button"
                onClick={() => {
                  setFile(null);
                  publish.reset();
                }}
                className="text-sm font-medium text-neutral-500 hover:text-neutral-900"
              >
                Choose a different photo
              </button>
            </div>
          </form>
        )}
      </main>
    </div>
  );
}
