<script setup lang="ts">
import { computed, ref } from 'vue'
import Button from 'primevue/button'
import { useConfirm } from 'primevue/useconfirm'
import { useI18n } from 'vue-i18n'
import NavToggle from '@/components/layout/NavToggle.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import CategoryTreeList from '@/components/category/CategoryTreeList.vue'
import CategoryForm from '@/components/category/CategoryForm.vue'
import { fetchCategoryUsage, useDeleteCategory } from '@/api/queries'
import { useCatalog } from '@/composables/useCatalog'
import { notify, notifyError } from '@/lib/notify'
import type { Category, CategoryKind } from '@/api/types'

const { categories, tagById, categoryById, loading } = useCatalog()
const deleteMutation = useDeleteCategory()
const confirm = useConfirm()
const { t } = useI18n()

type Mode = { type: 'none' } | { type: 'edit'; id: number } | { type: 'create'; parentId: number | null; kind: CategoryKind }
const mode = ref<Mode>({ type: 'none' })

const selected = computed(() => (mode.value.type === 'edit' ? (categoryById.value.get(mode.value.id) ?? null) : null))
const defaults = computed(() =>
  mode.value.type === 'create' ? { parentId: mode.value.parentId, kind: mode.value.kind } : undefined,
)
const stats = computed(() => ({
  groups: categories.value.filter((c) => c.kind === 'ABSTRACT').length,
  calendars: categories.value.filter((c) => c.kind === 'CALENDAR').length,
}))

function create(kind: CategoryKind, parentId: number | null = null) {
  mode.value = { type: 'create', parentId, kind }
}

function onSaved(category: Category) {
  mode.value = { type: 'edit', id: category.id }
}

async function remove(category: Category) {
  let usage
  try {
    usage = await fetchCategoryUsage(category.id)
  } catch (error) {
    notifyError(error)
    return
  }
  const cascade = usage.descendantCount > 0 || usage.eventCount > 0
  const parts = [
    usage.descendantCount && t('categories.subCategoriesCount', usage.descendantCount),
    usage.eventCount && t('common.eventsCount', usage.eventCount),
  ].filter(Boolean)
  const message = cascade
    ? t('categories.deleteCascade', { parts: parts.join(` ${t('common.and')} `) })
    : t('categories.deleteSimple')
  const viewNote = usage.viewCount ? t('categories.deleteViewNote', usage.viewCount) : ''
  confirm.require({
    header: t('categories.deleteHeader', { name: category.name }),
    message: [message, viewNote].filter(Boolean).join(' '),
    icon: 'pi pi-exclamation-triangle',
    acceptProps: { label: cascade ? t('common.deleteAll') : t('common.delete'), severity: 'danger' },
    rejectProps: { label: t('common.cancel'), severity: 'secondary', outlined: true },
    accept: async () => {
      await deleteMutation.mutateAsync({ id: category.id, cascade })
      notify('success', t('categories.deleted'), category.name)
      if (mode.value.type === 'edit' && mode.value.id === category.id) mode.value = { type: 'none' }
    },
  })
}
</script>

<template>
  <div class="page">
    <header class="page-header">
      <NavToggle />
      <h1>{{ t('categories.title') }}</h1>
      <span class="muted">{{ t('categories.groupsCount', stats.groups) }} · {{ t('common.calendarsCount', stats.calendars) }}</span>
      <div class="spacer" />
      <Button :label="t('categories.newGroup')" icon="pi pi-folder-plus" size="small" severity="secondary" outlined @click="create('ABSTRACT')" />
      <Button :label="t('categories.newCalendar')" icon="pi pi-plus" size="small" @click="create('CALENDAR')" />
    </header>
    <div class="page-body">
      <div class="layout" :class="{ 'with-panel': mode.type !== 'none' }">
        <div class="card tree-card">
          <EmptyState
            v-if="!loading && !categories.length"
            icon="pi pi-sitemap"
            :title="t('categories.emptyTitle')"
            :text="t('categories.emptyText')"
          >
            <Button :label="t('categories.newGroup')" icon="pi pi-folder-plus" outlined @click="create('ABSTRACT')" />
            <Button :label="t('categories.newCalendar')" icon="pi pi-plus" @click="create('CALENDAR')" />
          </EmptyState>
          <CategoryTreeList
            v-else
            :categories="categories"
            :tag-by-id="tagById"
            :category-by-id="categoryById"
            :selected-id="selected?.id ?? null"
            @select="mode = { type: 'edit', id: $event.id }"
            @add-child="create('CALENDAR', $event.id)"
            @remove="remove"
          />
        </div>
        <div v-if="mode.type !== 'none'" class="card panel">
          <CategoryForm
            :key="mode.type === 'edit' ? `e${mode.id}` : `c${defaults?.parentId}-${defaults?.kind}`"
            :category="selected"
            :defaults="defaults"
            @saved="onSaved"
            @cancel="mode = { type: 'none' }"
            @remove="remove"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.layout {
  display: grid;
  gap: 16px;
  grid-template-columns: 1fr;
  align-items: start;
}

.layout.with-panel {
  grid-template-columns: minmax(0, 1fr) minmax(340px, 420px);
}

.panel {
  position: sticky;
  top: 0;
}

@media (max-width: 1100px) {
  .layout.with-panel {
    grid-template-columns: 1fr;
  }
  .panel {
    position: static;
    order: -1;
  }
}
</style>
