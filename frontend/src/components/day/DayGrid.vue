<script setup lang="ts">
import { computed, nextTick, ref, useTemplateRef, watch } from 'vue'
import { useElementSize, useIntervalFn, useNow } from '@vueuse/core'
import { useI18n } from 'vue-i18n'
import Popover from 'primevue/popover'
import DayEventCard from './DayEventCard.vue'
import type { CalendarEvent, ResolvedCalendar, Tag } from '@/api/types'
import { buildHeader } from '@/lib/buildHeader'
import { layoutColumn } from '@/lib/layoutEvents'
import {
  atMinutes,
  DAY_END_MIN,
  DAY_START_MIN,
  formatMinutes,
  formatTimeRange,
  minutesOfDay,
  overlaps,
  slotLabels,
  SLOT_COUNT,
  SLOT_MINUTES,
  snapDown,
  toDayKey,
  visibleSpan,
} from '@/lib/time'
import { useEventEditor } from '@/stores/eventEditor'

const props = defineProps<{
  day: Date
  calendars: ResolvedCalendar[]
  events: CalendarEvent[]
  tagById: Map<number, Tag>
  slotHeight: number
}>()

const editor = useEventEditor()
const { t } = useI18n()

// ------------------------------------------------------------------ geometry
const GUTTER = 64
const COL_MIN = 168
const GROUP_ROW = 30
const CALENDAR_ROW = 58
const PAD = 12 // vertical padding so the 6:00 / 0:00 labels are fully visible

const scroller = useTemplateRef<HTMLElement>('scroller')
const { width: scrollerWidth } = useElementSize(scroller)

const header = computed(() => buildHeader(props.calendars))
const groupRows = computed(() => header.value.rows - 1)
const headerHeight = computed(() => groupRows.value * GROUP_ROW + CALENDAR_ROW)
const colWidth = computed(() => {
  const n = Math.max(props.calendars.length, 1)
  return Math.max(COL_MIN, Math.floor((scrollerWidth.value - GUTTER) / n))
})
const bodyHeight = computed(() => SLOT_COUNT * props.slotHeight)
const pxPerMinute = computed(() => props.slotHeight / SLOT_MINUTES)

const gridStyle = computed(() => ({
  gridTemplateColumns: `${GUTTER}px repeat(${props.calendars.length}, ${colWidth.value}px)`,
  gridTemplateRows: `${groupRows.value > 0 ? `repeat(${groupRows.value}, ${GROUP_ROW}px) ` : ''}${CALENDAR_ROW}px ${bodyHeight.value + 2 * PAD}px`,
  '--slot': `${props.slotHeight}px`,
  '--pad': `${PAD}px`,
}))
const bodyRow = computed(() => header.value.rows + 1)

function y(minutes: number) {
  return PAD + (minutes - DAY_START_MIN) * pxPerMinute.value
}

const labels = slotLabels()

// ------------------------------------------------------------------ events per column
interface ParsedEvent {
  event: CalendarEvent
  start: Date
  end: Date
  startMin: number
  endMin: number
}

const parsed = computed<ParsedEvent[]>(() =>
  props.events.map((event) => {
    const start = new Date(event.start)
    const end = new Date(event.end)
    return {
      event,
      start,
      end,
      startMin: minutesOfDay(start, props.day),
      endMin: minutesOfDay(end, props.day),
    }
  }),
)

const columns = computed(() =>
  props.calendars.map((calendar) => {
    const own = parsed.value.filter((p) => p.event.categoryId === calendar.id)
    const visible = own
      .map((p) => ({ p, span: visibleSpan(p.start, p.end, props.day) }))
      .filter((x): x is { p: ParsedEvent; span: NonNullable<typeof x.span> } => x.span !== null)
    const earlier = own.filter((p) => p.endMin <= DAY_START_MIN)
    const laidOut = layoutColumn(
      visible.map((v) => ({ item: v, start: v.span.start, end: v.span.end })),
      15,
    )
    return { calendar, laidOut, earlier }
  }),
)

// ------------------------------------------------------------------ hover comparison band
const hovered = ref<ParsedEvent | null>(null)

