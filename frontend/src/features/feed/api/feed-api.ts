import { api } from "@shared";
import type { FeedItem } from "../model/feed";

/** Thrown when the feed comes back 401 — the auth middleware's silent refresh has failed. */
export class SessionExpiredError extends Error {}

/**
 * The viewer's home feed. Fan-out-on-read assembly lands with a later slice (ADR-0003);
 * for now every page comes back empty, which is enough for a freshly onboarded user to
 * land somewhere.
 */
export async function fetchFeed(): Promise<FeedItem[]> {
  const { data, response } = await api.GET("/api/feed");
  if (response.status === 401) throw new SessionExpiredError();
  if (!data) throw new Error(`Feed request failed: ${response.status}`);

  return (data.items ?? []).map((card) => ({
    postId: card.postId ?? "",
    author: card.author ?? "",
  }));
}
