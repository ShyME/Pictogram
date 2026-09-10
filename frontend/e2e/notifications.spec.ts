import type { Page } from '@playwright/test';
import { fileURLToPath } from 'node:url';
import { expect, test } from './fixtures';
import { FeedPage } from './pages/feed.page';
import { LoginPage } from './pages/login.page';
import { NewPostPage } from './pages/newPost.page';
import { OnboardingPage } from './pages/onboarding.page';
import { ProfilePage } from './pages/profile.page';

const PHOTO = fileURLToPath(new URL('fixtures/photo.jpg', import.meta.url));

test('a like turns into a bell badge, a notification row, a click-through, then clears', async ({
  browser,
}) => {
  const suffix = Date.now().toString(36);
  const author = `e2e_ntf_a_${suffix}`;
  const liker = `e2e_ntf_b_${suffix}`;
  const caption = `notify me ${suffix}`;

  const authorContext = await browser.newContext();
  const likerContext = await browser.newContext();

  try {
    const authorPage = await authorContext.newPage();
    await onboard(authorPage, author, 'Notify Author');
    await publish(authorPage, PHOTO, caption);

    const likerPage = await likerContext.newPage();
    await onboard(likerPage, liker, 'Notify Liker');
    const authorProfile = new ProfilePage(likerPage);
    await authorProfile.open(author);
    await authorProfile.followButton.click();
    await expect(authorProfile.followingButton).toBeVisible();

    const likerFeed = new FeedPage(likerPage);
    await likerFeed.open();
    await likerFeed.cardByCaption(caption).getByRole('button', { name: 'Like' }).click();
    await expect(likerFeed.cardByCaption(caption).getByText('1 like')).toBeVisible();

    // The event travels social -> Kafka -> notifications before the author can see it, so
    // reload the notifications screen until the row lands.
    const bellLink = authorPage.getByRole('link', { name: /notifications/i });
    const row = authorPage
      .getByRole('listitem')
      .filter({ hasText: /Notify Liker.*liked your post/i });

    await expect(async () => {
      await authorPage.goto('/');
      await expect(authorPage.getByRole('link', { name: 'Notifications, 1 unread' })).toBeVisible({
        timeout: 3000,
      });
    }).toPass({ timeout: 30_000 });

    await bellLink.click();
    await expect(authorPage).toHaveURL(/\/notifications$/);
    await expect(row).toBeVisible();

    // The whole row links through to the post.
    await row.getByRole('link').click();
    await expect(authorPage).toHaveURL(/\/p\/[0-9a-f-]+$/);
    await expect(authorPage.getByRole('article').getByText(caption)).toBeVisible();

    // Opening the screen marked everything read — the badge is gone.
    await authorPage.goto('/');
    await expect(authorPage.getByRole('link', { name: /\d+ unread/ })).toHaveCount(0);
    await expect(authorPage.getByRole('link', { name: 'Notifications' })).toBeVisible();
  } finally {
    await authorContext.close();
    await likerContext.close();
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
