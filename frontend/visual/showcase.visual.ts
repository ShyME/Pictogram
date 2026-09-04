import { expect, test } from '@playwright/test';
import { waitForFonts } from './support';

test.beforeEach(async ({ page }) => {
  await page.goto('/ui');
  await expect(page.getByRole('heading', { name: 'shared/ui showcase' })).toBeVisible();
  await waitForFonts(page);
});

test('all primitives', async ({ page }) => {
  await expect(page).toHaveScreenshot('showcase.png', { fullPage: true });
});

test('dialog — open', async ({ page }) => {
  await page.getByRole('button', { name: 'Delete post…' }).click();
  await expect(page.getByRole('dialog')).toBeVisible();
  await expect(page).toHaveScreenshot('dialog.png');
});

test('dropdown menu — open', async ({ page }) => {
  await page.getByRole('button', { name: 'Account' }).click();
  const menu = page.getByRole('menu');
  await expect(menu).toBeVisible();
  await expect(menu).toHaveScreenshot('dropdown-menu.png');
});
