import { expect, test } from "@playwright/test";
import { LoginPage } from "./pages/login.page";
import { OnboardingPage } from "./pages/onboarding.page";
import { FeedPage } from "./pages/feed.page";
import { ProfilePage } from "./pages/profile.page";

// The ticket's broad journey (#11), against `task up` (compose.yaml +
// compose.mock-oauth.yaml): a signed-in user opens a public profile by username and sees
// the details; a username nobody holds shows a clear not-found page.
test("public profile page: an existing user renders, a missing one shows not-found", async ({ page }) => {
  const login = new LoginPage(page);
  const onboarding = new OnboardingPage(page);
  const feed = new FeedPage(page);
  const profile = new ProfilePage(page);

  const username = `e2e_p_${Date.now().toString(36)}`;

  await login.open();
  await login.signInWithGoogle();
  await expect(page).toHaveURL(/\/onboarding$/);
  await onboarding.completeWith(username, "E2E Profile Tester");
  await expect(feed.emptyState).toBeVisible();

  // Reach your own profile the way a user would — the handle in the app header.
  await feed.myProfileLink(username).click();
  await expect(page).toHaveURL(new RegExp(`/u/${username}$`));
  await expect(profile.displayName("E2E Profile Tester")).toBeVisible();
  await expect(profile.handle(username)).toBeVisible();
  await expect(profile.editProfileButton).toBeVisible();

  await profile.open(`ghost_${Date.now().toString(36)}`);
  await expect(profile.notFoundHeading).toBeVisible();
});
