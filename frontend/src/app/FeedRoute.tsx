import { CommentCount, CommentThread, prefetchCommentCounts } from '@features/comments';
import {
  exploreSource,
  FeedPage,
  FeedScopeToggle,
  followingSource,
  readFeedScopePreference,
  writeFeedScopePreference,
  type FeedScope,
} from '@features/feed';
import { LikeButton, prefetchPostLikes } from '@features/likes';
import { PostDetailDialog } from '@features/post';
import { useState } from 'react';
import { useOutletContext } from 'react-router';
import type { AppLayoutContext } from './AppLayout';

export function FeedRoute() {
  const { profile } = useOutletContext<AppLayoutContext>();
  const [scope, setScope] = useState<FeedScope>(readFeedScopePreference);

  const selectScope = (next: FeedScope) => {
    setScope(next);
    writeFeedScopePreference(next);
  };

  return (
    <FeedPage
      source={scope === 'explore' ? exploreSource : followingSource}
      toolbar={
        <FeedScopeToggle
          active={scope}
          onSelectFollowing={() => {
            selectScope('following');
          }}
          onSelectExplore={() => {
            selectScope('explore');
          }}
        />
      }
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
