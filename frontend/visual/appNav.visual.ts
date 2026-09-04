import { expect, test } from '@playwright/test';
import { waitForFonts } from './support';

const MD = 768;

test.beforeEach(async ({ page }) => {
  await page.goto('/ui');
  await expect(page.getByRole('heading', { name: 'shared/ui showcase' })).toBeVisible();
  await waitForFonts(page);
});

test('app nav', async ({ page }) => {
  const nav = page.locator('header').filter({ has: page.getByRole('link', { name: 'Pictogram' }) });
  await expect(nav).toHaveScreenshot('app-nav.png');
});

test('app nav — avatar menu open', async ({ page }, testInfo) => {
  test.skip(
    (testInfo.project.use.viewport?.width ?? 0) >= MD,
    'the avatar menu only exists below the md breakpoint',
  );

  await page.getByRole('button', { name: 'Open menu' }).click();
  const menu = page.getByRole('menu');
  await expect(menu).toBeVisible();
  await expect(menu).toHaveScreenshot('app-nav-menu.png');
});
