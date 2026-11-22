import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Alert } from '../../../components/ui/Alert'
import { Button } from '../../../components/ui/Button'
import { Modal } from '../../../components/ui/Modal'
import { TextField } from '../../../components/ui/TextField'
import { errorMessage } from '../../../lib/errors'
import type { Label, LabelColor } from '../../../types/api'
import { LABEL_COLOR_VALUES, LABEL_COLORS } from '../colors'
import { useCreateLabel, useDeleteLabel, useLabels, useUpdateLabel } from '../hooks'
import { labelSchema, type LabelValues } from '../schemas'
import { LabelChip } from './LabelChip'

interface Props {
  boardId: string
  open: boolean
  onClose: () => void
}

/** Create, rename, recolour and delete the labels of a board (admins only). */
export function LabelManagerDialog({ boardId, open, onClose }: Props) {
  return (
    <Modal open={open} onClose={onClose} title="Labels">
      <LabelManager boardId={boardId} onClose={onClose} />
    </Modal>
  )
}

function LabelManager({ boardId, onClose }: { boardId: string; onClose: () => void }) {
  const labels = useLabels(boardId)
  const createLabel = useCreateLabel(boardId)
  const [editingId, setEditingId] = useState<string | null>(null)

  return (
    <div className="flex flex-col gap-5">
      {labels.isPending && (
        <p role="status" className="text-sm text-slate-500">
          Loading labels…
        </p>
      )}
      {labels.isError && <Alert>{errorMessage(labels.error)}</Alert>}
      {labels.data && labels.data.length === 0 && (
        <p className="text-sm text-slate-500">This board has no labels yet.</p>
      )}
      {labels.data && labels.data.length > 0 && (
        <ul className="flex flex-col gap-2">
          {labels.data.map((label) =>
            editingId === label.id ? (
              <li key={label.id} className="rounded-md border border-slate-200 p-3">
                <EditLabel boardId={boardId} label={label} onDone={() => setEditingId(null)} />
              </li>
            ) : (
              <LabelRow
                key={label.id}
                boardId={boardId}
                label={label}
                onEdit={() => setEditingId(label.id)}
              />
            ),
          )}
        </ul>
      )}

      <div className="border-t border-slate-200 pt-4">
        <h3 className="mb-2 text-sm font-semibold text-slate-900">New label</h3>
        <LabelForm
          submitLabel="Create label"
          pending={createLabel.isPending}
          error={createLabel.isError ? errorMessage(createLabel.error) : null}
          onSubmit={async (values) => {
            try {
              await createLabel.mutateAsync(values)
              return true
            } catch {
              return false
            }
          }}
        />
      </div>

      <div className="flex justify-end">
        <Button variant="secondary" onClick={onClose}>
          Done
        </Button>
      </div>
    </div>
  )
}

function LabelRow({
  boardId,
  label,
  onEdit,
}: {
  boardId: string
  label: Label
  onEdit: () => void
}) {
  const deleteLabel = useDeleteLabel(boardId)
  const [confirming, setConfirming] = useState(false)

  return (
    <li className="flex flex-col gap-2 rounded-md border border-slate-200 px-3 py-2">
      <div className="flex items-center justify-between gap-2">
        <LabelChip label={label} />
        {!confirming && (
          <div className="flex gap-1">
            <Button
              size="sm"
              variant="secondary"
              aria-label={`Edit label ${label.name}`}
              onClick={onEdit}
            >
              Edit
            </Button>
            <Button
              size="sm"
              variant="secondary"
              aria-label={`Delete label ${label.name}`}
              onClick={() => setConfirming(true)}
            >
              Delete
            </Button>
          </div>
        )}
      </div>
      {confirming && (
        <div className="flex flex-col gap-2">
          <p className="text-sm text-slate-600">
            Delete this label? It will be removed from every card.
          </p>
          {deleteLabel.isError && <Alert>{errorMessage(deleteLabel.error)}</Alert>}
          <div className="flex gap-2">
            <Button
              size="sm"
              variant="danger"
              loading={deleteLabel.isPending}
              onClick={() => deleteLabel.mutate(label.id)}
            >
              Delete label
            </Button>
            <Button size="sm" variant="secondary" onClick={() => setConfirming(false)}>
              Cancel
            </Button>
          </div>
        </div>
      )}
    </li>
  )
}

function EditLabel({
  boardId,
  label,
  onDone,
}: {
  boardId: string
  label: Label
  onDone: () => void
}) {
  const updateLabel = useUpdateLabel(boardId)
  return (
    <LabelForm
      initial={{ name: label.name, color: label.color }}
      submitLabel="Save"
      pending={updateLabel.isPending}
      error={updateLabel.isError ? errorMessage(updateLabel.error) : null}
      onCancel={onDone}
      onSubmit={async (values) => {
        try {
          await updateLabel.mutateAsync({ labelId: label.id, ...values })
          onDone()
          return false
        } catch {
          return false
        }
      }}
    />
  )
}

interface LabelFormProps {
  initial?: { name: string; color: LabelColor }
  submitLabel: string
  pending: boolean
  error: string | null
  /** Return true to clear the form for the next label. */
  onSubmit: (values: { name: string; color: LabelColor }) => Promise<boolean>
  onCancel?: () => void
}

function LabelForm({ initial, submitLabel, pending, error, onSubmit, onCancel }: LabelFormProps) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<LabelValues>({
    resolver: zodResolver(labelSchema),
    defaultValues: initial ?? { name: '', color: 'BLUE' },
  })

  const submit = handleSubmit(async (values) => {
    if (await onSubmit({ name: values.name, color: values.color as LabelColor })) {
      reset({ name: '', color: values.color })
    }
  })

  return (
    <form onSubmit={submit} noValidate className="flex flex-col gap-3">
      {error && <Alert>{error}</Alert>}
      <TextField label="Label name" error={errors.name?.message} {...register('name')} />
      <fieldset>
        <legend className="mb-1 text-sm font-medium text-slate-700">Color</legend>
        <div className="flex flex-wrap gap-2">
          {LABEL_COLOR_VALUES.map((color) => (
            <label key={color} className="cursor-pointer">
              <input type="radio" value={color} className="peer sr-only" {...register('color')} />
              <span
                className={`block size-7 rounded-full ring-offset-2 peer-checked:ring-2 peer-checked:ring-slate-900 peer-focus-visible:outline-2 peer-focus-visible:outline-indigo-600 ${LABEL_COLORS[color].swatch}`}
              />
              <span className="sr-only">{LABEL_COLORS[color].name}</span>
            </label>
          ))}
        </div>
      </fieldset>
      <div className="flex gap-2">
        <Button type="submit" size="sm" loading={pending}>
          {submitLabel}
        </Button>
        {onCancel && (
          <Button size="sm" variant="secondary" onClick={onCancel}>
            Cancel
          </Button>
        )}
      </div>
    </form>
  )
}
