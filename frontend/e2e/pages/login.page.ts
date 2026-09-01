import type { Locator, Page } from "@playwright/test";

// Page object for the login screen and the Google sign-in hand-off (ADR-0007): locators
// and actions here, assertions in the spec.
export class LoginPage {
  readonly heading: Locator;
  readonly continueWithGoogle: Locator;
  readonly errorAlert: Locator;

  private readonly page: Page;

  constructor(page: Page) {
    this.page = page;
    this.heading = page.getByRole("heading", { name: "Pictogram" });
    this.continueWithGoogle = page.getByRole("link", { name: /continue with google/i });
    this.errorAlert = page.getByRole("alert");
  }

  async open(): Promise<void> {
    await this.page.goto("/");
  }

  /** The screen the backend redirects to when Google sign-in can't complete (#27). */
  async openWithError(reason: string): Promise<void> {
    await this.page.goto(`/login?error=${reason}`);
  }

  /**
   * Clicks "Continue with Google" and completes the mock provider's login form. Only a
   * username (the Google `sub`) is needed — compose.mock-oauth.yaml's tokenCallbacks add
   * the email claims — and it lands a Pictogram session back in the SPA.
   */
  async signInWithGoogle(): Promise<void> {
    await this.continueWithGoogle.click();

    // A unique subject each run -> a fresh Pictogram user that still needs onboarding.
    const googleSubject = `e2e-${Date.now().toString(36)}`;
    const username = this.page.locator('input[name="username"]');
    await username.waitFor({ state: "visible" });
    await username.fill(googleSubject);
    await this.page.locator('input[type="submit"]').click();
  }
}
