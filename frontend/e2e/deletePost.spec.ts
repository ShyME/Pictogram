import { expect, test } from '@playwright/test';
import { fileURLToPath } from 'node:url';
import { FeedPage } from './pages/feed.page';
import { LoginPage } from './pages/login.page';
import { NewPostPage } from './pages/newPost.page';
import { OnboardingPage } from './pages/onboarding.page';
import { ProfilePage } from './pages/profile.page';

const PHOTO = fileURLToPath(new URL('fixtures/photo.jpg', import.meta.url));

test('delete a post: confirm the permanent delete and it leaves the grid', async ({ page }) => {
  const login = new LoginPage(page);
  const onboarding = new OnboardingPage(page);
  const feed = new FeedPage(page);
  const profile = new ProfilePage(page);
  const compose = new NewPostPage(page);

  const username = `e2e_del_${Date.now().toString(36)}`;
  const caption = `throwaway ${Date.now().toString(36)}`;

  await login.open();
  await login.signInWithGoogle();
  await expect(page).toHaveURL(/\/onboarding$/);
  await onboarding.completeWith(username, 'Delete Tester');
  await expect(feed.emptyState).toBeVisible();

  await feed.newPostLink.click();
  await compose.selectPhoto(PHOTO);
  await expect(compose.zoom).toBeVisible();
  await compose.frameShot();
  await compose.caption.fill(caption);
  await compose.share.click();

  await expect(page).toHaveURL(new RegExp(`/u/${username}$`));
  await expect(profile.postByCaption(caption)).toBeVisible();

  await profile
    .postCellByCaption(caption)
    .getByRole('button', { name: /delete/i })
    .click();
  await expect(profile.confirmDeleteDialog).toContainText(/can.t be undone/i);
  await profile.confirmDeleteDialog.getByRole('button', { name: /delete/i }).click();

  await expect(profile.confirmDeleteDialog).toBeHidden();
  await expect(profile.postByCaption(caption)).toHaveCount(0);
  await expect(profile.emptyGrid).toBeVisible();

  await page.reload();
  await expect(profile.postByCaption(caption)).toHaveCount(0);
});
