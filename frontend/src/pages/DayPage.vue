<script setup lang="ts">
import { computed, watch, watchEffect } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useEventListener } from '@vueuse/core'
import { useI18n } from 'vue-i18n'
import { addDays, isToday } from 'date-fns'
import Button from 'primevue/button'
import Select from 'primevue/select'
import SelectButton from 'primevue/selectbutton'
import Skeleton from 'primevue/skeleton'
import DayGrid from '@/components/day/DayGrid.vue'
import NavToggle from '@/components/layout/NavToggle.vue'
import ModeSwitch from '@/components/layout/ModeSwitch.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { useSchedule } from '@/api/queries'
import { useCatalog } from '@/composables/useCatalog'
import { useHolidays } from '@/composables/useHolidays'
import { formatDate } from '@/i18n/dates'
import { useUiStore, type Density } from '@/stores/ui'
import { useEventEditor } from '@/stores/eventEditor'
import { atMinutes, dayRange, parseDayKey, toDayKey } from '@/lib/time'
import type { FilterMode } from '@/api/types'

const route = useRoute()
const router = useRouter()
const ui = useUiStore()
const editor = useEventEditor()
const { t } = useI18n()
const { views, tagById, calendars: allCalendars, viewName } = useCatalog()
const { holidayOn, holidayName } = useHolidays()

const day = computed(() => parseDayKey(route.params.date as string | undefined))
watchEffect(() => (ui.lastDay = toDayKey(day.value)))

// Keep the URL canonical (/day/yyyy-MM-dd)
watch(
  () => route.params.date,
  (date) => {
    if (!date || toDayKey(parseDayKey(date as string)) !== date) {
      router.replace({ name: 'day', params: { date: toDayKey(day.value) } })
    }
  },
  { immediate: true },
)

/** The selected view, falling back to the system view "All calendars". */
const view = computed(
  () => views.value.find((v) => v.id === ui.viewId) ?? views.value.find((v) => v.system) ?? views.value[0],
)
const viewId = computed(() => view.value?.id)
const range = computed(() => dayRange(day.value))
const schedule = useSchedule(viewId, range)

const calendars = computed(() => schedule.data.value?.calendars ?? [])
const events = computed(() => schedule.data.value?.events ?? [])

const title = computed(() => formatDate(day.value, 'dayTitle'))
const holiday = computed(() => holidayOn(toDayKey(day.value)))
const viewOptions = computed(() => views.value.map((v) => ({ label: viewName(v), value: v.id, mode: v.filterMode })))
const modeIcon: Record<FilterMode, string> = {
  ALL: 'pi pi-th-large',
  INCLUDE: 'pi pi-filter',
  EXCLUDE: 'pi pi-filter-slash',
}
const densityOptions = computed<{ value: Density; icon: string; label: string }[]>(() => [
  { value: 'compact', icon: 'pi pi-minus', label: t('density.compact') },
  { value: 'comfortable', icon: 'pi pi-equals', label: t('density.comfortable') },
  { value: 'spacious', icon: 'pi pi-bars', label: t('density.spacious') },
])

function go(offset: number) {
  router.push({ name: 'day', params: { date: toDayKey(addDays(day.value, offset)) } })
}

function today() {
  router.push({ name: 'day', params: { date: toDayKey(new Date()) } })
}

function selectView(id: number) {
  ui.viewId = id
}

function newEvent() {
  const now = new Date()
  const base = isToday(day.value) ? Math.min(Math.ceil((now.getHours() * 60 + now.getMinutes()) / 30) * 30, 23 * 60) : 9 * 60
  editor.create({
    categoryId: calendars.value[0]?.id,
    start: atMinutes(day.value, base),
    end: atMinutes(day.value, base + 60),
  })
}

