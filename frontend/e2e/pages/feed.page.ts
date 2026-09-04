import type { Locator, Page } from '@playwright/test';

export class FeedPage {
  readonly emptyState: Locator;
  readonly signOutButton: Locator;
  readonly newPostLink: Locator;
  readonly cards: Locator;

  private readonly page: Page;

  constructor(page: Page) {
    this.page = page;
    this.emptyState = page.getByText(/find people to follow/i);
    this.signOutButton = page.getByRole('button', { name: /sign out/i });
    this.newPostLink = page.getByRole('link', { name: /new post/i });
    this.cards = page.getByRole('article');
  }

  async open(): Promise<void> {
    await this.page.goto('/');
  }

  cardByCaption(caption: string): Locator {
    return this.cards.filter({ hasText: caption });
  }

  get postDetail(): Locator {
    return this.page.getByRole('dialog');
  }

  async openComments(caption: string): Promise<void> {
    await this.cardByCaption(caption).getByRole('button', { name: 'Open comments' }).click();
  }

  myProfileLink(username: string): Locator {
    return this.page.getByRole('link', { name: `@${username}` });
  }

  async signOut(): Promise<void> {
    await this.signOutButton.click();
  }
}
