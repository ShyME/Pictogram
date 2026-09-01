// @vitest-environment node
import { readFile } from "node:fs/promises";
import openapiTS, { astToString, COMMENT_HEADER } from "openapi-typescript";

// The backend build fails when backend/openapi.json drifts from the running app; this is the
// other half — schema.d.ts must be what `pnpm generate:api` produces from that file, so a
// backend endpoint change can't leave the frontend types silently stale.
test("schema.d.ts matches backend/openapi.json", async () => {
  const specPath = new URL("../../../../backend/openapi.json", import.meta.url);
  const spec = JSON.parse(await readFile(specPath, "utf8"));
  const regenerated = `${COMMENT_HEADER}${astToString(await openapiTS(spec))}`;

  const committed = await readFile(new URL("../../../src/shared/api/schema.d.ts", import.meta.url), "utf8");

  expect(committed).toBe(regenerated);
});
