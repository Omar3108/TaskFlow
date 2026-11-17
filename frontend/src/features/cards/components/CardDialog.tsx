import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Alert } from '../../../components/ui/Alert'
import { Button } from '../../../components/ui/Button'
import { Modal } from '../../../components/ui/Modal'
import { Select } from '../../../components/ui/Select'
import { TextArea } from '../../../components/ui/TextArea'
import { TextField } from '../../../components/ui/TextField'
import { ApiError } from '../../../lib/apiClient'
import { errorMessage } from '../../../lib/errors'
import { emptyToNull } from '../../../lib/forms'
import type { Card, Member } from '../../../types/api'
import { useMembers } from '../../workspaces/hooks'
import { useCard, useDeleteCard, useUpdateCard } from '../hooks'
import { cardSchema, type CardValues } from '../schemas'

interface Props {
  cardId: string
  boardId: string
  workspaceId: string
  onClose: () => void
}

/** Shows and edits one card. The board only has summaries, so the full card is loaded here. */
export function CardDialog({ cardId, boardId, workspaceId, onClose }: Props) {
  return (
    <Modal open onClose={onClose} title="Card details">
      <CardDialogBody
        cardId={cardId}
        boardId={boardId}
        workspaceId={workspaceId}
        onClose={onClose}
      />
    </Modal>
  )
}

function CardDialogBody({ cardId, boardId, workspaceId, onClose }: Props) {
  const card = useCard(cardId)
  const members = useMembers(workspaceId)

  if (card.isPending) {
    return (
      <p role="status" className="text-sm text-slate-500">
        Loading card…
      </p>
    )
  }
  if (card.isError) {
    const gone = card.error instanceof ApiError && card.error.status === 404
    return (
      <div className="flex flex-col gap-4">
        <Alert>{gone ? 'This card no longer exists.' : errorMessage(card.error)}</Alert>
        <div className="flex justify-end">
          <Button variant="secondary" onClick={onClose}>
            Close
          </Button>
        </div>
      </div>
    )
  }

  return (
    // key: a new version (after a save or "load latest") remounts the form with fresh values.
    <CardForm
      key={card.data.version}
      card={card.data}
      members={members.data ?? []}
      membersLoading={members.isPending}
      boardId={boardId}
      onClose={onClose}
      onReload={() => void card.refetch()}
      reloading={card.isFetching}
    />
  )
}

interface FormProps {
  card: Card
  members: Member[]
  membersLoading: boolean
  boardId: string
  onClose: () => void
  onReload: () => void
  reloading: boolean
}

function CardForm({
  card,
  members,
  membersLoading,
  boardId,
  onClose,
  onReload,
  reloading,
}: FormProps) {
  const updateCard = useUpdateCard(boardId, card.id)
  const deleteCard = useDeleteCard(boardId)
  const [confirmingDelete, setConfirmingDelete] = useState(false)
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<CardValues>({
    resolver: zodResolver(cardSchema),
    defaultValues: {
      title: card.title,
      description: card.description ?? '',
      assigneeId: card.assignee?.id ?? '',
      dueDate: card.dueDate ?? '',
    },
  })

  const onSubmit = handleSubmit((values) =>
    updateCard.mutate(
      {
        title: values.title,
        description: emptyToNull(values.description),
        assigneeId: values.assigneeId || null,
        dueDate: values.dueDate || null,
        // The version this form was filled from: if somebody saved since, the server says 409.
        version: card.version,
      },
      { onSuccess: onClose },
    ),
  )

  const conflict = updateCard.error instanceof ApiError && updateCard.error.status === 409

  return (
    <form onSubmit={onSubmit} noValidate className="flex flex-col gap-4">
      {conflict ? (
        <div
          role="alert"
          className="flex flex-col items-start gap-2 rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800"
        >
          <p>
            Someone else changed this card while you were editing it. Load the latest version to see
            their changes; your unsaved edits will be discarded.
          </p>
          <Button size="sm" variant="secondary" onClick={onReload} loading={reloading}>
            Load latest version
          </Button>
        </div>
      ) : (
        updateCard.isError && <Alert>{errorMessage(updateCard.error)}</Alert>
      )}

      <TextField label="Title" error={errors.title?.message} {...register('title')} />
      <TextArea
        label="Description"
        rows={5}
        error={errors.description?.message}
        {...register('description')}
      />
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <Select label="Assignee" disabled={membersLoading} {...register('assigneeId')}>
          <option value="">Unassigned</option>
          {members.map((member) => (
            <option key={member.userId} value={member.userId}>
              {member.displayName}
            </option>
          ))}
          {/* Keep the current assignee selectable while the member list is loading. */}
          {card.assignee && !members.some((m) => m.userId === card.assignee!.id) && (
            <option value={card.assignee.id}>{card.assignee.displayName}</option>
          )}
        </Select>
        <TextField label="Due date" type="date" {...register('dueDate')} />
      </div>

      {confirmingDelete ? (
        <div className="flex flex-col gap-2 rounded-md border border-red-200 bg-red-50 p-3">
          <p className="text-sm text-red-800">Delete this card? This cannot be undone.</p>
          {deleteCard.isError && <Alert>{errorMessage(deleteCard.error)}</Alert>}
          <div className="flex gap-2">
            <Button
              variant="danger"
              size="sm"
              loading={deleteCard.isPending}
              onClick={() => deleteCard.mutate(card.id, { onSuccess: onClose })}
            >
              Delete card
            </Button>
            <Button variant="secondary" size="sm" onClick={() => setConfirmingDelete(false)}>
              Keep it
            </Button>
          </div>
        </div>
      ) : null}

      <div className="flex items-center justify-between gap-2">
        <Button
          variant="secondary"
          onClick={() => setConfirmingDelete(true)}
          disabled={confirmingDelete}
        >
          Delete
        </Button>
        <div className="flex gap-2">
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" loading={updateCard.isPending} disabled={conflict}>
            Save
          </Button>
        </div>
      </div>
    </form>
  )
}
