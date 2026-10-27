import { describe, expect, it } from 'vitest'
import en from '../locales/en'
import de from '../locales/de'
import { i18n } from '../index'
import { formatDate } from '../dates'

/** Flattens nested messages into dotted keys. */
function keys(obj: object, prefix = ''): string[] {
  return Object.entries(obj).flatMap(([k, v]) =>
    typeof v === 'object' && v !== null ? keys(v, `${prefix}${k}.`) : [`${prefix}${k}`],
  )
}

describe('locales', () => {
  it('German defines exactly the English keys', () => {
    expect(keys(de).sort()).toEqual(keys(en).sort())
  })

  it('every message compiles and renders in both languages', () => {
    for (const locale of ['en', 'de'] as const) {
      i18n.global.locale.value = locale
      for (const key of keys(en)) {
        const text = i18n.global.t(key, { n: 2, name: 'X', title: 'X', parts: 'X', list: 'X', text: 'X' }, 2)
        expect(text, `${locale}:${key}`).not.toBe(key)
      }
    }
    i18n.global.locale.value = 'en'
  })

  it('pluralises and formats dates per language', () => {
    const { t } = i18n.global
    const day = new Date(2026, 9, 19)
    expect(t('common.eventsCount', 1)).toBe('1 event')
    expect(t('common.eventsCount', 3)).toBe('3 events')
    expect(formatDate(day, 'dayTitle')).toBe('Monday, 19 October 2026')
    i18n.global.locale.value = 'de'
    expect(t('common.eventsCount', 1)).toBe('1 Termin')
    expect(t('common.eventsCount', 0)).toBe('keine Termine')
    expect(formatDate(day, 'dayTitle')).toBe('Montag, 19. Oktober 2026')
    expect(formatDate(day, 'weekdayShort')).toBe('Mo')
    i18n.global.locale.value = 'en'
  })
})
