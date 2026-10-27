<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Textarea from 'primevue/textarea'
import Select from 'primevue/select'
import SelectButton from 'primevue/selectbutton'
import MultiSelect from 'primevue/multiselect'
import Message from 'primevue/message'
import { useI18n } from 'vue-i18n'
import ColorSwatches from '@/components/common/ColorSwatches.vue'
import TagChip from '@/components/tag/TagChip.vue'
import { useSaveCategory } from '@/api/queries'
import { useCatalog } from '@/composables/useCatalog'
import { notify } from '@/lib/notify'
import { PALETTE } from '@/lib/colors'
import type { Category, CategoryKind } from '@/api/types'

const props = defineProps<{
  /** Category being edited, or null to create a new one. */
  category: Category | null
  /** Defaults for a new category. */
  defaults?: { parentId?: number | null; kind?: CategoryKind }
}>()

const emit = defineEmits<{ saved: [category: Category]; cancel: []; remove: [category: Category] }>()

const { categories, tags, tagById, categoryById, fullLabel } = useCatalog()
const saveMutation = useSaveCategory()
const { t } = useI18n()

const form = reactive({
  name: '',
  description: '',
  kind: 'CALENDAR' as CategoryKind,
  color: PALETTE[0] as string,
  parentId: null as number | null,
  sortOrder: 0,
  tagIds: [] as number[],
})
const submitted = ref(false)

function reset() {
  submitted.value = false
  const c = props.category
  if (c) {
    Object.assign(form, {
      name: c.name,
      description: c.description ?? '',
      kind: c.kind,
      color: c.color,
      parentId: c.parentId ?? null,
      sortOrder: c.sortOrder,
      tagIds: [...c.tagIds],
    })
  } else {
    const kind = props.defaults?.kind ?? 'CALENDAR'
    const calendarCount = categories.value.filter((x) => x.kind === 'CALENDAR').length
    Object.assign(form, {
      name: '',
      description: '',
      kind,
      color: kind === 'ABSTRACT' ? '#64748B' : PALETTE[calendarCount % (PALETTE.length - 1)],
      parentId: props.defaults?.parentId ?? null,
      sortOrder: 0,
      tagIds: [],
    })
  }
}

watch(() => [props.category, props.defaults], reset, { immediate: true, deep: true })

const isEdit = computed(() => props.category !== null)

const kindOptions = computed(() => [
  {
    label: t('categories.kindGroup'),
    value: 'ABSTRACT',
    icon: 'pi pi-folder',
    disabled: !!props.category && props.category.kind === 'CALENDAR' && props.category.eventCount > 0,
  },
  {
    label: t('categories.kindCalendar'),
    value: 'CALENDAR',
    icon: 'pi pi-calendar',
    disabled: !!props.category && props.category.kind === 'ABSTRACT' && props.category.childCount > 0,
  },
])

const kindLockReason = computed(() => {
  const c = props.category
  if (!c) return ''
  if (c.kind === 'ABSTRACT' && c.childCount > 0) return t('categories.lockGroup')
  if (c.kind === 'CALENDAR' && c.eventCount > 0) return t('categories.lockCalendar')
  return ''
})

/** Possible parents: ABSTRACT categories outside the own subtree. */
const parentOptions = computed(() => {
  const selfId = props.category?.id
  const isInOwnSubtree = (id: number): boolean => {
    let current: number | null | undefined = id
    while (current != null) {
      if (current === selfId) return true
      current = categoryById.value.get(current)?.parentId
    }
    return false
  }
  return categories.value
    .filter((c) => c.kind === 'ABSTRACT' && (selfId === undefined || !isInOwnSubtree(c.id)))
    .map((c) => ({ label: fullLabel(c.id), value: c.id, depth: c.depth }))
})

/** Tags that the category would inherit from the chosen parent (live preview). */
const inherited = computed(() => {
  const parent = form.parentId == null ? undefined : categoryById.value.get(form.parentId)
  if (!parent) return []
  const result = new Map<number, number>()
  for (const t of parent.inheritedTags) result.set(t.tagId, t.sourceCategoryId)
  for (const id of parent.tagIds) result.set(id, parent.id)
  return [...result.entries()]
    .filter(([tagId]) => !form.tagIds.includes(tagId))
    .map(([tagId, sourceId]) => ({ tag: tagById.value.get(tagId), source: categoryById.value.get(sourceId) }))
    .filter((x) => x.tag)
})

const nameError = computed(() => (!form.name.trim() ? t('common.nameRequired') : ''))

