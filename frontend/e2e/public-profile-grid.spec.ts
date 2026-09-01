import { expect, test } from '@playwright/test';
import { fileURLToPath } from 'node:url';
import { FeedPage } from './pages/feed.page';
import { LoginPage } from './pages/login.page';
import { NewPostPage } from './pages/new-post.page';
import { OnboardingPage } from './pages/onboarding.page';
import { ProfilePage } from './pages/profile.page';

const PHOTO = fileURLToPath(new URL('./fixtures/photo.jpg', import.meta.url));

test("a signed-out visitor sees another user's posts on their profile", async ({ browser }) => {
  const caption = `public grid ${Date.now().toString(36)}`;
  const username = `e2e_pg_${Date.now().toString(36)}`;

  const authorContext = await browser.newContext();
  const visitorContext = await browser.newContext();

  try {
    const authorPage = await authorContext.newPage();
    const login = new LoginPage(authorPage);
    const onboarding = new OnboardingPage(authorPage);
    const feed = new FeedPage(authorPage);
    const compose = new NewPostPage(authorPage);

    await login.open();
    await login.signInWithGoogle();
    await expect(authorPage).toHaveURL(/\/onboarding$/);
    await onboarding.completeWith(username, 'Grid Author');
    await expect(feed.emptyState).toBeVisible();

    await feed.newPostLink.click();
    await compose.selectPhoto(PHOTO);
    await expect(compose.zoom).toBeVisible();
    await compose.frameShot();
    await compose.caption.fill(caption);
    await compose.share.click();
    await expect(authorPage).toHaveURL(new RegExp(`/u/${username}$`));
    await expect(new ProfilePage(authorPage).postByCaption(caption)).toBeVisible();

    const visitorPage = await visitorContext.newPage();
    const visitorView = new ProfilePage(visitorPage);
    await visitorView.open(username);

    await expect(visitorView.displayName('Grid Author')).toBeVisible();
    await expect(visitorView.postByCaption(caption)).toBeVisible();
    await expect(visitorView.emptyGrid).toBeHidden();
    await expect(visitorPage.getByRole('button', { name: 'Delete' })).toBeHidden();
  } finally {
    await authorContext.close();
    await visitorContext.close();
  }
});
