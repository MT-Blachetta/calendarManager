<script setup lang="ts">
import { computed, useTemplateRef, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useEventListener } from '@vueuse/core'
import { useI18n } from 'vue-i18n'
import { addMonths, isSameMonth } from 'date-fns'
import Button from 'primevue/button'
import Popover from 'primevue/popover'
import DatePicker from 'primevue/datepicker'
import Select from 'primevue/select'
import NavToggle from '@/components/layout/NavToggle.vue'
import ModeSwitch from '@/components/layout/ModeSwitch.vue'
import CategoryPickerList from '@/components/month/CategoryPickerList.vue'
import MonthGrid from '@/components/month/MonthGrid.vue'
import MonthDayPanel from '@/components/month/MonthDayPanel.vue'
import { useDaySummary } from '@/api/queries'
import { useCatalog } from '@/composables/useCatalog'
import { useUiStore } from '@/stores/ui'
import { useEventEditor } from '@/stores/eventEditor'
import { browserTimeZone, gridRange, parseMonthKey, toMonthKey } from '@/lib/month'
import { parseDayKey, toDayKey } from '@/lib/time'
import { monthRoute } from '@/lib/routes'
import type { SummaryCalendar, SummaryDay } from '@/api/types'
import { formatDate } from '@/i18n/dates'

const route = useRoute()
const router = useRouter()
const ui = useUiStore()
const editor = useEventEditor()
const { t } = useI18n()
const { categories, categoryById, fullLabel, loading: catalogLoading } = useCatalog()

// ---------------------------------------------------------------- URL state
const month = computed(() => parseMonthKey(route.params.month as string | undefined))
const monthKey = computed(() => toMonthKey(month.value))

/** Selected calendar or group from ?category= (null = all calendars). */
const categoryId = computed<number | null>(() => {
  const raw = route.query.category
  const id = typeof raw === 'string' && raw !== '' ? Number(raw) : NaN
  return Number.isInteger(id) ? id : null
})
const selectedDay = computed(() => {
  const raw = route.query.day
  return typeof raw === 'string' && toDayKey(parseDayKey(raw)) === raw ? raw : null
})

// Canonical URL (/month/yyyy-MM); a bare /month restores the remembered calendar/group;
// unknown categories are dropped once the catalog is loaded.
watch(
  [() => route.params.month, categoryId, () => catalogLoading.value],
  ([param, id, isLoading]) => {
    const target = !param && route.query.category === undefined ? ui.monthCategoryId : id
    const unknown = target !== null && !isLoading && !categoryById.value.has(target)
    if (param !== monthKey.value || unknown || target !== id) {
      router.replace(monthRoute(monthKey.value, unknown ? null : target, selectedDay.value))
    }
  },
  { immediate: true },
)

// Remember selection and month for the sidebar / Day ↔ Month switch.
watch(categoryId, (id) => (ui.monthCategoryId = id), { immediate: true })
watch(
  [month, selectedDay],
  ([m, day]) => {
    if (day) ui.lastDay = day
    else if (!isSameMonth(parseDayKey(ui.lastDay), m))
      ui.lastDay = toDayKey(isSameMonth(new Date(), m) ? new Date() : m)
  },
  { immediate: true },
)

function navigate(target: { month?: string; category?: number | null; day?: string | null }, replace = false) {
  const category = target.category === undefined ? categoryId.value : target.category
  const day = target.day === undefined ? selectedDay.value : target.day
  const to = monthRoute(target.month ?? monthKey.value, category, day)
  return replace ? router.replace(to) : router.push(to)
}

function goMonth(offset: number) {
  navigate({ month: toMonthKey(addMonths(month.value, offset)), day: null })
}

function goToday() {
  navigate({ month: toMonthKey(new Date()), day: toDayKey(new Date()) })
}

function selectCategory(id: number | null) {
  navigate({ category: id })
}

