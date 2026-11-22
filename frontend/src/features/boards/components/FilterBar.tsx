import { useEffect, useRef, useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { Select } from '../../../components/ui/Select'
import { useLabels } from '../../labels/hooks'
import { useMembers } from '../../workspaces/hooks'
import { hasFilters, type BoardFilters, type DueFilter } from '../filters'

interface Props {
  boardId: string
  workspaceId: string
  filters: BoardFilters
  onChange: (filters: BoardFilters) => void
}

/** How long to wait after the last keystroke before searching. */
const SEARCH_DELAY_MS = 300

export function FilterBar({ boardId, workspaceId, filters, onChange }: Props) {
  const members = useMembers(workspaceId)
  const labels = useLabels(boardId)
  // The search box answers every keystroke; the URL (and so the request) follows after a pause.
  const [text, setText] = useState(filters.q ?? '')
  const timer = useRef<number | undefined>(undefined)

  useEffect(() => () => window.clearTimeout(timer.current), [])

  const onSearch = (value: string) => {
    setText(value)
    window.clearTimeout(timer.current)
    timer.current = window.setTimeout(() => onChange({ ...filters, q: value }), SEARCH_DELAY_MS)
  }

  const set = (changes: Partial<BoardFilters>) => onChange({ ...filters, q: text, ...changes })

  return (
    <div
      role="search"
      className="mb-4 flex flex-col gap-3 rounded-xl border border-slate-200 bg-white p-3 sm:flex-row sm:flex-wrap sm:items-end"
    >
      <div className="flex flex-1 flex-col gap-1 sm:min-w-48">
        <label htmlFor="board-search" className="text-sm font-medium text-slate-700">
          Search
        </label>
        <input
          id="board-search"
          type="search"
          value={text}
          maxLength={100}
          placeholder="Title or description"
          onChange={(event) => onSearch(event.target.value)}
          className="rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-900 shadow-xs outline-none focus:ring-2 focus:ring-indigo-500"
        />
      </div>
      <Select
        label="Assignee"
        value={filters.assignee ?? ''}
        onChange={(event) => set({ assignee: event.target.value || undefined })}
      >
        <option value="">Anyone</option>
        <option value="none">Unassigned</option>
        {members.data?.map((member) => (
          <option key={member.userId} value={member.userId}>
            {member.displayName}
          </option>
        ))}
      </Select>
      <Select
        label="Label"
        value={filters.label ?? ''}
        onChange={(event) => set({ label: event.target.value || undefined })}
      >
        <option value="">Any label</option>
        {labels.data?.map((label) => (
          <option key={label.id} value={label.id}>
            {label.name}
          </option>
        ))}
      </Select>
      <Select
        label="Due"
        value={filters.due ?? ''}
        onChange={(event) =>
          set({ due: (event.target.value || undefined) as DueFilter | undefined })
        }
      >
        <option value="">Any date</option>
        <option value="overdue">Overdue</option>
        <option value="week">Due in the next 7 days</option>
        <option value="none">No due date</option>
      </Select>
      {hasFilters({ ...filters, q: text }) && (
        <Button
          variant="secondary"
          onClick={() => {
            window.clearTimeout(timer.current)
            setText('')
            onChange({})
          }}
        >
          Clear filters
        </Button>
      )}
    </div>
  )
}
