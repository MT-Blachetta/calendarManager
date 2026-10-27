/**
 * Google-Calendar-like layout of overlapping events inside one calendar column.
 *
 * Events are grouped into clusters of transitively overlapping events; inside a cluster each event gets the
 * first free lane. Every event of a cluster shares the cluster's lane count (its width is 1/lanes) and expands
 * to the right over lanes that are free for its whole duration.
 */
export interface LayoutInput<T> {
  item: T
  start: number
  end: number
}

export interface LaidOut<T> extends LayoutInput<T> {
  lane: number
  lanes: number
  span: number
}

export function layoutColumn<T>(inputs: LayoutInput<T>[], minDuration = 0): LaidOut<T>[] {
  const sorted = inputs
    .map((input) => ({ ...input, layoutEnd: Math.max(input.end, input.start + minDuration) }))
    .sort((a, b) => a.start - b.start || b.layoutEnd - a.layoutEnd)

  const result: LaidOut<T>[] = []
  let cluster: (typeof sorted[number] & { lane: number })[] = []
  let laneEnds: number[] = []
  let clusterEnd = -Infinity

  const flush = () => {
    const lanes = laneEnds.length
    for (const event of cluster) {
      let span = 1
      while (event.lane + span < lanes) {
        const nextLane = event.lane + span
        const blocked = cluster.some(
          (other) =>
            other.lane === nextLane && other.start < event.layoutEnd && event.start < other.layoutEnd,
        )
        if (blocked) break
        span++
      }
      result.push({ item: event.item, start: event.start, end: event.end, lane: event.lane, lanes, span })
    }
    cluster = []
    laneEnds = []
  }

  for (const event of sorted) {
    if (event.start >= clusterEnd && cluster.length > 0) flush()
    let lane = laneEnds.findIndex((end) => end <= event.start)
    if (lane === -1) {
      lane = laneEnds.length
      laneEnds.push(event.layoutEnd)
    } else {
      laneEnds[lane] = event.layoutEnd
    }
    cluster.push({ ...event, lane })
    clusterEnd = cluster.length === 1 ? event.layoutEnd : Math.max(clusterEnd, event.layoutEnd)
  }
  if (cluster.length > 0) flush()
  return result
}
