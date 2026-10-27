<script setup lang="ts">
defineProps<{ icon: string; title: string; text?: string }>()
</script>

<template>
  <div class="empty">
    <div class="icon"><i :class="icon" /></div>
    <h2>{{ title }}</h2>
    <p v-if="text">{{ text }}</p>
    <div class="actions"><slot /></div>
  </div>
</template>

<style scoped>
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 100%;
  min-height: 260px;
  padding: 32px;
  text-align: center;
}

.icon {
  display: grid;
  place-items: center;
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: var(--app-accent-soft);
  color: var(--app-accent);
  margin-bottom: 6px;
}

.icon i {
  font-size: 24px;
}

h2 {
  font-size: 16px;
}

p {
  margin: 0;
  max-width: 420px;
  color: var(--app-text-muted);
}

.actions {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}
</style>
