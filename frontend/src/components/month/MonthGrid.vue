<script setup lang="ts">
import { computed, nextTick, useTemplateRef, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { addDays, getISOWeek, isSameMonth, isWeekend } from 'date-fns'
import type { SummaryCalendar, SummaryDay } from '@/api/types'
import { gridRange, heatLevel, monthGrid } from '@/lib/month'
import { parseDayKey, toDayKey } from '@/lib/time'
import { formatDate } from '@/i18n/dates'
import { useHolidays } from '@/composables/useHolidays'

const props = defineProps<{
  month: Date
  /** Days with events, keyed by "yyyy-MM-dd". */
  days: Map<string, SummaryDay>
  calendarById: Map<number, SummaryCalendar>
  selectedDay: string | null
  loading?: boolean
}>()

const emit = defineEmits<{
  /** A day was selected (click, keyboard). */
  select: [day: string]
  /** A day should be opened in the day view (double click, Enter on the selected day). */
  open: [day: string]
}>()

const { t } = useI18n()
const { between, holidayName, holidayShortName } = useHolidays()

const MAX_DOTS = 5
const weekdays = computed(() => monthGrid(props.month)[0]!.map((d) => formatDate(d, 'weekdayShort')))
const holidays = computed(() => {
  const { from, to } = gridRange(props.month)
  return between(from, to)
})
const todayKey = toDayKey(new Date())

const weeks = computed(() =>
  monthGrid(props.month).map((week) => ({
    number: getISOWeek(week[0]!),
    days: week.map((date) => {
      const key = toDayKey(date)
      const summary = props.days.get(key)
      const count = summary?.eventCount ?? 0
      const calendars = (summary?.calendarIds ?? [])
        .map((id) => props.calendarById.get(id))
        .filter((c): c is SummaryCalendar => !!c)
      const holiday = holidays.value.get(key)
      const holidayLabel = holiday ? holidayName(holiday) : ''
      const holidayShort = holiday ? holidayShortName(holiday) : ''
      const events = count ? t('common.eventsCount', count) : t('month.noEvents')
      return {
        key,
        date,
        inMonth: isSameMonth(date, props.month),
        weekend: isWeekend(date),
        today: key === todayKey,
        count,
        level: heatLevel(count),
        calendars,
        holiday: holidayLabel,
        holidayShort,
        label: [formatDate(date, 'dayTitle'), holidayLabel, events].filter(Boolean).join(', '),
      }
    }),
  })),
)

/** Roving tab index: the selected day, else today if visible, else the 1st of the month. */
const focusKey = computed(() => {
  const keys = weeks.value.flatMap((w) => w.days.map((d) => d.key))
  if (props.selectedDay && keys.includes(props.selectedDay)) return props.selectedDay
  if (keys.includes(todayKey) && isSameMonth(new Date(), props.month)) return todayKey
  return toDayKey(props.month)
})

const grid = useTemplateRef<HTMLElement>('grid')

function onKeydown(event: KeyboardEvent, key: string) {
  const offsets: Record<string, number> = { ArrowLeft: -1, ArrowRight: 1, ArrowUp: -7, ArrowDown: 7 }
  const offset = offsets[event.key]
  if (offset !== undefined) {
    event.preventDefault()
    event.stopPropagation()
    const next = toDayKey(addDays(parseDayKey(key), offset))
    emit('select', next)
    // the parent may switch months; focus the new cell after re-render
    nextTick(() => grid.value?.querySelector<HTMLElement>(`[data-day="${next}"]`)?.focus())
  } else if (event.key === 'Enter') {
    event.preventDefault()
    event.stopPropagation()
    if (props.selectedDay === key) emit('open', key)
    else emit('select', key)
  }
}

watch(
  () => props.selectedDay,
  (day) => {
    if (day && grid.value?.contains(document.activeElement)) {
      nextTick(() => grid.value?.querySelector<HTMLElement>(`[data-day="${day}"]`)?.focus())
    }
  },
)
</script>

<template>
  <div ref="grid" class="month-grid" :class="{ loading }" role="grid" :aria-label="formatDate(month, 'monthTitle')">
    <div class="head" role="row">
      <div class="week-no" role="columnheader" :title="t('month.weekTitle')">{{ t('month.weekShort') }}</div>
      <div
        v-for="(name, i) in weekdays"
        :key="name"
        role="columnheader"
        class="weekday"
        :class="{ weekend: i >= 5 }"
      >
        {{ name }}
      </div>
    </div>
    <div v-for="week in weeks" :key="week.number + week.days[0]!.key" class="week" role="row">
      <div class="week-no tabular" role="rowheader">{{ week.number }}</div>
      <div v-for="day in week.days" :key="day.key" role="gridcell" :aria-selected="selectedDay === day.key">
        <button
          type="button"
          class="day"
          :class="[
            `level-${day.level}`,
            {
              outside: !day.inMonth,
              weekend: day.weekend,
              today: day.today,
              selected: selectedDay === day.key,
              'has-events': day.count > 0,
              holiday: !!day.holiday,
            },
          ]"
          :data-day="day.key"
          :tabindex="day.key === focusKey ? 0 : -1"
          :aria-label="day.label"
          @click="emit('select', day.key)"
          @dblclick="emit('open', day.key)"
          @keydown="onKeydown($event, day.key)"
        >
          <span class="date tabular">{{ day.date.getDate() }}</span>
          <span v-if="day.holiday" class="holiday-name" :title="day.holiday">{{ day.holidayShort }}</span>
          <span v-if="day.count" class="count tabular">
            {{ day.count }}<span class="count-label">{{ t('month.events', day.count) }}</span>
          </span>
          <span v-if="day.calendars.length" class="dots" aria-hidden="true">
            <span
              v-for="cal in day.calendars.slice(0, MAX_DOTS)"
              :key="cal.id"
              class="dot"
              :style="{ '--dot': cal.color }"
              :title="cal.name"
            />
            <span v-if="day.calendars.length > MAX_DOTS" class="more">+{{ day.calendars.length - MAX_DOTS }}</span>
          </span>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.month-grid {
  --wk: 40px;
  display: grid;
  grid-template-rows: auto repeat(6, minmax(78px, 1fr));
  height: 100%;
  min-height: 520px;
  transition: opacity 150ms;
}

