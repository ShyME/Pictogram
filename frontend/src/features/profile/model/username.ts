// Mirrors the backend `Username` shape rule (profile module): 3–20 characters of
// lowercase letters, digits or underscore. Checked here for instant feedback; the
// backend stays the authority and also owns uniqueness.
export const USERNAME_PATTERN = /^[a-z0-9_]{3,20}$/;

export const USERNAME_RULE = "3–20 characters: lowercase letters, digits or underscore.";

export function isUsernameShapeValid(value: string): boolean {
  return USERNAME_PATTERN.test(value);
}

/** The two username failures a form shows inline: a live shape check, and the server's "taken". */
export type UsernameError = "username-shape" | "username-taken";

export const USERNAME_MESSAGE: Record<UsernameError, string> = {
  "username-shape": `Usernames are ${USERNAME_RULE}`,
  "username-taken": "That username is already taken. Try another.",
};

/**
 * Which username error to show: a live shape check on what's typed wins, so the hint
 * updates as the user fixes it; otherwise fall back to the last submit's server verdict.
 */
export function usernameError(value: string, serverError: string | null): UsernameError | null {
  if (value.length > 0 && !isUsernameShapeValid(value)) return "username-shape";
  if (serverError === "username-taken" || serverError === "username-shape") return serverError;
  return null;
}
