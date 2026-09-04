import { Modal, ModalContent, ModalTitle } from '@shared';
import type { ReactNode } from 'react';

export type PostDetail = { postId: string; imageUrl: string; caption: string | null };

export function PostDetailDialog({
  postId,
  imageUrl,
  caption,
  renderLike,
  renderComments,
  onClose,
}: PostDetail & {
  renderLike?: (postId: string) => ReactNode;
  renderComments?: (postId: string) => ReactNode;
  onClose: () => void;
}) {
  return (
    <Modal
      isOpen
      onOpenChange={(isOpen) => {
        if (!isOpen) onClose();
      }}
    >
      <ModalContent className="flex max-h-[85dvh] max-w-lg flex-col overflow-hidden p-0">
        <ModalTitle className="sr-only">Post</ModalTitle>
        <div className="min-h-0 overflow-y-auto">
          <img
            src={imageUrl}
            alt={caption ?? 'A post'}
            className="aspect-square w-full bg-surface-muted object-cover"
          />
          <div className="flex items-center justify-between gap-2 px-4 pb-2 pt-3">
            {renderLike?.(postId)}
            <button
              type="button"
              onClick={onClose}
              className="ml-auto rounded-control px-3 py-1.5 text-sm font-medium text-foreground-muted transition-colors hover:bg-surface-muted"
            >
              Close
            </button>
          </div>
          {caption && <p className="px-4 pb-3 pt-1 text-sm text-foreground">{caption}</p>}
          {renderComments && (
            <div className="border-t border-border px-4 py-4">{renderComments(postId)}</div>
          )}
        </div>
      </ModalContent>
    </Modal>
  );
}
