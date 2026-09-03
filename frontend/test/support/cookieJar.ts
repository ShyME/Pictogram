import { vi } from 'vitest';

// A writable stand-in for document.cookie: the `unicorn/no-document-cookie` lint rule forbids
// assigning to it directly, and jsdom's real jar carries state between tests.
export function stubCookieJar(initial = ''): (value: string) => void {
  let jar = initial;
  vi.spyOn(document, 'cookie', 'get').mockImplementation(() => jar);
  return (value: string) => {
    jar = value;
  };
}
