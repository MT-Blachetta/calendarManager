<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'
import { useConfirm } from 'primevue/useconfirm'
import { useI18n } from 'vue-i18n'
import NavToggle from '@/components/layout/NavToggle.vue'
import TagChip from '@/components/tag/TagChip.vue'
import ColorSwatches from '@/components/common/ColorSwatches.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { useDeleteTag, useSaveTag, useTags } from '@/api/queries'
import { DEFAULT_TAG_COLOR } from '@/lib/colors'
import { notify } from '@/lib/notify'
import type { Tag } from '@/api/types'

const tagsQuery = useTags()
const saveMutation = useSaveTag()
const deleteMutation = useDeleteTag()
const confirm = useConfirm()
const { t } = useI18n()

const search = ref('')
const tags = computed(() => {
  const q = search.value.trim().toLowerCase()
  const all = tagsQuery.data.value ?? []
  return q ? all.filter((t) => t.name.toLowerCase().includes(q) || t.description?.toLowerCase().includes(q)) : all
})

const dialogOpen = ref(false)
const editing = ref<Tag | null>(null)
const submitted = ref(false)
const form = reactive({ name: '', color: DEFAULT_TAG_COLOR, description: '' })

function openCreate() {
  editing.value = null
  Object.assign(form, { name: '', color: '#0EA5E9', description: '' })
  submitted.value = false
  dialogOpen.value = true
}

function openEdit(tag: Tag) {
  editing.value = tag
  Object.assign(form, { name: tag.name, color: tag.color, description: tag.description ?? '' })
  submitted.value = false
  dialogOpen.value = true
}

async function save() {
  submitted.value = true
  if (!form.name.trim()) return
  const saved = await saveMutation.mutateAsync({
    id: editing.value?.id,
    body: {
      name: form.name.trim(),
      color: form.color,
      description: form.description.trim() || undefined,
      version: editing.value?.version,
    },
  })
  notify('success', editing.value ? t('tags.updated') : t('tags.created'), saved.name)
  dialogOpen.value = false
}

function remove(tag: Tag) {
  confirm.require({
    header: t('tags.deleteHeader'),
    message: t('tags.deleteUsed', { name: tag.name, n: tag.usageCount }, tag.usageCount),
    icon: 'pi pi-trash',
    acceptProps: { label: t('common.delete'), severity: 'danger' },
    rejectProps: { label: t('common.cancel'), severity: 'secondary', outlined: true },
    accept: async () => {
      await deleteMutation.mutateAsync(tag.id)
      notify('success', t('tags.deleted'), tag.name)
    },
  })
}
</script>

<template>
  <div class="page">
    <header class="page-header">
      <NavToggle />
      <h1>{{ t('tags.title') }}</h1>
      <span class="muted">{{ t('tags.subtitle') }}</span>
      <div class="spacer" />
      <IconField>
        <InputIcon class="pi pi-search" />
        <InputText v-model="search" :placeholder="t('tags.search')" size="small" />
      </IconField>
      <Button :label="t('tags.newTag')" icon="pi pi-plus" size="small" @click="openCreate" />
    </header>
    <div class="page-body">
      <div class="card">
        <DataTable
          :value="tags"
          :loading="tagsQuery.isLoading.value"
          data-key="id"
          sort-field="name"
          :sort-order="1"
          row-hover
          @row-click="openEdit($event.data as Tag)"
        >
          <template #empty>
            <EmptyState icon="pi pi-tags" :title="t('tags.emptyTitle')" :text="t('tags.emptyText')">
              <Button :label="t('tags.newTag')" icon="pi pi-plus" @click="openCreate" />
            </EmptyState>
          </template>
          <Column field="name" :header="t('tags.tag')" sortable>
            <template #body="{ data }">
              <TagChip :name="data.name" :color="data.color" />
            </template>
          </Column>
          <Column field="description" :header="t('common.description')">
            <template #body="{ data }"><span class="muted">{{ data.description || '—' }}</span></template>
          </Column>
          <Column field="usageCount" :header="t('tags.usage')" sortable style="width: 130px">
            <template #body="{ data }"><span class="tabular">{{ data.usageCount }}</span></template>
          </Column>
          <Column style="width: 110px; text-align: right">
            <template #body="{ data }">
              <div class="row-actions">
                <Button icon="pi pi-pencil" text rounded severity="secondary" :aria-label="t('common.edit')" @click.stop="openEdit(data)" />
                <Button icon="pi pi-trash" text rounded severity="danger" :aria-label="t('common.delete')" @click.stop="remove(data)" />
              </div>
            </template>
          </Column>
        </DataTable>
      </div>
    </div>

    <Dialog
      v-model:visible="dialogOpen"
      modal
      :header="editing ? t('tags.editTag') : t('tags.newTag')"
      :style="{ width: 'min(460px, calc(100vw - 32px))' }"
      :draggable="false"
    >
      <form class="form-grid" @submit.prevent="save">
        <div class="field">
          <label for="tag-name">{{ t('common.name') }}</label>
          <InputText id="tag-name" v-model="form.name" autofocus maxlength="50" :invalid="submitted && !form.name.trim()" />
          <span v-if="submitted && !form.name.trim()" class="error">{{ t('common.nameRequired') }}</span>
        </div>
        <div class="field">
          <label>{{ t('common.color') }}</label>
          <ColorSwatches v-model="form.color" />
        </div>
        <div class="field">
          <label for="tag-desc">{{ t('common.description') }}</label>
          <Textarea id="tag-desc" v-model="form.description" rows="2" maxlength="500" auto-resize />
        </div>
        <div class="field">
          <label>{{ t('common.preview') }}</label>
          <div><TagChip :name="form.name || t('tags.tag')" :color="form.color" /></div>
        </div>
        <button type="submit" hidden />
      </form>
      <template #footer>
        <Button :label="t('common.cancel')" severity="secondary" outlined @click="dialogOpen = false" />
        <Button :label="editing ? t('common.save') : t('common.create')" icon="pi pi-check" :loading="saveMutation.isPending.value" @click="save" />
      </template>
    </Dialog>
  </div>
</template>

<style scoped>
.row-actions {
  display: flex;
  justify-content: flex-end;
}

:deep(.p-datatable-tbody > tr) {
  cursor: pointer;
}
</style>
