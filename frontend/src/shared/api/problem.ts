import type { components } from "./schema";

export type ProblemDetail = components["schemas"]["ProblemDetail"];

const PROBLEM_TYPE_BASE = "https://pictogram.dev/problems/";

export function problemSlug(error: unknown): string | null {
  if (!error || typeof error !== "object") return null;
  const { type } = error as ProblemDetail;
  if (typeof type !== "string") return null;
  return type.startsWith(PROBLEM_TYPE_BASE) ? type.slice(PROBLEM_TYPE_BASE.length) : type;
}
