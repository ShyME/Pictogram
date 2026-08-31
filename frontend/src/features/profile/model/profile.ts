import type { components } from "@shared";

export type Profile = {
  userId: string;
  username: string;
  displayName: string | null;
  bio: string | null;
};

// The backend contract marks every ProfileView field optional (no `required` in the
// generated schema), but `userId`/`username` are always present on a real profile;
// `displayName`/`bio` are genuinely absent when the user left them blank.
export function toProfile(view: components["schemas"]["ProfileView"]): Profile {
  return {
    userId: view.userId ?? "",
    username: view.username ?? "",
    displayName: view.displayName ?? null,
    bio: view.bio ?? null,
  };
}
