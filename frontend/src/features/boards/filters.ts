/**
 * Board filters live in the URL (?q=login&label=...&assignee=none&due=overdue), so they survive
 * a reload, work with the back button and can be shared as a link. These functions translate
 * between the URL, the form and the API, which uses slightly different names.
 */

export type DueFilter = 'overdue' | 'week' | 'none'

export interface BoardFilters {
  q?: string
  /** A member id, or 'none' for unassigned cards. */
  assignee?: string
  label?: string
  due?: DueFilter
}

const DUE_VALUES: DueFilter[] = ['overdue', 'week', 'none']

/** Reads the filters from the URL, ignoring empty or unknown values. */
export function readFilters(params: URLSearchParams): BoardFilters {
  const filters: BoardFilters = {}
  const q = params.get('q')?.trim()
  if (q) filters.q = q
  const assignee = params.get('assignee')
  if (assignee) filters.assignee = assignee
  const label = params.get('label')
  if (label) filters.label = label
  const due = params.get('due')
  if (due && (DUE_VALUES as string[]).includes(due)) filters.due = due as DueFilter
  return filters
}

/** Writes the filters to URL parameters, leaving out the empty ones. */
export function writeFilters(filters: BoardFilters): URLSearchParams {
  const params = new URLSearchParams()
  if (filters.q?.trim()) params.set('q', filters.q.trim())
  if (filters.assignee) params.set('assignee', filters.assignee)
  if (filters.label) params.set('label', filters.label)
  if (filters.due) params.set('due', filters.due)
  return params
}

export function hasFilters(filters: BoardFilters) {
  return Object.keys(readFilters(writeFilters(filters))).length > 0
}

/** The query string of GET /boards/{id}, in the API's names ("" when there are no filters). */
export function toApiQuery(filters: BoardFilters): string {
  const params = new URLSearchParams()
  if (filters.q) params.set('q', filters.q)
  if (filters.assignee === 'none') params.set('unassigned', 'true')
  else if (filters.assignee) params.set('assigneeId', filters.assignee)
  if (filters.label) params.set('labelId', filters.label)
  if (filters.due) params.set('due', filters.due.toUpperCase())
  const query = params.toString()
  return query ? `?${query}` : ''
}
