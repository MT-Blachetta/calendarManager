<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useUiStore } from '@/stores/ui'
import { monthRoute } from '@/lib/routes'

const route = useRoute()
const router = useRouter()
const ui = useUiStore()
const { t } = useI18n()

function goDay() {
  if (route.name !== 'day') router.push({ name: 'day', params: { date: ui.lastDay } })
}

function goMonth() {
  if (route.name !== 'month') router.push(monthRoute(ui.lastDay.slice(0, 7), ui.monthCategoryId))
}
</script>

<template>
  <div class="mode-switch" role="tablist" :aria-label="t('nav.layout')">
    <button type="button" role="tab" :aria-selected="route.name === 'day'" :class="{ active: route.name === 'day' }" @click="goDay">
      {{ t('nav.day') }}
    </button>
    <button type="button" role="tab" :aria-selected="route.name === 'month'" :class="{ active: route.name === 'month' }" @click="goMonth">
      {{ t('nav.month') }}
    </button>
  </div>
</template>

<style scoped>
.mode-switch {
  display: inline-flex;
  padding: 3px;
  gap: 2px;
  border-radius: 9px;
  background: var(--app-border-subtle);
}

button {
  border: 0;
  border-radius: 7px;
  padding: 5px 12px;
  background: transparent;
  color: var(--app-text-muted);
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

button:hover {
  color: var(--app-text);
}

button.active {
  background: var(--app-panel);
  color: var(--app-text);
  box-shadow: var(--app-shadow);
}
</style>
