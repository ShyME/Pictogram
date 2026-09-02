import { expect, test } from '@playwright/test';
import { fileURLToPath } from 'node:url';
import { FeedPage } from './pages/feed.page';
import { LoginPage } from './pages/login.page';
import { NewPostPage } from './pages/newPost.page';
import { OnboardingPage } from './pages/onboarding.page';
import { ProfilePage } from './pages/profile.page';

const PHOTO = fileURLToPath(new URL('fixtures/photo.jpg', import.meta.url));

test('publish a post: pick a photo, crop, caption, and see it in the grid', async ({ page }) => {
  const login = new LoginPage(page);
  const onboarding = new OnboardingPage(page);
  const feed = new FeedPage(page);
  const profile = new ProfilePage(page);
  const compose = new NewPostPage(page);

  const username = `e2e_post_${Date.now().toString(36)}`;
  const caption = `first light ${Date.now().toString(36)}`;

  // The cropper positions the image via imperative style-property writes so that CSP
  // style-src can stay 'self'. A style={} prop or setAttribute('style') would trip a
  // violation here (reported as a console error) — see SquareCropper / WebSecurityHeaders.
  const cspViolations: string[] = [];
  page.on('console', (message) => {
    if (message.type() === 'error' && /content security policy/i.test(message.text())) {
      cspViolations.push(message.text());
    }
  });

  await login.open();
  await login.signInWithGoogle();
  await expect(page).toHaveURL(/\/onboarding$/);
  await onboarding.completeWith(username, 'Post Tester');
  await expect(feed.emptyState).toBeVisible();

  await feed.newPostLink.click();
  await expect(page).toHaveURL(/\/new$/);
  await expect(compose.heading).toBeVisible();

  await compose.selectPhoto(PHOTO);
  await expect(compose.zoom).toBeVisible();
  await compose.frameShot();
  expect(cspViolations).toEqual([]);

  await compose.caption.fill(caption);
  await compose.share.click();

  await expect(page).toHaveURL(new RegExp(`/u/${username}$`));
  await expect(profile.postByCaption(caption)).toBeVisible();
  await expect(profile.emptyGrid).toBeHidden();
});
