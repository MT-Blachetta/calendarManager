/** Route to the month view of "yyyy-MM", optionally filtered to a calendar or group and with a selected day. */
export function monthRoute(month: string, categoryId: number | null | undefined, day?: string | null) {
  const query: Record<string, string> = {}
  if (categoryId != null) query.category = String(categoryId)
  if (day) query.day = day
  return { name: 'month' as const, params: { month }, query }
}
