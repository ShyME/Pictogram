import type { Locator, Page } from '@playwright/test';

export class FollowListPage {
  readonly heading: Locator;

  private readonly page: Page;
  // Scoped to the list's own `<main>`: the chat rail (#203) is a sibling `<aside>` that
  // also renders `<li>@handle</li>` rows for everyone the viewer follows, so an unscoped
  // `listitem` locator matches both it and the list.
  private readonly main: Locator;

  constructor(page: Page) {
    this.page = page;
    this.main = page.getByRole('main');
    this.heading = this.main.getByRole('heading', { level: 1 });
  }

  async openFollowers(username: string): Promise<void> {
    await this.page.goto(`/u/${username}/followers`);
  }

  async openFollowing(username: string): Promise<void> {
    await this.page.goto(`/u/${username}/following`);
  }

  row(username: string): Locator {
    return this.main.getByRole('listitem').filter({ has: this.page.getByText(`@${username}`) });
  }

  accountLink(username: string): Locator {
    return this.row(username).getByRole('link').first();
  }

  get emptyState(): Locator {
    return this.main.getByText(/no followers yet|not following anyone yet/i);
  }
}
