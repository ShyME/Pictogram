import { CommentThread } from '@features/comments';
import { LikeButton, LikeCount } from '@features/likes';
import { PostDetailPage } from '@features/post';
import { useOutletContext } from 'react-router';
import type { PublicLayoutContext } from './PublicLayout';

export function PostDetailRoute() {
  const { viewer } = useOutletContext<PublicLayoutContext>();

  return (
    <PostDetailPage
      renderLike={(postId) =>
        viewer ? <LikeButton postId={postId} /> : <LikeCount postId={postId} />
      }
      renderComments={(postId, authorId) => (
        <CommentThread
          postId={postId}
          canComment={viewer !== null}
          viewerId={viewer?.userId ?? null}
          postAuthorId={authorId}
        />
      )}
    />
  );
}
