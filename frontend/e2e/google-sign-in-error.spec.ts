import { expect, test } from "@playwright/test";
import { LoginPage } from "./pages/login.page";

test("an unusable Google account lands back on login with an explanation", async ({ page }) => {
  const login = new LoginPage(page);

  await login.openWithError("email-unverified");

  await expect(login.errorAlert).toContainText(/verif/i);
  await expect(login.continueWithGoogle).toBeVisible();
});

test("a generic sign-in failure shows a retry-friendly message", async ({ page }) => {
  const login = new LoginPage(page);

  await login.openWithError("sign-in-failed");

  await expect(login.errorAlert).toContainText(/didn't complete/i);
  await expect(login.continueWithGoogle).toBeVisible();
});
