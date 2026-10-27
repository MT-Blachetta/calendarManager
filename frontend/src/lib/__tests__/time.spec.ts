import { describe, expect, it } from 'vitest'
import { formatMinutes, minutesOfDay, slotLabels, SLOT_COUNT, visibleSpan } from '../time'

const day = new Date(2026, 9, 18)
const at = (d: number, h: number, m = 0) => new Date(2026, 9, d, h, m)

describe('time axis', () => {
  it('labels 6:00 to 0:00 in 30 minute steps', () => {
    const labels = slotLabels()
    expect(labels).toHaveLength(SLOT_COUNT + 1)
    expect(labels[0]).toBe('6:00')
    expect(labels[1]).toBe('6:30')
    expect(labels[labels.length - 2]).toBe('23:30')
    expect(labels[labels.length - 1]).toBe('0:00')
    expect(formatMinutes(9 * 60 + 5)).toBe('9:05')
  })

  it('computes wall-clock minutes relative to the day', () => {
    expect(minutesOfDay(at(18, 20, 15), day)).toBe(1215)
    expect(minutesOfDay(at(17, 23), day)).toBe(-60)
    expect(minutesOfDay(at(19, 1), day)).toBe(1500)
  })

  it('clips events to the visible window', () => {
    expect(visibleSpan(at(18, 20, 15), at(18, 21, 45), day)).toEqual({
      start: 1215,
      end: 1305,
      clippedTop: false,
      clippedBottom: false,
    })
    expect(visibleSpan(at(18, 5), at(18, 7), day)).toMatchObject({ start: 360, clippedTop: true })
    expect(visibleSpan(at(18, 23), at(19, 1), day)).toMatchObject({ end: 1440, clippedBottom: true })
    expect(visibleSpan(at(18, 4), at(18, 6), day)).toBeNull()
    expect(visibleSpan(at(19, 0), at(19, 1), day)).toBeNull()
  })
})
