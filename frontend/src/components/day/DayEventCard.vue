<script setup lang="ts">
import { computed } from 'vue'
import type { CalendarEvent } from '@/api/types'
import { durationMinutes, formatTimeRange } from '@/lib/time'

const props = defineProps<{
  event: CalendarEvent
  start: Date
  end: Date
  color: string
  clippedTop: boolean
  clippedBottom: boolean
  relation: 'hovered' | 'concurrent' | 'dimmed' | null
}>()

const minutes = computed(() => durationMinutes(props.start, props.end))
const timeLabel = computed(() => formatTimeRange(props.start, props.end))
const size = computed(() => (minutes.value >= 55 ? 'large' : minutes.value >= 25 ? 'medium' : 'small'))
const ariaLabel = computed(
  () => `${props.event.title}, ${timeLabel.value}${props.event.place ? `, ${props.event.place}` : ''}`,
)
</script>

<template>
  <button
    type="button"
    class="event"
    :class="[
      `size-${size}`,
      relation ? `is-${relation}` : '',
      { 'clipped-top': clippedTop, 'clipped-bottom': clippedBottom },
    ]"
    :style="{ '--c': color }"
    :aria-label="ariaLabel"
  >
    <span class="title">{{ event.title }}</span>
    <span v-if="size !== 'small'" class="time tabular">{{ timeLabel }}</span>
    <span v-if="size === 'large' && event.place" class="place">
      <i class="pi pi-map-marker" />{{ event.place }}
    </span>
  </button>
</template>

<style scoped>
.event {
  position: absolute;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 1px;
  overflow: hidden;
  padding: 3px 7px 3px 8px;
  border: 0;
  border-left: 3px solid var(--c);
  border-radius: 6px;
  background: color-mix(in srgb, var(--c) var(--event-tint), var(--app-panel));
  color: var(--event-text);
  font: inherit;
  text-align: left;
  cursor: pointer;
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--app-panel) 80%, transparent);
  transition:
    opacity 120ms ease,
    box-shadow 120ms ease,
    transform 120ms ease;
}

.event:hover,
.event:focus-visible {
  outline: none;
  box-shadow:
    0 0 0 1px var(--c),
    var(--app-shadow-lg);
  z-index: 3;
}

.title {
  font-size: 12.5px;
  font-weight: 650;
  line-height: 1.25;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.size-large .title {
  white-space: normal;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.size-small {
  padding-top: 1px;
  padding-bottom: 1px;
  justify-content: center;
}

.size-small .title {
  font-size: 11.5px;
}

.time,
.place {
  font-size: 11px;
  line-height: 1.3;
  opacity: 0.8;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.place i {
  font-size: 10px;
  margin-right: 3px;
}

.clipped-top {
  border-top-left-radius: 0;
  border-top-right-radius: 0;
  border-top: 2px dashed var(--c);
}

.clipped-bottom {
  border-bottom-left-radius: 0;
  border-bottom-right-radius: 0;
  border-bottom: 2px dashed var(--c);
}

/* comparison states while another event is hovered */
.is-dimmed {
  opacity: 0.38;
}

.is-concurrent {
  box-shadow:
    0 0 0 2px var(--c),
    var(--app-shadow);
}

.is-hovered {
  box-shadow:
    0 0 0 2px var(--c),
    var(--app-shadow-lg);
}
</style>
