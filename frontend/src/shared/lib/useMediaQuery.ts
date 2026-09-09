import { useSyncExternalStore } from 'react';

// A media query as a reactive boolean, matched in JS so a component renders only the
// branch that applies instead of shipping both and hiding one with CSS. The server
// snapshot is `false` — the narrow branch — so a match only ever appears after mount.
// eslint-disable-next-line unicorn/consistent-boolean-name -- a generic media-query hook; `useMediaQuery` is the idiomatic React name
export function useMediaQuery(query: string): boolean {
  return useSyncExternalStore(
    (onChange) => {
      const mql = matchMedia(query);
      mql.addEventListener('change', onChange);
      return () => {
        mql.removeEventListener('change', onChange);
      };
    },
    () => matchMedia(query).matches,
    () => false,
  );
}
