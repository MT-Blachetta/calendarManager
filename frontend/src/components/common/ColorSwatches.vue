<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { PALETTE } from '@/lib/colors'

const { t } = useI18n()

const model = defineModel<string>({ required: true })
</script>

<template>
  <div class="swatches" role="radiogroup" :aria-label="t('common.color')">
    <button
      v-for="color in PALETTE"
      :key="color"
      type="button"
      role="radio"
      class="swatch"
      :class="{ selected: model.toUpperCase() === color }"
      :style="{ '--dot': color }"
      :aria-checked="model.toUpperCase() === color"
      :aria-label="color"
      @click="model = color"
    >
      <i v-if="model.toUpperCase() === color" class="pi pi-check" />
    </button>
    <label class="custom" :title="t('common.customColor')">
      <input v-model="model" type="color" :aria-label="t('common.customColor')" />
    </label>
  </div>
</template>

<style scoped>
.swatches {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.swatch,
.custom {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  border: 0;
  background: var(--dot);
  display: grid;
  place-items: center;
  cursor: pointer;
  color: #fff;
  padding: 0;
}

.swatch i {
  font-size: 12px;
}

.swatch.selected {
  box-shadow:
    0 0 0 2px var(--app-panel),
    0 0 0 4px var(--dot);
}

.custom {
  background: conic-gradient(#ef4444, #f59e0b, #84cc16, #06b6d4, #4f46e5, #ec4899, #ef4444);
  position: relative;
  overflow: hidden;
}

.custom input {
  opacity: 0;
  position: absolute;
  inset: 0;
  cursor: pointer;
}
</style>
