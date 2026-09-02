import type { Locator, Page } from '@playwright/test';

export class NewPostPage {
  readonly heading: Locator;
  readonly caption: Locator;
  readonly share: Locator;
  readonly zoom: Locator;

  private readonly page: Page;

  constructor(page: Page) {
    this.page = page;
    this.heading = page.getByRole('heading', { name: /new post/i });
    this.caption = page.getByLabel(/caption/i);
    this.share = page.getByRole('button', { name: /share/i });
    this.zoom = page.getByRole('slider', { name: /zoom/i });
  }

  async selectPhoto(filePath: string): Promise<void> {
    await this.page.locator('input[type="file"]').setInputFiles(filePath);
  }

  async frameShot(): Promise<void> {
    await this.zoom.fill('2');
    const frame = this.page.getByRole('img', { name: /position your photo/i });
    const box = await frame.boundingBox();
    if (!box) throw new Error('crop frame not visible');
    const cx = box.x + box.width / 2;
    const cy = box.y + box.height / 2;
    await this.page.mouse.move(cx, cy);
    await this.page.mouse.down();
    await this.page.mouse.move(cx - 40, cy - 25, { steps: 5 });
    await this.page.mouse.up();
  }
}
