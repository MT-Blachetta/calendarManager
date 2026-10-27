<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import DatePicker from 'primevue/datepicker'
import Button from 'primevue/button'
import { useUiStore } from '@/stores/ui'
import { useCatalog } from '@/composables/useCatalog'
import { useHolidays } from '@/composables/useHolidays'
import { parseDayKey, toDayKey } from '@/lib/time'
import type { FilterMode } from '@/api/types'
import { monthRoute } from '@/lib/routes'

const emit = defineEmits<{ navigate: [] }>()
const ui = useUiStore()
const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const { views, calendars, viewName } = useCatalog()
const { holidayOn, holidayName } = useHolidays()

const currentDay = computed({
  get: () => parseDayKey(route.name === 'day' ? (route.params.date as string) : ui.lastDay),
  set: (date: Date | Date[] | (Date | null)[] | null | undefined) => {
    if (date instanceof Date) {
      router.push({ name: 'day', params: { date: toDayKey(date) } })
      emit('navigate')
    }
  },
})

const nav = [
  { to: '/day', label: 'nav.day', icon: 'pi pi-calendar-clock', name: 'day' },
  { to: '/month', label: 'nav.month', icon: 'pi pi-calendar', name: 'month' },
  { to: '/events', label: 'nav.events', icon: 'pi pi-list', name: 'events' },
  { to: '/categories', label: 'nav.categories', icon: 'pi pi-sitemap', name: 'categories' },
  { to: '/tags', label: 'nav.tags', icon: 'pi pi-tags', name: 'tags' },
  { to: '/views', label: 'nav.views', icon: 'pi pi-filter', name: 'views' },
  { to: '/settings', label: 'nav.settings', icon: 'pi pi-cog', name: 'settings' },
]

/** Slot data of a PrimeVue DatePicker day cell (month is 0-based). */
interface PickerDate {
  day: number
  month: number
  year: number
}

function holidayOf(date: PickerDate) {
  const key = `${date.year}-${String(date.month + 1).padStart(2, '0')}-${String(date.day).padStart(2, '0')}`
  return holidayOn(key)
}

const modeIcon: Record<FilterMode, string> = {
  ALL: 'pi pi-th-large',
  INCLUDE: 'pi pi-filter',
  EXCLUDE: 'pi pi-filter-slash',
}

function openView(id: number) {
  ui.viewId = id
  router.push({ name: 'day', params: { date: toDayKey(currentDay.value) } })
  emit('navigate')
}

function goTo(to: string) {
  if (to === '/day') {
    router.push({ name: 'day', params: { date: ui.lastDay } })
  } else if (to === '/month') {
    router.push(monthRoute(ui.lastDay.slice(0, 7), ui.monthCategoryId))
  } else {
    router.push(to)
  }
  emit('navigate')
}
</script>

<template>
  <aside class="sidebar">
    <div class="brand">
      <img src="/favicon.svg" alt="" width="28" height="28" />
      <div>
        <div class="brand-name">calendarManager</div>
        <div class="brand-sub">{{ t('app.tagline') }}</div>
      </div>
    </div>

    <div class="mini-calendar">
      <DatePicker v-model="currentDay" inline :show-other-months="true" :aria-label="t('nav.miniCalendar')">
        <template #date="{ date }">
          <span
            v-if="holidayOf(date)"
            class="mini-day holiday"
            :title="holidayName(holidayOf(date)!)"
            :aria-label="`${date.day}, ${holidayName(holidayOf(date)!)}`"
            >{{ date.day }}</span
          >
          <span v-else class="mini-day">{{ date.day }}</span>
        </template>
      </DatePicker>
    </div>

    <section class="section">
      <div class="section-title">
        <span>{{ t('nav.views') }}</span>
        <RouterLink to="/views" class="section-action" @click="emit('navigate')">{{ t('nav.manage') }}</RouterLink>
      </div>
      <button
        v-for="view in views"
        :key="view.id"
        type="button"
        class="item"
        :class="{ active: ui.viewId === view.id && route.name === 'day' }"
        @click="openView(view.id)"
      >
        <i :class="modeIcon[view.filterMode]" />
        <span class="item-label">{{ viewName(view) }}</span>
      </button>
    </section>

    <section class="section">
      <div class="section-title"><span>{{ t('nav.navigate') }}</span></div>
      <button
        v-for="item in nav"
        :key="item.to"
        type="button"
        class="item"
        :class="{ active: route.name === item.name }"
        @click="goTo(item.to)"
      >
        <i :class="item.icon" />
        <span class="item-label">{{ t(item.label) }}</span>
        <span v-if="item.name === 'categories'" class="count">{{ calendars.length }}</span>
      </button>
    </section>

    <div class="footer">
      <Button
        :icon="ui.isDark ? 'pi pi-sun' : 'pi pi-moon'"
        :label="ui.isDark ? t('nav.lightMode') : t('nav.darkMode')"
        text
        size="small"
        severity="secondary"
        @click="ui.toggleTheme()"
      />
      <a class="api-link" href="/swagger-ui.html" target="_blank" rel="noopener">
        <i class="pi pi-code" /> API
      </a>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  display: flex;
  flex-direction: column;
  gap: 8px;
  height: 100%;
  width: var(--sidebar-width);
  padding: 16px 12px 12px;
  background: var(--app-panel);
  border-right: 1px solid var(--app-border);
  overflow-y: auto;
}

