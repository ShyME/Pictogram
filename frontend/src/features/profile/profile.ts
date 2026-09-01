import type { components } from "@shared";

export type Profile = {
  userId: string;
  username: string;
  displayName: string | null;
  bio: string | null;
};

export function toProfile(view: components["schemas"]["ProfileView"]): Profile {
  return {
    userId: view.userId ?? "",
    username: view.username ?? "",
    displayName: view.displayName ?? null,
    bio: view.bio ?? null,
  };
}
