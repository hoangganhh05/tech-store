/**
 * Convert an API ISO timestamp (stored in UTC) to the local value expected by
 * an HTML datetime-local input. The input intentionally has no timezone
 * suffix, so the browser will interpret the returned value in the user's
 * local timezone when it is submitted again.
 */
export function toDateTimeLocalValue(value: string | null | undefined): string {
  if (!value) return "";

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";

  const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  return localDate.toISOString().slice(0, 16);
}
