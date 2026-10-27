<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import Select from 'primevue/select'
import DatePicker from 'primevue/datepicker'
import { useConfirm } from 'primevue/useconfirm'
import { useI18n } from 'vue-i18n'
import { useEventEditor } from '@/stores/eventEditor'
import { useCatalog } from '@/composables/useCatalog'
import { useDeleteEvent, useSaveEvent } from '@/api/queries'
import { durationMinutes, formatDuration } from '@/lib/time'
import { notify } from '@/lib/notify'
import { pickerFormat } from '@/i18n/dates'

const editor = useEventEditor()
const { calendarOptions, categoryById } = useCatalog()
const saveMutation = useSaveEvent()
const deleteMutation = useDeleteEvent()
const confirm = useConfirm()
const { t } = useI18n()

const form = reactive({
  categoryId: undefined as number | undefined,
  title: '',
  place: '',
  description: '',
  start: new Date(),
  end: new Date(),
})
const submitted = ref(false)

const isEdit = computed(() => editor.editing !== null)

watch(
  () => editor.dialogOpen,
  (open) => {
    if (!open) return
    submitted.value = false
    const e = editor.editing
    if (e) {
      Object.assign(form, {
        categoryId: e.categoryId,
        title: e.title,
        place: e.place ?? '',
        description: e.description ?? '',
        start: new Date(e.start),
        end: new Date(e.end),
      })
    } else {
      const d = editor.draft
      Object.assign(form, {
        categoryId: d?.categoryId,
        title: '',
        place: '',
        description: '',
        start: d?.start ?? new Date(),
        end: d?.end ?? new Date(Date.now() + 3_600_000),
      })
    }
  },
)

/** Moving the start keeps the duration. */
function setStart(value: Date | Date[] | (Date | null)[] | null | undefined) {
  if (!(value instanceof Date)) return
  const delta = value.getTime() - form.start.getTime()
  form.start = value
  form.end = new Date(form.end.getTime() + delta)
}

const errors = computed(() => ({
  title: !form.title.trim() ? t('event.titleRequired') : '',
  categoryId: form.categoryId === undefined ? t('event.chooseCalendar') : '',
  end:
    form.start instanceof Date && form.end instanceof Date && form.end <= form.start ? t('event.endAfterStart') : '',
}))
const valid = computed(() => !errors.value.title && !errors.value.categoryId && !errors.value.end)
const duration = computed(() =>
  form.start instanceof Date && form.end instanceof Date && form.end > form.start
    ? formatDuration(durationMinutes(form.start, form.end))
    : '',
)
const selectedCalendar = computed(() =>
  form.categoryId === undefined ? undefined : categoryById.value.get(form.categoryId),
)

async function save() {
  submitted.value = true
  if (!valid.value) return
  const saved = await saveMutation.mutateAsync({
    id: editor.editing?.id,
    body: {
      categoryId: form.categoryId!,
      title: form.title.trim(),
      place: form.place.trim() || undefined,
      description: form.description.trim() || undefined,
      start: form.start.toISOString(),
      end: form.end.toISOString(),
      version: editor.editing?.version,
    },
  })
  notify('success', isEdit.value ? t('event.updated') : t('event.created'), saved.title)
  if (editor.selected?.id === saved.id) editor.show(saved)
  editor.dialogOpen = false
}

function remove() {
  const event = editor.editing
  if (!event) return
  confirm.require({
    header: t('event.deleteHeader'),
    message: t('event.deleteMessage', { title: event.title }),
    icon: 'pi pi-trash',
    acceptProps: { label: t('common.delete'), severity: 'danger' },
    rejectProps: { label: t('common.cancel'), severity: 'secondary', outlined: true },
    accept: async () => {
      await deleteMutation.mutateAsync(event.id)
      notify('success', t('event.deleted'), event.title)
      if (editor.selected?.id === event.id) editor.show(null)
      editor.dialogOpen = false
    },
  })
}
</script>

