import { defineStore } from 'pinia'
import { useMediaQuery, useStorage, usePreferredDark } from '@vueuse/core'
import { computed, ref, watchEffect } from 'vue'
import { toDayKey } from '@/lib/time'
import { isHolidayRegion, type HolidayRegion } from '@/lib/holidays'
import { DEFAULT_LOCALE, isLocale, type Locale } from '@/i18n'
import { DARK_MODE_CLASS } from '@/theme'

export type Density = 'compact' | 'comfortable' | 'spacious'

/** localStorage serializer for ids (a `null` default would otherwise make VueUse store/read strings). */
const nullableNumber = {
  read: (raw: string): number | null => {
    const n = Number(raw)
    return raw === '' || raw === 'null' || Number.isNaN(n) ? null : n
  },
  write: (value: number | null): string => (value === null ? 'null' : String(value)),
}

/** localStorage serializer that falls back to `fallback` for unknown (e.g. outdated) values. */
function oneOf<T extends string>(guard: (value: unknown) => value is T, fallback: T) {
  return {
    read: (raw: string): T => (guard(raw) ? raw : fallback),
    write: (value: T): string => value,
  }
}

const DENSITIES: Density[] = ['compact', 'comfortable', 'spacious']
const THEMES = ['light', 'dark', 'system'] as const
export type Theme = (typeof THEMES)[number]

/** Defaults of the settings page. */
export const DEFAULT_SETTINGS = {
  locale: DEFAULT_LOCALE as Locale,
  theme: 'system' as Theme,
  density: 'comfortable' as Density,
  holidayRegion: 'DE' as HolidayRegion,
}

export const SLOT_HEIGHT: Record<Density, number> = { compact: 30, comfortable: 44, spacious: 64 }

/** UI preferences persisted in localStorage. */
export const useUiStore = defineStore('ui', () => {
  const viewId = useStorage<number | null>('cm.viewId', null, undefined, {
    serializer: nullableNumber,
  })
  const density = useStorage<Density>('cm.density', DEFAULT_SETTINGS.density, undefined, {
    serializer: oneOf((v): v is Density => DENSITIES.includes(v as Density), DEFAULT_SETTINGS.density),
  })
  const theme = useStorage<Theme>('cm.theme', DEFAULT_SETTINGS.theme, undefined, {
    serializer: oneOf((v): v is Theme => THEMES.includes(v as Theme), DEFAULT_SETTINGS.theme),
  })
  /** UI language. */
  const locale = useStorage<Locale>('cm.locale', DEFAULT_SETTINGS.locale, undefined, {
    serializer: oneOf(isLocale, DEFAULT_SETTINGS.locale),
  })
  /** Public holidays shown in the month view and the mini calendar. */
  const holidayRegion = useStorage<HolidayRegion>('cm.holidayRegion', DEFAULT_SETTINGS.holidayRegion, undefined, {
    serializer: oneOf(isHolidayRegion, DEFAULT_SETTINGS.holidayRegion),
  })
  const sidebarCollapsed = useStorage('cm.sidebarCollapsed', false)
  /** Calendar or group selected in the month view (null = all calendars). */
  const monthCategoryId = useStorage<number | null>('cm.monthCategoryId', null, undefined, {
    serializer: nullableNumber,
  })
  const prefersDark = usePreferredDark()
  /** Day last shown in the day view (yyyy-MM-dd), used when navigating back to it. */
  const lastDay = ref(toDayKey(new Date()))
  const isMobile = useMediaQuery('(max-width: 900px)')
  const mobileNavOpen = ref(false)

  const isDark = computed(() => theme.value === 'dark' || (theme.value === 'system' && prefersDark.value))
  watchEffect(() => document.documentElement.classList.toggle(DARK_MODE_CLASS, isDark.value))

  const slotHeight = computed(() => SLOT_HEIGHT[density.value])

  function toggleTheme() {
    theme.value = isDark.value ? 'light' : 'dark'
  }

  function resetSettings() {
    locale.value = DEFAULT_SETTINGS.locale
    theme.value = DEFAULT_SETTINGS.theme
    density.value = DEFAULT_SETTINGS.density
    holidayRegion.value = DEFAULT_SETTINGS.holidayRegion
  }

  return {
    viewId,
    monthCategoryId,
    density,
    theme,
    locale,
    holidayRegion,
    isDark,
    sidebarCollapsed,
    slotHeight,
    lastDay,
    isMobile,
    mobileNavOpen,
    toggleTheme,
    resetSettings,
  }
})