const band = computed(() => {
  const h = hovered.value
  if (!h) return null
  const start = Math.max(h.startMin, DAY_START_MIN)
  const end = Math.min(h.endMin, DAY_END_MIN)
  return {
    top: y(start),
    height: Math.max((end - start) * pxPerMinute.value, 2),
    startLabel: formatMinutes(start),
    endLabel: formatMinutes(end),
  }
})

function relation(p: ParsedEvent): 'hovered' | 'concurrent' | 'dimmed' | null {
  const h = hovered.value
  if (!h) return null
  if (h.event.id === p.event.id) return 'hovered'
  return overlaps(h.startMin, h.endMin, p.startMin, p.endMin) ? 'concurrent' : 'dimmed'
}

// ------------------------------------------------------------------ now indicator
const now = useNow({ scheduler: (cb) => useIntervalFn(cb, 30_000) })
const nowTop = computed(() => {
  if (toDayKey(now.value) !== toDayKey(props.day)) return null
  const m = minutesOfDay(now.value, props.day)
  return m < DAY_START_MIN || m > DAY_END_MIN ? null : y(m)
})

// ------------------------------------------------------------------ click / drag to create
const selection = ref<{ index: number; anchor: number; current: number } | null>(null)
let pointerMoved = false

function minuteAt(event: PointerEvent, column: HTMLElement): number {
  const rect = column.getBoundingClientRect()
  const m = DAY_START_MIN + (event.clientY - rect.top - PAD) / pxPerMinute.value
  return Math.min(Math.max(snapDown(m), DAY_START_MIN), DAY_END_MIN - SLOT_MINUTES)
}

function onPointerDown(event: PointerEvent, index: number) {
  if (event.button !== 0 || (event.target as HTMLElement).closest('.event, .earlier-chip')) return
  const column = event.currentTarget as HTMLElement
  column.setPointerCapture(event.pointerId)
  const m = minuteAt(event, column)
  pointerMoved = false
  selection.value = { index, anchor: m, current: m }
}

function onPointerMove(event: PointerEvent) {
  if (!selection.value) return
  const m = minuteAt(event, event.currentTarget as HTMLElement)
  if (m !== selection.value.current) {
    pointerMoved = true
    selection.value = { ...selection.value, current: m }
  }
}

const selectionRange = computed(() => {
  const s = selection.value
  if (!s) return null
  const start = Math.min(s.anchor, s.current)
  const end = Math.max(s.anchor, s.current) + SLOT_MINUTES
  return { index: s.index, start, end }
})

function onPointerUp() {
  const range = selectionRange.value
  selection.value = null
  if (!range) return
  const calendar = props.calendars[range.index]
  if (!calendar) return
  const end = pointerMoved ? range.end : Math.min(range.start + 60, DAY_END_MIN)
  editor.create({
    categoryId: calendar.id,
    start: atMinutes(props.day, range.start),
    end: atMinutes(props.day, end),
  })
}

function onPointerCancel() {
  selection.value = null
}

// ------------------------------------------------------------------ earlier-events popover
const earlierPopover = useTemplateRef<InstanceType<typeof Popover>>('earlierPopover')
const earlierList = ref<ParsedEvent[]>([])

function showEarlier(domEvent: Event, list: ParsedEvent[]) {
  earlierList.value = list
  earlierPopover.value?.toggle(domEvent)
}

function openEvent(p: ParsedEvent) {
  earlierPopover.value?.hide()
  editor.show(p.event)
}

// ------------------------------------------------------------------ header helpers
function calendarTags(calendar: ResolvedCalendar) {
  return calendar.effectiveTagIds.map((id) => props.tagById.get(id)).filter((t): t is Tag => !!t)
}

function calendarTooltip(calendar: ResolvedCalendar) {
  return [...calendar.path.map((p) => p.name), calendar.name].join(' › ')
}

// ------------------------------------------------------------------ initial scroll position
function scrollToInteresting() {
  const el = scroller.value
  if (!el) return
  const target =
    nowTop.value !== null ? nowTop.value - 2 * props.slotHeight : y(7 * 60) // 7:00 by default
  el.scrollTop = Math.max(target, 0)
}

