import { zodResolver } from '@hookform/resolvers/zod'
import { useState, type ReactNode } from 'react'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router'
import { Alert } from '../../../components/ui/Alert'
import { Button } from '../../../components/ui/Button'
import { Modal } from '../../../components/ui/Modal'
import { TextArea } from '../../../components/ui/TextArea'
import { TextField } from '../../../components/ui/TextField'
import { errorMessage } from '../../../lib/errors'
import { emptyToNull } from '../../../lib/forms'
import { useCreateBoard, useUpdateBoard } from '../hooks'
import { boardSchema, type BoardValues } from '../schemas'

type Mode =
  | { kind: 'create'; workspaceId: string }
  | {
      kind: 'edit'
      board: { id: string; workspaceId: string; name: string; description: string | null }
    }

interface Props {
  mode: Mode
  open: boolean
  onClose: () => void
}

/** Creates a board (then opens it) or edits the name and description of an existing one. */
export function BoardFormDialog({ mode, open, onClose }: Props) {
  return (
    <Modal
      open={open}
      onClose={onClose}
      title={mode.kind === 'create' ? 'New board' : 'Edit board'}
    >
      {mode.kind === 'create' ? (
        <CreateBoardForm workspaceId={mode.workspaceId} onDone={onClose} />
      ) : (
        <EditBoardForm board={mode.board} onDone={onClose} />
      )}
    </Modal>
  )
}

function CreateBoardForm({ workspaceId, onDone }: { workspaceId: string; onDone: () => void }) {
  const createBoard = useCreateBoard(workspaceId)
  const navigate = useNavigate()
  const [defaultColumns, setDefaultColumns] = useState(true)

  return (
    <BoardFields
      defaultValues={{ name: '', description: '' }}
      submitLabel="Create board"
      pending={createBoard.isPending}
      error={createBoard.isError ? errorMessage(createBoard.error) : null}
      onCancel={onDone}
      onSubmit={({ name, description }) =>
        createBoard.mutate(
          { name, description: emptyToNull(description), defaultColumns },
          { onSuccess: (board) => navigate(`/boards/${board.id}`) },
        )
      }
    >
      <label className="flex items-center gap-2 text-sm text-slate-700">
        <input
          type="checkbox"
          checked={defaultColumns}
          onChange={(event) => setDefaultColumns(event.target.checked)}
          className="size-4 rounded border-slate-300 text-indigo-600"
        />
        Start with To do, In progress and Done columns
      </label>
    </BoardFields>
  )
}

function EditBoardForm({
  board,
  onDone,
}: {
  board: { id: string; workspaceId: string; name: string; description: string | null }
  onDone: () => void
}) {
  const updateBoard = useUpdateBoard(board)

  return (
    <BoardFields
      defaultValues={{ name: board.name, description: board.description ?? '' }}
      submitLabel="Save"
      pending={updateBoard.isPending}
      error={updateBoard.isError ? errorMessage(updateBoard.error) : null}
      onCancel={onDone}
      onSubmit={({ name, description }) =>
        updateBoard.mutate({ name, description: emptyToNull(description) }, { onSuccess: onDone })
      }
    />
  )
}

interface FieldsProps {
  defaultValues: BoardValues
  submitLabel: string
  pending: boolean
  error: string | null
  onCancel: () => void
  onSubmit: (values: BoardValues) => void
  children?: ReactNode
}

function BoardFields({
  defaultValues,
  submitLabel,
  pending,
  error,
  onCancel,
  onSubmit,
  children,
}: FieldsProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<BoardValues>({ resolver: zodResolver(boardSchema), defaultValues })

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4">
      {error && <Alert>{error}</Alert>}
      <TextField label="Board name" autoFocus error={errors.name?.message} {...register('name')} />
      <TextArea
        label="Description (optional)"
        rows={3}
        error={errors.description?.message}
        {...register('description')}
      />
      {children}
      <div className="flex justify-end gap-2">
        <Button variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" loading={pending}>
          {submitLabel}
        </Button>
      </div>
    </form>
  )
}
