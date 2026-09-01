import { api } from "@shared";
import type { FeedItem } from "./feed";

export class SessionExpiredError extends Error {}

export async function fetchFeed(): Promise<FeedItem[]> {
  const { data, response } = await api.GET("/api/feed");
  if (response.status === 401) throw new SessionExpiredError();
  if (!data) throw new Error(`Feed request failed: ${response.status}`);

  return (data.items ?? []).map((card) => ({
    postId: card.postId ?? "",
    author: card.author ?? "",
  }));
}
