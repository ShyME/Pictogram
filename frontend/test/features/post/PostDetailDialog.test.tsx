import type { Post } from '@features/post/post';
import { PostDetailDialog } from '@features/post/PostDetailDialog';
import { fireEvent, render, screen } from '@testing-library/react';
import { expect, test, vi } from 'vitest';

const post: Post = {
  postId: 'p-1',
  authorId: 'u-1',
  mediaId: 'm-9',
  caption: 'a caption',
  publishedAt: '2026-09-01T10:00:00Z',
};

test('shows the full-size image, the caption, and the injected like control', () => {
  render(
    <PostDetailDialog
      post={post}
      renderLike={(postId) => <button type="button">heart {postId}</button>}
      onClose={vi.fn()}
    />,
  );

  expect(screen.getByRole('img')).toHaveAttribute('src', '/api/media/m-9/original');
  expect(screen.getByText('a caption')).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'heart p-1' })).toBeInTheDocument();
});

test('closes on the Close button', () => {
  const onClose = vi.fn();
  render(<PostDetailDialog post={post} onClose={onClose} />);

  fireEvent.click(screen.getByRole('button', { name: 'Close' }));

  expect(onClose).toHaveBeenCalledOnce();
});

test('closes on Escape', () => {
  const onClose = vi.fn();
  render(<PostDetailDialog post={post} onClose={onClose} />);

  fireEvent.keyDown(document, { key: 'Escape' });

  expect(onClose).toHaveBeenCalledOnce();
});

test('closes on a backdrop click but not on a click inside the dialog', () => {
  const onClose = vi.fn();
  render(<PostDetailDialog post={post} onClose={onClose} />);

  fireEvent.click(screen.getByRole('dialog'));
  expect(onClose).not.toHaveBeenCalled();

  const backdrop = screen.getByRole('dialog').parentElement;
  if (!backdrop) throw new Error('expected a backdrop element');
  fireEvent.click(backdrop);
  expect(onClose).toHaveBeenCalledOnce();
});
