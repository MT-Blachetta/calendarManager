<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import Drawer from 'primevue/drawer'
import Button from 'primevue/button'
import TagChip from '@/components/tag/TagChip.vue'
import { useEventEditor } from '@/stores/eventEditor'
import { useCatalog } from '@/composables/useCatalog'
import { useSchedule } from '@/api/queries'
import { useUiStore } from '@/stores/ui'
import { durationMinutes, formatDuration, formatTimeRange, toDayKey } from '@/lib/time'
import type { CalendarEvent } from '@/api/types'
import { formatDate } from '@/i18n/dates'

const editor = useEventEditor()
const { t } = useI18n()
const ui = useUiStore()
const route = useRoute()
const router = useRouter()
const { categoryById, views, effectiveTags, ancestors, tagById } = useCatalog()

const visible = computed({
  get: () => editor.selected !== null,
  set: (v: boolean) => {
    if (!v) editor.show(null)
  },
})

const event = computed(() => editor.selected)
const start = computed(() => (event.value ? new Date(event.value.start) : new Date()))
const end = computed(() => (event.value ? new Date(event.value.end) : new Date()))
const calendar = computed(() => (event.value ? categoryById.value.get(event.value.categoryId) : undefined))
const path = computed(() => (calendar.value ? ancestors(calendar.value.id) : []))
const tags = computed(() => (calendar.value ? effectiveTags(calendar.value) : []))

/** Concurrent events among the calendars of the current view. */
const viewId = computed(
  () => (views.value.find((v) => v.id === ui.viewId) ?? views.value.find((v) => v.system))?.id,
)
const concurrentRange = computed(() => ({ from: start.value, to: end.value }))
const concurrentQuery = useSchedule(
  computed(() => (event.value ? viewId.value : undefined)),
  concurrentRange,
)

const concurrent = computed(() => {
  const e = event.value
  const data = concurrentQuery.data.value
  if (!e || !data) return []
  const calendars = new Map(data.calendars.map((c) => [c.id, c]))
  return data.events
    .filter((other) => other.id !== e.id)
    .map((other) => {
      const s = new Date(other.start)
      const t = new Date(other.end)
      const overlap = Math.min(t.getTime(), end.value.getTime()) - Math.max(s.getTime(), start.value.getTime())
      return { event: other, start: s, end: t, overlap: Math.round(overlap / 60_000), calendar: calendars.get(other.categoryId) }
    })
    .filter((x) => x.overlap > 0)
    .sort((a, b) => a.start.getTime() - b.start.getTime())
})

const isOtherDay = computed(
  () => route.name !== 'day' || route.params.date !== toDayKey(start.value),
)

function showDay() {
  router.push({ name: 'day', params: { date: toDayKey(start.value) } })
}

function edit() {
  if (event.value) editor.edit(event.value)
}

function open(other: CalendarEvent) {
  editor.show(other)
}
</script>

