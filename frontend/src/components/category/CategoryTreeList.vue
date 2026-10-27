<script setup lang="ts">
import { computed, ref } from 'vue'
import Button from 'primevue/button'
import { useI18n } from 'vue-i18n'
import TagChip from '@/components/tag/TagChip.vue'
import type { Category, Tag } from '@/api/types'

const props = defineProps<{
  categories: Category[]
  tagById: Map<number, Tag>
  categoryById: Map<number, Category>
  selectedId: number | null
}>()

const emit = defineEmits<{
  select: [category: Category]
  addChild: [parent: Category]
  remove: [category: Category]
}>()

const { t } = useI18n()
const collapsed = ref(new Set<number>())

/** Categories are delivered in depth-first order; hide rows below collapsed ancestors. */
const rows = computed(() =>
  props.categories.filter((c) => {
    let parentId = c.parentId
    while (parentId != null) {
      if (collapsed.value.has(parentId)) return false
      parentId = props.categoryById.get(parentId)?.parentId
    }
    return true
  }),
)

function toggle(id: number) {
  const next = new Set(collapsed.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  collapsed.value = next
}

function sourceName(categoryId: number) {
  return props.categoryById.get(categoryId)?.name ?? ''
}
</script>

<template>
  <ul class="tree" role="tree">
    <li
      v-for="c in rows"
      :key="c.id"
      role="treeitem"
      :aria-expanded="c.childCount ? !collapsed.has(c.id) : undefined"
      :aria-selected="selectedId === c.id"
      class="row"
      :class="{ selected: selectedId === c.id, abstract: c.kind === 'ABSTRACT' }"
      :style="{ '--depth': c.depth }"
      tabindex="0"
      @click="emit('select', c)"
      @keydown.enter="emit('select', c)"
    >
      <span class="indent" />
      <button
        v-if="c.childCount"
        type="button"
        class="toggle"
        :aria-label="collapsed.has(c.id) ? t('common.expand') : t('common.collapse')"
        @click.stop="toggle(c.id)"
      >
        <i :class="collapsed.has(c.id) ? 'pi pi-chevron-right' : 'pi pi-chevron-down'" />
      </button>
      <span v-else class="toggle placeholder" />
      <i
        class="kind-icon"
        :class="c.kind === 'ABSTRACT' ? 'pi pi-folder' : 'pi pi-calendar'"
        :style="{ color: c.kind === 'CALENDAR' ? c.color : undefined }"
      />
      <span class="name">{{ c.name }}</span>
      <span class="tags">
        <TagChip
          v-for="id in c.tagIds"
          :key="`o${id}`"
          :name="tagById.get(id)?.name ?? '?'"
          :color="tagById.get(id)?.color ?? '#64748b'"
        />
        <TagChip
          v-for="tag in c.inheritedTags"
          :key="`i${tag.tagId}`"
          :name="tagById.get(tag.tagId)?.name ?? '?'"
          :color="tagById.get(tag.tagId)?.color ?? '#64748b'"
          inherited
          :title="t('categories.inheritedFrom', { name: sourceName(tag.sourceCategoryId) })"
        />
      </span>
      <span class="meta muted tabular">
        <template v-if="c.kind === 'CALENDAR'">{{ t('common.eventsCount', c.eventCount) }}</template>
        <template v-else>{{ t('categories.subCount', { n: c.childCount }) }}</template>
      </span>
      <span class="actions">
        <Button
          v-if="c.kind === 'ABSTRACT'"
          v-tooltip.top="t('categories.addSubCategory')"
          icon="pi pi-plus"
          text
          rounded
          size="small"
          severity="secondary"
          :aria-label="t('categories.addSubCategory')"
          @click.stop="emit('addChild', c)"
        />
        <Button
          v-tooltip.top="t('common.delete')"
          icon="pi pi-trash"
          text
          rounded
          size="small"
          severity="danger"
          :aria-label="t('common.delete')"
          @click.stop="emit('remove', c)"
        />
      </span>
    </li>
  </ul>
</template>

<style scoped>
.tree {
  list-style: none;
  margin: 0;
  padding: 6px;
}

.row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 40px;
  padding: 4px 6px;
  border-radius: 8px;
  cursor: pointer;
  outline: none;
}

.row:hover,
.row:focus-visible {
  background: var(--app-hover);
}

.row.selected {
  background: var(--app-accent-soft);
}

.indent {
  flex: none;
  width: calc(var(--depth) * 22px);
  align-self: stretch;
  background-image: repeating-linear-gradient(
    to right,
    transparent 0 10px,
    var(--app-border) 10px 11px,
    transparent 11px 22px
  );
}

.toggle {
  flex: none;
  width: 22px;
  height: 22px;
  display: grid;
  place-items: center;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--app-text-muted);
  cursor: pointer;
  padding: 0;
}

.toggle:hover {
  background: var(--app-border-subtle);
}

.toggle i {
  font-size: 11px;
}

.toggle.placeholder {
  cursor: default;
}

.kind-icon {
  flex: none;
  font-size: 14px;
  color: var(--app-text-muted);
}

.name {
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  min-width: 60px;
}

.abstract .name {
  font-weight: 650;
}

.tags {
  display: flex;
  gap: 4px;
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.meta {
  flex: none;
  font-size: 12px;
}

.actions {
  flex: none;
  display: flex;
  opacity: 0;
  transition: opacity 120ms;
}

.row:hover .actions,
.row:focus-within .actions,
.row.selected .actions {
  opacity: 1;
}

@media (hover: none) {
  .actions {
    opacity: 1;
  }
}
</style>
