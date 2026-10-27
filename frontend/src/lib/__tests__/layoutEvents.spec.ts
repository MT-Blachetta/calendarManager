import { describe, expect, it } from 'vitest'
import { layoutColumn } from '../layoutEvents'

const ev = (id: string, start: number, end: number) => ({ item: id, start, end })
const byId = <T extends { item: string }>(list: T[]) =>
  Object.fromEntries(list.map((e) => [e.item, e]))

describe('layoutColumn', () => {
  it('keeps non-overlapping events full width', () => {
    const r = byId(layoutColumn([ev('a', 0, 60), ev('b', 60, 120)]))
    expect(r.a).toMatchObject({ lane: 0, lanes: 1, span: 1 })
    expect(r.b).toMatchObject({ lane: 0, lanes: 1, span: 1 })
  })

  it('puts overlapping events side by side', () => {
    const r = byId(layoutColumn([ev('a', 0, 120), ev('b', 30, 90), ev('c', 60, 150)]))
    expect(r.a).toMatchObject({ lane: 0, lanes: 3 })
    expect(r.b).toMatchObject({ lane: 1, lanes: 3 })
    expect(r.c).toMatchObject({ lane: 2, lanes: 3 })
  })

  it('reuses freed lanes and expands into free lanes', () => {
    // a: 0-60 | b: 0-200 ; c: 60-120 reuses lane 0 ; d: 130-200 lane 0
    const r = byId(layoutColumn([ev('a', 0, 60), ev('b', 0, 200), ev('c', 60, 120), ev('d', 130, 200)]))
    expect(r.b).toMatchObject({ lane: 0, lanes: 2, span: 1 })
    expect(r.a).toMatchObject({ lane: 1, lanes: 2 })
    expect(r.c).toMatchObject({ lane: 1, lanes: 2 })
  })

  it('expands an event over lanes that stay free', () => {
    // b and c overlap a but not each other; d only overlaps a -> d spans lanes 1..2? (only 2 lanes exist)
    const r = byId(layoutColumn([ev('a', 0, 300), ev('b', 0, 100), ev('c', 0, 50), ev('d', 200, 300)]))
    expect(r.a).toMatchObject({ lane: 0, lanes: 3 })
    expect(r.d).toMatchObject({ lane: 1, lanes: 3, span: 2 })
    expect(r.c).toMatchObject({ lane: 2, span: 1 })
  })

  it('separates clusters', () => {
    const r = byId(layoutColumn([ev('a', 0, 60), ev('b', 30, 90), ev('c', 100, 160)]))
    expect(r.c).toMatchObject({ lane: 0, lanes: 1 })
  })

  it('applies a minimum duration for layout', () => {
    const r = byId(layoutColumn([ev('a', 0, 5), ev('b', 10, 30)], 20))
    expect(r.a!.lanes).toBe(2)
  })
})
