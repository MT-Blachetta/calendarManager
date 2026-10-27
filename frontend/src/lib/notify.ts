import ToastEventBus from 'primevue/toasteventbus'
import { ApiError } from '@/api/client'
import { t } from '@/i18n'

/** Shows a toast from anywhere (no component context needed). */
export function notify(
  severity: 'success' | 'info' | 'warn' | 'error',
  summary: string,
  detail?: string,
  life = 3500,
) {
  ToastEventBus.emit('add', { severity, summary, detail, life })
}

export function notifyError(error: unknown, summary = t('errors.actionFailed')) {
  const detail =
    error instanceof ApiError
      ? error.detail
      : error instanceof Error
        ? error.message
        : t('errors.unexpected')
  notify('error', summary, detail, 6000)
}
