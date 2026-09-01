import type { Locator, Page } from "@playwright/test";

// Page object for the public `/u/<username>` profile page (ADR-0007): locators and
// navigation here, assertions in the spec.
export class ProfilePage {
  readonly notFoundHeading: Locator;
  readonly followButton: Locator;
  readonly followingButton: Locator;
  readonly editProfileLink: Locator;
  readonly followerCount: Locator;
  readonly followingCount: Locator;
  readonly followersLink: Locator;
  readonly followingLink: Locator;

  private readonly page: Page;

  constructor(page: Page) {
    this.page = page;
    this.notFoundHeading = page.getByRole("heading", { name: /doesn.t exist/i });
    this.followButton = page.getByRole("button", { name: /^follow$/i });
    this.followingButton = page.getByRole("button", { name: /^following$/i });
    this.editProfileLink = page.getByRole("link", { name: /edit profile/i });
    this.followerCount = page.getByRole("definition").filter({ hasText: /followers/ });
    this.followingCount = page.getByRole("definition").filter({ hasText: /following/ });
    this.followersLink = page.getByRole("link", { name: /followers/ });
    this.followingLink = page.getByRole("link", { name: /following/ });
  }

  async open(username: string): Promise<void> {
    await this.page.goto(`/u/${username}`);
  }

  handle(username: string): Locator {
    return this.page.getByText(`@${username}`, { exact: true });
  }

  /** A post in the grid, found by its caption (its image's alt text). */
  postByCaption(caption: string): Locator {
    return this.page.getByRole("img", { name: caption });
  }

  /** The grid cell for a post, found by its caption — the delete control lives here. */
  postCellByCaption(caption: string): Locator {
    return this.page.getByRole("listitem").filter({ has: this.postByCaption(caption) });
  }

  get confirmDeleteDialog(): Locator {
    return this.page.getByRole("alertdialog", { name: /delete this post/i });
  }

  get emptyGrid(): Locator {
    return this.page.getByText("No posts yet");
  }

  displayName(name: string): Locator {
    return this.page.getByRole("heading", { name });
  }
}
