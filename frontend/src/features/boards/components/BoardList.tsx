import { useState } from 'react'
import { Link } from 'react-router'
import { Alert } from '../../../components/ui/Alert'
import { Button } from '../../../components/ui/Button'
import { errorMessage } from '../../../lib/errors'
import type { WorkspaceRole } from '../../../types/api'
import { useBoards } from '../hooks'
import { canManageBoards } from '../permissions'
import { BoardFormDialog } from './BoardFormDialog'

interface Props {
  workspaceId: string
  role: WorkspaceRole
}

export function BoardList({ workspaceId, role }: Props) {
  const boards = useBoards(workspaceId)
  const [creating, setCreating] = useState(false)
  const canManage = canManageBoards(role)

  let content
  if (boards.isPending) {
    content = (
      <p role="status" className="text-sm text-slate-500">
        Loading boards…
      </p>
    )
  } else if (boards.isError) {
    content = <Alert>{errorMessage(boards.error)}</Alert>
  } else if (boards.data.length === 0) {
    content = (
      <p className="rounded-xl border border-dashed border-slate-300 bg-white p-6 text-sm text-slate-500">
        {canManage
          ? 'No boards yet. Create the first one to start tracking work.'
          : 'No boards yet. An admin of this workspace can create one.'}
      </p>
    )
  } else {
    content = (
      <ul className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {boards.data.map((board) => (
          <li key={board.id}>
            <Link
              to={`/boards/${board.id}`}
              className="flex h-full flex-col gap-1 rounded-xl border border-slate-200 bg-white p-5 shadow-sm transition-shadow hover:shadow-md focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600"
            >
              <span className="font-semibold text-slate-900">{board.name}</span>
              {board.description && (
                <span className="line-clamp-2 text-sm text-slate-600">{board.description}</span>
              )}
            </Link>
          </li>
        ))}
      </ul>
    )
  }

  return (
    <div className="flex flex-col gap-3">
      {canManage && (
        <div>
          <Button variant="secondary" onClick={() => setCreating(true)}>
            New board
          </Button>
        </div>
      )}
      {content}
      <BoardFormDialog
        mode={{ kind: 'create', workspaceId }}
        open={creating}
        onClose={() => setCreating(false)}
      />
    </div>
  )
}
