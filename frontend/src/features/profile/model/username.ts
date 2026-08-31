// Mirrors the backend `Username` shape rule (profile module): 3–20 characters of
// lowercase letters, digits or underscore. Checked here for instant feedback; the
// backend stays the authority and also owns uniqueness.
export const USERNAME_PATTERN = /^[a-z0-9_]{3,20}$/;

export const USERNAME_RULE = "3–20 characters: lowercase letters, digits or underscore.";

export function isUsernameShapeValid(value: string): boolean {
  return USERNAME_PATTERN.test(value);
}
