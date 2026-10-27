import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useCategories, useTags, useViews } from '@/api/queries'
import type { CalendarViewDto, Category, Tag } from '@/api/types'

export interface CalendarOptionGroup {
  label: string
  items: { label: string; value: number; color: string }[]
}

/** Shared lookups over tags, categories and views (cached by vue-query). */
/** Name of the system view as seeded by the database migration; shown translated unless renamed. */
const SYSTEM_VIEW_NAME = 'All calendars'

export function useCatalog() {
  const { t } = useI18n()
  const tagsQuery = useTags()
  const categoriesQuery = useCategories()
  const viewsQuery = useViews()

  const tags = computed<Tag[]>(() => tagsQuery.data.value ?? [])
  const categories = computed<Category[]>(() => categoriesQuery.data.value ?? [])
  const views = computed(() => viewsQuery.data.value ?? [])

  const tagById = computed(() => new Map(tags.value.map((t) => [t.id, t])))
  const categoryById = computed(() => new Map(categories.value.map((c) => [c.id, c])))
  const calendars = computed(() => categories.value.filter((c) => c.kind === 'CALENDAR'))

  /** Ancestors of a category from root to parent. */
  function ancestors(id: number): Category[] {
    const result: Category[] = []
    const seen = new Set<number>()
    let current = categoryById.value.get(id)
    while (current?.parentId != null && !seen.has(current.id)) {
      seen.add(current.id)
      current = categoryById.value.get(current.parentId)
      if (current) result.unshift(current)
    }
    return result
  }

  function pathLabel(id: number, separator = ' › '): string {
    return ancestors(id)
      .map((a) => a.name)
      .join(separator)
  }

  function fullLabel(id: number, separator = ' › '): string {
    const c = categoryById.value.get(id)
    if (!c) return `#${id}`
    const path = pathLabel(id, separator)
    return path ? `${path}${separator}${c.name}` : c.name
  }

  /** Calendars grouped by their parent path (for Select with option groups). */
  const calendarOptions = computed<CalendarOptionGroup[]>(() => {
    const groups = new Map<string, CalendarOptionGroup>()
    for (const c of calendars.value) {
      const label = pathLabel(c.id) || t('common.topLevel')
      if (!groups.has(label)) groups.set(label, { label, items: [] })
      groups.get(label)!.items.push({ label: c.name, value: c.id, color: c.color })
    }
    return [...groups.values()]
  })

  /** Display name of a view (the built-in "All calendars" view is translated). */
  function viewName(view: CalendarViewDto): string {
    return view.system && view.name === SYSTEM_VIEW_NAME ? t('common.allCalendars') : view.name
  }

  /** Tags of a category including inherited ones. */
  function effectiveTags(category: Category): Tag[] {
    const ids = [...category.tagIds, ...category.inheritedTags.map((t) => t.tagId)]
    return ids.map((id) => tagById.value.get(id)).filter((t): t is Tag => !!t)
  }

  const loading = computed(
    () => tagsQuery.isLoading.value || categoriesQuery.isLoading.value || viewsQuery.isLoading.value,
  )

  return {
    tags,
    categories,
    calendars,
    views,
    tagById,
    categoryById,
    ancestors,
    pathLabel,
    fullLabel,
    calendarOptions,
    effectiveTags,
    viewName,
    loading,
  }
}