watch(
  () => toDayKey(props.day),
  () => nextTick(scrollToInteresting),
  { immediate: true },
)

defineExpose({ scrollToInteresting })
</script>

<template>
  <div ref="scroller" class="scroller">
    <div class="grid" :style="gridStyle" @pointerleave="hovered = null">
      <!-- corner -->
      <div class="corner" :style="{ gridRow: `1 / span ${header.rows}`, height: `${headerHeight}px` }">
        <span class="corner-label">{{ t('common.calendarsCount', calendars.length) }}</span>
      </div>

      <!-- hierarchical header -->
      <template v-for="cell in header.cells" :key="cell.key">
        <div
          v-if="cell.kind === 'group'"
          class="hcell group"
          :class="`level-${cell.row}`"
          :style="{
            gridRow: `${cell.row} / span ${cell.rowSpan}`,
            gridColumn: `${cell.col + 1} / span ${cell.colSpan}`,
            top: `${(cell.row - 1) * GROUP_ROW}px`,
          }"
        >
          <span class="group-label" :style="{ left: `${GUTTER + 8}px` }">
            <i class="pi pi-folder" />{{ cell.label }}
          </span>
        </div>
        <div
          v-else
          v-tooltip.bottom="calendarTooltip(calendars[cell.index!]!)"
          class="hcell calendar"
          :style="{
            gridRow: `${cell.row} / span ${cell.rowSpan}`,
            gridColumn: `${cell.col + 1} / span 1`,
            top: `${(cell.row - 1) * GROUP_ROW}px`,
            '--c': calendars[cell.index!]!.color,
          }"
        >
          <div class="calendar-name">
            <span class="color-dot" :style="{ '--dot': calendars[cell.index!]!.color }" />
            <span class="name-text">{{ cell.label }}</span>
          </div>
          <div class="calendar-tags">
            <span
              v-for="tag in calendarTags(calendars[cell.index!]!).slice(0, 3)"
              :key="tag.id"
              class="tag-chip"
              :style="{ '--t': tag.color }"
              >{{ tag.name }}</span
            >
            <span
              v-if="calendarTags(calendars[cell.index!]!).length > 3"
              class="tag-chip more"
              >+{{ calendarTags(calendars[cell.index!]!).length - 3 }}</span
            >
          </div>
        </div>
      </template>

      <!-- time gutter -->
      <div class="gutter" :style="{ gridRow: bodyRow }">
        <span
          v-for="(label, i) in labels"
          :key="label + i"
          class="time-label tabular"
          :class="{ hour: i % 2 === 0 }"
          :style="{ top: `${PAD + i * slotHeight}px` }"
          >{{ label }}</span
        >
      </div>

      <!-- calendar columns -->
      <div
        v-for="(column, index) in columns"
        :key="column.calendar.id"
        class="column"
        :style="{ gridRow: bodyRow, gridColumn: index + 2 }"
        @pointerdown="onPointerDown($event, index)"
        @pointermove="onPointerMove"
        @pointerup="onPointerUp"
        @pointercancel="onPointerCancel"
      >
        <button
          v-if="column.earlier.length"
          type="button"
          class="earlier-chip"
          @click="showEarlier($event, column.earlier)"
        >
          <i class="pi pi-arrow-up" /> {{ t('day.beforeStart', { n: column.earlier.length }) }}
        </button>

        <div
          v-if="selectionRange && selectionRange.index === index"
          class="selection"
          :style="{
            top: `${y(selectionRange.start)}px`,
            height: `${(selectionRange.end - selectionRange.start) * pxPerMinute}px`,
          }"
        >
          {{ formatTimeRange(atMinutes(day, selectionRange.start), atMinutes(day, selectionRange.end)) }}
        </div>

        <DayEventCard
          v-for="slot in column.laidOut"
          :key="slot.item.p.event.id"
          :event="slot.item.p.event"
          :start="slot.item.p.start"
          :end="slot.item.p.end"
          :color="column.calendar.color"
          :clipped-top="slot.item.span.clippedTop"
          :clipped-bottom="slot.item.span.clippedBottom"
          :relation="relation(slot.item.p)"
          :style="{
            top: `${y(slot.start)}px`,
            height: `${Math.max((slot.end - slot.start) * pxPerMinute, 18)}px`,
            left: `calc(${(slot.lane / slot.lanes) * 100}% + 3px)`,
            width: `calc(${(slot.span / slot.lanes) * 100}% - 6px)`,
          }"
          @mouseenter="hovered = slot.item.p"
          @mouseleave="hovered = null"
          @focus="hovered = slot.item.p"
          @blur="hovered = null"
          @click="editor.show(slot.item.p.event)"
        />
      </div>

      <!-- comparison band across all calendars -->
      <div class="overlay band-layer" :style="{ gridRow: bodyRow }">
        <div v-if="band" class="band" :style="{ top: `${band.top}px`, height: `${band.height}px` }" />
      </div>

      <!-- now indicator -->
      <div v-if="nowTop !== null" class="overlay now-layer" :style="{ gridRow: bodyRow }">
        <div class="now-line" :style="{ top: `${nowTop}px` }" />
      </div>
      <div v-if="band" class="band-label tabular" :style="{ gridRow: bodyRow, top: 0 }">
        <span class="start" :style="{ top: `${band.top}px` }">{{ band.startLabel }}</span>
        <span class="end" :style="{ top: `${band.top + band.height}px` }">{{ band.endLabel }}</span>
      </div>
    </div>

    <Popover ref="earlierPopover">
      <div class="earlier-list">
        <div class="earlier-title">{{ t('day.beforeStartTitle') }}</div>
        <button
          v-for="p in earlierList"
          :key="p.event.id"
          type="button"
          class="earlier-item"
          @click="openEvent(p)"
        >
          <span class="tabular muted">{{ formatTimeRange(p.start, p.end) }}</span>
          <span>{{ p.event.title }}</span>
        </button>
      </div>
    </Popover>
  </div>
