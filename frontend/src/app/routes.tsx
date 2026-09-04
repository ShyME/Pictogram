import { LoginPage, installApiAuth } from '@features/auth';
import { FeedPage } from '@features/feed';
import { FollowButton, FollowCounts, FollowListPage } from '@features/follow';
import { LikeButton, LikeCount, prefetchPostLikes } from '@features/likes';
import { PostGrid } from '@features/post';
import { EditProfilePage, OnboardingPage, ProfilePage, profileLoader } from '@features/profile';
import { createBrowserRouter } from 'react-router';
import { AppLayout } from './AppLayout';
import { NewPostRoute } from './NewPostRoute';
import { RouteError } from './RouteError';
import { UiShowcase } from './UiShowcase';
import { followListLoader } from './followListLoader';
import {
  editProfileLoader,
  loginLoader,
  newPostLoader,
  onboardingLoader,
  rootLoader,
} from './guards';

installApiAuth();

export const router = createBrowserRouter([
  {
    errorElement: <RouteError />,
    children: [
      {
        element: <AppLayout />,
        loader: rootLoader,
        children: [
          {
            index: true,
            element: (
              <FeedPage
                renderLike={(postId) => <LikeButton postId={postId} />}
                preloadLikes={prefetchPostLikes}
              />
            ),
          },
        ],
      },
      { path: '/login', element: <LoginPage />, loader: loginLoader },
      { path: '/onboarding', element: <OnboardingPage />, loader: onboardingLoader },
      { path: '/settings/profile', element: <EditProfilePage />, loader: editProfileLoader },
      { path: '/new', element: <NewPostRoute />, loader: newPostLoader },
      {
        path: '/u/:username',
        element: (
          <ProfilePage
            renderGrid={(authorId, isOwnProfile, isViewerAuthenticated) => (
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
                preloadLikes={prefetchPostLikes}
              />
            )}
            renderFollowButton={(userId) => <FollowButton userId={userId} />}
            renderFollowCounts={(userId, handle) => (
              <FollowCounts userId={userId} username={handle} />
            )}
          />
        ),
        loader: profileLoader,
      },
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
      // Dev/test-only component showcase; the branch and its import fold away in a
      // production build (import.meta.env.DEV === false).
      ...(import.meta.env.DEV ? [{ path: '/ui', element: <UiShowcase /> }] : []),
    ],
  },
]);
