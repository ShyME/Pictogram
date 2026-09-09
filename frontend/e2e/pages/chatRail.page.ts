import type { Locator, Page } from '@playwright/test';

export class ChatRailPage {
  private readonly page: Page;
  readonly rail: Locator;

  constructor(page: Page) {
    this.page = page;
    this.rail = page.getByRole('complementary', { name: 'Messages' });
  }

  // Narrow viewport only: the rail is a drawer behind this `AppNav` icon.
  get drawerTrigger(): Locator {
    return this.page.getByRole('button', { name: /open messages/i });
  }

  // The unread badge on the narrow-viewport `AppNav` icon.
  get navUnreadBadge(): Locator {
    return this.page.locator('header').getByRole('img', { name: /unread/i });
  }

  async openDrawer(): Promise<void> {
    await this.drawerTrigger.click();
    await this.rail.waitFor();
  }

  row(name: string): Locator {
    return this.rail.getByRole('button', { name: new RegExp(name) });
  }

  presenceDot(state: 'Online' | 'Offline'): Locator {
    return this.rail.getByRole('img', { name: state });
  }

  unreadMarker(name: string): Locator {
    return this.row(name).getByRole('img', { name: /unread/i });
  }

  filter(): Locator {
    return this.rail.getByRole('searchbox', { name: /filter/i });
  }

  overlay(peerName: string): Locator {
    return this.page.getByRole('dialog', { name: `Chat with ${peerName}` });
  }
}
