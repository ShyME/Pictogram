import type { Locator, Page } from "@playwright/test";

// Page object for the public `/u/<username>` profile page (ADR-0007): locators and
// navigation here, assertions in the spec.
export class ProfilePage {
  readonly notFoundHeading: Locator;
  readonly followButton: Locator;
  readonly editProfileButton: Locator;

  private readonly page: Page;

  constructor(page: Page) {
    this.page = page;
    this.notFoundHeading = page.getByRole("heading", { name: /doesn.t exist/i });
    this.followButton = page.getByRole("button", { name: /^follow$/i });
    this.editProfileButton = page.getByRole("button", { name: /edit profile/i });
  }

  async open(username: string): Promise<void> {
    await this.page.goto(`/u/${username}`);
  }

  handle(username: string): Locator {
    return this.page.getByText(`@${username}`, { exact: true });
  }

  displayName(name: string): Locator {
    return this.page.getByRole("heading", { name });
  }
}
