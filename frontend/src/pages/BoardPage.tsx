import { Link, useParams } from 'react-router'
import { Alert } from '../components/ui/Alert'
import { BoardHeader } from '../features/boards/components/BoardHeader'
import { KanbanBoard } from '../features/boards/components/KanbanBoard'
import { useBoard } from '../features/boards/hooks'
import { ApiError } from '../lib/apiClient'
import { errorMessage } from '../lib/errors'

export function BoardPage() {
  const { boardId = '' } = useParams()
  const board = useBoard(boardId)

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

  return (
    <>
      <BoardHeader board={board.data} />
      <KanbanBoard board={board.data} />
    </>
  )
}
