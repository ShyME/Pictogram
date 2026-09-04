import type { Page } from '@playwright/test';
import { fileURLToPath } from 'node:url';
import { expect, test } from './fixtures';
import { FeedPage } from './pages/feed.page';
import { LoginPage } from './pages/login.page';
import { NewPostPage } from './pages/newPost.page';
import { OnboardingPage } from './pages/onboarding.page';
import { ProfilePage } from './pages/profile.page';

const PHOTO = fileURLToPath(new URL('fixtures/photo.jpg', import.meta.url));

test('a viewer comments on a post from the feed and both people see the thread', async ({
  browser,
}) => {
  const suffix = Date.now().toString(36);
  const viewer = `e2e_cmt_v_${suffix}`;
  const author = `e2e_cmt_a_${suffix}`;
  const caption = `talk to me ${suffix}`;

  const viewerContext = await browser.newContext();
  const authorContext = await browser.newContext();

  try {
    const authorPage = await authorContext.newPage();
    await onboard(authorPage, author, 'Comment Author');
    await publish(authorPage, PHOTO, caption);

    const viewerPage = await viewerContext.newPage();
    await onboard(viewerPage, viewer, 'Comment Viewer');

    const authorProfile = new ProfilePage(viewerPage);
    await authorProfile.open(author);
    await authorProfile.followButton.click();
    await expect(authorProfile.followingButton).toBeVisible();

    const feed = new FeedPage(viewerPage);
    await feed.open();
    await feed.openComments(caption);

    const thread = feed.postDetail;
    await expect(thread.getByText(/no comments yet/i)).toBeVisible();

    await thread.getByLabel('Add a comment').fill('first! check https://pictogram.dev');
    await thread.getByRole('button', { name: 'Post' }).click();

    await expect(thread.getByText('first! check')).toBeVisible();
    await expect(thread.getByRole('link', { name: 'https://pictogram.dev' })).toBeVisible();

    // The viewer sees a delete control on their own comment.
    await expect(thread.getByRole('button', { name: 'Delete' })).toBeVisible();

    // The comment survives a reload...
    await viewerPage.reload();
    await feed.openComments(caption);
    await expect(feed.postDetail.getByText('first! check')).toBeVisible();

    // ...and the author sees it on their own post detail.
    const ownProfile = new ProfilePage(authorPage);
    await ownProfile.open(author);
    await ownProfile.openPostDetail(caption);
    await expect(ownProfile.postDetail.getByText('first! check')).toBeVisible();

    // The feed card shows the comment count.
    await feed.open();
    await expect(feed.cardByCaption(caption).getByText('1 comment')).toBeVisible();

    // The post's author can remove a visitor's comment, and the count falls back.
    await ownProfile.postDetail.getByRole('button', { name: 'Delete' }).click();
    await expect(ownProfile.postDetail.getByText('first! check')).toBeHidden();

    await feed.open();
    await expect(feed.cardByCaption(caption).getByText('0 comments')).toBeVisible();
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