.brand {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 0 8px 8px;
}

.brand-name {
  font-weight: 700;
  letter-spacing: -0.01em;
}

.brand-sub {
  font-size: 11.5px;
  color: var(--app-text-muted);
}

.mini-calendar :deep(.p-datepicker-panel) {
  border: none;
  padding: 0;
  background: transparent;
  width: 100%;
}

.mini-calendar :deep(.p-datepicker-day) {
  width: 2rem;
  height: 2rem;
  font-size: 12.5px;
}

.mini-calendar :deep(.p-datepicker-day-cell) {
  padding: 1px;
}

.mini-calendar :deep(.p-datepicker-weekday) {
  font-size: 11px;
  color: var(--app-text-muted);
}

.mini-day {
  display: grid;
  place-items: center;
  position: relative;
  width: 100%;
  height: 100%;
  border-radius: inherit;
}

/* public holiday: red date with a dot below, as in printed calendars */
.mini-day.holiday {
  color: var(--app-holiday);
  font-weight: 700;
  background: var(--app-holiday-soft);
}

.mini-day.holiday::after {
  content: '';
  position: absolute;
  bottom: 3px;
  left: 50%;
  width: 4px;
  height: 4px;
  margin-left: -2px;
  border-radius: 50%;
  background: currentColor;
}

.mini-calendar :deep(.p-datepicker-day-selected) .mini-day.holiday {
  color: inherit;
  background: transparent;
}

.mini-calendar :deep(.p-datepicker-other-month) .mini-day.holiday {
  opacity: 0.6;
}

.mini-calendar :deep(.p-datepicker-header) {
  padding: 0 0 6px;
  background: transparent;
}

.section {
  display: flex;
  flex-direction: column;
  gap: 1px;
  padding-top: 6px;
}

.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 10px;
  font-size: 11px;
  font-weight: 650;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--app-text-muted);
}

.section-action {
  font-size: 11px;
  text-transform: none;
  letter-spacing: 0;
  color: var(--app-accent);
  text-decoration: none;
}

.item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 7px 10px;
  border: 0;
  border-radius: var(--app-radius-sm);
  background: transparent;
  color: var(--app-text);
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.item i {
  font-size: 14px;
  color: var(--app-text-muted);
  width: 16px;
}

.item:hover {
  background: var(--app-hover);
}

.item.active {
  background: var(--app-accent-soft);
  color: var(--app-accent);
  font-weight: 600;
}

.item.active i {
  color: var(--app-accent);
}

.item-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.count {
  font-size: 11px;
  color: var(--app-text-muted);
}

.footer {
  margin-top: auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 8px;
  border-top: 1px solid var(--app-border-subtle);
}

.api-link {
  font-size: 12.5px;
  color: var(--app-text-muted);
  text-decoration: none;
  padding: 6px 8px;
}

.api-link:hover {
  color: var(--app-accent);
}
</style>