async function save() {
  submitted.value = true
  if (nameError.value) return
  const saved = await saveMutation.mutateAsync({
    id: props.category?.id,
    body: {
      name: form.name.trim(),
      description: form.description.trim() || undefined,
      kind: form.kind,
      color: form.color,
      parentId: form.parentId ?? undefined,
      sortOrder: form.sortOrder ?? 0,
      tagIds: form.tagIds,
      version: props.category?.version,
    },
  })
  notify('success', isEdit.value ? t('categories.updated') : t('categories.created'), saved.name)
  emit('saved', saved)
}
</script>

<template>
  <form class="category-form form-grid" @submit.prevent="save">
    <div class="form-title">
      <h2>{{ isEdit ? t('categories.editTitle') : form.kind === 'ABSTRACT' ? t('categories.newGroup') : t('categories.newCalendar') }}</h2>
      <span v-if="isEdit && category" class="muted small">{{ fullLabel(category.id) }}</span>
    </div>

    <div class="field">
      <label>{{ t('categories.kind') }}</label>
      <SelectButton
        v-model="form.kind"
        :options="kindOptions"
        option-label="label"
        option-value="value"
        option-disabled="disabled"
        :allow-empty="false"
      >
        <template #option="{ option }">
          <i :class="option.icon" /><span>{{ option.label }}</span>
        </template>
      </SelectButton>
      <span class="hint">
        {{ kindLockReason || (form.kind === 'ABSTRACT' ? t('categories.hintGroup') : t('categories.hintCalendar')) }}
      </span>
    </div>

    <div class="field">
      <label for="cat-name">{{ t('common.name') }}</label>
      <InputText id="cat-name" v-model="form.name" maxlength="100" :invalid="submitted && !!nameError" />
      <span v-if="submitted && nameError" class="error">{{ nameError }}</span>
    </div>

    <div class="field">
      <label for="cat-parent">{{ t('categories.parent') }}</label>
      <Select
        v-model="form.parentId"
        input-id="cat-parent"
        :options="parentOptions"
        option-label="label"
        option-value="value"
        :placeholder="t('categories.parentPlaceholder')"
        show-clear
        filter
      />
      <span class="hint">{{ t('categories.parentHint') }}</span>
    </div>

    <div class="field">
      <label>{{ t('common.color') }}</label>
      <ColorSwatches v-model="form.color" />
    </div>

    <div class="field">
      <label for="cat-tags">{{ t('common.tags') }}</label>
      <MultiSelect
        v-model="form.tagIds"
        input-id="cat-tags"
        :options="tags"
        option-label="name"
        option-value="id"
        display="chip"
        filter
        :placeholder="t('categories.noOwnTags')"
      >
        <template #option="{ option }">
          <span class="color-square" :style="{ '--dot': option.color }" />
          <span class="opt-label">{{ option.name }}</span>
        </template>
      </MultiSelect>
      <div v-if="inherited.length" class="inherited">
        <span class="hint">{{ t('categories.inherited') }}</span>
        <TagChip
          v-for="item in inherited"
          :key="item.tag!.id"
          :name="item.tag!.name"
          :color="item.tag!.color"
          inherited
          :title="t('categories.inheritedFrom', { name: item.source?.name ?? '' })"
        />
      </div>
    </div>

    <div class="row-2">
      <div class="field">
        <label for="cat-order">{{ t('categories.sortOrder') }}</label>
        <InputNumber v-model="form.sortOrder" input-id="cat-order" show-buttons :min="-999" :max="999" />
      </div>
    </div>

    <div class="field">
      <label for="cat-desc">{{ t('common.description') }}</label>
      <Textarea id="cat-desc" v-model="form.description" rows="2" maxlength="1000" auto-resize />
    </div>

    <Message v-if="isEdit && category && category.kind === 'CALENDAR'" severity="secondary" size="small" variant="simple">
      {{ t('categories.eventsInCalendar', category.eventCount) }}
    </Message>

    <div class="form-actions">
      <Button
        v-if="isEdit && category"
        :label="t('common.delete')"
        icon="pi pi-trash"
        severity="danger"
        text
        @click="emit('remove', category)"
      />
      <span class="spacer" />
      <Button :label="t('common.cancel')" severity="secondary" outlined @click="emit('cancel')" />
      <Button type="submit" :label="isEdit ? t('common.save') : t('common.create')" icon="pi pi-check" :loading="saveMutation.isPending.value" />
    </div>
  </form>
</template>

<style scoped>
.category-form {
  padding: 20px;
}

.form-title {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.form-title h2 {
  font-size: 16px;
}

.small {
  font-size: 12px;
}

.inherited {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}

.opt-label {
  margin-left: 8px;
}

:deep(.p-selectbutton .p-togglebutton-content) {
  gap: 6px;
}

.form-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.spacer {
  flex: 1;
}
</style>
