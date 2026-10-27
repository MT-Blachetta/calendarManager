import { format as formatFns } from 'date-fns'
import { de as deFns, enGB } from 'date-fns/locale'
import { currentLocale, type Locale } from './index'

/** date-fns patterns per UI language (English uses British day-month order and 24 h times). */
const DATE_FORMATS = {
  en: {
    /** Monday, 19 October 2026 */
    dayTitle: 'EEEE, d MMMM yyyy',
    /** 19 October 2026 */
    dayLong: 'd MMMM yyyy',
    /** 19 October */
    dayMonth: 'd MMMM',
    /** Monday */
    weekday: 'EEEE',
    /** Mon */
    weekdayShort: 'EEE',
    /** October 2026 */
    monthTitle: 'MMMM yyyy',
    /** Mon, 19 Oct 2026 */
    dateShort: 'EEE, d MMM yyyy',
    /** Mon, 19 Oct */
    dayShort: 'EEE, d MMM',
    /** Mon 19 Oct, 9:30 */
    dateTimeShort: 'EEE d MMM, H:mm',
  },
  de: {
    dayTitle: 'EEEE, d. MMMM yyyy',
    dayLong: 'd. MMMM yyyy',
    dayMonth: 'd. MMMM',
    weekday: 'EEEE',
    weekdayShort: 'EEEEEE',
    monthTitle: 'MMMM yyyy',
    dateShort: 'EEEEEE, d. MMM yyyy',
    dayShort: 'EEEEEE, d. MMM',
    dateTimeShort: 'EEEEEE d. MMM, H:mm',
  },
} satisfies Record<Locale, Record<string, string>>

export type DateFormat = keyof (typeof DATE_FORMATS)['en']

/** PrimeVue DatePicker patterns (PrimeVue syntax: D = short day name, M = short month name). */
const PICKER_FORMATS = {
  en: { dateTime: 'D, d M yy', date: 'd M yy' },
  de: { dateTime: 'D, d. M yy', date: 'dd.mm.yy' },
} satisfies Record<Locale, Record<string, string>>

const DATE_FNS_LOCALES = { en: enGB, de: deFns }

/** Formats a date in the active UI language. Reactive when used in computed properties or templates. */
export function formatDate(date: Date, pattern: DateFormat): string {
  const locale = currentLocale()
  return formatFns(date, DATE_FORMATS[locale][pattern], { locale: DATE_FNS_LOCALES[locale] })
}

export function pickerFormat(kind: keyof (typeof PICKER_FORMATS)['en']): string {
  return PICKER_FORMATS[currentLocale()][kind]
}
