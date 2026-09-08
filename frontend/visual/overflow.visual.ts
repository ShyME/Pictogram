import type { Locator, Page } from '@playwright/test';
import {
  expect,
  STRESS_PROFILE_PATH,
  stubApp,
  stubNeedsOnboarding,
  stubStress,
  test,
} from './appWorld';
import { stubSignedOut, waitForFonts } from './support';

// ADR-0013 / #139: responsive is a property of finished layouts. Every screen must render
// without horizontal overflow at a narrow phone width and a wide desktop one — including
// when the content is worst-case (a 20-char username, a long display name, an unbreakable
// token, a long bare URL — see `stubStress`). This is an assertion suite, not a screenshot
// one.

// Page-level overflow: a screen that is wider than its viewport. Catches the flow screens
// (profile, follow lists, forms) where a wide child pushes the document out.
async function expectNoHorizontalOverflow(page: Page): Promise<void> {
  await waitForFonts(page);
  const { clientWidth, scrollWidth } = await page.evaluate(() => ({
    clientWidth: document.documentElement.clientWidth,
    scrollWidth: document.documentElement.scrollWidth,
  }));
  expect(
    scrollWidth,
    `document overflows by ${scrollWidth - clientWidth}px at a ${clientWidth}px viewport`,
  ).toBeLessThanOrEqual(clientWidth + 1);
}

// Container-level overflow: content wider than a specific box. The feed card and the post
// detail modal are `overflow-hidden`, so a caption or name that fails to wrap is clipped
// silently and never grows the document — this is what checks those.
async function expectContentFits(locator: Locator, label: string): Promise<void> {
  const overflowBy = await locator.evaluate((el) => el.scrollWidth - el.clientWidth);
  expect(overflowBy, `${label} content overflows its box by ${overflowBy}px`).toBeLessThanOrEqual(
    1,
  );
}

test.describe('no horizontal overflow with worst-case content', () => {
  test('feed', async ({ page }) => {
    await stubStress(page);
    await page.goto('/');
    await expect(page.getByRole('article').first()).toBeVisible();
    await page.waitForLoadState('networkidle');
    await expectNoHorizontalOverflow(page);
    await expectContentFits(page.getByRole('article').first(), 'feed card');
  });

  test('post detail', async ({ page }) => {
    await stubStress(page);
    await page.goto('/');
    await expect(page.getByRole('article').first()).toBeVisible();
    await page.getByRole('article').first().getByRole('button', { name: 'Open comments' }).click();
    const dialog = page.getByRole('dialog');
    await expect(dialog).toBeVisible();
    await page.waitForLoadState('networkidle');
    await expectNoHorizontalOverflow(page);
    await expectContentFits(dialog, 'post detail modal');
  });

  test('profile and post grid', async ({ page }) => {
    await stubStress(page);
    await page.goto(STRESS_PROFILE_PATH);
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
    await page.waitForLoadState('networkidle');
    await expectNoHorizontalOverflow(page);
  });

  test('chat overlay with a worst-case message', async ({ page }) => {
    // The overlay pins to the bottom of the viewport on every screen (#166); a long
    // unbreakable token or bare URL in a message must wrap inside the transcript, not
    // push the panel — or the page — wide.
    const worstCase = `unbreakable-${'x'.repeat(90)} https://example.com/${'segment/'.repeat(12)}end`;
    await stubApp(page);
    await page.goto('/u/vivian');
    await page.getByRole('button', { name: 'Message' }).click();
    const overlay = page.getByRole('dialog', { name: /chat with/i });
    await expect(overlay).toBeVisible();
    await overlay.getByRole('textbox').fill(worstCase);
    await overlay.getByRole('button', { name: 'Send' }).click();
    await expect(overlay.getByText(/^unbreakable-x+/)).toBeVisible();
    await expectNoHorizontalOverflow(page);
    await expectContentFits(overlay, 'chat overlay');
  });

  test('followers list', async ({ page }) => {
    await stubStress(page);
    await page.goto(`${STRESS_PROFILE_PATH}/followers`);
    await expect(page.getByRole('listitem').first()).toBeVisible();
    await page.waitForLoadState('networkidle');
    await expectNoHorizontalOverflow(page);
  });

  test('following list', async ({ page }) => {
    await stubStress(page);
    await page.goto(`${STRESS_PROFILE_PATH}/following`);
    await expect(page.getByRole('listitem').first()).toBeVisible();
    await page.waitForLoadState('networkidle');
    await expectNoHorizontalOverflow(page);
  });

  test('edit profile', async ({ page }) => {
    await stubStress(page);
    await page.goto('/settings/profile');
    await expect(page.getByRole('heading', { name: /edit profile/i })).toBeVisible();
    await expectNoHorizontalOverflow(page);
  });

  test('new post', async ({ page }) => {
    await stubStress(page);
    await page.goto('/new');
    await expect(page.getByRole('heading', { name: /new post/i })).toBeVisible();
    await expectNoHorizontalOverflow(page);
  });

  test('onboarding', async ({ page }) => {
    await stubNeedsOnboarding(page);
    await page.goto('/onboarding');
    await expect(page.getByRole('heading', { name: /pick a username/i })).toBeVisible();
    await expectNoHorizontalOverflow(page);
  });

  test('login', async ({ page }) => {
    await stubSignedOut(page);
    await page.goto('/login');
    await expect(page.getByRole('heading', { name: /pictogram/i })).toBeVisible();
    await expectNoHorizontalOverflow(page);
  });
});
