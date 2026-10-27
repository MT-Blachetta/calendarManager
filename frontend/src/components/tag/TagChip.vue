<script setup lang="ts">
withDefaults(defineProps<{ name: string; color: string; inherited?: boolean; title?: string }>(), {
  inherited: false,
  title: undefined,
})
</script>

<template>
  <span class="chip" :class="{ inherited }" :style="{ '--t': color }" :title="title">
    <i v-if="inherited" class="pi pi-arrow-down-right" />{{ name }}
  </span>
</template>

<style scoped>
.chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11.5px;
  line-height: 18px;
  padding: 0 8px;
  border-radius: 999px;
  white-space: nowrap;
  color: color-mix(in srgb, var(--t) 78%, var(--app-text));
  background: color-mix(in srgb, var(--t) 15%, transparent);
  border: 1px solid color-mix(in srgb, var(--t) 32%, transparent);
}

.chip.inherited {
  background: transparent;
  border-style: dashed;
}

.chip i {
  font-size: 9px;
  opacity: 0.7;
}
</style>
