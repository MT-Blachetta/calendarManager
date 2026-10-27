<script setup lang="ts">
import Button from 'primevue/button'
import { useI18n } from 'vue-i18n'
import { useUiStore } from '@/stores/ui'

const ui = useUiStore()
const { t } = useI18n()
</script>

<template>
  <Button
    v-if="ui.isMobile"
    icon="pi pi-bars"
    text
    rounded
    severity="secondary"
    :aria-label="t('nav.openNavigation')"
    @click="ui.mobileNavOpen = true"
  />
</template>