function selectDay(day: string) {
  const date = parseDayKey(day)
  if (!isSameMonth(date, month.value)) navigate({ month: toMonthKey(date), day })
  else navigate({ day }, true)
}

function openDay(day: string) {
  ui.lastDay = day
  router.push({ name: 'day', params: { date: day } })
}

// ---------------------------------------------------------------- data
const zone = browserTimeZone()
const range = computed(() => {
  const { from, to } = gridRange(month.value)
  return { from: toDayKey(from), to: toDayKey(to) }
})
const summary = useDaySummary(categoryId, range, zone)

const calendars = computed<SummaryCalendar[]>(() => summary.data.value?.calendars ?? [])
const calendarById = computed(() => new Map(calendars.value.map((c) => [c.id, c])))
const days = computed(() => new Map<string, SummaryDay>((summary.data.value?.days ?? []).map((d) => [d.date, d])))

const selection = computed(() => (categoryId.value === null ? undefined : categoryById.value.get(categoryId.value)))
const selectionLabel = computed(() =>
  selection.value ? fullLabel(selection.value.id) : t('common.allCalendars'),
)

// ---------------------------------------------------------------- month picker
const pickerPopover = useTemplateRef<InstanceType<typeof Popover>>('pickerPopover')
const pickerValue = computed({
  get: () => month.value,
  set: (value: Date | Date[] | (Date | null)[] | null | undefined) => {
    if (value instanceof Date) {
      pickerPopover.value?.hide()
      navigate({ month: toMonthKey(value), day: null })
    }
  },
})

// ---------------------------------------------------------------- mobile picker
const pickerOptions = computed(() => [
  { label: t('common.allCalendars'), value: null as number | null, depth: 0, kind: 'ALL' },
  ...categories.value.map((c) => ({ label: c.name, value: c.id as number | null, depth: c.depth + 1, kind: c.kind })),
])

