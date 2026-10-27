<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { refDebounced } from '@vueuse/core'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import SelectButton from 'primevue/selectbutton'
import MultiSelect from 'primevue/multiselect'
import TreeSelect from 'primevue/treeselect'
import Message from 'primevue/message'
import type { TreeNode } from 'primevue/treenode'
import { useI18n } from 'vue-i18n'
import { useResolvedFilter, useSaveView } from '@/api/queries'
import { useCatalog } from '@/composables/useCatalog'
import { notify } from '@/lib/notify'
import type { CalendarViewDto, FilterMode, ViewFilterRequest } from '@/api/types'

const props = defineProps<{ view: CalendarViewDto | null }>()
const emit = defineEmits<{ saved: [view: CalendarViewDto]; remove: [view: CalendarViewDto]; open: [view: CalendarViewDto]; cancel: [] }>()

const { categories, tags, calendars } = useCatalog()
const saveMutation = useSaveView()
const { t } = useI18n()

const form = reactive({
  name: '',
  description: '',
  filterMode: 'INCLUDE' as FilterMode,
  categoryIds: [] as number[],
  matchTagIds: [] as number[],
  exceptTagIds: [] as number[],
  sortOrder: 0,
})
const submitted = ref(false)

watch(
  () => props.view,
  (v) => {
    submitted.value = false
    Object.assign(form, {
      name: v?.name ?? '',
      description: v?.description ?? '',
      filterMode: v?.filterMode ?? 'INCLUDE',
      categoryIds: [...(v?.categoryIds ?? [])],
      matchTagIds: [...(v?.matchTagIds ?? [])],
      exceptTagIds: [...(v?.exceptTagIds ?? [])],
      sortOrder: v?.sortOrder ?? 0,
    })
  },
  { immediate: true },
)

const isSystem = computed(() => props.view?.system ?? false)

const modeOptions = computed(() => [
  { label: t('views.modeAll'), value: 'ALL', icon: 'pi pi-th-large' },
  { label: t('views.modeInclude'), value: 'INCLUDE', icon: 'pi pi-filter' },
  { label: t('views.modeExclude'), value: 'EXCLUDE', icon: 'pi pi-filter-slash' },
])

// ---- category tree for the subtree picker
const treeNodes = computed<TreeNode[]>(() => {
  const nodes = new Map<number, TreeNode>()
  const roots: TreeNode[] = []
  for (const c of categories.value) {
    nodes.set(c.id, {
      key: String(c.id),
      label: c.name,
      icon: c.kind === 'ABSTRACT' ? 'pi pi-folder' : 'pi pi-calendar',
      data: c,
      children: [],
    })
  }
  for (const c of categories.value) {
    const node = nodes.get(c.id)!
    const parent = c.parentId != null ? nodes.get(c.parentId) : undefined
    if (parent) parent.children!.push(node)
    else roots.push(node)
  }
  return roots
})

const categorySelection = computed({
  get: () => Object.fromEntries(form.categoryIds.map((id) => [String(id), true])),
  set: (value: Record<string, boolean> | null) => {
    form.categoryIds = Object.entries(value ?? {})
      .filter(([, selected]) => selected)
      .map(([key]) => Number(key))
  },
})

// Tags must not be both hidden and exceptions
watch(
  () => form.matchTagIds,
  (ids) => (form.exceptTagIds = form.exceptTagIds.filter((id) => !ids.includes(id))),
)
const exceptOptions = computed(() => tags.value.filter((t) => !form.matchTagIds.includes(t.id)))

// ---- live preview
const filter = computed<ViewFilterRequest>(() => ({
  filterMode: form.filterMode,
  categoryIds: form.filterMode === 'ALL' ? [] : form.categoryIds,
  matchTagIds: form.filterMode === 'ALL' ? [] : form.matchTagIds,
  exceptTagIds: form.filterMode === 'EXCLUDE' ? form.exceptTagIds : [],
}))
const debouncedFilter = refDebounced(filter, 250)
const preview = useResolvedFilter(debouncedFilter)
const visibleCalendars = computed(() => preview.data.value ?? [])

const errors = computed(() => ({
  name: !form.name.trim() ? t('common.nameRequired') : '',
  filter:
    form.filterMode === 'INCLUDE' && !form.categoryIds.length && !form.matchTagIds.length
      ? t('views.filterRequired')
      : '',
}))

async function save() {
  submitted.value = true
  if (errors.value.name || errors.value.filter) return
  const saved = await saveMutation.mutateAsync({
    id: props.view?.id,
    body: {
      name: form.name.trim(),
      description: form.description.trim() || undefined,
      ...filter.value,
      sortOrder: form.sortOrder,
      version: props.view?.version,
    },
  })
  notify('success', props.view ? t('views.updated') : t('views.created'), saved.name)
  emit('saved', saved)
}
</script>

