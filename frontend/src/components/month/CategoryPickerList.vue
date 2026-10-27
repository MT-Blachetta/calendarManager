<script setup lang="ts">
import { computed, ref } from 'vue'
import InputText from 'primevue/inputtext'
import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'
import { useI18n } from 'vue-i18n'
import type { Category } from '@/api/types'

const props = defineProps<{
  /** All categories in depth-first order. */
  categories: Category[]
  categoryById: Map<number, Category>
  /** Selected calendar or group; null = all calendars. */
  selectedId: number | null
}>()

const emit = defineEmits<{ select: [id: number | null] }>()

const { t } = useI18n()
const search = ref('')
const collapsed = ref(new Set<number>())

/** Number of calendars in the subtree of every category. */
const calendarCounts = computed(() => {
  const counts = new Map<number, number>()
  for (const c of props.categories) {
    if (c.kind !== 'CALENDAR') continue
    let id: number | null | undefined = c.id
    while (id != null) {
      counts.set(id, (counts.get(id) ?? 0) + 1)
      id = props.categoryById.get(id)?.parentId
    }
  }
  return counts
})

const totalCalendars = computed(() => props.categories.filter((c) => c.kind === 'CALENDAR').length)

function ancestorIds(c: Category): number[] {
  const ids: number[] = []
  let parentId = c.parentId
  while (parentId != null) {
    ids.push(parentId)
    parentId = props.categoryById.get(parentId)?.parentId
  }
  return ids
}

const rows = computed(() => {
  const q = search.value.trim().toLowerCase()
  if (q) {
    // Show matches together with their ancestors for context.
    const visible = new Set<number>()
    for (const c of props.categories) {
      if (c.name.toLowerCase().includes(q)) {
        visible.add(c.id)
        ancestorIds(c).forEach((id) => visible.add(id))
      }
    }
    return props.categories.filter((c) => visible.has(c.id))
  }
  return props.categories.filter((c) => !ancestorIds(c).some((id) => collapsed.value.has(id)))
})

function toggle(id: number) {
  const next = new Set(collapsed.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  collapsed.value = next
}
</script>

<template>
  <nav class="picker" :aria-label="t('month.pickerAria')">
    <div class="picker-head">
      <span class="title">{{ t('month.pickerTitle') }}</span>
      <IconField v-if="categories.length > 8" class="search">
        <InputIcon class="pi pi-search" />
        <InputText v-model="search" size="small" :placeholder="t('common.filter')" :aria-label="t('month.filterCalendars')" />
      </IconField>
    </div>
    <ul class="list" role="listbox" :aria-activedescendant="`cat-${selectedId ?? 'all'}`">
      <li
        id="cat-all"
        role="option"
        :aria-selected="selectedId === null"
        class="row"
        :class="{ selected: selectedId === null }"
        tabindex="0"
        @click="emit('select', null)"
        @keydown.enter.space.prevent="emit('select', null)"
      >
        <span class="toggle placeholder" />
        <i class="pi pi-th-large kind" />
        <span class="name">{{ t('common.allCalendars') }}</span>
        <span class="count tabular">{{ totalCalendars }}</span>
      </li>
      <li
        v-for="c in rows"
        :id="`cat-${c.id}`"
        :key="c.id"
        role="option"
        :aria-selected="selectedId === c.id"
        class="row"
        :class="{ selected: selectedId === c.id, group: c.kind === 'ABSTRACT' }"
        :style="{ '--depth': c.depth }"
        tabindex="0"
        @click="emit('select', c.id)"
        @keydown.enter.space.prevent="emit('select', c.id)"
      >
        <span class="indent" />
        <button
          v-if="c.childCount && !search"
          type="button"
          class="toggle"
          :aria-label="collapsed.has(c.id) ? t('month.expandNamed', { name: c.name }) : t('month.collapseNamed', { name: c.name })"
          @click.stop="toggle(c.id)"
        >
          <i :class="collapsed.has(c.id) ? 'pi pi-chevron-right' : 'pi pi-chevron-down'" />
        </button>
        <span v-else class="toggle placeholder" />
        <i v-if="c.kind === 'ABSTRACT'" class="pi pi-folder kind" />
        <span v-else class="color-dot kind-dot" :style="{ '--dot': c.color }" />
        <span class="name" :title="c.name">{{ c.name }}</span>
        <span v-if="c.kind === 'ABSTRACT'" class="count tabular" :title="t('common.calendarsCount', calendarCounts.get(c.id) ?? 0)">
          {{ calendarCounts.get(c.id) ?? 0 }}
        </span>
      </li>
      <li v-if="search && !rows.length" class="none muted">{{ t('common.noMatch') }}</li>
    </ul>
  </nav>
</template>

<style scoped>
.picker {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.picker-head {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px 12px 8px;
}

.title {
  font-size: 11px;
  font-weight: 650;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--app-text-muted);
  padding: 0 4px;
}

.search :deep(input) {
  width: 100%;
}

.list {
  list-style: none;
  margin: 0;
  padding: 0 8px 12px;
  overflow-y: auto;
  min-height: 0;
}

.row {
  display: flex;
  align-items: center;
  gap: 6px;
  min-height: 34px;
  padding: 2px 8px 2px 4px;
  border-radius: 7px;
  cursor: pointer;
  outline: none;
  user-select: none;
}

.row:hover,
.row:focus-visible {
  background: var(--app-hover);
}

.row:focus-visible {
  box-shadow: inset 0 0 0 2px var(--app-accent);
}

.row.selected {
  background: var(--app-accent-soft);
  color: var(--app-accent);
  font-weight: 600;
}

.indent {
  flex: none;
  width: calc(var(--depth, 0) * 16px);
}

.toggle {
  flex: none;
  width: 20px;
  height: 20px;
  display: grid;
  place-items: center;
  border: 0;
  border-radius: 5px;
  background: transparent;
  color: var(--app-text-muted);
  cursor: pointer;
  padding: 0;
}

.toggle i {
  font-size: 10px;
}

.toggle:hover {
  background: var(--app-border-subtle);
}

.toggle.placeholder {
  cursor: inherit;
}

.kind {
  flex: none;
  width: 16px;
  font-size: 13px;
  color: var(--app-text-muted);
  text-align: center;
}

.selected .kind {
  color: var(--app-accent);
}

.kind-dot {
  margin: 0 3px;
}

.name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.group .name {
  font-weight: 600;
}

.count {
  flex: none;
  font-size: 11px;
  color: var(--app-text-muted);
  padding: 0 6px;
  border-radius: 999px;
  background: var(--app-border-subtle);
}

.none {
  padding: 8px 12px;
  font-size: 13px;
}
</style>
