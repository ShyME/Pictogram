import type { Page } from '@playwright/test';

// The visual server has no backend. Stub the session probe so the guarded screens
// (`/login`) resolve to their signed-out state instead of the route error boundary.
export async function stubSignedOut(page: Page): Promise<void> {
  await page.route('**/api/profiles/me', (route) =>
    route.fulfill({
      status: 401,
      contentType: 'application/problem+json',
      body: JSON.stringify({ type: 'about:blank', title: 'Unauthorized', status: 401 }),
    }),
  );
  await page.route('**/api/auth/refresh', (route) => route.fulfill({ status: 401, body: '' }));
}

// Wait for webfonts so the first paint after navigation isn't the fallback face.
export async function waitForFonts(page: Page): Promise<void> {
  await page.evaluate(() => document.fonts.ready);
}
