// The feed and the profile grid both load a page, turn it into cards, and prime the
// per-card like / comment-count caches from that one page load (ADR-0005). Priming is
// best-effort: if a likes or comment-count batch is down the hearts and counts degrade to
// their own fetch, the page still renders. A priming failure must never reach the caller.
export async function primeBestEffort(
  tasks: readonly (Promise<unknown> | undefined | null)[],
): Promise<void> {
  await Promise.allSettled(tasks.filter((task): task is Promise<unknown> => task != null));
}

// Hydration is required — a failure there is the page's failure. Priming runs after and is
// swallowed. Both steps are injected so this composes without the page component.
export async function composeCards<TItem, TCard>(
  items: TItem[],
  options: {
    hydrate: (items: TItem[]) => Promise<TCard[]>;
    idOf: (card: TCard) => string;
    prime: (ids: string[]) => readonly (Promise<unknown> | undefined | null)[];
  },
): Promise<TCard[]> {
  const cards = await options.hydrate(items);
  await primeBestEffort(options.prime(cards.map((card) => options.idOf(card))));
  return cards;
}
