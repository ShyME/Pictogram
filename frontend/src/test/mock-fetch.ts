import { vi } from "vitest";

/**
 * Installs a `globalThis.fetch` stub for a test and returns the list of requests it
 * received. `route` decides each response; `hits` is how many earlier calls hit the same
 * path (so a route can 401 once, then succeed). The client, its retry middleware and the
 * raw refresh call all dispatch through `globalThis.fetch`, so one stub covers them.
 */
export function stubFetch(
  route: (request: Request, hits: number) => Response | Promise<Response>,
): Request[] {
  const calls: Request[] = [];
  vi.stubGlobal(
    "fetch",
    vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const request =
        input instanceof Request
          ? input
          : new Request(new URL(String(input), window.location.href), init);
      const path = new URL(request.url).pathname;
      const hits = calls.filter((c) => new URL(c.url).pathname === path).length;
      calls.push(request);
      return route(request, hits);
    }),
  );
  return calls;
}

export const pathOf = (request: Request): string => new URL(request.url).pathname;

export function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

/** An RFC 9457 problem body as the backend's `shared.http` layer sends it. */
export function problemResponse(slug: string, status: number): Response {
  return new Response(
    JSON.stringify({ type: `https://pictogram.dev/problems/${slug}`, title: slug, status }),
    { status, headers: { "Content-Type": "application/problem+json" } },
  );
}
