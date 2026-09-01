// @vitest-environment node
import { readFile } from 'node:fs/promises';
import openapiTS, { astToString, COMMENT_HEADER } from 'openapi-typescript';

test('schema.d.ts matches backend/openapi.json', async () => {
  const specPath = new URL('../../../../backend/openapi.json', import.meta.url);
  const spec = JSON.parse(await readFile(specPath, 'utf8'));
  const regenerated = `${COMMENT_HEADER}${astToString(await openapiTS(spec))}`;

  const committed = await readFile(
    new URL('../../../src/shared/api/schema.d.ts', import.meta.url),
    'utf8',
  );

  expect(committed).toBe(regenerated);
});
