import { CommentCount, CommentThread, prefetchCommentCounts } from '@features/comments';
import { exploreSource, FeedPage } from '@features/feed';
import { LikeButton, LikeCount, prefetchPostLikes } from '@features/likes';
import { PostDetailDialog } from '@features/post';
import { useOutletContext } from 'react-router';
import type { PublicLayoutContext } from './PublicLayout';

export function ExploreRoute() {
  const { viewer } = useOutletContext<PublicLayoutContext>();
  const renderLike = (postId: string) =>
    viewer ? <LikeButton postId={postId} /> : <LikeCount postId={postId} />;

  return (
    <FeedPage
      source={exploreSource}
      renderLike={renderLike}
      renderCommentCount={(postId) => <CommentCount postId={postId} />}
      preloadLikes={prefetchPostLikes}
      preloadComments={prefetchCommentCounts}
      renderPostDetail={(detail, onClose) => (
        <PostDetailDialog
          {...detail}
          onClose={onClose}
          renderLike={renderLike}
          renderComments={(postId) => (
            <CommentThread
              postId={postId}
              canComment={viewer != null}
              viewerId={viewer?.userId ?? null}
              postAuthorId={detail.authorId}
            />
          )}
        />
      )}
    />
  );
}
