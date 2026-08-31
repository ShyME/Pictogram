import type { Locator, Page } from "@playwright/test";

export class FeedPage {
  readonly emptyState: Locator;
  readonly signOutButton: Locator;

  constructor(page: Page) {
    this.emptyState = page.getByText(/find people to follow/i);
    this.signOutButton = page.getByRole("button", { name: /sign out/i });
  }

  async signOut(): Promise<void> {
    await this.signOutButton.click();
  }
}
