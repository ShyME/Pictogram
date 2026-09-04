import { expect, test } from '@playwright/test';
import { stubApp, stubNeedsOnboarding } from './appWorld';
import { waitForFonts } from './support';

// One committed screenshot baseline per screen retrofitted onto the design system (#136).
// The nav, tokens and primitives are exercised here in situ, not only on `/ui`.

test.describe('retrofitted screens', () => {
  test('onboarding', async ({ page }) => {
    await stubNeedsOnboarding(page);
    await page.goto('/onboarding');
    await expect(page.getByRole('heading', { name: /pick a username/i })).toBeVisible();
    await waitForFonts(page);
    await expect(page).toHaveScreenshot('onboarding.png', { fullPage: true });
  });

  test('edit profile', async ({ page }) => {
    await stubApp(page);
    await page.goto('/settings/profile');
    await expect(page.getByRole('heading', { name: /edit profile/i })).toBeVisible();
    await waitForFonts(page);
    await expect(page).toHaveScreenshot('edit-profile.png', { fullPage: true });
  });

  test('new post', async ({ page }) => {
    await stubApp(page);
    await page.goto('/new');
    await expect(page.getByRole('heading', { name: /new post/i })).toBeVisible();
    await waitForFonts(page);
    await expect(page).toHaveScreenshot('new-post.png', { fullPage: true });
  });

  test('profile and post grid', async ({ page }) => {
    await stubApp(page);
    await page.goto('/u/ansel');
    await expect(page.getByRole('heading', { name: 'Ansel Adams' })).toBeVisible();
    await expect(page.getByRole('listitem').first()).toBeVisible();
    await page.waitForLoadState('networkidle');
    await waitForFonts(page);
    await expect(page).toHaveScreenshot('profile.png', { fullPage: true });
  });

  test('feed and feed card', async ({ page }) => {
    await stubApp(page);
    await page.goto('/');
    await expect(page.getByRole('article').first()).toBeVisible();
    await page.waitForLoadState('networkidle');
    await waitForFonts(page);
    await expect(page).toHaveScreenshot('feed.png', { fullPage: true });
  });

  test('followers list', async ({ page }) => {
    await stubApp(page);
    await page.goto('/u/ansel/followers');
    await expect(page.getByRole('heading', { name: /people who follow @ansel/i })).toBeVisible();
    await expect(page.getByRole('listitem').first()).toBeVisible();
    await page.waitForLoadState('networkidle');
    await waitForFonts(page);
    await expect(page).toHaveScreenshot('followers.png', { fullPage: true });
  });

  test('following list', async ({ page }) => {
    await stubApp(page);
    await page.goto('/u/ansel/following');
    await expect(page.getByRole('heading', { name: /accounts @ansel follows/i })).toBeVisible();
    await expect(page.getByRole('listitem').first()).toBeVisible();
    await page.waitForLoadState('networkidle');
    await waitForFonts(page);
    await expect(page).toHaveScreenshot('following.png', { fullPage: true });
  });
});
