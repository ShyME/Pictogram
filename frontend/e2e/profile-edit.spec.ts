import { expect, test } from "@playwright/test";
import { LoginPage } from "./pages/login.page";
import { OnboardingPage } from "./pages/onboarding.page";
import { FeedPage } from "./pages/feed.page";
import { ProfilePage } from "./pages/profile.page";
import { EditProfilePage } from "./pages/edit-profile.page";

// The ticket's broad journey (#12), against `task up`: a signed-in user edits each field of
// their profile, is warned before a username change, and after the rename the old handle
// stops resolving.
test("edit profile: change display name, bio and username; the old handle 404s", async ({ page }) => {
  const login = new LoginPage(page);
  const onboarding = new OnboardingPage(page);
  const feed = new FeedPage(page);
  const profile = new ProfilePage(page);
  const edit = new EditProfilePage(page);

  const original = `e2e_e_${Date.now().toString(36)}`;
  const renamed = `${original}_v2`;

  await login.open();
  await login.signInWithGoogle();
  await expect(page).toHaveURL(/\/onboarding$/);
  await onboarding.completeWith(original, "Edit Tester");
  await expect(feed.emptyState).toBeVisible();

  await feed.myProfileLink(original).click();
  await expect(page).toHaveURL(new RegExp(`/u/${original}$`));
  await profile.editProfileLink.click();
  await expect(page).toHaveURL(/\/settings\/profile$/);

  await edit.displayName.fill("Edith Tester");
  await edit.bio.fill("Now with a bio.");
  await expect(edit.renameWarning).toBeHidden();

  await edit.username.fill(renamed);
  await expect(edit.renameWarning).toBeVisible();

  await edit.saveChanges.click();

  await expect(page).toHaveURL(new RegExp(`/u/${renamed}$`));
  await expect(profile.displayName("Edith Tester")).toBeVisible();
  await expect(profile.handle(renamed)).toBeVisible();
  await expect(page.getByText("Now with a bio.")).toBeVisible();

  await profile.open(original);
  await expect(profile.notFoundHeading).toBeVisible();
});
