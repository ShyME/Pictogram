import type { Locator, Page } from "@playwright/test";

// Page object for the `/u/<username>/{followers,following}` list screens (#57): locators
// and navigation here, assertions in the spec.
export class FollowListPage {
  readonly heading: Locator;

  private readonly page: Page;

  constructor(page: Page) {
    this.page = page;
    this.heading = page.getByRole("heading", { level: 1 });
  }

  async openFollowers(username: string): Promise<void> {
    await this.page.goto(`/u/${username}/followers`);
  }

  async openFollowing(username: string): Promise<void> {
    await this.page.goto(`/u/${username}/following`);
  }

  /** The row for an account, found by its `@handle` link. */
  row(username: string): Locator {
    return this.page.getByRole("listitem").filter({ has: this.page.getByText(`@${username}`) });
  }

  accountLink(username: string): Locator {
    return this.row(username).getByRole("link").first();
  }

  get emptyState(): Locator {
    return this.page.getByText(/no followers yet|not following anyone yet/i);
  }
}
