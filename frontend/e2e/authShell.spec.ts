import { expect, test } from './fixtures';
import { FeedPage } from './pages/feed.page';
import { LoginPage } from './pages/login.page';
import { OnboardingPage } from './pages/onboarding.page';

test('new user: sign in with Google, onboard, land on the empty feed', async ({ page }) => {
  const login = new LoginPage(page);
  const onboarding = new OnboardingPage(page);
  const feed = new FeedPage(page);

  await login.open();
  await expect(page).toHaveURL(/\/login$/);
  await expect(login.continueWithGoogle).toBeVisible();

  await login.signInWithGoogle();

  await expect(page).toHaveURL(/\/onboarding$/);
  await expect(onboarding.heading).toBeVisible();

  // The refresh that ran on the post-sign-in landing had to clear the double-submit CSRF gate
  // on /api/auth/** (#125); the SPA can only have done that with a readable XSRF-TOKEN cookie.
  const cookies = await page.context().cookies();
  expect(cookies.map((cookie) => cookie.name)).toContain('XSRF-TOKEN');

  await onboarding.completeWith(`e2e_${Date.now().toString(36)}`, 'E2E Tester');

  await expect(feed.emptyState).toBeVisible();

  await feed.signOut();
  await expect(page).toHaveURL(/\/login$/);
  await expect(login.continueWithGoogle).toBeVisible();
});
