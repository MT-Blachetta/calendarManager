<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { refDebounced } from '@vueuse/core'
import { addDays, startOfDay } from 'date-fns'
import { useI18n } from 'vue-i18n'
import DataTable, { type DataTablePageEvent, type DataTableSortEvent } from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import DatePicker from 'primevue/datepicker'
import MultiSelect from 'primevue/multiselect'
import InputText from 'primevue/inputtext'
import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'
import { useConfirm } from 'primevue/useconfirm'
import NavToggle from '@/components/layout/NavToggle.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { useDeleteEvent, useEventSearch, type EventSearchParams } from '@/api/queries'
import { useCatalog } from '@/composables/useCatalog'
import { useEventEditor } from '@/stores/eventEditor'
import { formatTimeRange } from '@/lib/time'
import { notify } from '@/lib/notify'
import type { CalendarEvent } from '@/api/types'
import { formatDate, pickerFormat } from '@/i18n/dates'

const editor = useEventEditor()
const { t } = useI18n()
const confirm = useConfirm()
const deleteMutation = useDeleteEvent()
const { calendarOptions, categoryById, pathLabel } = useCatalog()

const range = ref<(Date | null)[] | null>([startOfDay(new Date()), addDays(startOfDay(new Date()), 6)])
const calendarIds = ref<number[]>([])
const search = ref('')
const debouncedSearch = refDebounced(search, 300)
const page = ref(0)
const size = ref(25)
const sort = ref<EventSearchParams['sort']>('start')
const direction = ref<EventSearchParams['direction']>('asc')

watch([range, calendarIds, debouncedSearch], () => (page.value = 0))

const params = computed<EventSearchParams>(() => {
  const [from, to] = range.value ?? []
  return {
    from: from ?? undefined,
    to: to ? addDays(startOfDay(to), 1) : from ? addDays(startOfDay(from), 1) : undefined,
    categoryIds: calendarIds.value,
    q: debouncedSearch.value.trim(),
    page: page.value,
    size: size.value,
    sort: sort.value,
    direction: direction.value,
  }
})

const query = useEventSearch(params)
const rows = computed(() =>
  (query.data.value?.content ?? []).map((e) => ({ ...e, startDate: new Date(e.start), endDate: new Date(e.end) })),
)

function onPage(e: DataTablePageEvent) {
  page.value = e.page
  size.value = e.rows
}

function onSort(e: DataTableSortEvent) {
  sort.value = (e.sortField as EventSearchParams['sort']) ?? 'start'
  direction.value = e.sortOrder === -1 ? 'desc' : 'asc'
}

function newEvent() {
  const start = new Date()
  start.setMinutes(0, 0, 0)
  start.setHours(start.getHours() + 1)
  editor.create({ categoryId: calendarIds.value[0], start, end: new Date(start.getTime() + 3_600_000) })
}

function remove(event: CalendarEvent) {
  confirm.require({
    header: t('event.deleteHeader'),
    message: t('event.deleteMessage', { title: event.title }),
    icon: 'pi pi-trash',
    acceptProps: { label: t('common.delete'), severity: 'danger' },
    rejectProps: { label: t('common.cancel'), severity: 'secondary', outlined: true },
    accept: async () => {
      await deleteMutation.mutateAsync(event.id)
      notify('success', t('event.deleted'), event.title)
    },
  })
}

function clearFilters() {
  range.value = null
  calendarIds.value = []
  search.value = ''
}
</script>

