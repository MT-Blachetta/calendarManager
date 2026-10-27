import { addDays, differenceInCalendarDays, format, isValid, parse, startOfDay } from 'date-fns'
import { t } from '@/i18n'
import { formatDate } from '@/i18n/dates'

/** The day grid shows 6:00 – 0:00 (24:00) in 30-minute slots, as in the specification. */
export const DAY_START_HOUR = 6
export const DAY_END_HOUR = 24
export const SLOT_MINUTES = 30
export const DAY_START_MIN = DAY_START_HOUR * 60
export const DAY_END_MIN = DAY_END_HOUR * 60
export const VISIBLE_MINUTES = DAY_END_MIN - DAY_START_MIN
export const SLOT_COUNT = VISIBLE_MINUTES / SLOT_MINUTES

const DAY_KEY = 'yyyy-MM-dd'

/** "yyyy-MM-dd" of a local date. */
export function toDayKey(date: Date): string {
  return format(date, DAY_KEY)
}

/** Parses "yyyy-MM-dd" as local midnight; falls back to today. */
export function parseDayKey(key: string | undefined | null): Date {
  if (key) {
    const parsed = parse(key, DAY_KEY, new Date())
    if (isValid(parsed)) return startOfDay(parsed)
  }
  return startOfDay(new Date())
}

/** Local [midnight, next midnight) of the day (DST-safe). */
export function dayRange(day: Date): { from: Date; to: Date } {
  const from = startOfDay(day)
  return { from, to: addDays(from, 1) }
}

/**
 * Wall-clock minutes of `date` relative to local midnight of `day`
 * (negative before the day, >= 1440 after it).
 */
export function minutesOfDay(date: Date, day: Date): number {
  return differenceInCalendarDays(date, day) * 1440 + date.getHours() * 60 + date.getMinutes()
}

/** Labels of the time gutter: "6:00", "6:30", …, "23:30", "0:00". */
export function slotLabels(): string[] {
  const labels: string[] = []
  for (let m = DAY_START_MIN; m <= DAY_END_MIN; m += SLOT_MINUTES) {
    labels.push(formatMinutes(m))
  }
  return labels
}

/** 24h "H:mm" of minutes since midnight (1440 → "0:00"). */
export function formatMinutes(minutes: number): string {
  const m = ((minutes % 1440) + 1440) % 1440
  return `${Math.floor(m / 60)}:${String(m % 60).padStart(2, '0')}`
}

export function formatTime(date: Date): string {
  return format(date, 'H:mm')
}

export function formatTimeRange(start: Date, end: Date): string {
  const sameDay = toDayKey(start) === toDayKey(end)
  const endsAtMidnight = !sameDay && differenceInCalendarDays(end, start) === 1 && end.getHours() === 0 && end.getMinutes() === 0
  if (sameDay || endsAtMidnight) return `${formatTime(start)} – ${formatTime(end)}`
  return `${formatDate(start, 'dateTimeShort')} – ${formatDate(end, 'dateTimeShort')}`
}

export function formatDuration(minutes: number): string {
  const h = Math.floor(minutes / 60)
  const m = Math.round(minutes % 60)
  if (h === 0) return t('time.minutes', { m })
  return m === 0 ? t('time.hours', { h }) : t('time.hoursMinutes', { h, m })
}

export function durationMinutes(start: Date, end: Date): number {
  return Math.round((end.getTime() - start.getTime()) / 60_000)
}

export interface VisibleSpan {
  /** Clipped minutes relative to midnight, within [DAY_START_MIN, DAY_END_MIN]. */
  start: number
  end: number
  /** The event starts before the visible range / continues after it. */
  clippedTop: boolean
  clippedBottom: boolean
}

/** Clips an event to the visible 6:00–24:00 window of `day`; null if it is not visible at all. */
export function visibleSpan(start: Date, end: Date, day: Date): VisibleSpan | null {
  const s = minutesOfDay(start, day)
  const e = minutesOfDay(end, day)
  if (e <= DAY_START_MIN || s >= DAY_END_MIN) return null
  return {
    start: Math.max(s, DAY_START_MIN),
    end: Math.min(e, DAY_END_MIN),
    clippedTop: s < DAY_START_MIN,
    clippedBottom: e > DAY_END_MIN,
  }
}

/** Whether two half-open intervals [aStart, aEnd) and [bStart, bEnd) overlap. */
export function overlaps(aStart: number, aEnd: number, bStart: number, bEnd: number): boolean {
  return aStart < bEnd && bStart < aEnd
}

/** Snap minutes down to the slot grid. */
export function snapDown(minutes: number, step = SLOT_MINUTES): number {
  return Math.floor(minutes / step) * step
}

/** A local Date on `day` at `minutes` after midnight. */
export function atMinutes(day: Date, minutes: number): Date {
  const d = startOfDay(day)
  return new Date(d.getFullYear(), d.getMonth(), d.getDate(), 0, minutes)
}
