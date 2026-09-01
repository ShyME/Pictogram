import { expect, test } from "@playwright/test";
import { FeedPage } from "./pages/feed.page";
import { LoginPage } from "./pages/login.page";
import { OnboardingPage } from "./pages/onboarding.page";

test("new user: sign in with Google, onboard, land on the empty feed", async ({ page }) => {
  const login = new LoginPage(page);
  const onboarding = new OnboardingPage(page);
  const feed = new FeedPage(page);

  await login.open();
  await expect(page).toHaveURL(/\/login$/);
  await expect(login.continueWithGoogle).toBeVisible();

  await login.signInWithGoogle();

  await expect(page).toHaveURL(/\/onboarding$/);
  await expect(onboarding.heading).toBeVisible();

  await onboarding.completeWith(`e2e_${Date.now().toString(36)}`, "E2E Tester");

  await expect(feed.emptyState).toBeVisible();

  await feed.signOut();
  await expect(page).toHaveURL(/\/login$/);
  await expect(login.continueWithGoogle).toBeVisible();
});
