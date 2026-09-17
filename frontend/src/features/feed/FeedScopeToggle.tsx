import { Button } from '@shared';
import type { FeedScope } from './feedScopePreference';

export function FeedScopeToggle({
  active,
  onSelectFollowing,
  onSelectExplore,
}: {
  active: FeedScope;
  onSelectFollowing: () => void;
  onSelectExplore: () => void;
}) {
  return (
    <div role="tablist" aria-label="Feed scope" className="mb-6 flex justify-center gap-2">
      <Button
        type="button"
        role="tab"
        aria-selected={active === 'following'}
        variant={active === 'following' ? 'primary' : 'secondary'}
        size="sm"
        onClick={onSelectFollowing}
      >
        Following
      </Button>
      <Button
        type="button"
        role="tab"
        aria-selected={active === 'explore'}
        variant={active === 'explore' ? 'primary' : 'secondary'}
        size="sm"
        onClick={onSelectExplore}
      >
        Explore
      </Button>
    </div>
  );
}
