import createClient from 'openapi-fetch'
import type { paths } from './schema'
import { currentLocale, t } from '@/i18n'

/** RFC 9457 problem details as returned by the backend. */
export interface Problem {
  title?: string
  detail?: string
  status?: number
  errors?: Record<string, string>
}

export class ApiError extends Error {
  readonly status: number
  readonly detail: string
  readonly fieldErrors: Record<string, string>

  constructor(status: number, problem: Problem | undefined, options?: ErrorOptions) {
    const detail = problem?.detail ?? problem?.title ?? t('errors.requestFailed', { status })
    super(detail, options)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = problem?.errors ?? {}
    const fields = Object.entries(this.fieldErrors)
      .map(([field, message]) => `${field}: ${message}`)
      .join('; ')
    this.detail = fields ? `${detail} ${fields}` : detail
  }
}

/** Typed API client; requests go to /api on the same origin (Vite proxies to the backend in dev). */
export const api = createClient<paths>({ baseUrl: '' })

// The backend localizes error messages (problem details) by Accept-Language.
api.use({
  onRequest({ request }) {
    request.headers.set('Accept-Language', currentLocale())
    return request
  },
})

/** Unwraps an openapi-fetch result, throwing an {@link ApiError} on non-2xx responses. */
export async function unwrap<T>(
  promise: Promise<{ data?: T; error?: unknown; response: Response }>,
): Promise<T> {
  let result: { data?: T; error?: unknown; response: Response }
  try {
    result = await promise
  } catch (cause) {
    throw new ApiError(0, { detail: t('errors.backendUnreachable') }, { cause })
  }
  if (!result.response.ok) {
    throw new ApiError(result.response.status, result.error as Problem | undefined)
  }
  return result.data as T
}
