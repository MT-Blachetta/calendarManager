<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { addMonths, startOfDay } from 'date-fns'
import Button from 'primevue/button'
import Select from 'primevue/select'
import SelectButton from 'primevue/selectbutton'
import NavToggle from '@/components/layout/NavToggle.vue'
import { useUiStore, type Density, type Theme } from '@/stores/ui'
import { useHolidays } from '@/composables/useHolidays'
import { GERMAN_STATES, type HolidayRegion } from '@/lib/holidays'
import { LOCALES, LOCALE_NAMES } from '@/i18n'
import { formatDate } from '@/i18n/dates'
import { parseDayKey } from '@/lib/time'
import { notify } from '@/lib/notify'

const ui = useUiStore()
const { t } = useI18n()
const { between, holidayName } = useHolidays()

const UPCOMING_COUNT = 6

const languageOptions = LOCALES.map((value) => ({ value, label: LOCALE_NAMES[value] }))

const themeOptions = computed<{ value: Theme; label: string; icon: string }[]>(() => [
  { value: 'light', label: t('settings.themeLight'), icon: 'pi pi-sun' },
  { value: 'dark', label: t('settings.themeDark'), icon: 'pi pi-moon' },
  { value: 'system', label: t('settings.themeSystem'), icon: 'pi pi-desktop' },
])

const densityOptions = computed<{ value: Density; label: string; icon: string }[]>(() => [
  { value: 'compact', label: t('density.compact'), icon: 'pi pi-minus' },
  { value: 'comfortable', label: t('density.comfortable'), icon: 'pi pi-equals' },
  { value: 'spacious', label: t('density.spacious'), icon: 'pi pi-bars' },
])

/** Off, nationwide, then the federal states sorted by their name in the UI language. */
const regionOptions = computed<{ value: HolidayRegion; label: string }[]>(() => {
  const states = GERMAN_STATES.map((s) => ({
    value: `DE-${s}` as HolidayRegion,
    label: t('settings.regionState', { state: t(`settings.states.${s}`) }),
  })).sort((a, b) => a.label.localeCompare(b.label, ui.locale))
  return [
    { value: 'off', label: t('settings.regionOff') },
    { value: 'DE', label: t('settings.regionDE') },
    ...states,
  ]
})

const upcoming = computed(() => {
  const today = startOfDay(new Date())
  return [...between(today, addMonths(today, 12)).values()].slice(0, UPCOMING_COUNT).map((h) => ({
    holiday: h,
    date: parseDayKey(h.date),
    name: holidayName(h),
  }))
})

function reset() {
  ui.resetSettings()
  notify('success', t('settings.resetDone'))
}
</script>

<template>
  <div class="page">
    <header class="page-header">
      <NavToggle />
      <h1>{{ t('settings.title') }}</h1>
      <span class="muted">{{ t('settings.subtitle') }}</span>
    </header>
    <div class="page-body">
      <div class="settings">
        <section class="card section" aria-labelledby="set-language">
          <div class="section-text">
            <h2 id="set-language"><i class="pi pi-language" />{{ t('settings.language') }}</h2>
            <p class="muted">{{ t('settings.languageHint') }}</p>
          </div>
          <div class="section-controls">
            <SelectButton
              v-model="ui.locale"
              :options="languageOptions"
              option-label="label"
              option-value="value"
              :allow-empty="false"
              :aria-label="t('settings.language')"
            >
              <template #option="{ option }">
                <span :lang="option.value">{{ option.label }}</span>
              </template>
            </SelectButton>
          </div>
        </section>

        <section class="card section" aria-labelledby="set-appearance">
          <div class="section-text">
            <h2 id="set-appearance"><i class="pi pi-palette" />{{ t('settings.appearance') }}</h2>
            <p class="muted">{{ t('settings.themeHint') }}</p>
          </div>
          <div class="section-controls">
            <div class="field">
              <label>{{ t('settings.theme') }}</label>
              <SelectButton
                v-model="ui.theme"
                :options="themeOptions"
                option-label="label"
                option-value="value"
                :allow-empty="false"
                :aria-label="t('settings.theme')"
              >
                <template #option="{ option }"><i :class="option.icon" /><span>{{ option.label }}</span></template>
              </SelectButton>
            </div>
            <div class="field">
              <label>{{ t('settings.density') }}</label>
              <SelectButton
                v-model="ui.density"
                :options="densityOptions"
                option-label="label"
                option-value="value"
                :allow-empty="false"
                :aria-label="t('settings.density')"
              >
                <template #option="{ option }"><i :class="option.icon" /><span>{{ option.label }}</span></template>
              </SelectButton>
            </div>
          </div>
        </section>

        <section class="card section" aria-labelledby="set-holidays">
          <div class="section-text">
            <h2 id="set-holidays"><i class="pi pi-star" />{{ t('settings.holidays') }}</h2>
            <p class="muted">{{ t('settings.holidaysHint') }}</p>
          </div>
          <div class="section-controls">
            <div class="field">
              <label for="set-region">{{ t('settings.region') }}</label>
              <Select
                v-model="ui.holidayRegion"
                input-id="set-region"
                :options="regionOptions"
                option-label="label"
                option-value="value"
                filter
                class="region"
              />
            </div>
            <div v-if="ui.holidayRegion !== 'off'" class="field">
              <label>{{ t('settings.upcoming') }}</label>
              <ul v-if="upcoming.length" class="upcoming">
                <li v-for="item in upcoming" :key="item.holiday.date">
                  <span class="date tabular">{{ formatDate(item.date, 'dateShort') }}</span>
                  <span>{{ item.name }}</span>
                </li>
              </ul>
              <span v-else class="muted">{{ t('settings.noUpcoming') }}</span>
            </div>
          </div>
        </section>

        <footer class="settings-footer">
          <p class="muted"><i class="pi pi-info-circle" /> {{ t('settings.storageNote') }}</p>
          <Button :label="t('settings.reset')" icon="pi pi-refresh" severity="secondary" outlined size="small" @click="reset" />
        </footer>
      </div>
    </div>
  </div>
</template>

<style scoped>
.settings {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 880px;
}

.section {
  display: grid;
  grid-template-columns: minmax(200px, 280px) 1fr;
  gap: 16px 32px;
  padding: 20px 24px;
}

.section-text h2 {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
}

.section-text h2 i {
  font-size: 15px;
  color: var(--app-accent);
}

.section-text p {
  margin: 6px 0 0;
  font-size: 13px;
}

.section-controls {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-width: 0;
}

.section-controls :deep(.p-togglebutton-content) {
  gap: 6px;
}

.region {
  width: min(100%, 380px);
}

.upcoming {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
}

.upcoming li {
  display: flex;
  gap: 12px;
}

.upcoming .date {
  flex: none;
  min-width: 132px;
  color: var(--app-holiday);
  font-weight: 600;
}

.settings-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 0 4px;
}

.settings-footer p {
  margin: 0;
  font-size: 12.5px;
  max-width: 560px;
}

@media (max-width: 720px) {
  .section {
    grid-template-columns: 1fr;
    padding: 16px;
  }
}
</style>
