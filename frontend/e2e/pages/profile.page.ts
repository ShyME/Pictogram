import type { Locator, Page } from '@playwright/test';

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
    this.notFoundHeading = page.getByRole('heading', { name: /doesn.t exist/i });
    this.followButton = page.getByRole('button', { name: /^follow$/i });
    this.followingButton = page.getByRole('button', { name: /^following$/i });
    this.editProfileLink = page.getByRole('link', { name: /edit profile/i });
    this.followerCount = page.getByRole('definition').filter({ hasText: /followers/ });
    this.followingCount = page.getByRole('definition').filter({ hasText: /following/ });
    this.followersLink = page.getByRole('link', { name: /followers/ });
    this.followingLink = page.getByRole('link', { name: /following/ });
  }

  async open(username: string): Promise<void> {
    await this.page.goto(`/u/${username}`);
  }

  handle(username: string): Locator {
    // Scoped to the page body: the AppNav also shows the signed-in viewer's own @handle.
    return this.page.getByRole('main').getByText(`@${username}`, { exact: true });
  }

  postByCaption(caption: string): Locator {
    return this.page.getByRole('img', { name: caption });
  }

  postCellByCaption(caption: string): Locator {
    return this.page.getByRole('listitem').filter({ has: this.postByCaption(caption) });
  }

  get confirmDeleteDialog(): Locator {
    return this.page.getByRole('alertdialog', { name: /delete this post/i });
  }

  get postDetail(): Locator {
    return this.page.getByRole('dialog');
  }

  async openPostDetail(caption: string): Promise<void> {
    await this.postCellByCaption(caption).getByRole('button').first().click();
  }

  get emptyGrid(): Locator {
    return this.page.getByText('No posts yet');
  }

  displayName(name: string): Locator {
    return this.page.getByRole('heading', { name });
  }
}
