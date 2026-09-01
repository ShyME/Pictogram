import type { Locator, Page } from "@playwright/test";

// Page object for the public `/u/<username>` profile page (ADR-0007): locators and
// navigation here, assertions in the spec.
export class ProfilePage {
  readonly notFoundHeading: Locator;
  readonly followButton: Locator;
  readonly editProfileLink: Locator;

  private readonly page: Page;

  constructor(page: Page) {
    this.page = page;
    this.notFoundHeading = page.getByRole("heading", { name: /doesn.t exist/i });
    this.followButton = page.getByRole("button", { name: /^follow$/i });
    this.editProfileLink = page.getByRole("link", { name: /edit profile/i });
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