<template>
  <form class="editor" @submit.prevent="save">
    <div class="form-grid main">
      <div class="field">
        <label for="view-name">{{ t('common.name') }}</label>
        <InputText id="view-name" v-model="form.name" maxlength="100" :invalid="submitted && !!errors.name" />
        <span v-if="submitted && errors.name" class="error">{{ errors.name }}</span>
      </div>
      <div class="field">
        <label for="view-desc">{{ t('common.description') }}</label>
        <Textarea id="view-desc" v-model="form.description" rows="2" maxlength="500" auto-resize />
      </div>

      <Message v-if="isSystem" severity="info" size="small">
        {{ t('views.systemInfo') }}
      </Message>

      <div class="field">
        <label>{{ t('views.filterMode') }}</label>
        <SelectButton
          v-model="form.filterMode"
          :options="modeOptions"
          option-label="label"
          option-value="value"
          :allow-empty="false"
          :disabled="isSystem"
        >
          <template #option="{ option }"><i :class="option.icon" /><span>{{ option.label }}</span></template>
        </SelectButton>
        <span class="hint">
          <template v-if="form.filterMode === 'ALL'">{{ t('views.hintAll') }}</template>
          <i18n-t v-else-if="form.filterMode === 'INCLUDE'" keypath="views.hintInclude" scope="global">
            <template #and><b>{{ t('common.and') }}</b></template>
          </i18n-t>
          <i18n-t v-else keypath="views.hintExclude" scope="global">
            <template #or><b>{{ t('common.or') }}</b></template>
          </i18n-t>
        </span>
      </div>

      <template v-if="form.filterMode !== 'ALL'">
        <div class="field">
          <label>{{ form.filterMode === 'INCLUDE' ? t('views.showCategories') : t('views.hideCategories') }}</label>
          <TreeSelect
            v-model="categorySelection"
            :options="treeNodes"
            selection-mode="multiple"
            :meta-key-selection="false"
            display="chip"
            :placeholder="t('views.noCategories')"
            filter
            fluid
          />
          <span class="hint">{{ t('views.groupIncludesSubs') }}</span>
        </div>
        <div class="field">
          <label>{{ form.filterMode === 'INCLUDE' ? t('views.includeTags') : t('views.hideTags') }}</label>
          <MultiSelect
            v-model="form.matchTagIds"
            :options="tags"
            option-label="name"
            option-value="id"
            display="chip"
            filter
            :placeholder="t('views.noTags')"
          >
            <template #option="{ option }">
              <span class="color-square" :style="{ '--dot': option.color }" /><span class="opt">{{ option.name }}</span>
            </template>
          </MultiSelect>
          <span class="hint">{{ t('views.inheritedCount') }}</span>
        </div>
        <div v-if="form.filterMode === 'EXCLUDE'" class="field">
          <label>{{ t('views.exceptTags') }}</label>
          <MultiSelect
            v-model="form.exceptTagIds"
            :options="exceptOptions"
            option-label="name"
            option-value="id"
            display="chip"
            filter
            :placeholder="t('views.noExceptions')"
          >
            <template #option="{ option }">
              <span class="color-square" :style="{ '--dot': option.color }" /><span class="opt">{{ option.name }}</span>
            </template>
          </MultiSelect>
        </div>
      </template>
      <span v-if="submitted && errors.filter" class="error">{{ errors.filter }}</span>

      <div class="form-actions">
        <Button
          v-if="view && !view.system"
          :label="t('common.delete')"
          icon="pi pi-trash"
          severity="danger"
          text
          @click="emit('remove', view)"
        />
        <span class="spacer" />
        <Button v-if="view" :label="t('common.open')" icon="pi pi-calendar" severity="secondary" outlined @click="emit('open', view)" />
        <Button v-else :label="t('common.cancel')" severity="secondary" outlined @click="emit('cancel')" />
        <Button type="submit" :label="view ? t('common.save') : t('common.create')" icon="pi pi-check" :loading="saveMutation.isPending.value" />
      </div>
    </div>

    <aside class="preview">
      <div class="preview-head">
        <h3>{{ t('common.preview') }}</h3>
        <i18n-t keypath="views.previewCount" tag="span" class="tabular" scope="global">
          <template #visible><b>{{ visibleCalendars.length }}</b></template>
          <template #total>{{ calendars.length }}</template>
        </i18n-t>
      </div>
      <ul class="preview-list">
        <li v-for="c in visibleCalendars" :key="c.id">
          <span class="color-dot" :style="{ '--dot': c.color }" />
          <span class="name">{{ c.name }}</span>
          <span class="muted path">{{ c.path.map((p) => p.name).join(' › ') }}</span>
        </li>
        <li v-if="!visibleCalendars.length" class="muted none">{{ t('views.previewNone') }}</li>
      </ul>
    </aside>
  </form>
</template>

<style scoped>
.editor {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 0;
}

.main {
  padding: 20px;
  border-right: 1px solid var(--app-border);
}

:deep(.p-selectbutton .p-togglebutton-content) {
  gap: 6px;
}

.opt {
  margin-left: 8px;
}

.form-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.spacer {
  flex: 1;
}

.preview {
  padding: 20px;
  background: color-mix(in srgb, var(--p-primary-500) 3%, var(--app-panel));
  border-radius: 0 var(--app-radius) var(--app-radius) 0;
}

.preview-head {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-bottom: 12px;
}

.preview-head h3 {
  font-size: 14px;
}

.preview-head span {
  font-size: 12.5px;
  color: var(--app-text-muted);
}

.preview-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.preview-list li {
  display: grid;
  grid-template-columns: 10px 1fr;
  column-gap: 8px;
  align-items: center;
  padding: 6px 8px;
  border-radius: 6px;
  background: var(--app-panel);
  border: 1px solid var(--app-border-subtle);
}

.preview-list .name {
  font-weight: 600;
  font-size: 13px;
}

.preview-list .path {
  grid-column: 2;
  font-size: 11.5px;
}

.preview-list .none {
  display: block;
  border: 0;
  background: transparent;
}

@media (max-width: 900px) {
  .editor {
    grid-template-columns: 1fr;
  }
  .main {
    border-right: 0;
    border-bottom: 1px solid var(--app-border);
  }
  .preview {
    border-radius: 0 0 var(--app-radius) var(--app-radius);
  }
}
</style>
