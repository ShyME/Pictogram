import { Button, Textarea, toast } from '@shared';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useEffect, useId, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router';
import { CAPTION_MAX_LENGTH, captionLength, isCaptionWithinLimit } from './caption';
import { SquareCropper, type CropperHandle } from './cropper/SquareCropper';
import { publishPost, uploadPhoto } from './postApi';
import { postsByAuthorKey } from './queryKeys';

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
  const [caption, setCaption] = useState('');

  useEffect(() => {
    if (!previewUrl) return;
    return () => {
      URL.revokeObjectURL(previewUrl);
    };
  }, [previewUrl]);

  const publish = useMutation({
    mutationFn: async () => {
      if (!cropper.current) throw new Error('The crop is not ready.');
      const framed = await cropper.current.getCroppedBlob();
      const mediaId = await uploadPhoto(framed);
      return publishPost({ mediaId, caption });
    },
    onSuccess: (outcome) => {
      if (outcome.status !== 'published') return;
      toast({ variant: 'success', title: 'Post shared' });
      void queryClient.invalidateQueries({ queryKey: postsByAuthorKey(authorId) });
      void navigate(`/u/${profileUsername}`, { replace: true });
    },
    onError: () => {
      toast({
        variant: 'error',
        title: 'Something went wrong',
        description: "We couldn't publish your post. Please try again.",
      });
    },
  });

  function onPickFile(event: React.ChangeEvent<HTMLInputElement>) {
    const picked = event.target.files?.[0] ?? null;
    if (!picked) return;
    setFile(picked);
    setPreviewUrl(URL.createObjectURL(picked));
  }

  const captionCount = captionLength(caption);
  const isCaptionValid = isCaptionWithinLimit(caption);
  const outcome = publish.data;

  return (
    <main className="mx-auto max-w-md px-4 py-8">
      <div className="flex items-baseline justify-between gap-4">
        <h1 className="text-xl font-semibold tracking-tight text-foreground">New post</h1>
        <Button variant="ghost" size="sm" asChild>
          <Link to={`/u/${profileUsername}`}>Cancel</Link>
        </Button>
      </div>

      {file ? (
        <form
          onSubmit={(event) => {
            event.preventDefault();
            if (isCaptionValid) publish.mutate();
          }}
          className="mt-6"
        >
          {previewUrl && <SquareCropper ref={cropper} src={previewUrl} />}

          <div className="mt-4">
            <label htmlFor={captionFieldId} className="block text-sm font-medium text-foreground">
              Caption <span className="font-normal text-foreground-subtle">(optional)</span>
            </label>
            <Textarea
              id={captionFieldId}
              name="caption"
              value={caption}
              onChange={(event) => {
                setCaption(event.target.value);
              }}
              rows={3}
              className="mt-1 resize-none"
              aria-invalid={!isCaptionValid}
            />
            <p
              className={`mt-1 text-right text-xs ${
                isCaptionValid ? 'text-foreground-subtle' : 'text-danger-text'
              }`}
            >
              {captionCount} / {CAPTION_MAX_LENGTH}
            </p>
          </div>

          {outcome?.status === 'media-unusable' && (
            <p role="alert" className="mt-2 text-sm text-danger-text">
              That photo couldn&rsquo;t be used. Pick it again and retry.
            </p>
          )}
          {outcome?.status === 'caption-too-long' && (
            <p role="alert" className="mt-2 text-sm text-danger-text">
              Your caption is too long. Shorten it and try again.
            </p>
          )}

          <div className="mt-4 flex items-center gap-3">
            <Button type="submit" loading={publish.isPending} disabled={!isCaptionValid}>
              {publish.isPending ? 'Sharing…' : 'Share'}
            </Button>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={() => {
                setFile(null);
                setPreviewUrl(null);
                publish.reset();
              }}
            >
              Choose a different photo
            </Button>
          </div>
        </form>
      ) : (
        <label className="mt-6 flex aspect-square w-full cursor-pointer flex-col items-center justify-center gap-2 rounded-card border-2 border-dashed border-border-strong bg-surface text-sm text-foreground-muted hover:border-foreground-subtle">
          <span className="text-base font-medium text-foreground">Select a photo</span>
          <span>It&rsquo;ll be cropped to a square.</span>
          <input type="file" accept="image/*" onChange={onPickFile} className="sr-only" />
        </label>
      )}
    </main>
  );
}
