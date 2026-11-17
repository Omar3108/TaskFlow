import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Alert } from '../../../components/ui/Alert'
import { Button } from '../../../components/ui/Button'
import { Select } from '../../../components/ui/Select'
import { TextField } from '../../../components/ui/TextField'
import { errorMessage } from '../../../lib/errors'
import type { WorkspaceRole } from '../../../types/api'
import { useCreateInvitation, useInvitations, useRevokeInvitation } from '../hooks'
import { invitableRoles, ROLE_LABELS } from '../permissions'
import { invitationSchema, type InvitationValues } from '../schemas'

interface Props {
  workspaceId: string
  currentRole: WorkspaceRole
}

/** Invite form, the one-time invitation link, and the list of pending invitations. */
export function InvitationPanel({ workspaceId, currentRole }: Props) {
  const invitations = useInvitations(workspaceId, true)
  const createInvitation = useCreateInvitation(workspaceId)
  const revokeInvitation = useRevokeInvitation(workspaceId)
  // The server returns the token only once, so the link is kept here until dismissed.
  const [link, setLink] = useState<{ email: string; url: string } | null>(null)
  const [copied, setCopied] = useState(false)

  const roles = invitableRoles(currentRole)
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<InvitationValues>({
    resolver: zodResolver(invitationSchema),
    defaultValues: { email: '', role: 'MEMBER' },
  })

  const onSubmit = handleSubmit((values) => {
    setLink(null)
    setCopied(false)
    createInvitation.mutate(values, {
      onSuccess: ({ invitation, token }) => {
        setLink({ email: invitation.email, url: `${window.location.origin}/invite/${token}` })
        reset()
      },
    })
  })

  const copyLink = async () => {
    if (!link) return
    try {
      await navigator.clipboard.writeText(link.url)
      setCopied(true)
    } catch {
      // Clipboard access can be denied; the link stays visible to copy by hand.
      setCopied(false)
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <form
        onSubmit={onSubmit}
        noValidate
        className="flex flex-col gap-3 rounded-xl border border-slate-200 bg-white p-4 sm:flex-row sm:items-start"
      >
        <div className="flex-1">
          <TextField
            label="Email address"
            type="email"
            placeholder="teammate@example.com"
            error={errors.email?.message}
            {...register('email')}
          />
        </div>
        <Select label="Role" {...register('role')}>
          {roles.map((role) => (
            <option key={role} value={role}>
              {ROLE_LABELS[role]}
            </option>
          ))}
        </Select>
        <Button type="submit" loading={createInvitation.isPending} className="sm:mt-6">
          Invite
        </Button>
      </form>

      {createInvitation.isError && <Alert>{errorMessage(createInvitation.error)}</Alert>}

      {link && (
        <div
          role="status"
          className="flex flex-col gap-2 rounded-xl border border-emerald-200 bg-emerald-50 p-4"
        >
          <p className="text-sm text-emerald-900">
            Invitation created for <span className="font-medium">{link.email}</span>. Share this
            link with them — it is shown only once.
          </p>
          <div className="flex flex-col gap-2 sm:flex-row">
            <input
              readOnly
              value={link.url}
              aria-label="Invitation link"
              onFocus={(event) => event.target.select()}
              className="min-w-0 flex-1 rounded-md border border-emerald-300 bg-white px-3 py-2 text-sm text-slate-700"
            />
            <Button variant="secondary" onClick={() => void copyLink()}>
              {copied ? 'Copied' : 'Copy link'}
            </Button>
          </div>
        </div>
      )}

      {revokeInvitation.isError && <Alert>{errorMessage(revokeInvitation.error)}</Alert>}

      {invitations.isError && <Alert>{errorMessage(invitations.error)}</Alert>}
      {invitations.data && invitations.data.length === 0 && (
        <p className="text-sm text-slate-500">No pending invitations.</p>
      )}
      {invitations.data && invitations.data.length > 0 && (
        <ul className="divide-y divide-slate-200 rounded-xl border border-slate-200 bg-white">
          {invitations.data.map((invitation) => (
            <li key={invitation.id} className="flex items-center justify-between gap-3 p-4">
              <div className="min-w-0">
                <p className="truncate font-medium text-slate-900">{invitation.email}</p>
                <p className="text-sm text-slate-500">
                  {ROLE_LABELS[invitation.role]} · invited by {invitation.invitedByName} · expires{' '}
                  {new Date(invitation.expiresAt).toLocaleDateString()}
                </p>
              </div>
              <Button
                variant="secondary"
                size="sm"
                disabled={revokeInvitation.isPending}
                onClick={() => revokeInvitation.mutate(invitation.id)}
                aria-label={`Revoke invitation for ${invitation.email}`}
              >
                Revoke
              </Button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
