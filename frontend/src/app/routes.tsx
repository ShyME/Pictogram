import { createBrowserRouter } from "react-router";
import { LoginPage, installApiAuth } from "@features/auth";
import { EditProfilePage, OnboardingPage, ProfilePage, profileLoader } from "@features/profile";
import { FeedPage } from "@features/feed";
import { PostGrid } from "@features/post";
import { FollowButton, FollowCounts, FollowListPage } from "@features/follow";
import { AppLayout } from "./AppLayout";
import { NewPostRoute } from "./NewPostRoute";
import { RouteError } from "./RouteError";
import { followListLoader } from "./follow-list-loader";
import { editProfileLoader, loginLoader, newPostLoader, onboardingLoader, rootLoader } from "./guards";

installApiAuth();

export const router = createBrowserRouter([
  {
    errorElement: <RouteError />,
    children: [
      {
        element: <AppLayout />,
        loader: rootLoader,
        children: [{ index: true, element: <FeedPage /> }],
      },
      { path: "/login", element: <LoginPage />, loader: loginLoader },
      { path: "/onboarding", element: <OnboardingPage />, loader: onboardingLoader },
      { path: "/settings/profile", element: <EditProfilePage />, loader: editProfileLoader },
      { path: "/new", element: <NewPostRoute />, loader: newPostLoader },
      {
        path: "/u/:username",
        element: (
          <ProfilePage
            renderGrid={(authorId, isOwnProfile) => (
              <PostGrid authorId={authorId} manageable={isOwnProfile} />
            )}
            renderFollowButton={(userId) => <FollowButton userId={userId} />}
            renderFollowCounts={(userId, handle) => <FollowCounts userId={userId} username={handle} />}
          />
        ),
        loader: profileLoader,
      },
      {
        path: "/u/:username/followers",
        element: <FollowListPage mode="followers" />,
        loader: followListLoader,
      },
      {
        path: "/u/:username/following",
        element: <FollowListPage mode="following" />,
        loader: followListLoader,
      },
    ],
  },
]);
