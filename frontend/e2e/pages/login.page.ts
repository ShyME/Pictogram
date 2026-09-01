import type { Locator, Page } from '@playwright/test';

export class LoginPage {
  readonly heading: Locator;
  readonly continueWithGoogle: Locator;
  readonly errorAlert: Locator;

  private readonly page: Page;

  constructor(page: Page) {
    this.page = page;
    this.heading = page.getByRole('heading', { name: 'Pictogram' });
    this.continueWithGoogle = page.getByRole('link', { name: /continue with google/i });
    this.errorAlert = page.getByRole('alert');
  }

  async open(): Promise<void> {
    await this.page.goto('/');
  }

  async openWithError(reason: string): Promise<void> {
    await this.page.goto(`/login?error=${reason}`);
  }

  async signInWithGoogle(): Promise<void> {
    await this.continueWithGoogle.click();

    const googleSubject = `e2e-${Date.now().toString(36)}`;
    const username = this.page.locator('input[name="username"]');
    await username.waitFor({ state: 'visible' });
    await username.fill(googleSubject);
    await this.page.locator('input[type="submit"]').click();
  }
}
