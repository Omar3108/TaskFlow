import { useState } from 'react'
import { toast } from 'sonner'
import { Alert } from '../../../components/ui/Alert'
import { Button } from '../../../components/ui/Button'
import { ConfirmDialog } from '../../../components/ui/ConfirmDialog'
import { Select } from '../../../components/ui/Select'
import { errorMessage } from '../../../lib/errors'
import type { Member, WorkspaceRole } from '../../../types/api'
import { useChangeMemberRole, useMembers, useRemoveMember } from '../hooks'
import { canChangeRoleOf, canRemove, ROLE_LABELS } from '../permissions'
import { RoleBadge } from './RoleBadge'

interface Props {
  workspaceId: string
  currentUserId: string
  currentRole: WorkspaceRole
}

export function MemberList({ workspaceId, currentUserId, currentRole }: Props) {
  const members = useMembers(workspaceId)
  const changeRole = useChangeMemberRole(workspaceId)
  const removeMember = useRemoveMember(workspaceId)
  const [memberToRemove, setMemberToRemove] = useState<Member | null>(null)

  if (members.isPending) {
    return (
      <p role="status" className="text-sm text-slate-500">
        Loading members…
      </p>
    )
  }
  if (members.isError) {
    return <Alert>{errorMessage(members.error)}</Alert>
  }

  const closeRemoveDialog = () => {
    setMemberToRemove(null)
    removeMember.reset()
  }

  return (
    <>
      {changeRole.isError && (
        <div className="mb-3">
          <Alert>{errorMessage(changeRole.error)}</Alert>
        </div>
      )}

      <ul className="divide-y divide-slate-200 rounded-xl border border-slate-200 bg-white">
        {members.data.map((member) => {
          const isSelf = member.userId === currentUserId
          const roleEditable = !isSelf && canChangeRoleOf(currentRole, member.role)
          const removable = !isSelf && canRemove(currentRole, member.role)

          return (
            <li
              key={member.userId}
              className="flex flex-col gap-3 p-4 sm:flex-row sm:items-center sm:justify-between"
            >
              <div className="min-w-0">
                <p className="truncate font-medium text-slate-900">
                  {member.displayName}
                  {isSelf && <span className="ml-1 font-normal text-slate-500">(you)</span>}
                </p>
                <p className="truncate text-sm text-slate-500">{member.email}</p>
              </div>

              <div className="flex shrink-0 items-center gap-3">
                {roleEditable ? (
                  <Select
                    label={`Role of ${member.displayName}`}
                    hideLabel
                    value={member.role}
                    disabled={changeRole.isPending}
                    onChange={(event) =>
                      changeRole.mutate({
                        userId: member.userId,
                        role: event.target.value as WorkspaceRole,
                      })
                    }
                    className="py-1"
                  >
                    <option value="MEMBER">{ROLE_LABELS.MEMBER}</option>
                    <option value="ADMIN">{ROLE_LABELS.ADMIN}</option>
                  </Select>
                ) : (
                  <RoleBadge role={member.role} />
                )}
                {removable && (
                  <Button
                    variant="secondary"
                    size="sm"
                    onClick={() => setMemberToRemove(member)}
                    aria-label={`Remove ${member.displayName}`}
                  >
                    Remove
                  </Button>
                )}
              </div>
            </li>
          )
        })}
      </ul>

      <ConfirmDialog
        open={memberToRemove !== null}
        title="Remove member"
        confirmLabel="Remove"
        loading={removeMember.isPending}
        error={removeMember.isError ? errorMessage(removeMember.error) : null}
        onCancel={closeRemoveDialog}
        onConfirm={() => {
          if (memberToRemove) {
            removeMember.mutate(memberToRemove.userId, {
              onSuccess: () => {
                toast.success(`${memberToRemove.displayName} was removed`)
                closeRemoveDialog()
              },
            })
          }
        }}
      >
        <span className="font-medium">{memberToRemove?.displayName}</span> will lose access to this
        workspace and everything in it.
      </ConfirmDialog>
    </>
  )
}
