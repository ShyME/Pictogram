import { expect, test } from "@playwright/test";
import { AppShellPage } from "./pages/app-shell.page";

// The one blackbox journey until the feature tickets add theirs (#10–#19): the built
// image serves the SPA and it renders. Proves the Playwright wiring end to end.
test("the app shell loads from the built image", async ({ page }) => {
  const app = new AppShellPage(page);

  await app.open();

  await expect(app.heading).toBeVisible();
});
