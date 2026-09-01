import type { components } from "@shared";

export type Account = {
  userId: string;
  username: string;
  displayName: string | null;
};

export function toAccount(view: components["schemas"]["ProfileView"]): Account {
  return {
    userId: view.userId ?? "",
    username: view.username ?? "",
    displayName: view.displayName ?? null,
  };
}
