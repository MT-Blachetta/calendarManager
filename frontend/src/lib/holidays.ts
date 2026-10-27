import { addDays, format } from 'date-fns'

/** The 16 German federal states (ISO 3166-2:DE without the "DE-" prefix). */
export const GERMAN_STATES = [
  'BW', 'BY', 'BE', 'BB', 'HB', 'HH', 'HE', 'MV', 'NI', 'NW', 'RP', 'SL', 'SN', 'ST', 'SH', 'TH',
] as const
export type GermanState = (typeof GERMAN_STATES)[number]

/** Which public holidays are shown: none, the nationwide German ones, or those of one federal state. */
export type HolidayRegion = 'off' | 'DE' | `DE-${GermanState}`

export const HOLIDAY_REGIONS: HolidayRegion[] = ['off', 'DE', ...GERMAN_STATES.map((s) => `DE-${s}` as const)]

export function isHolidayRegion(value: unknown): value is HolidayRegion {
  return typeof value === 'string' && (HOLIDAY_REGIONS as string[]).includes(value)
}

/** Holiday ids; the names are translated in the locale files (`holidays.names.<id>`). */
export type HolidayId =
  | 'newYear'
  | 'epiphany'
  | 'womensDay'
  | 'goodFriday'
  | 'easterSunday'
  | 'easterMonday'
  | 'labourDay'
  | 'liberationDay'
  | 'ascension'
  | 'whitSunday'
  | 'whitMonday'
  | 'corpusChristi'
  | 'assumption'
  | 'childrensDay'
  | 'unityDay'
  | 'reformationDay'
  | 'allSaints'
  | 'repentanceDay'
  | 'christmasDay'
  | 'boxingDay'

export interface Holiday {
  /** "yyyy-MM-dd" */
  date: string
  id: HolidayId
  /** Observed in all federal states. */
  nationwide: boolean
}

/** Easter Sunday (Gregorian calendar, anonymous algorithm by Meeus/Jones/Butcher). */
export function easterSunday(year: number): Date {
  const a = year % 19
  const b = Math.floor(year / 100)
  const c = year % 100
  const d = Math.floor(b / 4)
  const e = b % 4
  const f = Math.floor((b + 8) / 25)
  const g = Math.floor((b - f + 1) / 3)
  const h = (19 * a + b - d - g + 15) % 30
  const i = Math.floor(c / 4)
  const k = c % 4
  const l = (32 + 2 * e + 2 * i - h - k) % 7
  const m = Math.floor((a + 11 * h + 22 * l) / 451)
  const month = Math.floor((h + l - 7 * m + 114) / 31)
  const day = ((h + l - 7 * m + 114) % 31) + 1
  return new Date(year, month - 1, day)
}

/** Buß- und Bettag: the Wednesday before 23 November. */
function repentanceDay(year: number): Date {
  const nov22 = new Date(year, 10, 22)
  return addDays(nov22, -((nov22.getDay() - 3 + 7) % 7))
}

type Rule = {
  id: HolidayId
  date: (year: number, easter: Date) => Date
  /** States observing the holiday; `'all'` = nationwide. */
  states: 'all' | readonly GermanState[]
  /** Years in which the rule applies (inclusive). */
  from?: number
  to?: number
  only?: readonly number[]
}

const fixed = (month: number, day: number) => (year: number) => new Date(year, month - 1, day)
const easter = (offset: number) => (_: number, e: Date) => addDays(e, offset)

/**
 * Statutory public holidays in Germany. Holidays that apply only in some municipalities
 * (Augsburger Friedensfest; Corpus Christi in parts of Saxony and Thuringia) are not included;
 * Assumption Day is listed for Bavaria, where most municipalities observe it.
 */
const RULES: Rule[] = [
  { id: 'newYear', date: fixed(1, 1), states: 'all' },
  { id: 'epiphany', date: fixed(1, 6), states: ['BW', 'BY', 'ST'] },
  { id: 'womensDay', date: fixed(3, 8), states: ['BE'], from: 2019 },
  { id: 'womensDay', date: fixed(3, 8), states: ['MV'], from: 2023 },
  { id: 'goodFriday', date: easter(-2), states: 'all' },
  { id: 'easterSunday', date: easter(0), states: ['BB'] },
  { id: 'easterMonday', date: easter(1), states: 'all' },
  { id: 'labourDay', date: fixed(5, 1), states: 'all' },
  { id: 'liberationDay', date: fixed(5, 8), states: ['BE'], only: [2020, 2025] },
  { id: 'ascension', date: easter(39), states: 'all' },
  { id: 'whitSunday', date: easter(49), states: ['BB'] },
  { id: 'whitMonday', date: easter(50), states: 'all' },
  { id: 'corpusChristi', date: easter(60), states: ['BW', 'BY', 'HE', 'NW', 'RP', 'SL'] },
  { id: 'assumption', date: fixed(8, 15), states: ['BY', 'SL'] },
  { id: 'childrensDay', date: fixed(9, 20), states: ['TH'], from: 2019 },
  { id: 'unityDay', date: fixed(10, 3), states: 'all', from: 1990 },
  { id: 'reformationDay', date: fixed(10, 31), states: 'all', only: [2017] },
  { id: 'reformationDay', date: fixed(10, 31), states: ['BB', 'MV', 'SN', 'ST', 'TH'], to: 2016 },
  { id: 'reformationDay', date: fixed(10, 31), states: ['BB', 'MV', 'SN', 'ST', 'TH'], from: 2018 },
  { id: 'reformationDay', date: fixed(10, 31), states: ['HB', 'HH', 'NI', 'SH'], from: 2018 },
  { id: 'allSaints', date: fixed(11, 1), states: ['BW', 'BY', 'NW', 'RP', 'SL'] },
  { id: 'repentanceDay', date: repentanceDay, states: ['SN'], from: 1995 },
  { id: 'christmasDay', date: fixed(12, 25), states: 'all' },
  { id: 'boxingDay', date: fixed(12, 26), states: 'all' },
]

function applies(rule: Rule, year: number): boolean {
  if (rule.only) return rule.only.includes(year)
  return (rule.from === undefined || year >= rule.from) && (rule.to === undefined || year <= rule.to)
}

/** Public holidays of one year, sorted by date: nationwide ones, plus those of `state` if given. */
export function germanHolidays(year: number, state?: GermanState | null): Holiday[] {
  const e = easterSunday(year)
  const result: Holiday[] = []
  for (const rule of RULES) {
    if (!applies(rule, year)) continue
    const nationwide = rule.states === 'all'
    if (!nationwide && !(state && rule.states.includes(state))) continue
    result.push({ date: format(rule.date(year, e), 'yyyy-MM-dd'), id: rule.id, nationwide })
  }
  return result.sort((a, b) => a.date.localeCompare(b.date))
}

/** Holidays between two dates (inclusive), keyed by "yyyy-MM-dd". */
export function holidaysBetween(from: Date, to: Date, region: HolidayRegion): Map<string, Holiday> {
  const map = new Map<string, Holiday>()
  if (region === 'off') return map
  const state = region === 'DE' ? null : (region.slice(3) as GermanState)
  const fromKey = format(from, 'yyyy-MM-dd')
  const toKey = format(to, 'yyyy-MM-dd')
  for (let year = from.getFullYear(); year <= to.getFullYear(); year++) {
    for (const h of germanHolidays(year, state)) {
      if (h.date >= fromKey && h.date <= toKey && !map.has(h.date)) map.set(h.date, h)
    }
  }
  return map
}
