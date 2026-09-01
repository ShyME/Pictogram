import type { components } from "@shared";

/**
 * One row of a follower / following list: enough to render the row and link through to the
 * profile. `follow` owns no profile data (ADR-0002), so the list screen composes this from
 * a follow-list call plus one `GET /api/profiles?ids=` batch (ADR-0005).
 */
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
