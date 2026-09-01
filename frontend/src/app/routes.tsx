import { createBrowserRouter } from "react-router";
import { LoginPage, installApiAuth } from "@features/auth";
import { EditProfilePage, OnboardingPage, ProfilePage, profileLoader } from "@features/profile";
import { FeedPage } from "@features/feed";
import { PostGrid } from "@features/post";
import { FollowButton, FollowCounts } from "@features/follow";
import { AppLayout } from "./AppLayout";
import { NewPostRoute } from "./NewPostRoute";
import { RouteError } from "./RouteError";
import { editProfileLoader, loginLoader, newPostLoader, onboardingLoader, rootLoader } from "./guards";

// Register the access-token / silent-refresh middleware before any loader runs.
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
            renderFollowCounts={(userId) => <FollowCounts userId={userId} />}
          />
        ),
        loader: profileLoader,
      },
    ],
  },
]);
