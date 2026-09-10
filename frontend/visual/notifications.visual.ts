import type { Page } from '@playwright/test';
import { expect, stubApp, test } from './appWorld';
import { waitForFonts } from './support';

// The notifications bell + badge in `AppNav` and the `/notifications` screen (#199),
// driven by the fixed world in `appWorld.ts`.

async function stubUnreadCount(page: Page, count: number): Promise<void> {
  await page.route('**/api/notifications/unread-count', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ count }),
    }),
  );
}

const nav = (page: Page) =>
  page.locator('header').filter({ has: page.getByRole('link', { name: 'Pictogram' }) });

test.describe('notifications bell', () => {
  for (const [name, count] of [
    ['zero', 0],
    ['some', 3],
    ['capped', 12],
  ] as const) {
    test(`badge — ${name}`, async ({ page }) => {
      await stubApp(page);
      await stubUnreadCount(page, count);
      await page.goto('/');
      await expect(page.getByRole('article').first()).toBeVisible();
      await expect(page.getByRole('link', { name: /notifications/i })).toBeVisible();
      await waitForFonts(page);
      await expect(nav(page)).toHaveScreenshot(`bell-${name}.png`);
    });
  }
});

test('notifications screen', async ({ page }) => {
  await stubApp(page);
  await page.goto('/notifications');
  await expect(page.getByRole('heading', { name: 'Notifications' })).toBeVisible();
  await expect(page.getByRole('listitem').first()).toBeVisible();
  await page.waitForLoadState('networkidle');
  await waitForFonts(page);
  await expect(page).toHaveScreenshot('notifications.png', { fullPage: true });
});