</template>

<style scoped>
.scroller {
  position: relative;
  height: 100%;
  overflow: auto;
  overscroll-behavior: contain;
  background: var(--app-panel);
}

.grid {
  display: grid;
  width: max-content;
  min-width: 100%;
  position: relative;
}

/* ---- header ---- */
.corner,
.hcell {
  position: sticky;
  z-index: 5;
  background: var(--grid-header-bg);
  border-bottom: 1px solid var(--app-border);
}

.corner {
  grid-column: 1;
  left: 0;
  top: 0;
  z-index: 6;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding-bottom: 8px;
  border-right: 1px solid var(--app-border);
}

.corner-label {
  font-size: 10.5px;
  color: var(--app-text-muted);
  text-align: center;
  line-height: 1.2;
}

.hcell.group {
  display: flex;
  align-items: center;
  border-left: 1px solid var(--app-border);
  font-size: 12px;
  font-weight: 600;
  color: var(--app-text-muted);
  letter-spacing: 0.02em;
  min-width: 0;
}

.hcell.group.level-1 {
  background: color-mix(in srgb, var(--p-primary-500) 7%, var(--grid-header-bg));
  color: var(--app-text);
}

.hcell.group.level-2 {
  background: color-mix(in srgb, var(--p-primary-500) 3.5%, var(--grid-header-bg));
}