// ---------------------------------------------------------------- keyboard
useEventListener(window, 'keydown', (e: KeyboardEvent) => {
  const target = e.target as HTMLElement | null
  if (e.ctrlKey || e.metaKey || e.altKey || editor.dialogOpen) return
  if (target && (target.isContentEditable || ['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName))) return
  if (target?.closest('.month-grid, .picker, .p-popover')) return
  if (document.querySelector('.p-dialog-mask, .p-drawer-mask')) return
  if (e.key === 'ArrowLeft' || e.key === 'PageUp') goMonth(-1)
  else if (e.key === 'ArrowRight' || e.key === 'PageDown') goMonth(1)
  else if (e.key === 't' || e.key === 'T') goToday()
  else if (e.key === 'Escape' && selectedDay.value) navigate({ day: null }, true)
  else return
  e.preventDefault()
})
</script>

<template>
  <div class="page month-page">
    <header class="page-header">
      <NavToggle />
      <ModeSwitch />
      <div class="date-nav">
        <Button :label="t('common.today')" severity="secondary" outlined size="small" v-tooltip.bottom="t('month.todayHint')" @click="goToday" />
        <Button icon="pi pi-chevron-left" text rounded severity="secondary" :aria-label="t('month.previous')" v-tooltip.bottom="t('month.previousHint')" @click="goMonth(-1)" />
        <Button icon="pi pi-chevron-right" text rounded severity="secondary" :aria-label="t('month.next')" v-tooltip.bottom="t('month.nextHint')" @click="goMonth(1)" />
      </div>
      <button type="button" class="month-title" aria-haspopup="dialog" @click="pickerPopover?.toggle($event)">
        <h1>{{ formatDate(month, 'monthTitle') }}</h1>
        <i class="pi pi-chevron-down" />
      </button>
      <Popover ref="pickerPopover">
        <DatePicker v-model="pickerValue" view="month" date-format="mm/yy" inline />
      </Popover>
      <div class="spacer" />
      <span v-if="!ui.isMobile" class="selection-chip" :title="selectionLabel">
        <i :class="!selection ? 'pi pi-th-large' : selection.kind === 'ABSTRACT' ? 'pi pi-folder' : 'pi pi-calendar'" />
        {{ selectionLabel }}
        <span class="muted">· {{ t('common.calendarsCount', calendars.length) }}</span>
      </span>
      <Select
        v-else
        :model-value="categoryId"
        :options="pickerOptions"
        option-label="label"
        option-value="value"
        filter
        size="small"
        class="mobile-picker"
        :placeholder="t('common.allCalendars')"
        :aria-label="t('month.calendarOrGroup')"
        @update:model-value="selectCategory"
      >
        <template #option="{ option }">
          <span class="opt" :style="{ paddingLeft: `${option.depth * 14}px` }">
            <i :class="option.kind === 'ALL' ? 'pi pi-th-large' : option.kind === 'ABSTRACT' ? 'pi pi-folder' : 'pi pi-calendar'" />
            {{ option.label }}
          </span>
        </template>
      </Select>
    </header>

    <div class="month-layout">
      <CategoryPickerList
        v-if="!ui.isMobile"
        class="picker-col"
        :categories="categories"
        :category-by-id="categoryById"
        :selected-id="categoryId"
        @select="selectCategory"
      />
      <div class="grid-col">
        <div class="card grid-card">
          <MonthGrid
            :month="month"
            :days="days"
            :calendar-by-id="calendarById"
            :selected-day="selectedDay"
            :loading="summary.isFetching.value"
            @select="selectDay"
            @open="openDay"
          />
        </div>
      </div>
      <div class="panel-col card">
        <MonthDayPanel
          :month="month"
          :selected-day="selectedDay"
          :category-id="categoryId"
          :category-label="selectionLabel"
          :calendars="calendars"
          :calendar-by-id="calendarById"
          :days="days"
          @open-day="openDay"
          @clear="navigate({ day: null }, true)"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.date-nav {
  display: flex;
  align-items: center;
  gap: 2px;
}

.month-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: 0;
  background: transparent;
  color: inherit;
  font: inherit;
  padding: 4px 8px;
  border-radius: 8px;
  cursor: pointer;
}

.month-title:hover {
  background: var(--app-hover);
}

.month-title h1 {
  text-align: left;
}

.month-title i {
  font-size: 11px;
  color: var(--app-text-muted);
}

.selection-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  max-width: 420px;
  padding: 6px 12px;
  border-radius: 999px;
  background: var(--app-accent-soft);
  color: var(--app-accent);
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.selection-chip .muted {
  font-weight: 500;
}

.mobile-picker {
  min-width: 180px;
}

.opt {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.opt i {
  font-size: 12px;
  color: var(--app-text-muted);
}

.month-layout {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 250px minmax(0, 1fr) 320px;
}

.picker-col {
  border-right: 1px solid var(--app-border);
  background: var(--app-panel);
  overflow: hidden;
}

.grid-col {
  min-width: 0;
  min-height: 0;
  overflow: auto;
  padding: 16px;
}

.grid-card {
  height: 100%;
  overflow: hidden;
}

.panel-col {
  margin: 16px 16px 16px 0;
  min-height: 0;
  display: flex;
  overflow: hidden;
}

.panel-col > * {
  flex: 1;
}

@media (max-width: 1280px) {
  .month-layout {
    grid-template-columns: 230px minmax(0, 1fr);
    grid-template-rows: minmax(0, 1fr) auto;
    overflow: auto;
  }
  .picker-col {
    grid-row: 1 / span 2;
  }
  .grid-col {
    overflow: visible;
  }
  .grid-card {
    height: auto;
  }
  .panel-col {
    margin: 0 16px 16px;
  }
}

@media (max-width: 900px) {
  .month-layout {
    grid-template-columns: minmax(0, 1fr);
    display: block;
    overflow: auto;
  }
  .grid-col {
    padding: 12px;
  }
  .panel-col {
    margin: 0 12px 12px;
  }
}
</style>