.month-grid.loading {
  opacity: 0.6;
}

.head,
.week {
  display: grid;
  grid-template-columns: var(--wk) repeat(7, minmax(0, 1fr));
}

.weekday,
.head .week-no {
  padding: 10px 10px 8px;
  font-size: 11.5px;
  font-weight: 650;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--app-text-muted);
  border-bottom: 1px solid var(--app-border);
}

.weekday.weekend {
  color: color-mix(in srgb, var(--app-text-muted) 70%, var(--p-primary-500));
}

.week-no {
  display: flex;
  justify-content: center;
  padding-top: 10px;
  font-size: 11px;
  color: var(--app-text-muted);
  border-right: 1px solid var(--app-border);
  border-bottom: 1px solid var(--app-border-subtle);
}

.head .week-no {
  padding-left: 0;
  padding-right: 0;
}

.week > [role='gridcell'] {
  min-width: 0;
  border-right: 1px solid var(--app-border-subtle);
  border-bottom: 1px solid var(--app-border-subtle);
}

.day {
  --tint: 0%;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  width: 100%;
  height: 100%;
  padding: 8px;
  border: 0;
  background: color-mix(in srgb, var(--p-primary-500) var(--tint), var(--app-panel));
  color: var(--app-text);
  font: inherit;
  text-align: left;
  cursor: pointer;
  outline: none;
  transition:
    background-color 120ms,
    box-shadow 120ms;
}

.day.has-events::before {
  content: '';
  position: absolute;
  inset: 0 0 auto;
  height: 3px;
  background: var(--p-primary-500);
}

.day.outside.has-events::before {
  opacity: 0.35;
}

.day.weekend:not(.has-events),
.day.holiday:not(.has-events) {
  background: color-mix(in srgb, var(--app-text-muted) 4%, var(--app-panel));
}

.day.level-1 {
  --tint: var(--heat-1);
}
.day.level-2 {
  --tint: var(--heat-2);
}
.day.level-3 {
  --tint: var(--heat-3);
}
.day.level-4 {
  --tint: var(--heat-4);
}

.day:hover {
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--p-primary-500) 45%, transparent);
}

.day:focus-visible {
  box-shadow: inset 0 0 0 2px var(--app-accent);
}

.day.selected {
  box-shadow: inset 0 0 0 2px var(--app-accent);
}

.day.outside {
  color: var(--app-text-muted);
}

.day.outside.has-events {
  --tint: calc(var(--heat-1) / 2);
}

.day.outside .dots,
.day.outside .count {
  opacity: 0.6;
}

.holiday-name {
  max-width: 100%;
  margin-top: -2px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  font-size: 11px;
  font-weight: 600;
  line-height: 1.25;
  color: var(--app-holiday);
  overflow-wrap: break-word;
  hyphens: auto;
}

.day.outside .holiday-name {
  opacity: 0.6;
}

.date {
  flex: none;
  display: inline-grid;
  place-items: center;
  min-width: 26px;
  height: 26px;
  margin: -3px 0 0 -5px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
}

.has-events .date {
  font-weight: 750;
}

.holiday .date {
  color: var(--app-holiday);
}

.today .date {
  background: var(--app-accent);
  color: #fff;
}

.count {
  font-size: 12px;
  font-weight: 650;
  color: var(--heat-count);
}

.count-label {
  font-weight: 500;
  margin-left: 0.3em;
}

.dots {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 3px;
  margin-top: auto;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--dot);
  box-shadow: 0 0 0 1.5px var(--app-panel);
}

.more {
  font-size: 10px;
  color: var(--app-text-muted);
  margin-left: 2px;
}

@media (max-width: 640px) {
  .month-grid {
    --wk: 28px;
    grid-template-rows: auto repeat(6, minmax(58px, 1fr));
    min-height: 0;
  }
  .day {
    padding: 6px 4px;
    align-items: center;
  }
  .date {
    margin: 0;
  }
  .count-label,
  .weekday {
    font-size: 10px;
  }
  .count-label,
  .holiday-name {
    display: none;
  }
  .dots {
    justify-content: center;
  }
}
</style>
