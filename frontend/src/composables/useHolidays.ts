import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useUiStore } from '@/stores/ui'
import { holidaysBetween, type Holiday } from '@/lib/holidays'

/** Public holidays of the region chosen in the settings, with translated names. */
export function useHolidays() {
  const ui = useUiStore()
  const { t } = useI18n()

  /** Holidays from `from` to `to` (inclusive) keyed by "yyyy-MM-dd"; reactive to the region setting. */
  function between(from: Date, to: Date): Map<string, Holiday> {
    return holidaysBetween(from, to, ui.holidayRegion)
  }

  /** Lazily filled per-year lookup (the mini calendar can be paged to any year); reset when the region changes. */
  const yearCache = computed(() => {
    const region = ui.holidayRegion
    const cache = new Map<number, Map<string, Holiday>>()
    return (year: number) => {
      let map = cache.get(year)
      if (!map) {
        map = holidaysBetween(new Date(year, 0, 1), new Date(year, 11, 31), region)
        cache.set(year, map)
      }
      return map
    }
  })

  /** The holiday on a day ("yyyy-MM-dd"), if any. */
  function holidayOn(dayKey: string): Holiday | undefined {
    return yearCache.value(Number(dayKey.slice(0, 4))).get(dayKey)
  }

  function holidayName(holiday: Holiday): string {
    return t(`holidays.names.${holiday.id}`)
  }

  /** Compact name for narrow places such as month grid cells. */
  function holidayShortName(holiday: Holiday): string {
    return t(`holidays.short.${holiday.id}`)
  }

  return { between, holidayOn, holidayName, holidayShortName }
}
