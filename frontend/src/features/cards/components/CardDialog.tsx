import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { Alert } from '../../../components/ui/Alert'
import { Button } from '../../../components/ui/Button'
import { Modal } from '../../../components/ui/Modal'
import { Select } from '../../../components/ui/Select'
import { TextArea } from '../../../components/ui/TextArea'
import { TextField } from '../../../components/ui/TextField'
import { ApiError } from '../../../lib/apiClient'
import { errorMessage } from '../../../lib/errors'
import { emptyToNull } from '../../../lib/forms'
import type { Card, Label, Member, WorkspaceRole } from '../../../types/api'
import { useAuth } from '../../auth/AuthContext'
import { CommentSection } from '../../comments/components/CommentSection'
import { LabelChip } from '../../labels/components/LabelChip'
import { useLabels } from '../../labels/hooks'
import { useMembers } from '../../workspaces/hooks'
import { useCard, useDeleteCard, useUpdateCard } from '../hooks'
import { cardSchema, type CardValues } from '../schemas'
import { toast } from 'sonner'
import { Skeleton, SkeletonStatus } from '../../../components/ui/Skeleton'

interface Props {
  cardId: string
  boardId: string
  workspaceId: string
  /** Role of the signed-in user: admins may delete other people's comments. */
  role: WorkspaceRole
  onClose: () => void
}

/** Shows and edits one card. The board only has summaries, so the full card is loaded here. */
export function CardDialog(props: Props) {
  return (
    <Modal open onClose={props.onClose} title="Card details" size="lg">
      <CardDialogBody {...props} />
    </Modal>
  )
}

function CardDialogBody({ cardId, boardId, workspaceId, role, onClose }: Props) {
  const card = useCard(cardId)
  const members = useMembers(workspaceId)
  const labels = useLabels(boardId)
  const { user } = useAuth()

  if (card.isPending) {
    return (
      <SkeletonStatus label="Loading card…">
        <div className="flex flex-col gap-4">
          <Skeleton className="h-9 w-full" />
          <Skeleton className="h-28 w-full" />
          <div className="grid grid-cols-2 gap-4">
            <Skeleton className="h-9" />
            <Skeleton className="h-9" />
          </div>
        </div>
      </SkeletonStatus>
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
    <div className="flex flex-col gap-6">
      {/* key: a new version (after a save or "load latest") remounts the form with fresh values. */}
      <CardForm
        key={card.data.version}
        card={card.data}
        members={members.data ?? []}
        membersLoading={members.isPending}
        boardLabels={labels.data ?? []}
        boardId={boardId}
        onClose={onClose}
        onReload={() => void card.refetch()}
        reloading={card.isFetching}
      />
      {/* Outside the card form: comments are saved on their own, not with "Save". */}
      <CommentSection cardId={cardId} boardId={boardId} currentUserId={user?.id} role={role} />
    </div>
  )
}

interface FormProps {
  card: Card
  members: Member[]
  membersLoading: boolean
  boardLabels: Label[]
  boardId: string
  onClose: () => void
  onReload: () => void
  reloading: boolean
}

function CardForm({
  card,
  members,
  membersLoading,
  boardLabels,
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
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<CardValues>({
    resolver: zodResolver(cardSchema),
    defaultValues: {
      title: card.title,
      description: card.description ?? '',
      assigneeId: card.assignee?.id ?? '',
      dueDate: card.dueDate ?? '',
      labelIds: card.labels.map((label) => label.id),
    },
  })

  // mutateAsync rather than mutate(..., { onSuccess }): saving stores a new version in the
  // cache, which remounts this form (key={version}) before the request has fully settled, and
  // TanStack Query drops the per-call callbacks of an unmounted component. The promise still
  // resolves, so closing the dialog and the toast do not depend on this component surviving.
  const onSubmit = handleSubmit(async (values) => {
    try {
      await updateCard.mutateAsync({
        title: values.title,
        description: emptyToNull(values.description),
        assigneeId: values.assigneeId || null,
        dueDate: values.dueDate || null,
        labelIds: values.labelIds,
        // The version this form was filled from: if somebody saved since, the server says 409.
        version: card.version,
      })
    } catch {
      // Shown by the alert below (updateCard.error); nothing else to do here.
      return
    }
    toast.success('Card saved')
    onClose()
  })

  const onDelete = async () => {
    try {
      await deleteCard.mutateAsync(card.id)
    } catch {
      return
    }
    toast.success('Card deleted')
    onClose()
  }

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

      <fieldset>
        <legend className="mb-1 text-sm font-medium text-slate-700">Labels</legend>
        {boardLabels.length === 0 ? (
          <p className="text-sm text-slate-500">This board has no labels yet.</p>
        ) : (
          // Controlled on purpose: with register(), React Hook Form returns a list for several
          // checkboxes of the same name but a single string (or false) when there is only one,
          // so a board with one label could not save. Here the value is always a list.
          <Controller
            control={control}
            name="labelIds"
            render={({ field }) => (
              <div className="flex flex-wrap gap-x-4 gap-y-2">
                {boardLabels.map((label) => (
                  <label key={label.id} className="flex items-center gap-1.5">
                    <input
                      type="checkbox"
                      checked={field.value.includes(label.id)}
                      onChange={(event) =>
                        field.onChange(
                          event.target.checked
                            ? [...field.value, label.id]
                            : field.value.filter((id) => id !== label.id),
                        )
                      }
                      onBlur={field.onBlur}
                      className="size-4 rounded border-slate-300"
                    />
                    <LabelChip label={label} />
                  </label>
                ))}
              </div>
            )}
          />
        )}
      </fieldset>

      {confirmingDelete && (
        <div className="flex flex-col gap-2 rounded-md border border-red-200 bg-red-50 p-3">
          <p className="text-sm text-red-800">Delete this card? This cannot be undone.</p>
          {deleteCard.isError && <Alert>{errorMessage(deleteCard.error)}</Alert>}
          <div className="flex gap-2">
            <Button
              variant="danger"
              size="sm"
              loading={deleteCard.isPending}
              onClick={() => void onDelete()}
            >
              Delete card
            </Button>
            <Button variant="secondary" size="sm" onClick={() => setConfirmingDelete(false)}>
              Keep it
            </Button>
          </div>
        </div>
      )}

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
