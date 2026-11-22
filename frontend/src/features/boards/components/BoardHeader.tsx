import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { Button } from '../../../components/ui/Button'
import { ConfirmDialog } from '../../../components/ui/ConfirmDialog'
import { errorMessage } from '../../../lib/errors'
import type { BoardDetail } from '../../../types/api'
import { useDeleteBoard } from '../hooks'
import { canManageBoards } from '../permissions'
import { LabelManagerDialog } from '../../labels/components/LabelManagerDialog'
import { BoardFormDialog } from './BoardFormDialog'
import { toast } from 'sonner'

export function BoardHeader({ board }: { board: BoardDetail }) {
  const navigate = useNavigate()
  const deleteBoard = useDeleteBoard(board)
  const [editing, setEditing] = useState(false)
  const [confirmingDelete, setConfirmingDelete] = useState(false)
  const [managingLabels, setManagingLabels] = useState(false)
  const canManage = canManageBoards(board.role)

  return (
    <div className="mb-6">
      <Link
        to={`/workspaces/${board.workspaceId}`}
        className="text-sm font-medium text-indigo-600 hover:underline"
      >
        ← Back to workspace
      </Link>
      <div className="mt-2 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">{board.name}</h1>
          {board.description && (
            <p className="mt-1 max-w-2xl text-sm text-slate-600">{board.description}</p>
          )}
        </div>
        {canManage && (
          <div className="flex gap-2">
            <Button variant="secondary" onClick={() => setManagingLabels(true)}>
              Labels
            </Button>
            <Button variant="secondary" onClick={() => setEditing(true)}>
              Edit board
            </Button>
            <Button variant="secondary" onClick={() => setConfirmingDelete(true)}>
              Delete board
            </Button>
          </div>
        )}
      </div>

      <LabelManagerDialog
        boardId={board.id}
        open={managingLabels}
        onClose={() => setManagingLabels(false)}
      />
      <BoardFormDialog
        mode={{ kind: 'edit', board }}
        open={editing}
        onClose={() => setEditing(false)}
      />
      <ConfirmDialog
        open={confirmingDelete}
        title="Delete board"
        confirmLabel="Delete board"
        loading={deleteBoard.isPending}
        error={deleteBoard.isError ? errorMessage(deleteBoard.error) : null}
        onCancel={() => {
          setConfirmingDelete(false)
          deleteBoard.reset()
        }}
        onConfirm={() =>
          deleteBoard.mutate(undefined, {
            onSuccess: () => {
              toast.success(`Board ${board.name} deleted`)
              navigate(`/workspaces/${board.workspaceId}`, { replace: true })
            },
          })
        }
      >
        Delete <strong>{board.name}</strong> with all its columns and cards? This cannot be undone.
      </ConfirmDialog>
    </div>
  )
}
