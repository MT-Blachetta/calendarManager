import { createI18n } from 'vue-i18n'
import en from './locales/en'
import de from './locales/de'

export type MessageSchema = typeof en

/** Supported UI languages; English is the default. */
export const LOCALES = ['en', 'de'] as const
export type Locale = (typeof LOCALES)[number]
export const DEFAULT_LOCALE: Locale = 'en'

/** Language names in their own language (shown the same in every UI language). */
export const LOCALE_NAMES: Record<Locale, string> = { en: 'English', de: 'Deutsch' }

export function isLocale(value: unknown): value is Locale {
  return typeof value === 'string' && (LOCALES as readonly string[]).includes(value)
}

export const i18n = createI18n<[MessageSchema], Locale, false>({
  legacy: false,
  locale: DEFAULT_LOCALE,
  fallbackLocale: DEFAULT_LOCALE,
  messages: { en, de },
})

/** Translate outside of components (stores, toasts, API client). */
export const t = i18n.global.t

export function currentLocale(): Locale {
  return i18n.global.locale.value
}
