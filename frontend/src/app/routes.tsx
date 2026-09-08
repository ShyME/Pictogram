import { LoginPage, installApiAuth } from '@features/auth';
import { MessageButton } from '@features/chat';
import { CommentThread } from '@features/comments';
import { FollowButton, FollowCounts, FollowListPage } from '@features/follow';
import { LikeButton, LikeCount, prefetchPostLikes } from '@features/likes';
import { PostGrid } from '@features/post';
import { EditProfilePage, OnboardingPage, ProfilePage, profileLoader } from '@features/profile';
import { createBrowserRouter } from 'react-router';
import { AppLayout } from './AppLayout';
import { FeedRoute } from './FeedRoute';
import { NewPostRoute } from './NewPostRoute';
import { PublicLayout } from './PublicLayout';
import { RouteError } from './RouteError';
import { UiShowcase } from './UiShowcase';
import { followListLoader } from './followListLoader';
import { loginLoader, onboardingLoader, rootLoader, viewerLoader } from './guards';

installApiAuth();

export const router = createBrowserRouter([
  {
    errorElement: <RouteError />,
    children: [
      {
        element: <AppLayout />,
        loader: rootLoader,
        children: [
          { index: true, element: <FeedRoute /> },
          { path: '/new', element: <NewPostRoute /> },
          { path: '/settings/profile', element: <EditProfilePage /> },
          {
            path: '/u/:username/followers',
            element: <FollowListPage mode="followers" />,
            loader: followListLoader,
          },
          {
            path: '/u/:username/following',
            element: <FollowListPage mode="following" />,
            loader: followListLoader,
          },
        ],
      },
      { path: '/login', element: <LoginPage />, loader: loginLoader },
      { path: '/onboarding', element: <OnboardingPage />, loader: onboardingLoader },
      {
        element: <PublicLayout />,
        loader: viewerLoader,
        children: [
          {
            path: '/u/:username',
            element: (
              <ProfilePage
                renderGrid={(authorId, isOwnProfile, isViewerAuthenticated, viewerId) => (
                  <PostGrid
                    authorId={authorId}
                    manageable={isOwnProfile}
                    renderLike={(postId) =>
                      isViewerAuthenticated ? (
                        <LikeButton postId={postId} />
                      ) : (
                        <LikeCount postId={postId} />
                      )
                    }
                    renderComments={(postId) => (
                      <CommentThread
                        postId={postId}
                        canComment={isViewerAuthenticated}
                        viewerId={viewerId}
                        postAuthorId={authorId}
                      />
                    )}
                    preloadLikes={prefetchPostLikes}
                  />
                )}
                renderFollowButton={(userId) => <FollowButton userId={userId} />}
                renderFollowCounts={(userId, handle) => (
                  <FollowCounts userId={userId} username={handle} />
                )}
                renderMessageButton={(peer) => <MessageButton peer={peer} />}
              />
            ),
            loader: profileLoader,
          },
        ],
      },
      // Dev/test-only component showcase; the branch and its import fold away in a
      // production build (import.meta.env.DEV === false).
      ...(import.meta.env.DEV ? [{ path: '/ui', element: <UiShowcase /> }] : []),
    ],
  },
]);
