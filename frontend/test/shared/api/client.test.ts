import type { components } from "@shared/api/schema";
import { api } from "@shared/api/client";

test("reads GET /api/profiles/me through the generated client", async () => {
  const profile = {
    userId: "1a2b3c4d-0000-0000-0000-000000000000",
    username: "ada",
    displayName: "Ada Lovelace",
    bio: "Countess of Lovelace",
  } satisfies components["schemas"]["ProfileView"];

  const requests: Request[] = [];
  const stubFetch: typeof fetch = (input) => {
    requests.push(input as Request);
    return Promise.resolve(
      new Response(JSON.stringify(profile), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
  };

  const { data, error } = await api.GET("/api/profiles/me", { fetch: stubFetch });

  expect(requests).toHaveLength(1);
  expect(requests[0].method).toBe("GET");
  expect(new URL(requests[0].url).pathname).toBe("/api/profiles/me");
  expect(error).toBeUndefined();
  expect(data?.username).toBe("ada");
});
