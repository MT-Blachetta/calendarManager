<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Button from 'primevue/button'
import { useConfirm } from 'primevue/useconfirm'
import { useI18n } from 'vue-i18n'
import NavToggle from '@/components/layout/NavToggle.vue'
import ViewEditor from '@/components/view/ViewEditor.vue'
import { useDeleteView } from '@/api/queries'
import { useCatalog } from '@/composables/useCatalog'
import { useUiStore } from '@/stores/ui'
import { notify } from '@/lib/notify'
import type { CalendarViewDto, FilterMode } from '@/api/types'

const route = useRoute()
const router = useRouter()
const ui = useUiStore()
const confirm = useConfirm()
const deleteMutation = useDeleteView()
const { t } = useI18n()
const { views, categoryById, tagById, viewName } = useCatalog()

const isNew = computed(() => route.params.id === 'new')
const selected = computed(() => {
  if (isNew.value) return null
  const id = Number(route.params.id)
  return views.value.find((v) => v.id === id) ?? views.value[0] ?? null
})

const modeIcon: Record<FilterMode, string> = {
  ALL: 'pi pi-th-large',
  INCLUDE: 'pi pi-filter',
  EXCLUDE: 'pi pi-filter-slash',
}

function summary(v: CalendarViewDto): string {
  if (v.filterMode === 'ALL') return t('common.allCalendars')
  const cats = v.categoryIds.map((id) => categoryById.value.get(id)?.name).filter(Boolean)
  const match = v.matchTagIds.map((id) => tagById.value.get(id)?.name).filter(Boolean)
  const except = v.exceptTagIds.map((id) => tagById.value.get(id)?.name).filter(Boolean)
  const parts: string[] = []
  if (v.filterMode === 'INCLUDE') {
    if (cats.length) parts.push(t('views.summaryIn', { list: cats.join(', ') }))
    if (match.length) parts.push(t('views.summaryTagged', { list: match.join(' / ') }))
    return t('views.summaryOnly', { parts: parts.join(` ${t('common.and')} `) })
  }
  if (cats.length) parts.push(cats.join(', '))
  if (match.length) parts.push(t('views.summaryTagged', { list: match.join(' / ') }))
  const text = t('views.summaryHide', { parts: parts.join(` ${t('common.or')} `) || t('views.summaryNothing') })
  return except.length ? t('views.summaryExcept', { text, list: except.join(' / ') }) : text
}

function select(v: CalendarViewDto) {
  router.push({ name: 'views', params: { id: v.id } })
}

function open(v: CalendarViewDto) {
  ui.viewId = v.id
  router.push({ name: 'day', params: { date: ui.lastDay } })
}

function remove(v: CalendarViewDto) {
  confirm.require({
    header: t('views.deleteHeader'),
    message: t('views.deleteMessage', { name: v.name }),
    icon: 'pi pi-trash',
    acceptProps: { label: t('common.delete'), severity: 'danger' },
    rejectProps: { label: t('common.cancel'), severity: 'secondary', outlined: true },
    accept: async () => {
      await deleteMutation.mutateAsync(v.id)
      if (ui.viewId === v.id) ui.viewId = null
      notify('success', t('views.deleted'), v.name)
      router.push({ name: 'views' })
    },
  })
}
</script>

<template>
  <div class="page">
    <header class="page-header">
      <NavToggle />
      <h1>{{ t('views.title') }}</h1>
      <span class="muted">{{ t('views.subtitle') }}</span>
      <div class="spacer" />
      <Button :label="t('views.newView')" icon="pi pi-plus" size="small" @click="router.push({ name: 'views', params: { id: 'new' } })" />
    </header>
    <div class="page-body">
      <div class="layout">
        <nav class="card list" :aria-label="t('views.title')">
          <button
            v-for="v in views"
            :key="v.id"
            type="button"
            class="view-item"
            :class="{ active: selected?.id === v.id && !isNew }"
            @click="select(v)"
          >
            <i :class="modeIcon[v.filterMode]" class="mode" />
            <span class="text">
              <span class="name">{{ viewName(v) }}<span v-if="v.system" class="badge">{{ t('views.defaultBadge') }}</span></span>
              <span class="summary muted">{{ summary(v) }}</span>
            </span>
          </button>
          <button v-if="isNew" type="button" class="view-item active">
            <i class="pi pi-plus mode" />
            <span class="text"><span class="name">{{ t('views.newView') }}</span></span>
          </button>
        </nav>
        <div class="card">
          <ViewEditor
            v-if="selected || isNew"
            :key="isNew ? 'new' : selected?.id"
            :view="selected"
            @saved="(v) => router.push({ name: 'views', params: { id: v.id } })"
            @remove="remove"
            @open="open"
            @cancel="router.push({ name: 'views' })"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.layout {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.list {
  display: flex;
  flex-direction: column;
  padding: 6px;
  gap: 2px;
}

.view-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 10px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.view-item:hover {
  background: var(--app-hover);
}

.view-item.active {
  background: var(--app-accent-soft);
}

.mode {
  margin-top: 3px;
  color: var(--app-text-muted);
}

.active .mode {
  color: var(--app-accent);
}

.text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.name {
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
}

.badge {
  font-size: 10px;
  font-weight: 650;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding: 1px 6px;
  border-radius: 999px;
  color: var(--app-accent);
  background: var(--app-accent-soft);
}

.summary {
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

@media (max-width: 900px) {
  .layout {
    grid-template-columns: 1fr;
  }
}
</style>
