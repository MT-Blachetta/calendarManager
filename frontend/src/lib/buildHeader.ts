/**
 * Builds the hierarchical column header of the day grid from the visible calendars (in column order) and
 * their ancestor paths: every ancestor category becomes a cell spanning the columns of its visible
 * descendants; calendars at a shallower depth span down to the last header row.
 */
export interface HeaderCalendar {
  id: number
  name: string
  path: { id: number; name: string }[]
}

export interface HeaderCell {
  key: string
  label: string
  kind: 'group' | 'calendar'
  /** Category id (group or calendar). */
  id: number
  /** 1-based grid row / column (columns exclude the time gutter). */
  row: number
  rowSpan: number
  col: number
  colSpan: number
  /** Index of the calendar column (calendar cells only). */
  index?: number
}

export interface HeaderLayout {
  rows: number
  cells: HeaderCell[]
}

export function buildHeader(calendars: HeaderCalendar[]): HeaderLayout {
  const groupDepth = calendars.reduce((max, c) => Math.max(max, c.path.length), 0)
  const rows = groupDepth + 1
  const cells: HeaderCell[] = []

  for (let level = 0; level < groupDepth; level++) {
    let i = 0
    while (i < calendars.length) {
      const group = calendars[i]!.path[level]
      if (!group) {
        i++
        continue
      }
      let j = i + 1
      while (j < calendars.length && sameAncestry(calendars[i]!, calendars[j]!, level)) j++
      cells.push({
        key: `g-${level}-${group.id}-${i}`,
        label: group.name,
        kind: 'group',
        id: group.id,
        row: level + 1,
        rowSpan: 1,
        col: i + 1,
        colSpan: j - i,
      })
      i = j
    }
  }

  calendars.forEach((calendar, index) => {
    cells.push({
      key: `c-${calendar.id}`,
      label: calendar.name,
      kind: 'calendar',
      id: calendar.id,
      row: calendar.path.length + 1,
      rowSpan: rows - calendar.path.length,
      col: index + 1,
      colSpan: 1,
      index,
    })
  })
  return { rows, cells }
}

/** Both calendars share the same ancestors up to and including `level`. */
function sameAncestry(a: HeaderCalendar, b: HeaderCalendar, level: number): boolean {
  for (let l = 0; l <= level; l++) {
    if (a.path[l]?.id === undefined || a.path[l]?.id !== b.path[l]?.id) return false
  }
  return true
}