<template>
  <div class="page">
    <header class="page-header">
      <NavToggle />
      <h1>{{ t('events.title') }}</h1>
      <div class="spacer" />
      <Button :label="t('day.newEvent')" icon="pi pi-plus" size="small" @click="newEvent" />
    </header>
    <div class="page-body">
      <div class="filters">
        <DatePicker
          v-model="range"
          selection-mode="range"
          :manual-input="false"
          :date-format="pickerFormat('date')"
          :placeholder="t('events.anyDate')"
          show-icon
          icon-display="input"
          show-button-bar
          size="small"
          class="range"
        />
        <MultiSelect
          v-model="calendarIds"
          :options="calendarOptions"
          option-label="label"
          option-value="value"
          option-group-label="label"
          option-group-children="items"
          :placeholder="t('common.allCalendars')"
          display="chip"
          filter
          :max-selected-labels="3"
          size="small"
          class="calendars"
        />
        <IconField class="search">
          <InputIcon class="pi pi-search" />
          <InputText v-model="search" :placeholder="t('events.search')" size="small" />
        </IconField>
        <Button :label="t('common.clear')" text size="small" severity="secondary" @click="clearFilters" />
      </div>

      <div class="card">
        <DataTable
          :value="rows"
          lazy
          paginator
          :rows="size"
          :first="page * size"
          :total-records="query.data.value?.totalElements ?? 0"
          :rows-per-page-options="[10, 25, 50, 100]"
          :loading="query.isFetching.value"
          :sort-field="sort"
          :sort-order="direction === 'asc' ? 1 : -1"
          data-key="id"
          row-hover
          @page="onPage"
          @sort="onSort"
          @row-click="editor.show($event.data as CalendarEvent)"
        >
          <template #empty>
            <EmptyState icon="pi pi-inbox" :title="t('events.emptyTitle')" :text="t('events.emptyText')">
              <Button :label="t('day.newEvent')" icon="pi pi-plus" @click="newEvent" />
            </EmptyState>
          </template>
          <Column field="start" :header="t('events.when')" sortable style="width: 240px">
            <template #body="{ data }">
              <div class="when">
                <span>{{ formatDate(data.startDate, 'dateShort') }}</span>
                <span class="muted tabular">{{ formatTimeRange(data.startDate, data.endDate) }}</span>
              </div>
            </template>
          </Column>
          <Column field="title" :header="t('event.title')" sortable>
            <template #body="{ data }">
              <div class="title-cell">
                <span class="title">{{ data.title }}</span>
                <span v-if="data.description" class="muted desc">{{ data.description }}</span>
              </div>
            </template>
          </Column>
          <Column :header="t('event.calendar')" style="width: 260px">
            <template #body="{ data }">
              <div class="calendar-cell">
                <span class="color-dot" :style="{ '--dot': categoryById.get(data.categoryId)?.color }" />
                <div>
                  <div>{{ categoryById.get(data.categoryId)?.name }}</div>
                  <div class="muted path">{{ pathLabel(data.categoryId) }}</div>
                </div>
              </div>
            </template>
          </Column>
          <Column field="place" :header="t('event.place')" sortable style="width: 200px">
            <template #body="{ data }"><span :class="{ muted: !data.place }">{{ data.place || '—' }}</span></template>
          </Column>
          <Column style="width: 110px">
            <template #body="{ data }">
              <div class="row-actions">
                <Button icon="pi pi-pencil" text rounded severity="secondary" :aria-label="t('common.edit')" @click.stop="editor.edit(data)" />
                <Button icon="pi pi-trash" text rounded severity="danger" :aria-label="t('common.delete')" @click.stop="remove(data)" />
              </div>
            </template>
          </Column>
        </DataTable>
      </div>
    </div>
  </div>
</template>

<style scoped>
.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
  align-items: center;
}

.range {
  width: 250px;
}

.calendars {
  min-width: 240px;
  max-width: 420px;
}

.search {
  flex: 1;
  min-width: 220px;
}

.search :deep(input) {
  width: 100%;
}

.when,
.title-cell {
  display: flex;
  flex-direction: column;
}

.title {
  font-weight: 600;
}

.desc,
.path {
  font-size: 12px;
  max-width: 420px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.calendar-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.row-actions {
  display: flex;
  justify-content: flex-end;
}

:deep(.p-datatable-tbody > tr) {
  cursor: pointer;
}
</style>
