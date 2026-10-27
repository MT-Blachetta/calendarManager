import { describe, expect, it } from 'vitest'
import { gridRange, heatLevel, monthGrid, parseMonthKey, toMonthKey } from '../month'
import { toDayKey } from '../time'

describe('month grid', () => {
  it('parses and formats month keys', () => {
    expect(toMonthKey(parseMonthKey('2026-10'))).toBe('2026-10')
    expect(parseMonthKey('2026-10').getDate()).toBe(1)
    expect(toMonthKey(parseMonthKey('garbage'))).toBe(toMonthKey(new Date()))
  })

  it('starts on the Monday on or before the 1st and spans 6 weeks', () => {
    const weeks = monthGrid(parseMonthKey('2026-10')) // 1 Oct 2026 is a Thursday
    expect(weeks).toHaveLength(6)
    expect(weeks.every((w) => w.length === 7)).toBe(true)
    expect(toDayKey(weeks[0]![0]!)).toBe('2026-09-28')
    expect(toDayKey(weeks[5]![6]!)).toBe('2026-11-08')
    expect(weeks.flat().every((d, i, all) => i === 0 || d.getDay() === (all[i - 1]!.getDay() + 1) % 7)).toBe(true)
  })

  it('handles months starting on a Monday and across DST changes', () => {
    const june = monthGrid(parseMonthKey('2026-06')) // 1 Jun 2026 is a Monday
    expect(toDayKey(june[0]![0]!)).toBe('2026-06-01')
    const march = monthGrid(parseMonthKey('2026-03')) // DST starts 29 Mar in Europe
    const keys = march.flat().map(toDayKey)
    expect(new Set(keys).size).toBe(42)
    expect(keys).toContain('2026-03-29')
    expect(keys).toContain('2026-03-30')
  })

  it('exposes the visible range', () => {
    const { from, to } = gridRange(parseMonthKey('2026-02'))
    expect(toDayKey(from)).toBe('2026-01-26')
    expect(toDayKey(to)).toBe('2026-03-08')
  })

  it('maps event counts to highlight levels', () => {
    expect([0, 1, 2, 3, 4, 7, 8, 50].map(heatLevel)).toEqual([0, 1, 2, 2, 3, 3, 4, 4])
  })
})
