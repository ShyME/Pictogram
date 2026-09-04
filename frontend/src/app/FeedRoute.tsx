import { CommentCount, CommentThread, prefetchCommentCounts } from '@features/comments';
import { FeedPage } from '@features/feed';
import { LikeButton, prefetchPostLikes } from '@features/likes';
import { PostDetailDialog } from '@features/post';
import { useOutletContext } from 'react-router';
import type { AppLayoutContext } from './AppLayout';

export function FeedRoute() {
  const { profile } = useOutletContext<AppLayoutContext>();

  return (
    <FeedPage
      renderLike={(postId) => <LikeButton postId={postId} />}
      renderCommentCount={(postId) => <CommentCount postId={postId} />}
      preloadLikes={prefetchPostLikes}
      preloadComments={prefetchCommentCounts}
      renderPostDetail={(detail, onClose) => (
        <PostDetailDialog
          {...detail}
          onClose={onClose}
          renderLike={(postId) => <LikeButton postId={postId} />}
          renderComments={(postId) => (
            <CommentThread
              postId={postId}
              canComment
              viewerId={profile.userId}
              postAuthorId={detail.authorId}
            />
          )}
        />
      )}
    />
  );
}
