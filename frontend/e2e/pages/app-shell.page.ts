import type { Locator, Page } from "@playwright/test";

// Page object for the app shell — the pattern every feature ticket's journey copies
// (ADR-0007): locators and actions live here, assertions live in the spec.
export class AppShellPage {
  readonly heading: Locator;

  private readonly page: Page;

  constructor(page: Page) {
    this.page = page;
    this.heading = page.getByRole("heading", { name: /pictogram/i });
  }

  async open(): Promise<void> {
    await this.page.goto("/");
  }
}