<template>
  <Drawer
    v-model:visible="visible"
    position="right"
    :modal="false"
    :dismissable="!editor.dialogOpen"
    class="event-drawer"
    :style="{ width: 'min(420px, 100vw)' }"
  >
    <template #header>
      <div class="drawer-head">
        <span class="kicker"><span class="color-dot" :style="{ '--dot': calendar?.color }" /> {{ calendar?.name }}</span>
      </div>
    </template>

    <div v-if="event" class="details" :style="{ '--c': calendar?.color ?? '#64748b' }">
      <h2 class="title">{{ event.title }}</h2>
      <div v-if="path.length" class="breadcrumb">
        <template v-for="(p, i) in path" :key="p.id">
          <span>{{ p.name }}</span><i v-if="i < path.length - 1" class="pi pi-angle-right" />
        </template>
      </div>

      <dl class="facts">
        <div>
          <dt><i class="pi pi-clock" /></dt>
          <dd>
            <div>{{ formatDate(start, 'dayTitle') }}</div>
            <div class="tabular">
              {{ formatTimeRange(start, end) }}
              <span class="muted">· {{ formatDuration(durationMinutes(start, end)) }}</span>
            </div>
          </dd>
        </div>
        <div v-if="event.place">
          <dt><i class="pi pi-map-marker" /></dt>
          <dd>{{ event.place }}</dd>
        </div>
        <div v-if="event.description">
          <dt><i class="pi pi-align-left" /></dt>
          <dd class="description">{{ event.description }}</dd>
        </div>
        <div v-if="tags.length">
          <dt><i class="pi pi-tags" /></dt>
          <dd class="tags">
            <TagChip
              v-for="tag in tags"
              :key="tag.id"
              :name="tag.name"
              :color="tag.color"
              :inherited="!calendar?.tagIds.includes(tag.id)"
            />
          </dd>
        </div>
      </dl>

      <div class="actions">
        <Button :label="t('common.edit')" icon="pi pi-pencil" size="small" @click="edit" />
        <Button v-if="isOtherDay" :label="t('event.showDay')" icon="pi pi-calendar" size="small" severity="secondary" outlined @click="showDay" />
      </div>

      <section class="concurrent">
        <h3>
          {{ t('event.sameTime') }}
          <span class="count">{{ concurrent.length }}</span>
        </h3>
        <p class="muted hint">{{ t('event.sameTimeHint') }}</p>
        <div v-if="!concurrent.length" class="none muted">{{ t('event.nothingElse') }}</div>
        <button
          v-for="item in concurrent"
          :key="item.event.id"
          type="button"
          class="alt"
          :style="{ '--c': item.calendar?.color ?? '#64748b' }"
          @click="open(item.event)"
        >
          <span class="alt-title">{{ item.event.title }}</span>
          <span class="alt-meta">
            <span class="color-dot" :style="{ '--dot': item.calendar?.color }" />
            {{ item.calendar?.name ?? '—' }} · <span class="tabular">{{ formatTimeRange(item.start, item.end) }}</span>
          </span>
          <span class="alt-overlap">{{ t('event.overlaps', { duration: formatDuration(item.overlap) }) }}</span>
          <span v-if="item.calendar?.effectiveTagIds.length" class="alt-tags">
            <TagChip
              v-for="id in item.calendar.effectiveTagIds.slice(0, 3)"
              :key="id"
              :name="tagById.get(id)?.name ?? ''"
              :color="tagById.get(id)?.color ?? '#64748b'"
            />
          </span>
        </button>
      </section>
    </div>
  </Drawer>
</template>

<style scoped>
.drawer-head .kicker {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  font-weight: 650;
  color: var(--app-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.details {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.title {
  font-size: 20px;
  line-height: 1.25;
  padding-left: 12px;
  border-left: 4px solid var(--c);
}

.breadcrumb {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
  font-size: 12.5px;
  color: var(--app-text-muted);
}

.breadcrumb i {
  font-size: 10px;
}

.facts {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 0;
}

.facts > div {
  display: grid;
  grid-template-columns: 22px 1fr;
  gap: 10px;
}

.facts dt {
  color: var(--app-text-muted);
  padding-top: 2px;
}

.facts dd {
  margin: 0;
}

.description {
  white-space: pre-wrap;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.actions {
  display: flex;
  gap: 8px;
}

.concurrent {
  border-top: 1px solid var(--app-border);
  padding-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.concurrent h3 {
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.count {
  font-size: 11px;
  padding: 1px 7px;
  border-radius: 999px;
  background: var(--app-accent-soft);
  color: var(--app-accent);
}

.hint {
  margin: 0;
  font-size: 12px;
}

.none {
  font-size: 13px;
  padding: 10px 0;
}

.alt {
  display: grid;
  gap: 3px;
  text-align: left;
  padding: 10px 12px;
  border: 1px solid var(--app-border);
  border-left: 3px solid var(--c);
  border-radius: 8px;
  background: var(--app-panel);
  color: inherit;
  font: inherit;
  cursor: pointer;
}

.alt:hover {
  background: color-mix(in srgb, var(--c) 8%, var(--app-panel));
}

.alt-title {
  font-weight: 650;
}

.alt-meta {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--app-text-muted);
}

.alt-overlap {
  font-size: 11.5px;
  color: var(--app-accent);
  font-weight: 600;
}

.alt-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}
</style>
