import type { CropperHandle } from '@features/post/cropper/SquareCropper';
import { NewPostPage } from '@features/post/NewPostPage';
import { jsonResponse, pathOf, problemResponse, stubFetch } from '@test-support/mockFetch';
import { renderWithProviders } from '@test-support/render';
import { fireEvent, screen, waitFor } from '@testing-library/react';
import type { ReactNode, Ref } from 'react';
import { useImperativeHandle } from 'react';
import { afterEach, expect, test, vi } from 'vitest';

const navigate = vi.fn();

vi.mock('react-router', () => ({
  useNavigate: () => navigate,
  Link: ({ to, children }: { to: string; children: ReactNode }) => <a href={to}>{children}</a>,
}));

vi.mock('@features/post/cropper/SquareCropper', () => ({
  SquareCropper: ({ ref }: { ref: Ref<CropperHandle> }) => {
    useImperativeHandle(ref, () => ({
      getCroppedBlob: () => Promise.resolve(new Blob(['framed'], { type: 'image/jpeg' })),
    }));
    return <div data-testid="cropper" />;
  },
}));

afterEach(() => {
  vi.unstubAllGlobals();
  navigate.mockReset();
});

function renderComposer() {
  return renderWithProviders(<NewPostPage authorId="u-1" profileUsername="ada" />);
}

function pickAPhoto(container: HTMLElement) {
  const input = container.querySelector('input[type="file"]');
  if (!input) throw new Error('file input not rendered');
  const file = new File(['bytes'], 'photo.png', { type: 'image/png' });
  fireEvent.change(input, { target: { files: [file] } });
}

test('starts on a file picker and moves to the cropper once a photo is chosen', () => {
  const { container } = renderComposer();
  expect(screen.getByText(/select a photo/i)).toBeInTheDocument();

  pickAPhoto(container);

  expect(screen.getByTestId('cropper')).toBeInTheDocument();
  expect(screen.getByRole('button', { name: /share/i })).toBeInTheDocument();
});

test("uploads the framed bytes, publishes with the caption, and lands on the author's profile", async () => {
  const calls = stubFetch((request) =>
    jsonResponse(
      pathOf(request) === '/api/media'
        ? { mediaId: 'm-9' }
        : { postId: 'p-9', authorId: 'u-1', mediaId: 'm-9', caption: 'hello', publishedAt: 't' },
      201,
    ),
  );
  const { container } = renderComposer();
  pickAPhoto(container);

  fireEvent.change(screen.getByLabelText(/caption/i), { target: { value: 'hello' } });
  fireEvent.click(screen.getByRole('button', { name: /share/i }));

  await waitFor(() => {
    expect(navigate).toHaveBeenCalledWith('/u/ada', { replace: true });
  });
  expect(calls.map((request) => pathOf(request))).toEqual(['/api/media', '/api/posts']);
  const publishBody = (await calls[1].clone().json()) as unknown;
  expect(publishBody).toEqual({ mediaId: 'm-9', caption: 'hello' });
});

test('blocks Share and counts down while the caption is over the limit', () => {
  const { container } = renderComposer();
  pickAPhoto(container);

  fireEvent.change(screen.getByLabelText(/caption/i), { target: { value: 'x'.repeat(2201) } });

  expect(screen.getByRole('button', { name: /share/i })).toBeDisabled();
  expect(screen.getByText('2201 / 2200')).toBeInTheDocument();
});

test('surfaces a publish failure and stays on the composer', async () => {
  stubFetch((request) =>
    pathOf(request) === '/api/media'
      ? jsonResponse({ mediaId: 'm-9' }, 201)
      : problemResponse('post-media-unusable', 422),
  );
  const { container } = renderComposer();
  pickAPhoto(container);
  fireEvent.click(screen.getByRole('button', { name: /share/i }));

  expect(await screen.findByRole('alert')).toHaveTextContent(/couldn.t be used/i);
  expect(navigate).not.toHaveBeenCalled();
});
