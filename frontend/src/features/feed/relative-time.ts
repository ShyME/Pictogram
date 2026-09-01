const MINUTE = 60;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;
const WEEK = 7 * DAY;

export function relativeTime(iso: string, now: Date = new Date()): string {
  const then = Date.parse(iso);
  if (Number.isNaN(then)) return "";

  const seconds = Math.max(0, Math.round((now.getTime() - then) / 1000));
  if (seconds < 45) return "just now";
  if (seconds < HOUR) return `${Math.round(seconds / MINUTE)}m`;
  if (seconds < DAY) return `${Math.round(seconds / HOUR)}h`;
  if (seconds < WEEK) return `${Math.round(seconds / DAY)}d`;
  if (seconds < 5 * WEEK) return `${Math.round(seconds / WEEK)}w`;

  return new Date(then).toLocaleDateString(undefined, { month: "short", day: "numeric" });
}
