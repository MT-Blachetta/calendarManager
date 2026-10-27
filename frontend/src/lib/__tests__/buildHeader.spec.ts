import { describe, expect, it } from 'vitest'
import { buildHeader, type HeaderCalendar } from '../buildHeader'

const ent = { id: 1, name: 'Entertainment' }
const tv = { id: 2, name: 'TV' }
const cinema = { id: 5, name: 'Cinema' }
const health = { id: 6, name: 'Health' }

const calendars: HeaderCalendar[] = [
  { id: 3, name: 'ARD', path: [ent, tv] },
  { id: 4, name: 'ZDF', path: [ent, tv] },
  { id: 7, name: 'Cinema City', path: [ent, cinema] },
  { id: 8, name: 'Dentist', path: [health] },
  { id: 9, name: 'Club', path: [] },
]

describe('buildHeader', () => {
  it('creates one row per hierarchy level plus the calendar row', () => {
    expect(buildHeader(calendars).rows).toBe(3)
    expect(buildHeader([]).rows).toBe(1)
  })

  it('spans group cells over their descendant columns', () => {
    const groups = buildHeader(calendars).cells.filter((c) => c.kind === 'group')
    expect(groups.map((g) => [g.label, g.row, g.col, g.colSpan])).toEqual([
      ['Entertainment', 1, 1, 3],
      ['Health', 1, 4, 1],
      ['TV', 2, 1, 2],
      ['Cinema', 2, 3, 1],
    ])
  })

  it('lets shallow calendars span down to the last row', () => {
    const cells = buildHeader(calendars).cells.filter((c) => c.kind === 'calendar')
    expect(cells.map((c) => [c.label, c.row, c.rowSpan, c.col])).toEqual([
      ['ARD', 3, 1, 1],
      ['ZDF', 3, 1, 2],
      ['Cinema City', 3, 1, 3],
      ['Dentist', 2, 2, 4],
      ['Club', 1, 3, 5],
    ])
  })

  it('does not merge equally named groups with different ancestry', () => {
    const cells = buildHeader([
      { id: 10, name: 'A', path: [{ id: 1, name: 'X' }, { id: 2, name: 'Sub' }] },
      { id: 11, name: 'B', path: [{ id: 3, name: 'Y' }, { id: 4, name: 'Sub' }] },
    ]).cells.filter((c) => c.kind === 'group' && c.row === 2)
    expect(cells).toHaveLength(2)
  })
})
