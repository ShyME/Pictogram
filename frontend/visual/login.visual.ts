import { expect, test } from '@playwright/test';
import { stubSignedOut, waitForFonts } from './support';

test.beforeEach(async ({ page }) => {
  await stubSignedOut(page);
});

test('login screen', async ({ page }) => {
  await page.goto('/login');
  await expect(page.getByRole('heading', { name: 'Pictogram' })).toBeVisible();
  await waitForFonts(page);
  await expect(page).toHaveScreenshot('login.png', { fullPage: true });
});

test('login screen with a sign-in error', async ({ page }) => {
  await page.goto('/login?error=sign-in-failed');
  await expect(page.getByRole('alert')).toBeVisible();
  await waitForFonts(page);
  await expect(page).toHaveScreenshot('login-error.png', { fullPage: true });
});
