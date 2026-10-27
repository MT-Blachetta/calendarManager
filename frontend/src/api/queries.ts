import { computed, type MaybeRefOrGetter, toValue } from 'vue'
import { keepPreviousData, useMutation, useQuery } from '@tanstack/vue-query'
import { api, unwrap } from './client'
import type {
  CategoryRequest,
  EventRequest,
  TagRequest,
  ViewFilterRequest,
  ViewRequest,
} from './types'

// ---------------------------------------------------------------- queries

export function useTags() {
  return useQuery({
    queryKey: ['tags'],
    queryFn: () => unwrap(api.GET('/api/tags')),
  })
}

export function useCategories() {
  return useQuery({
    queryKey: ['categories'],
    queryFn: () => unwrap(api.GET('/api/categories')),
  })
}

export function useViews() {
  return useQuery({
    queryKey: ['views'],
    queryFn: () => unwrap(api.GET('/api/views')),
  })
}

/** Visible calendars and events of a view in [from, to). */
export function useSchedule(
  viewId: MaybeRefOrGetter<number | undefined>,
  range: MaybeRefOrGetter<{ from: Date; to: Date }>,
) {
  return useQuery({
    queryKey: computed(() => [
      'schedule',
      toValue(viewId),
      toValue(range).from.toISOString(),
      toValue(range).to.toISOString(),
    ]),
    enabled: computed(() => toValue(viewId) !== undefined),
    placeholderData: keepPreviousData,
    queryFn: () =>
      unwrap(
        api.GET('/api/views/{id}/schedule', {
          params: {
            path: { id: toValue(viewId)! },
            query: {
              from: toValue(range).from.toISOString(),
              to: toValue(range).to.toISOString(),
            },
          },
        }),
      ),
  })
}

export interface EventSearchParams {
  from?: Date
  to?: Date
  categoryIds?: number[]
  q?: string
  page: number
  size: number
  sort: 'start' | 'end' | 'title' | 'place'
  direction: 'asc' | 'desc'
}

export function useEventSearch(
  params: MaybeRefOrGetter<EventSearchParams>,
  enabled: MaybeRefOrGetter<boolean> = true,
) {
  return useQuery({
    queryKey: computed(() => ['events', toValue(params)]),
    enabled: computed(() => toValue(enabled)),
    placeholderData: keepPreviousData,
    queryFn: () => {
      const p = toValue(params)
      return unwrap(
        api.GET('/api/events', {
          params: {
            query: {
              from: p.from?.toISOString(),
              to: p.to?.toISOString(),
              categoryId: p.categoryIds?.length ? p.categoryIds : undefined,
              q: p.q || undefined,
              page: p.page,
              size: p.size,
              sort: p.sort,
              direction: p.direction,
            },
          },
        }),
      )
    },
  })
}

/**
 * Days with events in the calendars of a calendar or group (all calendars if categoryId is null)
 * within [from, to] (local dates "yyyy-MM-dd" in the given IANA time zone).
 */
export function useDaySummary(
  categoryId: MaybeRefOrGetter<number | null>,
  range: MaybeRefOrGetter<{ from: string; to: string }>,
  zone: string,
) {
  return useQuery({
    queryKey: computed(() => ['day-summary', toValue(categoryId), toValue(range), zone]),
    placeholderData: keepPreviousData,
    queryFn: () =>
      unwrap(
        api.GET('/api/events/day-summary', {
          params: {
            query: {
              categoryId: toValue(categoryId) ?? undefined,
              from: toValue(range).from,
              to: toValue(range).to,
              zone,
            },
          },
        }),
      ),
  })
}

/** Live preview of an unsaved view filter. */
export function useResolvedFilter(filter: MaybeRefOrGetter<ViewFilterRequest | undefined>) {
  return useQuery({
    queryKey: computed(() => ['resolve', toValue(filter)]),
    enabled: computed(() => toValue(filter) !== undefined),
    placeholderData: keepPreviousData,
    queryFn: () => unwrap(api.POST('/api/views/resolve', { body: toValue(filter)! })),
  })
}

export function fetchCategoryUsage(id: number) {
  return unwrap(api.GET('/api/categories/{id}/usage', { params: { path: { id } } }))
}

// ---------------------------------------------------------------- mutations
// (all mutations invalidate every query on success, see main.ts)

export function useSaveTag() {
  return useMutation({
    mutationFn: ({ id, body }: { id?: number; body: TagRequest }) =>
      id === undefined
        ? unwrap(api.POST('/api/tags', { body }))
        : unwrap(api.PUT('/api/tags/{id}', { params: { path: { id } }, body })),
  })
}

export function useDeleteTag() {
  return useMutation({
    mutationFn: (id: number) => unwrap(api.DELETE('/api/tags/{id}', { params: { path: { id } } })),
  })
}

export function useSaveCategory() {
  return useMutation({
    mutationFn: ({ id, body }: { id?: number; body: CategoryRequest }) =>
      id === undefined
        ? unwrap(api.POST('/api/categories', { body }))
        : unwrap(api.PUT('/api/categories/{id}', { params: { path: { id } }, body })),
  })
}

export function useDeleteCategory() {
  return useMutation({
    mutationFn: ({ id, cascade }: { id: number; cascade: boolean }) =>
      unwrap(
        api.DELETE('/api/categories/{id}', { params: { path: { id }, query: { cascade } } }),
      ),
  })
}

export function useSaveEvent() {
  return useMutation({
    mutationFn: ({ id, body }: { id?: number; body: EventRequest }) =>
      id === undefined
        ? unwrap(api.POST('/api/events', { body }))
        : unwrap(api.PUT('/api/events/{id}', { params: { path: { id } }, body })),
  })
}

export function useDeleteEvent() {
  return useMutation({
    mutationFn: (id: number) =>
      unwrap(api.DELETE('/api/events/{id}', { params: { path: { id } } })),
  })
}

export function useSaveView() {
  return useMutation({
    mutationFn: ({ id, body }: { id?: number; body: ViewRequest }) =>
      id === undefined
        ? unwrap(api.POST('/api/views', { body }))
        : unwrap(api.PUT('/api/views/{id}', { params: { path: { id } }, body })),
  })
}

export function useDeleteView() {
  return useMutation({
    mutationFn: (id: number) => unwrap(api.DELETE('/api/views/{id}', { params: { path: { id } } })),
  })
}
