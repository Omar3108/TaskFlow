import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { Alert } from '../../../components/ui/Alert'
import { Button } from '../../../components/ui/Button'
import { Modal } from '../../../components/ui/Modal'
import { TextField } from '../../../components/ui/TextField'
import { errorMessage } from '../../../lib/errors'
import type { Workspace } from '../../../types/api'
import { useCreateWorkspace } from '../hooks'
import { workspaceNameSchema, type WorkspaceNameValues } from '../schemas'

interface Props {
  open: boolean
  onClose: () => void
  onCreated: (workspace: Workspace) => void
}

export function CreateWorkspaceDialog({ open, onClose, onCreated }: Props) {
  return (
    <Modal open={open} onClose={onClose} title="Create workspace">
      <CreateWorkspaceForm onCancel={onClose} onCreated={onCreated} />
    </Modal>
  )
}

function CreateWorkspaceForm({
  onCancel,
  onCreated,
}: {
  onCancel: () => void
  onCreated: (workspace: Workspace) => void
}) {
  const createWorkspace = useCreateWorkspace()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<WorkspaceNameValues>({
    resolver: zodResolver(workspaceNameSchema),
    defaultValues: { name: '' },
  })

  const onSubmit = handleSubmit(({ name }) =>
    createWorkspace.mutate(name, { onSuccess: onCreated }),
  )

  return (
    <form onSubmit={onSubmit} noValidate className="flex flex-col gap-4">
      {createWorkspace.isError && <Alert>{errorMessage(createWorkspace.error)}</Alert>}
      <TextField
        label="Workspace name"
        placeholder="e.g. Marketing team"
        autoFocus
        error={errors.name?.message}
        {...register('name')}
      />
      <div className="flex justify-end gap-2">
        <Button variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" loading={createWorkspace.isPending}>
          Create
        </Button>
      </div>
    </form>
  )
}