// Keyboard shortcuts: T = today, ←/→ = previous/next day, N = new event
useEventListener(window, 'keydown', (e: KeyboardEvent) => {
  const target = e.target as HTMLElement | null
  if (e.ctrlKey || e.metaKey || e.altKey || editor.dialogOpen) return
  if (target && (target.isContentEditable || ['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName))) return
  if (document.querySelector('.p-dialog-mask, .p-drawer-mask')) return
  if (e.key === 't' || e.key === 'T') today()
  else if (e.key === 'ArrowLeft') go(-1)
  else if (e.key === 'ArrowRight') go(1)
  else if (e.key === 'n' || e.key === 'N') {
    e.preventDefault()
    newEvent()
  } else return
})
</script>

<template>
  <div class="page day-page">
    <header class="page-header day-header">
      <NavToggle />
      <ModeSwitch />
      <div class="date-nav">
        <Button :label="t('common.today')" severity="secondary" outlined size="small" v-tooltip.bottom="t('day.todayHint')" @click="today" />
        <Button icon="pi pi-chevron-left" text rounded severity="secondary" :aria-label="t('day.previous')" v-tooltip.bottom="t('day.previousHint')" @click="go(-1)" />
        <Button icon="pi pi-chevron-right" text rounded severity="secondary" :aria-label="t('day.next')" v-tooltip.bottom="t('day.nextHint')" @click="go(1)" />
      </div>
      <div class="title-block">
        <h1>{{ title }}</h1>
        <span v-if="isToday(day)" class="today-badge">{{ t('common.today') }}</span>
        <span v-if="holiday" class="holiday-badge" :title="t('holidays.publicHoliday')">
          <i class="pi pi-star-fill" />{{ holidayName(holiday) }}
        </span>
      </div>
      <div class="spacer" />
      <Select
        :model-value="viewId"
        :options="viewOptions"
        option-label="label"
        option-value="value"
        class="view-select"
        size="small"
        :aria-label="t('day.viewSelect')"
        @update:model-value="selectView"
      >
        <template #value="{ value }">
          <span v-if="view" class="view-value">
            <i :class="modeIcon[view.filterMode]" />{{ viewOptions.find((o) => o.value === value)?.label }}
          </span>
        </template>
        <template #option="{ option }">
          <span class="view-value"><i :class="modeIcon[option.mode as FilterMode]" />{{ option.label }}</span>
        </template>
      </Select>
      <SelectButton
        v-model="ui.density"
        :options="densityOptions"
        option-label="label"
        option-value="value"
        :allow-empty="false"
        size="small"
        class="density"
        :aria-label="t('day.density')"
      >
        <template #option="{ option }">
          <i v-tooltip.bottom="option.label" :class="option.icon" />
        </template>
      </SelectButton>
      <Button :label="t('day.newEvent')" icon="pi pi-plus" size="small" v-tooltip.bottom="t('day.newEventHint')" @click="newEvent" />
    </header>

    <div class="day-body">
      <div v-if="schedule.isLoading.value" class="loading">
        <Skeleton height="64px" class="mb" />
        <Skeleton height="100%" />
      </div>
      <EmptyState
        v-else-if="!calendars.length"
        icon="pi pi-calendar-times"
        :title="allCalendars.length ? t('day.noCalendarsInView') : t('day.noCalendarsYet')"
        :text="allCalendars.length ? t('day.noCalendarsInViewText') : t('day.noCalendarsYetText')"
      >
        <Button
          v-if="allCalendars.length && view"
          :label="t('day.editView')"
          icon="pi pi-filter"
          outlined
          @click="router.push({ name: 'views', params: { id: view.id } })"
        />
        <Button v-else :label="t('day.createCalendar')" icon="pi pi-plus" @click="router.push('/categories')" />
      </EmptyState>
      <DayGrid
        v-else
        :day="day"
        :calendars="calendars"
        :events="events"
        :tag-by-id="tagById"
        :slot-height="ui.slotHeight"
      />
    </div>
  </div>
</template>

<style scoped>
.day-header {
  gap: 8px 12px;
}

.date-nav {
  display: flex;
  align-items: center;
  gap: 2px;
}

.title-block {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.title-block h1 {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.today-badge {
  font-size: 11px;
  font-weight: 650;
  padding: 2px 8px;
  border-radius: 999px;
  color: var(--app-accent);
  background: var(--app-accent-soft);
}

.holiday-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  font-weight: 650;
  padding: 2px 8px;
  border-radius: 999px;
  color: var(--app-holiday);
  background: var(--app-holiday-soft);
  white-space: nowrap;
}

.holiday-badge i {
  font-size: 9px;
}

.view-select {
  min-width: 200px;
}

.view-value {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.view-value i {
  font-size: 12px;
  color: var(--app-text-muted);
}

.density :deep(.p-togglebutton) {
  padding-inline: 10px;
}

.day-body {
  flex: 1;
  min-height: 0;
  position: relative;
}

.loading {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

@media (max-width: 900px) {
  .density {
    display: none;
  }
  .view-select {
    min-width: 150px;
  }
}
</style>
