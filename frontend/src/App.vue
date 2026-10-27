<script setup lang="ts">
import { watch } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import Drawer from 'primevue/drawer'
import Toast from 'primevue/toast'
import ConfirmDialog from 'primevue/confirmdialog'
import { usePrimeVue, type PrimeVueLocaleOptions } from 'primevue/config'
import { de as primeDe } from 'primelocale/js/de.js'
import { en as primeEn } from 'primelocale/js/en.js'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import EventFormDialog from '@/components/event/EventFormDialog.vue'
import EventDetailsDrawer from '@/components/event/EventDetailsDrawer.vue'
import { useUiStore } from '@/stores/ui'
import { i18n, type Locale } from '@/i18n'

const PRIME_LOCALES = { en: primeEn, de: primeDe } as Record<Locale, PrimeVueLocaleOptions>

const ui = useUiStore()
const { t } = useI18n()
const primevue = usePrimeVue()
const route = useRoute()

// Apply the language setting to vue-i18n, PrimeVue components (date pickers, tables, …) and <html lang>.
// Weeks start on Monday (ISO 8601) in all date pickers, also in English.
watch(
  () => ui.locale,
  (locale) => {
    i18n.global.locale.value = locale
    document.documentElement.lang = locale
    primevue.config.locale = { ...primevue.config.locale, ...PRIME_LOCALES[locale], firstDayOfWeek: 1 }
  },
  { immediate: true },
)

watch(
  [() => route.meta.titleKey, () => ui.locale],
  ([key]) => {
    document.title = typeof key === 'string' ? `${t(key)} · calendarManager` : 'calendarManager'
  },
  { immediate: true },
)

watch(
  () => route.fullPath,
  () => (ui.mobileNavOpen = false),
)
</script>

<template>
  <div class="shell">
    <AppSidebar v-if="!ui.isMobile" />
    <Drawer
      v-else
      v-model:visible="ui.mobileNavOpen"
      position="left"
      :show-close-icon="false"
      class="mobile-nav"
    >
      <AppSidebar @navigate="ui.mobileNavOpen = false" />
    </Drawer>
    <main class="main">
      <RouterView />
    </main>
  </div>
  <EventFormDialog />
  <EventDetailsDrawer />
  <Toast position="bottom-right" />
  <ConfirmDialog />
</template>

<style scoped>
.shell {
  display: flex;
  height: 100%;
  min-height: 0;
}

.main {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
</style>

<style>
.mobile-nav.p-drawer {
  width: var(--sidebar-width) !important;
}

.mobile-nav .p-drawer-header {
  display: none;
}

.mobile-nav .p-drawer-content {
  padding: 0;
}

.mobile-nav .sidebar {
  border-right: 0;
}
</style>
