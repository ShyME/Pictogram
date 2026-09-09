import type { Locator, Page } from '@playwright/test';

export class ChatRailPage {
  private readonly page: Page;
  readonly rail: Locator;

  constructor(page: Page) {
    this.page = page;
    this.rail = page.getByRole('complementary', { name: 'Messages' });
  }

  row(name: string): Locator {
    return this.rail.getByRole('button', { name: new RegExp(name) });
  }

  presenceDot(state: 'Online' | 'Offline'): Locator {
    return this.rail.getByRole('img', { name: state });
  }

  filter(): Locator {
    return this.rail.getByRole('searchbox', { name: /filter/i });
  }

  overlay(peerName: string): Locator {
    return this.page.getByRole('dialog', { name: `Chat with ${peerName}` });
  }
}
