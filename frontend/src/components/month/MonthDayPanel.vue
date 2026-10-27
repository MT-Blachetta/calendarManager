<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { addDays, endOfMonth, isSameMonth } from 'date-fns'
import Button from 'primevue/button'
import Skeleton from 'primevue/skeleton'
import { useEventSearch } from '@/api/queries'
import type { CalendarEvent, SummaryCalendar, SummaryDay } from '@/api/types'
import { useEventEditor } from '@/stores/eventEditor'
import { atMinutes, formatTimeRange, parseDayKey } from '@/lib/time'
import { formatDate } from '@/i18n/dates'
import { useHolidays } from '@/composables/useHolidays'

const props = defineProps<{
  month: Date
  selectedDay: string | null
  /** Calendar or group selected in the picker (null = all). */
  categoryId: number | null
  categoryLabel: string
  calendars: SummaryCalendar[]
  calendarById: Map<number, SummaryCalendar>
  days: Map<string, SummaryDay>
}>()

const emit = defineEmits<{ openDay: [day: string]; clear: [] }>()
const editor = useEventEditor()
const { t } = useI18n()
const { between, holidayOn, holidayName } = useHolidays()

// ---------------------------------------------------------------- selected day
const day = computed(() => (props.selectedDay ? parseDayKey(props.selectedDay) : null))
const calendarIds = computed(() => props.calendars.map((c) => c.id))

const eventsQuery = useEventSearch(
  computed(() => ({
    from: day.value ?? undefined,
    to: day.value ? addDays(day.value, 1) : undefined,
    // "All calendars" needs no filter; a selection is restricted to its calendars
    categoryIds: props.categoryId === null ? undefined : calendarIds.value,
    page: 0,
    size: 200,
    sort: 'start' as const,
    direction: 'asc' as const,
  })),
  computed(() => day.value !== null && (props.categoryId === null || calendarIds.value.length > 0)),
)

const events = computed(() =>
  (eventsQuery.data.value?.content ?? []).map((e) => ({
    event: e,
    start: new Date(e.start),
    end: new Date(e.end),
    calendar: props.calendarById.get(e.categoryId),
  })),
)

function newEvent() {
  if (!day.value) return
  editor.create({
    categoryId: props.calendars[0]?.id,
    start: atMinutes(day.value, 9 * 60),
    end: atMinutes(day.value, 10 * 60),
  })
}

function show(event: CalendarEvent) {
  editor.show(event)
}

const dayHoliday = computed(() => (props.selectedDay ? holidayOn(props.selectedDay) : undefined))

// ---------------------------------------------------------------- month overview
const monthHolidays = computed(() =>
  [...between(props.month, endOfMonth(props.month)).values()].map((h) => ({
    holiday: h,
    date: parseDayKey(h.date),
    name: holidayName(h),
  })),
)

const monthStats = computed(() => {
  const perCalendar = new Map<number, number>()
  let activeDays = 0
  let eventDays = 0
  for (const [key, summary] of props.days) {
    if (!isSameMonth(parseDayKey(key), props.month)) continue
    activeDays++
    eventDays += summary.eventCount
    for (const id of summary.calendarIds) perCalendar.set(id, (perCalendar.get(id) ?? 0) + 1)
  }
  return {
    activeDays,
    eventDays,
    calendars: props.calendars.map((c) => ({ calendar: c, days: perCalendar.get(c.id) ?? 0 })),
  }
})
</script>

