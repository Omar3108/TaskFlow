import { Link, useParams, useSearchParams } from 'react-router'
import { Alert } from '../components/ui/Alert'
import { BoardHeader } from '../features/boards/components/BoardHeader'
import { FilterBar } from '../features/boards/components/FilterBar'
import { KanbanBoard } from '../features/boards/components/KanbanBoard'
import { hasFilters, readFilters, writeFilters } from '../features/boards/filters'
import { useBoard } from '../features/boards/hooks'
import { ApiError } from '../lib/apiClient'
import { errorMessage } from '../lib/errors'

export function BoardPage() {
  const { boardId = '' } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()
  const filters = readFilters(searchParams)
  const board = useBoard(boardId, filters)

  if (board.isPending) {
    return (
      <p role="status" className="text-sm text-slate-500">
        Loading board…
      </p>
    )
  }

  if (board.isError) {
    // The API answers 404 both for unknown boards and for boards of other workspaces.
    const notFound = board.error instanceof ApiError && board.error.status === 404
    return (
      <div className="flex flex-col items-start gap-3">
        <Alert>
          {notFound
            ? 'This board does not exist or you do not have access to it.'
            : errorMessage(board.error)}
        </Alert>
        <Link to="/" className="text-sm font-medium text-indigo-600 hover:underline">
          Back to your workspaces
        </Link>
      </div>
    )
  }

  const filtered = hasFilters(filters)
  const noMatches =
    filtered &&
    board.data.columns.length > 0 &&
    board.data.columns.every((column) => column.cards.length === 0)

  return (
    <>
      <BoardHeader board={board.data} />
      <FilterBar
        boardId={board.data.id}
        workspaceId={board.data.workspaceId}
        filters={filters}
        // replace: typing in the search box must not add one history entry per keystroke.
        onChange={(next) => setSearchParams(writeFilters(next), { replace: true })}
      />
      {noMatches && (
        <p role="status" className="mb-3 text-sm text-slate-600">
          No cards match these filters.
        </p>
      )}
      {/* Dimmed while the results of a new filter load (the previous ones stay visible). */}
      <div className={board.isPlaceholderData ? 'opacity-60 transition-opacity' : undefined}>
        <KanbanBoard board={board.data} />
      </div>
    </>
  )
}
