import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { CalendarEvent } from '@/api/types'

export interface EventDraft {
  categoryId?: number
  start: Date
  end: Date
}

/** Global state of the event dialog and the event details drawer (opened from grid, top bar, lists). */
export const useEventEditor = defineStore('eventEditor', () => {
  const dialogOpen = ref(false)
  const editing = ref<CalendarEvent | null>(null)
  const draft = ref<EventDraft | null>(null)
  const selected = ref<CalendarEvent | null>(null)

  function create(initial: EventDraft) {
    editing.value = null
    draft.value = initial
    dialogOpen.value = true
  }

  function edit(event: CalendarEvent) {
    editing.value = event
    draft.value = null
    dialogOpen.value = true
  }

  function show(event: CalendarEvent | null) {
    selected.value = event
  }

  return { dialogOpen, editing, draft, selected, create, edit, show }
})
