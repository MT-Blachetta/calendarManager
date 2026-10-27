import { describe, expect, it } from 'vitest'
import { easterSunday, germanHolidays, holidaysBetween } from '../holidays'

const ids = (year: number, state?: Parameters<typeof germanHolidays>[1]) =>
  germanHolidays(year, state).map((h) => `${h.date} ${h.id}`)

describe('German public holidays', () => {
  it('computes Easter Sunday', () => {
    expect(easterSunday(2024)).toEqual(new Date(2024, 2, 31))
    expect(easterSunday(2025)).toEqual(new Date(2025, 3, 20))
    expect(easterSunday(2026)).toEqual(new Date(2026, 3, 5))
    expect(easterSunday(2027)).toEqual(new Date(2027, 2, 28))
    expect(easterSunday(2038)).toEqual(new Date(2038, 3, 25))
  })

  it('lists the nine nationwide holidays of 2026', () => {
    expect(ids(2026)).toEqual([
      '2026-01-01 newYear',
      '2026-04-03 goodFriday',
      '2026-04-06 easterMonday',
      '2026-05-01 labourDay',
      '2026-05-14 ascension',
      '2026-05-25 whitMonday',
      '2026-10-03 unityDay',
      '2026-12-25 christmasDay',
      '2026-12-26 boxingDay',
    ])
    expect(germanHolidays(2026).every((h) => h.nationwide)).toBe(true)
  })

  it('adds the holidays of a federal state', () => {
    const bavaria = ids(2026, 'BY')
    expect(bavaria).toContain('2026-01-06 epiphany')
    expect(bavaria).toContain('2026-06-04 corpusChristi')
    expect(bavaria).toContain('2026-08-15 assumption')
    expect(bavaria).toContain('2026-11-01 allSaints')
    expect(bavaria).not.toContain('2026-10-31 reformationDay')
    expect(ids(2026, 'SN')).toContain('2026-11-18 repentanceDay')
    expect(ids(2026, 'BB')).toEqual(expect.arrayContaining(['2026-04-05 easterSunday', '2026-05-24 whitSunday']))
    expect(ids(2026, 'TH')).toContain('2026-09-20 childrensDay')
  })

  it('respects the years in which a rule applies', () => {
    expect(ids(2017)).toContain('2017-10-31 reformationDay') // 500th anniversary, nationwide
    expect(ids(2016, 'NI')).not.toContain('2016-10-31 reformationDay')
    expect(ids(2018, 'NI')).toContain('2018-10-31 reformationDay')
    expect(ids(2022, 'MV')).not.toContain('2022-03-08 womensDay')
    expect(ids(2023, 'MV')).toContain('2023-03-08 womensDay')
    expect(ids(2025, 'BE')).toContain('2025-05-08 liberationDay')
    expect(ids(2026, 'BE')).not.toContain('2026-05-08 liberationDay')
  })

  it('selects holidays in a range across years', () => {
    const map = holidaysBetween(new Date(2026, 11, 20), new Date(2027, 0, 10), 'DE-BW')
    expect([...map.keys()]).toEqual(['2026-12-25', '2026-12-26', '2027-01-01', '2027-01-06'])
    expect(holidaysBetween(new Date(2026, 0, 1), new Date(2026, 11, 31), 'off').size).toBe(0)
  })
})
