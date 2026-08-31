import { createBrowserRouter } from "react-router";
import { LoginPage, installApiAuth } from "@features/auth";
import {
  EditProfilePage,
  OnboardingPage,
  ProfilePage,
  editProfileLoader,
  profileLoader,
} from "@features/profile";
import { FeedPage } from "@features/feed";
import { AppLayout } from "./AppLayout";
import { RouteError } from "./RouteError";
import { loginLoader, onboardingLoader, rootLoader } from "./guards";

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
      { path: "/u/:username", element: <ProfilePage />, loader: profileLoader },
    ],
  },
]);
