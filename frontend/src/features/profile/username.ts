export const USERNAME_PATTERN = /^[a-z0-9_]{3,20}$/;

export const USERNAME_RULE = "3–20 characters: lowercase letters, digits or underscore.";

export function isUsernameShapeValid(value: string): boolean {
  return USERNAME_PATTERN.test(value);
}

export type UsernameError = "username-shape" | "username-taken";

export const USERNAME_MESSAGE: Record<UsernameError, string> = {
  "username-shape": `Usernames are ${USERNAME_RULE}`,
  "username-taken": "That username is already taken. Try another.",
};

export function usernameError(value: string, serverError: string | null): UsernameError | null {
  if (value.length > 0 && !isUsernameShapeValid(value)) return "username-shape";
  if (serverError === "username-taken" || serverError === "username-shape") return serverError;
  return null;
}
