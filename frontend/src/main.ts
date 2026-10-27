import '@fontsource-variable/inter'
import 'primeicons/primeicons.css'
import './styles/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import PrimeVue from 'primevue/config'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import Tooltip from 'primevue/tooltip'
import { MutationCache, QueryCache, QueryClient, VueQueryPlugin } from '@tanstack/vue-query'

import App from './App.vue'
import router from './router'
import { CalendarPreset, DARK_MODE_CLASS } from './theme'
import { notifyError } from './lib/notify'
import { i18n, t } from './i18n'

const queryClient: QueryClient = new QueryClient({
  defaultOptions: {
    queries: { staleTime: 30_000, refetchOnWindowFocus: true, retry: 1 },
  },
  queryCache: new QueryCache({ onError: (error) => notifyError(error, t('errors.loadingFailed')) }),
  mutationCache: new MutationCache({
    onError: (error) => notifyError(error),
    // Every change can affect inherited tags, view resolution and schedules: refresh everything.
    onSuccess: (): Promise<void> => queryClient.invalidateQueries(),
  }),
})

createApp(App)
  .use(createPinia())
  .use(i18n)
  .use(router)
  .use(PrimeVue, {
    theme: {
      preset: CalendarPreset,
      options: { darkModeSelector: `.${DARK_MODE_CLASS}`, cssLayer: false },
    },
    ripple: false,
  })
  .use(ToastService)
  .use(ConfirmationService)
  .use(VueQueryPlugin, { queryClient })
  .directive('tooltip', Tooltip)
  .mount('#app')
