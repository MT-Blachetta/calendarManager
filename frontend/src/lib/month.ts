import {
  addDays,
  format,
  isValid,
  parse,
  startOfMonth,
  startOfWeek,
} from 'date-fns'

const MONTH_KEY = 'yyyy-MM'

/** Weeks start on Monday (ISO 8601). */
export const WEEK_STARTS_ON = 1

/** The month grid always shows 6 weeks so its height does not jump between months. */
export const GRID_WEEKS = 6

export function toMonthKey(date: Date): string {
  return format(date, MONTH_KEY)
}

/** Parses "yyyy-MM" as the first day of that month; falls back to the current month. */
export function parseMonthKey(key: string | undefined | null): Date {
  if (key) {
    const parsed = parse(key, MONTH_KEY, new Date())
    if (isValid(parsed)) return startOfMonth(parsed)
  }
  return startOfMonth(new Date())
}

/** 6 weeks × 7 days covering the month, starting on the Monday on or before the 1st. */
export function monthGrid(month: Date): Date[][] {
  const first = startOfWeek(startOfMonth(month), { weekStartsOn: WEEK_STARTS_ON })
  return Array.from({ length: GRID_WEEKS }, (_, w) =>
    Array.from({ length: 7 }, (_, d) => addDays(first, w * 7 + d)),
  )
}

/** First and last visible day of the month grid (inclusive). */
export function gridRange(month: Date): { from: Date; to: Date } {
  const weeks = monthGrid(month)
  return { from: weeks[0]![0]!, to: weeks[GRID_WEEKS - 1]![6]! }
}

/** Highlight intensity 0–4 for a number of events on a day. */
export function heatLevel(count: number): 0 | 1 | 2 | 3 | 4 {
  if (count <= 0) return 0
  if (count === 1) return 1
  if (count <= 3) return 2
  if (count <= 7) return 3
  return 4
}

/** IANA time zone of the browser, e.g. "Europe/Berlin". */
export function browserTimeZone(): string {
  return Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC'
}
