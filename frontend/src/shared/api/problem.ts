import type { components } from "./schema";

/**
 * RFC 9457 Problem Details as the backend sends them (`shared.http`): every expected
 * failure carries a stable `type` URI whose slug clients branch on without parsing prose.
 * Generated from `openapi.json`, where the error responses reference this schema.
 */
export type ProblemDetail = components["schemas"]["ProblemDetail"];

const PROBLEM_TYPE_BASE = "https://pictogram.dev/problems/";

/**
 * The stable slug from an error body's `type` (e.g. `"username-taken"`), or `null` when
 * the body is not a Problem Detail. openapi-fetch surfaces the parsed body as `error`.
 */
export function problemSlug(error: unknown): string | null {
  if (!error || typeof error !== "object") return null;
  const { type } = error as ProblemDetail;
  if (typeof type !== "string") return null;
  return type.startsWith(PROBLEM_TYPE_BASE) ? type.slice(PROBLEM_TYPE_BASE.length) : type;
}
