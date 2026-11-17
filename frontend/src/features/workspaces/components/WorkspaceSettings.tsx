import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Alert } from '../../../components/ui/Alert'
import { Button } from '../../../components/ui/Button'
import { ConfirmDialog } from '../../../components/ui/ConfirmDialog'
import { TextField } from '../../../components/ui/TextField'
import { errorMessage } from '../../../lib/errors'
import type { Workspace } from '../../../types/api'
import { useDeleteWorkspace, useLeaveWorkspace, useRenameWorkspace } from '../hooks'
import { canEditWorkspace, canLeave } from '../permissions'
import { workspaceNameSchema, type WorkspaceNameValues } from '../schemas'

interface Props {
  workspace: Workspace
  currentUserId: string
  /** Called after the workspace was deleted or left: it is no longer accessible. */
  onGone: () => void
}

export function WorkspaceSettings({ workspace, currentUserId, onGone }: Props) {
  const rename = useRenameWorkspace(workspace.id)
  const deleteWorkspace = useDeleteWorkspace(workspace.id)
  const leave = useLeaveWorkspace(workspace.id)
  const [confirming, setConfirming] = useState<'delete' | 'leave' | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isDirty },
    reset,
  } = useForm<WorkspaceNameValues>({
    resolver: zodResolver(workspaceNameSchema),
    defaultValues: { name: workspace.name },
  })

  const onRename = handleSubmit(({ name }) =>
    rename.mutate(name, { onSuccess: (updated) => reset({ name: updated.name }) }),
  )

  const closeDialog = () => {
    setConfirming(null)
    deleteWorkspace.reset()
    leave.reset()
  }

  return (
    <div className="flex flex-col gap-4">
      {canEditWorkspace(workspace.role) && (
        <form
          onSubmit={onRename}
          noValidate
          className="flex flex-col gap-3 rounded-xl border border-slate-200 bg-white p-4 sm:flex-row sm:items-start"
        >
          <div className="flex-1">
            <TextField label="Workspace name" error={errors.name?.message} {...register('name')} />
          </div>
          <Button
            type="submit"
            variant="secondary"
            disabled={!isDirty}
            loading={rename.isPending}
            className="sm:mt-6"
          >
            Rename
          </Button>
        </form>
      )}
      {rename.isError && <Alert>{errorMessage(rename.error)}</Alert>}

      <div className="flex flex-col gap-3 rounded-xl border border-red-200 bg-white p-4 sm:flex-row sm:items-center sm:justify-between">
        {canEditWorkspace(workspace.role) ? (
          <>
            <p className="text-sm text-slate-600">
              Deleting the workspace removes its members, invitations and boards for everyone.
            </p>
            <Button variant="danger" onClick={() => setConfirming('delete')}>
              Delete workspace
            </Button>
          </>
        ) : (
          canLeave(workspace.role) && (
            <>
              <p className="text-sm text-slate-600">
                You will lose access to this workspace until someone invites you again.
              </p>
              <Button variant="danger" onClick={() => setConfirming('leave')}>
                Leave workspace
              </Button>
            </>
          )
        )}
      </div>

      <ConfirmDialog
        open={confirming === 'delete'}
        title="Delete workspace"
        confirmLabel="Delete"
        loading={deleteWorkspace.isPending}
        error={deleteWorkspace.isError ? errorMessage(deleteWorkspace.error) : null}
        onCancel={closeDialog}
        onConfirm={() => deleteWorkspace.mutate(undefined, { onSuccess: onGone })}
      >
        <span className="font-medium">{workspace.name}</span> and everything in it will be
        permanently deleted. This cannot be undone.
      </ConfirmDialog>

      <ConfirmDialog
        open={confirming === 'leave'}
        title="Leave workspace"
        confirmLabel="Leave"
        loading={leave.isPending}
        error={leave.isError ? errorMessage(leave.error) : null}
        onCancel={closeDialog}
        onConfirm={() => leave.mutate(currentUserId, { onSuccess: onGone })}
      >
        You will lose access to <span className="font-medium">{workspace.name}</span>.
      </ConfirmDialog>
    </div>
  )
}