<template>
  <aside class="panel">
    <!-- details of the selected day -->
    <template v-if="day">
      <header class="panel-head">
        <div>
          <div class="kicker">{{ formatDate(day, 'weekday') }}</div>
          <h2>{{ formatDate(day, 'dayLong') }}</h2>
        </div>
        <Button icon="pi pi-times" text rounded severity="secondary" :aria-label="t('month.closeDay')" @click="emit('clear')" />
      </header>
      <div v-if="dayHoliday" class="holiday-chip">
        <i class="pi pi-star-fill" />{{ holidayName(dayHoliday) }}
        <span class="muted">· {{ t('holidays.publicHoliday') }}</span>
      </div>
      <div class="actions">
        <Button :label="t('month.dayView')" icon="pi pi-calendar-clock" size="small" @click="emit('openDay', selectedDay!)" />
        <Button :label="t('day.newEvent')" icon="pi pi-plus" size="small" severity="secondary" outlined :disabled="!calendars.length" @click="newEvent" />
      </div>
      <div class="scope muted"><i class="pi pi-filter" /> {{ categoryLabel }}</div>

      <div v-if="eventsQuery.isLoading.value" class="list">
        <Skeleton v-for="i in 3" :key="i" height="58px" />
      </div>
      <div v-else-if="!events.length" class="empty muted">{{ t('month.noEventsOnDay') }}</div>
      <ul v-else class="list" :aria-label="t('month.eventsOn', { date: formatDate(day, 'dayMonth') })">
        <li v-for="item in events" :key="item.event.id">
          <button type="button" class="event" :style="{ '--c': item.calendar?.color ?? '#64748b' }" @click="show(item.event)">
            <span class="time tabular">{{ formatTimeRange(item.start, item.end) }}</span>
            <span class="title">{{ item.event.title }}</span>
            <span class="meta">
              <span class="color-dot" :style="{ '--dot': item.calendar?.color }" />{{ item.calendar?.name }}
              <span v-if="item.event.place" class="place"><i class="pi pi-map-marker" />{{ item.event.place }}</span>
            </span>
          </button>
        </li>
      </ul>
    </template>

    <!-- month overview / legend -->
    <template v-else>
      <header class="panel-head">
        <div>
          <div class="kicker">{{ t('month.overview') }}</div>
          <h2>{{ formatDate(month, 'monthTitle') }}</h2>
        </div>
      </header>
      <div class="stats">
        <div class="stat">
          <span class="value tabular">{{ monthStats.activeDays }}</span>
          <span class="label">{{ t('month.daysWithEvents', monthStats.activeDays) }}</span>
        </div>
        <div class="stat" :title="t('month.eventsStatTitle')">
          <span class="value tabular">{{ monthStats.eventDays }}</span>
          <span class="label">{{ t('month.events', monthStats.eventDays) }}</span>
        </div>
      </div>
      <div class="scope muted"><i class="pi pi-filter" /> {{ categoryLabel }}</div>
      <ul v-if="monthStats.calendars.length" class="legend">
        <li v-for="row in monthStats.calendars" :key="row.calendar.id" :class="{ inactive: !row.days }">
          <span class="color-dot" :style="{ '--dot': row.calendar.color }" />
          <span class="name">{{ row.calendar.name }}</span>
          <span class="muted tabular">{{ t('common.daysCount', row.days) }}</span>
        </li>
      </ul>
      <div v-else class="empty muted">{{ t('month.emptyGroup') }}</div>
      <div class="scale">
        <span class="muted">{{ t('month.fewer') }}</span>
        <span v-for="level in [1, 2, 3, 4]" :key="level" class="swatch" :class="`level-${level}`" />
        <span class="muted">{{ t('month.more') }}</span>
      </div>
      <section v-if="monthHolidays.length" class="holidays">
        <h3 class="kicker">{{ t('month.holidaysTitle') }}</h3>
        <ul>
          <li v-for="item in monthHolidays" :key="item.holiday.date">
            <span class="holiday-date tabular">{{ formatDate(item.date, 'dayShort') }}</span>
            <span class="holiday-label">{{ item.name }}</span>
          </li>
        </ul>
      </section>
      <p class="hint muted">{{ t('month.hint') }}</p>
    </template>
  </aside>
</template>

<style scoped>
.panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 18px;
  min-height: 0;
  overflow-y: auto;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.kicker {
  font-size: 11px;
  font-weight: 650;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--app-text-muted);
}

h2 {
  font-size: 18px;
}

.actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.scope {
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.scope i {
  font-size: 11px;
}

.list,
.legend {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.event {
  display: grid;
  gap: 2px;
  width: 100%;
  padding: 8px 10px;
  border: 1px solid var(--app-border);
  border-left: 3px solid var(--c);
  border-radius: 8px;
  background: var(--app-panel);
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.event:hover,
.event:focus-visible {
  outline: none;
  background: color-mix(in srgb, var(--c) 8%, var(--app-panel));
}

.time {
  font-size: 12px;
  color: var(--app-text-muted);
}

.title {
  font-weight: 650;
}

.meta {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--app-text-muted);
  flex-wrap: wrap;
}

.place {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.place::before {
  content: '·';
  margin-right: 2px;
}

.place i {
  font-size: 10px;
}

.empty {
  padding: 12px 0;
  font-size: 13px;
}

.stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.stat {
  display: flex;
  flex-direction: column;
  padding: 10px 12px;
  border-radius: 10px;
  background: var(--app-accent-soft);
}

.stat .value {
  font-size: 22px;
  font-weight: 700;
  color: var(--app-accent);
}

.stat .label {
  font-size: 12px;
  color: var(--app-text-muted);
}

.legend li {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.legend .name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.legend .inactive {
  opacity: 0.55;
}

.scale {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11.5px;
}

.swatch {
  width: 16px;
  height: 12px;
  border-radius: 3px;
  border: 1px solid var(--app-border-subtle);
}

.swatch.level-1 {
  background: color-mix(in srgb, var(--p-primary-500) var(--heat-1), var(--app-panel));
}
.swatch.level-2 {
  background: color-mix(in srgb, var(--p-primary-500) var(--heat-2), var(--app-panel));
}
.swatch.level-3 {
  background: color-mix(in srgb, var(--p-primary-500) var(--heat-3), var(--app-panel));
}
.swatch.level-4 {
  background: color-mix(in srgb, var(--p-primary-500) var(--heat-4), var(--app-panel));
}

.hint {
  font-size: 12px;
  margin: 0;
}

.holiday-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  align-self: flex-start;
  font-size: 12.5px;
  font-weight: 650;
  padding: 3px 10px;
  border-radius: 999px;
  color: var(--app-holiday);
  background: var(--app-holiday-soft);
}

.holiday-chip i {
  font-size: 10px;
}

.holiday-chip .muted {
  font-weight: 500;
}

.holidays {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-top: 12px;
  border-top: 1px solid var(--app-border-subtle);
}

.holidays ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.holidays li {
  display: flex;
  gap: 10px;
  font-size: 13px;
}

.holiday-date {
  flex: none;
  min-width: 92px;
  color: var(--app-holiday);
  font-weight: 600;
}

.holiday-label {
  min-width: 0;
}
</style>
