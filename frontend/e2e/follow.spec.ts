import { expect, test, type Page } from "@playwright/test";
import { LoginPage } from "./pages/login.page";
import { OnboardingPage } from "./pages/onboarding.page";
import { FeedPage } from "./pages/feed.page";
import { ProfilePage } from "./pages/profile.page";

// The ticket's broad journey (#17), against `task up` (compose.yaml +
// compose.mock-oauth.yaml): one user opens another user's profile, follows them from the
// page, and the follower count moves; unfollowing moves it back. Two isolated browser
// contexts stand in for two people — the shared mock provider has a single interactive
// login, so they sign in one after the other, not concurrently.
test("follow and unfollow another user from their profile, and the counts move", async ({
  browser,
}) => {
  const suffix = Date.now().toString(36);
  const alice = `e2e_flw_a_${suffix}`;
  const bob = `e2e_flw_b_${suffix}`;

  const aliceContext = await browser.newContext();
  const bobContext = await browser.newContext();

  try {
    const alicePage = await aliceContext.newPage();
    await onboard(alicePage, alice, "Alice Follow");

    const bobPage = await bobContext.newPage();
    await onboard(bobPage, bob, "Bob Follow");

    const bobViewOfAlice = new ProfilePage(bobPage);
    await bobViewOfAlice.open(alice);
    await expect(bobViewOfAlice.followButton).toBeVisible();
    await expect(bobViewOfAlice.followerCount).toContainText("0");

    await bobViewOfAlice.followButton.click();
    await expect(bobViewOfAlice.followingButton).toBeVisible();
    await expect(bobViewOfAlice.followerCount).toContainText("1");

    // The follow sticks across a reload.
    await bobPage.reload();
    await expect(bobViewOfAlice.followingButton).toBeVisible();
    await expect(bobViewOfAlice.followerCount).toContainText("1");

    // Alice sees the follower on her own profile — and no follow button there.
    const aliceViewOfSelf = new ProfilePage(alicePage);
    await aliceViewOfSelf.open(alice);
    await expect(aliceViewOfSelf.followerCount).toContainText("1");
    await expect(aliceViewOfSelf.editProfileLink).toBeVisible();
    await expect(aliceViewOfSelf.followButton).toBeHidden();

    // Unfollowing puts it back.
    await bobViewOfAlice.followingButton.click();
    await expect(bobViewOfAlice.followButton).toBeVisible();
    await expect(bobViewOfAlice.followerCount).toContainText("0");
  } finally {
    await aliceContext.close();
    await bobContext.close();
  }
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