.group-label {
  position: sticky;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 8px;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.group-label i {
  font-size: 11px;
  opacity: 0.7;
}

.hcell.calendar {
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  gap: 4px;
  padding: 6px 10px 7px;
  border-left: 1px solid var(--app-border);
  box-shadow: inset 0 -3px 0 var(--c);
  min-width: 0;
}

.calendar-name {
  display: flex;
  align-items: center;
  gap: 7px;
  font-weight: 650;
  font-size: 13px;
  min-width: 0;
}

.name-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.calendar-tags {
  display: flex;
  gap: 4px;
  overflow: hidden;
  min-height: 17px;
}

.tag-chip {
  flex: none;
  font-size: 10.5px;
  line-height: 15px;
  padding: 0 6px;
  border-radius: 999px;
  color: color-mix(in srgb, var(--t, #64748b) 75%, var(--app-text));
  background: color-mix(in srgb, var(--t, #64748b) 14%, transparent);
  border: 1px solid color-mix(in srgb, var(--t, #64748b) 30%, transparent);
  white-space: nowrap;
}

.tag-chip.more {
  --t: #64748b;
}

/* ---- gutter ---- */
.gutter {
  grid-column: 1;
  position: sticky;
  left: 0;
  z-index: 4;
  background: var(--grid-header-bg);
  border-right: 1px solid var(--app-border);
}

.time-label {
  position: absolute;
  right: 10px;
  transform: translateY(-50%);
  font-size: 11px;
  color: var(--app-text-muted);
  opacity: 0.75;
}

.time-label.hour {
  font-size: 12px;
  font-weight: 600;
  color: var(--app-text);
  opacity: 0.85;
}

/* ---- columns ---- */
.column {
  position: relative;
  border-left: 1px solid var(--app-border);
  cursor: cell;
  touch-action: pan-x pan-y;
  background-color: var(--app-panel);
  background-image:
    linear-gradient(to bottom, var(--grid-line) 1px, transparent 1px),
    linear-gradient(to bottom, var(--grid-line-half) 1px, transparent 1px);
  background-size:
    100% calc(var(--slot) * 2),
    100% var(--slot);
  background-position:
    0 var(--pad),
    0 var(--pad);
  background-repeat: repeat-y;
  background-clip: content-box;
  padding: var(--pad) 0;
}

.column:hover {
  background-color: color-mix(in srgb, var(--p-primary-500) 2%, var(--app-panel));
}

.selection {
  position: absolute;
  left: 3px;
  right: 3px;
  z-index: 2;
  border-radius: 6px;
  border: 2px dashed var(--app-accent);
  background: color-mix(in srgb, var(--p-primary-500) 12%, transparent);
  color: var(--app-accent);
  font-size: 11.5px;
  font-weight: 600;
  padding: 2px 6px;
  pointer-events: none;
}

.earlier-chip {
  position: absolute;
  top: 1px;
  right: 4px;
  z-index: 3;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font: inherit;
  font-size: 10.5px;
  padding: 0 7px;
  height: 18px;
  border-radius: 999px;
  border: 1px solid var(--app-border);
  background: var(--app-panel);
  color: var(--app-text-muted);
  cursor: pointer;
}

.earlier-chip i {
  font-size: 9px;
}

/* ---- overlays ---- */
.overlay {
  grid-column: 2 / -1;
  position: relative;
  pointer-events: none;
}

.band-layer {
  z-index: 1;
}

.band {
  position: absolute;
  left: 0;
  right: 0;
  background: color-mix(in srgb, var(--p-primary-500) 9%, transparent);
  border-top: 1px dashed color-mix(in srgb, var(--p-primary-500) 55%, transparent);
  border-bottom: 1px dashed color-mix(in srgb, var(--p-primary-500) 55%, transparent);
}

.band-label {
  grid-column: 1;
  position: sticky;
  left: 0;
  z-index: 4;
  pointer-events: none;
}

.band-label span {
  position: absolute;
  left: 6px;
  right: 6px;
  transform: translateY(-50%);
  font-size: 10px;
  font-weight: 650;
  text-align: center;
  color: #fff;
  background: var(--p-primary-600);
  border-radius: 4px;
  padding: 1px 2px;
  white-space: nowrap;
  overflow: hidden;
}

.now-layer {
  z-index: 3;
}

.now-line {
  position: absolute;
  left: 0;
  right: 0;
  height: 2px;
  background: var(--app-now);
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--app-now) 25%, transparent);
}

.now-line::before {
  content: '';
  position: absolute;
  left: -5px;
  top: -4px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--app-now);
}

/* ---- popover ---- */
.earlier-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 220px;
}

.earlier-title {
  font-size: 11px;
  font-weight: 650;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--app-text-muted);
  padding: 0 6px 4px;
}

.earlier-item {
  display: flex;
  gap: 10px;
  padding: 6px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.earlier-item:hover {
  background: var(--app-hover);
}
</style>
