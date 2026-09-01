import type { Locator, Page } from '@playwright/test';

export class OnboardingPage {
  readonly heading: Locator;
  readonly username: Locator;
  readonly displayName: Locator;
  readonly createProfile: Locator;
  readonly usernameError: Locator;

  constructor(page: Page) {
    this.heading = page.getByRole('heading', { name: /pick a username/i });
    this.username = page.getByLabel('Username');
    this.displayName = page.getByLabel(/display name/i);
    this.createProfile = page.getByRole('button', { name: /create profile/i });
    this.usernameError = page.getByRole('alert');
  }

  async completeWith(username: string, displayName?: string): Promise<void> {
    await this.username.fill(username);
    if (displayName) await this.displayName.fill(displayName);
    await this.createProfile.click();
  }
}
