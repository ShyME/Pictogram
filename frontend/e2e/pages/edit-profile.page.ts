import type { Locator, Page } from "@playwright/test";

export class EditProfilePage {
  readonly heading: Locator;
  readonly username: Locator;
  readonly displayName: Locator;
  readonly bio: Locator;
  readonly saveChanges: Locator;
  readonly renameWarning: Locator;

  private readonly page: Page;

  constructor(page: Page) {
    this.page = page;
    this.heading = page.getByRole("heading", { name: /edit profile/i });
    this.username = page.getByLabel("Username");
    this.displayName = page.getByLabel(/display name/i);
    this.bio = page.getByLabel(/bio/i);
    this.saveChanges = page.getByRole("button", { name: /save changes/i });
    this.renameWarning = page.getByRole("status");
  }

  async open(): Promise<void> {
    await this.page.goto("/settings/profile");
  }
}
