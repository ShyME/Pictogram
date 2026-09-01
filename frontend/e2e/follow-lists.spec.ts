import { expect, test, type Page } from '@playwright/test';
import { FeedPage } from './pages/feed.page';
import { FollowListPage } from './pages/follow-list.page';
import { LoginPage } from './pages/login.page';
import { OnboardingPage } from './pages/onboarding.page';
import { ProfilePage } from './pages/profile.page';

test("browse a profile's following and followers lists and click through to an account", async ({
  browser,
}) => {
  const suffix = Date.now().toString(36);
  const alice = `e2e_fll_a_${suffix}`;
  const bob = `e2e_fll_b_${suffix}`;

  const aliceContext = await browser.newContext();
  const bobContext = await browser.newContext();

  try {
    const alicePage = await aliceContext.newPage();
    await onboard(alicePage, alice, 'Alice List');

    const bobPage = await bobContext.newPage();
    await onboard(bobPage, bob, 'Bob List');

    const aliceOnBob = new ProfilePage(alicePage);
    await aliceOnBob.open(bob);
    await aliceOnBob.followButton.click();
    await expect(aliceOnBob.followingButton).toBeVisible();

    await new ProfilePage(alicePage).open(alice);
    await aliceOnBob.followingLink.click();
    await expect(alicePage).toHaveURL(new RegExp(`/u/${alice}/following$`));

    const aliceFollowing = new FollowListPage(alicePage);
    await expect(aliceFollowing.heading).toHaveText(new RegExp(`follows`, 'i'));
    await expect(aliceFollowing.row(bob)).toBeVisible();
    await aliceFollowing.accountLink(bob).click();
    await expect(alicePage).toHaveURL(new RegExp(`/u/${bob}$`));

    const bobOnSelf = new ProfilePage(bobPage);
    await bobOnSelf.open(bob);
    await bobOnSelf.followersLink.click();
    await expect(bobPage).toHaveURL(new RegExp(`/u/${bob}/followers$`));
    await expect(new FollowListPage(bobPage).row(alice)).toBeVisible();
  } finally {
    await aliceContext.close();
    await bobContext.close();
  }
});

test('a signed-out visitor who opens a list screen is sent to sign in', async ({ page }) => {
  await page.goto('/u/nobody_here/followers');
  await expect(page).toHaveURL(/\/login$/);
});

async function onboard(page: Page, username: string, displayName: string): Promise<void> {
  const login = new LoginPage(page);
  const onboarding = new OnboardingPage(page);
  const feed = new FeedPage(page);

  await login.open();
  await login.signInWithGoogle();
  await expect(page).toHaveURL(/\/onboarding$/);
  await onboarding.completeWith(username, displayName);
  await expect(feed.emptyState).toBeVisible();
}
