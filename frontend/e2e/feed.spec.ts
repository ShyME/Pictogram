import type { Page } from '@playwright/test';
import { fileURLToPath } from 'node:url';
import { expect, test } from './fixtures';
import { FeedPage } from './pages/feed.page';
import { LoginPage } from './pages/login.page';
import { NewPostPage } from './pages/newPost.page';
import { OnboardingPage } from './pages/onboarding.page';
import { ProfilePage } from './pages/profile.page';

const PHOTO = fileURLToPath(new URL('fixtures/photo.jpg', import.meta.url));

test('the home feed shows the posts of people you follow, newest first', async ({ browser }) => {
  const suffix = Date.now().toString(36);
  const viewer = `e2e_feed_v_${suffix}`;
  const author = `e2e_feed_a_${suffix}`;
  const older = `sunrise ${suffix}`;
  const newer = `sunset ${suffix}`;

  const viewerContext = await browser.newContext();
  const authorContext = await browser.newContext();

  try {
    const authorPage = await authorContext.newPage();
    await onboard(authorPage, author, 'Feed Author');
    await publish(authorPage, PHOTO, older);
    await publish(authorPage, PHOTO, newer);

    const viewerPage = await viewerContext.newPage();
    await onboard(viewerPage, viewer, 'Feed Viewer');

    const authorProfile = new ProfilePage(viewerPage);
    await authorProfile.open(author);
    await authorProfile.followButton.click();
    await expect(authorProfile.followingButton).toBeVisible();

    const feed = new FeedPage(viewerPage);
    await feed.open();

    await expect(feed.cards).toHaveCount(2);
    await expect(feed.cards.first()).toContainText(newer);
    await expect(feed.cards.last()).toContainText(older);
    await expect(feed.cards.first().getByRole('img')).toBeVisible();
    await expect(feed.cards.first().getByRole('link', { name: /Feed Author/ })).toBeVisible();

    await feed.cards
      .first()
      .getByRole('link', { name: /Feed Author/ })
      .click();
    await expect(viewerPage).toHaveURL(new RegExp(`/u/${author}$`));
  } finally {
    await viewerContext.close();
    await authorContext.close();
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

async function publish(page: Page, photoPath: string, caption: string): Promise<void> {
  const feed = new FeedPage(page);
  const compose = new NewPostPage(page);
  const profile = new ProfilePage(page);

  // The "New post" link lives in the feed layout header; a fresh publish may start
  // from the author's profile grid, which has no such link.
  await feed.open();
  await feed.newPostLink.click();
  await expect(compose.heading).toBeVisible();
  await compose.selectPhoto(photoPath);
  await expect(compose.zoom).toBeVisible();
  await compose.frameShot();
  await compose.caption.fill(caption);
  await compose.share.click();
  await expect(profile.postByCaption(caption)).toBeVisible();
}
