import { expect, test, type Page } from '@playwright/test';
import { fileURLToPath } from 'node:url';
import { FeedPage } from './pages/feed.page';
import { LoginPage } from './pages/login.page';
import { NewPostPage } from './pages/newPost.page';
import { OnboardingPage } from './pages/onboarding.page';
import { ProfilePage } from './pages/profile.page';

const PHOTO = fileURLToPath(new URL('fixtures/photo.jpg', import.meta.url));

test('like and unlike a post from the feed, and the count moves', async ({ browser }) => {
  const suffix = Date.now().toString(36);
  const viewer = `e2e_like_v_${suffix}`;
  const author = `e2e_like_a_${suffix}`;
  const caption = `worth a like ${suffix}`;

  const viewerContext = await browser.newContext();
  const authorContext = await browser.newContext();

  try {
    const authorPage = await authorContext.newPage();
    await onboard(authorPage, author, 'Like Author');
    await publish(authorPage, PHOTO, caption);

    const viewerPage = await viewerContext.newPage();
    await onboard(viewerPage, viewer, 'Like Viewer');

    const authorProfile = new ProfilePage(viewerPage);
    await authorProfile.open(author);
    await authorProfile.followButton.click();
    await expect(authorProfile.followingButton).toBeVisible();

    const feed = new FeedPage(viewerPage);
    await feed.open();

    const card = feed.cardByCaption(caption);
    await expect(card.getByText('0 likes')).toBeVisible();

    await card.getByRole('button', { name: 'Like' }).click();
    await expect(card.getByText('1 like')).toBeVisible();
    await expect(card.getByRole('button', { name: 'Unlike' })).toBeVisible();

    await viewerPage.reload();
    await expect(card.getByText('1 like')).toBeVisible();
    await expect(card.getByRole('button', { name: 'Unlike' })).toBeVisible();

    // The same like state shows on the author's profile post detail.
    await authorProfile.open(author);
    await authorProfile.openPostDetail(caption);
    await expect(authorProfile.postDetail.getByText('1 like')).toBeVisible();
    await expect(authorProfile.postDetail.getByRole('button', { name: 'Unlike' })).toBeVisible();
    await authorProfile.postDetail.getByRole('button', { name: 'Close' }).click();

    await feed.open();
    await card.getByRole('button', { name: 'Unlike' }).click();
    await expect(card.getByText('0 likes')).toBeVisible();
    await expect(card.getByRole('button', { name: 'Like' })).toBeVisible();
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