<template>
  <Dialog
    v-model:visible="editor.dialogOpen"
    modal
    :header="isEdit ? t('event.editTitle') : t('event.newTitle')"
    :style="{ width: 'min(560px, calc(100vw - 32px))' }"
    :draggable="false"
  >
    <form class="form-grid" @submit.prevent="save">
      <div class="field">
        <label for="ev-title">{{ t('event.title') }}</label>
        <InputText id="ev-title" v-model="form.title" autofocus :invalid="submitted && !!errors.title" maxlength="200" />
        <span v-if="submitted && errors.title" class="error">{{ errors.title }}</span>
      </div>

      <div class="field">
        <label for="ev-calendar">{{ t('event.calendar') }}</label>
        <Select
          v-model="form.categoryId"
          input-id="ev-calendar"
          :options="calendarOptions"
          option-label="label"
          option-value="value"
          option-group-label="label"
          option-group-children="items"
          :placeholder="t('event.chooseCalendar')"
          filter
          :invalid="submitted && !!errors.categoryId"
        >
          <template #value="{ placeholder }">
            <span v-if="selectedCalendar" class="calendar-value">
              <span class="color-dot" :style="{ '--dot': selectedCalendar.color }" />{{ selectedCalendar.name }}
            </span>
            <span v-else class="muted">{{ placeholder }}</span>
          </template>
          <template #optiongroup="{ option }">
            <span class="group-label"><i class="pi pi-folder" /> {{ option.label }}</span>
          </template>
          <template #option="{ option }">
            <span class="calendar-value"><span class="color-dot" :style="{ '--dot': option.color }" />{{ option.label }}</span>
          </template>
        </Select>
        <span v-if="submitted && errors.categoryId" class="error">{{ errors.categoryId }}</span>
      </div>

      <div class="row-2">
        <div class="field">
          <label for="ev-start">{{ t('event.start') }}</label>
          <DatePicker
            :model-value="form.start"
            input-id="ev-start"
            show-time
            hour-format="24"
            :step-minute="5"
            :date-format="pickerFormat('dateTime')"
            :manual-input="false"
            show-icon
            icon-display="input"
            @update:model-value="setStart"
          />
        </div>
        <div class="field">
          <label for="ev-end">{{ t('event.end') }} <span v-if="duration" class="duration">· {{ duration }}</span></label>
          <DatePicker
            v-model="form.end"
            input-id="ev-end"
            show-time
            hour-format="24"
            :step-minute="5"
            :date-format="pickerFormat('dateTime')"
            :manual-input="false"
            show-icon
            icon-display="input"
            :invalid="!!errors.end"
          />
          <span v-if="errors.end" class="error">{{ errors.end }}</span>
        </div>
      </div>

      <div class="field">
        <label for="ev-place">{{ t('event.place') }}</label>
        <InputText id="ev-place" v-model="form.place" maxlength="200" :placeholder="t('event.placePlaceholder')" />
      </div>

      <div class="field">
        <label for="ev-description">{{ t('common.description') }}</label>
        <Textarea id="ev-description" v-model="form.description" rows="3" maxlength="1000" auto-resize />
        <span class="hint">{{ form.description.length }} / 1000</span>
      </div>
      <button type="submit" hidden />
    </form>

    <template #footer>
      <Button
        v-if="isEdit"
        :label="t('common.delete')"
        icon="pi pi-trash"
        severity="danger"
        text
        class="delete"
        @click="remove"
      />
      <Button :label="t('common.cancel')" severity="secondary" outlined @click="editor.dialogOpen = false" />
      <Button
        :label="isEdit ? t('common.save') : t('common.create')"
        icon="pi pi-check"
        :loading="saveMutation.isPending.value"
        @click="save"
      />
    </template>
  </Dialog>
</template>

<style scoped>
.calendar-value {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.group-label {
  font-size: 12px;
  color: var(--app-text-muted);
}

.duration {
  text-transform: none;
  letter-spacing: 0;
  font-weight: 500;
}

.delete {
  margin-right: auto;
}
</style>
