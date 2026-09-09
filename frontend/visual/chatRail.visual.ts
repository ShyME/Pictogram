import type { Page, TestInfo } from '@playwright/test';
import { expect, stubApp, stubNoFollows, test } from './appWorld';
import { waitForFonts } from './support';

// The chat sidebar (#203): a docked rail beside the feed on a wide viewport, a drawer
// behind the `AppNav` chat icon on a narrow one. Baselines cover the states the ticket
// calls out — expanded, collapsed, filtered, empty, and the narrow drawer.

const MD = 768;
const viewportWidth = (testInfo: TestInfo) => testInfo.project.use.viewport?.width ?? 0;

const rail = (page: Page) => page.getByRole('complementary', { name: 'Messages' });

test.describe('docked rail (wide viewport)', () => {
  test('expanded', async ({ page }, testInfo) => {
    test.skip(viewportWidth(testInfo) < MD, 'the docked rail only exists at md and wider');
    await stubApp(page);
    await page.goto('/');
    await expect(rail(page).getByText('Vivian Maier')).toBeVisible();
    await page.waitForLoadState('networkidle');
    await waitForFonts(page);
    await expect(rail(page)).toHaveScreenshot('rail-expanded.png');
  });

  test('filtered', async ({ page }, testInfo) => {
    test.skip(viewportWidth(testInfo) < MD, 'the docked rail only exists at md and wider');
    await stubApp(page);
    await page.goto('/');
    await expect(rail(page).getByText('Vivian Maier')).toBeVisible();
    await rail(page)
      .getByRole('searchbox', { name: /filter/i })
      .fill('viv');
    await expect(rail(page).getByText('Dorothea Lange')).toBeHidden();
    await waitForFonts(page);
    await expect(rail(page)).toHaveScreenshot('rail-filtered.png');
  });

  test('empty', async ({ page }, testInfo) => {
    test.skip(viewportWidth(testInfo) < MD, 'the docked rail only exists at md and wider');
    await stubNoFollows(page);
    await page.goto('/');
    await expect(rail(page).getByText(/no one to message yet/i)).toBeVisible();
    await waitForFonts(page);
    await expect(rail(page)).toHaveScreenshot('rail-empty.png');
  });

  test('collapsed', async ({ page }, testInfo) => {
    test.skip(viewportWidth(testInfo) < MD, 'the docked rail only exists at md and wider');
    await page.addInitScript(() => {
      localStorage.setItem('pictogram.chat-rail.collapsed', '1');
    });
    await stubApp(page);
    await page.goto('/');
    await expect(page.getByRole('button', { name: /expand messages/i })).toBeVisible();
    await waitForFonts(page);
    await expect(rail(page)).toHaveScreenshot('rail-collapsed.png');
  });
});

test.describe('drawer (narrow viewport)', () => {
  test('open', async ({ page }, testInfo) => {
    test.skip(viewportWidth(testInfo) >= MD, 'the drawer only exists below the md breakpoint');
    await stubApp(page);
    await page.goto('/');
    await page.getByRole('button', { name: /open messages/i }).click();
    await expect(rail(page).getByText('Vivian Maier')).toBeVisible();
    await page.waitForLoadState('networkidle');
    await waitForFonts(page);
    await expect(page).toHaveScreenshot('rail-drawer-open.png');
  });
});
