export type FeedScope = 'following' | 'explore';

const SCOPE_KEY = 'pictogram.feed-scope';

export function readFeedScopePreference(): FeedScope {
  try {
    return localStorage.getItem(SCOPE_KEY) === 'explore' ? 'explore' : 'following';
  } catch {
    return 'following';
  }
}

export function writeFeedScopePreference(scope: FeedScope): void {
  try {
    localStorage.setItem(SCOPE_KEY, scope);
  } catch {
    // Nothing to persist to — the in-memory toggle state below still drives this visit.
  }
}
