import { vi } from "vitest";

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

export function problemResponse(slug: string, status: number): Response {
  return new Response(
    JSON.stringify({ type: `https://pictogram.dev/problems/${slug}`, title: slug, status }),
    { status, headers: { "Content-Type": "application/problem+json" } },
  );
}
